package cc.ikey.playground.simpleapp;

import cc.ikey.playground.agentbuddy.sdk.MethodInstrumentation;
import cc.ikey.playground.agentbuddy.sdk.advice.AssignTo;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class ChangeAPIResponseInstrumentation extends MethodInstrumentation {

    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        return nameStartsWith("cc.ikey.playground.simpleapp")
                .and(not(isInterface()))
                .and(declaresMethod(getMethodMatcher()))
                .and(hasSuperType(named("cc.ikey.playground.simpleapp.SimpleAppApplication")));
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return named("getUUID")
                .and(takesArguments(0))
                .and(returns(String.class));
    }

    @Override
    public Collection<String> getInstrumentationGroupNames() {
        return Collections.singletonList("test");
    }

    public static class AdviceClass {
        @AssignTo.Return(typing = Assigner.Typing.DYNAMIC)
        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class, inline = false)
        public static String afterExecute(@Advice.Return String response,
                                          @Advice.Thrown Throwable t) throws IOException {
            return "玛卡巴卡";
        }
    }
}
