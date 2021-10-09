package cc.ikey.playground.agentbuddy.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 用于隔离各个Plugin之间的依赖，避免版本冲突等可能存在的影响。
 */
public class PluginClassLoader extends URLClassLoader {
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
}
