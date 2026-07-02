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

package com.github.hsindumas.stagger.util;

import com.github.hsindumas.stagger.utils.JavaClassUtil;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaType;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Regression tests for JavaClassUtil interface accessor helpers.
 *
 * @author HsinDumas
 */
class JavaClassUtilTest {

	@Test
	void shouldReturnImplementedInterfacesFromAccessor() {
		JavaClass javaClass = Mockito.mock(JavaClass.class);
		JavaType interfaceType = Mockito.mock(JavaType.class);
		when(javaClass.getImplements()).thenReturn(Collections.singletonList(interfaceType));

		List<JavaType> interfaces = JavaClassUtil.getImplementedInterfaces(javaClass);

		assertEquals(1, interfaces.size(), "Implemented interface list should be returned as-is");
		assertSame(interfaceType, interfaces.get(0), "Returned interface should match accessor result");
	}

	@Test
	void shouldFallbackToEmptyWhenImplementedInterfacesAccessorFails() {
		JavaClass javaClass = Mockito.mock(JavaClass.class);
		when(javaClass.getImplements()).thenThrow(new RuntimeException("boom"));

		List<JavaType> interfaces = JavaClassUtil.getImplementedInterfaces(javaClass);

		assertTrue(interfaces.isEmpty(), "Accessor failure should fallback to empty implemented-interface list");
	}

	@Test
	void shouldReturnInterfaceClassesFromAccessor() {
		JavaClass javaClass = Mockito.mock(JavaClass.class);
		JavaClass interfaceClass = Mockito.mock(JavaClass.class);
		when(javaClass.getInterfaces()).thenReturn(Collections.singletonList(interfaceClass));

		List<JavaClass> interfaces = JavaClassUtil.getInterfaceClasses(javaClass);

		assertEquals(1, interfaces.size(), "Interface class list should be returned as-is");
		assertSame(interfaceClass, interfaces.get(0), "Returned interface class should match accessor result");
	}

	@Test
	void shouldFallbackToEmptyWhenInterfaceClassesAccessorFails() {
		JavaClass javaClass = Mockito.mock(JavaClass.class);
		when(javaClass.getInterfaces()).thenThrow(new RuntimeException("boom"));

		List<JavaClass> interfaces = JavaClassUtil.getInterfaceClasses(javaClass);

		assertTrue(interfaces.isEmpty(), "Accessor failure should fallback to empty interface-class list");
	}

}
