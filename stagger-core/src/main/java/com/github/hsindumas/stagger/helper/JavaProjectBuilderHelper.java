/*
 * Copyright (C) 2018-2024 stagger
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
package com.github.hsindumas.stagger.helper;

/**
 * the helper to create {@link JavaProjectBuilder} object
 *
 * @author luchuanbaker@qq.com
 * @author HsinDumas
 */
public class JavaProjectBuilderHelper {

	/**
	 * private constructor
	 */
	private JavaProjectBuilderHelper() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * create a new {@link JavaProjectBuilder} object
	 * @return a new {@link JavaProjectBuilder} object
	 */
	public static JavaProjectBuilder create() {
		return new JavaProjectBuilder();
	}

	/**
	 * create a new {@link JavaProjectBuilder} object
	 * @param classLibraryBuilder the {@link SortedClassLibraryBuilder} object
	 * @return a new {@link JavaProjectBuilder} object
	 */
	public static JavaProjectBuilder create(SortedClassLibraryBuilder classLibraryBuilder) {
		return new JavaProjectBuilder(classLibraryBuilder);
	}

}
