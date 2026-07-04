/*
 * Copyright (C) 2018-2024 stagger
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

import com.github.hsindumas.stagger.function.HtmlEscape;
import com.github.hsindumas.stagger.function.LineBreaksToBr;
import com.github.hsindumas.stagger.function.RemoveLineBreaks;
import com.github.hsindumas.stagger.function.WordXmlEscape;
import com.github.hsindumas.stagger.common.util.FileUtil;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.hsindumas.stagger.template.engine.TemplateFunction;
import com.github.hsindumas.stagger.template.engine.Template;

/**
 * Template handle util (Beetl syntax on top of FreeMarker runtime)
 *
 * @author sunyu on 2016/12/6.
 * @author HsinDumas
 */
public class BeetlTemplateUtil {

	private static final String HTML_SUFFIX = ".html";

	private static final String TEMPLATE_ROOT = "template/";

	private static final Configuration FREEMARKER_CONFIGURATION = createFreemarkerConfiguration();

	private static final Map<String, freemarker.template.Template> TEMPLATE_CACHE = new ConcurrentHashMap<>(32);

	/**
	 * private constructor
	 */
	private BeetlTemplateUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Get Beetl template by file name
	 * @param templateName template name
	 * @return Beetl Template Object
	 */
	public static Template getByName(String templateName) {
		String normalizedTemplateName = normalizeTemplateName(templateName);
		return new Template(params -> renderTemplate(normalizedTemplateName, params));
	}

	/**
	 * Batch bind binding value to Beetl templates and return all file rendered, Map key
	 * is file name,value is file content
	 * @param path path
	 * @param params params
	 * @return map
	 */
	public static Map<String, String> getTemplatesRendered(String path, Map<String, Object> params) {
		Map<String, String> templateMap = new HashMap<>(16);
		File[] files = FileUtil.getResourceFolderFiles(path);
		for (File f : files) {
			if (f.isFile()) {
				String fileName = f.getName();
				String templateName = buildTemplateName(path, fileName);
				Template tp = getByName(templateName);
				if (Objects.nonNull(params)) {
					tp.binding(params);
				}
				templateMap.put(fileName, tp.render());
			}
		}
		return templateMap;
	}

	private static String normalizeTemplateName(String templateName) {
		String normalized = templateName.replace('\\', '/');
		if (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		if (normalized.startsWith(TEMPLATE_ROOT)) {
			normalized = normalized.substring(TEMPLATE_ROOT.length());
		}
		return normalized;
	}

	private static String buildTemplateName(String path, String fileName) {
		String normalizedPath = path.replace('\\', '/');
		if (normalizedPath.startsWith("/")) {
			normalizedPath = normalizedPath.substring(1);
		}
		if (normalizedPath.startsWith(TEMPLATE_ROOT)) {
			normalizedPath = normalizedPath.substring(TEMPLATE_ROOT.length());
		}
		if (!normalizedPath.isEmpty() && !normalizedPath.endsWith("/")) {
			normalizedPath += "/";
		}
		return normalizeTemplateName(normalizedPath + fileName);
	}

	private static String renderTemplate(String templateName, Map<String, Object> params) {
		try {
			freemarker.template.Template template = TEMPLATE_CACHE.computeIfAbsent(templateName,
					BeetlTemplateUtil::compileTemplate);
			Map<String, Object> model = new HashMap<>(16);
			if (Objects.nonNull(params)) {
				model.putAll(params);
			}
			StringWriter writer = new StringWriter(4096);
			template.process(model, writer);
			String content = writer.toString();
			if (templateName.endsWith(HTML_SUFFIX)) {
				return HtmlCompressorUtil.compress(content);
			}
			return content;
		}
		catch (IOException | TemplateException e) {
			throw new RuntimeException("Can't render template: " + templateName, e);
		}
	}

	private static freemarker.template.Template compileTemplate(String templateName) {
		try {
			String source = readClasspathResource(TEMPLATE_ROOT + templateName);
			String converted = BeetlSyntaxConverter.convert(source);
			return new freemarker.template.Template(templateName, new StringReader(converted),
					FREEMARKER_CONFIGURATION);
		}
		catch (IOException e) {
			throw new RuntimeException("Can't compile template: " + templateName, e);
		}
	}

	private static String readClasspathResource(String resourcePath) throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			classLoader = BeetlTemplateUtil.class.getClassLoader();
		}
		InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
		if (inputStream == null) {
			throw new IOException("Template resource not found: " + resourcePath);
		}
		try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			StringBuilder source = new StringBuilder(4096);
			char[] buffer = new char[4096];
			int readLength;
			while ((readLength = reader.read(buffer)) != -1) {
				source.append(buffer, 0, readLength);
			}
			return source.toString();
		}
	}

	private static Configuration createFreemarkerConfiguration() {
		Configuration configuration = new Configuration(Configuration.VERSION_2_3_34);
		configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
		configuration.setBooleanFormat("c");
		configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		configuration.setLogTemplateExceptions(false);
		configuration.setWrapUncheckedExceptions(true);
		configuration.setFallbackOnNullLoopVariable(false);
		registerSharedFunctions(configuration);
		return configuration;
	}

	private static void registerSharedFunctions(Configuration configuration) {
		try {
			configuration.setSharedVariable("htmlEscape", new BeetlFunctionMethod(new HtmlEscape()));
			configuration.setSharedVariable("wordXmlEscape", new BeetlFunctionMethod(new WordXmlEscape()));
			configuration.setSharedVariable("removeLineBreaks", new BeetlFunctionMethod(new RemoveLineBreaks()));
			configuration.setSharedVariable("lineBreaksToBr", new BeetlFunctionMethod(new LineBreaksToBr()));
			configuration.setSharedVariable("isNotEmpty", new IsNotEmptyMethod());
			TemplateStringUtil stringUtil = new TemplateStringUtil();
			configuration.setSharedVariable("strUtil", stringUtil);
			configuration.setSharedVariable("strutil", stringUtil);
		}
		catch (TemplateModelException e) {
			throw new IllegalStateException("Failed to initialize FreeMarker shared functions.", e);
		}
	}

	private static final class BeetlFunctionMethod implements TemplateMethodModelEx {

		private final TemplateFunction function;

		private BeetlFunctionMethod(TemplateFunction function) {
			this.function = function;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Object exec(List arguments) throws TemplateModelException {
			Object[] params = new Object[arguments.size()];
			for (int i = 0; i < arguments.size(); i++) {
				Object argument = arguments.get(i);
				params[i] = unwrapModel(argument);
			}
			return function.call(params);
		}

	}

	private static final class IsNotEmptyMethod implements TemplateMethodModelEx {

		@Override
		@SuppressWarnings("rawtypes")
		public Object exec(List arguments) throws TemplateModelException {
			if (arguments.isEmpty()) {
				return Boolean.FALSE;
			}
			Object firstArgument = unwrapModel(arguments.get(0));
			return isNotEmpty(firstArgument);
		}

		private boolean isNotEmpty(Object value) {
			if (value == null) {
				return false;
			}
			if (value instanceof CharSequence) {
				return ((CharSequence) value).length() > 0;
			}
			if (value instanceof Collection<?>) {
				return !((Collection<?>) value).isEmpty();
			}
			if (value instanceof Map<?, ?>) {
				return !((Map<?, ?>) value).isEmpty();
			}
			if (value.getClass().isArray()) {
				return Array.getLength(value) > 0;
			}
			return true;
		}

	}

	/**
	 * Basic string helpers exposed to templates.
	 */
	public static final class TemplateStringUtil {

		public String replace(Object source, Object search, Object replacement) {
			if (source == null) {
				return "";
			}
			String sourceText = String.valueOf(source);
			if (search == null) {
				return sourceText;
			}
			String replacementText = replacement == null ? "" : String.valueOf(replacement);
			return sourceText.replace(String.valueOf(search), replacementText);
		}

		public List<String> split(Object source, Object delimiter) {
			if (source == null) {
				return List.of();
			}
			String sourceText = String.valueOf(source);
			if (delimiter == null) {
				return List.of(sourceText);
			}
			String delimiterText = String.valueOf(delimiter);
			if (delimiterText.isEmpty()) {
				List<String> items = new ArrayList<>(sourceText.length());
				for (int i = 0; i < sourceText.length(); i++) {
					items.add(String.valueOf(sourceText.charAt(i)));
				}
				return items;
			}
			return List.of(sourceText.split(Pattern.quote(delimiterText), -1));
		}

	}

	private static Object unwrapModel(Object model) throws TemplateModelException {
		if (model instanceof TemplateModel) {
			return DeepUnwrap.permissiveUnwrap((TemplateModel) model);
		}
		return model;
	}

	private static final class BeetlSyntaxConverter {

		private static final Pattern SCRIPT_PATTERN = Pattern.compile("<%(.*?)%>", Pattern.DOTALL);

		private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{(.*?)}", Pattern.DOTALL);

		private static final Pattern FOR_STATEMENT_PATTERN = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s+in\\s+(.+)$",
				Pattern.DOTALL);

		private BeetlSyntaxConverter() {
		}

		private static String convert(String source) {
			StringBuilder output = new StringBuilder(source.length() + 256);
			Deque<String> directives = new ArrayDeque<>();
			Matcher matcher = SCRIPT_PATTERN.matcher(source);
			int index = 0;
			while (matcher.find()) {
				output.append(convertExpressionBlocks(source.substring(index, matcher.start())));
				output.append(convertScriptBlock(matcher.group(1), directives));
				index = matcher.end();
			}
			output.append(convertExpressionBlocks(source.substring(index)));
			while (!directives.isEmpty()) {
				output.append(closeDirective(directives.pop()));
			}
			return output.toString();
		}

		private static String convertExpressionBlocks(String text) {
			Matcher expressionMatcher = EXPRESSION_PATTERN.matcher(text);
			StringBuffer converted = new StringBuffer(text.length() + 64);
			while (expressionMatcher.find()) {
				String expression = convertExpression(expressionMatcher.group(1));
				expressionMatcher.appendReplacement(converted, Matcher.quoteReplacement("${" + expression + "}"));
			}
			expressionMatcher.appendTail(converted);
			return converted.toString();
		}

		private static String convertExpression(String expression) {
			String converted = expression.trim();
			converted = converted.replaceAll("\\.~size\\b", "?size");
			converted = converted.replace("strutil.", "strUtil.");
			return converted;
		}

		private static String convertScriptBlock(String script, Deque<String> directives) {
			String trimmed = script.trim();
			if (trimmed.isEmpty()) {
				return "";
			}
			StringBuilder out = new StringBuilder(trimmed.length() + 32);
			int index = 0;
			while (index < trimmed.length()) {
				index = skipSeparators(trimmed, index);
				if (index >= trimmed.length()) {
					break;
				}
				char current = trimmed.charAt(index);
				if (current == '}') {
					index = handleClosing(trimmed, index, directives, out);
					continue;
				}
				if (startsWithKeyword(trimmed, index, "if")) {
					index = handleIf(trimmed, index, directives, out);
					continue;
				}
				if (startsWithKeyword(trimmed, index, "for")) {
					index = handleFor(trimmed, index, directives, out);
					continue;
				}
				if (startsWithKeyword(trimmed, index, "var")) {
					index = handleVar(trimmed, index, out);
					continue;
				}
				int end = findStatementEnd(trimmed, index);
				String statement = trimmed.substring(index, end).trim();
				appendAssignIfNeeded(statement, out);
				index = end;
			}
			return out.toString();
		}

		private static int handleIf(String script, int start, Deque<String> directives, StringBuilder out) {
			int openParen = findNextNonWhitespace(script, start + 2);
			int closeParen = findMatchingParenthesis(script, openParen);
			String condition = script.substring(openParen + 1, closeParen);
			out.append("<#if ").append(convertCondition(condition)).append(">");
			directives.push("if");
			return consumeOpeningBrace(script, closeParen + 1);
		}

		private static int handleFor(String script, int start, Deque<String> directives, StringBuilder out) {
			int openParen = findNextNonWhitespace(script, start + 3);
			int closeParen = findMatchingParenthesis(script, openParen);
			String statement = script.substring(openParen + 1, closeParen).trim();
			Matcher matcher = FOR_STATEMENT_PATTERN.matcher(statement);
			if (!matcher.matches()) {
				throw new IllegalStateException("Unsupported for statement: " + statement);
			}
			String loopVar = matcher.group(1);
			String loopExpr = convertExpression(matcher.group(2));
			out.append("<#list ").append(loopExpr).append(" as ").append(loopVar).append(">");
			out.append("<#assign ")
				.append(loopVar)
				.append("LP={\"first\":")
				.append(loopVar)
				.append("?is_first,")
				.append("\"index\":")
				.append(loopVar)
				.append("?index,")
				.append("\"dataIndex\":")
				.append(loopVar)
				.append("?index,")
				.append("\"last\":")
				.append(loopVar)
				.append("?is_last,")
				.append("\"size\":")
				.append(loopExpr)
				.append("?size}>");
			directives.push("for");
			return consumeOpeningBrace(script, closeParen + 1);
		}

		private static int handleVar(String script, int start, StringBuilder out) {
			int statementEnd = findStatementEnd(script, start + 3);
			String statement = script.substring(start + 3, statementEnd).trim();
			appendAssignIfNeeded(statement, out);
			return statementEnd;
		}

		private static int handleClosing(String script, int start, Deque<String> directives, StringBuilder out) {
			int index = skipWhitespace(script, start + 1);
			if (startsWithKeyword(script, index, "else")) {
				int elseIndex = skipWhitespace(script, index + 4);
				if (startsWithKeyword(script, elseIndex, "if")) {
					int openParen = findNextNonWhitespace(script, elseIndex + 2);
					int closeParen = findMatchingParenthesis(script, openParen);
					String condition = script.substring(openParen + 1, closeParen);
					out.append("<#elseif ").append(convertCondition(condition)).append(">");
					return consumeOpeningBrace(script, closeParen + 1);
				}
				out.append("<#else>");
				return consumeOpeningBrace(script, elseIndex);
			}
			if (!directives.isEmpty()) {
				out.append(closeDirective(directives.pop()));
			}
			return index;
		}

		private static void appendAssignIfNeeded(String statement, StringBuilder out) {
			if (statement.isEmpty()) {
				return;
			}
			int assignmentIndex = findAssignmentOperator(statement);
			if (assignmentIndex < 0) {
				return;
			}
			String left = statement.substring(0, assignmentIndex).trim();
			String right = statement.substring(assignmentIndex + 1).trim();
			if (left.isEmpty() || right.isEmpty()) {
				return;
			}
			out.append("<#assign ").append(left).append("=").append(convertExpression(right)).append(">");
		}

		private static String closeDirective(String directive) {
			if ("if".equals(directive)) {
				return "</#if>";
			}
			if ("for".equals(directive)) {
				return "</#list>";
			}
			return "";
		}

		private static int skipSeparators(String text, int start) {
			int index = start;
			while (index < text.length()) {
				char current = text.charAt(index);
				if (!Character.isWhitespace(current) && current != ';') {
					break;
				}
				index++;
			}
			return index;
		}

		private static int skipWhitespace(String text, int start) {
			int index = start;
			while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
				index++;
			}
			return index;
		}

		private static boolean startsWithKeyword(String text, int start, String keyword) {
			if (start < 0 || start + keyword.length() > text.length()) {
				return false;
			}
			if (!text.regionMatches(start, keyword, 0, keyword.length())) {
				return false;
			}
			int end = start + keyword.length();
			if (end >= text.length()) {
				return true;
			}
			char next = text.charAt(end);
			return !Character.isLetterOrDigit(next) && next != '_';
		}

		private static int findStatementEnd(String text, int start) {
			boolean inSingleQuote = false;
			boolean inDoubleQuote = false;
			for (int index = start; index < text.length(); index++) {
				char current = text.charAt(index);
				if (current == '\'' && !inDoubleQuote) {
					inSingleQuote = !inSingleQuote;
				}
				else if (current == '"' && !inSingleQuote) {
					inDoubleQuote = !inDoubleQuote;
				}
				else if (current == ';' && !inSingleQuote && !inDoubleQuote) {
					return index + 1;
				}
			}
			return text.length();
		}

		private static int findMatchingParenthesis(String text, int openParen) {
			int depth = 0;
			for (int i = openParen; i < text.length(); i++) {
				char current = text.charAt(i);
				if (current == '(') {
					depth++;
				}
				else if (current == ')') {
					depth--;
					if (depth == 0) {
						return i;
					}
				}
			}
			throw new IllegalStateException("Unmatched parentheses in script block: " + text);
		}

		private static int findNextNonWhitespace(String text, int start) {
			int index = skipWhitespace(text, start);
			if (index >= text.length() || text.charAt(index) != '(') {
				throw new IllegalStateException("Expected '(' in script block: " + text);
			}
			return index;
		}

		private static int consumeOpeningBrace(String text, int start) {
			int index = skipWhitespace(text, start);
			if (index < text.length() && text.charAt(index) == '{') {
				return index + 1;
			}
			return index;
		}

		private static int findAssignmentOperator(String statement) {
			for (int i = 0; i < statement.length(); i++) {
				char current = statement.charAt(i);
				if (current != '=') {
					continue;
				}
				char previous = i > 0 ? statement.charAt(i - 1) : '\0';
				char next = i + 1 < statement.length() ? statement.charAt(i + 1) : '\0';
				if (previous == '!' || previous == '<' || previous == '>' || previous == '=' || next == '=') {
					continue;
				}
				return i;
			}
			return -1;
		}

		private static String convertCondition(String condition) {
			String converted = convertExpression(condition);
			StringBuilder normalized = new StringBuilder(converted.length() + 16);
			boolean inSingleQuote = false;
			boolean inDoubleQuote = false;
			for (int i = 0; i < converted.length(); i++) {
				char current = converted.charAt(i);
				if (current == '\'' && !inDoubleQuote) {
					inSingleQuote = !inSingleQuote;
					normalized.append(current);
					continue;
				}
				if (current == '"' && !inSingleQuote) {
					inDoubleQuote = !inDoubleQuote;
					normalized.append(current);
					continue;
				}
				if (!inSingleQuote && !inDoubleQuote) {
					if (current == '>' && i + 1 < converted.length() && converted.charAt(i + 1) == '=') {
						normalized.append(" gte ");
						i++;
						continue;
					}
					if (current == '<' && i + 1 < converted.length() && converted.charAt(i + 1) == '=') {
						normalized.append(" lte ");
						i++;
						continue;
					}
					if (current == '>') {
						normalized.append(" gt ");
						continue;
					}
					if (current == '<') {
						normalized.append(" lt ");
						continue;
					}
				}
				normalized.append(current);
			}
			return normalized.toString().replaceAll("\\s+", " ").trim();
		}

	}

}
