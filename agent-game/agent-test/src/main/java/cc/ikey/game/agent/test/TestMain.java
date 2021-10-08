package cc.ikey.game.agent.test;

import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class TestMain {
    private Test1 test1 = null;
    private static final JDKDelegateClassloader jdkClassLoader;

    static {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        ClassLoader extClassLoader = systemClassLoader;
        while (extClassLoader.getParent() != null) {
            extClassLoader = extClassLoader.getParent();
        }
        List<URL> jdkUrls = new ArrayList<>();
        String javaHome = System.getProperty("java.home").replace(File.separator + "jre", "");
        URL[] urls = ((URLClassLoader) systemClassLoader).getURLs();
        for (URL url : urls) {
            if (url.getPath().startsWith(javaHome)) {
                jdkUrls.add(url);
            }
        }

        jdkClassLoader = new JDKDelegateClassloader(jdkUrls.toArray(new URL[0]), extClassLoader);
    }

    public static void main(String[] args) throws Exception {
        Thread.currentThread().setContextClassLoader(jdkClassLoader);
        TestMain testMain = new TestMain();
        Field aSwitch = TestMain.class.getField("aSwitch");
        boolean val = aSwitch.getBoolean(testMain);
        Assertions.assertFalse(val);
    }
}
