package com.github.hsindumas.stagger.function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author HsinDumas
 */
class HtmlEscapeTest {

	@ParameterizedTest
	@MethodSource("provideNormalTestCases")
	void testHtmlEscape(String input, String expected) {
		HtmlEscape htmlEscape = new HtmlEscape();

		String result = htmlEscape.call(new Object[] { input });

		assertEquals(expected, result);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void testHtmlEscapeNullAndEmptyString(Object[] input) {
		HtmlEscape htmlEscape = new HtmlEscape();

		String result = htmlEscape.call(input);

		assertEquals("", result);
	}

	/**
	 * Provides test cases for normal inputs.
	 * @return a stream of arguments for testing
	 */
	static Stream<Arguments> provideNormalTestCases() {
		return Stream.of(Arguments.of("&", "&amp;"), Arguments.of("\"", "&quot;"), Arguments.of("<p>", ""),
				Arguments.of("<p>ab</p>abc", "ab abc"), Arguments.of("</p>", ""),
				Arguments.of("<p>Hello & \"World\"</p>", "Hello &amp; &quot;World&quot; "), Arguments.of("", ""),
				Arguments.of(null, ""));
	}

}