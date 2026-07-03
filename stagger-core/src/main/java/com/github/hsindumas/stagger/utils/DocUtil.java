/*
 * Copyright (C) 2018-2025 stagger
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

import com.github.hsindumas.stagger.builder.WordDocBuilder;
import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.DocAnnotationConstants;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.constants.DocTags;
import com.github.hsindumas.stagger.constants.JAXRSAnnotations;
import com.github.hsindumas.stagger.constants.JakartaJaxrsAnnotations;
import com.github.hsindumas.stagger.constants.JavaTypeConstants;
import com.github.hsindumas.stagger.constants.MediaType;
import com.github.hsindumas.stagger.extension.dict.DictionaryValuesResolver;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiDataDictionary;
import com.github.hsindumas.stagger.model.ApiDocDict;
import com.github.hsindumas.stagger.model.ApiErrorCode;
import com.github.hsindumas.stagger.model.ApiErrorCodeDictionary;
import com.github.hsindumas.stagger.model.ApiReqParam;
import com.github.hsindumas.stagger.model.DataDict;
import com.github.hsindumas.stagger.model.DocJavaField;
import com.github.hsindumas.stagger.model.FormData;
import com.github.hsindumas.stagger.model.SystemPlaceholders;
import com.github.hsindumas.stagger.model.request.RequestMapping;
import com.github.hsindumas.stagger.source.SourceClass;
import com.github.hsindumas.stagger.source.SourceDocletTag;
import com.mifmif.common.regex.Generex;
import com.power.common.util.CollectionUtil;
import com.power.common.util.DateTimeUtil;
import com.power.common.util.EnumUtil;
import com.power.common.util.IDCardUtil;
import com.power.common.util.RandomUtil;
import com.power.common.util.StringUtil;
import com.github.hsindumas.stagger.helper.JavaProjectBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Description: DocUtil
 *
 * @author yu 2018/06/11.
 * @author HsinDumas
 */
public class DocUtil {

	/**
	 * private constructor
	 */
	private DocUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * logger
	 */
	private static final Logger logger = Logger.getLogger(DocUtil.class.getName());

	/**
	 * Constraint and field dictionary resolver for mock values.
	 */
	private static final MockValueResolver MOCK_VALUE_RESOLVER = new MockValueResolver();

	/**
	 * This map contains the default JSON format patterns for various date and time types.
	 * These patterns are used when the @JsonFormat annotation is applied to fields of
	 * these types without specifying a pattern. If no pattern is configured, these
	 * default patterns will be used. <pre>
	 * {@code
	 * public class JsonFormatExample  {
	 *
	 *     // "calendarStringNoPattern": "2024-06-29T04:55:13.479+00:00"
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private Calendar calendarStringNoPattern;
	 *
	 *     // "dateStringNoPattern": "2024-06-29T04:55:13.479+00:00"
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private Date dateStringNoPattern;
	 *
	 *     // "localDateTimeStringNoPattern": "2024-06-29T12:55:13.4799336"
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private LocalDateTime localDateTimeStringNoPattern;
	 *
	 *     // "localDateStringNoPattern": "2024-06-29"
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private LocalDate localDateStringNoPattern;
	 *
	 *     // "localTimeStringNoPattern": "12:55:13.4799336"
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private LocalTime localTimeStringNoPattern;
	 *
	 *     // "zonedDateTimeStringNoPattern": "2024-06-29T12:55:13.4799336+08:00"
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private ZonedDateTime zonedDateTimeStringNoPattern;
	 *
	 *     // "offsetDateTimeStringNoPattern": "2024-06-30T14:28:55.7346858+08:00"
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private OffsetDateTime offsetDateTimeStringNoPattern;
	 *
	 *     // "yearStringNoPattern": "2024"
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private Year yearStringNoPattern;
	 *
	 *     // "yearMonthStringNoPattern": "2024-06"
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private YearMonth yearMonthStringNoPattern;
	 *
	 *     // "monthDayStringNoPattern": "--06-29",
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private MonthDay monthDayStringNoPattern;
	 *
	 *     // "instantStringNoPattern": "2024-06-29T04:55:13.479933600Z"
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private Instant instantStringNoPattern;
	 *
	 *     // "offsetTimeStringNoPattern": "20:10:37.334190400+08:00"
	 *     &#64;JsonFormat(shape = JsonFormat.Shape.STRING)
	 *     private OffsetTime offsetTimeStringNoPattern;
	 * }
	 * }
	 * </pre>
	 */
	private static final Map<String, String> DEFAULT_JSON_FORMAT_PATTERNS = new LinkedHashMap<>();

	static {

		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_UTIL_CALENDAR_FULLY, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_UTIL_DATE_FULLY, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_TIME_LOCAL_DATE_TIME_FULLY,
				"yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_TIME_LOCAL_DATE_FULLY, "yyyy-MM-dd");
		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_TIME_LOCAL_TIME_FULLY, "HH:mm:ss.SSSSSS");
		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_TIME_ZONED_DATE_TIME_FULLY,
				"yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_TIME_OFFSET_DATE_TIME_FULLY,
				"yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_TIME_YEAR_FULLY, "yyyy");
		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_TIME_YEAR_MONTH_FULLY, "yyyy-MM");
		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_TIME_MONTH_DAY_FULLY, "--MM-dd");
		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_TIME_INSTANT_FULLY,
				"yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX");
		DEFAULT_JSON_FORMAT_PATTERNS.put(JavaTypeConstants.JAVA_TIME_OFFSET_TIME_FULLY, "HH:mm:ss.SSSSSSXXX");

	}

	/**
	 * Cache the regex and its pattern object
	 */
	private static final Map<String, Pattern> PATTERN_CACHE = new HashMap<>();

	/**
	 * "packageFilters" cache
	 */
	private static final Map<String, Set<String>> FILTER_METHOD_CACHE = new HashMap<>();

	/**
	 * Generate a random value based on java type name.
	 * @param typeName field type name
	 * @return random value
	 */
	public static String jsonValueByType(String typeName) {
		String type = typeName.contains(".") ? typeName.substring(typeName.lastIndexOf(".") + 1) : typeName;
		// if the type is Instant, transform to LocalDateTime
		if ("Instant".equalsIgnoreCase(type)) {
			type = "LocalDateTime";
		}
		String randomMock = System.getProperty(DocGlobalConstants.RANDOM_MOCK);
		boolean randomMockFlag = Boolean.parseBoolean(randomMock);
		String value = randomMockFlag ? RandomUtil.randomValueByType(type)
				: RandomUtil.generateDefaultValueByType(type);
		if (javaPrimaryType(type)) {
			return value;
		}
		else if ("Void".equalsIgnoreCase(type)) {
			return "null";
		}
		else {
			return "\"" + value + "\"";
		}
	}

	/**
	 * Generate random field values based on field names and type.
	 * @param typeName field type name
	 * @param filedName field name
	 * @return random value
	 */
	public static String getValByTypeAndFieldName(String typeName, String filedName) {
		return getValByTypeAndFieldName(typeName, filedName, Collections.emptyList());
	}

	/**
	 * Generate random field values based on field names, type and annotations.
	 * @param typeName field type name
	 * @param filedName field name
	 * @param annotations field/parameter annotations
	 * @return random value
	 */
	public static String getValByTypeAndFieldName(String typeName, String filedName, List<?> annotations) {
		String randomMock = System.getProperty(DocGlobalConstants.RANDOM_MOCK);
		boolean randomMockFlag = Boolean.parseBoolean(randomMock);
		boolean isArray = true;
		String type = typeName.contains("java.lang") ? typeName.substring(typeName.lastIndexOf(".") + 1) : typeName;
		String key = filedName.toLowerCase() + "-" + type.toLowerCase();
		String value = null;
		if (!type.contains("[")) {
			isArray = false;
		}
		if (!randomMockFlag) {
			return jsonValueByType(typeName);
		}

		value = MOCK_VALUE_RESOLVER.resolveByConstraints(type, annotations);
		if (StringUtil.isEmpty(value)) {
			value = MOCK_VALUE_RESOLVER.resolveByFieldName(key);
		}

		if (StringUtil.isNotEmpty(value) && isArray) {
			value = value + "," + value + "," + value;
		}

		if (Objects.isNull(value)) {
			return jsonValueByType(typeName);
		}
		else {
			if (javaPrimaryType(type)) {
				return value;
			}
			else {
				return handleJsonStr(value);
			}
		}
	}

	/**
	 * To obtain a field's value using Java reflection and remove double quotes from a
	 * string
	 * @param type0 field type name
	 * @param filedName field name
	 * @param removeDoubleQuotation removeDoubleQuotation
	 * @return String
	 */
	public static String getValByTypeAndFieldName(String type0, String filedName, boolean removeDoubleQuotation) {
		return getValByTypeAndFieldName(type0, filedName, Collections.emptyList(), removeDoubleQuotation);
	}

	/**
	 * To obtain a field's value using Java reflection and remove double quotes from a
	 * string.
	 * @param type0 field type name
	 * @param filedName field name
	 * @param annotations field/parameter annotations
	 * @param removeDoubleQuotation removeDoubleQuotation
	 * @return String
	 */
	public static String getValByTypeAndFieldName(String type0, String filedName, List<?> annotations,
			boolean removeDoubleQuotation) {
		if (removeDoubleQuotation) {
			return getValByTypeAndFieldName(type0, filedName, annotations).replace("\"", "");
		}
		else {
			return getValByTypeAndFieldName(type0, filedName, annotations);
		}
	}

	/**
	 * match controller package
	 * @param packageFilters package filter
	 * @param controllerName controller name
	 * @return boolean
	 */
	public static boolean isMatch(String packageFilters, String controllerName) {
		if (StringUtil.isEmpty(packageFilters)) {
			return false;
		}
		String[] patterns = packageFilters.split(",");
		for (String str : patterns) {
			if (str.contains("*")) {
				Pattern pattern = Pattern.compile(str);
				if (pattern.matcher(controllerName).matches()) {
					return true;
				}
			}
			else if (controllerName.startsWith(str)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * match the controller package
	 * @param packageFilters package filter
	 * @param controllerClass controller class
	 * @return boolean
	 */
	public static boolean isMatch(String packageFilters, Object controllerClass) {
		if (StringUtil.isEmpty(packageFilters) || Objects.isNull(controllerClass)) {
			return false;
		}

		String controllerName = getClassCanonicalName(controllerClass);
		if (StringUtil.isEmpty(controllerName)) {
			return false;
		}

		boolean pointToMethod = false;
		List<?> methods = getClassMethods(controllerClass);
		int capacity = Math.max((int) (methods.size() / 0.75F) + 1, 16);
		Set<String> filterMethods = new HashSet<>(capacity);

		String[] filters = packageFilters.split(",");

		for (String filter : filters) {
			if (filter.contains("*")) {
				Pattern pattern = getPattern(filter);

				boolean matchControllerName = pattern.matcher(controllerName).matches();
				if (matchControllerName) {
					cacheFilterMethods(controllerName, Collections.singleton(DocGlobalConstants.DEFAULT_FILTER_METHOD));
					return true;
				}
				else {
					List<String> controllerMethods = methods.stream()
						.map(DocUtil::getMethodName)
						.collect(Collectors.toList());
					Set<String> methodsMatch = controllerMethods.stream()
						.filter(method -> pattern.matcher(controllerName + "." + method).matches())
						.collect(Collectors.toSet());
					if (!methodsMatch.isEmpty()) {
						pointToMethod = true;
						filterMethods.addAll(methodsMatch);
					}
				}
			}
			else if (controllerName.equals(filter) || controllerName.contains(filter)) {
				cacheFilterMethods(controllerName, Collections.singleton(DocGlobalConstants.DEFAULT_FILTER_METHOD));
				return true;
			}
			else if (filter.contains(controllerName)) {
				pointToMethod = true;
				String method = filter.replace(controllerName, "").replace(".", "");
				filterMethods.add(method);
			}
		}

		if (pointToMethod) {
			cacheFilterMethods(controllerName, filterMethods);
			return true;
		}

		return false;
	}

	/**
	 * Get pattern from the cache by a regex string. If there is no cache, then compile a
	 * new pattern object and put it into cache
	 * @param regex a regex string
	 * @return a usable pattern object
	 */
	private static Pattern getPattern(String regex) {
		Pattern pattern = PATTERN_CACHE.get(regex);
		if (pattern == null) {
			pattern = Pattern.compile(regex);
			PATTERN_CACHE.put(regex, pattern);
		}
		return pattern;
	}

	/**
	 * Put the specified method names into a cache.
	 * @param controller the controller canonical name
	 * @param methods the methods will be cached
	 */
	private static void cacheFilterMethods(String controller, Set<String> methods) {
		FILTER_METHOD_CACHE.put(controller, methods);
	}

	/**
	 * Get filter method name from cache, no cache will return "*", which means all
	 * methods.
	 * @param controller the controller canonical name
	 * @return the cached methods or "*"
	 */
	private static Set<String> getFilterMethodsCache(String controller) {
		return FILTER_METHOD_CACHE.getOrDefault(controller,
				Collections.singleton(DocGlobalConstants.DEFAULT_FILTER_METHOD));
	}

	/**
	 * Find methods if the user specified in "packageFilters". If not specified, return
	 * "*" by default, which means need all methods.
	 * @param controllerName controllerName
	 * @return the methods user specified
	 * @see #cacheFilterMethods(String, Set)
	 * @see #isMatch(String, Object)
	 */
	public static Set<String> findFilterMethods(String controllerName) {
		return getFilterMethodsCache(controllerName);
	}

	/**
	 * An interpreter for strings with named placeholders.
	 * @param str string to format
	 * @param values to replace
	 * @return formatted string
	 */
	public static String formatAndRemove(String str, Map<String, String> values) {
		if (SystemPlaceholders.hasSystemProperties(str)) {
			str = DocUtil.delPropertiesUrl(str, new HashSet<>());
		}
		if (!str.contains(":")) {
			return str;
		}

		List<String> pathList = splitPathBySlash(str);
		List<String> finalPaths = new ArrayList<>(pathList.size());
		for (String pathParam : pathList) {
			if (pathParam.startsWith("http:") || pathParam.startsWith("https:")) {
				finalPaths.add(pathParam + DocGlobalConstants.PATH_DELIMITER);
				continue;
			}
			if (pathParam.startsWith("${")) {
				finalPaths.add(pathParam);
				continue;
			}
			if (pathParam.contains(":") && pathParam.startsWith("{")) {
				int length = pathParam.length();
				String reg = pathParam.substring(pathParam.indexOf(":") + 1, length - 1);
				Generex generex = new Generex(reg);
				// Generate random String
				String randomStr = generex.random();
				String key = pathParam.substring(1, pathParam.indexOf(":"));
				if (!values.containsKey(key)) {
					values.put(key, randomStr);
				}
				String path = pathParam.substring(0, pathParam.indexOf(":")) + "}";
				finalPaths.add(path);
				continue;
			}
			finalPaths.add(pathParam);
		}
		str = StringUtils.join(finalPaths, '/');

		StringBuilder builder = new StringBuilder(str);
		Set<Map.Entry<String, String>> entries = values.entrySet();
		Iterator<Map.Entry<String, String>> iteratorMap = entries.iterator();
		while (iteratorMap.hasNext()) {
			Map.Entry<String, String> next = iteratorMap.next();
			int start;
			String pattern = "{" + next.getKey() + "}";
			String value = next.getValue();
			// values.remove(next.getKey());
			// Replace every occurence of {key} with value
			while ((start = builder.indexOf(pattern)) != -1) {
				builder.replace(start, start + pattern.length(), value);
			}
			iteratorMap.remove();
			values.remove(next.getKey());

		}
		return builder.toString();
	}

	/**
	 * // /detail/{id:[a-zA-Z0-9]{3}}/{name:[a-zA-Z0-9]{3}} remove pattern
	 * @param str path
	 * @return String
	 */
	public static String formatPathUrl(String str) {
		if (SystemPlaceholders.hasSystemProperties(str)) {
			str = DocUtil.delPropertiesUrl(str, new HashSet<>());
		}
		if (!str.contains(":")) {
			return str;
		}

		StringBuilder urlBuilder = new StringBuilder();
		String[] urls = str.split(";");
		int index = 0;
		for (String url : urls) {
			String[] strArr = url.split(DocGlobalConstants.PATH_DELIMITER);
			for (int i = 0; i < strArr.length; i++) {
				String pathParam = strArr[i];
				if (pathParam.startsWith("http:") || pathParam.startsWith("https:") || pathParam.startsWith("{{")) {
					continue;
				}
				if (pathParam.startsWith("{") && pathParam.contains(":")) {
					strArr[i] = pathParam.substring(0, pathParam.indexOf(":")) + "}";
				}
			}
			if (index < urls.length - 1) {
				urlBuilder.append(StringUtils.join(Arrays.asList(strArr), '/')).append(";");
			}
			else {
				urlBuilder.append(StringUtils.join(Arrays.asList(strArr), '/'));
			}
			index++;
		}
		return urlBuilder.toString();
	}

	/**
	 * handle spring mvc method
	 * @param method method name
	 * @return String
	 */
	public static String handleHttpMethod(String method) {
		switch (method) {
			// for spring
			case "RequestMethod.GET":
			case "HttpMethod.GET":
			case "org.springframework.http.HttpMethod.GET":
				// for solon
			case "MethodType.GET":
				return "GET";
			case "RequestMethod.POST":
			case "HttpMethod.POST":
			case "org.springframework.http.HttpMethod.POST":
			case "MethodType.POST":
				return "POST";
			case "RequestMethod.PUT":
			case "HttpMethod.PUT":
			case "org.springframework.http.HttpMethod.PUT":
			case "MethodType.PUT":
				return "PUT";
			case "RequestMethod.DELETE":
			case "HttpMethod.DELETE":
			case "org.springframework.http.HttpMethod.DELETE":
			case "MethodType.DELETE":
				return "DELETE";
			case "RequestMethod.PATCH":
			case "HttpMethod.PATCH":
			case "org.springframework.http.HttpMethod.PATCH":
			case "MethodType.PATCH":
				return "PATCH";
			default:
				return "GET";
		}
	}

	/**
	 * handle spring mvc mapping value
	 * @param classLoader ClassLoader
	 * @param annotation JavaAnnotation
	 * @return String
	 */
	public static String handleMappingValue(ClassLoader classLoader, Object annotation) {
		String url = getRequestMappingUrl(classLoader, annotation);
		if (StringUtil.isEmpty(url)) {
			return DocGlobalConstants.PATH_DELIMITER;
		}
		else {
			return StringUtil.trimBlank(url);
		}
	}

	/**
	 * Split url
	 * @param url URL to be divided
	 * @return list of url
	 */
	public static List<String> split(String url) {
		char[] chars = url.toCharArray();
		List<String> result = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		Stack<Character> stack = new Stack<>();
		for (char s : chars) {
			if ('[' == s || ']' == s) {
				continue;
			}
			if ('{' == s) {
				stack.push(s);
			}
			if ('}' == s) {
				if (stack.isEmpty()) {
					throw new RuntimeException("Invalid mapping pattern detected: " + url);
				}
				else {
					stack.pop();
				}
			}
			if (',' == s && stack.isEmpty()) {
				result.add(sb.toString());
				sb.delete(0, sb.length());
				continue;
			}
			sb.append(s);
		}
		result.add(sb.toString());
		return result;
	}

	/**
	 * obtain params comments from a method-like metadata object
	 * @param method method metadata object
	 * @param tagName java comments tag
	 * @param className class name
	 * @return Map
	 */
	public static Map<String, String> getCommentsByTag(final Object method, final String tagName,
			final String className) {
		List<?> paramTags = invokeListAccessor(method, "getTagsByName", tagName);
		String methodName = getMethodName(method);
		String declaringClassName = getMethodDeclaringClassCanonicalName(method);
		String tagValNullMsg = "ERROR: #" + methodName + "() - bad @" + tagName + " Javadoc tag usage from "
				+ declaringClassName + ", This is an invalid comment.";
		String tagValErrorMsg = "ERROR: An invalid comment was written [@" + tagName + " |]," + "Please @see "
				+ declaringClassName + "." + methodName + "()";
		return getCommentsByTag(paramTags, tagName, className, tagValNullMsg, tagValErrorMsg);
	}

	public static Map<String, String> getRecordCommentsByTag(Object javaClass, final String tagName) {
		List<?> paramTags = invokeListAccessor(javaClass, "getTagsByName", tagName);
		String className = getClassCanonicalName(javaClass);
		String tagValNullMsg = "ERROR: " + "Bad @" + tagName + " Javadoc  tag usage from " + className
				+ ", This is an invalid comment.";
		String tagValErrorMsg = "ERROR: An invalid comment was written [@" + tagName + " |]," + "Please @see "
				+ className;
		return getCommentsByTag(paramTags, tagName, className, tagValNullMsg, tagValErrorMsg);
	}

	public static Map<String, String> getCommentsByTag(List<?> paramTags, final String tagName) {
		return getCommentsByTag(paramTags, tagName, null, null, null);
	}

	private static Map<String, String> getCommentsByTag(List<?> paramTags, final String tagName, String className,
			String tagValNullMsg, String tagValErrorMsg) {
		Map<String, String> paramTagMap = new HashMap<>(paramTags.size());
		for (Object docletTag : paramTags) {
			String value = getDocletTagValue(docletTag);
			if (StringUtil.isEmpty(value) && StringUtil.isNotEmpty(className)) {
				throw new RuntimeException(tagValNullMsg);
			}
			if (DocTags.PARAM.equals(tagName) || DocTags.EXTENSION.equals(tagName)) {
				String pName = value;
				String pValue = DocGlobalConstants.NO_COMMENTS_FOUND;

				// Fixed #1129
				// Split the value by the first sequence of whitespace
				// (space,newline,tab, etc.) only into two parts.
				String[] parts = value.trim().split("\\s+", 2);
				// Successfully split into parameter name and description
				if (parts.length == 2) {
					// Parameter name
					pName = parts[0];
					// Description, preserving internal newlines and formatting
					pValue = parts[1];
				}
				// Only one part found, likely just the parameter name
				// without a description, OR an empty/whitespace-only value after trim
				else if (parts.length == 1) {
					// Covers both "paramName" and "" cases
					// pValue remains DocGlobalConstants.NO_COMMENTS_FOUND
					// Set pName based on whether the single part is empty
					pName = parts[0];
				}

				if ("|".equals(StringUtil.trim(pValue)) && StringUtil.isNotEmpty(className)) {
					throw new RuntimeException(tagValErrorMsg);
				}
				paramTagMap.put(pName, pValue);
			}
			else {
				paramTagMap.put(value, DocGlobalConstants.EMPTY);
			}
		}
		return paramTagMap;
	}

	/**
	 * Reads doclet-tag name in an implementation-agnostic way.
	 * @param docletTag doclet-tag object
	 * @return tag name or empty string when unavailable
	 */
	public static String getDocletTagName(Object docletTag) {
		return getDocletTagProperty(docletTag, "getName");
	}

	/**
	 * Reads doclet-tag value in an implementation-agnostic way.
	 * @param docletTag doclet-tag object
	 * @return tag value or empty string when unavailable
	 */
	public static String getDocletTagValue(Object docletTag) {
		return getDocletTagProperty(docletTag, "getValue");
	}

	private static String getDocletTagProperty(Object docletTag, String accessorName) {
		if (Objects.isNull(docletTag)) {
			return StringUtil.EMPTY;
		}
		try {
			Object value = docletTag.getClass().getMethod(accessorName).invoke(docletTag);
			if (value instanceof String) {
				return (String) value;
			}
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			// Keep return empty when tag metadata cannot be resolved.
		}
		return StringUtil.EMPTY;
	}

	/**
	 * Reads method name in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return method name or empty string when unavailable
	 */
	public static String getMethodName(Object method) {
		return invokeStringAccessor(method, "getName");
	}

	/**
	 * Reads method return type canonical name in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return canonical type name or empty string when unavailable
	 */
	public static String getMethodReturnTypeCanonicalName(Object method) {
		Object returnType = invokeNoArgAccessor(method, "getReturnType");
		return invokeStringAccessor(returnType, "getCanonicalName");
	}

	/**
	 * Reads method return type generic canonical name in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return generic canonical type name or empty string when unavailable
	 */
	public static String getMethodReturnTypeGenericCanonicalName(Object method) {
		Object returnType = invokeNoArgAccessor(method, "getReturnType");
		return invokeStringAccessor(returnType, "getGenericCanonicalName");
	}

	/**
	 * Reads method declaration signature in an implementation-agnostic way.
	 * @param method method metadata object
	 * @param withModifiers whether modifiers should be included
	 * @return declaration signature or empty string when unavailable
	 */
	public static String getMethodDeclarationSignature(Object method, boolean withModifiers) {
		if (Objects.isNull(method)) {
			return StringUtil.EMPTY;
		}
		try {
			Object value = method.getClass()
				.getMethod("getDeclarationSignature", boolean.class)
				.invoke(method, withModifiers);
			if (value instanceof String) {
				return (String) value;
			}
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			// Keep parser-agnostic behavior: unavailable signature metadata returns
			// empty.
		}
		return StringUtil.EMPTY;
	}

	/**
	 * Reads method declaring class canonical name in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return canonical class name or empty string when unavailable
	 */
	public static String getMethodDeclaringClassCanonicalName(Object method) {
		Object declaringClass = getMethodDeclaringClass(method);
		return getClassCanonicalName(declaringClass);
	}

	/**
	 * Reads method declaring class metadata in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return declaring class metadata object or null when unavailable
	 */
	public static Object getMethodDeclaringClass(Object method) {
		return invokeNoArgAccessor(method, "getDeclaringClass");
	}

	/**
	 * Reads class canonical name in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return canonical class name or empty string when unavailable
	 */
	public static String getClassCanonicalName(Object javaClass) {
		return invokeStringAccessor(javaClass, "getCanonicalName");
	}

	/**
	 * Reads class simple name in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return simple class name or empty string when unavailable
	 */
	public static String getClassSimpleName(Object javaClass) {
		return invokeStringAccessor(javaClass, "getName");
	}

	/**
	 * Reads class package name in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return package name or empty string when unavailable
	 */
	public static String getClassPackageName(Object javaClass) {
		Object pkg = invokeNoArgAccessor(javaClass, "getPackage");
		return invokeStringAccessor(pkg, "getName");
	}

	/**
	 * Reads class methods in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return method list or empty list when unavailable
	 */
	public static List<?> getClassMethods(Object javaClass) {
		return invokeListAccessor(javaClass, "getMethods");
	}

	/**
	 * Reads class enum constants in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return enum constants list or empty list when unavailable
	 */
	public static List<?> getClassEnumConstants(Object javaClass) {
		return invokeListAccessor(javaClass, "getEnumConstants");
	}

	/**
	 * Reads super class metadata in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return super class metadata object or null when unavailable
	 */
	public static Object getClassSuperJavaClass(Object javaClass) {
		return invokeNoArgAccessor(javaClass, "getSuperJavaClass");
	}

	/**
	 * Reads class fields in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return field list or empty list when unavailable
	 */
	public static List<?> getClassFields(Object javaClass) {
		return invokeListAccessor(javaClass, "getFields");
	}

	/**
	 * Reads nested classes in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return nested class list or empty list when unavailable
	 */
	public static List<?> getClassNestedClasses(Object javaClass) {
		return invokeListAccessor(javaClass, "getNestedClasses");
	}

	/**
	 * Reads class annotations in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return annotation list or empty list when unavailable
	 */
	public static List<?> getClassAnnotations(Object javaClass) {
		return invokeListAccessor(javaClass, "getAnnotations");
	}

	/**
	 * Reads class tags in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return tag list or empty list when unavailable
	 */
	public static List<?> getClassTags(Object javaClass) {
		return invokeListAccessor(javaClass, "getTags");
	}

	/**
	 * Reads class tag by name in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @param tagName tag name
	 * @return tag object or null when unavailable
	 */
	public static Object getClassTagByName(Object javaClass, String tagName) {
		if (Objects.isNull(javaClass) || StringUtil.isEmpty(tagName)) {
			return null;
		}
		try {
			return javaClass.getClass().getMethod("getTagByName", String.class).invoke(javaClass, tagName);
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	/**
	 * Reads class comment in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return class comment or empty string when unavailable
	 */
	public static String getClassComment(Object javaClass) {
		return invokeStringAccessor(javaClass, "getComment");
	}

	/**
	 * Reads class generic fully-qualified name in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return generic fully-qualified name or empty string when unavailable
	 */
	public static String getClassGenericFullyQualifiedName(Object javaClass) {
		return invokeStringAccessor(javaClass, "getGenericFullyQualifiedName");
	}

	/**
	 * Reads class binary name in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return binary class name or empty string when unavailable
	 */
	public static String getClassBinaryName(Object javaClass) {
		return invokeStringAccessor(javaClass, "getBinaryName");
	}

	/**
	 * Reads class annotation flag in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return true if annotation, false otherwise
	 */
	public static boolean isClassAnnotation(Object javaClass) {
		if (Objects.isNull(javaClass)) {
			return false;
		}
		try {
			Object value = javaClass.getClass().getMethod("isAnnotation").invoke(javaClass);
			return value instanceof Boolean && (Boolean) value;
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	/**
	 * Reads class enum flag in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return true if enum, false otherwise
	 */
	public static boolean isClassEnum(Object javaClass) {
		if (Objects.isNull(javaClass)) {
			return false;
		}
		try {
			Object value = javaClass.getClass().getMethod("isEnum").invoke(javaClass);
			return value instanceof Boolean && (Boolean) value;
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	/**
	 * Reads class interface flag in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @return true if interface, false otherwise
	 */
	public static boolean isClassInterface(Object javaClass) {
		if (Objects.isNull(javaClass)) {
			return false;
		}
		try {
			Object value = javaClass.getClass().getMethod("isInterface").invoke(javaClass);
			return value instanceof Boolean && (Boolean) value;
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	/**
	 * Reads method parameters in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return parameters list or empty list when unavailable
	 */
	public static List<?> getMethodParameters(Object method) {
		return invokeListAccessor(method, "getParameters");
	}

	/**
	 * Reads method annotations in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return annotation list or empty list when unavailable
	 */
	public static List<?> getMethodAnnotations(Object method) {
		return invokeListAccessor(method, "getAnnotations");
	}

	/**
	 * Reads method comment in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return method comment or empty string when unavailable
	 */
	public static String getMethodComment(Object method) {
		return invokeStringAccessor(method, "getComment");
	}

	/**
	 * Reads method modifiers in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return modifier list or empty list when unavailable
	 */
	public static List<String> getMethodModifiers(Object method) {
		List<?> modifiers = invokeListAccessor(method, "getModifiers");
		List<String> values = new ArrayList<>(modifiers.size());
		for (Object modifier : modifiers) {
			if (modifier != null) {
				values.add(modifier.toString());
			}
		}
		return values;
	}

	/**
	 * Reads method private flag in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return true if private, false otherwise
	 */
	public static boolean isMethodPrivate(Object method) {
		if (Objects.isNull(method)) {
			return false;
		}
		try {
			Object value = method.getClass().getMethod("isPrivate").invoke(method);
			return value instanceof Boolean && (Boolean) value;
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	/**
	 * Reads method default flag in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return true if default method, false otherwise
	 */
	public static boolean isMethodDefault(Object method) {
		if (Objects.isNull(method)) {
			return false;
		}
		try {
			Object value = method.getClass().getMethod("isDefault").invoke(method);
			return value instanceof Boolean && (Boolean) value;
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	/**
	 * Reads method tags by name in an implementation-agnostic way.
	 * @param method method metadata object
	 * @param tagName tag name
	 * @return tag list or empty list when unavailable
	 */
	public static List<?> getMethodTagsByName(Object method, String tagName) {
		if (StringUtil.isEmpty(tagName)) {
			return Collections.emptyList();
		}
		return invokeListAccessor(method, "getTagsByName", tagName);
	}

	/**
	 * Reads field annotations in an implementation-agnostic way.
	 * @param field field metadata object
	 * @return annotation list or empty list when unavailable
	 */
	public static List<?> getFieldAnnotations(Object field) {
		return invokeListAccessor(field, "getAnnotations");
	}

	/**
	 * Reads field transient flag in an implementation-agnostic way.
	 * @param field field metadata object
	 * @return true if transient, false otherwise
	 */
	public static boolean isFieldTransient(Object field) {
		if (Objects.isNull(field)) {
			return false;
		}
		try {
			Object value = field.getClass().getMethod("isTransient").invoke(field);
			return value instanceof Boolean && (Boolean) value;
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	/**
	 * Reads field static flag in an implementation-agnostic way.
	 * @param field field metadata object
	 * @return true if static, false otherwise
	 */
	public static boolean isFieldStatic(Object field) {
		if (Objects.isNull(field)) {
			return false;
		}
		try {
			Object value = field.getClass().getMethod("isStatic").invoke(field);
			return value instanceof Boolean && (Boolean) value;
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	/**
	 * Reads field name in an implementation-agnostic way.
	 * @param field field metadata object
	 * @return field name or empty string when unavailable
	 */
	public static String getFieldName(Object field) {
		return invokeStringAccessor(field, "getName");
	}

	/**
	 * Reads field comment in an implementation-agnostic way.
	 * @param field field metadata object
	 * @return field comment or empty string when unavailable
	 */
	public static String getFieldComment(Object field) {
		return invokeStringAccessor(field, "getComment");
	}

	/**
	 * Reads field tags in an implementation-agnostic way.
	 * @param field field metadata object
	 * @return tags list or empty list when unavailable
	 */
	public static List<?> getFieldTags(Object field) {
		return invokeListAccessor(field, "getTags");
	}

	/**
	 * Reads field initialization expression in an implementation-agnostic way.
	 * @param field field metadata object
	 * @return initialization expression or empty string when unavailable
	 */
	public static String getFieldInitializationExpression(Object field) {
		return invokeStringAccessor(field, "getInitializationExpression");
	}

	/**
	 * Reads field type fully-qualified name in an implementation-agnostic way.
	 * @param field field metadata object
	 * @return field type fully-qualified name or empty string when unavailable
	 */
	public static String getFieldTypeFullyQualifiedName(Object field) {
		Object type = invokeNoArgAccessor(field, "getType");
		return invokeStringAccessor(type, "getFullyQualifiedName");
	}

	/**
	 * Reads field type generic fully-qualified name in an implementation-agnostic way.
	 * @param field field metadata object
	 * @return generic fully-qualified type name or empty string when unavailable
	 */
	public static String getFieldGenericFullyQualifiedName(Object field) {
		Object type = invokeNoArgAccessor(field, "getType");
		return invokeStringAccessor(type, "getGenericFullyQualifiedName");
	}

	/**
	 * Reads method parameter types in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return parameter type list or empty list when unavailable
	 */
	public static List<?> getMethodParameterTypes(Object method) {
		return invokeListAccessor(method, "getParameterTypes");
	}

	/**
	 * Reads method varArgs flag in an implementation-agnostic way.
	 * @param method method metadata object
	 * @return true if varArgs, false otherwise
	 */
	public static boolean isMethodVarArgs(Object method) {
		if (Objects.isNull(method)) {
			return false;
		}
		try {
			Object value = method.getClass().getMethod("isVarArgs").invoke(method);
			return value instanceof Boolean && (Boolean) value;
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	/**
	 * Reads method tag by name in an implementation-agnostic way.
	 * @param method method metadata object
	 * @param tagName tag name
	 * @return tag object or null when unavailable
	 */
	public static Object getMethodTagByName(Object method, String tagName) {
		if (Objects.isNull(method) || StringUtil.isEmpty(tagName)) {
			return null;
		}
		try {
			return method.getClass().getMethod("getTagByName", String.class).invoke(method, tagName);
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	/**
	 * Resolves class method by signature in an implementation-agnostic way.
	 * @param javaClass class metadata object
	 * @param methodName method name
	 * @param parameterTypes parameter type list
	 * @param varArgs varArgs flag
	 * @return method metadata object or null when unavailable
	 */
	public static Object getClassMethodBySignature(Object javaClass, String methodName, List<?> parameterTypes,
			boolean varArgs) {
		if (Objects.isNull(javaClass) || StringUtil.isEmpty(methodName) || Objects.isNull(parameterTypes)) {
			return null;
		}
		try {
			return javaClass.getClass()
				.getMethod("getMethod", String.class, List.class, boolean.class)
				.invoke(javaClass, methodName, parameterTypes, varArgs);
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	/**
	 * Reads parameter name in an implementation-agnostic way.
	 * @param parameter parameter metadata object
	 * @return parameter name or empty string when unavailable
	 */
	public static String getParameterName(Object parameter) {
		return invokeStringAccessor(parameter, "getName");
	}

	/**
	 * Reads parameter fully-qualified type name in an implementation-agnostic way.
	 * @param parameter parameter metadata object
	 * @return fully-qualified type name or empty string when unavailable
	 */
	public static String getParameterFullyQualifiedName(Object parameter) {
		return invokeStringAccessor(parameter, "getFullyQualifiedName");
	}

	/**
	 * Reads parameter generic fully-qualified type name in an implementation-agnostic
	 * way.
	 * @param parameter parameter metadata object
	 * @return generic fully-qualified type name or empty string when unavailable
	 */
	public static String getParameterGenericFullyQualifiedName(Object parameter) {
		return invokeStringAccessor(parameter, "getGenericFullyQualifiedName");
	}

	/**
	 * Reads parameter type value in an implementation-agnostic way.
	 * @param parameter parameter metadata object
	 * @return type value or empty string when unavailable
	 */
	public static String getParameterTypeValue(Object parameter) {
		Object type = invokeNoArgAccessor(parameter, "getType");
		if (Objects.isNull(type)) {
			return StringUtil.EMPTY;
		}
		return invokeStringAccessor(type, "getValue");
	}

	/**
	 * Reads parameter type canonical name in an implementation-agnostic way.
	 * @param parameter parameter metadata object
	 * @return canonical type name or empty string when unavailable
	 */
	public static String getParameterTypeCanonicalName(Object parameter) {
		Object type = invokeNoArgAccessor(parameter, "getType");
		return invokeStringAccessor(type, "getCanonicalName");
	}

	/**
	 * Reads parameter type generic canonical name in an implementation-agnostic way.
	 * @param parameter parameter metadata object
	 * @return generic canonical type name or empty string when unavailable
	 */
	public static String getParameterTypeGenericCanonicalName(Object parameter) {
		Object type = invokeNoArgAccessor(parameter, "getType");
		return invokeStringAccessor(type, "getGenericCanonicalName");
	}

	/**
	 * Reads parameter annotations in an implementation-agnostic way.
	 * @param parameter parameter metadata object
	 * @return annotation list or empty list when unavailable
	 */
	public static List<?> getParameterAnnotations(Object parameter) {
		return invokeListAccessor(parameter, "getAnnotations");
	}

	/**
	 * Reads type canonical name in an implementation-agnostic way.
	 * @param javaType type metadata object
	 * @return canonical type name or empty string when unavailable
	 */
	public static String getTypeCanonicalName(Object javaType) {
		return invokeStringAccessor(javaType, "getCanonicalName");
	}

	/**
	 * Reads type value in an implementation-agnostic way.
	 * @param javaType type metadata object
	 * @return type value or empty string when unavailable
	 */
	public static String getTypeValue(Object javaType) {
		return invokeStringAccessor(javaType, "getValue");
	}

	/**
	 * Reads type generic value in an implementation-agnostic way.
	 * @param javaType type metadata object
	 * @return generic value or empty string when unavailable
	 */
	public static String getTypeGenericValue(Object javaType) {
		return invokeStringAccessor(javaType, "getGenericValue");
	}

	/**
	 * Reads type fully-qualified name in an implementation-agnostic way.
	 * @param javaType type metadata object
	 * @return fully-qualified type name or empty string when unavailable
	 */
	public static String getTypeFullyQualifiedName(Object javaType) {
		return invokeStringAccessor(javaType, "getFullyQualifiedName");
	}

	/**
	 * Reads type binary name in an implementation-agnostic way.
	 * @param javaType type metadata object
	 * @return binary name or empty string when unavailable
	 */
	public static String getTypeBinaryName(Object javaType) {
		return invokeStringAccessor(javaType, "getBinaryName");
	}

	/**
	 * Reads type generic fully-qualified name in an implementation-agnostic way.
	 * @param javaType type metadata object
	 * @return generic fully-qualified type name or empty string when unavailable
	 */
	public static String getTypeGenericFullyQualifiedName(Object javaType) {
		return invokeStringAccessor(javaType, "getGenericFullyQualifiedName");
	}

	/**
	 * Reads type generic canonical name in an implementation-agnostic way.
	 * @param javaType type metadata object
	 * @return generic canonical type name or empty string when unavailable
	 */
	public static String getTypeGenericCanonicalName(Object javaType) {
		return invokeStringAccessor(javaType, "getGenericCanonicalName");
	}

	/**
	 * Reads annotation type value in an implementation-agnostic way.
	 * @param annotation annotation object
	 * @return annotation type value or empty string when unavailable
	 */
	public static String getAnnotationTypeValue(Object annotation) {
		return getAnnotationTypeProperty(annotation, "getValue");
	}

	/**
	 * Reads annotation type fully-qualified name in an implementation-agnostic way.
	 * @param annotation annotation object
	 * @return fully-qualified annotation type name or empty string when unavailable
	 */
	public static String getAnnotationTypeFullyQualifiedName(Object annotation) {
		return getAnnotationTypeProperty(annotation, "getFullyQualifiedName");
	}

	/**
	 * Reads annotation type simple name in an implementation-agnostic way.
	 * @param annotation annotation object
	 * @return simple annotation type name or empty string when unavailable
	 */
	public static String getAnnotationTypeSimpleName(Object annotation) {
		return getAnnotationTypeProperty(annotation, "getSimpleName");
	}

	/**
	 * Reads annotation property in an implementation-agnostic way.
	 * @param annotation annotation object
	 * @param propertyName property name
	 * @return property value or null when unavailable
	 */
	public static Object getAnnotationProperty(Object annotation, String propertyName) {
		return invokeAnnotationAccessor(annotation, "getProperty", propertyName);
	}

	/**
	 * Reads annotation named parameter in an implementation-agnostic way.
	 * @param annotation annotation object
	 * @param propertyName property name
	 * @return named parameter value or null when unavailable
	 */
	public static Object getAnnotationNamedParameter(Object annotation, String propertyName) {
		return invokeAnnotationAccessor(annotation, "getNamedParameter", propertyName);
	}

	/**
	 * Reads annotation named parameter map in an implementation-agnostic way.
	 * @param annotation annotation object
	 * @return named parameter map, or empty map when unavailable
	 */
	public static Map<String, Object> getAnnotationNamedParameterMap(Object annotation) {
		return invokeAnnotationMapAccessor(annotation, "getNamedParameterMap");
	}

	/**
	 * Reads annotation property map in an implementation-agnostic way.
	 * @param annotation annotation object
	 * @return property map, or empty map when unavailable
	 */
	public static Map<String, Object> getAnnotationPropertyMap(Object annotation) {
		return invokeAnnotationMapAccessor(annotation, "getPropertyMap");
	}

	private static String getAnnotationTypeProperty(Object annotation, String accessorName) {
		if (Objects.isNull(annotation)) {
			return StringUtil.EMPTY;
		}
		try {
			Object annotationType = annotation.getClass().getMethod("getType").invoke(annotation);
			if (Objects.isNull(annotationType)) {
				return StringUtil.EMPTY;
			}
			Object value = annotationType.getClass().getMethod(accessorName).invoke(annotationType);
			return Objects.nonNull(value) ? value.toString() : StringUtil.EMPTY;
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return StringUtil.EMPTY;
		}
	}

	private static Object invokeAnnotationAccessor(Object annotation, String accessorName, String propertyName) {
		if (Objects.isNull(annotation) || StringUtil.isEmpty(propertyName)) {
			return null;
		}
		try {
			return annotation.getClass().getMethod(accessorName, String.class).invoke(annotation, propertyName);
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> invokeAnnotationMapAccessor(Object annotation, String accessorName) {
		if (Objects.isNull(annotation)) {
			return Collections.emptyMap();
		}
		try {
			Object value = annotation.getClass().getMethod(accessorName).invoke(annotation);
			if (value instanceof Map) {
				return (Map<String, Object>) value;
			}
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			// Keep return empty when annotation map cannot be resolved.
		}
		return Collections.emptyMap();
	}

	private static Object invokeNoArgAccessor(Object target, String accessorName) {
		if (Objects.isNull(target) || StringUtil.isEmpty(accessorName)) {
			return null;
		}
		try {
			return target.getClass().getMethod(accessorName).invoke(target);
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	private static String invokeStringAccessor(Object target, String accessorName) {
		Object value = invokeNoArgAccessor(target, accessorName);
		return Objects.nonNull(value) ? value.toString() : StringUtil.EMPTY;
	}

	@SuppressWarnings("unchecked")
	private static List<?> invokeListAccessor(Object target, String accessorName) {
		Object value = invokeNoArgAccessor(target, accessorName);
		if (value instanceof List) {
			return (List<?>) value;
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	private static List<?> invokeListAccessor(Object target, String accessorName, String argument) {
		if (Objects.isNull(target) || StringUtil.isEmpty(accessorName)) {
			return Collections.emptyList();
		}
		try {
			Object value = target.getClass().getMethod(accessorName, String.class).invoke(target, argument);
			if (value instanceof List) {
				return (List<?>) value;
			}
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			// ignore and return empty list
		}
		return Collections.emptyList();
	}

	/**
	 * Obtain java doc tag comments in a parser-agnostic way.
	 * @param method method metadata object
	 * @param tagName java comments tag
	 * @param className class name
	 * @return first matched key/value string
	 */
	public static String getNormalTagComments(final Object method, final String tagName, final String className) {
		Map<String, String> map = getCommentsByTag(method, tagName, className);
		return getFirstKeyAndValue(map);
	}

	/**
	 * Get field tags using parser-agnostic field metadata.
	 * @param field field metadata object
	 * @param docJavaField doc java field
	 * @return map
	 */
	public static Map<String, String> getFieldTagsValue(final Object field, DocJavaField docJavaField) {
		List<?> paramTags = getFieldTags(field);
		if (CollectionUtil.isEmpty(paramTags) && Objects.nonNull(docJavaField)) {
			paramTags = docJavaField.getDocletTags();
		}
		return paramTags.stream()
			.collect(Collectors.toMap(DocUtil::getDocletTagName, DocUtil::getDocletTagValue,
					(key1, key2) -> key1 + "," + key2));
	}

	/**
	 * Get the first element of a map.
	 * @param map map
	 * @return String
	 */
	public static String getFirstKeyAndValue(Map<String, String> map) {
		String value = null;
		if (map != null && !map.isEmpty()) {
			Map.Entry<String, String> entry = map.entrySet().iterator().next();
			if (entry != null) {
				if (DocGlobalConstants.NO_COMMENTS_FOUND.equals(entry.getValue())) {
					value = entry.getKey();
				}
				else {
					value = entry.getKey() + entry.getValue();
				}
				// value = replaceNewLineToHtmlBr(value);
			}
		}
		return value;
	}

	/**
	 * Use md5 generate id number
	 * @param value value
	 * @return String
	 */
	public static String generateId(String value) {
		if (StringUtil.isEmpty(value)) {
			return null;
		}
		String valueId = DigestUtils.md5Hex(value);
		int length = valueId.length();
		if (valueId.length() < 32) {
			return valueId;
		}
		else {
			return valueId.substring(length - 32, length);
		}
	}

	public static String replaceNewLineToHtmlBr(String content) {
		if (StringUtil.isNotEmpty(content)) {
			return content.replaceAll("(\r\n|\r|\n|\n\r)", "<br>");
		}
		return StringUtils.EMPTY;
	}

	public static String handleJsonStr(String content) {
		return "\"" + content + "\"";
	}

	public static Map<String, String> formDataToMap(List<FormData> formDataList) {
		Map<String, String> formDataMap = new IdentityHashMap<>();
		for (FormData formData : formDataList) {
			if ("file".equals(formData.getType())) {
				continue;
			}
			if (Objects.nonNull(formData.getContentType())) {
				continue;
			}
			if (formData.getKey().contains("[]")) {
				String key = formData.getKey().substring(0, formData.getKey().indexOf("["));
				formDataMap.put(key, formData.getValue() + "&" + key + "=" + formData.getValue());
				continue;
			}
			formDataMap.put(formData.getKey(), formData.getValue());
		}
		return formDataMap;
	}

	public static boolean javaPrimaryType(String type) {
		switch (type) {
			case "Integer":
			case "int":
			case "Long":
			case "long":
			case "Double":
			case "double":
			case "Float":
			case "Number":
			case "float":
			case "Boolean":
			case "boolean":
			case "Short":
			case "short":
			case "BigDecimal":
			case "BigInteger":
			case "Byte":
			case "Character":
			case "character":
				return true;
			default:
				return false;
		}
	}

	public static String javaTypeToOpenApiTypeConvert(String javaTypeName) {
		if (StringUtil.isEmpty(javaTypeName)) {
			return "object";
		}
		if (javaTypeName.length() == 1) {
			return "object";
		}
		if (javaTypeName.contains("[]")) {
			return "array";
		}
		javaTypeName = javaTypeName.toLowerCase();
		switch (javaTypeName) {
			case "java.lang.string":
			case "string":
			case "char":
			case "date":
			case "java.util.uuid":
			case "uuid":
			case "enum":
			case "java.util.date":
			case "java.util.calendar":
			case "localdatetime":
			case "java.time.instant":
			case "java.time.localdatetime":
			case "java.time.year":
			case "java.time.localtime":
			case "java.time.yearmonth":
			case "java.time.monthday":
			case "java.time.localdate":
			case "java.time.period":
			case "localdate":
			case "offsetdatetime":
			case "localtime":
			case "timestamp":
			case "zoneddatetime":
			case "period":
			case "java.time.zoneddatetime":
			case "java.time.offsetdatetime":
			case "java.sql.timestamp":
			case "java.lang.character":
			case "character":
			case "org.bson.types.objectid":
				return "string";
			case "java.util.list":
			case "list":
			case "java.util.set":
			case "set":
			case "java.util.linkedlist":
			case "linkedlist":
			case "java.util.arraylist":
			case "arraylist":
			case "java.util.treeset":
			case "treeset":
			case "enumset":
				return "array";
			case "java.util.byte":
			case "byte":
			case "java.lang.integer":
			case "integer":
			case "int":
			case "short":
			case "java.lang.short":
			case "int32":
				return "integer";
			case "double":
			case "java.lang.long":
			case "long":
			case "java.lang.float":
			case "float":
			case "bigdecimal":
			case "int64":
			case "biginteger":
			case "number":
				return "number";
			case "java.lang.boolean":
			case "boolean":
				return "boolean";
			case "multipartfile":
			case "file":
				return "file";
			default:
				return "object";
		}
	}

	/**
	 * Gets escape and clean comment.
	 * @param comment the comment
	 * @return the escape and clean comment
	 */
	public static String getEscapeAndCleanComment(String comment) {
		if (StringUtil.isEmpty(comment)) {
			return "";
		}
		return comment.replaceAll("&", "&amp;")
			.replaceAll("<", "&lt;")
			.replaceAll(">", "&gt;")
			.replaceAll("\"", "&quot;")
			.replaceAll("'", "&apos;");
	}

	/**
	 * Get the url from 'value' or 'path' attribute
	 * @param classLoader classLoader
	 * @param annotation RequestMapping GetMapping PostMapping etc.
	 * @return the url
	 */
	public static String getRequestMappingUrl(ClassLoader classLoader, Object annotation) {
		return getPathUrl(classLoader, annotation, DocAnnotationConstants.VALUE_PROP, DocAnnotationConstants.NAME_PROP,
				DocAnnotationConstants.PATH_PROP);
	}

	/**
	 * Get mapping url from Annotation
	 * @param classLoader classLoader
	 * @param annotation JavaAnnotation
	 * @param props annotation properties
	 * @return the path
	 */
	public static String getPathUrl(ClassLoader classLoader, Object annotation, String... props) {
		for (String prop : props) {
			Object annotationValue = getAnnotationProperty(annotation, prop);
			if (Objects.nonNull(annotationValue)) {
				Object url = resolveAnnotationValue(classLoader, annotationValue);
				if (Objects.nonNull(url)) {
					return url.toString();
				}
			}
		}
		return StringUtil.EMPTY;
	}

	/**
	 * Resolve annotation value from a generic value object.
	 * @param classLoader classLoader
	 * @param annotationValue annotation value object
	 * @return annotation value
	 */
	public static String resolveAnnotationValue(ClassLoader classLoader, Object annotationValue) {
		if (Objects.isNull(annotationValue)) {
			return StringUtil.EMPTY;
		}
		if (isExpressionType(annotationValue, "Constant")) {
			Object value = invokeNoArgAccessor(annotationValue, "getValue");
			return Objects.nonNull(value) ? value.toString() : StringUtil.EMPTY;
		}
		if (isExpressionType(annotationValue, "Add")) {
			Object left = invokeNoArgAccessor(annotationValue, "getLeft");
			Object right = invokeNoArgAccessor(annotationValue, "getRight");
			String leftValue = resolveAnnotationValue(classLoader, left);
			String rightValue = resolveAnnotationValue(classLoader, right);
			return StringUtil.removeQuotes(leftValue + rightValue);
		}
		if (isExpressionType(annotationValue, "FieldRef")) {
			Object javaField = invokeNoArgAccessor(annotationValue, "getField");
			if (Objects.nonNull(javaField)) {
				Object declaringClass = invokeNoArgAccessor(javaField, "getDeclaringClass");
				String fieldValue = JavaFieldUtil.getConstantsFieldValue(classLoader, declaringClass,
						getFieldName(javaField));
				if (StringUtil.isNotEmpty(fieldValue)) {
					return StringUtil.removeQuotes(fieldValue);
				}
				return StringUtil.removeQuotes(getFieldInitializationExpression(javaField));
			}
		}
		Object parameterValue = invokeNoArgAccessor(annotationValue, "getParameterValue");
		if (Objects.nonNull(parameterValue)) {
			return parameterValue.toString();
		}
		return Optional.ofNullable(annotationValue).map(Object::toString).orElse(StringUtil.EMPTY);
	}

	/**
	 * Resolve annotation value with thread context class loader.
	 * @param annotationValue annotation value object
	 * @return annotation value
	 */
	public static String resolveAnnotationValue(Object annotationValue) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		return resolveAnnotationValue(classLoader, annotationValue);
	}

	/**
	 * handle spring mvc RequestHeader value
	 * @param classLoader classLoader
	 * @param annotation JavaAnnotation
	 * @return String
	 */
	public static String handleRequestHeaderValue(ClassLoader classLoader, Object annotation) {
		String header = getRequestHeaderValue(classLoader, annotation);
		if (StringUtil.isEmpty(header)) {
			return header;
		}
		return StringUtil.removeDoubleQuotes(StringUtil.trimBlank(header));

	}

	/**
	 * Obtain constant from @RequestHeader annotation
	 * @param classLoader classLoader
	 * @param annotation RequestMapping GetMapping PostMapping etc.
	 * @return The constant value
	 */
	public static String getRequestHeaderValue(ClassLoader classLoader, Object annotation) {
		Object annotationValue = getAnnotationProperty(annotation, DocAnnotationConstants.VALUE_PROP);
		return resolveAnnotationValue(classLoader, annotationValue);
	}

	public static List<ApiErrorCode> errorCodeDictToList(ApiConfig config, ProjectDocConfigBuilder configBuilder) {
		return errorCodeDictToList(config, null, configBuilder);
	}

	public static List<ApiErrorCode> errorCodeDictToList(ApiConfig config, JavaProjectBuilder javaProjectBuilder) {
		return errorCodeDictToList(config, javaProjectBuilder, null);
	}

	private static List<ApiErrorCode> errorCodeDictToList(ApiConfig config, JavaProjectBuilder javaProjectBuilder,
			ProjectDocConfigBuilder configBuilder) {
		if (CollectionUtil.isNotEmpty(config.getErrorCodes())) {
			return config.getErrorCodes();
		}
		List<ApiErrorCodeDictionary> errorCodeDictionaries = config.getErrorCodeDictionaries();
		if (CollectionUtil.isEmpty(errorCodeDictionaries)) {
			return new ArrayList<>(0);
		}
		ClassLoader classLoader = config.getClassLoader();
		Set<ApiErrorCode> errorCodeList = new LinkedHashSet<>();
		try {
			for (ApiErrorCodeDictionary dictionary : errorCodeDictionaries) {
				Class<?> clzz = dictionary.getEnumClass();
				if (Objects.isNull(clzz)) {
					if (StringUtil.isEmpty(dictionary.getEnumClassName())) {
						throw new RuntimeException("Enum class name can't be null.");
					}
					clzz = classLoader.loadClass(dictionary.getEnumClassName());
				}

				Class<?> valuesResolverClass = null;
				if (StringUtil.isNotEmpty(dictionary.getValuesResolverClass())) {
					valuesResolverClass = classLoader.loadClass(dictionary.getValuesResolverClass());
				}
				if (null != valuesResolverClass
						&& DictionaryValuesResolver.class.isAssignableFrom(valuesResolverClass)) {
					DictionaryValuesResolver resolver = (DictionaryValuesResolver) DocClassUtil
						.newInstance(valuesResolverClass);
					// add two method results
					errorCodeList.addAll(resolver.resolve());
					errorCodeList.addAll(resolver.resolve(clzz));
				}
				else if (clzz.isInterface()) {
					Set<Class<? extends Enum<?>>> enumImplementSet = dictionary.getEnumImplementSet();
					if (CollectionUtil.isEmpty(enumImplementSet)) {
						continue;
					}

					for (Class<? extends Enum<?>> enumClass : enumImplementSet) {
						String enumClassName = resolveClassName(enumClass);
						if (hasIgnoreDocTag(configBuilder, javaProjectBuilder, enumClassName)) {
							continue;
						}
						List<ApiErrorCode> enumDictionaryList = EnumUtil.getEnumInformation(enumClass,
								dictionary.getCodeField(), dictionary.getDescField());
						errorCodeList.addAll(enumDictionaryList);
					}

				}
				else {
					String className = resolveClassName(clzz);
					if (hasIgnoreDocTag(configBuilder, javaProjectBuilder, className)) {
						continue;
					}
					List<ApiErrorCode> enumDictionaryList = EnumUtil.getEnumInformation(clzz, dictionary.getCodeField(),
							dictionary.getDescField());
					errorCodeList.addAll(enumDictionaryList);
				}

			}
		}
		catch (ClassNotFoundException e) {
			logger.warning(e.getMessage());
		}
		return new ArrayList<>(errorCodeList);
	}

	/**
	 * Build dictionary
	 * @param config api config
	 * @param configBuilder project doc config builder
	 * @return list of ApiDocDict
	 */
	public static List<ApiDocDict> buildDictionary(ApiConfig config, ProjectDocConfigBuilder configBuilder) {
		return buildDictionary(config, null, configBuilder);
	}

	public static List<ApiDocDict> buildDictionary(ApiConfig config, JavaProjectBuilder javaProjectBuilder) {
		return buildDictionary(config, javaProjectBuilder, null);
	}

	private static List<ApiDocDict> buildDictionary(ApiConfig config, JavaProjectBuilder javaProjectBuilder,
			ProjectDocConfigBuilder configBuilder) {
		List<ApiDataDictionary> apiDataDictionaryList = config.getDataDictionaries();
		if (CollectionUtil.isEmpty(apiDataDictionaryList)) {
			return new ArrayList<>(0);
		}
		List<ApiDocDict> apiDocDictList = new ArrayList<>();
		try {
			ClassLoader classLoader = config.getClassLoader();
			int order = 0;
			for (ApiDataDictionary apiDataDictionary : apiDataDictionaryList) {
				order++;
				Class<?> clazz = apiDataDictionary.getEnumClass();
				if (Objects.isNull(clazz)) {
					if (StringUtil.isEmpty(apiDataDictionary.getEnumClassName())) {
						throw new RuntimeException("Enum class name can't be null.");
					}
					clazz = classLoader.loadClass(apiDataDictionary.getEnumClassName());
				}

				if (clazz.isInterface()) {
					Set<Class<? extends Enum<?>>> enumImplementSet = apiDataDictionary.getEnumImplementSet();
					if (CollectionUtil.isEmpty(enumImplementSet)) {
						continue;
					}

					for (Class<? extends Enum<?>> enumClass : enumImplementSet) {
						String enumClassName = resolveClassName(enumClass);
						if (hasIgnoreDocTag(configBuilder, javaProjectBuilder, enumClassName)) {
							continue;
						}
						ApiDocDict apiDocDict = new ApiDocDict();
						apiDocDict.setOrder(order++);
						apiDocDict.setTitle(resolveDictionaryTitle(configBuilder, javaProjectBuilder, enumClassName,
								enumClass.getSimpleName()));
						apiDocDict
							.setDescription(resolveApiNoteComment(configBuilder, javaProjectBuilder, enumClassName));
						List<DataDict> enumDictionaryList = EnumUtil.getEnumInformation(enumClass,
								apiDataDictionary.getCodeField(), apiDataDictionary.getDescField());
						apiDocDict.setDataDictList(enumDictionaryList);
						apiDocDictList.add(apiDocDict);
					}

				}
				else {
					String className = resolveClassName(clazz);
					if (hasIgnoreDocTag(configBuilder, javaProjectBuilder, className)) {
						continue;
					}
					ApiDocDict apiDocDict = new ApiDocDict();
					apiDocDict.setOrder(order);
					apiDocDict.setTitle(apiDataDictionary.getTitle());
					apiDocDict.setDescription(resolveApiNoteComment(configBuilder, javaProjectBuilder, className));
					if (apiDataDictionary.getTitle() == null) {
						apiDocDict.setTitle(resolveDictionaryTitle(configBuilder, javaProjectBuilder, className,
								clazz.getSimpleName()));
					}
					List<DataDict> enumDictionaryList = EnumUtil.getEnumInformation(clazz,
							apiDataDictionary.getCodeField(), apiDataDictionary.getDescField());
					if (!clazz.isEnum()) {
						throw new RuntimeException(clazz.getCanonicalName() + " is not an enum class.");
					}
					apiDocDict.setDataDictList(enumDictionaryList);
					apiDocDictList.add(apiDocDict);
				}

			}
		}
		catch (ClassNotFoundException e) {
			logger.warning(e.getMessage());
		}
		return apiDocDictList;
	}

	private static boolean hasIgnoreDocTag(ProjectDocConfigBuilder configBuilder, JavaProjectBuilder javaProjectBuilder,
			String className) {
		Optional<SourceClass> sourceClass = findSourceClass(configBuilder, className);
		if (sourceClass.isPresent()) {
			return hasSourceDocletTag(sourceClass.get(), DocTags.IGNORE);
		}
		if (Objects.nonNull(configBuilder)) {
			Object javaClass = configBuilder.getClassByName(className);
			return Objects.nonNull(javaClass) && Objects.nonNull(DocUtil.getClassTagByName(javaClass, DocTags.IGNORE));
		}
		if (Objects.isNull(javaProjectBuilder)) {
			return false;
		}
		Object javaClass = javaProjectBuilder.getClassByName(className);
		return Objects.nonNull(javaClass) && Objects.nonNull(DocUtil.getClassTagByName(javaClass, DocTags.IGNORE));
	}

	private static String resolveDictionaryTitle(ProjectDocConfigBuilder configBuilder,
			JavaProjectBuilder javaProjectBuilder, String className, String fallbackTitle) {
		Optional<SourceClass> sourceClass = findSourceClass(configBuilder, className);
		if (sourceClass.isPresent()) {
			String sourceComment = sourceClass.get().comment();
			if (StringUtils.isNotBlank(sourceComment)) {
				return sourceComment;
			}
			if (StringUtil.isNotEmpty(sourceClass.get().simpleName())) {
				return sourceClass.get().simpleName();
			}
		}
		if (Objects.nonNull(javaProjectBuilder)) {
			Object javaClass = javaProjectBuilder.getClassByName(className);
			if (Objects.nonNull(javaClass)) {
				String classComment = DocUtil.getClassComment(javaClass);
				return StringUtils.isBlank(classComment) ? DocUtil.getClassSimpleName(javaClass) : classComment;
			}
		}
		if (Objects.nonNull(configBuilder)) {
			Object javaClass = configBuilder.getClassByName(className);
			if (Objects.nonNull(javaClass)) {
				String classComment = DocUtil.getClassComment(javaClass);
				return StringUtils.isBlank(classComment) ? DocUtil.getClassSimpleName(javaClass) : classComment;
			}
		}
		return fallbackTitle;
	}

	private static String resolveApiNoteComment(ProjectDocConfigBuilder configBuilder,
			JavaProjectBuilder javaProjectBuilder, String className) {
		Optional<SourceClass> sourceClass = findSourceClass(configBuilder, className);
		if (sourceClass.isPresent()) {
			String sourceApiNote = sourceDocletTagValue(sourceClass.get(), DocTags.API_NOTE);
			return DocUtil.getEscapeAndCleanComment(sourceApiNote);
		}
		if (Objects.nonNull(configBuilder)) {
			Object javaClass = configBuilder.getClassByName(className);
			if (Objects.nonNull(javaClass)) {
				Object apiNoteTag = DocUtil.getClassTagByName(javaClass, DocTags.API_NOTE);
				return DocUtil.getEscapeAndCleanComment(
						Optional.ofNullable(apiNoteTag).map(DocUtil::getDocletTagValue).orElse(StringUtil.EMPTY));
			}
		}
		if (Objects.isNull(javaProjectBuilder)) {
			return StringUtil.EMPTY;
		}
		Object javaClass = javaProjectBuilder.getClassByName(className);
		if (Objects.isNull(javaClass)) {
			return StringUtil.EMPTY;
		}
		Object apiNoteTag = DocUtil.getClassTagByName(javaClass, DocTags.API_NOTE);
		return DocUtil.getEscapeAndCleanComment(
				Optional.ofNullable(apiNoteTag).map(DocUtil::getDocletTagValue).orElse(StringUtil.EMPTY));
	}

	private static Optional<SourceClass> findSourceClass(ProjectDocConfigBuilder configBuilder, String className) {
		if (Objects.isNull(configBuilder) || StringUtil.isEmpty(className)) {
			return Optional.empty();
		}
		return configBuilder.findSourceClass(className);
	}

	private static boolean hasSourceDocletTag(SourceClass sourceClass, String tagName) {
		for (SourceDocletTag sourceDocletTag : sourceClass.docletTags()) {
			if (tagName.equals(sourceDocletTag.name())) {
				return true;
			}
		}
		return false;
	}

	private static String sourceDocletTagValue(SourceClass sourceClass, String tagName) {
		for (SourceDocletTag sourceDocletTag : sourceClass.docletTags()) {
			if (tagName.equals(sourceDocletTag.name())) {
				return sourceDocletTag.value();
			}
		}
		return StringUtil.EMPTY;
	}

	private static String resolveClassName(Class<?> clazz) {
		if (Objects.isNull(clazz)) {
			return StringUtil.EMPTY;
		}
		String canonicalName = clazz.getCanonicalName();
		if (StringUtil.isNotEmpty(canonicalName)) {
			return canonicalName;
		}
		return clazz.getName();
	}

	/**
	 * Format field Type
	 * @param genericMap genericMap
	 * @param fieldGicName fieldGicName
	 * @return string
	 */
	public static String formatFieldTypeGicName(Map<String, String> genericMap, String fieldGicName) {
		String fieldGicNameCopy = fieldGicName;
		for (Map.Entry<String, String> entry : genericMap.entrySet()) {
			fieldGicNameCopy = replaceGenericParameter(fieldGicName, entry.getKey(), entry.getValue());
		}
		return fieldGicNameCopy;
	}

	/**
	 * Replaces the specified generic parameter in a string with a given type, supporting
	 * multi-level generics.
	 * @param baseString The base string
	 * @param originalGenericParameter The generic parameter to be replaced, like "T"
	 * @param replacementType The type to replace the original parameter with, like "User"
	 * @return The modified string
	 */
	public static String replaceGenericParameter(String baseString, String originalGenericParameter,
			String replacementType) {
		StringBuilder result = new StringBuilder(baseString);
		String searchPattern = "<" + originalGenericParameter + ">";
		int index = 0;
		while ((index = result.indexOf(searchPattern, index)) != -1) {
			// Replace the specified generic parameter with the replacement type
			result.replace(index, index + searchPattern.length(), "<" + replacementType + ">");
			// Update the index to continue searching for the next occurrence
			index += replacementType.length() + 2; // +2 for '<' and '>' characters
		}
		return result.toString();
	}

	public static String handleConstants(Map<String, String> constantsMap, String value) {
		Object constantsValue = constantsMap.get(value);
		if (Objects.nonNull(constantsValue)) {
			return constantsValue.toString();
		}
		return value;
	}

	public static String handleContentType(ClassLoader classLoader, String mediaType, Object annotation,
			String annotationName) {
		if (JakartaJaxrsAnnotations.JAX_PRODUCES_FULLY.equals(annotationName)
				|| JAXRSAnnotations.JAX_PRODUCES_FULLY.equals(annotationName)) {
			String annotationValue = StringUtil.removeQuotes(DocUtil.getRequestHeaderValue(classLoader, annotation));
			if ("MediaType.APPLICATION_JSON".equals(annotationValue) || "application/json".equals(annotationValue)
					|| "MediaType.TEXT_PLAIN".equals(annotationValue) || "text/plain".equals(annotationValue)) {
				mediaType = MediaType.APPLICATION_JSON;
			}
		}
		return mediaType;
	}

	public static boolean filterPath(RequestMapping requestMapping, ApiReqParam apiReqHeader) {
		if (StringUtil.isEmpty(apiReqHeader.getPathPatterns())
				&& StringUtil.isEmpty(apiReqHeader.getExcludePathPatterns())) {
			return true;
		}
		return DocPathUtil.matches(requestMapping.getShortUrl(), apiReqHeader.getPathPatterns(),
				apiReqHeader.getExcludePathPatterns());

	}

	public static String paramCommentResolve(String comment) {
		if (StringUtil.isEmpty(comment)) {
			comment = DocGlobalConstants.NO_COMMENTS_FOUND;
		}
		else {
			if (comment.contains("|")) {
				comment = comment.substring(0, comment.indexOf("|"));
			}
		}
		return comment;
	}

	/**
	 * del ${server.port:/error}
	 * @param value url
	 * @param visitedPlaceholders cycle
	 * @return url deleted
	 */
	public static String delPropertiesUrl(String value, Set<String> visitedPlaceholders) {
		int startIndex = value.indexOf(SystemPlaceholders.PLACEHOLDER_PREFIX);
		if (startIndex == -1) {
			return value;
		}
		StringBuilder result = new StringBuilder(value);
		while (startIndex != -1) {
			int endIndex = findPlaceholderEndIndex(result, startIndex);
			if (endIndex != -1) {
				String placeholder = result.substring(startIndex + SystemPlaceholders.PLACEHOLDER_PREFIX.length(),
						endIndex);
				String originalPlaceholder = placeholder;
				if (visitedPlaceholders == null) {
					visitedPlaceholders = new HashSet<>(4);
				}
				if (!visitedPlaceholders.add(originalPlaceholder)) {
					throw new IllegalArgumentException(
							"Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
				}
				// Recursive invocation, parsing placeholders contained in the placeholder
				// key.
				placeholder = delPropertiesUrl(placeholder, visitedPlaceholders);
				String propVal = SystemPlaceholders.replaceSystemProperties(placeholder);
				if (propVal == null) {
					int separatorIndex = placeholder.indexOf(":");
					if (separatorIndex != -1) {
						String actualPlaceholder = placeholder.substring(0, separatorIndex);
						String defaultValue = placeholder.substring(separatorIndex + ":".length());
						propVal = SystemPlaceholders.replaceSystemProperties(actualPlaceholder);
						if (propVal == null) {
							propVal = defaultValue;
						}
					}
				}
				if (propVal != null) {
					propVal = delPropertiesUrl(propVal, visitedPlaceholders);
					result.replace(startIndex - 1, endIndex + SystemPlaceholders.PLACEHOLDER_PREFIX.length() - 1,
							propVal);
					startIndex = result.indexOf(SystemPlaceholders.PLACEHOLDER_PREFIX, startIndex + propVal.length());
				}
				else {
					// Proceed with unprocessed value.
					startIndex = result.indexOf(SystemPlaceholders.PLACEHOLDER_PREFIX,
							endIndex + SystemPlaceholders.PLACEHOLDER_PREFIX.length());
				}

				visitedPlaceholders.remove(originalPlaceholder);
			}
			else {
				startIndex = -1;
			}
		}
		return result.toString();
	}

	private static int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
		int index = startIndex + SystemPlaceholders.PLACEHOLDER_PREFIX.length();
		int withinNestedPlaceholder = 0;
		while (index < buf.length()) {
			if (substringMatch(buf, index, SystemPlaceholders.PLACEHOLDER_SUFFIX)) {
				if (withinNestedPlaceholder > 0) {
					withinNestedPlaceholder--;
					index = index + ("}".length());
				}
				else {
					return index;
				}
			}
			else if (substringMatch(buf, index, SystemPlaceholders.SIMPLE_PREFIX)) {
				withinNestedPlaceholder++;
				index = index + SystemPlaceholders.SIMPLE_PREFIX.length();
			}
			else {
				index++;
			}
		}
		return -1;
	}

	public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
		if (index + substring.length() > str.length()) {
			return false;
		}
		for (int i = 0; i < substring.length(); i++) {
			if (str.charAt(index + i) != substring.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * split url by '/' example:
	 * ${server.error.path:${error.path:/error}}/test/{name:[a-zA-Z0-9]{3}}/{bb}/add
	 * @param url url
	 * @return List of path
	 */
	public static List<String> splitPathBySlash(String url) {
		if (StringUtil.isEmpty(url)) {
			return new ArrayList<>(0);
		}
		String[] result = url.split(DocGlobalConstants.PATH_DELIMITER);
		List<String> path = new ArrayList<>();
		for (int i = 0; i < result.length; i++) {
			if (StringUtil.isEmpty(result[i])) {
				continue;
			}
			if (i < result.length - 1) {
				if ((result[i].startsWith("${") && !result[i].endsWith("}"))
						&& (!result[i + 1].startsWith("${") && result[i + 1].endsWith("}"))) {
					String merged = result[i] + DocGlobalConstants.PATH_DELIMITER + result[i + 1];
					path.add(merged);
					i++;
				}
				else {
					path.add(result[i]);
				}
			}
			else {
				path.add(result[i]);
			}
		}
		return path;
	}

	/**
	 * parse tag value, detect the value type, should be one type of String, Map, List
	 * @param value tag value
	 * @return one of String, Map, List
	 */
	public static Object detectTagValue(String value) {
		String v = value.trim();
		// if the value is a List
		if (v.startsWith("[") && v.endsWith("]")) {
			return JsonUtil.toObject(v, List.class);
		}
		if (v.startsWith("{") && v.endsWith("}")) {
			return JsonUtil.toObject(v, Map.class);
		}
		return v;
	}

	/**
	 * Generates a JSON formatted string based on the provided Java field and JSON format
	 * annotation.
	 * @param javaField The Java field to generate JSON format string for.
	 * @param jsonFormatAnnotation The JSON format annotation containing pattern and shape
	 * properties.
	 * @return JSON formatted string based on the annotation properties or null if
	 * annotation is null.
	 */
	public static String getJsonFormatString(Object javaField, Object jsonFormatAnnotation) {
		// If the JSON format annotation is null, directly return null.
		if (Objects.isNull(jsonFormatAnnotation)) {
			return null;
		}

		// Get the type of the Java field.
		Object javaClass = invokeNoArgAccessor(javaField, "getType");
		String fullyQualifiedName = getTypeFullyQualifiedName(javaClass);

		// If the field type is java.time.ZoneOffset, directly return the current time
		// zone offset string.
		if (isClassA(javaClass, "java.time.ZoneOffset")) {
			return handleJsonStr(String.valueOf(OffsetDateTime.now().getOffset()));
		}

		// Get the pattern, shape, timezone, and locale properties from the JSON format
		// annotation.
		Object pattern = getAnnotationProperty(jsonFormatAnnotation, DocAnnotationConstants.JSON_FORMAT_PATTERN_PROP);
		Object shape = getAnnotationProperty(jsonFormatAnnotation, DocAnnotationConstants.JSON_FORMAT_SHAPE_PROP);
		Object timezone = getAnnotationProperty(jsonFormatAnnotation, DocAnnotationConstants.JSON_FORMAT_TIMEZONE_PROP);
		Object locale = getAnnotationProperty(jsonFormatAnnotation, DocAnnotationConstants.JSON_FORMAT_LOCALE_PROP);

		// Determine the pattern value, if the pattern is not specified, use the default
		// pattern based on the field type.
		String patternValue = (pattern != null) ? StringUtil.removeDoubleQuotes(pattern.toString())
				: DEFAULT_JSON_FORMAT_PATTERNS.getOrDefault(fullyQualifiedName, "");

		// If the field is a time type, generate the JSON string based on the time type,
		// shape, pattern, timezone, and locale.
		if (isTimeType(javaClass)) {
			return generateTimeBasedValue(javaClass, shape, patternValue, timezone, locale);
		}

		// If the field is a numeric type, generate a random number JSON string based on
		// the numeric type, pattern, and shape.
		if (isNumericType(javaClass)) {
			return generateRandomNumber(javaClass, patternValue, shape);
		}

		// if enum to number
		if (isClassEnum(javaClass)) {
			if (Objects.equals(DocAnnotationConstants.JSON_FORMAT_SHAPE_NUMBER, getFieldRefName(shape))) {
				return "0";
			}
		}

		return null;
	}

	/**
	 * Determines whether the specified Java class is a time type.
	 * @param javaClass The Java class to check.
	 * @return True if the class is a time type, otherwise false.
	 */
	public static boolean isTimeType(Object javaClass) {
		return isTimeType(getTypeFullyQualifiedName(javaClass));
	}

	/**
	 * Determines whether the specified Java class is a time type.
	 * @param fullyQualifiedName The Java class fullyQualifiedName to check.
	 * @return True if the class is a time type, otherwise false.
	 */
	public static boolean isTimeType(String fullyQualifiedName) {
		return fullyQualifiedName.startsWith("java.time")
				|| JavaTypeConstants.JAVA_UTIL_DATE_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_UTIL_CALENDAR_FULLY.equals(fullyQualifiedName);
	}

	/**
	 * Determines whether the specified Java class is a numeric type.
	 * @param javaClass The Java class to check.
	 * @return True if the class is a numeric type, otherwise false.
	 */
	public static boolean isNumericType(Object javaClass) {
		return isNumericType(getTypeFullyQualifiedName(javaClass));
	}

	/**
	 * Determines whether the specified Java class is a numeric type.
	 * @param fullyQualifiedName The Java class fullyQualifiedName to check.
	 * @return True if the class is a numeric type, otherwise false.
	 */
	public static boolean isNumericType(String fullyQualifiedName) {
		return JavaTypeConstants.JAVA_LANG_INTEGER_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_LANG_LONG_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_LANG_FLOAT_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_LANG_DOUBLE_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_MATH_BIG_DECIMAL_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_MATH_BIG_INTEGER_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_LANG_SHORT_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_LANG_BYTE_FULLY.equals(fullyQualifiedName);
	}

	/**
	 * Generates a time-based value string based on the specified Java class and
	 * JsonFormat annotation values. This method primarily handles the serialization
	 * format of date-time properties based on the shape, pattern, timezone, and locale
	 * values specified in the JsonFormat annotation.
	 * @param javaClass The Java class object containing the annotated field, used to
	 * determine the serialization method.
	 * @param shape The shape value of the JsonFormat annotation, used to determine the
	 * serialization format.
	 * @param patternValue The pattern value of the JsonFormat annotation, used when the
	 * serialization format is a string.
	 * @param timezone The timezone value of the JsonFormat annotation, used to adjust the
	 * time zone during serialization.
	 * @param locale The locale value of the JsonFormat annotation, used to adjust the
	 * locale during serialization.
	 * @return Returns the serialized time-based value string, or null if the shape does
	 * not match the handled conditions.
	 */
	private static String generateTimeBasedValue(Object javaClass, Object shape, String patternValue, Object timezone,
			Object locale) {
		// Check if the shape value is not null and is an instance of FieldRef, then
		// extract the shape's name.
		// If JsonFormat has shape property
		if (Objects.nonNull(shape)) {
			String name = getFieldRefName(shape);

			// When the shape is JsonFormat.Shape.NUMBER, call the method to generate a
			// numeric time value.
			// if the shape is JsonFormat.Shape.NUMBER
			if (Objects.equals(DocAnnotationConstants.JSON_FORMAT_SHAPE_NUMBER, name)) {
				return generateTimeToNumberValue(javaClass);
			}

			// When the shape is JsonFormat.Shape.STRING, call the method to generate a
			// string time value, and further process the return value.
			// if the shape is JsonFormat.Shape.STRING
			if (Objects.equals(DocAnnotationConstants.JSON_FORMAT_SHAPE_STRING, name)) {
				String timeValue = generateTimeStringValue(javaClass, patternValue, timezone, locale);
				// If the generated time string is empty, use the pattern value as the
				// return value.
				if (StringUtil.isEmpty(timeValue)) {
					return patternValue;
				}
				// Further handle the generated time string before returning.
				return handleJsonStr(timeValue);
			}
		}
		// If the shape does not match any handled conditions, return null.
		return null;
	}

	/**
	 * Generates a random number based on the specified Java class type, pattern, and
	 * shape.
	 * @param javaClass The Java class representing the number type.
	 * @param patternValue The pattern value to use for formatting.
	 * @param shape The shape of the JSON format.
	 * @return A random number formatted based on the pattern and shape.
	 */
	private static String generateRandomNumber(Object javaClass, String patternValue, Object shape) {
		// generate random number
		String randomNumber = isIntegerType(javaClass) ? String.valueOf(RandomUtil.randomInt())
				: new DecimalFormat(patternValue).format(RandomUtil.randomDouble());

		if (Objects.equals(DocAnnotationConstants.JSON_FORMAT_SHAPE_STRING, getFieldRefName(shape))) {
			return handleJsonStr(randomNumber);
		}

		return StringUtil.removeQuotes(randomNumber);
	}

	/**
	 * Checks if the specified Java class is an integer type.
	 * @param javaClass The Java class to check.
	 * @return True if the class is an integer type, otherwise false.
	 */
	private static boolean isIntegerType(Object javaClass) {
		String fullyQualifiedName = getTypeFullyQualifiedName(javaClass);

		return JavaTypeConstants.JAVA_LANG_INTEGER_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_MATH_BIG_INTEGER_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_LANG_LONG_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_LANG_SHORT_FULLY.equals(fullyQualifiedName)
				|| JavaTypeConstants.JAVA_LANG_BYTE_FULLY.equals(fullyQualifiedName);
	}

	/**
	 * Generates a time-related value based on the specified Java class type and pattern.
	 * @param javaClass The Java class representing the time type.
	 * @param patternValue The pattern value to use for formatting.
	 * @return A formatted time-related value based on the pattern.
	 */
	private static String generateTimeStringValue(Object javaClass, String patternValue, Object timezone,
			Object locale) {
		ZoneId zoneId = Objects.isNull(timezone) ? ZoneId.systemDefault()
				: TimeZone.getTimeZone(timezone.toString()).toZoneId();
		Locale formatLocale = Objects.isNull(locale) ? Locale.getDefault() : Locale.forLanguageTag(locale.toString());
		try {
			if (isClassEnum(javaClass)) {
				return getClassEnumConstants(javaClass).stream().findFirst().map(DocUtil::getFieldName).orElse(null);
			}

			return Instant.now().atZone(zoneId).format(DateTimeFormatter.ofPattern(patternValue, formatLocale));
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Generates a number value for the given Java class type.
	 * @param javaClass The Java class representing the number type.
	 * @return A number value as a string.
	 */
	private static String generateTimeToNumberValue(Object javaClass) {
		if (isClassA(javaClass, JavaTypeConstants.JAVA_UTIL_CALENDAR_FULLY)
				|| isClassA(javaClass, JavaTypeConstants.JAVA_UTIL_DATE_FULLY)) {
			return String.valueOf(System.currentTimeMillis());
		}
		if (isClassA(javaClass, JavaTypeConstants.JAVA_TIME_YEAR_FULLY)) {
			return Year.now().toString();
		}
		if (isClassA(javaClass, JavaTypeConstants.JAVA_TIME_DAY_OF_WEEK_FULLY)) {
			return String.valueOf(LocalDate.now().getDayOfWeek().getValue());
		}
		if (isClassA(javaClass, JavaTypeConstants.JAVA_TIME_LOCAL_DATE_TIME_FULLY)) {
			LocalDateTime now = LocalDateTime.now();
			return "[" + now.getYear() + "," + now.getMonthValue() + "," + now.getDayOfMonth() + "," + now.getHour()
					+ "," + now.getMinute() + "," + now.getSecond() + "," + now.getNano() + "]";
		}
		if (isClassA(javaClass, JavaTypeConstants.JAVA_TIME_LOCAL_DATE_FULLY)) {
			LocalDate now = LocalDate.now();
			return "[" + now.getYear() + "," + now.getMonthValue() + "," + now.getDayOfMonth() + "]";
		}
		if (isClassA(javaClass, JavaTypeConstants.JAVA_TIME_LOCAL_TIME_FULLY)) {
			LocalTime now = LocalTime.now();
			return "[" + now.getHour() + "," + now.getMinute() + "," + now.getSecond() + "," + now.getNano() + "]";
		}
		if (isClassA(javaClass, JavaTypeConstants.JAVA_TIME_ZONED_DATE_TIME_FULLY)
				|| isClassA(javaClass, JavaTypeConstants.JAVA_TIME_OFFSET_DATE_TIME_FULLY)
				|| isClassA(javaClass, JavaTypeConstants.JAVA_TIME_INSTANT_FULLY)) {
			Instant now = Instant.now();
			long seconds = now.getEpochSecond();
			int nanos = now.getNano();
			return seconds + "." + nanos;
		}
		if (isClassA(javaClass, JavaTypeConstants.JAVA_TIME_YEAR_MONTH_FULLY)) {
			YearMonth now = YearMonth.now();
			return "[" + now.getYear() + "," + now.getMonthValue() + "]";
		}
		if (isClassA(javaClass, JavaTypeConstants.JAVA_TIME_MONTH_DAY_FULLY)) {
			MonthDay now = MonthDay.now();
			return "[" + now.getMonthValue() + "," + now.getDayOfMonth() + "]";
		}
		if (isClassA(javaClass, JavaTypeConstants.JAVA_TIME_OFFSET_TIME_FULLY)) {
			LocalTime now = LocalTime.now();
			return "[" + now.getHour() + "," + now.getMinute() + "," + now.getSecond() + "," + now.getNano() + ","
					+ "\"" + ZoneId.systemDefault().getRules().getOffset(Instant.now()) + "\"" + "]";
		}
		if (isClassA(javaClass, JavaTypeConstants.JAVA_TIME_MONTH_FULLY)) {
			return String.valueOf(LocalDate.now().getMonth().getValue());
		}
		return null;
	}

	/**
	 * Processes the field type name based on JSON format.
	 * <p>
	 * This method is used to determine the corresponding JSON type representation based
	 * on the Java type and its annotations. It primarily handles the conversion of Java
	 * types to JSON types based on the @JsonFormat annotation's properties.
	 * @param isShowJavaType Whether to show Java types, not directly related to the
	 * processing logic here but may be used in future extensions.
	 * @param fullyQualifiedName The fully qualified name of the Java field type.
	 * @param jsonFormatAnnotation The @JsonFormat annotation instance of the field, used
	 * to extract shape information.
	 * @return The string representation of the JSON type, or null if the conversion
	 * cannot be determined.
	 */
	public static String processFieldTypeNameByJsonFormat(boolean isShowJavaType, String fullyQualifiedName,
			Object jsonFormatAnnotation) {
		if (isShowJavaType) {
			return JavaFieldUtil.convertToSimpleTypeName(fullyQualifiedName);
		}
		// Get the pattern, shape, timezone, and locale properties from the JSON format
		// annotation.
		Object shape = getAnnotationProperty(jsonFormatAnnotation, DocAnnotationConstants.JSON_FORMAT_SHAPE_PROP);
		String name = getFieldRefName(shape);
		if (StringUtil.isNotEmpty(name)) {
			// if the shape is string, then the type is string
			if (Objects.equals(DocAnnotationConstants.JSON_FORMAT_SHAPE_STRING, name)) {
				return "string";
			}
			// if the shape is number
			if (Objects.equals(DocAnnotationConstants.JSON_FORMAT_SHAPE_NUMBER, name)) {
				if (DocUtil.isTimeType(fullyQualifiedName)) {
					switch (fullyQualifiedName) {
						case JavaTypeConstants.JAVA_UTIL_CALENDAR_FULLY:
						case JavaTypeConstants.JAVA_UTIL_DATE_FULLY:
							return "int64";
						case JavaTypeConstants.JAVA_TIME_YEAR_FULLY:
							return "int32";
						case JavaTypeConstants.JAVA_TIME_DAY_OF_WEEK_FULLY:
						case JavaTypeConstants.JAVA_TIME_MONTH_FULLY:
							return "int8";
						case JavaTypeConstants.JAVA_TIME_LOCAL_DATE_TIME_FULLY:
						case JavaTypeConstants.JAVA_TIME_LOCAL_DATE_FULLY:
						case JavaTypeConstants.JAVA_TIME_LOCAL_TIME_FULLY:
						case JavaTypeConstants.JAVA_TIME_YEAR_MONTH_FULLY:
						case JavaTypeConstants.JAVA_TIME_MONTH_DAY_FULLY:
						case JavaTypeConstants.JAVA_TIME_OFFSET_TIME_FULLY:
							return "array";
						case JavaTypeConstants.JAVA_TIME_ZONED_DATE_TIME_FULLY:
						case JavaTypeConstants.JAVA_TIME_OFFSET_DATE_TIME_FULLY:
						case JavaTypeConstants.JAVA_TIME_INSTANT_FULLY:
							return "double";
						default:
							return null;
					}
				}
			}
		}
		return null;
	}

	private static boolean isExpressionType(Object expression, String simpleName) {
		return Objects.nonNull(expression) && Objects.equals(simpleName, expression.getClass().getSimpleName());
	}

	private static String getFieldRefName(Object fieldRef) {
		return invokeStringAccessor(fieldRef, "getName");
	}

	private static boolean isClassA(Object javaClass, String typeName) {
		if (Objects.isNull(javaClass) || StringUtil.isEmpty(typeName)) {
			return false;
		}
		try {
			Object value = javaClass.getClass().getMethod("isA", String.class).invoke(javaClass, typeName);
			if (value instanceof Boolean) {
				return (Boolean) value;
			}
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			// Fall through to name-based checks.
		}
		String fullyQualifiedName = getTypeFullyQualifiedName(javaClass);
		if (typeName.equals(fullyQualifiedName)) {
			return true;
		}
		String canonicalName = getTypeCanonicalName(javaClass);
		if (typeName.equals(canonicalName)) {
			return true;
		}
		return typeName.equals(getClassCanonicalName(javaClass));
	}

	/**
	 * Generate indent string based on level.
	 * @param level the nesting level
	 * @return the indent string
	 */
	public static StringBuilder getStringBuilderByLevel(int level) {
		StringBuilder indentBuilder = new StringBuilder();
		for (int i = 0; i < level; i++) {
			indentBuilder.append(DocGlobalConstants.FIELD_SPACE);
		}
		indentBuilder.append(DocGlobalConstants.PARAM_PREFIX);
		return indentBuilder;
	}

	/**
	 * Generate indent string based on level.
	 * @param level the nesting level
	 * @return the indent string
	 */
	public static String getIndentByLevel(int level) {
		return getStringBuilderByLevel(level).toString();
	}

	/**
	 * replace docx content
	 * @param content doc content
	 * @param docxOutputPath docx output path
	 * @param templateDocx docx template
	 * @throws Exception exception
	 * @since 3.0.8
	 */
	public static void copyAndReplaceDocx(String content, String docxOutputPath, String templateDocx) throws Exception {
		InputStream resourceAsStream = WordDocBuilder.class.getClassLoader().getResourceAsStream(templateDocx);
		Objects.requireNonNull(resourceAsStream, "word template docx is not found");

		ZipInputStream zipInputStream = new ZipInputStream(resourceAsStream);
		ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(Paths.get(docxOutputPath)));
		// Traverse the files in the compressed package
		ZipEntry entry;
		while ((entry = zipInputStream.getNextEntry()) != null) {
			String entryName = entry.getName();
			// copy fix the bug: invalid entry compressed size
			zipOutputStream.putNextEntry(new ZipEntry(entryName));
			if ("word/document.xml".equals(entryName)) {
				byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
				zipOutputStream.write(bytes, 0, bytes.length);
			}
			else {
				// copy
				byte[] buffer = new byte[1024];
				int len;
				while ((len = zipInputStream.read(buffer)) > 0) {
					zipOutputStream.write(buffer, 0, len);
				}
			}

			zipOutputStream.closeEntry();
			zipInputStream.closeEntry();
		}

		zipInputStream.close();
		zipOutputStream.close();
	}

}
