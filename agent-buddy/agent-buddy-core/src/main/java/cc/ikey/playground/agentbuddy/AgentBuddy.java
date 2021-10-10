/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cc.ikey.playground.agentbuddy;

import cc.ikey.playground.agentbuddy.bci.IndyBootstrap;
import cc.ikey.playground.agentbuddy.bci.bytebuddy.*;
import cc.ikey.playground.agentbuddy.bci.bytebuddy.postprocessor.AssignToPostProcessorFactory;
import cc.ikey.playground.agentbuddy.classloader.PluginClassLoader;
import cc.ikey.playground.agentbuddy.logging.AgentLogger;
import cc.ikey.playground.agentbuddy.sdk.MethodInstrumentation;
import cc.ikey.playground.agentbuddy.util.DependencyInjectingServiceLoader;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.TypeConstantAdjustment;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static cc.ikey.playground.agentbuddy.bci.bytebuddy.ClassLoaderNameMatcher.classLoaderWithName;
import static cc.ikey.playground.agentbuddy.bci.bytebuddy.ClassLoaderNameMatcher.isReflectionClassLoader;
import static net.bytebuddy.asm.Advice.ExceptionHandler.Default.PRINTING;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class AgentBuddy {
    private static AgentLogger LOGGER = AgentLogger.getLogger(AgentBuddy.class);

    private static Instrumentation instrumentation;
    private static File agentJarFile;
    private static ResettableClassFileTransformer resettableClassFileTransformer;
    /**
     * A mapping from advice class name to the class loader that loaded the corresponding instrumentation.
     * We need this in order to locate the advice class file. This implies that the advice class needs to be collocated
     * with the corresponding instrumentation class.
     */
    private static final Map<String, ClassLoader> adviceClassName2instrumentationClassLoader = new ConcurrentHashMap<>();

    public static void initialize(final String agentArguments, final Instrumentation instrumentation, final File agentJarFile) {
        AgentBuddy.instrumentation = instrumentation;
        AgentBuddy.agentJarFile = agentJarFile;
        LOGGER.info("玛卡巴卡");
    }

    private static synchronized void initInstrumentation(Instrumentation instrumentation,
                                                         Iterable<MethodInstrumentation> instrumentations) {
        if (AgentBuddy.instrumentation != null) {
            LOGGER.warning("Instrumentation has already been initialized");
            return;
        }
        for (MethodInstrumentation apmInstrumentation : instrumentations) {
            adviceClassName2instrumentationClassLoader.put(
                    apmInstrumentation.getAdviceClassName(),
                    apmInstrumentation.getClass().getClassLoader());
        }
        AgentBuilder agentBuilder = initAgentBuilder(instrumentation, instrumentations, AgentBuilder.DescriptionStrategy.Default.POOL_ONLY);
        resettableClassFileTransformer = agentBuilder.installOn(AgentBuddy.instrumentation);
    }

    private static AgentBuilder initAgentBuilder(Instrumentation instrumentation,
                                                 Iterable<MethodInstrumentation> instrumentations,
                                                 AgentBuilder.DescriptionStrategy descriptionStrategy) {
        AgentBuddy.instrumentation = instrumentation;
        final ByteBuddy byteBuddy = new ByteBuddy()
                .with(TypeValidation.of(LOGGER.isDebugEnabled()))
                .with(FailSafeDeclaredMethodsCompiler.INSTANCE);
        AgentBuilder agentBuilder = getAgentBuilder(
                byteBuddy, descriptionStrategy, true
        );
        int numberOfAdvices = 0;
        for (final MethodInstrumentation advice : instrumentations) {
            numberOfAdvices++;
            agentBuilder = applyAdvice(agentBuilder, advice, advice.getTypeMatcher());
        }
        LOGGER.debug("Applied {} advices", numberOfAdvices);
        return agentBuilder;
    }

    private static AgentBuilder applyAdvice(final AgentBuilder agentBuilder,
                                            final MethodInstrumentation instrumentation, final ElementMatcher<? super TypeDescription> typeMatcher) {
        LOGGER.debug("Applying instrumentation {}", instrumentation.getClass().getName());
        final ElementMatcher.Junction<ProtectionDomain> versionPostFilter = instrumentation.getProtectionDomainPostFilter();
        final ElementMatcher<? super MethodDescription> methodMatcher = new ElementMatcher.Junction.Conjunction<>(instrumentation.getMethodMatcher(), not(isAbstract()));
        return agentBuilder
                .type((typeDescription, classLoader, module, classBeingRedefined, protectionDomain) -> {
                    boolean typeMatches;
                    try {
                        typeMatches = typeMatcher.matches(typeDescription) && versionPostFilter.matches(protectionDomain);
                    } catch (Exception ignored) {
                        // could be because of a missing type
                        typeMatches = false;
                    }
                    if (typeMatches) {
                        LOGGER.debug("Type match for instrumentation {}: {} matches {}",
                                instrumentation.getClass().getSimpleName(), typeMatcher, typeDescription);
                        try {
                            instrumentation.onTypeMatch(typeDescription, classLoader, protectionDomain, classBeingRedefined);
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                        if (LOGGER.isDebugEnabled()) {
                            logClassLoaderHierarchy(classLoader, instrumentation);
                        }
                    }
                    return typeMatches;
                })
                .transform(new PatchBytecodeVersionTo51Transformer())
                .transform(getTransformer(instrumentation, methodMatcher))
                .transform((builder, typeDescription, classLoader, module) -> builder.visit(MinimumClassFileVersionValidator.V1_4)
                        // As long as we allow 1.4 bytecode, we need to add this constant pool adjustment as well
                        .visit(TypeConstantAdjustment.INSTANCE));
    }

    private static AgentBuilder.Transformer.ForAdvice getTransformer(final MethodInstrumentation instrumentation, final ElementMatcher<? super MethodDescription> methodMatcher) {
        validateAdvice(instrumentation);
        Advice.WithCustomMapping withCustomMapping = Advice
                .withCustomMapping()
                .with(new AssignToPostProcessorFactory())
                .bind(new SimpleMethodSignatureOffsetMappingFactory())
                .bind(new AnnotationValueOffsetMappingFactory());
        Advice.OffsetMapping.Factory<?> offsetMapping = instrumentation.getOffsetMapping();
        if (offsetMapping != null) {
            withCustomMapping = withCustomMapping.bind(offsetMapping);
        }
        withCustomMapping = withCustomMapping.bootstrap(IndyBootstrap.getIndyBootstrapMethod());
        return new AgentBuilder.Transformer.ForAdvice(withCustomMapping)
                .advice((ElementMatcher<MethodDescription>) target -> {
                    boolean matches;
                    try {
                        matches = methodMatcher.matches(target);
                    } catch (Exception ignored) {
                        // could be because of a missing type
                        matches = false;
                    }
                    if (matches) {
                        LOGGER.debug("Method match for instrumentation {}: {} matches {}",
                                instrumentation.getClass().getSimpleName(), methodMatcher, target);
                    }
                    return matches;
                }, instrumentation.getAdviceClassName())
                .include(ClassLoader.getSystemClassLoader(), instrumentation.getClass().getClassLoader())
                .withExceptionHandler(PRINTING);
    }

    public static void validateAdvice(MethodInstrumentation instrumentation) {
        String adviceClassName = instrumentation.getAdviceClassName();
        if (instrumentation.getClass().getName().equals(adviceClassName)) {
            throw new IllegalStateException("The advice must be declared in a separate class: " + adviceClassName);
        }
        ClassLoader adviceClassLoader = instrumentation.getClass().getClassLoader();
        if (adviceClassLoader == null) {
            // the bootstrap class loader can't do resource lookup
            // if classes are added via java.lang.instrument.Instrumentation.appendToBootstrapClassLoaderSearch
            adviceClassLoader = ClassLoader.getSystemClassLoader();
        }
        TypePool pool = new TypePool.Default.WithLazyResolution(TypePool.CacheProvider.NoOp.INSTANCE, ClassFileLocator.ForClassLoader.of(adviceClassLoader), TypePool.Default.ReaderMode.FAST);
        TypeDescription typeDescription = pool.describe(adviceClassName).resolve();
        int adviceModifiers = typeDescription.getModifiers();
        if (!Modifier.isPublic(adviceModifiers)) {
            throw new IllegalStateException(String.format("advice class %s should be public", adviceClassName));
        }
        for (MethodDescription.InDefinedShape enterAdvice : typeDescription.getDeclaredMethods().filter(isStatic().and(isAnnotatedWith(Advice.OnMethodEnter.class)))) {
            validateAdviceReturnAndParameterTypes(enterAdvice, adviceClassName);

            for (AnnotationDescription enter : enterAdvice.getDeclaredAnnotations().filter(ElementMatchers.annotationType(Advice.OnMethodEnter.class))) {
                checkInline(enterAdvice, adviceClassName, enter.prepare(Advice.OnMethodEnter.class).load().inline());
            }
        }
        for (MethodDescription.InDefinedShape exitAdvice : typeDescription.getDeclaredMethods().filter(isStatic().and(isAnnotatedWith(Advice.OnMethodExit.class)))) {
            validateAdviceReturnAndParameterTypes(exitAdvice, adviceClassName);
            if (exitAdvice.getReturnType().asRawType().getTypeName().startsWith("cc.ikey.playground.agentbuddy")) {
                throw new IllegalStateException("Advice return type must be visible from the bootstrap class loader and must not be an agent type.");
            }
            for (AnnotationDescription exit : exitAdvice.getDeclaredAnnotations().filter(ElementMatchers.annotationType(Advice.OnMethodExit.class))) {
                checkInline(exitAdvice, adviceClassName, exit.prepare(Advice.OnMethodExit.class).load().inline());
            }
        }
        if (!(adviceClassLoader instanceof PluginClassLoader) && !adviceClassName.startsWith("cc.ikey.playground.agentbuddy.")) {
            throw new IllegalStateException(String.format(
                    "Invalid Advice class - %s - Indy-dispatched advice class must be in a sub-package of 'cc.ikey.playground.agentbuddy'.",
                    adviceClassName)
            );

        }
    }

    private static void validateAdviceReturnAndParameterTypes(MethodDescription.InDefinedShape advice, String adviceClass) {
        String adviceMethod = advice.getInternalName();
        try {
            checkNotAgentType(advice.getReturnType(), "return type", adviceClass, adviceMethod);

            for (ParameterDescription.InDefinedShape parameter : advice.getParameters()) {
                checkNotAgentType(parameter.getType(), "parameter", adviceClass, adviceMethod);

                AnnotationDescription.Loadable<Advice.Return> returnAnnotation = parameter.getDeclaredAnnotations().ofType(Advice.Return.class);
                if (returnAnnotation != null && !returnAnnotation.load().readOnly()) {
                    throw new IllegalStateException("Advice parameter must not use '@Advice.Return(readOnly=false)', use @AssignTo.Return instead");
                }
            }
        } catch (Exception e) {
            // Because types are lazily resolved, unexpected things are expected
            throw new IllegalStateException(String.format("unable to validate advice defined in %s#%s", adviceClass, adviceMethod), e);
        }
    }

    private static void checkInline(MethodDescription.InDefinedShape advice, String adviceClassName, boolean isInline) {
        if (isInline) {
            throw new IllegalStateException(String.format("Indy-dispatched advice %s#%s has to be declared with inline=false", adviceClassName, advice.getName()));
        } else if (!Modifier.isPublic(advice.getModifiers())) {
            throw new IllegalStateException(String.format("Indy-dispatched advice %s#%s has to be declared public", adviceClassName, advice.getName()));
        }
    }

    private static void checkNotAgentType(TypeDescription.Generic type, String description, String adviceClass, String adviceMethod) {
        // We have to use 'raw' type as framework classes are not accessible to the boostrap classloader, and
        // trying to resolve them will create exceptions.
        String name = type.asRawType().getTypeName();
        if (name.startsWith("cc.ikey.playground.agentbuddy")) {
            throw new IllegalStateException(String.format("Advice %s in %s#%s must not be an agent type: %s", description, adviceClass, adviceMethod, name));
        }
    }

    private static void logClassLoaderHierarchy(ClassLoader classLoader, MethodInstrumentation advice) {
        LOGGER.trace("Advice {} is loaded by {}", advice.getClass().getName(), advice.getClass().getClassLoader());
        if (classLoader != null) {
            boolean canLoadAgent = false;
            try {
                classLoader.loadClass(advice.getClass().getName());
                canLoadAgent = true;
            } catch (ClassNotFoundException ignore) {
            }
            LOGGER.trace("{} can load advice ({}): {}", classLoader, advice.getClass().getName(), canLoadAgent);
            logClassLoaderHierarchy(classLoader.getParent(), advice);
        } else {
            LOGGER.trace("bootstrap classloader");
        }
    }

    private static AgentBuilder getAgentBuilder(final ByteBuddy byteBuddy,
                                                final AgentBuilder.DescriptionStrategy descriptionStrategy,
                                                final boolean premain) {
        AgentBuilder.LocationStrategy locationStrategy = AgentBuilder.LocationStrategy.ForClassLoader.WEAK;
        if (agentJarFile != null) {
            try {
                locationStrategy = new AgentBuilder.LocationStrategy.Compound(
                        // it's important to first try loading from the agent jar and not the class loader of the instrumented class
                        // the latter may not have access to the agent resources:
                        // when adding the agent to the bootstrap CL (appendToBootstrapClassLoaderSearch)
                        // the bootstrap CL can load its classes but not its resources
                        // the application class loader may cache the fact that a resource like AbstractSpan.class can't be resolved
                        // and also refuse to load the class
                        new AgentBuilder.LocationStrategy.Simple(ClassFileLocator.ForJarFile.of(agentJarFile)),
                        AgentBuilder.LocationStrategy.ForClassLoader.WEAK,
                        new AgentBuilder.LocationStrategy.Simple(new RootPackageCustomLocator("java.", ClassFileLocator.ForClassLoader.ofBootLoader()))
                );
            } catch (IOException e) {
                LOGGER.warning("Failed to add ClassFileLocator for the agent jar. Some instrumentations may not work", e);
            }
        }
        return new AgentBuilder.Default(byteBuddy)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                // when runtime attaching, only retransform up to 100 classes at once and sleep 100ms in-between as retransformation causes a stop-the-world pause
                .with(premain ? AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE : AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(100))
                .with(premain ? AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE : AgentBuilder.RedefinitionStrategy.Listener.Pausing.of(100, TimeUnit.MILLISECONDS))
                .with(new AgentBuilder.RedefinitionStrategy.Listener.Adapter() {
                    @Override
                    public Iterable<? extends List<Class<?>>> onError(int index, List<Class<?>> batch, Throwable throwable, List<Class<?>> types) {
                        LOGGER.warning("Error while redefining classes {}", throwable.getMessage());
                        LOGGER.debug(throwable.getMessage(), throwable);
                        return super.onError(index, batch, throwable, types);
                    }
                })
                .with(descriptionStrategy)
                .with(locationStrategy)
                .with(new ErrorLoggingListener())
                // ReaderMode.FAST as we don't need to read method parameter names
                .with(AgentBuilder.PoolStrategy.Default.FAST)
                .ignore(any(), isReflectionClassLoader())
                .or(any(), classLoaderWithName("org.codehaus.groovy.runtime.callsite.CallSiteClassLoader"))
                .or(nameStartsWith("co.elastic.apm.agent.shaded"))
                .or(nameStartsWith("org.aspectj."))
                .or(nameStartsWith("org.groovy."))
                .or(nameStartsWith("com.p6spy."))
                .or(nameStartsWith("net.bytebuddy."))
                .or(nameStartsWith("org.stagemonitor."))
                .or(nameStartsWith("com.newrelic."))
                .or(nameStartsWith("com.dynatrace."))
                // AppDynamics
                .or(nameStartsWith("com.singularity."))
                .or(nameStartsWith("com.instana."))
                .or(nameStartsWith("datadog."))
                .or(nameStartsWith("org.glowroot."))
                .or(nameStartsWith("com.compuware."))
                .or(nameStartsWith("io.sqreen."))
                .or(nameStartsWith("com.contrastsecurity."))
                .or(nameContains("javassist"))
                .or(nameContains(".asm."))
                .disableClassFormatChanges();
    }

    private static Collection<? extends ClassLoader> createExternalPluginClassLoaders(String pluginsDirString) {
        if (pluginsDirString == null) {
            LOGGER.debug("No plugins dir");
            return Collections.emptyList();
        }
        File pluginsDir = new File(pluginsDirString);
        if (!pluginsDir.exists()) {
            LOGGER.debug("Plugins dir does not exist: {}", pluginsDirString);
            return Collections.emptyList();
        }
        File[] pluginJars = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (pluginJars == null) {
            LOGGER.info("Invalid plugins dir {}", pluginsDirString);
            return Collections.emptyList();
        }
        List<ClassLoader> result = new ArrayList<>(pluginJars.length);
        for (File pluginJar : pluginJars) {
            try {
                result.add(new PluginClassLoader(pluginJar, AgentBuddy.class.getClassLoader()));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
            LOGGER.info("Loading plugin {}", pluginJar.getName());
        }
        return result;
    }

    private static Iterable<MethodInstrumentation> loadInstrumentations() {
        List<ClassLoader> pluginClassLoaders = new ArrayList<>();
        pluginClassLoaders.add(AgentBuddy.class.getClassLoader());
        pluginClassLoaders.addAll(createExternalPluginClassLoaders("./plugins"));
        return DependencyInjectingServiceLoader.load(MethodInstrumentation.class, pluginClassLoaders);
    }

    public static ClassLoader getAgentClassLoader() {
        ClassLoader agentClassLoader = AgentBuddy.class.getClassLoader();
        if (agentClassLoader == null) {
            throw new IllegalStateException("Agent is loaded from bootstrap class loader as opposed to the dedicated agent class loader");
        }
        return agentClassLoader;
    }

    /**
     * Returns the class loader that loaded the instrumentation class corresponding the given advice class.
     * We expect to be able to find the advice class file through this class loader.
     *
     * @param adviceClass name of the advice class
     * @return class loader that can be used for the advice class file lookup
     */
    public static ClassLoader getInstrumentationClassLoader(String adviceClass) {
        ClassLoader classLoader = adviceClassName2instrumentationClassLoader.get(adviceClass);
        if (classLoader == null) {
            throw new IllegalStateException("There's no mapping for key " + adviceClass);
        }
        return classLoader;
    }

    public static File getAgentJarFile() {
        return agentJarFile;
    }

}
