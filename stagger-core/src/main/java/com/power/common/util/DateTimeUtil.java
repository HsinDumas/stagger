package com.power.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * Date and time helpers.
 */
public final class DateTimeUtil {

	public static final String DATE_FORMAT_DAY = "yyyy-MM-dd";

	public static final String DATE_FORMAT_SECOND = "yyyy-MM-dd HH:mm:ss";

	public static final String DATE_FORMAT_ZONED_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ssXXX";

	private DateTimeUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static String dateToStr(Date date, String pattern) {
		if (date == null) {
			return StringUtil.EMPTY;
		}
		return dateToStr(date, pattern, Locale.getDefault());
	}

	public static String dateToStr(Date date, String pattern, Locale locale) {
		if (date == null) {
			return StringUtil.EMPTY;
		}
		ZoneId zoneId = ZoneId.systemDefault();
		LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), zoneId);
		return localDateTime.format(DateTimeFormatter.ofPattern(pattern, locale));
	}

	public static String nowStrTime() {
		return nowStrTime(DATE_FORMAT_SECOND);
	}

	public static String nowStrTime(String pattern) {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
	}

	public static String zonedDateTimeToStr(ZonedDateTime value, String pattern) {
		if (value == null) {
			return StringUtil.EMPTY;
		}
		return value.format(DateTimeFormatter.ofPattern(pattern));
	}

	public static String long2Str(Long timeMillis) {
		if (timeMillis == null) {
			return StringUtil.EMPTY;
		}
		return long2Str(timeMillis.longValue(), DATE_FORMAT_SECOND);
	}

	public static String long2Str(long timeMillis, String pattern) {
		LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault());
		return dateTime.format(DateTimeFormatter.ofPattern(pattern));
	}

}
