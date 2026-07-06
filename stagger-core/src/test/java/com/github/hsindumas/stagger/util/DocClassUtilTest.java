package com.github.hsindumas.stagger.util;

import com.github.hsindumas.stagger.utils.JavaClassValidateUtil;
import com.github.hsindumas.stagger.utils.DocClassUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Description: DocUtil junit test
 *
 * @author yu 2018/06/16.
 * @author HsinDumas
 */
public class DocClassUtilTest {

	@Test
	public void testGetSimpleGicName() {
		String className = "com.github.hsindumas.stagger.controller.Teacher<com.github.hsindumas.stagger.controller.Teacher<com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User>,com.github.hsindumas.stagger.controller.Teacher<com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User>,com.github.hsindumas.stagger.controller.Teacher<com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User>>";
		String[] arr = DocClassUtil.getSimpleGicName(className);
		assertEquals(3, arr.length);
		assertTrue(arr[0].contains("Teacher"));
		assertTrue(arr[1].contains("Teacher"));
		assertTrue(arr[2].contains("Teacher"));
	}

	@Test
	public void testIsPrimitive() {
		String typeName = "java.time.LocalDateTime";
		assertTrue(JavaClassValidateUtil.isPrimitive(typeName));
	}

	@Test
	public void testProcessReturnType() {
		String typeName = "org.springframework.data.domain.Pageable";
		assertEquals("com.github.hsindumas.stagger.model.framework.PageableAsQueryParam",
				DocClassUtil.rewriteRequestParam(typeName));

	}

}
