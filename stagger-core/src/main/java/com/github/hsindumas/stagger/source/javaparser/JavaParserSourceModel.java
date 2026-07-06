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

package com.github.hsindumas.stagger.source.javaparser;

import com.github.hsindumas.stagger.source.SourceAnnotation;
import com.github.hsindumas.stagger.source.SourceAnnotationValue;
import com.github.hsindumas.stagger.source.SourceClass;
import com.github.hsindumas.stagger.source.SourceDocletTag;
import com.github.hsindumas.stagger.source.SourceField;
import com.github.hsindumas.stagger.source.SourceMethod;
import com.github.hsindumas.stagger.source.SourceModel;
import com.github.hsindumas.stagger.source.SourceParameter;
import com.github.hsindumas.stagger.source.SourceProject;
import com.github.hsindumas.stagger.source.SourceScanRequest;
import com.github.hsindumas.stagger.source.SourceType;
import com.github.hsindumas.stagger.source.SourceTypeParam;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JavaParser-backed source model.
 *
 * @author HsinDumas
 * @since 5.0.0
 */
public class JavaParserSourceModel implements SourceModel {

    private static final ParserConfiguration.LanguageLevel LANGUAGE_LEVEL = resolveLanguageLevel();

    @Override
    public SourceProject build(SourceScanRequest request) {
        Objects.requireNonNull(request, "request");
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setCharacterEncoding(request.getCharset());
        parserConfiguration.setLanguageLevel(LANGUAGE_LEVEL);
        JavaParser javaParser = new JavaParser(parserConfiguration);

        Map<String, SourceClass> classes = new LinkedHashMap<>();
        for (Path sourceRoot : request.getSourceRoots()) {
            if (Objects.isNull(sourceRoot) || Files.notExists(sourceRoot)) {
                continue;
            }
            this.scanSourceRoot(javaParser, sourceRoot, classes);
        }
        return new JavaParserSourceProject(classes);
    }

    private void scanSourceRoot(JavaParser javaParser, Path sourceRoot, Map<String, SourceClass> classes) {
        try (Stream<Path> sourceFiles = Files.walk(sourceRoot)) {
            sourceFiles
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> this.parseSource(javaParser, path, classes));
        } catch (IOException ignored) {
            // ignore unreadable source roots; caller can decide strictness later.
        }
    }

    private void parseSource(JavaParser javaParser, Path sourceFile, Map<String, SourceClass> classes) {
        ParseResult<CompilationUnit> parseResult;
        try {
            parseResult = javaParser.parse(sourceFile);
        } catch (IOException ignored) {
            return;
        }
        Optional<CompilationUnit> compilationUnitOptional = parseResult.getResult();
        if (compilationUnitOptional.isEmpty()) {
            return;
        }
        CompilationUnit compilationUnit = compilationUnitOptional.get();
        String packageName = compilationUnit
                .getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        for (TypeDeclaration<?> typeDeclaration : compilationUnit.getTypes()) {
            this.collectTypes(packageName, typeDeclaration, Collections.emptyList(), classes);
        }
    }

    private void collectTypes(
            String packageName,
            TypeDeclaration<?> typeDeclaration,
            List<String> ownerTypes,
            Map<String, SourceClass> classes) {
        String qualifiedName = buildQualifiedName(packageName, ownerTypes, typeDeclaration.getNameAsString());
        classes.put(qualifiedName, new JavaParserSourceClass(qualifiedName, typeDeclaration));

        if (!(typeDeclaration instanceof NodeWithMembers<?>)) {
            return;
        }
        NodeWithMembers<?> nodeWithMembers = (NodeWithMembers<?>) typeDeclaration;

        List<String> nestedOwnerTypes = new ArrayList<>(ownerTypes);
        nestedOwnerTypes.add(typeDeclaration.getNameAsString());
        for (BodyDeclaration<?> member : nodeWithMembers.getMembers()) {
            if (!member.isTypeDeclaration()) {
                continue;
            }
            this.collectTypes(packageName, member.asTypeDeclaration(), nestedOwnerTypes, classes);
        }
    }

    private static String buildQualifiedName(String packageName, List<String> ownerTypes, String simpleName) {
        String joinedOwners = ownerTypes.isEmpty() ? simpleName : String.join(".", ownerTypes) + "." + simpleName;
        if (packageName.isEmpty()) {
            return joinedOwners;
        }
        return packageName + "." + joinedOwners;
    }

    private static ParserConfiguration.LanguageLevel resolveLanguageLevel() {
        for (ParserConfiguration.LanguageLevel level : ParserConfiguration.LanguageLevel.values()) {
            if ("JAVA_25".equals(level.name())) {
                return level;
            }
        }
        return ParserConfiguration.LanguageLevel.BLEEDING_EDGE;
    }

    private static List<SourceAnnotation> annotationsOf(Object nodeWithAnnotations) {
        if (!(nodeWithAnnotations instanceof NodeWithAnnotations<?> annotationHolder)) {
            return Collections.emptyList();
        }
        return annotationHolder.getAnnotations().stream()
                .map(JavaParserSourceAnnotation::new)
                .collect(Collectors.toList());
    }

    private static String javadocDescription(Object nodeWithJavadoc) {
        if (!(nodeWithJavadoc instanceof NodeWithJavadoc<?> javadocHolder)) {
            return "";
        }
        Optional<Javadoc> javadocOptional = javadocHolder.getJavadoc();
        if (javadocOptional.isEmpty()) {
            return "";
        }
        return javadocOptional.get().getDescription().toText().trim();
    }

    private static List<SourceDocletTag> docletTagsOf(Object nodeWithJavadoc) {
        if (!(nodeWithJavadoc instanceof NodeWithJavadoc<?> javadocHolder)) {
            return Collections.emptyList();
        }
        Optional<Javadoc> javadocOptional = javadocHolder.getJavadoc();
        if (javadocOptional.isEmpty()) {
            return Collections.emptyList();
        }
        return javadocOptional.get().getBlockTags().stream()
                .map(JavaParserSourceDocletTag::new)
                .collect(Collectors.toList());
    }

    private static final class JavaParserSourceProject implements SourceProject {

        private final Map<String, SourceClass> classes;

        private JavaParserSourceProject(Map<String, SourceClass> classes) {
            this.classes = Collections.unmodifiableMap(new LinkedHashMap<>(classes));
        }

        @Override
        public Optional<SourceClass> findClass(String qualifiedName) {
            if (Objects.isNull(qualifiedName) || qualifiedName.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(this.classes.get(qualifiedName));
        }

        @Override
        public Collection<SourceClass> classes() {
            return this.classes.values();
        }
    }

    private static final class JavaParserSourceClass implements SourceClass {

        private final String qualifiedName;

        private final TypeDeclaration<?> declaration;

        private JavaParserSourceClass(String qualifiedName, TypeDeclaration<?> declaration) {
            this.qualifiedName = qualifiedName;
            this.declaration = declaration;
        }

        @Override
        public String qualifiedName() {
            return this.qualifiedName;
        }

        @Override
        public String simpleName() {
            return this.declaration.getNameAsString();
        }

        @Override
        public List<SourceAnnotation> annotations() {
            return annotationsOf(this.declaration);
        }

        @Override
        public List<SourceMethod> methods() {
            if (!(this.declaration instanceof NodeWithMembers<?>)) {
                return Collections.emptyList();
            }
            NodeWithMembers<?> nodeWithMembers = (NodeWithMembers<?>) this.declaration;
            List<SourceMethod> methods = new ArrayList<>();
            for (BodyDeclaration<?> member : nodeWithMembers.getMembers()) {
                if (member.isMethodDeclaration()) {
                    methods.add(JavaParserSourceMethod.fromMethod(member.asMethodDeclaration()));
                } else if (member.isConstructorDeclaration()) {
                    methods.add(JavaParserSourceMethod.fromConstructor(member.asConstructorDeclaration()));
                }
            }
            return methods;
        }

        @Override
        public List<SourceField> fields() {
            if (!(this.declaration instanceof NodeWithMembers<?>)) {
                return Collections.emptyList();
            }
            NodeWithMembers<?> nodeWithMembers = (NodeWithMembers<?>) this.declaration;
            List<SourceField> fields = new ArrayList<>();
            for (BodyDeclaration<?> member : nodeWithMembers.getMembers()) {
                if (!member.isFieldDeclaration()) {
                    continue;
                }
                FieldDeclaration fieldDeclaration = member.asFieldDeclaration();
                fieldDeclaration
                        .getVariables()
                        .forEach(variable -> fields.add(new JavaParserSourceField(
                                fieldDeclaration,
                                variable.getNameAsString(),
                                variable.getType(),
                                variable.getInitializer().map(Expression::toString))));
            }
            return fields;
        }

        @Override
        public List<SourceTypeParam> typeParameters() {
            if (!(this.declaration instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration)) {
                return Collections.emptyList();
            }
            return classOrInterfaceDeclaration.getTypeParameters().stream()
                    .map(JavaParserSourceTypeParam::new)
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<SourceType> superType() {
            if (!(this.declaration instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration)
                    || classOrInterfaceDeclaration.isInterface()
                    || classOrInterfaceDeclaration.getExtendedTypes().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new JavaParserSourceType(
                    classOrInterfaceDeclaration.getExtendedTypes().get(0)));
        }

        @Override
        public List<SourceType> interfaces() {
            List<SourceType> interfaces = new ArrayList<>();
            if (this.declaration instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
                classOrInterfaceDeclaration
                        .getImplementedTypes()
                        .forEach(type -> interfaces.add(new JavaParserSourceType(type)));
                if (classOrInterfaceDeclaration.isInterface()) {
                    classOrInterfaceDeclaration
                            .getExtendedTypes()
                            .forEach(type -> interfaces.add(new JavaParserSourceType(type)));
                }
            } else if (this.declaration instanceof EnumDeclaration enumDeclaration) {
                enumDeclaration.getImplementedTypes().forEach(type -> interfaces.add(new JavaParserSourceType(type)));
            } else if (this.declaration instanceof RecordDeclaration recordDeclaration) {
                recordDeclaration.getImplementedTypes().forEach(type -> interfaces.add(new JavaParserSourceType(type)));
            }
            return interfaces;
        }

        @Override
        public String comment() {
            return javadocDescription(this.declaration);
        }

        @Override
        public List<SourceDocletTag> docletTags() {
            return docletTagsOf(this.declaration);
        }

        @Override
        public boolean isEnum() {
            return this.declaration.isEnumDeclaration();
        }

        @Override
        public List<String> enumConstants() {
            if (!this.declaration.isEnumDeclaration()) {
                return Collections.emptyList();
            }
            return this.declaration.asEnumDeclaration().getEntries().stream()
                    .map(entry -> entry.getNameAsString())
                    .collect(Collectors.toList());
        }

        @Override
        public boolean isInterface() {
            return this.declaration.isClassOrInterfaceDeclaration()
                    && this.declaration.asClassOrInterfaceDeclaration().isInterface();
        }

        @Override
        public boolean isAnnotation() {
            return this.declaration.isAnnotationDeclaration();
        }

        @Override
        public boolean isRecord() {
            return this.declaration.isRecordDeclaration();
        }

        @Override
        public boolean isSealed() {
            return this.declaration.getModifiers().stream()
                    .anyMatch(modifier -> Modifier.Keyword.SEALED.equals(modifier.getKeyword()));
        }

        @Override
        public List<String> permittedSubtypes() {
            if (!(this.declaration instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration)) {
                return Collections.emptyList();
            }
            return classOrInterfaceDeclaration.getPermittedTypes().stream()
                    .map(ClassOrInterfaceType::asString)
                    .collect(Collectors.toList());
        }
    }

    private static final class JavaParserSourceMethod implements SourceMethod {

        private final String name;

        private final Optional<SourceType> returnType;

        private final List<SourceParameter> parameters;

        private final List<SourceAnnotation> annotations;

        private final String comment;

        private final List<SourceDocletTag> docletTags;

        private final boolean constructor;

        private JavaParserSourceMethod(
                String name,
                Optional<SourceType> returnType,
                List<SourceParameter> parameters,
                List<SourceAnnotation> annotations,
                String comment,
                List<SourceDocletTag> docletTags,
                boolean constructor) {
            this.name = name;
            this.returnType = returnType;
            this.parameters = parameters;
            this.annotations = annotations;
            this.comment = comment;
            this.docletTags = docletTags;
            this.constructor = constructor;
        }

        private static JavaParserSourceMethod fromMethod(MethodDeclaration methodDeclaration) {
            return new JavaParserSourceMethod(
                    methodDeclaration.getNameAsString(),
                    Optional.of(new JavaParserSourceType(methodDeclaration.getType())),
                    methodDeclaration.getParameters().stream()
                            .map(JavaParserSourceParameter::new)
                            .collect(Collectors.toList()),
                    annotationsOf(methodDeclaration),
                    javadocDescription(methodDeclaration),
                    docletTagsOf(methodDeclaration),
                    false);
        }

        private static JavaParserSourceMethod fromConstructor(ConstructorDeclaration constructorDeclaration) {
            return new JavaParserSourceMethod(
                    constructorDeclaration.getNameAsString(),
                    Optional.empty(),
                    constructorDeclaration.getParameters().stream()
                            .map(JavaParserSourceParameter::new)
                            .collect(Collectors.toList()),
                    annotationsOf(constructorDeclaration),
                    javadocDescription(constructorDeclaration),
                    docletTagsOf(constructorDeclaration),
                    true);
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public Optional<SourceType> returnType() {
            return this.returnType;
        }

        @Override
        public List<SourceParameter> parameters() {
            return this.parameters;
        }

        @Override
        public List<SourceAnnotation> annotations() {
            return this.annotations;
        }

        @Override
        public String comment() {
            return this.comment;
        }

        @Override
        public List<SourceDocletTag> docletTags() {
            return this.docletTags;
        }

        @Override
        public boolean isConstructor() {
            return this.constructor;
        }
    }

    private static final class JavaParserSourceField implements SourceField {

        private final FieldDeclaration fieldDeclaration;

        private final String name;

        private final SourceType type;

        private final Optional<String> initializer;

        private JavaParserSourceField(
                FieldDeclaration fieldDeclaration, String name, Type type, Optional<String> initializer) {
            this.fieldDeclaration = fieldDeclaration;
            this.name = name;
            this.type = new JavaParserSourceType(type);
            this.initializer = initializer;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public SourceType type() {
            return this.type;
        }

        @Override
        public List<SourceAnnotation> annotations() {
            return annotationsOf(this.fieldDeclaration);
        }

        @Override
        public String comment() {
            return javadocDescription(this.fieldDeclaration);
        }

        @Override
        public Optional<String> initializer() {
            return this.initializer;
        }
    }

    private static final class JavaParserSourceParameter implements SourceParameter {

        private final Parameter parameter;

        private JavaParserSourceParameter(Parameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public String name() {
            return this.parameter.getNameAsString();
        }

        @Override
        public SourceType type() {
            return new JavaParserSourceType(this.parameter.getType());
        }

        @Override
        public List<SourceAnnotation> annotations() {
            return annotationsOf(this.parameter);
        }

        @Override
        public boolean isVarArgs() {
            return this.parameter.isVarArgs();
        }
    }

    private static final class JavaParserSourceAnnotation implements SourceAnnotation {

        private final AnnotationExpr annotationExpr;

        private JavaParserSourceAnnotation(AnnotationExpr annotationExpr) {
            this.annotationExpr = annotationExpr;
        }

        @Override
        public String qualifiedName() {
            try {
                return this.annotationExpr.resolve().getQualifiedName();
            } catch (RuntimeException ignored) {
                return this.annotationExpr.getNameAsString();
            }
        }

        @Override
        public Map<String, SourceAnnotationValue> members() {
            Map<String, SourceAnnotationValue> members = new LinkedHashMap<>();
            if (this.annotationExpr instanceof NormalAnnotationExpr normalAnnotationExpr) {
                for (MemberValuePair pair : normalAnnotationExpr.getPairs()) {
                    members.put(pair.getNameAsString(), new JavaParserSourceAnnotationValue(pair.getValue()));
                }
            } else if (this.annotationExpr instanceof SingleMemberAnnotationExpr singleMemberAnnotationExpr) {
                members.put("value", new JavaParserSourceAnnotationValue(singleMemberAnnotationExpr.getMemberValue()));
            }
            return members;
        }
    }

    private static final class JavaParserSourceAnnotationValue implements SourceAnnotationValue {

        private final Expression expression;

        private JavaParserSourceAnnotationValue(Expression expression) {
            this.expression = expression;
        }

        @Override
        public String asString() {
            if (this.expression.isStringLiteralExpr()) {
                return this.expression.asStringLiteralExpr().asString();
            }
            if (this.expression.isTextBlockLiteralExpr()) {
                return this.expression.asTextBlockLiteralExpr().getValue();
            }
            if (this.expression.isCharLiteralExpr()) {
                return this.expression.asCharLiteralExpr().getValue();
            }
            return this.expression.toString();
        }

        @Override
        public List<SourceAnnotationValue> asList() {
            if (!this.expression.isArrayInitializerExpr()) {
                return Collections.emptyList();
            }
            ArrayInitializerExpr arrayInitializerExpr = this.expression.asArrayInitializerExpr();
            return arrayInitializerExpr.getValues().stream()
                    .map(JavaParserSourceAnnotationValue::new)
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<SourceAnnotation> asAnnotation() {
            if (!this.expression.isAnnotationExpr()) {
                return Optional.empty();
            }
            return Optional.of(new JavaParserSourceAnnotation(this.expression.asAnnotationExpr()));
        }

        @Override
        public Optional<SourceType> asType() {
            if (!this.expression.isClassExpr()) {
                return Optional.empty();
            }
            return Optional.of(
                    new JavaParserSourceType(this.expression.asClassExpr().getType()));
        }

        @Override
        public Object raw() {
            return this.expression;
        }
    }

    private static final class JavaParserSourceType implements SourceType {

        private final Type type;

        private JavaParserSourceType(Type type) {
            this.type = type;
        }

        @Override
        public String qualifiedName() {
            try {
                return this.type.resolve().describe();
            } catch (RuntimeException ignored) {
                return this.type.asString();
            }
        }

        @Override
        public List<SourceType> typeArguments() {
            if (!this.type.isClassOrInterfaceType()) {
                return Collections.emptyList();
            }
            ClassOrInterfaceType classOrInterfaceType = this.type.asClassOrInterfaceType();
            Optional<com.github.javaparser.ast.NodeList<Type>> typeArguments = classOrInterfaceType.getTypeArguments();
            if (typeArguments.isEmpty()) {
                return Collections.emptyList();
            }
            return typeArguments.get().stream().map(JavaParserSourceType::new).collect(Collectors.toList());
        }

        @Override
        public boolean isArray() {
            return this.type.isArrayType();
        }

        @Override
        public boolean isPrimitive() {
            return this.type.isPrimitiveType();
        }

        @Override
        public boolean isWildcard() {
            return this.type.isWildcardType();
        }

        @Override
        public Optional<SourceType> wildcardBound() {
            if (!this.type.isWildcardType()) {
                return Optional.empty();
            }
            WildcardType wildcardType = this.type.asWildcardType();
            if (wildcardType.getExtendedType().isPresent()) {
                return Optional.of(
                        new JavaParserSourceType(wildcardType.getExtendedType().get()));
            }
            if (wildcardType.getSuperType().isPresent()) {
                return Optional.of(
                        new JavaParserSourceType(wildcardType.getSuperType().get()));
            }
            return Optional.empty();
        }
    }

    private static final class JavaParserSourceTypeParam implements SourceTypeParam {

        private final TypeParameter typeParameter;

        private JavaParserSourceTypeParam(TypeParameter typeParameter) {
            this.typeParameter = typeParameter;
        }

        @Override
        public String name() {
            return this.typeParameter.getNameAsString();
        }

        @Override
        public List<SourceType> bounds() {
            return this.typeParameter.getTypeBound().stream()
                    .map(JavaParserSourceType::new)
                    .collect(Collectors.toList());
        }
    }

    private static final class JavaParserSourceDocletTag implements SourceDocletTag {

        private final JavadocBlockTag blockTag;

        private JavaParserSourceDocletTag(JavadocBlockTag blockTag) {
            this.blockTag = blockTag;
        }

        @Override
        public String name() {
            return this.blockTag.getTagName();
        }

        @Override
        public String value() {
            return this.blockTag.getContent().toText().trim();
        }

        @Override
        public Map<String, String> namedParameters() {
            Map<String, String> named = new LinkedHashMap<>();
            this.blockTag.getName().ifPresent(name -> named.put(name, this.value()));
            return named;
        }
    }
}
