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

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.common.util.StringUtil;
import com.github.hsindumas.stagger.constants.DocAnnotationConstants;
import com.github.hsindumas.stagger.constants.DocTags;
import com.github.hsindumas.stagger.constants.ParamTypeConstants;
import com.github.hsindumas.stagger.helper.ParamsBuildHelper;
import com.github.hsindumas.stagger.model.ApiReqParam;
import com.github.hsindumas.stagger.model.annotation.HeaderAnnotation;
import com.github.hsindumas.stagger.model.enums.EnumInfoAndValues;
import com.github.hsindumas.stagger.utils.DocClassUtil;
import com.github.hsindumas.stagger.utils.DocUtil;
import com.github.hsindumas.stagger.utils.JavaClassUtil;
import com.github.hsindumas.stagger.utils.JavaClassValidateUtil;
import com.github.hsindumas.stagger.utils.JavaFieldUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * Header Handler
 *
 * @author yu3.sun on 2022/8/30
 * @author HsinDumas
 */
public interface IHeaderHandler {

    /**
     * Handle header
     * @param method JavaMethod
     * @param projectBuilder ProjectDocConfigBuilder
     * @return {@code List<ApiReqParam>}
     */
    @SuppressWarnings("unchecked")
    default List<ApiReqParam> handle(Object method, ProjectDocConfigBuilder projectBuilder) {
        List<ApiReqParam> mappingHeaders = new ArrayList<>();
        List<?> annotations = DocUtil.getMethodAnnotations(method);
        HeaderAnnotation headerAnnotation = getHeaderAnnotation();
        for (Object annotation : annotations) {
            String annotationName = DocUtil.getAnnotationTypeValue(annotation);
            Object headersObject = DocUtil.getAnnotationNamedParameter(annotation, "headers");
            if (!isMapping(annotationName) || Objects.isNull(headersObject)) {
                continue;
            }
            String mappingHeader = StringUtil.removeQuotes(headersObject.toString());
            if (!mappingHeader.startsWith("[")) {
                processMappingHeaders(mappingHeader, mappingHeaders);
                continue;
            }
            List<String> headers = (LinkedList<String>) headersObject;
            for (String str : headers) {
                String header = StringUtil.removeQuotes(str);
                if (header.startsWith("!")) {
                    continue;
                }
                processMappingHeaders(header, mappingHeaders);
            }
        }
        List<ApiReqParam> reqHeaders = new ArrayList<>();
        for (Object javaParameter : DocUtil.getMethodParameters(method)) {
            List<?> javaAnnotations = DocUtil.getParameterAnnotations(javaParameter);
            String className = DocUtil.getMethodDeclaringClassCanonicalName(method);
            String parameterName = DocUtil.getParameterName(javaParameter);
            Map<String, String> paramCommentMap = DocUtil.getCommentsByTag(method, DocTags.PARAM, className);

            for (Object annotation : javaAnnotations) {
                String annotationName = JavaClassUtil.getClassSimpleName(DocUtil.getAnnotationTypeValue(annotation));
                if (!headerAnnotation.getAnnotationName().equals(annotationName)) {
                    continue;
                }
                ApiReqParam apiReqHeader = new ApiReqParam();
                apiReqHeader.setName(parameterName);
                apiReqHeader.setRequired(true);
                apiReqHeader.setDesc(DocUtil.paramCommentResolve(paramCommentMap.get(parameterName)));

                handleParamAnnotation(annotation, apiReqHeader, projectBuilder, method);
                handleParamTypeAndValue(javaParameter, apiReqHeader, projectBuilder, paramCommentMap);
                reqHeaders.add(apiReqHeader);
                break;
            }
        }
        return Stream.of(mappingHeaders, reqHeaders)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * handle Param
     * @param javaParameter javaParameter
     * @param apiReqHeader apiReqHeader
     * @param builder builder
     * @param paramCommentMap paramCommentMap
     */
    default void handleParamTypeAndValue(
            Object javaParameter,
            ApiReqParam apiReqHeader,
            ProjectDocConfigBuilder builder,
            Map<String, String> paramCommentMap) {
        String fullyQualifiedName = DocUtil.getParameterFullyQualifiedName(javaParameter);
        String genericFullyQualifiedName = DocUtil.getParameterGenericFullyQualifiedName(javaParameter);
        String paramName = DocUtil.getParameterName(javaParameter);
        Object javaClass = builder.getClassByName(genericFullyQualifiedName);
        boolean enumType = builder.isEnumType(genericFullyQualifiedName);
        String simpleTypeName = DocUtil.getParameterTypeValue(javaParameter);
        List<?> parameterAnnotations = DocUtil.getParameterAnnotations(javaParameter);

        if (JavaClassValidateUtil.isCollection(fullyQualifiedName)
                || JavaClassValidateUtil.isArray(fullyQualifiedName)) {
            String[] gicNameArr = DocClassUtil.getSimpleGicName(genericFullyQualifiedName);
            String gicName = gicNameArr[0];
            if (JavaClassValidateUtil.isArray(gicName)) {
                gicName = gicName.substring(0, gicName.indexOf("["));
            }
            // handle array and list mock value
            Object gicJavaClass = builder.getClassByName(gicName);
            boolean gicEnumType = builder.isEnumType(gicName);
            if (gicEnumType) {
                boolean hasGicJavaEnumClass = DocUtil.isClassEnum(gicJavaClass);
                if (Objects.nonNull(gicJavaClass)) {
                    String enumComment = ParamsBuildHelper.handleEnumComment(gicJavaClass, builder);
                    apiReqHeader.setDesc(apiReqHeader.getDesc() + enumComment);
                }
                apiReqHeader.setType(ParamTypeConstants.PARAM_TYPE_ARRAY);
                if (hasGicJavaEnumClass) {
                    EnumInfoAndValues enumInfoAndValue =
                            JavaClassUtil.getEnumInfoAndValue(gicJavaClass, builder, Boolean.FALSE);
                    if (Objects.nonNull(enumInfoAndValue)) {
                        String enumValue = StringUtil.removeDoubleQuotes(String.valueOf(enumInfoAndValue.getValue()));
                        apiReqHeader.setValue(enumValue + "," + enumValue).setEnumInfoAndValues(enumInfoAndValue);
                    }
                } else {
                    String enumSampleValue = builder.getEnumSampleValue(gicName);
                    if (StringUtil.isNotEmpty(enumSampleValue)) {
                        String sampleValue = StringUtil.removeDoubleQuotes(enumSampleValue);
                        apiReqHeader
                                .setValue(sampleValue + "," + sampleValue)
                                .setEnumInfoAndValues(EnumInfoAndValues.builder()
                                        .setEnumValues(Collections.singletonList(sampleValue)));
                    }
                }
            } else if (JavaClassValidateUtil.isPrimitive(gicName)) {
                String mockValue = JavaFieldUtil.createMockValue(
                        paramCommentMap, paramName, gicName, gicName, parameterAnnotations);
                if (StringUtil.isNotEmpty(mockValue) && !mockValue.contains(",")) {
                    mockValue = StringUtils.join(
                            mockValue,
                            ",",
                            JavaFieldUtil.createMockValue(
                                    paramCommentMap, paramName, gicName, gicName, parameterAnnotations));
                }

                apiReqHeader.setType(ParamTypeConstants.PARAM_TYPE_ARRAY);
                apiReqHeader.setValue(mockValue);
            } else {
                apiReqHeader.setType(ParamTypeConstants.PARAM_TYPE_ARRAY);
            }
        } else if (JavaClassValidateUtil.isPrimitive(fullyQualifiedName)) {
            String mockValue = JavaFieldUtil.createMockValue(
                    paramCommentMap, paramName, fullyQualifiedName, simpleTypeName, parameterAnnotations);

            apiReqHeader.setType(DocClassUtil.processTypeNameForParams(simpleTypeName));
            apiReqHeader.setValue(mockValue);
        }
        // Handle if it is enum types
        else if (enumType) {
            boolean hasJavaEnumClass = DocUtil.isClassEnum(javaClass);
            if (Objects.nonNull(javaClass)) {
                String enumComment = ParamsBuildHelper.handleEnumComment(javaClass, builder);
                apiReqHeader.setDesc(apiReqHeader.getDesc() + enumComment);
            }
            apiReqHeader.setType(ParamTypeConstants.PARAM_TYPE_ENUM);
            if (hasJavaEnumClass) {
                EnumInfoAndValues enumInfoAndValue =
                        JavaClassUtil.getEnumInfoAndValue(javaClass, builder, Boolean.FALSE);
                if (Objects.nonNull(enumInfoAndValue)) {
                    String enumValue = StringUtil.removeDoubleQuotes(String.valueOf(enumInfoAndValue.getValue()));
                    apiReqHeader
                            .setValue(enumValue)
                            .setEnumInfoAndValues(enumInfoAndValue)
                            .setType(enumInfoAndValue.getType());
                }
            } else {
                String enumSampleValue = builder.getEnumSampleValue(genericFullyQualifiedName);
                if (StringUtil.isNotEmpty(enumSampleValue)) {
                    String sampleValue = StringUtil.removeDoubleQuotes(enumSampleValue);
                    apiReqHeader
                            .setValue(sampleValue)
                            .setEnumInfoAndValues(
                                    EnumInfoAndValues.builder().setEnumValues(Collections.singletonList(sampleValue)));
                }
            }
        } else {
            apiReqHeader.setType(ParamTypeConstants.PARAM_TYPE_OBJECT);
        }
    }

    /**
     * Handle annotation
     * @param annotation annotation
     * @param apiReqHeader apiReqHeader
     * @param projectBuilder projectBuilder
     */
    default void handleParamAnnotation(
            Object annotation, ApiReqParam apiReqHeader, ProjectDocConfigBuilder projectBuilder, Object method) {
        HeaderAnnotation headerAnnotation = getHeaderAnnotation();
        Map<String, String> constantsMap = projectBuilder.getConstantsMap();
        Map<String, Object> requestHeaderMap = DocUtil.getAnnotationNamedParameterMap(annotation);
        if (requestHeaderMap != null && requestHeaderMap.size() > 0) {
            // Obtain header value
            String headerNameProp = null;
            if (requestHeaderMap.containsKey(headerAnnotation.getValueProp())) {
                headerNameProp = headerAnnotation.getValueProp();
            } else if (requestHeaderMap.containsKey(DocAnnotationConstants.NAME_PROP)) {
                // Spring's @RequestHeader supports both value and name.
                headerNameProp = DocAnnotationConstants.NAME_PROP;
            }
            if (StringUtil.isNotEmpty(headerNameProp)) {
                ClassLoader classLoader = projectBuilder.getApiConfig().getClassLoader();
                String attrValue = DocUtil.handleRequestHeaderValue(classLoader, annotation);
                Object rawHeaderName = requestHeaderMap.get(headerNameProp);
                String constValue = StringUtil.removeQuotes(String.valueOf(rawHeaderName));
                String resolvedHeaderName = StringUtil.isEmpty(attrValue) ? constValue : attrValue;
                resolvedHeaderName = DocUtil.handleConstants(constantsMap, resolvedHeaderName);
                if (StringUtil.isNotEmpty(resolvedHeaderName)) {
                    resolvedHeaderName = resolveHeaderConstantExpression(method, classLoader, resolvedHeaderName);
                }
                apiReqHeader.setName(StringUtil.removeQuotes(resolvedHeaderName));
            }

            // Obtain header default value
            if (requestHeaderMap.containsKey(headerAnnotation.getDefaultValueProp())) {
                StringBuilder desc = new StringBuilder();
                String defaultValue = String.valueOf(requestHeaderMap.get(headerAnnotation.getDefaultValueProp()));
                desc.append("(defaultValue: ")
                        .append(StringUtil.removeQuotes(defaultValue))
                        .append(")");
                apiReqHeader.setValue(StringUtil.removeQuotes(defaultValue));
                apiReqHeader.setDesc(apiReqHeader.getDesc() + desc);
            }

            if (requestHeaderMap.containsKey(headerAnnotation.getRequiredProp())) {
                apiReqHeader.setRequired(
                        !Boolean.FALSE.toString().equals(requestHeaderMap.get(headerAnnotation.getRequiredProp())));
            }
        }
    }

    /**
     * Resolve constant-like header expressions (e.g. HEADER_X / AppConst.Header.DEVICE_CODE).
     */
    static String resolveHeaderConstantExpression(Object method, ClassLoader classLoader, String expression) {
        String expr = StringUtil.removeQuotes(StringUtil.trimBlank(expression));
        if (StringUtil.isEmpty(expr)) {
            return expression;
        }
        // Already a literal header name like x-device-code.
        if (expr.contains("-") || expr.contains(" ")) {
            return expr;
        }
        String resolved = resolveExpressionAgainstClassLoader(classLoader, expr);
        if (StringUtil.isNotEmpty(resolved)) {
            return resolved;
        }
        String qualifiedExpr = qualifyExpressionWithImports(method, expr);
        if (StringUtil.isNotEmpty(qualifiedExpr) && !qualifiedExpr.equals(expr)) {
            resolved = resolveExpressionAgainstClassLoader(classLoader, qualifiedExpr);
            if (StringUtil.isNotEmpty(resolved)) {
                return resolved;
            }
        }
        String declaringClassName = DocUtil.getMethodDeclaringClassCanonicalName(method);
        if (StringUtil.isNotEmpty(declaringClassName)) {
            resolved = resolveSimpleConstantFromClass(classLoader, declaringClassName, expr);
            if (StringUtil.isNotEmpty(resolved)) {
                return resolved;
            }
        }
        return expr;
    }

    static String qualifyExpressionWithImports(Object method, String expression) {
        int dot = expression.indexOf('.');
        if (dot <= 0) {
            return expression;
        }
        String firstToken = expression.substring(0, dot);
        // Already qualified package name.
        if (Character.isLowerCase(firstToken.charAt(0))) {
            return expression;
        }
        Object declaringClass = DocUtil.getMethodDeclaringClass(method);
        if (Objects.isNull(declaringClass)) {
            return expression;
        }
        Object source = invokeAccessor(declaringClass, "getSource");
        Object importsObj = invokeAccessor(source, "getImports");
        if (!(importsObj instanceof List)) {
            return expression;
        }
        @SuppressWarnings("unchecked")
        List<Object> imports = (List<Object>) importsObj;
        for (Object imp : imports) {
            String importText = String.valueOf(imp).trim();
            importText = importText.replace("import", "").replace("static", "").replace(";", "").trim();
            if (StringUtil.isEmpty(importText) || importText.endsWith(".*")) {
                continue;
            }
            if (importText.endsWith("." + firstToken)) {
                return importText + expression.substring(dot);
            }
        }
        return expression;
    }

    static Object invokeAccessor(Object target, String methodName) {
        if (Objects.isNull(target)) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * Resolve expression by trying to load a class from left side and reading static fields on the right side.
     */
    static String resolveExpressionAgainstClassLoader(ClassLoader classLoader, String expression) {
        String[] parts = expression.split("\\.");
        if (parts.length < 2) {
            return StringUtil.EMPTY;
        }
        for (int split = parts.length - 1; split >= 1; split--) {
            String className = String.join(".", java.util.Arrays.copyOfRange(parts, 0, split));
            String[] fieldPath = java.util.Arrays.copyOfRange(parts, split, parts.length);
            Class<?> clazz = loadClassWithInnerFallback(classLoader, className);
            if (Objects.nonNull(clazz)) {
                String resolved = resolveStaticFieldPath(clazz, fieldPath);
                if (StringUtil.isNotEmpty(resolved)) {
                    return resolved;
                }
            }
        }
        return StringUtil.EMPTY;
    }

    static Class<?> loadClassWithInnerFallback(ClassLoader classLoader, String dottedName) {
        try {
            return classLoader.loadClass(dottedName);
        } catch (ClassNotFoundException ignored) {
            // continue with nested-class fallback
        }
        String[] segments = dottedName.split("\\.");
        if (segments.length < 2) {
            return null;
        }
        for (int pivot = segments.length - 1; pivot >= 1; pivot--) {
            StringBuilder binaryName = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                if (i > 0) {
                    binaryName.append(i < pivot ? '.' : '$');
                }
                binaryName.append(segments[i]);
            }
            try {
                return classLoader.loadClass(binaryName.toString());
            } catch (ClassNotFoundException ignored) {
                // Try next pivot.
            }
        }
        return null;
    }

    /**
     * Resolve simple constant name from declaring class and its inner classes.
     */
    static String resolveSimpleConstantFromClass(ClassLoader classLoader, String declaringClassName, String fieldName) {
        try {
            Class<?> clazz = classLoader.loadClass(declaringClassName);
            String value = readStaticFieldValue(clazz, fieldName);
            if (StringUtil.isNotEmpty(value)) {
                return value;
            }
            for (Class<?> nested : clazz.getDeclaredClasses()) {
                value = readStaticFieldValue(nested, fieldName);
                if (StringUtil.isNotEmpty(value)) {
                    return value;
                }
            }
        } catch (ClassNotFoundException ignored) {
            // Best-effort fallback.
        }
        return StringUtil.EMPTY;
    }

    /**
     * Resolve a field path where every segment is a static field in order.
     */
    static String resolveStaticFieldPath(Class<?> startClass, String[] fieldPath) {
        Class<?> currentClass = startClass;
        Object currentObject = null;
        for (int i = 0; i < fieldPath.length; i++) {
            Field field = findField(currentClass, fieldPath[i]);
            if (Objects.isNull(field) || !Modifier.isStatic(field.getModifiers())) {
                return StringUtil.EMPTY;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(currentObject);
                if (i == fieldPath.length - 1) {
                    return Objects.nonNull(value) ? String.valueOf(value) : StringUtil.EMPTY;
                }
                if (Objects.isNull(value)) {
                    return StringUtil.EMPTY;
                }
                currentObject = value;
                currentClass = value.getClass();
            } catch (IllegalAccessException ignored) {
                return StringUtil.EMPTY;
            }
        }
        return StringUtil.EMPTY;
    }

    static String readStaticFieldValue(Class<?> clazz, String fieldName) {
        Field field = findField(clazz, fieldName);
        if (Objects.isNull(field) || !Modifier.isStatic(field.getModifiers())) {
            return StringUtil.EMPTY;
        }
        try {
            field.setAccessible(true);
            Object value = field.get(null);
            return Objects.nonNull(value) ? String.valueOf(value) : StringUtil.EMPTY;
        } catch (IllegalAccessException ignored) {
            return StringUtil.EMPTY;
        }
    }

    static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (Objects.nonNull(current)) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * process mapping headers
     * @param header header
     * @param mappingHeaders mapping headers
     */
    default void processMappingHeaders(String header, List<ApiReqParam> mappingHeaders) {
        if (header.contains("!=")) {
            String headerName = header.substring(0, header.indexOf("!"));
            ApiReqParam apiReqHeader = ApiReqParam.builder()
                    .setName(headerName)
                    .setRequired(true)
                    .setValue(null)
                    .setDesc("header condition")
                    .setType("string");
            mappingHeaders.add(apiReqHeader);
        } else {
            String headerName;
            String headerValue = null;
            if (header.contains("=")) {
                int index = header.indexOf("=");
                headerName = header.substring(0, index);
                headerValue = header.substring(index + 1);
            } else {
                headerName = header;
            }
            ApiReqParam apiReqHeader = ApiReqParam.builder()
                    .setName(headerName)
                    .setRequired(true)
                    .setValue(headerValue)
                    .setDesc("header condition")
                    .setType("string");
            mappingHeaders.add(apiReqHeader);
        }
    }

    /**
     * check mapping annotation
     * @param annotationName annotation name
     * @return boolean
     */
    boolean isMapping(String annotationName);

    /**
     * Get framework annotation info
     * @return Header annotation info
     */
    HeaderAnnotation getHeaderAnnotation();
}
