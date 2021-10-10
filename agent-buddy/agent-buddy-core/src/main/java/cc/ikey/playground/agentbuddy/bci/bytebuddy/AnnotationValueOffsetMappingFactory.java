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
package cc.ikey.playground.agentbuddy.bci.bytebuddy;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class AnnotationValueOffsetMappingFactory implements Advice.OffsetMapping.Factory<AnnotationValueOffsetMappingFactory.AnnotationValueExtractor> {

    @Override
    public Class<AnnotationValueExtractor> getAnnotationType() {
        return AnnotationValueExtractor.class;
    }

    @Override
    public Advice.OffsetMapping make(final ParameterDescription.InDefinedShape target,
                                     final AnnotationDescription.Loadable<AnnotationValueExtractor> annotation,
                                     final AdviceType adviceType) {
        return (instrumentedType, instrumentedMethod, assigner, argumentHandler, sort) -> Advice.OffsetMapping.Target.ForStackManipulation.of(getAnnotationValue(instrumentedMethod, annotation.load()));
    }

    private Object getAnnotationValue(MethodDescription instrumentedMethod, AnnotationValueExtractor annotationValueExtractor) {
        MethodDescription methodDescription = instrumentedMethod;
        do {
            for (TypeDescription typeDescription : methodDescription.getDeclaredAnnotations().asTypeList()) {
                if (named(annotationValueExtractor.annotationClassName()).matches(typeDescription)) {
                    for (MethodDescription.InDefinedShape annotationMethod : typeDescription.getDeclaredMethods()) {
                        if (annotationMethod.getName().equals(annotationValueExtractor.method())) {
                            return methodDescription.getDeclaredAnnotations().ofType(typeDescription).getValue(annotationMethod).resolve();
                        }
                    }
                }
            }

            methodDescription = findInstrumentedMethodInSuperClass(methodDescription.getDeclaringType().getSuperClass(), instrumentedMethod);
        } while (methodDescription != null);
        return null;
    }

    private MethodDescription findInstrumentedMethodInSuperClass(TypeDescription.Generic superClass, MethodDescription instrumentedMethod) {
        if (superClass == null) {
            return null;

        }
        for (MethodDescription declaredMethod : superClass.getDeclaredMethods()) {
            if (instrumentedMethod.getInternalName().equals(declaredMethod.getInternalName())
                    && instrumentedMethod.getParameters().asTypeList().asErasures().equals(declaredMethod.getParameters().asTypeList().asErasures())) {
                return declaredMethod;
            }
        }
        return null;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface AnnotationValueExtractor {
        String annotationClassName();

        String method();
    }

}
