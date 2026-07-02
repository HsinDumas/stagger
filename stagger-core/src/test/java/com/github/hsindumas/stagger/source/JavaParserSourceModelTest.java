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
package com.github.hsindumas.stagger.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link com.github.hsindumas.stagger.source.javaparser.JavaParserSourceModel}.
 *
 * @author HsinDumas
 */
class JavaParserSourceModelTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldBuildProjectAndExtractClassMetadata() throws IOException {
		Path sourceRoot = this.tempDir.resolve("src/main/java/sample");
		Files.createDirectories(sourceRoot);
		String source = "package sample;\n\n" + "import java.util.List;\n\n" + "/**\n" + " * Demo class.\n"
				+ " * @author Demo\n" + " */\n" + "@Deprecated\n" + "@SuppressWarnings({\"unchecked\", \"rawtypes\"})\n"
				+ "public sealed class Demo permits DemoChild {\n" + "\t/** field comment */\n"
				+ "\tprivate List<String> names = List.of();\n\n" + "\t/**\n" + "\t * Hello method.\n"
				+ "\t * @param id identifier\n" + "\t */\n" + "\tpublic String hello(String id) {\n"
				+ "\t\treturn id;\n" + "\t}\n" + "}\n\n" + "final class DemoChild extends Demo {\n" + "}\n";
		Files.writeString(sourceRoot.resolve("Demo.java"), source, StandardCharsets.UTF_8);

		SourceScanRequest request = SourceScanRequest.builder()
			.addSourceRoot(this.tempDir.resolve("src/main/java"))
			.build();
		SourceProject project = SourceProjects.create().build(request);

		Optional<SourceClass> demoOptional = project.findClass("sample.Demo");
		assertTrue(demoOptional.isPresent(), "Demo class should be discovered");
		SourceClass demo = demoOptional.get();
		assertEquals("Demo", demo.simpleName());
		assertTrue(demo.isSealed(), "Demo should be marked as sealed");
		assertFalse(demo.permittedSubtypes().isEmpty(), "Permitted subtypes should be extracted");
		assertTrue(demo.fields().stream().anyMatch(field -> "names".equals(field.name())));

		SourceMethod helloMethod = demo.methods()
			.stream()
			.filter(method -> "hello".equals(method.name()))
			.findFirst()
			.orElseThrow();
		assertTrue(helloMethod.returnType().isPresent());
		assertEquals(1, helloMethod.parameters().size());
		assertEquals("id", helloMethod.parameters().get(0).name());

		SourceAnnotation suppress = demo.annotations()
			.stream()
			.filter(annotation -> annotation.qualifiedName().endsWith("SuppressWarnings"))
			.findFirst()
			.orElseThrow();
		Map<String, SourceAnnotationValue> members = suppress.members();
		assertTrue(members.containsKey("value"));
		List<SourceAnnotationValue> values = members.get("value").asList();
		assertEquals(2, values.size());
		assertEquals("unchecked", values.get(0).asString());
	}

	@Test
	void shouldDiscoverNestedTypes() throws IOException {
		Path sourceRoot = this.tempDir.resolve("src/main/java/sample");
		Files.createDirectories(sourceRoot);
		String source = "package sample;\n\n" + "class Outer {\n" + "\tstatic class Inner {\n" + "\t}\n" + "}\n";
		Files.writeString(sourceRoot.resolve("Outer.java"), source, StandardCharsets.UTF_8);

		SourceScanRequest request = SourceScanRequest.builder()
			.addSourceRoot(this.tempDir.resolve("src/main/java"))
			.build();
		SourceProject project = SourceProjects.create().build(request);

		Optional<SourceClass> outer = project.findClass("sample.Outer");
		Optional<SourceClass> inner = project.findClass("sample.Outer.Inner");
		assertTrue(outer.isPresent());
		assertTrue(inner.isPresent());
		assertNotNull(project.classes());
	}

}
