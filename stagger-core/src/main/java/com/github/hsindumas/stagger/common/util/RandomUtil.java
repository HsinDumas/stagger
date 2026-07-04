package com.github.hsindumas.stagger.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random value helpers.
 */
public final class RandomUtil {

	private static final String ALPHA_NUMERIC = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private RandomUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static int randomInt(int min, int max) {
		if (max <= min) {
			return min;
		}
		return ThreadLocalRandom.current().nextInt(min, max);
	}

	public static int randomInt(int limit) {
		if (limit <= 0) {
			return 0;
		}
		return ThreadLocalRandom.current().nextInt(limit);
	}

	public static int randomInt() {
		return ThreadLocalRandom.current().nextInt();
	}

	public static double randomDouble(double min, double max) {
		if (max <= min) {
			return min;
		}
		return ThreadLocalRandom.current().nextDouble(min, max);
	}

	public static double randomDouble() {
		return ThreadLocalRandom.current().nextDouble();
	}

	public static String randomValueByType(String typeName) {
		String type = normalize(typeName);
		switch (type) {
			case "int":
			case "integer":
			case "short":
			case "byte":
				return String.valueOf(randomInt(1, 100));
			case "long":
				return String.valueOf(ThreadLocalRandom.current().nextLong(1, 10_000));
			case "double":
			case "float":
			case "bigdecimal":
			case "biginteger":
			case "number":
				return String.valueOf(randomDouble(1.0d, 100.0d));
			case "boolean":
				return String.valueOf(ThreadLocalRandom.current().nextBoolean());
			case "char":
			case "character":
				return "a";
			case "uuid":
				return UUID.randomUUID().toString();
			case "localdate":
				return LocalDate.now().toString();
			case "localdatetime":
			case "offsetdatetime":
			case "zoneddatetime":
			case "instant":
			case "date":
			case "timestamp":
				return DateTimeUtil.nowStrTime(DateTimeUtil.DATE_FORMAT_SECOND);
			case "string":
			default:
				return randomString(8);
		}
	}

	public static String generateDefaultValueByType(String typeName) {
		String type = normalize(typeName);
		switch (type) {
			case "int":
			case "integer":
			case "short":
			case "byte":
			case "long":
				return "0";
			case "double":
			case "float":
			case "bigdecimal":
			case "biginteger":
			case "number":
				return "0.0";
			case "boolean":
				return "false";
			case "char":
			case "character":
				return "a";
			case "uuid":
				return "00000000-0000-0000-0000-000000000000";
			case "localdate":
				return LocalDate.now().toString();
			case "localdatetime":
			case "offsetdatetime":
			case "zoneddatetime":
			case "instant":
			case "date":
			case "timestamp":
				return LocalDateTime.now()
					.format(java.time.format.DateTimeFormatter.ofPattern(DateTimeUtil.DATE_FORMAT_SECOND));
			case "string":
			default:
				return "string";
		}
	}

	private static String randomString(int length) {
		if (length <= 0) {
			return StringUtil.EMPTY;
		}
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int index = ThreadLocalRandom.current().nextInt(ALPHA_NUMERIC.length());
			builder.append(ALPHA_NUMERIC.charAt(index));
		}
		return builder.toString();
	}

	private static String normalize(String typeName) {
		if (StringUtil.isEmpty(typeName)) {
			return "string";
		}
		String type = typeName;
		int lastDot = type.lastIndexOf('.');
		if (lastDot >= 0 && lastDot + 1 < type.length()) {
			type = type.substring(lastDot + 1);
		}
		return type.toLowerCase(Locale.ROOT);
	}

}
