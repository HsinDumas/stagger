package com.github.hsindumas.stagger.common.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * String helpers.
 */
public final class StringUtil {

    public static final String EMPTY = "";

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private StringUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public static String trimBlank(String value) {
        if (value == null) {
            return null;
        }
        return WHITESPACE_PATTERN.matcher(value).replaceAll(EMPTY);
    }

    public static String camelToUnderline(String value) {
        if (isEmpty(value)) {
            return value;
        }
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isUpperCase(current)) {
                if (i > 0) {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(current));
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    public static String firstToLowerCase(String value) {
        if (isEmpty(value)) {
            return value;
        }
        if (value.length() == 1) {
            return value.toLowerCase(Locale.ROOT);
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    public static String firstToUpperCase(String value) {
        if (isEmpty(value)) {
            return value;
        }
        if (value.length() == 1) {
            return value.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public static String removeQuotes(String value) {
        if (isEmpty(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }

    public static String removeDoubleQuotes(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\"", EMPTY);
    }
}
