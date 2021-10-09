package cc.ikey.game.agent;

import cc.ikey.game.agent.logging.AgentLogger;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.Optional;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;

public class AgentMain {
    private static AgentLogger LOGGER = AgentLogger.getLogger(AgentMain.class);

    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }

    public static void premain(String args, Instrumentation inst) {
        try {
            ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                    .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
    //                .with(poolStrategy)
    //                .with(installationListener)
    //                .with(listener)
                    .disableNativeMethodPrefix()
                    .ignore(none())
                    .type(named("java.lang.String")).transform(new FixedValueTransformer(88))
                    .installOn(inst);
            inst.addTransformer(new MyTransformer());
            LOGGER.info("Agent初始化完毕");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static class FixedValueTransformer implements AgentBuilder.Transformer {

        private final Object value;

        public FixedValueTransformer(Object value) {
            this.value = value;
        }

        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                TypeDescription typeDescription,
                                                ClassLoader classLoader,
                                                JavaModule module) {
            return builder.method(named("length")).intercept(FixedValue.value(value));
        }
    }
}
