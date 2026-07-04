/*
 * Copyright (C) 2018-2026 stagger
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.github.hsindumas.stagger.utils;

import com.github.hsindumas.stagger.constants.DocValidatorAnnotationEnum;
import com.github.hsindumas.stagger.constants.JSRAnnotationConstants;
import com.github.hsindumas.stagger.constants.JSRAnnotationPropConstants;
import com.mifmif.common.regex.Generex;
import com.github.hsindumas.stagger.common.util.DateTimeUtil;
import com.github.hsindumas.stagger.common.util.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Resolves sample values using layered strategy: explicit mock tag (handled upstream),
 * then validation constraints, then field-name dictionary, then type default fallback.
 *
 * @author HsinDumas
 */
public final class MockValueResolver {

	private static final String DEFAULT_FIELD_VALUES_RESOURCE = "mock/field-values.properties";

	private static final String FIELD_VALUES_OVERRIDE_PATH = "stagger.mock.field.values.path";

	private volatile Map<String, String> fieldValueTemplates = Collections.emptyMap();

	private volatile String loadedOverridePath = null;

	MockValueResolver() {
		reloadTemplatesIfNeeded();
	}

	String resolveByFieldName(String key) {
		reloadTemplatesIfNeeded();
		for (Map.Entry<String, String> entry : fieldValueTemplates.entrySet()) {
			if (key.contains(entry.getKey())) {
				String value = resolveTemplate(entry.getValue());
				if (StringUtil.isNotEmpty(value)) {
					return value;
				}
			}
		}
		return null;
	}

	String resolveByConstraints(String typeName, List<?> annotations) {
		if (annotations == null || annotations.isEmpty()) {
			return null;
		}
		ConstraintProfile profile = buildConstraintProfile(annotations);
		String normalizedType = normalizeType(typeName);

		if (profile.email) {
			return MockValues.email();
		}

		if (StringUtil.isNotEmpty(profile.patternRegex)) {
			String patternValue = generateByPattern(profile.patternRegex);
			if (StringUtil.isNotEmpty(patternValue)) {
				return patternValue;
			}
		}

		if (isTemporalType(normalizedType) && profile.hasTemporalConstraint()) {
			return generateTemporalValue(normalizedType, profile);
		}

		if (isNumericType(normalizedType) && profile.hasNumericConstraint()) {
			return generateNumericValue(normalizedType, profile);
		}

		if (isStringType(normalizedType) && profile.hasStringConstraint()) {
			return generateStringValue(profile);
		}

		return null;
	}

	private void reloadTemplatesIfNeeded() {
		String overridePath = StringUtil.trimBlank(System.getProperty(FIELD_VALUES_OVERRIDE_PATH));
		if (!fieldValueTemplates.isEmpty() && Objects.equals(overridePath, loadedOverridePath)) {
			return;
		}
		synchronized (this) {
			if (!fieldValueTemplates.isEmpty() && Objects.equals(overridePath, loadedOverridePath)) {
				return;
			}
			Map<String, String> loaded = loadFieldValueTemplates(overridePath);
			fieldValueTemplates = loaded;
			loadedOverridePath = overridePath;
		}
	}

	private Map<String, String> loadFieldValueTemplates(String overridePath) {
		LinkedHashMap<String, String> templates = new LinkedHashMap<>(64);
		loadPropertiesFromClasspath(DEFAULT_FIELD_VALUES_RESOURCE, templates);
		if (StringUtil.isNotEmpty(overridePath)) {
			loadPropertiesFromFile(overridePath, templates);
		}
		return templates;
	}

	private void loadPropertiesFromClasspath(String resource, Map<String, String> target) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			classLoader = MockValueResolver.class.getClassLoader();
		}
		if (classLoader == null) {
			return;
		}
		try (InputStream inputStream = classLoader.getResourceAsStream(resource)) {
			if (inputStream == null) {
				return;
			}
			Properties properties = new Properties();
			try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
				properties.load(reader);
			}
			for (String key : properties.stringPropertyNames()) {
				target.put(key, properties.getProperty(key));
			}
		}
		catch (IOException ignored) {
			// Keep defaults when externalized dictionary cannot be loaded.
		}
	}

	private void loadPropertiesFromFile(String filePath, Map<String, String> target) {
		Path path = Paths.get(filePath);
		if (!Files.isRegularFile(path)) {
			return;
		}
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			Properties properties = new Properties();
			properties.load(reader);
			for (String key : properties.stringPropertyNames()) {
				target.put(key, properties.getProperty(key));
			}
		}
		catch (IOException ignored) {
			// Ignore malformed override and keep classpath defaults.
		}
	}

	private String resolveTemplate(String template) {
		if (StringUtil.isEmpty(template)) {
			return template;
		}
		String trimmed = template.trim();
		if (!trimmed.startsWith("${") || !trimmed.endsWith("}")) {
			return trimmed;
		}
		String token = trimmed.substring(2, trimmed.length() - 1);
		if (token.startsWith("mock.int.")) {
			String[] values = token.split("\\.");
			if (values.length == 4) {
				Integer min = parseInteger(values[2]);
				Integer max = parseInteger(values[3]);
				if (min != null && max != null) {
					return String.valueOf(MockValues.randomInt(min, max));
				}
			}
		}
		switch (token) {
			case "mock.uuid":
				return java.util.UUID.randomUUID().toString();
			case "mock.username":
				return MockValues.username();
			case "mock.author":
				return MockValues.author();
			case "mock.email":
				return MockValues.email();
			case "mock.domain":
				return MockValues.domain();
			case "mock.phone":
				return MockValues.phone();
			case "mock.telephone":
				return MockValues.telephone();
			case "mock.address":
				return MockValues.address();
			case "mock.ipv4":
				return MockValues.ipv4();
			case "mock.ipv6":
				return MockValues.ipv6();
			case "mock.company":
				return MockValues.company();
			case "mock.url":
				return MockValues.url();
			case "mock.semver":
				return MockValues.semver();
			case "mock.now.millis":
				return String.valueOf(System.currentTimeMillis());
			case "mock.now.datetime":
				return DateTimeUtil.dateToStr(new Date(), DateTimeUtil.DATE_FORMAT_SECOND);
			case "mock.now.date":
				return DateTimeUtil.dateToStr(new Date(), DateTimeUtil.DATE_FORMAT_DAY);
			case "mock.now.time":
				return LocalDateTime.now().toLocalTime().withNano(0).toString();
			case "mock.message":
				return MockValues.message();
			case "mock.idcard":
				return MockValues.idCard();
			default:
				return trimmed;
		}
	}

	private ConstraintProfile buildConstraintProfile(List<?> annotations) {
		ConstraintProfile profile = new ConstraintProfile();
		for (Object annotation : annotations) {
			String annotationName = resolveAnnotationName(annotation);
			if (StringUtil.isEmpty(annotationName)) {
				continue;
			}
			Map<String, String> properties = resolveAnnotationProperties(annotationName, annotation);
			applyConstraint(annotationName, properties, profile);
		}
		return profile;
	}

	private String resolveAnnotationName(Object annotation) {
		String name = DocUtil.getAnnotationTypeValue(annotation);
		if (StringUtil.isNotEmpty(name)) {
			return name;
		}
		return DocUtil.getAnnotationTypeSimpleName(annotation);
	}

	private Map<String, String> resolveAnnotationProperties(String annotationName, Object annotation) {
		Map<String, String> values = new LinkedHashMap<>(DocValidatorAnnotationEnum.getDefaults(annotationName));
		Map<String, Object> annotationPropertyMap = DocUtil.getAnnotationPropertyMap(annotation);
		for (Map.Entry<String, Object> entry : annotationPropertyMap.entrySet()) {
			String resolved = StringUtil.removeDoubleQuotes(DocUtil.resolveAnnotationValue(entry.getValue()));
			values.put(entry.getKey(), resolved);
		}
		Object valueProp = DocUtil.getAnnotationProperty(annotation, JSRAnnotationPropConstants.VALUE);
		if (Objects.nonNull(valueProp)) {
			String resolved = StringUtil.removeDoubleQuotes(DocUtil.resolveAnnotationValue(valueProp));
			values.put(JSRAnnotationPropConstants.VALUE, resolved);
		}
		return values;
	}

	private void applyConstraint(String annotationName, Map<String, String> properties, ConstraintProfile profile) {
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.EMAIL)) {
			profile.email = true;
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.PATTERN)) {
			profile.patternRegex = firstNotEmpty(properties.get("regexp"),
					properties.get(JSRAnnotationPropConstants.VALUE));
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.SIZE)
				|| equalsAnnotation(annotationName, JSRAnnotationConstants.LENGTH)
				|| equalsAnnotation(annotationName, JSRAnnotationConstants.RANGE)) {
			profile.minLength = max(profile.minLength, parseInteger(properties.get("min")));
			profile.maxLength = min(profile.maxLength, parseInteger(properties.get("max")));
			profile.minNumeric = max(profile.minNumeric, parseBigDecimal(properties.get("min")));
			profile.maxNumeric = min(profile.maxNumeric, parseBigDecimal(properties.get("max")));
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.MIN)) {
			profile.minNumeric = max(profile.minNumeric,
					parseBigDecimal(properties.get(JSRAnnotationPropConstants.VALUE)));
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.MAX)) {
			profile.maxNumeric = min(profile.maxNumeric,
					parseBigDecimal(properties.get(JSRAnnotationPropConstants.VALUE)));
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.DECIMAL_MIN)) {
			BigDecimal minValue = parseBigDecimal(properties.get(JSRAnnotationPropConstants.VALUE));
			if (Boolean.FALSE.toString().equalsIgnoreCase(properties.get("inclusive")) && minValue != null) {
				minValue = minValue.add(new BigDecimal("0.01"));
			}
			profile.minNumeric = max(profile.minNumeric, minValue);
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.DECIMAL_MAX)) {
			BigDecimal maxValue = parseBigDecimal(properties.get(JSRAnnotationPropConstants.VALUE));
			if (Boolean.FALSE.toString().equalsIgnoreCase(properties.get("inclusive")) && maxValue != null) {
				maxValue = maxValue.subtract(new BigDecimal("0.01"));
			}
			profile.maxNumeric = min(profile.maxNumeric, maxValue);
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.POSITIVE)) {
			profile.positive = true;
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.POSITIVE_OR_ZERO)) {
			profile.positiveOrZero = true;
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.NEGATIVE)) {
			profile.negative = true;
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.NEGATIVE_OR_ZERO)) {
			profile.negativeOrZero = true;
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.NOT_BLANK)) {
			profile.notBlank = true;
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.NOT_EMPTY)) {
			profile.notEmpty = true;
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.PAST)) {
			profile.past = true;
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.PAST_OR_PRESENT)) {
			profile.pastOrPresent = true;
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.FUTURE)) {
			profile.future = true;
			return;
		}
		if (equalsAnnotation(annotationName, JSRAnnotationConstants.FUTURE_OR_PRESENT)) {
			profile.futureOrPresent = true;
		}
	}

	private String generateByPattern(String patternRegex) {
		try {
			return new Generex(patternRegex).random();
		}
		catch (RuntimeException ignored) {
			return patternRegex;
		}
	}

	private String generateStringValue(ConstraintProfile profile) {
		int min = profile.minLength != null ? Math.max(0, profile.minLength)
				: (profile.notBlank || profile.notEmpty ? 1 : 6);
		int max = profile.maxLength != null ? Math.max(min, profile.maxLength) : Math.max(min, 12);
		if (max == 0) {
			return "";
		}
		int length = MockValues.randomInt(min, max);
		if (length <= 0) {
			length = 1;
		}
		String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int index = ThreadLocalRandom.current().nextInt(alphabet.length());
			builder.append(alphabet.charAt(index));
		}
		return builder.toString();
	}

	private String generateNumericValue(String normalizedType, ConstraintProfile profile) {
		BigDecimal min = profile.minNumeric;
		BigDecimal max = profile.maxNumeric;

		if (profile.positive) {
			min = max(min, BigDecimal.ONE);
		}
		if (profile.positiveOrZero) {
			min = max(min, BigDecimal.ZERO);
		}
		if (profile.negative) {
			max = min(max, BigDecimal.ONE.negate());
		}
		if (profile.negativeOrZero) {
			max = min(max, BigDecimal.ZERO);
		}

		if (isIntegerType(normalizedType)) {
			long low = min != null ? min.setScale(0, RoundingMode.CEILING).longValue() : defaultIntegerLow(profile);
			long high = max != null ? max.setScale(0, RoundingMode.FLOOR).longValue() : defaultIntegerHigh(profile);
			if (low > high) {
				long mid = low;
				low = high;
				high = mid;
			}
			if (low == high) {
				return String.valueOf(low);
			}
			return String.valueOf(ThreadLocalRandom.current().nextLong(low, high + 1));
		}

		BigDecimal low = min != null ? min : defaultDecimalLow(profile);
		BigDecimal high = max != null ? max : defaultDecimalHigh(profile);
		if (low.compareTo(high) > 0) {
			BigDecimal temp = low;
			low = high;
			high = temp;
		}
		if (low.compareTo(high) == 0) {
			return low.stripTrailingZeros().toPlainString();
		}
		double value = ThreadLocalRandom.current().nextDouble(low.doubleValue(), Math.nextUp(high.doubleValue()));
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}

	private String generateTemporalValue(String normalizedType, ConstraintProfile profile) {
		LocalDateTime now = LocalDateTime.now();
		if (profile.future) {
			now = now.plusDays(1);
		}
		else if (profile.past) {
			now = now.minusDays(1);
		}

		if ("localdate".equals(normalizedType)) {
			return now.toLocalDate().toString();
		}
		if ("localtime".equals(normalizedType)) {
			return now.toLocalTime().withNano(0).toString();
		}
		if ("localdatetime".equals(normalizedType)) {
			return now.withNano(0).toString().replace('T', ' ');
		}
		if ("long".equals(normalizedType) || "long[]".equals(normalizedType)) {
			return String.valueOf(now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
		}
		Date date = java.sql.Timestamp.valueOf(now);
		return DateTimeUtil.dateToStr(date, DateTimeUtil.DATE_FORMAT_SECOND);
	}

	private boolean isStringType(String normalizedType) {
		return "string".equals(normalizedType) || "char".equals(normalizedType) || "character".equals(normalizedType);
	}

	private boolean isNumericType(String normalizedType) {
		return isIntegerType(normalizedType) || "double".equals(normalizedType) || "float".equals(normalizedType)
				|| "number".equals(normalizedType) || "bigdecimal".equals(normalizedType);
	}

	private boolean isIntegerType(String normalizedType) {
		return "int".equals(normalizedType) || "integer".equals(normalizedType) || "long".equals(normalizedType)
				|| "short".equals(normalizedType) || "byte".equals(normalizedType)
				|| "biginteger".equals(normalizedType);
	}

	private boolean isTemporalType(String normalizedType) {
		return "date".equals(normalizedType) || "localdate".equals(normalizedType)
				|| "localdatetime".equals(normalizedType) || "localtime".equals(normalizedType)
				|| "instant".equals(normalizedType) || "offsetdatetime".equals(normalizedType)
				|| "zoneddatetime".equals(normalizedType) || "timestamp".equals(normalizedType)
				|| "long".equals(normalizedType) || "year".equals(normalizedType) || "yearmonth".equals(normalizedType)
				|| "monthday".equals(normalizedType);
	}

	private String normalizeType(String typeName) {
		if (StringUtil.isEmpty(typeName)) {
			return "";
		}
		String normalized = typeName;
		int genericIndex = normalized.indexOf('<');
		if (genericIndex > -1) {
			normalized = normalized.substring(0, genericIndex);
		}
		if (normalized.contains(".")) {
			normalized = normalized.substring(normalized.lastIndexOf('.') + 1);
		}
		return normalized.toLowerCase(Locale.ROOT);
	}

	private boolean equalsAnnotation(String name, String target) {
		return target.equalsIgnoreCase(name);
	}

	private String firstNotEmpty(String first, String second) {
		return StringUtil.isNotEmpty(first) ? first : second;
	}

	private Integer max(Integer first, Integer second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		return Math.max(first, second);
	}

	private BigDecimal max(BigDecimal first, BigDecimal second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		return first.max(second);
	}

	private Integer min(Integer first, Integer second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		return Math.min(first, second);
	}

	private BigDecimal min(BigDecimal first, BigDecimal second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		return first.min(second);
	}

	private Integer parseInteger(String value) {
		if (StringUtil.isEmpty(value)) {
			return null;
		}
		try {
			return Integer.parseInt(value.trim());
		}
		catch (NumberFormatException ignored) {
			return null;
		}
	}

	private BigDecimal parseBigDecimal(String value) {
		if (StringUtil.isEmpty(value)) {
			return null;
		}
		try {
			return new BigDecimal(value.trim());
		}
		catch (NumberFormatException ignored) {
			return null;
		}
	}

	private long defaultIntegerLow(ConstraintProfile profile) {
		if (profile.negative || profile.negativeOrZero) {
			return -100;
		}
		if (profile.positive || profile.positiveOrZero) {
			return profile.positive ? 1 : 0;
		}
		return 0;
	}

	private long defaultIntegerHigh(ConstraintProfile profile) {
		if (profile.negative) {
			return -1;
		}
		if (profile.negativeOrZero) {
			return 0;
		}
		return 100;
	}

	private BigDecimal defaultDecimalLow(ConstraintProfile profile) {
		if (profile.negative || profile.negativeOrZero) {
			return new BigDecimal("-999.99");
		}
		if (profile.positive) {
			return new BigDecimal("0.01");
		}
		return BigDecimal.ZERO;
	}

	private BigDecimal defaultDecimalHigh(ConstraintProfile profile) {
		if (profile.negative) {
			return new BigDecimal("-0.01");
		}
		if (profile.negativeOrZero) {
			return BigDecimal.ZERO;
		}
		return new BigDecimal("999.99");
	}

	private static final class ConstraintProfile {

		private boolean email;

		private boolean positive;

		private boolean positiveOrZero;

		private boolean negative;

		private boolean negativeOrZero;

		private boolean notBlank;

		private boolean notEmpty;

		private boolean past;

		private boolean pastOrPresent;

		private boolean future;

		private boolean futureOrPresent;

		private Integer minLength;

		private Integer maxLength;

		private BigDecimal minNumeric;

		private BigDecimal maxNumeric;

		private String patternRegex;

		private boolean hasStringConstraint() {
			return minLength != null || maxLength != null || notBlank || notEmpty;
		}

		private boolean hasNumericConstraint() {
			return minNumeric != null || maxNumeric != null || positive || positiveOrZero || negative || negativeOrZero;
		}

		private boolean hasTemporalConstraint() {
			return past || pastOrPresent || future || futureOrPresent;
		}

	}

}
