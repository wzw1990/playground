package cc.ikey.game.agent.test;

import java.net.URL;
import java.net.URLClassLoader;

public class JDKDelegateClassloader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public JDKDelegateClassloader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
}
