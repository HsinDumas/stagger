package com.github.hsindumas.stagger.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.hsindumas.stagger.utils.JsonUtil;
import org.junit.jupiter.api.Test;

/**
 * @author yu 2021/6/27.
 * @author HsinDumas
 */
public class JsonUtilTest {

    @Test
    public void toPrettyFormat() {
        String pretty = JsonUtil.toPrettyFormat(
                "{\"success\":true,\"message\":\"\",\"data\":\"\",\"code\":\"\",\"timestamp\":\"\",\"traceId\":\"\"}");
        assertTrue(pretty.contains("\n"));
        assertTrue(pretty.contains("\"success\":"));
        assertTrue(pretty.contains("\"traceId\":"));
    }
}
