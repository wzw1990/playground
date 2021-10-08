package cc.ikey.game.agent.test;

import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Field;

public class TestMain {
    private Test1 test1 = null;

    public static void main(String[] args) throws Exception {
        TestMain testMain = new TestMain();
        Field aSwitch = TestMain.class.getField("aSwitch");
        boolean val = aSwitch.getBoolean(testMain);
        Assertions.assertFalse(val);
    }
}
