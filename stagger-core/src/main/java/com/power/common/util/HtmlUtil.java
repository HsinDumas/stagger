package com.power.common.util;

/**
 * HTML helpers.
 */
public final class HtmlUtil {

	private HtmlUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static String lineBreaksToBr(String input) {
		if (input == null) {
			return null;
		}
		return input.replace("\r\n", "<br/>").replace("\n", "<br/>").replace("\r", "<br/>");
	}

}
