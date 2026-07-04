package com.github.hsindumas.stagger.common.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * ID card helper.
 */
public final class IDCardUtil {

	private IDCardUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static String getIdCard() {
		int areaCode = 110100 + ThreadLocalRandom.current().nextInt(900);
		int year = 1980 + ThreadLocalRandom.current().nextInt(30);
		int month = 1 + ThreadLocalRandom.current().nextInt(12);
		int day = 1 + ThreadLocalRandom.current().nextInt(28);
		int sequence = 100 + ThreadLocalRandom.current().nextInt(900);
		return String.format("%06d%04d%02d%02d%03dX", areaCode, year, month, day, sequence);
	}

}
