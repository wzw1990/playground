package cc.ikey.game.agent;

import cc.ikey.game.agent.logging.AgentLogger;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class MyTransformer implements ClassFileTransformer {
    private static AgentLogger LOGGER = AgentLogger.getLogger(AgentMain.class);

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null || !className.startsWith("cc")) {
            return classfileBuffer;
        }
        LOGGER.info("准备加载{}", className);
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
            CtField newField = CtField.make("public boolean aSwitch = false;", ctClass);
            ctClass.addField(newField);
            return ctClass.toBytecode();
        } catch (Throwable e) {
            LOGGER.error("创造Class出错了", e);
            return null;
        }
    }
}
