package cc.ikey.playground.agentbuddy;

import cc.ikey.playground.agentbuddy.logging.AgentLogger;

import java.io.File;
import java.lang.instrument.Instrumentation;

public class AgentBuddy {
    private static AgentLogger LOGGER = AgentLogger.getLogger(AgentBuddy.class);

    public static void initialize(final String agentArguments, final Instrumentation instrumentation, final File agentJarFile) {
        LOGGER.info("玛卡巴卡");
    }
}
