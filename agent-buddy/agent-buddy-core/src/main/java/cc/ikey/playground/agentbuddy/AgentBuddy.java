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

import cc.ikey.playground.agentbuddy.classloader.PluginClassLoader;
import cc.ikey.playground.agentbuddy.logging.AgentLogger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AgentBuddy {
    private static AgentLogger LOGGER = AgentLogger.getLogger(AgentBuddy.class);

    private static Instrumentation instrumentation;
    private static File agentJarFile;

    public static void initialize(final String agentArguments, final Instrumentation instrumentation, final File agentJarFile) {
        AgentBuddy.instrumentation = instrumentation;
        AgentBuddy.agentJarFile = agentJarFile;
        LOGGER.info("玛卡巴卡");
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
}
