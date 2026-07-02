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

package com.github.hsindumas.stagger.handler;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.ParamTypeConstants;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiReqParam;
import com.github.hsindumas.stagger.model.SourceCodePath;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;

/**
 * Tests for enum-aware header extraction in JaxrsHeaderHandler.
 *
 * @author HsinDumas
 */
class JaxrsHeaderHandlerTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldResolveNestedEnumHeaderFromSourceFallback() throws Exception {
		Path javaRoot = this.tempDir.resolve("src/main/java");
		Path packageRoot = javaRoot.resolve("sample/jaxrs");
		Files.createDirectories(packageRoot);
		String source = "package sample.jaxrs;\n\n" + "import jakarta.ws.rs.GET;\n"
				+ "import jakarta.ws.rs.HeaderParam;\n" + "import jakarta.ws.rs.Path;\n\n" + "@Path(\"/fixture\")\n"
				+ "public class NestedEnumHeaderResource {\n" + "  enum Level { BASIC, PRO }\n\n" + "  @GET\n"
				+ "  @Path(\"/header\")\n" + "  public String header(@HeaderParam(\"X-Level\") Level level) {\n"
				+ "    return \"ok\";\n" + "  }\n" + "}\n";
		Files.writeString(packageRoot.resolve("NestedEnumHeaderResource.java"), source, StandardCharsets.UTF_8);

		ProjectDocConfigBuilder builder = this.newBuilder(javaRoot);
		JavaMethod method = this.findMethod(builder, "sample.jaxrs.NestedEnumHeaderResource", "header");
		String enumType = method.getParameters().get(0).getGenericFullyQualifiedName();

		ProjectDocConfigBuilder fallbackBuilder = Mockito.spy(builder);
		doReturn(true).when(fallbackBuilder).isEnumType(enumType);
		doReturn("BASIC").when(fallbackBuilder).getEnumSampleValue(enumType);
		doReturn(null).when(fallbackBuilder).getClassByName(enumType);

		List<ApiReqParam> headers = new JaxrsHeaderHandler().handle(method, fallbackBuilder);
		assertEquals(1, headers.size());

		ApiReqParam header = headers.get(0);
		assertEquals("X-Level", header.getName());
		assertEquals(ParamTypeConstants.PARAM_TYPE_ENUM, header.getType());
		assertEquals("BASIC", header.getValue());
		assertEquals(List.of("BASIC"), readEnumValues(header));
	}

	@Test
	void shouldKeepDefaultValueForEnumHeader() throws Exception {
		Path javaRoot = this.tempDir.resolve("src/main/java");
		Path packageRoot = javaRoot.resolve("sample/jaxrs");
		Files.createDirectories(packageRoot);
		String source = "package sample.jaxrs;\n\n" + "import jakarta.ws.rs.DefaultValue;\n"
				+ "import jakarta.ws.rs.GET;\n" + "import jakarta.ws.rs.HeaderParam;\n"
				+ "import jakarta.ws.rs.Path;\n\n" + "@Path(\"/fixture\")\n"
				+ "public class EnumDefaultHeaderResource {\n" + "  enum Level { BASIC, PRO }\n\n" + "  @GET\n"
				+ "  @Path(\"/header-default\")\n" + "  public String headerDefault(\n"
				+ "      @HeaderParam(\"X-Level\") @DefaultValue(\"PRO\") Level level) {\n" + "    return \"ok\";\n"
				+ "  }\n" + "}\n";
		Files.writeString(packageRoot.resolve("EnumDefaultHeaderResource.java"), source, StandardCharsets.UTF_8);

		ProjectDocConfigBuilder builder = this.newBuilder(javaRoot);
		JavaMethod method = this.findMethod(builder, "sample.jaxrs.EnumDefaultHeaderResource", "headerDefault");
		String enumType = method.getParameters().get(0).getGenericFullyQualifiedName();

		ProjectDocConfigBuilder fallbackBuilder = Mockito.spy(builder);
		doReturn(true).when(fallbackBuilder).isEnumType(enumType);
		doReturn("BASIC").when(fallbackBuilder).getEnumSampleValue(enumType);
		doReturn(null).when(fallbackBuilder).getClassByName(enumType);

		List<ApiReqParam> headers = new JaxrsHeaderHandler().handle(method, fallbackBuilder);
		assertEquals(1, headers.size());

		ApiReqParam header = headers.get(0);
		assertEquals(ParamTypeConstants.PARAM_TYPE_ENUM, header.getType());
		assertEquals("PRO", header.getValue(), "Default value should have higher priority");
		assertEquals(List.of("BASIC"), readEnumValues(header));
	}

	private ProjectDocConfigBuilder newBuilder(Path javaRoot) {
		ApiConfig config = new ApiConfig();
		config.setSourceCodePaths(SourceCodePath.builder().setDesc("temp-source").setPath(javaRoot.toString()));
		config.setServerUrl("http://127.0.0.1:8080");
		return new ProjectDocConfigBuilder(config, null);
	}

	private JavaMethod findMethod(ProjectDocConfigBuilder builder, String className, String methodName) {
		JavaClass javaClass = builder.getClassByName(className);
		assertNotNull(javaClass, "Expected class to be loaded: " + className);
		return javaClass.getMethods()
			.stream()
			.filter(method -> methodName.equals(method.getName()))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Method not found: " + methodName));
	}

	@SuppressWarnings("unchecked")
	private static List<String> readEnumValues(ApiReqParam header) throws Exception {
		Field enumValues = ApiReqParam.class.getDeclaredField("enumValues");
		enumValues.setAccessible(true);
		Object value = enumValues.get(header);
		return value == null ? List.of() : (List<String>) value;
	}

}