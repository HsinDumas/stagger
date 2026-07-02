package com.github.hsindumas.stagger.util;

import com.github.hsindumas.stagger.utils.JavaClassValidateUtil;
import com.github.hsindumas.stagger.utils.DocClassUtil;

import org.junit.jupiter.api.Test;

/**
 * Description: DocUtil junit test
 *
 * @author yu 2018/06/16.
 * @author HsinDumas
 */
public class DocClassUtilTest {

	@Test
	public void testGetSimpleGicName() {
		char me = 'k';
		String className = "com.github.hsindumas.stagger.controller.Teacher<com.github.hsindumas.stagger.controller.Teacher<com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User>,com.github.hsindumas.stagger.controller.Teacher<com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User>,com.github.hsindumas.stagger.controller.Teacher<com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User,com.github.hsindumas.stagger.controller.User>>";
		String[] arr = DocClassUtil.getSimpleGicName(className);
		// System.out.println("arr:"+ JSON.toJSONString(arr));
	}

	@Test
	public void testIsPrimitive() {
		String typeName = "java.time.LocalDateTime";
		System.out.println(JavaClassValidateUtil.isPrimitive(typeName));
	}

	@Test
	public void testProcessReturnType() {
		String typeName = "org.springframework.data.domain.Pageable";
		System.out.println(DocClassUtil.rewriteRequestParam(typeName));

	}

}
