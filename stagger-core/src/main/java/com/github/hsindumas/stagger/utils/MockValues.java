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

import com.github.hsindumas.stagger.common.util.IDCardUtil;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Lightweight mock value helpers used by field dictionary and validation fallback.
 *
 * @author HsinDumas
 */
public final class MockValues {

	private static final String[] USER_NAMES = { "alex", "river", "harper", "morgan", "sky", "charlie" };

	private static final String[] AUTHORS = { "Ada Lovelace", "Linus Torvalds", "Ken Thompson", "Martin Fowler" };

	private static final String[] COMPANIES = { "Stagger Labs", "Acme Cloud", "Open Horizon", "Blue Orbit" };

	private static final String[] DOMAINS = { "example.com", "stagger.dev", "api.local", "service.io" };

	private static final String[] STREETS = { "Main Street", "Sunset Avenue", "Maple Road", "Lake View" };

	private MockValues() {
		throw new IllegalStateException("Utility class");
	}

	public static String username() {
		return pick(USER_NAMES) + randomInt(10, 999);
	}

	public static String author() {
		return pick(AUTHORS);
	}

	public static String email() {
		return username() + "@" + domain();
	}

	public static String domain() {
		return pick(DOMAINS);
	}

	public static String company() {
		return pick(COMPANIES);
	}

	public static String url() {
		return "https://" + domain() + "/" + username();
	}

	public static String semver() {
		return randomInt(0, 5) + "." + randomInt(0, 20) + "." + randomInt(0, 50);
	}

	public static String ipv4() {
		return randomInt(1, 254) + "." + randomInt(0, 255) + "." + randomInt(0, 255) + "." + randomInt(1, 254);
	}

	public static String ipv6() {
		return Integer.toHexString(randomInt(0x1000, 0xFFFF)) + ":" + Integer.toHexString(randomInt(0x1000, 0xFFFF))
				+ ":" + Integer.toHexString(randomInt(0x1000, 0xFFFF)) + ":"
				+ Integer.toHexString(randomInt(0x1000, 0xFFFF)) + ":" + Integer.toHexString(randomInt(0x1000, 0xFFFF))
				+ ":" + Integer.toHexString(randomInt(0x1000, 0xFFFF)) + ":"
				+ Integer.toHexString(randomInt(0x1000, 0xFFFF)) + ":" + Integer.toHexString(randomInt(0x1000, 0xFFFF));
	}

	public static String phone() {
		return "1" + randomDigits(10);
	}

	public static String telephone() {
		return "0" + randomInt(10, 99) + "-" + randomDigits(8);
	}

	public static String address() {
		return randomInt(1, 999) + " " + pick(STREETS);
	}

	public static String message() {
		return ThreadLocalRandom.current().nextBoolean() ? "success" : "fail";
	}

	public static String idCard() {
		return IDCardUtil.getIdCard();
	}

	public static int randomInt(int minInclusive, int maxInclusive) {
		if (maxInclusive <= minInclusive) {
			return minInclusive;
		}
		return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
	}

	private static String randomDigits(int length) {
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			builder.append(randomInt(0, 9));
		}
		return builder.toString();
	}

	private static String pick(String[] values) {
		return values[randomInt(0, values.length - 1)];
	}

}
