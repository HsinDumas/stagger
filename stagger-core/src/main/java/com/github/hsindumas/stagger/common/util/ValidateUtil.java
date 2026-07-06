package com.github.hsindumas.stagger.common.util;

import java.util.regex.Pattern;

/**
 * Validation helpers.
 */
public final class ValidateUtil {

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");

    private ValidateUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean validate(String value, String regex) {
        if (StringUtil.isEmpty(value) || StringUtil.isEmpty(regex)) {
            return false;
        }
        return Pattern.compile(regex).matcher(value).matches();
    }

    public static boolean isContainsChinese(String value) {
        if (StringUtil.isEmpty(value)) {
            return false;
        }
        return CHINESE_PATTERN.matcher(value).find();
    }

    public static boolean isNonNegativeInteger(String value) {
        if (StringUtil.isEmpty(value)) {
            return false;
        }
        return value.matches("^(0|[1-9]\\d*)$");
    }

    public static boolean isPositiveInteger(String value) {
        if (StringUtil.isEmpty(value)) {
            return false;
        }
        return value.matches("^[1-9]\\d*$");
    }
}
