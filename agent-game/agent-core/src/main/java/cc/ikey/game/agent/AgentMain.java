package cc.ikey.game.agent;

import cc.ikey.game.agent.logging.AgentLogger;

import java.lang.instrument.Instrumentation;

public class AgentMain {
    private static AgentLogger LOGGER = AgentLogger.getLogger(AgentMain.class);

    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }

    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer(new MyTransformer());
        LOGGER.info("Agent初始化完毕");
    }
}
