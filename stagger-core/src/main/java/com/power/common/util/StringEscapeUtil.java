package com.power.common.util;

/**
 * Java string escape helpers.
 */
public final class StringEscapeUtil {

	private StringEscapeUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static String escapeJava(String input) {
		if (input == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder(input.length() + 16);
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			switch (ch) {
				case '\\':
					builder.append("\\\\");
					break;
				case '\b':
					builder.append("\\b");
					break;
				case '\n':
					builder.append("\\n");
					break;
				case '\t':
					builder.append("\\t");
					break;
				case '\f':
					builder.append("\\f");
					break;
				case '\r':
					builder.append("\\r");
					break;
				case '"':
					builder.append("\\\"");
					break;
				case '\'':
					builder.append("\\'");
					break;
				default:
					builder.append(ch);
					break;
			}
		}
		return builder.toString();
	}

	public static String escapeJava(String input, Boolean ignoreChinese) {
		return escapeJava(input);
	}

	public static String escapeJavaIgnoreChinese(String input) {
		return escapeJava(input);
	}

	public static String unescapeJava(String input) {
		if (input == null) {
			return null;
		}
		StringBuilder result = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			if (ch != '\\' || i + 1 >= input.length()) {
				result.append(ch);
				continue;
			}
			char next = input.charAt(++i);
			switch (next) {
				case 'b':
					result.append('\b');
					break;
				case 'n':
					result.append('\n');
					break;
				case 't':
					result.append('\t');
					break;
				case 'f':
					result.append('\f');
					break;
				case 'r':
					result.append('\r');
					break;
				case '"':
					result.append('"');
					break;
				case '\'':
					result.append('\'');
					break;
				case '\\':
					result.append('\\');
					break;
				case 'u':
					if (i + 4 < input.length()) {
						String hex = input.substring(i + 1, i + 5);
						try {
							result.append((char) Integer.parseInt(hex, 16));
							i += 4;
						}
						catch (NumberFormatException ex) {
							result.append("\\u").append(hex);
							i += 4;
						}
					}
					else {
						result.append("\\u");
					}
					break;
				default:
					result.append(next);
					break;
			}
		}
		return result.toString();
	}

}
