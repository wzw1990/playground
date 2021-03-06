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

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;

import java.util.Collections;
import java.util.LinkedHashMap;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A {@link MethodGraph.Compiler} is used to determine all methods of a type which is about to be instrumented
 * <p>
 * In order to be able to instrument types which refer to unknown classes in some methods,
 * we eagerly resolve the parameters of all methods and exclude problematic methods.
 * </p>
 * <p>
 * Similar to {@link ForDeclaredMethods} but eagerly resolves arguments and fails safe
 * on exceptions in that it ignores methods causing exceptions.
 * </p>
 */
public enum FailSafeDeclaredMethodsCompiler implements MethodGraph.Compiler {

    /**
     * The singleton instance.
     */
    INSTANCE;

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodGraph.Linked compile(TypeDefinition typeDefinition) {
        return compile(typeDefinition, typeDefinition.asErasure());
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public MethodGraph.Linked compile(TypeDescription typeDescription) {
        return compile((TypeDefinition) typeDescription, typeDescription);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodGraph.Linked compile(TypeDefinition typeDefinition, TypeDescription viewPoint) {
        LinkedHashMap<MethodDescription.SignatureToken, MethodGraph.Node> nodes = new LinkedHashMap<MethodDescription.SignatureToken, MethodGraph.Node>();
        for (MethodDescription methodDescription : typeDefinition.getDeclaredMethods()
            .filter(
                // ignores all methods which refer to unknown types
                failSafe(
                    isVirtual()
                        .and(not(isBridge()))
                        .and(isVisibleTo(viewPoint))
                        .and(hasParameters(whereNone(hasType(not(isVisibleTo(viewPoint))))))
                )
            )
        ) {
            nodes.put(methodDescription.asSignatureToken(), new MethodGraph.Node.Simple(methodDescription));
        }
        return new MethodGraph.Linked.Delegation(new MethodGraph.Simple(nodes), MethodGraph.Empty.INSTANCE, Collections.<TypeDescription, MethodGraph>emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public MethodGraph.Linked compile(TypeDescription typeDescription, TypeDescription viewPoint) {
        return compile((TypeDefinition) typeDescription, viewPoint);
    }
}
