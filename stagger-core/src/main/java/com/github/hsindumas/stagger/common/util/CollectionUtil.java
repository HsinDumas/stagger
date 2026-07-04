package com.github.hsindumas.stagger.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Collection helpers.
 */
public final class CollectionUtil {

	private CollectionUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static <T> boolean isNotEmpty(Collection<T> collection) {
		return !isEmpty(collection);
	}

	public static <T> boolean isEmpty(Collection<T> collection) {
		return collection == null || collection.isEmpty();
	}

	@SafeVarargs
	public static <T> List<T> asList(T... values) {
		if (values == null || values.length == 0) {
			return Collections.emptyList();
		}
		List<T> result = new ArrayList<>(values.length);
		Collections.addAll(result, values);
		return result;
	}

}
