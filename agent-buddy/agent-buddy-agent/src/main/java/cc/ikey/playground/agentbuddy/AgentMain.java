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

import cc.ikey.playground.agentbuddy.logging.AgentLogger;
import cc.ikey.playground.classloader.AgentClassLoader;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class AgentMain {
    private static AgentLogger LOGGER = AgentLogger.getLogger(AgentMain.class);
    private static final String ATTACH_SIGNAL_KEY = "AgentBuddyAgent.attached";

    public static void agentmain(String args, Instrumentation inst) {
        entry(args, inst);
    }

    public static void premain(String args, Instrumentation inst) {
        entry(args, inst);
    }

    private synchronized static void entry(String args, Instrumentation inst) {
        if (Boolean.getBoolean(ATTACH_SIGNAL_KEY)) {
            return;
        }
        LOGGER.info("初始化Agent Buddy!");
        try {
            File agentJar = getAgentJarFile();
            URLClassLoader agentClassLoader = new AgentClassLoader(agentJar, getAgentClassLoaderParent());
            Class.forName("cc.ikey.playground.agentbuddy.AgentBuddy", true, agentClassLoader)
                    .getMethod("initialize", String.class, Instrumentation.class, File.class)
                    .invoke(null, args, inst, agentJar);
            System.setProperty(ATTACH_SIGNAL_KEY, Boolean.TRUE.toString());
            LOGGER.info("初始化Agent Buddy完毕!");
        } catch (Throwable e) {
            LOGGER.error("初始化Agent Buddy异常", e);
        }
    }

    private static ClassLoader getAgentClassLoaderParent() {
        try {
            // JDK9及更新版本
            return (ClassLoader) ClassLoader.class.getDeclaredMethod("getPlatformClassLoader").invoke(null);
        } catch (Exception e) {
            // JDK8及以下版本
            return null;
        }
    }

    private static File getAgentJarFile() throws URISyntaxException {
        ProtectionDomain protectionDomain = AgentMain.class.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException(String.format("不能获取AgentBuddy.jar位置, protection domain = %s", protectionDomain));
        }
        URL location = codeSource.getLocation();
        if (location == null) {
            throw new IllegalStateException(String.format("不能获取AgentBuddy.jar位置, code source = %s", codeSource));
        }
        final File agentJar = new File(location.toURI());
        if (!agentJar.getName().endsWith(".jar")) {
            throw new IllegalStateException("AgentBuddy似乎不是一个jar文件: " + agentJar);
        }
        return agentJar.getAbsoluteFile();
    }
}
