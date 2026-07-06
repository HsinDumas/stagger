package com.github.hsindumas.stagger.common.util;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Path pattern helper.
 */
public final class PathUtil {

    private PathUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean matches(String lookupPath, List<String> includePatterns, List<String> excludePatterns) {
        if (StringUtil.isEmpty(lookupPath)) {
            return false;
        }

        boolean included = true;
        if (CollectionUtil.isNotEmpty(includePatterns)) {
            included = includePatterns.stream()
                    .filter(StringUtil::isNotEmpty)
                    .map(StringUtil::trim)
                    .anyMatch(pattern -> matchesSingle(lookupPath, pattern));
        }

        if (!included) {
            return false;
        }

        if (CollectionUtil.isNotEmpty(excludePatterns)) {
            boolean excluded = excludePatterns.stream()
                    .filter(StringUtil::isNotEmpty)
                    .map(StringUtil::trim)
                    .anyMatch(pattern -> matchesSingle(lookupPath, pattern));
            return !excluded;
        }

        return true;
    }

    public static boolean matches(String lookupPath, List<String> includePatterns) {
        return matches(lookupPath, includePatterns, null);
    }

    private static boolean matchesSingle(String path, String pattern) {
        if (StringUtil.isEmpty(pattern)) {
            return false;
        }
        String regex = antPatternToRegex(pattern);
        return Pattern.compile(regex).matcher(path).matches();
    }

    private static String antPatternToRegex(String pattern) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char current = pattern.charAt(i);
            if (current == '*') {
                boolean isDoubleStar = i + 1 < pattern.length() && pattern.charAt(i + 1) == '*';
                if (isDoubleStar) {
                    regex.append(".*");
                    i++;
                } else {
                    regex.append("[^/]*");
                }
                continue;
            }
            if (current == '?') {
                regex.append('.');
                continue;
            }
            if (".[]{}()+-^$|\\".indexOf(current) >= 0) {
                regex.append('\\');
            }
            regex.append(current);
        }
        regex.append('$');
        return regex.toString();
    }
}
