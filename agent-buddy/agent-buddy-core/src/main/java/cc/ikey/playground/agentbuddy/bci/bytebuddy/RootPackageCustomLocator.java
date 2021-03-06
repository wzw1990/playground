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

import net.bytebuddy.dynamic.ClassFileLocator;

import java.io.IOException;

public class RootPackageCustomLocator implements ClassFileLocator {

    private final String rootPackage;
    private final ClassFileLocator classFileLocator;

    public RootPackageCustomLocator(String rootPackage, ClassFileLocator classFileLocator) {
        this.rootPackage = rootPackage;
        this.classFileLocator = classFileLocator;
    }

    /**
     * {@inheritDoc}
     */
    public Resolution locate(String name) throws IOException {
        return name.startsWith(rootPackage) ? classFileLocator.locate(name) : new Resolution.Illegal(name);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        classFileLocator.close();
    }
}
