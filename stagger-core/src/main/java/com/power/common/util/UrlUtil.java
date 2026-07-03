package com.power.common.util;

import java.util.Map;
import java.util.StringJoiner;

/**
 * URL helpers.
 */
public final class UrlUtil {

	private UrlUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static String urlJoin(String path, Map<String, String> params) {
		String base = path == null ? StringUtil.EMPTY : path;
		if (params == null || params.isEmpty()) {
			return base;
		}

		StringJoiner joiner = new StringJoiner("&");
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (StringUtil.isEmpty(entry.getKey())) {
				continue;
			}
			String value = entry.getValue() == null ? StringUtil.EMPTY : entry.getValue();
			joiner.add(entry.getKey() + "=" + value);
		}

		String queryString = joiner.toString();
		if (StringUtil.isEmpty(queryString)) {
			return base;
		}

		char separator = base.contains("?") ? '&' : '?';
		return base + separator + queryString;
	}

	public static String simplifyUrl(String inputUrl) {
		if (StringUtil.isEmpty(inputUrl)) {
			return StringUtil.EMPTY;
		}
		String normalized = inputUrl.replace('\\', '/').trim();

		String queryPart = StringUtil.EMPTY;
		int queryIndex = normalized.indexOf('?');
		if (queryIndex >= 0) {
			queryPart = normalized.substring(queryIndex);
			normalized = normalized.substring(0, queryIndex);
		}

		String prefix = StringUtil.EMPTY;
		int protocolIndex = normalized.indexOf("://");
		if (protocolIndex >= 0) {
			prefix = normalized.substring(0, protocolIndex + 3);
			normalized = normalized.substring(protocolIndex + 3);
		}

		normalized = normalized.replaceAll("/{2,}", "/");
		if (normalized.length() > 1 && normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}

		return prefix + normalized + queryPart;
	}

}
