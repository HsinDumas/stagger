package com.github.hsindumas.stagger.util;

import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.utils.DocPathUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author yu 2021/6/27.
 * @author HsinDumas
 */
public class DocPathUtilTest {

	@Test
	public void testMatches() {
		String pattern = "/app/page/**";
		String path = "/app/page/{pageIndex}/{pageSize}/{ag}";
		assertFalse(DocPathUtil.matches(path, null, pattern));
	}

	@Test
	public void testMatchesIncludedPattern() {
		String includePattern = "/app/page/**";
		String path = "/app/page/{pageIndex}/{pageSize}/{ag}";
		assertTrue(DocPathUtil.matches(path, includePattern, null));
	}

}
