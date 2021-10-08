package cc.ikey.game.agent.classloader;

import java.net.URL;
import java.net.URLClassLoader;

public class JDKDelegateClassloader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public JDKDelegateClassloader(URL[] urls) {
        super(urls, null);
    }
}
