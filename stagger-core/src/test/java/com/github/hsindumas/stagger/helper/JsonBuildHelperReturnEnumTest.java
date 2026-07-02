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

package com.github.hsindumas.stagger.helper;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.DocJavaMethod;
import com.github.hsindumas.stagger.model.SourceCodePath;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;

/**
 * Regression tests for enum return rendering in JsonBuildHelper.
 *
 * @author HsinDumas
 */
class JsonBuildHelperReturnEnumTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldRenderEnumReturnUsingFacadeResolvedEnumClass() throws Exception {
		Path javaRoot = this.writeFixture();
		ProjectDocConfigBuilder builder = this.newBuilder(javaRoot);
		JavaMethod method = this.findMethod(builder, "sample.json.EnumReturnResource", "getLevel");

		String result = JsonBuildHelper.buildReturnJson(DocJavaMethod.builder().setJavaMethod(method), builder);

		assertEquals("BASIC", result);
	}

	@Test
	void shouldRenderEnumReturnUsingSampleFallbackWhenFacadeClassUnavailable() throws Exception {
		Path javaRoot = this.writeFixture();
		ProjectDocConfigBuilder builder = this.newBuilder(javaRoot);
		JavaMethod rawMethod = this.findMethod(builder, "sample.json.EnumReturnResource", "getLevel");
		String returnType = rawMethod.getReturnType().getGenericCanonicalName();

		ProjectDocConfigBuilder fallbackBuilder = Mockito.spy(builder);
		doReturn(true).when(fallbackBuilder).isEnumType(returnType);
		doReturn(null).when(fallbackBuilder).getClassByName(returnType);
		doReturn("BASIC").when(fallbackBuilder).getEnumSampleValue(returnType);

		String result = JsonBuildHelper.buildReturnJson(DocJavaMethod.builder().setJavaMethod(rawMethod),
				fallbackBuilder);

		assertEquals("BASIC", result);
	}

	private Path writeFixture() throws Exception {
		Path javaRoot = this.tempDir.resolve("src/main/java");
		Path packageRoot = javaRoot.resolve("sample/json");
		Files.createDirectories(packageRoot);
		String source = "package sample.json;\n\n" + "public class EnumReturnResource {\n"
				+ "  enum Level { BASIC, PRO }\n\n" + "  public Level getLevel() {\n" + "    return Level.BASIC;\n"
				+ "  }\n" + "}\n";
		Files.writeString(packageRoot.resolve("EnumReturnResource.java"), source, StandardCharsets.UTF_8);
		return javaRoot;
	}

	private ProjectDocConfigBuilder newBuilder(Path javaRoot) {
		ApiConfig config = new ApiConfig();
		config.setSourceCodePaths(SourceCodePath.builder().setDesc("temp-source").setPath(javaRoot.toString()));
		config.setServerUrl("http://127.0.0.1:8080");
		return new ProjectDocConfigBuilder(config, null);
	}

	private JavaClass findClass(ProjectDocConfigBuilder builder, String className) {
		JavaClass javaClass = builder.getClassByName(className);
		assertNotNull(javaClass, "Expected class to be loaded: " + className);
		return javaClass;
	}

	private JavaMethod findMethod(ProjectDocConfigBuilder builder, String className, String methodName) {
		JavaClass javaClass = this.findClass(builder, className);
		return javaClass.getMethods()
			.stream()
			.filter(method -> methodName.equals(method.getName()))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Method not found: " + methodName));
	}

}