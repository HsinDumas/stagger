/*
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.SourceCodePath;
import com.github.hsindumas.stagger.utils.DocUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for interface annotation inheritance in IRequestMappingHandler.
 *
 * @author HsinDumas
 */
class IRequestMappingHandlerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldIncludeInterfaceMethodAnnotations() throws Exception {
        Path javaRoot = this.tempDir.resolve("src/main/java");
        Path packageRoot = javaRoot.resolve("sample/mapping");
        Files.createDirectories(packageRoot);
        String api = "package sample.mapping;\n\n" + "public interface MappingApi {\n" + "  @Deprecated\n"
                + "  String ping(String id);\n" + "}\n";
        String impl = "package sample.mapping;\n\n" + "public class MappingApiImpl implements MappingApi {\n"
                + "  public String ping(String id) {\n" + "    return id;\n" + "  }\n" + "}\n";
        Files.writeString(packageRoot.resolve("MappingApi.java"), api, StandardCharsets.UTF_8);
        Files.writeString(packageRoot.resolve("MappingApiImpl.java"), impl, StandardCharsets.UTF_8);

        ProjectDocConfigBuilder builder = this.newBuilder(javaRoot);
        Object method = this.findMethod(builder, "sample.mapping.MappingApiImpl", "ping");

        List<?> annotations = new SpringMVCRequestMappingHandler().getAnnotations(method);
        assertTrue(annotations.stream().anyMatch(this::isDeprecated), "Interface method annotation should be included");
    }

    @Test
    void shouldMergeInterfaceAndImplementationMethodAnnotations() throws Exception {
        Path javaRoot = this.tempDir.resolve("src/main/java");
        Path packageRoot = javaRoot.resolve("sample/mapping");
        Files.createDirectories(packageRoot);
        String marker = "package sample.mapping;\n\n" + "public @interface Marker {}\n";
        String api = "package sample.mapping;\n\n" + "public interface MappingApi {\n" + "  @Deprecated\n"
                + "  String ping(String id);\n" + "}\n";
        String impl = "package sample.mapping;\n\n" + "public class MappingApiImpl implements MappingApi {\n"
                + "  @Marker\n" + "  public String ping(String id) {\n" + "    return id;\n" + "  }\n" + "}\n";
        Files.writeString(packageRoot.resolve("Marker.java"), marker, StandardCharsets.UTF_8);
        Files.writeString(packageRoot.resolve("MappingApi.java"), api, StandardCharsets.UTF_8);
        Files.writeString(packageRoot.resolve("MappingApiImpl.java"), impl, StandardCharsets.UTF_8);

        ProjectDocConfigBuilder builder = this.newBuilder(javaRoot);
        Object method = this.findMethod(builder, "sample.mapping.MappingApiImpl", "ping");

        List<?> annotations = new SpringMVCRequestMappingHandler().getAnnotations(method);
        Set<String> annotationNames =
                annotations.stream().map(DocUtil::getAnnotationTypeValue).collect(Collectors.toSet());
        assertTrue(
                annotationNames.contains("Deprecated"), "Interface annotation should still be present after merging");
        assertTrue(
                annotationNames.contains("Marker"), "Implementation method annotation should be present after merging");
    }

    private ProjectDocConfigBuilder newBuilder(Path javaRoot) {
        ApiConfig config = new ApiConfig();
        config.setSourceCodePaths(
                SourceCodePath.builder().setDesc("temp-source").setPath(javaRoot.toString()));
        config.setServerUrl("http://127.0.0.1:8080");
        return new ProjectDocConfigBuilder(config, null);
    }

    private Object findMethod(ProjectDocConfigBuilder builder, String className, String methodName) {
        Object javaClass = builder.getClassByName(className);
        assertNotNull(javaClass, "Expected class to be loaded: " + className);
        return DocUtil.getClassMethods(javaClass).stream()
                .filter(method -> methodName.equals(DocUtil.getMethodName(method)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Method not found: " + methodName));
    }

    private boolean isDeprecated(Object annotation) {
        return "Deprecated".equals(DocUtil.getAnnotationTypeValue(annotation))
                || "java.lang.Deprecated".equals(DocUtil.getAnnotationTypeFullyQualifiedName(annotation));
    }
}
