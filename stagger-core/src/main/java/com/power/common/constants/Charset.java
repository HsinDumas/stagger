package com.power.common.constants;

import java.nio.charset.StandardCharsets;

/**
 * Charset constants.
 */
public final class Charset {

	public static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();

	private Charset() {
		throw new IllegalStateException("Utility class");
	}

}
