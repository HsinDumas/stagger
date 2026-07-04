package com.github.hsindumas.stagger.common.util;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regex helpers.
 */
public final class RegexUtil {

	private RegexUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static boolean isMatches(Collection<String> patterns, String content) {
		if (CollectionUtil.isEmpty(patterns) || StringUtil.isEmpty(content)) {
			return false;
		}
		for (String pattern : patterns) {
			if (isMatches(pattern, content)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isMatches(String pattern, String content) {
		if (StringUtil.isEmpty(pattern) || StringUtil.isEmpty(content)) {
			return false;
		}
		try {
			return Pattern.compile(pattern).matcher(content).matches();
		}
		catch (PatternSyntaxException ex) {
			return false;
		}
	}

}
