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

/**
 * <p>
 * 主要代码从https://github.com/elastic/apm-agent-java项目而来
 * </p>
 * <p>
 * 本项目较原项目删减了大部分兼容性代码，例如兼容OSGi规范、兼容低于Java7以下版本字节码、兼容JDK9及更新版本的JDK等等。
 * </p>
 */
public class AgentMain {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(AgentMain.class);
    private static final String AGENT_ATTACH_KEY = "AgentBuddyAgent.attached";

    public static void agentmain(String args, Instrumentation inst) {
        entry(args, inst);
    }

    public static void premain(String args, Instrumentation inst) {
        entry(args, inst);
    }

    private synchronized static void entry(String args, Instrumentation inst) {
        if (Boolean.getBoolean(AGENT_ATTACH_KEY)) {
            return;
        }
        LOGGER.info("初始化Agent Buddy!");
        try {
            // 获取Agent Jar，以便后续进行类加载
            File agentJar = getAgentJarFile();
            URLClassLoader agentClassLoader = new AgentClassLoader(agentJar, getAgentClassLoaderParent());
            Class.forName("cc.ikey.playground.agentbuddy.AgentBuddy", true, agentClassLoader)
                    .getMethod("initialize", String.class, Instrumentation.class, File.class)
                    .invoke(null, args, inst, agentJar);
            System.setProperty(AGENT_ATTACH_KEY, Boolean.TRUE.toString());
            LOGGER.info("初始化Agent Buddy完毕!");
        } catch (Throwable e) {
            LOGGER.error("初始化Agent Buddy异常", e);
        }
    }

    /**
     * 获取AgentClassLoader父级ClassLoader，JDK9及更新版本对于这块有改动，详见{@see <a href="https://www.oracle.com/java/technologies/javase/9-relnotes.html"/>}
     */
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
            throw new IllegalStateException(String.format("无法获取agent jar位置, protection domain = %s", protectionDomain));
        }
        URL location = codeSource.getLocation();
        if (location == null) {
            throw new IllegalStateException(String.format("无法获取agent jar位置, code source = %s", codeSource));
        }
        final File agentJar = new File(location.toURI());
        if (!agentJar.getName().endsWith(".jar")) {
            throw new IllegalStateException("似乎不是一个jar文件: " + agentJar);
        }
        return agentJar.getAbsoluteFile();
    }
}
