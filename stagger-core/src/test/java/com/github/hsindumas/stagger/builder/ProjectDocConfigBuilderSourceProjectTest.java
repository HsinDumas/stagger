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

package com.github.hsindumas.stagger.builder;

import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.SourceCodePath;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

/**
 * Tests for source abstraction integration in ProjectDocConfigBuilder.
 *
 * @author HsinDumas
 */
class ProjectDocConfigBuilderSourceProjectTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldBuildSourceProjectAlongsideQdoxModel() throws IOException {
		Path javaRoot = this.tempDir.resolve("src/main/java");
		Path packageRoot = javaRoot.resolve("demo");
		Files.createDirectories(packageRoot);
		String source = "package demo;\n\n" + "public class DemoController {\n" + "}\n";
		Files.writeString(packageRoot.resolve("DemoController.java"), source, StandardCharsets.UTF_8);

		ApiConfig config = new ApiConfig();
		config.setSourceCodePaths(SourceCodePath.builder().setDesc("temp-source").setPath(javaRoot.toString()));
		config.setServerUrl("http://127.0.0.1:8080");

		ProjectDocConfigBuilder configBuilder = new ProjectDocConfigBuilder(config, null);
		assertTrue(configBuilder.getSourceProject().findClass("demo.DemoController").isPresent(),
				"SourceProject should discover configured source class");
		assertNotNull(configBuilder.getClassByName("demo.DemoController"),
				"QDox model should remain available while SourceProject is enabled");
	}

	@Test
	void shouldDetectEnumTypeFromBuilderFacade() throws IOException {
		Path javaRoot = this.tempDir.resolve("src/main/java");
		Path packageRoot = javaRoot.resolve("demo");
		Files.createDirectories(packageRoot);
		String source = "package demo;\n\n" + "public class DemoController {\n"
				+ "  enum Level { BASIC, PRO }\n" + "}\n";
		Files.writeString(packageRoot.resolve("DemoController.java"), source, StandardCharsets.UTF_8);

		ApiConfig config = new ApiConfig();
		config.setSourceCodePaths(SourceCodePath.builder().setDesc("temp-source").setPath(javaRoot.toString()));
		config.setServerUrl("http://127.0.0.1:8080");

		ProjectDocConfigBuilder configBuilder = new ProjectDocConfigBuilder(config, null);
		assertTrue(configBuilder.isEnumType("demo.DemoController.Level"),
				"Builder facade should detect nested enum types");
		assertEquals("BASIC", configBuilder.getEnumSampleValue("demo.DemoController.Level"),
				"Builder facade should expose source-backed enum sample value");
		assertEquals("BASIC", configBuilder.getEnumSampleValue("Level"),
				"Builder facade should support simple-name lookup fallback");
		assertFalse(configBuilder.isEnumType("demo.DemoController"), "Non-enum class should not be treated as enum");
		assertNull(configBuilder.getEnumSampleValue("demo.DemoController"),
				"Non-enum class should not provide enum sample value");
	}

	@Test
	void shouldResolveImplementedInterfaceNamesFromFacade() throws IOException {
		Path javaRoot = this.tempDir.resolve("src/main/java");
		Path packageRoot = javaRoot.resolve("demo");
		Files.createDirectories(packageRoot);
		String api = "package demo;\n\n" + "public interface Api {}\n";
		String impl = "package demo;\n\n" + "public class Impl implements Api {}\n";
		Files.writeString(packageRoot.resolve("Api.java"), api, StandardCharsets.UTF_8);
		Files.writeString(packageRoot.resolve("Impl.java"), impl, StandardCharsets.UTF_8);

		ApiConfig config = new ApiConfig();
		config.setSourceCodePaths(SourceCodePath.builder().setDesc("temp-source").setPath(javaRoot.toString()));
		config.setServerUrl("http://127.0.0.1:8080");

		ProjectDocConfigBuilder configBuilder = new ProjectDocConfigBuilder(config, null);
		List<String> interfaces = configBuilder.getImplementedInterfaceNames("demo.Impl");
		assertTrue(interfaces.contains("demo.Api"), "Facade should resolve implemented interface names");
	}

	@Test
	void shouldResolveImplementedInterfaceNamesFromSourceFallback() throws IOException {
		Path javaRoot = this.tempDir.resolve("src/main/java");
		Path packageRoot = javaRoot.resolve("demo");
		Files.createDirectories(packageRoot);
		String api = "package demo;\n\n" + "public interface Api {}\n";
		String impl = "package demo;\n\n" + "public class Impl implements Api {}\n";
		Files.writeString(packageRoot.resolve("Api.java"), api, StandardCharsets.UTF_8);
		Files.writeString(packageRoot.resolve("Impl.java"), impl, StandardCharsets.UTF_8);

		ApiConfig config = new ApiConfig();
		config.setSourceCodePaths(SourceCodePath.builder().setDesc("temp-source").setPath(javaRoot.toString()));
		config.setServerUrl("http://127.0.0.1:8080");

		ProjectDocConfigBuilder configBuilder = new ProjectDocConfigBuilder(config, null);
		ProjectDocConfigBuilder fallbackBuilder = Mockito.spy(configBuilder);
		doReturn(null).when(fallbackBuilder).getClassByName("demo.Impl");

		List<String> interfaces = fallbackBuilder.getImplementedInterfaceNames("demo.Impl");
		assertTrue(interfaces.stream().anyMatch(name -> "demo.Api".equals(name) || "Api".equals(name)
				|| name.endsWith(".Api")),
				"Facade should fallback to SourceProject when QDox class lookup is unavailable");
	}

	@Test
	void shouldResolveImplementedInterfaceTypeArgumentFromFacade() throws IOException {
		Path javaRoot = this.tempDir.resolve("src/main/java");
		Path packageRoot = javaRoot.resolve("demo");
		Files.createDirectories(packageRoot);
		String encoder = "package demo;\n\n" + "public interface PayloadEncoder<T> {}\n";
		String payload = "package demo;\n\n" + "public class Payload {}\n";
		String impl = "package demo;\n\n" + "public class Impl implements PayloadEncoder<Payload> {}\n";
		Files.writeString(packageRoot.resolve("PayloadEncoder.java"), encoder, StandardCharsets.UTF_8);
		Files.writeString(packageRoot.resolve("Payload.java"), payload, StandardCharsets.UTF_8);
		Files.writeString(packageRoot.resolve("Impl.java"), impl, StandardCharsets.UTF_8);

		ApiConfig config = new ApiConfig();
		config.setSourceCodePaths(SourceCodePath.builder().setDesc("temp-source").setPath(javaRoot.toString()));
		config.setServerUrl("http://127.0.0.1:8080");

		ProjectDocConfigBuilder configBuilder = new ProjectDocConfigBuilder(config, null);
		Optional<String> typeArgument = configBuilder.getImplementedInterfaceTypeArgument("demo.Impl", 0,
				"demo.PayloadEncoder");
		assertTrue(typeArgument.isPresent(), "Facade should resolve implemented interface type argument");
		assertTrue(typeArgument.get().endsWith("Payload"), "Resolved type argument should reference payload type");
	}

	@Test
	void shouldResolveImplementedInterfaceTypeArgumentFromSourceFallback() throws IOException {
		Path javaRoot = this.tempDir.resolve("src/main/java");
		Path packageRoot = javaRoot.resolve("demo");
		Files.createDirectories(packageRoot);
		String encoder = "package demo;\n\n" + "public interface PayloadEncoder<T> {}\n";
		String payload = "package demo;\n\n" + "public class Payload {}\n";
		String impl = "package demo;\n\n" + "public class Impl implements PayloadEncoder<Payload> {}\n";
		Files.writeString(packageRoot.resolve("PayloadEncoder.java"), encoder, StandardCharsets.UTF_8);
		Files.writeString(packageRoot.resolve("Payload.java"), payload, StandardCharsets.UTF_8);
		Files.writeString(packageRoot.resolve("Impl.java"), impl, StandardCharsets.UTF_8);

		ApiConfig config = new ApiConfig();
		config.setSourceCodePaths(SourceCodePath.builder().setDesc("temp-source").setPath(javaRoot.toString()));
		config.setServerUrl("http://127.0.0.1:8080");

		ProjectDocConfigBuilder configBuilder = new ProjectDocConfigBuilder(config, null);
		ProjectDocConfigBuilder fallbackBuilder = Mockito.spy(configBuilder);
		doReturn(null).when(fallbackBuilder).getClassByName("demo.Impl");

		Optional<String> typeArgument = fallbackBuilder.getImplementedInterfaceTypeArgument("demo.Impl", 0,
				"demo.PayloadEncoder");
		assertTrue(typeArgument.isPresent(),
				"Facade should resolve type argument from SourceProject when QDox lookup is unavailable");
		assertTrue(typeArgument.get().endsWith("Payload"), "Source fallback should preserve payload type argument");
	}

	@Test
	void shouldResolveImplementedInterfaceTypeArgumentForNestedInterfaceNameFromFacade() throws IOException {
		Path javaRoot = this.tempDir.resolve("src/main/java");
		Path packageRoot = javaRoot.resolve("demo");
		Files.createDirectories(packageRoot);
		String outer = "package demo;\n\n" + "public interface Outer { interface Api<T> {} }\n";
		String payload = "package demo;\n\n" + "public class Payload {}\n";
		String impl = "package demo;\n\n" + "public class Impl implements Outer.Api<Payload> {}\n";
		Files.writeString(packageRoot.resolve("Outer.java"), outer, StandardCharsets.UTF_8);
		Files.writeString(packageRoot.resolve("Payload.java"), payload, StandardCharsets.UTF_8);
		Files.writeString(packageRoot.resolve("Impl.java"), impl, StandardCharsets.UTF_8);

		ApiConfig config = new ApiConfig();
		config.setSourceCodePaths(SourceCodePath.builder().setDesc("temp-source").setPath(javaRoot.toString()));
		config.setServerUrl("http://127.0.0.1:8080");

		ProjectDocConfigBuilder configBuilder = new ProjectDocConfigBuilder(config, null);
		Optional<String> typeArgument = configBuilder.getImplementedInterfaceTypeArgument("demo.Impl", 0,
				"demo.Outer$Api");
		assertTrue(typeArgument.isPresent(),
				"Facade should resolve nested interface names using binary-name style with '$'");
		assertTrue(typeArgument.get().endsWith("Payload"), "Resolved type argument should reference payload type");
	}

	@Test
	void shouldResolveImplementedInterfaceTypeArgumentForNestedInterfaceNameFromSourceFallback() throws IOException {
		Path javaRoot = this.tempDir.resolve("src/main/java");
		Path packageRoot = javaRoot.resolve("demo");
		Files.createDirectories(packageRoot);
		String outer = "package demo;\n\n" + "public interface Outer { interface Api<T> {} }\n";
		String payload = "package demo;\n\n" + "public class Payload {}\n";
		String impl = "package demo;\n\n" + "public class Impl implements Outer.Api<Payload> {}\n";
		Files.writeString(packageRoot.resolve("Outer.java"), outer, StandardCharsets.UTF_8);
		Files.writeString(packageRoot.resolve("Payload.java"), payload, StandardCharsets.UTF_8);
		Files.writeString(packageRoot.resolve("Impl.java"), impl, StandardCharsets.UTF_8);

		ApiConfig config = new ApiConfig();
		config.setSourceCodePaths(SourceCodePath.builder().setDesc("temp-source").setPath(javaRoot.toString()));
		config.setServerUrl("http://127.0.0.1:8080");

		ProjectDocConfigBuilder configBuilder = new ProjectDocConfigBuilder(config, null);
		ProjectDocConfigBuilder fallbackBuilder = Mockito.spy(configBuilder);
		doReturn(null).when(fallbackBuilder).getClassByName("demo.Impl");

		Optional<String> typeArgument = fallbackBuilder.getImplementedInterfaceTypeArgument("demo.Impl", 0,
				"demo.Outer$Api");
		assertTrue(typeArgument.isPresent(),
				"Source fallback should resolve nested interface names when candidate uses '$' style");
		assertTrue(typeArgument.get().endsWith("Payload"), "Source fallback should preserve payload type argument");
	}

}
