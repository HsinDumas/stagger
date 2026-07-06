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

import com.github.hsindumas.stagger.source.SourceAnnotation;
import com.github.hsindumas.stagger.source.SourceAnnotationValue;
import com.github.hsindumas.stagger.source.SourceClass;
import com.github.hsindumas.stagger.source.SourceDocletTag;
import com.github.hsindumas.stagger.source.SourceField;
import com.github.hsindumas.stagger.source.SourceMethod;
import com.github.hsindumas.stagger.source.SourceParameter;
import com.github.hsindumas.stagger.source.SourceProject;
import com.github.hsindumas.stagger.source.SourceProjects;
import com.github.hsindumas.stagger.source.SourceScanRequest;
import com.github.hsindumas.stagger.source.SourceType;
import com.github.hsindumas.stagger.source.SourceTypeParam;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * JavaProjectBuilder fallback delegate backed by SourceProject when legacy parser classes
 * are unavailable at runtime.
 *
 * @author HsinDumas
 */
public final class SourceProjectBuilderDelegate {

    private static final Set<String> PRIMITIVE_TYPE_NAMES =
            Set.of("byte", "short", "int", "long", "float", "double", "boolean", "char", "void");

    private Charset charset = StandardCharsets.UTF_8;

    private final Set<Path> sourceRoots = new LinkedHashSet<>();

    private final Set<Path> sourceFiles = new LinkedHashSet<>();

    private final Set<ClassLoader> runtimeClassLoaders = new LinkedHashSet<>();

    private Path extractedSourceRoot;

    private Consumer<Exception> errorHandler = exception -> {};

    private boolean dirty = true;

    private final Map<String, SourceJavaClass> classesByQualifiedName = new LinkedHashMap<>();

    private final Map<String, SourceJavaClass> classesByBinaryName = new LinkedHashMap<>();

    private final Map<String, List<SourceJavaClass>> classesBySimpleName = new HashMap<>();

    private final Map<String, SourceJavaClass> runtimeClassesByCanonicalName = new HashMap<>();

    private final Map<String, SourceJavaClass> runtimeClassesByBinaryName = new HashMap<>();

    public SourceProjectBuilderDelegate() {
        this.registerClassLoader(Thread.currentThread().getContextClassLoader());
        this.registerClassLoader(SourceProjectBuilderDelegate.class.getClassLoader());
        this.registerClassLoader(ClassLoader.getSystemClassLoader());
    }

    /**
     * Set source encoding.
     * @param encoding encoding name
     */
    public void setEncoding(String encoding) {
        if (encoding == null || encoding.isBlank()) {
            return;
        }
        try {
            this.charset = Charset.forName(encoding);
        } catch (RuntimeException ignored) {
            this.charset = StandardCharsets.UTF_8;
        }
    }

    /**
     * Add class loader. Fallback delegate currently keeps source-only class metadata and
     * does not require additional class loader wiring.
     * @param classLoader class loader
     */
    public void addClassLoader(ClassLoader classLoader) {
        this.registerClassLoader(classLoader);
    }

    /**
     * Add source root directory.
     * @param sourceRoot source root
     */
    public void addSourceTree(File sourceRoot) {
        if (sourceRoot == null) {
            return;
        }
        Path path = sourceRoot.toPath().normalize();
        if (this.sourceRoots.add(path)) {
            this.dirty = true;
        }
    }

    /**
     * Add source file.
     * @param sourceFile source file
     */
    public void addSource(File sourceFile) throws IOException {
        if (sourceFile == null) {
            return;
        }
        Path sourcePath = sourceFile.toPath().normalize();
        if (this.sourceFiles.add(sourcePath)) {
            this.dirty = true;
        }
    }

    /**
     * Add source URL.
     * @param sourceUrl source URL
     */
    public void addSource(URL sourceUrl) throws IOException {
        if (sourceUrl == null) {
            return;
        }
        if ("file".equalsIgnoreCase(sourceUrl.getProtocol())) {
            try {
                this.addSource(Path.of(sourceUrl.toURI()).toFile());
            } catch (URISyntaxException e) {
                throw new IOException("Invalid source URL: " + sourceUrl, e);
            }
            return;
        }
        if ("jar".equalsIgnoreCase(sourceUrl.getProtocol())) {
            Path extracted = this.extractJarSource(sourceUrl);
            if (extracted != null && this.sourceFiles.add(extracted)) {
                this.dirty = true;
            }
        }
    }

    private synchronized Path extractJarSource(URL sourceUrl) throws IOException {
        JarURLConnection connection = (JarURLConnection) sourceUrl.openConnection();
        String entryName = connection.getEntryName();
        if (entryName == null || !entryName.endsWith(".java")) {
            return null;
        }
        if (this.extractedSourceRoot == null) {
            this.extractedSourceRoot = Files.createTempDirectory("stagger-source-fallback-");
        }
        Path targetPath = this.extractedSourceRoot.resolve(entryName).normalize();
        if (!targetPath.startsWith(this.extractedSourceRoot)) {
            throw new IOException("Unsafe jar source entry path: " + entryName);
        }
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (InputStream inputStream = connection.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetPath;
    }

    /**
     * Set parser error handler callback.
     * @param errorHandler callback
     */
    public void setErrorHandler(Consumer<Exception> errorHandler) {
        if (errorHandler == null) {
            this.errorHandler = exception -> {};
            return;
        }
        this.errorHandler = errorHandler;
    }

    /**
     * Lookup class metadata by name.
     * @param className class name
     * @return class metadata or null
     */
    public Object getClassByName(String className) {
        this.rebuildIfNeeded();
        return this.resolveClass(className);
    }

    /**
     * List all discovered classes.
     * @return class metadata collection
     */
    public Collection<?> getClasses() {
        this.rebuildIfNeeded();
        return new ArrayList<>(this.classesByQualifiedName.values());
    }

    private synchronized void rebuildIfNeeded() {
        if (!this.dirty) {
            return;
        }
        this.dirty = false;
        this.classesByQualifiedName.clear();
        this.classesByBinaryName.clear();
        this.classesBySimpleName.clear();

        LinkedHashSet<Path> scanRoots = new LinkedHashSet<>(this.sourceRoots);
        for (Path sourceFile : this.sourceFiles) {
            Path parent = sourceFile.getParent();
            if (parent != null) {
                scanRoots.add(parent);
            }
        }
        if (scanRoots.isEmpty()) {
            return;
        }

        SourceScanRequest.Builder requestBuilder = SourceScanRequest.builder().setCharset(this.charset);
        for (Path scanRoot : scanRoots) {
            requestBuilder.addSourceRoot(scanRoot);
        }

        try {
            SourceProject sourceProject = SourceProjects.create().build(requestBuilder.build());
            for (SourceClass sourceClass : sourceProject.classes()) {
                SourceJavaClass classMetadata = new SourceJavaClass(sourceClass, this);
                this.classesByQualifiedName.put(classMetadata.getCanonicalName(), classMetadata);
                this.classesByBinaryName.put(classMetadata.getBinaryName(), classMetadata);
                this.classesBySimpleName
                        .computeIfAbsent(classMetadata.getName(), key -> new ArrayList<>())
                        .add(classMetadata);
            }
        } catch (RuntimeException e) {
            this.errorHandler.accept(new Exception("Unable to rebuild source-backed class index.", e));
        }
    }

    private SourceJavaClass resolveClass(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }
        String normalized = this.normalizeClassName(className);
        SourceJavaClass direct = this.classesByQualifiedName.get(normalized);
        if (direct != null) {
            return direct;
        }
        SourceJavaClass binary = this.classesByBinaryName.get(className);
        if (binary != null) {
            return binary;
        }
        binary = this.classesByBinaryName.get(normalized.replace('.', '$'));
        if (binary != null) {
            return binary;
        }
        List<SourceJavaClass> simpleMatches = this.classesBySimpleName.get(normalized);
        if (simpleMatches != null && !simpleMatches.isEmpty()) {
            return simpleMatches.get(0);
        }
        if (!normalized.contains(".")) {
            for (Map.Entry<String, SourceJavaClass> entry : this.classesByQualifiedName.entrySet()) {
                if (entry.getKey().endsWith("." + normalized)) {
                    return entry.getValue();
                }
            }
        }
        return this.resolveRuntimeClass(normalized);
    }

    String resolveTypeNameFromContext(String typeName, SourceJavaClass ownerClass) {
        if (typeName == null || typeName.isBlank()) {
            return "";
        }
        String normalized = this.normalizeClassName(typeName);
        if (normalized.isBlank()
                || normalized.startsWith("?")
                || normalized.contains(" ")
                || this.isPrimitiveName(normalized)) {
            return normalized;
        }
        if (normalized.contains(".")) {
            SourceJavaClass sourceClass = this.classesByQualifiedName.get(normalized);
            if (sourceClass != null) {
                return sourceClass.getCanonicalName();
            }
            if (this.resolveRuntimeClass(normalized) != null) {
                return normalized;
            }
            return normalized;
        }

        if (ownerClass != null) {
            for (String importName : ownerClass.imports()) {
                if (importName.endsWith("." + normalized)) {
                    return importName;
                }
                if (importName.endsWith(".*")) {
                    String wildcardCandidate = importName.substring(0, importName.length() - 2) + "." + normalized;
                    if (this.hasKnownType(wildcardCandidate)) {
                        return wildcardCandidate;
                    }
                }
            }

            String nestedCandidate = ownerClass.getCanonicalName() + "." + normalized;
            if (this.hasKnownType(nestedCandidate)) {
                return nestedCandidate;
            }

            String packageName = ownerClass.packageName();
            if (!packageName.isBlank()) {
                String packageCandidate = packageName + "." + normalized;
                if (this.hasKnownType(packageCandidate)) {
                    return packageCandidate;
                }
            }
        }

        List<SourceJavaClass> simpleMatches = this.classesBySimpleName.get(normalized);
        if (simpleMatches != null && !simpleMatches.isEmpty()) {
            return simpleMatches.get(0).getCanonicalName();
        }

        String javaLangType = "java.lang." + normalized;
        if (this.hasKnownType(javaLangType)) {
            return javaLangType;
        }
        return normalized;
    }

    private boolean hasKnownType(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return false;
        }
        if (this.classesByQualifiedName.containsKey(qualifiedName)
                || this.classesByBinaryName.containsKey(qualifiedName.replace('.', '$'))
                || this.runtimeClassesByCanonicalName.containsKey(qualifiedName)
                || this.runtimeClassesByBinaryName.containsKey(qualifiedName)) {
            return true;
        }
        return this.resolveRuntimeClass(qualifiedName) != null;
    }

    private void registerClassLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            this.runtimeClassLoaders.add(classLoader);
        }
    }

    private SourceJavaClass resolveRuntimeClass(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }
        String normalized = this.normalizeClassName(className);
        SourceJavaClass cached = this.runtimeClassesByCanonicalName.get(normalized);
        if (cached != null) {
            return cached;
        }
        cached = this.runtimeClassesByBinaryName.get(className);
        if (cached != null) {
            return cached;
        }
        for (String candidate : this.runtimeClassCandidates(normalized)) {
            Class<?> runtimeClass = this.tryLoadRuntimeClass(candidate);
            if (runtimeClass != null) {
                return this.cacheRuntimeClass(runtimeClass);
            }
        }
        return null;
    }

    private SourceJavaClass cacheRuntimeClass(Class<?> runtimeClass) {
        String binaryName = runtimeClass.getName();
        SourceJavaClass cached = this.runtimeClassesByBinaryName.get(binaryName);
        if (cached != null) {
            return cached;
        }
        SourceJavaClass runtimeMetadata = new SourceJavaClass(new ReflectionSourceClass(runtimeClass), this);
        this.runtimeClassesByBinaryName.put(binaryName, runtimeMetadata);
        this.runtimeClassesByBinaryName.put(runtimeMetadata.getBinaryName(), runtimeMetadata);
        this.runtimeClassesByCanonicalName.put(runtimeMetadata.getCanonicalName(), runtimeMetadata);
        this.runtimeClassesByCanonicalName.put(runtimeMetadata.getFullyQualifiedName(), runtimeMetadata);
        return runtimeMetadata;
    }

    private List<String> runtimeClassCandidates(String className) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String normalized = className.replace('$', '.');
        candidates.add(className);
        candidates.add(normalized);
        String nestedCandidate = normalized;
        while (nestedCandidate.contains(".")) {
            int dotIndex = nestedCandidate.lastIndexOf('.');
            nestedCandidate = nestedCandidate.substring(0, dotIndex) + "$" + nestedCandidate.substring(dotIndex + 1);
            candidates.add(nestedCandidate);
        }
        return new ArrayList<>(candidates);
    }

    private Class<?> tryLoadRuntimeClass(String className) {
        for (ClassLoader classLoader : this.runtimeClassLoaders) {
            try {
                return Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException | LinkageError ignored) {
                // Keep trying with other registered class loaders.
            }
        }
        return null;
    }

    String normalizeClassName(String className) {
        String normalized = this.stripGenerics(className.trim());
        int arrayIndex = normalized.indexOf('[');
        if (arrayIndex > -1) {
            normalized = normalized.substring(0, arrayIndex);
        }
        return normalized.replace('$', '.');
    }

    String stripGenerics(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int nested = 0;
        for (int i = 0; i < typeName.length(); i++) {
            char ch = typeName.charAt(i);
            if (ch == '<') {
                nested++;
                continue;
            }
            if (ch == '>') {
                if (nested > 0) {
                    nested--;
                }
                continue;
            }
            if (nested == 0) {
                builder.append(ch);
            }
        }
        return builder.toString().trim();
    }

    String packageNameOf(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return "";
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        String candidate = qualifiedName.substring(0, lastDot);
        while (candidate.contains(".")) {
            if (this.classesByQualifiedName.containsKey(candidate)) {
                int dot = candidate.lastIndexOf('.');
                return dot > -1 ? candidate.substring(0, dot) : "";
            }
            candidate = candidate.substring(0, candidate.lastIndexOf('.'));
        }
        return qualifiedName.substring(0, lastDot);
    }

    String simpleNameOf(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return "";
        }
        String normalized = this.normalizeClassName(qualifiedName);
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot < 0) {
            return normalized;
        }
        return normalized.substring(lastDot + 1);
    }

    String toBinaryName(String qualifiedName) {
        String normalized = this.normalizeClassName(qualifiedName);
        if (normalized.isEmpty()) {
            return normalized;
        }
        for (int dotIndex = normalized.lastIndexOf('.');
                dotIndex > 0;
                dotIndex = normalized.lastIndexOf('.', dotIndex - 1)) {
            String prefix = normalized.substring(0, dotIndex);
            if (this.classesByQualifiedName.containsKey(prefix)) {
                String nestedPart = normalized.substring(dotIndex + 1).replace('.', '$');
                return prefix + "$" + nestedPart;
            }
        }
        return normalized;
    }

    boolean isPrimitiveName(String fullyQualifiedName) {
        return PRIMITIVE_TYPE_NAMES.contains(this.normalizeClassName(fullyQualifiedName));
    }

    List<SourceJavaClass> findNestedClasses(String ownerQualifiedName) {
        if (ownerQualifiedName == null || ownerQualifiedName.isBlank()) {
            return Collections.emptyList();
        }
        String prefix = this.normalizeClassName(ownerQualifiedName) + ".";
        List<SourceJavaClass> nested = new ArrayList<>();
        for (SourceJavaClass sourceJavaClass : this.classesByQualifiedName.values()) {
            String className = sourceJavaClass.getCanonicalName();
            if (!className.startsWith(prefix)) {
                continue;
            }
            String remainder = className.substring(prefix.length());
            if (!remainder.isEmpty() && !remainder.contains(".")) {
                nested.add(sourceJavaClass);
            }
        }
        return nested;
    }

    List<String> readImports(SourceClass sourceClass) {
        Object declaration = this.readDeclaredField(sourceClass, "declaration");
        if (declaration == null) {
            return Collections.emptyList();
        }
        try {
            Object optionalCompilationUnit =
                    declaration.getClass().getMethod("findCompilationUnit").invoke(declaration);
            if (!(optionalCompilationUnit instanceof Optional)) {
                return Collections.emptyList();
            }
            Optional<?> compilationUnitOptional = (Optional<?>) optionalCompilationUnit;
            if (compilationUnitOptional.isEmpty()) {
                return Collections.emptyList();
            }
            Object compilationUnit = compilationUnitOptional.get();
            Object importDeclarations =
                    compilationUnit.getClass().getMethod("getImports").invoke(compilationUnit);
            if (!(importDeclarations instanceof Iterable<?>)) {
                return Collections.emptyList();
            }
            List<String> imports = new ArrayList<>();
            for (Object importDeclaration : (Iterable<?>) importDeclarations) {
                Object importName = importDeclaration
                        .getClass()
                        .getMethod("getNameAsString")
                        .invoke(importDeclaration);
                if (importName != null) {
                    imports.add(importName.toString());
                }
            }
            return imports;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    boolean readFieldModifier(SourceField sourceField, String methodName) {
        Object fieldDeclaration = this.readDeclaredField(sourceField, "fieldDeclaration");
        if (fieldDeclaration == null) {
            return false;
        }
        try {
            Object value = fieldDeclaration.getClass().getMethod(methodName).invoke(fieldDeclaration);
            return value instanceof Boolean && (Boolean) value;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    Object readDeclaredField(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    String typeNameOf(Object typeObject) {
        if (typeObject == null) {
            return "";
        }
        if (typeObject instanceof SourceJavaType) {
            return ((SourceJavaType) typeObject).getFullyQualifiedName();
        }
        if (typeObject instanceof String) {
            return this.normalizeClassName((String) typeObject);
        }
        for (String accessor : List.of("getFullyQualifiedName", "getCanonicalName", "getValue", "toString")) {
            try {
                Object value = "toString".equals(accessor)
                        ? typeObject.toString()
                        : typeObject.getClass().getMethod(accessor).invoke(typeObject);
                if (value != null) {
                    return this.normalizeClassName(value.toString());
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Ignore and try next accessor.
            }
        }
        return "";
    }

    boolean matchTypeName(String expectedTypeName, String actualTypeName) {
        String expected = this.normalizeClassName(expectedTypeName);
        String actual = this.normalizeClassName(actualTypeName);
        if (expected.equals(actual)) {
            return true;
        }
        if (expected.isEmpty() || actual.isEmpty()) {
            return false;
        }
        return this.simpleNameOf(expected).equals(this.simpleNameOf(actual));
    }

    boolean isAssignableTo(SourceJavaClass source, String targetTypeName, Set<String> visited) {
        if (source == null || targetTypeName == null || targetTypeName.isBlank()) {
            return false;
        }
        String target = this.normalizeClassName(targetTypeName);
        String sourceName = source.getCanonicalName();
        if (!visited.add(sourceName)) {
            return false;
        }
        if (this.matchTypeName(target, sourceName)) {
            return true;
        }
        Object parentClass = source.getSuperJavaClass();
        if (parentClass instanceof SourceJavaClass
                && this.isAssignableTo((SourceJavaClass) parentClass, target, visited)) {
            return true;
        }
        for (Object interfaceType : source.getInterfaces()) {
            Object interfaceClass = interfaceType;
            if (interfaceType instanceof SourceJavaType) {
                interfaceClass = ((SourceJavaType) interfaceType).getType();
            }
            if (interfaceClass instanceof SourceJavaClass
                    && this.isAssignableTo((SourceJavaClass) interfaceClass, target, visited)) {
                return true;
            }
        }
        return false;
    }

    List<SourceJavaAnnotation> wrapAnnotations(List<SourceAnnotation> annotations) {
        return this.wrapAnnotations(annotations, null);
    }

    List<SourceJavaAnnotation> wrapAnnotations(List<SourceAnnotation> annotations, SourceJavaClass ownerClass) {
        if (annotations == null || annotations.isEmpty()) {
            return Collections.emptyList();
        }
        return annotations.stream()
                .map(annotation -> new SourceJavaAnnotation(annotation, this, ownerClass))
                .collect(Collectors.toList());
    }

    List<SourceJavaDocletTag> wrapDocletTags(List<SourceDocletTag> docletTags) {
        if (docletTags == null || docletTags.isEmpty()) {
            return Collections.emptyList();
        }
        return docletTags.stream().map(SourceJavaDocletTag::new).collect(Collectors.toList());
    }

    List<SourceJavaType> wrapTypes(List<SourceType> sourceTypes, SourceJavaClass ownerClass) {
        if (sourceTypes == null || sourceTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return sourceTypes.stream()
                .map(type -> new SourceJavaType(type, this, ownerClass))
                .collect(Collectors.toList());
    }

    List<SourceJavaTypeParameter> wrapTypeParameters(List<SourceTypeParam> sourceTypeParams) {
        if (sourceTypeParams == null || sourceTypeParams.isEmpty()) {
            return Collections.emptyList();
        }
        return sourceTypeParams.stream()
                .map(typeParam -> new SourceJavaTypeParameter(typeParam.name()))
                .collect(Collectors.toList());
    }

    public static final class SourceJavaClass {

        private final SourceClass sourceClass;

        private final SourceProjectBuilderDelegate delegate;

        private final String canonicalName;

        private final String binaryName;

        private final String packageName;

        private final List<String> imports;

        private List<SourceJavaMethod> methods;

        private List<SourceJavaField> fields;

        private List<SourceJavaField> enumConstants;

        private List<SourceJavaAnnotation> annotations;

        private List<SourceJavaDocletTag> docletTags;

        private List<SourceJavaType> interfaces;

        private List<SourceJavaTypeParameter> typeParameters;

        private SourceJavaClass(SourceClass sourceClass, SourceProjectBuilderDelegate delegate) {
            this.sourceClass = sourceClass;
            this.delegate = delegate;
            this.canonicalName = delegate.normalizeClassName(sourceClass.qualifiedName());
            this.binaryName = delegate.toBinaryName(this.canonicalName);
            this.packageName = delegate.packageNameOf(this.canonicalName);
            this.imports = Collections.unmodifiableList(new ArrayList<>(delegate.readImports(sourceClass)));
        }

        public String getName() {
            return this.sourceClass.simpleName();
        }

        String packageName() {
            return this.packageName;
        }

        List<String> imports() {
            return this.imports;
        }

        public String getCanonicalName() {
            return this.canonicalName;
        }

        public String getFullyQualifiedName() {
            return this.canonicalName;
        }

        public String getGenericFullyQualifiedName() {
            return this.canonicalName;
        }

        public String getBinaryName() {
            return this.binaryName;
        }

        public SourceJavaPackage getPackage() {
            return new SourceJavaPackage(this.packageName);
        }

        public List<?> getMethods() {
            if (this.methods == null) {
                this.methods = this.sourceClass.methods().stream()
                        .filter(method -> !method.isConstructor())
                        .map(method -> new SourceJavaMethod(method, this, this.delegate))
                        .collect(Collectors.toList());
            }
            return this.methods;
        }

        public List<?> getFields() {
            if (this.fields == null) {
                this.fields = this.sourceClass.fields().stream()
                        .map(field -> new SourceJavaField(field, this, this.delegate))
                        .collect(Collectors.toList());
            }
            return this.fields;
        }

        public List<?> getEnumConstants() {
            if (!this.sourceClass.isEnum()) {
                return Collections.emptyList();
            }
            if (this.enumConstants == null) {
                this.enumConstants = this.sourceClass.enumConstants().stream()
                        .map(name -> SourceJavaField.enumConstant(name, this, this.delegate))
                        .collect(Collectors.toList());
            }
            return this.enumConstants;
        }

        public Object getSuperJavaClass() {
            return this.sourceClass
                    .superType()
                    .map(superType -> this.delegate.resolveClass(superType.qualifiedName()))
                    .orElse(null);
        }

        public List<?> getNestedClasses() {
            return this.delegate.findNestedClasses(this.canonicalName);
        }

        public List<?> getAnnotations() {
            if (this.annotations == null) {
                this.annotations = this.delegate.wrapAnnotations(this.sourceClass.annotations(), this);
            }
            return this.annotations;
        }

        public List<?> getTags() {
            if (this.docletTags == null) {
                this.docletTags = this.delegate.wrapDocletTags(this.sourceClass.docletTags());
            }
            return this.docletTags;
        }

        public List<?> getTagsByName(String tagName) {
            if (tagName == null || tagName.isBlank()) {
                return Collections.emptyList();
            }
            return this.getTags().stream()
                    .map(SourceJavaDocletTag.class::cast)
                    .filter(tag -> tagName.equals(tag.getName()))
                    .collect(Collectors.toList());
        }

        public Object getTagByName(String tagName) {
            List<?> tags = this.getTagsByName(tagName);
            if (tags.isEmpty()) {
                return null;
            }
            return tags.get(0);
        }

        public String getComment() {
            return this.sourceClass.comment();
        }

        public boolean isAnnotation() {
            return this.sourceClass.isAnnotation();
        }

        public boolean isEnum() {
            return this.sourceClass.isEnum();
        }

        public boolean isInterface() {
            return this.sourceClass.isInterface();
        }

        public boolean isRecord() {
            return this.sourceClass.isRecord();
        }

        public List<?> getImplements() {
            if (this.interfaces == null) {
                this.interfaces = this.delegate.wrapTypes(this.sourceClass.interfaces(), this);
            }
            return this.interfaces;
        }

        public List<?> getInterfaces() {
            List<Object> interfaceClasses = new ArrayList<>();
            for (Object typeObject : this.getImplements()) {
                if (!(typeObject instanceof SourceJavaType)) {
                    continue;
                }
                Object interfaceClass = ((SourceJavaType) typeObject).getType();
                if (interfaceClass instanceof SourceJavaClass) {
                    interfaceClasses.add(interfaceClass);
                }
            }
            return interfaceClasses;
        }

        public List<?> getTypeParameters() {
            if (this.typeParameters == null) {
                this.typeParameters = this.delegate.wrapTypeParameters(this.sourceClass.typeParameters());
            }
            return this.typeParameters;
        }

        public Object getMethod(String methodName, List<?> parameterTypes, boolean varArgs) {
            List<?> methodsBySignature = this.getMethodsBySignature(methodName, parameterTypes, true, varArgs);
            if (methodsBySignature.isEmpty()) {
                return null;
            }
            return methodsBySignature.get(0);
        }

        public List<?> getMethodsBySignature(
                String methodName, List<?> parameterTypes, boolean includeInherited, boolean varArgs) {
            if (methodName == null || methodName.isBlank()) {
                return Collections.emptyList();
            }
            List<SourceJavaMethod> candidates = new ArrayList<>();
            for (Object methodObject : this.getMethods()) {
                SourceJavaMethod method = (SourceJavaMethod) methodObject;
                if (!methodName.equals(method.getName())) {
                    continue;
                }
                if (varArgs != method.isVarArgs()) {
                    continue;
                }
                if (!method.matchesParameters(parameterTypes)) {
                    continue;
                }
                candidates.add(method);
            }

            if (!includeInherited) {
                return candidates;
            }

            Object parent = this.getSuperJavaClass();
            if (parent instanceof SourceJavaClass) {
                this.addSourceMethods(
                        candidates,
                        ((SourceJavaClass) parent).getMethodsBySignature(methodName, parameterTypes, true, varArgs));
            }
            for (Object interfaceClass : this.getInterfaces()) {
                if (interfaceClass instanceof SourceJavaClass) {
                    this.addSourceMethods(
                            candidates,
                            ((SourceJavaClass) interfaceClass)
                                    .getMethodsBySignature(methodName, parameterTypes, true, varArgs));
                }
            }
            return candidates;
        }

        private void addSourceMethods(List<SourceJavaMethod> target, List<?> sourceMethods) {
            for (Object sourceMethod : sourceMethods) {
                if (sourceMethod instanceof SourceJavaMethod) {
                    target.add((SourceJavaMethod) sourceMethod);
                }
            }
        }

        public SourceJavaSource getSource() {
            return new SourceJavaSource(this.imports);
        }

        public boolean isA(String typeName) {
            return this.delegate.isAssignableTo(this, typeName, new HashSet<>());
        }

        @Override
        public String toString() {
            return this.canonicalName;
        }
    }

    public static final class SourceJavaMethod {

        private final SourceMethod sourceMethod;

        private final SourceJavaClass declaringClass;

        private final SourceProjectBuilderDelegate delegate;

        private final List<SourceJavaParameter> parameters;

        private final List<SourceJavaAnnotation> annotations;

        private final List<SourceJavaDocletTag> docletTags;

        private final SourceJavaType returnType;

        private SourceJavaMethod(
                SourceMethod sourceMethod, SourceJavaClass declaringClass, SourceProjectBuilderDelegate delegate) {
            this.sourceMethod = sourceMethod;
            this.declaringClass = declaringClass;
            this.delegate = delegate;
            this.parameters = sourceMethod.parameters().stream()
                    .map(parameter -> new SourceJavaParameter(parameter, delegate, declaringClass))
                    .collect(Collectors.toList());
            this.annotations = delegate.wrapAnnotations(sourceMethod.annotations(), declaringClass);
            this.docletTags = delegate.wrapDocletTags(sourceMethod.docletTags());
            this.returnType = sourceMethod
                    .returnType()
                    .map(type -> new SourceJavaType(type, delegate, declaringClass))
                    .orElseGet(() -> new SourceJavaType(declaringClass.getCanonicalName(), delegate));
        }

        public String getName() {
            return this.sourceMethod.name();
        }

        public String getComment() {
            return this.sourceMethod.comment();
        }

        public List<?> getParameters() {
            return this.parameters;
        }

        public List<?> getParameterTypes() {
            return this.parameters.stream().map(SourceJavaParameter::getType).collect(Collectors.toList());
        }

        public List<?> getAnnotations() {
            return this.annotations;
        }

        public List<?> getTags() {
            return this.docletTags;
        }

        public List<?> getTagsByName(String tagName) {
            if (tagName == null || tagName.isBlank()) {
                return Collections.emptyList();
            }
            return this.docletTags.stream()
                    .filter(tag -> tagName.equals(tag.getName()))
                    .collect(Collectors.toList());
        }

        public Object getTagByName(String tagName) {
            List<?> tags = this.getTagsByName(tagName);
            if (tags.isEmpty()) {
                return null;
            }
            return tags.get(0);
        }

        public Object getReturnType() {
            return this.returnType;
        }

        public Object getDeclaringClass() {
            return this.declaringClass;
        }

        public List<?> getModifiers() {
            return Collections.singletonList("public");
        }

        public boolean isPrivate() {
            return false;
        }

        public boolean isDefault() {
            return false;
        }

        public boolean isVarArgs() {
            return this.parameters.stream().anyMatch(SourceJavaParameter::isVarArgs);
        }

        public String getDeclarationSignature(boolean withModifiers) {
            String parameterSignature = this.parameters.stream()
                    .map(parameter ->
                            ((SourceJavaType) parameter.getType()).getSimpleName() + " " + parameter.getName())
                    .collect(Collectors.joining(", "));
            String signature = this.returnType.getSimpleName() + " " + this.getName() + "(" + parameterSignature + ")";
            if (!withModifiers) {
                return signature;
            }
            return "public " + signature;
        }

        private boolean matchesParameters(List<?> expectedTypes) {
            List<?> parameterTypes = this.getParameterTypes();
            if (expectedTypes == null) {
                return parameterTypes.isEmpty();
            }
            if (parameterTypes.size() != expectedTypes.size()) {
                return false;
            }
            for (int i = 0; i < parameterTypes.size(); i++) {
                String expected = this.delegate.typeNameOf(expectedTypes.get(i));
                String actual = this.delegate.typeNameOf(parameterTypes.get(i));
                if (!this.delegate.matchTypeName(expected, actual)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static final class SourceJavaField {

        private final SourceJavaClass declaringClass;

        private final String name;

        private final SourceJavaType type;

        private final List<SourceJavaAnnotation> annotations;

        private final String comment;

        private final String initializationExpression;

        private final boolean staticField;

        private final boolean transientField;

        private final boolean privateField;

        private final boolean finalField;

        private SourceJavaField(
                SourceField sourceField, SourceJavaClass declaringClass, SourceProjectBuilderDelegate delegate) {
            this.declaringClass = declaringClass;
            this.name = sourceField.name();
            this.type = new SourceJavaType(sourceField.type(), delegate, declaringClass);
            this.annotations = delegate.wrapAnnotations(sourceField.annotations(), declaringClass);
            this.comment = sourceField.comment();
            this.initializationExpression = sourceField.initializer().orElse("");
            this.staticField = delegate.readFieldModifier(sourceField, "isStatic");
            this.transientField = delegate.readFieldModifier(sourceField, "isTransient");
            this.privateField = delegate.readFieldModifier(sourceField, "isPrivate");
            this.finalField = delegate.readFieldModifier(sourceField, "isFinal");
        }

        private SourceJavaField(
                String enumConstantName, SourceJavaClass declaringClass, SourceProjectBuilderDelegate delegate) {
            this.declaringClass = declaringClass;
            this.name = enumConstantName;
            this.type = new SourceJavaType(declaringClass.getCanonicalName(), delegate);
            this.annotations = Collections.emptyList();
            this.comment = "";
            this.initializationExpression = "";
            this.staticField = true;
            this.transientField = false;
            this.privateField = false;
            this.finalField = true;
        }

        private static SourceJavaField enumConstant(
                String enumConstantName, SourceJavaClass declaringClass, SourceProjectBuilderDelegate delegate) {
            return new SourceJavaField(enumConstantName, declaringClass, delegate);
        }

        public String getName() {
            return this.name;
        }

        public Object getType() {
            return this.type;
        }

        public List<?> getAnnotations() {
            return this.annotations;
        }

        public String getComment() {
            return this.comment;
        }

        public List<?> getTags() {
            return Collections.emptyList();
        }

        public Object getTagByName(String tagName) {
            return null;
        }

        public String getInitializationExpression() {
            return this.initializationExpression;
        }

        public Object getDeclaringClass() {
            return this.declaringClass;
        }

        public boolean isStatic() {
            return this.staticField;
        }

        public boolean isTransient() {
            return this.transientField;
        }

        public boolean isPrivate() {
            return this.privateField;
        }

        public boolean isFinal() {
            return this.finalField;
        }
    }

    public static final class SourceJavaParameter {

        private final SourceParameter sourceParameter;

        private final SourceJavaType type;

        private final List<SourceJavaAnnotation> annotations;

        private SourceJavaParameter(
                SourceParameter sourceParameter, SourceProjectBuilderDelegate delegate, SourceJavaClass ownerClass) {
            this.sourceParameter = sourceParameter;
            this.type = new SourceJavaType(sourceParameter.type(), delegate, ownerClass);
            this.annotations = delegate.wrapAnnotations(sourceParameter.annotations(), ownerClass);
        }

        public String getName() {
            return this.sourceParameter.name();
        }

        public Object getType() {
            return this.type;
        }

        public boolean isVarArgs() {
            return this.sourceParameter.isVarArgs();
        }

        public List<?> getAnnotations() {
            return this.annotations;
        }

        public String getFullyQualifiedName() {
            return this.type.getFullyQualifiedName();
        }

        public String getGenericFullyQualifiedName() {
            return this.type.getGenericFullyQualifiedName();
        }
    }

    public static final class SourceJavaType {

        private final SourceProjectBuilderDelegate delegate;

        private final String genericFullyQualifiedName;

        private final String fullyQualifiedName;

        private final String binaryName;

        private final List<SourceJavaType> actualTypeArguments;

        private final SourceJavaClass ownerClass;

        private SourceJavaType(SourceType sourceType, SourceProjectBuilderDelegate delegate) {
            this(sourceType, delegate, null);
        }

        private SourceJavaType(
                SourceType sourceType, SourceProjectBuilderDelegate delegate, SourceJavaClass ownerClass) {
            this(
                    delegate.buildTypeName(sourceType),
                    delegate,
                    sourceType.typeArguments().stream()
                            .map(type -> new SourceJavaType(type, delegate, ownerClass))
                            .collect(Collectors.toList()),
                    ownerClass);
        }

        private SourceJavaType(String typeName, SourceProjectBuilderDelegate delegate) {
            this(typeName, delegate, Collections.emptyList(), null);
        }

        private SourceJavaType(
                String typeName,
                SourceProjectBuilderDelegate delegate,
                List<SourceJavaType> actualTypeArguments,
                SourceJavaClass ownerClass) {
            this.delegate = delegate;
            this.genericFullyQualifiedName = typeName == null ? "" : typeName;
            this.ownerClass = ownerClass;
            this.fullyQualifiedName = delegate.resolveTypeNameFromContext(
                    delegate.stripGenerics(this.genericFullyQualifiedName), ownerClass);
            this.binaryName = delegate.toBinaryName(this.fullyQualifiedName);
            this.actualTypeArguments = actualTypeArguments;
        }

        public String getValue() {
            return this.genericFullyQualifiedName;
        }

        public String getGenericValue() {
            return this.genericFullyQualifiedName;
        }

        public String getCanonicalName() {
            return this.fullyQualifiedName;
        }

        public String getGenericCanonicalName() {
            return this.genericFullyQualifiedName;
        }

        public String getFullyQualifiedName() {
            return this.fullyQualifiedName;
        }

        public String getGenericFullyQualifiedName() {
            return this.genericFullyQualifiedName;
        }

        public String getBinaryName() {
            return this.binaryName;
        }

        public String getSimpleName() {
            return this.delegate.simpleNameOf(this.fullyQualifiedName);
        }

        public boolean isEnum() {
            SourceJavaClass sourceJavaClass = this.delegate.resolveClass(this.fullyQualifiedName);
            return sourceJavaClass != null && sourceJavaClass.isEnum();
        }

        public boolean isPrimitive() {
            return this.delegate.isPrimitiveName(this.fullyQualifiedName);
        }

        public boolean isArray() {
            return this.genericFullyQualifiedName.endsWith("[]");
        }

        public List<?> getActualTypeArguments() {
            return this.actualTypeArguments;
        }

        public Object getType() {
            SourceJavaClass sourceJavaClass = this.delegate.resolveClass(this.fullyQualifiedName);
            if (sourceJavaClass != null) {
                return sourceJavaClass;
            }
            if (this.ownerClass != null) {
                SourceJavaClass ownerResolved = this.delegate.resolveClass(this.ownerClass.getCanonicalName());
                if (ownerResolved != null) {
                    return ownerResolved;
                }
            }
            return this;
        }

        @Override
        public String toString() {
            return this.genericFullyQualifiedName;
        }
    }

    private String buildTypeName(SourceType sourceType) {
        String qualifiedName = sourceType.qualifiedName();
        if (!qualifiedName.contains("<") && !sourceType.typeArguments().isEmpty()) {
            String typeArguments = sourceType.typeArguments().stream()
                    .map(argument -> this.buildTypeName(argument))
                    .collect(Collectors.joining(", "));
            qualifiedName = this.stripGenerics(qualifiedName) + "<" + typeArguments + ">";
        }
        if (sourceType.isArray() && !qualifiedName.endsWith("[]")) {
            qualifiedName += "[]";
        }
        return qualifiedName;
    }

    public static final class SourceJavaTypeParameter {

        private final String name;

        private SourceJavaTypeParameter(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public static final class SourceJavaAnnotation {

        private final SourceAnnotation sourceAnnotation;

        private final SourceProjectBuilderDelegate delegate;

        private final SourceJavaAnnotationType type;

        private SourceJavaAnnotation(
                SourceAnnotation sourceAnnotation, SourceProjectBuilderDelegate delegate, SourceJavaClass ownerClass) {
            this.sourceAnnotation = sourceAnnotation;
            this.delegate = delegate;
            this.type = new SourceJavaAnnotationType(sourceAnnotation.qualifiedName(), delegate, ownerClass);
        }

        public Object getType() {
            return this.type;
        }

        public Object getProperty(String propertyName) {
            return this.convertValue(this.sourceAnnotation.members().get(propertyName));
        }

        public Object getNamedParameter(String propertyName) {
            return this.convertValue(this.sourceAnnotation.members().get(propertyName));
        }

        public Map<String, Object> getNamedParameterMap() {
            return this.getPropertyMap();
        }

        public Map<String, Object> getPropertyMap() {
            if (this.sourceAnnotation.members().isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Object> propertyMap = new LinkedHashMap<>();
            for (Map.Entry<String, SourceAnnotationValue> entry :
                    this.sourceAnnotation.members().entrySet()) {
                propertyMap.put(entry.getKey(), this.convertMapValue(entry.getValue()));
            }
            return propertyMap;
        }

        private Object convertValue(SourceAnnotationValue sourceValue) {
            if (sourceValue == null) {
                return null;
            }
            return new SourceJavaAnnotationValue(sourceValue, this.delegate);
        }

        private Object convertMapValue(SourceAnnotationValue sourceValue) {
            if (sourceValue == null) {
                return null;
            }
            List<SourceAnnotationValue> listValues = sourceValue.asList();
            if (!listValues.isEmpty()) {
                LinkedList<String> values = new LinkedList<>();
                for (SourceAnnotationValue value : listValues) {
                    values.add(value.asString());
                }
                return values;
            }
            return sourceValue.asString();
        }
    }

    public static final class SourceJavaAnnotationType {

        private final String qualifiedName;

        private final SourceProjectBuilderDelegate delegate;

        private SourceJavaAnnotationType(
                String qualifiedName, SourceProjectBuilderDelegate delegate, SourceJavaClass ownerClass) {
            this.qualifiedName = delegate.resolveTypeNameFromContext(qualifiedName, ownerClass);
            this.delegate = delegate;
        }

        public String getValue() {
            return this.delegate.simpleNameOf(this.qualifiedName);
        }

        public String getFullyQualifiedName() {
            return this.qualifiedName;
        }

        public String getSimpleName() {
            return this.delegate.simpleNameOf(this.qualifiedName);
        }

        @Override
        public String toString() {
            return this.qualifiedName;
        }
    }

    public static final class SourceJavaAnnotationValue {

        private final SourceAnnotationValue sourceValue;

        private final SourceProjectBuilderDelegate delegate;

        private SourceJavaAnnotationValue(SourceAnnotationValue sourceValue, SourceProjectBuilderDelegate delegate) {
            this.sourceValue = sourceValue;
            this.delegate = delegate;
        }

        public Object getValue() {
            return this.getParameterValue();
        }

        public List<?> getValueList() {
            List<SourceAnnotationValue> listValues = this.sourceValue.asList();
            if (listValues.isEmpty()) {
                return Collections.emptyList();
            }
            return listValues.stream()
                    .map(value -> new SourceJavaAnnotationValue(value, this.delegate))
                    .collect(Collectors.toList());
        }

        public Object getType() {
            return this.sourceValue
                    .asType()
                    .map(type -> new SourceJavaType(type, this.delegate))
                    .orElse(null);
        }

        public Object getParameterValue() {
            List<SourceAnnotationValue> listValues = this.sourceValue.asList();
            if (!listValues.isEmpty()) {
                boolean scalarList = listValues.stream()
                        .allMatch(value -> value.asType().isEmpty()
                                && value.asAnnotation().isEmpty()
                                && value.asList().isEmpty());
                if (scalarList) {
                    LinkedList<String> values = new LinkedList<>();
                    for (SourceAnnotationValue value : listValues) {
                        values.add(value.asString());
                    }
                    return values;
                }
                LinkedList<Object> values = new LinkedList<>();
                for (SourceAnnotationValue value : listValues) {
                    values.add(new SourceJavaAnnotationValue(value, this.delegate));
                }
                return values;
            }
            return this.sourceValue
                    .asType()
                    .map(type -> new SourceJavaType(type, this.delegate))
                    .map(Object.class::cast)
                    .orElse(this.sourceValue.asString());
        }

        @Override
        public String toString() {
            Object value = this.getParameterValue();
            return value == null ? "" : value.toString();
        }
    }

    public static final class SourceJavaDocletTag {

        private final SourceDocletTag sourceDocletTag;

        private SourceJavaDocletTag(SourceDocletTag sourceDocletTag) {
            this.sourceDocletTag = sourceDocletTag;
        }

        public String getName() {
            return this.sourceDocletTag.name();
        }

        public String getValue() {
            return this.sourceDocletTag.value();
        }

        public Map<String, String> getNamedParameterMap() {
            return this.sourceDocletTag.namedParameters();
        }

        public String getNamedParameter(String parameterName) {
            return this.sourceDocletTag.namedParameters().get(parameterName);
        }
    }

    public static final class SourceJavaPackage {

        private final String name;

        private SourceJavaPackage(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public static final class SourceJavaSource {

        private final List<String> imports;

        private SourceJavaSource(List<String> imports) {
            this.imports = imports;
        }

        public List<String> getImports() {
            return this.imports;
        }
    }

    private static final class ReflectionSourceClass implements SourceClass {

        private final Class<?> runtimeClass;

        private ReflectionSourceClass(Class<?> runtimeClass) {
            this.runtimeClass = runtimeClass;
        }

        @Override
        public String qualifiedName() {
            return this.runtimeClass.getCanonicalName() != null
                    ? this.runtimeClass.getCanonicalName()
                    : this.runtimeClass.getName();
        }

        @Override
        public String simpleName() {
            return this.runtimeClass.getSimpleName();
        }

        @Override
        public List<SourceAnnotation> annotations() {
            return Arrays.stream(this.runtimeClass.getDeclaredAnnotations())
                    .map(ReflectionSourceAnnotation::new)
                    .collect(Collectors.toList());
        }

        @Override
        public List<SourceMethod> methods() {
            return Arrays.stream(this.runtimeClass.getDeclaredMethods())
                    .map(ReflectionSourceMethod::new)
                    .collect(Collectors.toList());
        }

        @Override
        public List<SourceField> fields() {
            return Arrays.stream(this.runtimeClass.getDeclaredFields())
                    .map(ReflectionSourceField::new)
                    .collect(Collectors.toList());
        }

        @Override
        public List<SourceTypeParam> typeParameters() {
            return Arrays.stream(this.runtimeClass.getTypeParameters())
                    .map(ReflectionSourceTypeParam::new)
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<SourceType> superType() {
            Class<?> superClass = this.runtimeClass.getSuperclass();
            if (superClass == null) {
                return Optional.empty();
            }
            return Optional.of(new ReflectionSourceType(this.runtimeClass.getGenericSuperclass()));
        }

        @Override
        public List<SourceType> interfaces() {
            return Arrays.stream(this.runtimeClass.getGenericInterfaces())
                    .map(ReflectionSourceType::new)
                    .collect(Collectors.toList());
        }

        @Override
        public String comment() {
            return "";
        }

        @Override
        public List<SourceDocletTag> docletTags() {
            return Collections.emptyList();
        }

        @Override
        public boolean isEnum() {
            return this.runtimeClass.isEnum();
        }

        @Override
        public List<String> enumConstants() {
            if (!this.runtimeClass.isEnum()) {
                return Collections.emptyList();
            }
            Object[] constants = this.runtimeClass.getEnumConstants();
            if (constants == null || constants.length == 0) {
                return Collections.emptyList();
            }
            List<String> enumNames = new ArrayList<>(constants.length);
            for (Object constant : constants) {
                enumNames.add(String.valueOf(constant));
            }
            return enumNames;
        }

        @Override
        public boolean isInterface() {
            return this.runtimeClass.isInterface();
        }

        @Override
        public boolean isAnnotation() {
            return this.runtimeClass.isAnnotation();
        }

        @Override
        public boolean isRecord() {
            return this.runtimeClass.isRecord();
        }

        @Override
        public boolean isSealed() {
            return this.runtimeClass.isSealed();
        }

        @Override
        public List<String> permittedSubtypes() {
            if (!this.runtimeClass.isSealed()) {
                return Collections.emptyList();
            }
            Class<?>[] permitted = this.runtimeClass.getPermittedSubclasses();
            if (permitted == null || permitted.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.stream(permitted).map(Class::getName).collect(Collectors.toList());
        }
    }

    private static final class ReflectionSourceMethod implements SourceMethod {

        private final Method method;

        private ReflectionSourceMethod(Method method) {
            this.method = method;
        }

        @Override
        public String name() {
            return this.method.getName();
        }

        @Override
        public Optional<SourceType> returnType() {
            return Optional.of(new ReflectionSourceType(this.method.getGenericReturnType()));
        }

        @Override
        public List<SourceParameter> parameters() {
            return Arrays.stream(this.method.getParameters())
                    .map(ReflectionSourceParameter::new)
                    .collect(Collectors.toList());
        }

        @Override
        public List<SourceAnnotation> annotations() {
            return Arrays.stream(this.method.getDeclaredAnnotations())
                    .map(ReflectionSourceAnnotation::new)
                    .collect(Collectors.toList());
        }

        @Override
        public String comment() {
            return "";
        }

        @Override
        public List<SourceDocletTag> docletTags() {
            return Collections.emptyList();
        }

        @Override
        public boolean isConstructor() {
            return false;
        }
    }

    private static final class ReflectionSourceField implements SourceField {

        private final Field field;

        private ReflectionSourceField(Field field) {
            this.field = field;
        }

        @Override
        public String name() {
            return this.field.getName();
        }

        @Override
        public SourceType type() {
            return new ReflectionSourceType(this.field.getGenericType());
        }

        @Override
        public List<SourceAnnotation> annotations() {
            return Arrays.stream(this.field.getDeclaredAnnotations())
                    .map(ReflectionSourceAnnotation::new)
                    .collect(Collectors.toList());
        }

        @Override
        public String comment() {
            return "";
        }

        @Override
        public Optional<String> initializer() {
            return Optional.empty();
        }
    }

    private static final class ReflectionSourceParameter implements SourceParameter {

        private final Parameter parameter;

        private ReflectionSourceParameter(Parameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public String name() {
            return this.parameter.getName();
        }

        @Override
        public SourceType type() {
            return new ReflectionSourceType(this.parameter.getParameterizedType());
        }

        @Override
        public List<SourceAnnotation> annotations() {
            return Arrays.stream(this.parameter.getDeclaredAnnotations())
                    .map(ReflectionSourceAnnotation::new)
                    .collect(Collectors.toList());
        }

        @Override
        public boolean isVarArgs() {
            return this.parameter.isVarArgs();
        }
    }

    private static final class ReflectionSourceType implements SourceType {

        private final Type type;

        private ReflectionSourceType(Type type) {
            this.type = type;
        }

        @Override
        public String qualifiedName() {
            if (this.type instanceof Class<?>) {
                Class<?> classType = (Class<?>) this.type;
                if (classType.isArray()) {
                    return classType.getComponentType().getTypeName() + "[]";
                }
                return classType.getName();
            }
            if (this.type instanceof ParameterizedType) {
                return this.type.getTypeName();
            }
            if (this.type instanceof GenericArrayType) {
                return this.type.getTypeName();
            }
            if (this.type instanceof TypeVariable) {
                return ((TypeVariable<?>) this.type).getName();
            }
            if (this.type instanceof WildcardType) {
                return this.type.getTypeName();
            }
            return this.type.getTypeName();
        }

        @Override
        public List<SourceType> typeArguments() {
            if (this.type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) this.type;
                Type[] arguments = parameterizedType.getActualTypeArguments();
                if (arguments.length == 0) {
                    return Collections.emptyList();
                }
                return Arrays.stream(arguments).map(ReflectionSourceType::new).collect(Collectors.toList());
            }
            if (this.type instanceof Class<?> && ((Class<?>) this.type).isArray()) {
                return Collections.singletonList(new ReflectionSourceType(((Class<?>) this.type).getComponentType()));
            }
            if (this.type instanceof GenericArrayType) {
                return Collections.singletonList(
                        new ReflectionSourceType(((GenericArrayType) this.type).getGenericComponentType()));
            }
            return Collections.emptyList();
        }

        @Override
        public boolean isArray() {
            return (this.type instanceof Class<?> && ((Class<?>) this.type).isArray())
                    || this.type instanceof GenericArrayType;
        }

        @Override
        public boolean isPrimitive() {
            return this.type instanceof Class<?> && ((Class<?>) this.type).isPrimitive();
        }

        @Override
        public boolean isWildcard() {
            return this.type instanceof WildcardType;
        }

        @Override
        public Optional<SourceType> wildcardBound() {
            if (!(this.type instanceof WildcardType)) {
                return Optional.empty();
            }
            WildcardType wildcardType = (WildcardType) this.type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length > 0 && upperBounds[0] != Object.class) {
                return Optional.of(new ReflectionSourceType(upperBounds[0]));
            }
            Type[] lowerBounds = wildcardType.getLowerBounds();
            if (lowerBounds.length > 0) {
                return Optional.of(new ReflectionSourceType(lowerBounds[0]));
            }
            return Optional.empty();
        }
    }

    private static final class ReflectionSourceTypeParam implements SourceTypeParam {

        private final TypeVariable<?> typeVariable;

        private ReflectionSourceTypeParam(TypeVariable<?> typeVariable) {
            this.typeVariable = typeVariable;
        }

        @Override
        public String name() {
            return this.typeVariable.getName();
        }

        @Override
        public List<SourceType> bounds() {
            Type[] bounds = this.typeVariable.getBounds();
            if (bounds.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.stream(bounds).map(ReflectionSourceType::new).collect(Collectors.toList());
        }
    }

    private static final class ReflectionSourceAnnotation implements SourceAnnotation {

        private final Annotation annotation;

        private ReflectionSourceAnnotation(Annotation annotation) {
            this.annotation = annotation;
        }

        @Override
        public String qualifiedName() {
            return this.annotation.annotationType().getName();
        }

        @Override
        public Map<String, SourceAnnotationValue> members() {
            Map<String, SourceAnnotationValue> members = new LinkedHashMap<>();
            for (Method memberMethod : this.annotation.annotationType().getDeclaredMethods()) {
                if (memberMethod.getParameterCount() > 0) {
                    continue;
                }
                try {
                    Object value = memberMethod.invoke(this.annotation);
                    members.put(memberMethod.getName(), new ReflectionSourceAnnotationValue(value));
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Skip unsupported runtime annotation member.
                }
            }
            return members;
        }
    }

    private static final class ReflectionSourceAnnotationValue implements SourceAnnotationValue {

        private final Object value;

        private ReflectionSourceAnnotationValue(Object value) {
            this.value = value;
        }

        @Override
        public String asString() {
            if (this.value == null) {
                return "";
            }
            if (this.value instanceof String
                    || this.value instanceof Number
                    || this.value instanceof Boolean
                    || this.value instanceof Character) {
                return this.value.toString();
            }
            if (this.value instanceof Enum<?>) {
                return ((Enum<?>) this.value).name();
            }
            if (this.value instanceof Class<?>) {
                return ((Class<?>) this.value).getName();
            }
            return String.valueOf(this.value);
        }

        @Override
        public List<SourceAnnotationValue> asList() {
            if (this.value == null) {
                return Collections.emptyList();
            }
            if (!this.value.getClass().isArray()) {
                return Collections.emptyList();
            }
            int length = Array.getLength(this.value);
            if (length == 0) {
                return Collections.emptyList();
            }
            List<SourceAnnotationValue> listValues = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                listValues.add(new ReflectionSourceAnnotationValue(Array.get(this.value, i)));
            }
            return listValues;
        }

        @Override
        public Optional<SourceAnnotation> asAnnotation() {
            if (this.value instanceof Annotation) {
                return Optional.of(new ReflectionSourceAnnotation((Annotation) this.value));
            }
            return Optional.empty();
        }

        @Override
        public Optional<SourceType> asType() {
            if (this.value instanceof Class<?>) {
                return Optional.of(new ReflectionSourceType((Class<?>) this.value));
            }
            return Optional.empty();
        }

        @Override
        public Object raw() {
            return this.value;
        }
    }
}
