package cc.ikey.playground.classloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 该类的作用请参考{@link cc.ikey.playground.agentbuddy.bci.IndyBootstrap}的注释
 */
public class AgentClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public AgentClassLoader(File jar, ClassLoader parent) throws IOException {
        super(new URL[]{jar.toURI().toURL()}, parent);
    }

}
