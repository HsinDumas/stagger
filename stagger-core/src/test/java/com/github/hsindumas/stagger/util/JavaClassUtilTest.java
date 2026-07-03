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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for JavaClassUtil interface accessor helpers.
 *
 * @author HsinDumas
 */
public class JavaClassUtilTest {

	@Test
	void shouldReturnImplementedInterfacesFromAccessor() {
		Object interfaceType = new Object();
		InterfaceMetadata javaClass = new InterfaceMetadata(Collections.singletonList(interfaceType),
				Collections.emptyList());

		List<?> interfaces = JavaClassUtil.getImplementedInterfaces(javaClass);

		assertEquals(1, interfaces.size(), "Implemented interface list should be returned as-is");
		assertSame(interfaceType, interfaces.get(0), "Returned interface should match accessor result");
	}

	@Test
	void shouldFallbackToEmptyWhenImplementedInterfacesAccessorFails() {
		BrokenInterfaceMetadata javaClass = new BrokenInterfaceMetadata();

		List<?> interfaces = JavaClassUtil.getImplementedInterfaces(javaClass);

		assertTrue(interfaces.isEmpty(), "Accessor failure should fallback to empty implemented-interface list");
	}

	@Test
	void shouldReturnInterfaceClassesFromAccessor() {
		Object interfaceClass = new Object();
		InterfaceMetadata javaClass = new InterfaceMetadata(Collections.emptyList(),
				Collections.singletonList(interfaceClass));

		List<?> interfaces = JavaClassUtil.getInterfaceClasses(javaClass);

		assertEquals(1, interfaces.size(), "Interface class list should be returned as-is");
		assertSame(interfaceClass, interfaces.get(0), "Returned interface class should match accessor result");
	}

	@Test
	void shouldFallbackToEmptyWhenInterfaceClassesAccessorFails() {
		BrokenInterfaceMetadata javaClass = new BrokenInterfaceMetadata();

		List<?> interfaces = JavaClassUtil.getInterfaceClasses(javaClass);

		assertTrue(interfaces.isEmpty(), "Accessor failure should fallback to empty interface-class list");
	}

	public static class InterfaceMetadata {

		private final List<Object> implementsTypes;

		private final List<Object> interfaceClasses;

		public InterfaceMetadata(List<Object> implementsTypes, List<Object> interfaceClasses) {
			this.implementsTypes = implementsTypes;
			this.interfaceClasses = interfaceClasses;
		}

		public List<Object> getImplements() {
			return this.implementsTypes;
		}

		public List<Object> getInterfaces() {
			return this.interfaceClasses;
		}

	}

	public static class BrokenInterfaceMetadata {

		public List<Object> getImplements() {
			throw new RuntimeException("boom");
		}

		public List<Object> getInterfaces() {
			throw new RuntimeException("boom");
		}

	}

}
