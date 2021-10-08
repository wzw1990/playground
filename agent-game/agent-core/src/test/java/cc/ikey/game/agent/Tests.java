package cc.ikey.game.agent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

public class Tests {
    @Test
    public void notExistsField() throws Exception {
        Field aSwitch = Tests.class.getField("aSwitch");
        boolean val = aSwitch.getBoolean(this);
        Assertions.assertFalse(val);
    }
}
