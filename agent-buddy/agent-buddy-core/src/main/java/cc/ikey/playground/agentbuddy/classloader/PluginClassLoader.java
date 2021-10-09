package cc.ikey.playground.agentbuddy.classloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 用于隔离各个Plugin之间的依赖，避免版本冲突等可能存在的影响。
 */
public class PluginClassLoader extends URLClassLoader {
    private final List<String> classNames;

    public PluginClassLoader(File pluginJar, ClassLoader agentClassLoader) throws IOException {
        super(new URL[]{pluginJar.toURI().toURL()}, agentClassLoader);
        classNames = Collections.unmodifiableList(scanForClasses(pluginJar));
        //TODO 判断是否包含SDK
//        if (classNames.contains(ElasticApmInstrumentation.class.getName())) {
//            throw new IllegalStateException("The plugin %s contains the plugin SDK. Please make sure the scope for the dependency apm-agent-plugin-sdk is set to provided.");
//        }
    }

    private List<String> scanForClasses(File pluginJar) throws IOException {
        List<String> tempClassNames = new ArrayList<>();
        try (JarFile jarFile = new JarFile(pluginJar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    tempClassNames.add(jarEntry.getName().replace('/', '.').substring(0, jarEntry.getName().length() - 6));
                }
            }
        }
        return tempClassNames;
    }

    public List<String> getClassNames() {
        return classNames;
    }
}
