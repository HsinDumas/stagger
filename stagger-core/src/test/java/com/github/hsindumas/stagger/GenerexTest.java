package com.github.hsindumas.stagger;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mifmif.common.regex.Generex;
import org.junit.jupiter.api.Test;

/**
 * @author yu3.sun on 2022/10/15
 * @author HsinDumas
 */
public class GenerexTest {

    @Test
    public void testGenerateValue() {
        Generex generex = new Generex("[a-zA-Z0-9]{3}");
        // Generate random String
        String randomStr = generex.random();
        assertTrue(randomStr.matches("[a-zA-Z0-9]{3}"));
    }
}
