package cc.ikey.jdk.patch.classloader;

import sun.misc.Resource;
import sun.misc.URLClassPath;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.jar.Manifest;

public class JDKDelegateClassloader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public JDKDelegateClassloader(URL[] urls) {
        super(urls, null);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        System.out.println("玛卡巴卡");
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                long t0 = System.nanoTime();
                try {
                    Method findBootstrapClassOrNullMethod = ClassLoader.class.getDeclaredMethod("findBootstrapClassOrNull", String.class);
                    findBootstrapClassOrNullMethod.setAccessible(true);
                    findBootstrapClassOrNullMethod.invoke(this, name);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new ClassNotFoundException();
                }
                // If still not found, then invoke findClass in order
                // to find the class.
                long t1 = System.nanoTime();
                c = findClass(name);

                // this is the defining class loader; record the stats
                sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                sun.misc.PerfCounter.getFindClasses().increment();
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        System.out.println("唔西迪西" + name);
        String path = name.replace('.', '/').concat(".class");
        Resource res = new URLClassPath(getURLs()).getResource(path, false);
        System.out.println("xxx:" + res);
        if (res != null) {
            try {
                long t0 = System.nanoTime();
                int i = name.lastIndexOf('.');
                URL url = res.getCodeSourceURL();
                if (i != -1) {
                    String pkgname = name.substring(0, i);
                    // Check if package already loaded.
                    Manifest man = res.getManifest();
//                    definePackageInternal(pkgname, man, url);
                }
                // Now read the class bytes and define the class
                java.nio.ByteBuffer bb = res.getByteBuffer();
                if (bb != null) {
                    // Use (direct) ByteBuffer:
                    CodeSigner[] signers = res.getCodeSigners();
                    CodeSource cs = new CodeSource(url, signers);
                    sun.misc.PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
                    return defineClass(name, bb, cs);
                } else {
                    byte[] b = res.getBytes();
                    // must read certificates AFTER reading bytes.
                    CodeSigner[] signers = res.getCodeSigners();
                    CodeSource cs = new CodeSource(url, signers);
                    sun.misc.PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
                    return defineClass(name, b, 0, b.length, cs);
                }
            } catch (Exception e) {
                throw new ClassNotFoundException(name, e);
            }
        } else {
            return null;
        }
    }
}
