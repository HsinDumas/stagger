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
package com.github.hsindumas.stagger.template;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.constants.DocTags;
import com.github.hsindumas.stagger.helper.ParamsBuildHelper;
import com.github.hsindumas.stagger.model.ApiMethodDoc;
import com.github.hsindumas.stagger.model.ApiParam;
import com.github.hsindumas.stagger.model.ApiReturn;
import com.github.hsindumas.stagger.model.DocJavaMethod;
import com.github.hsindumas.stagger.model.DocJavaParameter;
import com.github.hsindumas.stagger.model.annotation.FrameworkAnnotations;
import com.github.hsindumas.stagger.utils.ApiParamTreeUtil;
import com.github.hsindumas.stagger.utils.DocClassUtil;
import com.github.hsindumas.stagger.utils.DocUtil;
import com.github.hsindumas.stagger.utils.JavaClassUtil;
import com.github.hsindumas.stagger.utils.JavaClassValidateUtil;
import com.github.hsindumas.stagger.utils.OpenApiSchemaUtil;
import com.github.hsindumas.stagger.common.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.github.hsindumas.stagger.constants.DocGlobalConstants.NO_COMMENTS_FOUND;

/**
 * Base Doc Build Template
 *
 * @author yu3.sun on 2022/10/2
 * @author HsinDumas
 */
public interface IBaseDocBuildTemplate {

	/**
	 * Comment Resolve
	 * @param comment comment
	 * @return String
	 */
	default String paramCommentResolve(String comment) {
		if (StringUtil.isEmpty(comment)) {
			comment = NO_COMMENTS_FOUND;
		}
		else {
			if (comment.contains("|")) {
				comment = comment.substring(0, comment.indexOf("|"));
			}
		}
		return comment;
	}

	/**
	 * Build return api params
	 * @param docJavaMethod JavaMethod
	 * @param projectBuilder ProjectDocConfigBuilder
	 * @return List
	 */
	default List<ApiParam> buildReturnApiParams(DocJavaMethod docJavaMethod, ProjectDocConfigBuilder projectBuilder) {
		Object method = docJavaMethod.getJavaMethod();
		String returnTypeCanonicalName = DocUtil.getMethodReturnTypeCanonicalName(method);
		if ("void".equals(returnTypeCanonicalName)
				&& Objects.isNull(projectBuilder.getApiConfig().getResponseBodyAdvice())) {
			return new ArrayList<>(0);
		}
		Object downloadTag = DocUtil.getMethodTagByName(method, DocTags.DOWNLOAD);
		if (Objects.nonNull(downloadTag)) {
			return new ArrayList<>(0);
		}
		String returnTypeGenericCanonicalName = DocUtil.getMethodReturnTypeGenericCanonicalName(method);
		if (Objects.nonNull(projectBuilder.getApiConfig().getResponseBodyAdvice())
				&& Objects.isNull(DocUtil.getMethodTagByName(method, DocTags.IGNORE_RESPONSE_BODY_ADVICE))) {
			String responseBodyAdvice = projectBuilder.getApiConfig().getResponseBodyAdvice().getClassName();
			if (!returnTypeGenericCanonicalName.startsWith(responseBodyAdvice)) {
				returnTypeGenericCanonicalName = responseBodyAdvice + "<" + returnTypeGenericCanonicalName + ">";
			}
		}
		Map<String, ?> actualTypesMap = docJavaMethod.getActualTypesMap();
		ApiReturn apiReturn = DocClassUtil.processReturnType(returnTypeGenericCanonicalName);
		String returnType = apiReturn.getGenericCanonicalName();
		if (Objects.nonNull(actualTypesMap)) {
			for (Map.Entry<String, ?> entry : actualTypesMap.entrySet()) {
				returnType = returnType.replace(entry.getKey(), DocUtil.getTypeCanonicalName(entry.getValue()));
			}
		}

		String typeName = apiReturn.getSimpleName();
		if (this.ignoreReturnObject(typeName, projectBuilder.getApiConfig().getIgnoreRequestParams())) {
			return new ArrayList<>(0);
		}
		if (JavaClassValidateUtil.isPrimitive(typeName)) {
			docJavaMethod.setReturnSchema(OpenApiSchemaUtil.primaryTypeSchema(typeName));
			return new ArrayList<>(0);
		}
		if (JavaClassValidateUtil.isCollection(typeName)) {
			if (returnType.contains("<")) {
				String gicName = returnType.substring(returnType.indexOf("<") + 1, returnType.lastIndexOf(">"));
				if (JavaClassValidateUtil.isPrimitive(gicName)) {
					docJavaMethod.setReturnSchema(OpenApiSchemaUtil.arrayTypeSchema(gicName));
					return new ArrayList<>(0);
				}
				return ParamsBuildHelper.buildParams(gicName, "", 0, null, Boolean.TRUE, new HashMap<>(16),
						projectBuilder, null, docJavaMethod.getJsonViewClasses(), 0, Boolean.FALSE, null);
			}
			else {
				return new ArrayList<>(0);
			}
		}
		if (JavaClassValidateUtil.isMap(typeName)) {
			String[] keyValue = DocClassUtil.getMapKeyValueType(returnType);
			if (keyValue.length == 0) {
				return new ArrayList<>(0);
			}
			return ParamsBuildHelper.buildParams(returnType, "", 0, null, Boolean.TRUE, new HashMap<>(16),
					projectBuilder, null, docJavaMethod.getJsonViewClasses(), 0, Boolean.FALSE, null);
		}
		if (StringUtil.isNotEmpty(returnType)) {
			return ParamsBuildHelper.buildParams(returnType, "", 0, null, Boolean.TRUE, new HashMap<>(16),
					projectBuilder, null, docJavaMethod.getJsonViewClasses(), 0, Boolean.FALSE, null);
		}
		return new ArrayList<>(0);
	}

	/**
	 * Convert params data to tree
	 * @param apiMethodDoc ApiMethodDoc
	 */
	default void convertParamsDataToTree(ApiMethodDoc apiMethodDoc) {
		apiMethodDoc.setPathParams(ApiParamTreeUtil.apiParamToTree(apiMethodDoc.getPathParams()));
		apiMethodDoc.setQueryParams(ApiParamTreeUtil.apiParamToTree(apiMethodDoc.getQueryParams()));
		apiMethodDoc.setRequestParams(ApiParamTreeUtil.apiParamToTree(apiMethodDoc.getRequestParams()));
	}

	/**
	 * Retrieves and processes the list of parameters for a given Java method, applying
	 * various transformations and ignoring specified parameters.
	 * @param builder The project documentation configuration builder.
	 * @param docJavaMethod The documented Java method.
	 * @param frameworkAnnotations The framework annotations used to identify specific
	 * annotations.
	 * @return A list of processed {@link DocJavaParameter} objects.
	 */
	default List<DocJavaParameter> getJavaParameterList(ProjectDocConfigBuilder builder,
			final DocJavaMethod docJavaMethod, FrameworkAnnotations frameworkAnnotations) {
		Object javaMethod = docJavaMethod.getJavaMethod();
		Map<String, String> replacementMap = builder.getReplaceClassMap();
		Map<String, String> paramTagMap = docJavaMethod.getParamTagMap();
		List<?> parameterList = DocUtil.getMethodParameters(javaMethod);
		if (parameterList.isEmpty()) {
			return new ArrayList<>(0);
		}
		Set<String> ignoreSets = ignoreParamsSets(javaMethod);
		List<DocJavaParameter> apiJavaParameterList = new ArrayList<>(parameterList.size());
		Map<String, ?> actualTypesMap = docJavaMethod.getActualTypesMap();
		for (Object parameter : parameterList) {
			String paramName = DocUtil.getParameterName(parameter);
			if (ignoreSets.contains(paramName)) {
				continue;
			}
			DocJavaParameter apiJavaParameter = new DocJavaParameter();
			apiJavaParameter.setJavaParameter(parameter);
			Object javaType = invokeParameterType(parameter);
			String javaTypeCanonicalName = DocUtil.getTypeCanonicalName(javaType);
			if (Objects.nonNull(actualTypesMap) && Objects.nonNull(actualTypesMap.get(javaTypeCanonicalName))) {
				javaType = actualTypesMap.get(javaTypeCanonicalName);
			}
			apiJavaParameter.setTypeValue(DocUtil.getTypeValue(javaType));
			StringBuilder genericCanonicalName = new StringBuilder(DocUtil.getTypeGenericCanonicalName(javaType));
			String fullyQualifiedName = DocUtil.getTypeFullyQualifiedName(javaType);
			apiJavaParameter.setFullyQualifiedName(fullyQualifiedName);
			String genericFullyQualifiedName = DocUtil.getTypeGenericFullyQualifiedName(javaType);
			String commentClass = paramTagMap.get(paramName);
			// ignore request params
			if (Objects.nonNull(commentClass) && commentClass.contains(DocTags.IGNORE)) {
				continue;
			}
			String rewriteClassName = getRewriteClassName(replacementMap, genericFullyQualifiedName, commentClass);
			// rewrite class
			if (JavaClassValidateUtil.isClassName(rewriteClassName)) {
				genericCanonicalName = new StringBuilder(rewriteClassName);
				genericFullyQualifiedName = DocClassUtil.getSimpleName(rewriteClassName);
			}
			if (JavaClassValidateUtil.isMvcIgnoreParams(genericCanonicalName.toString(),
					builder.getApiConfig().getIgnoreRequestParams())) {
				continue;
			}
			genericFullyQualifiedName = DocClassUtil.rewriteRequestParam(genericFullyQualifiedName);
			genericCanonicalName = new StringBuilder(DocClassUtil.rewriteRequestParam(genericCanonicalName.toString()));
			List<?> annotations = DocUtil.getParameterAnnotations(parameter);
			apiJavaParameter.setAnnotations(annotations);
			for (Object annotation : annotations) {
				String annotationName = DocUtil.getAnnotationTypeValue(annotation);
				if (Objects.nonNull(frameworkAnnotations)
						&& frameworkAnnotations.getRequestBodyAnnotation().getAnnotationName().equals(annotationName)) {
					if (Objects.nonNull(builder.getApiConfig().getRequestBodyAdvice()) && Objects
						.isNull(DocUtil.getMethodTagByName(javaMethod, DocTags.IGNORE_REQUEST_BODY_ADVICE))) {
						String requestBodyAdvice = builder.getApiConfig().getRequestBodyAdvice().getClassName();
						genericFullyQualifiedName = requestBodyAdvice;
						genericCanonicalName = new StringBuilder(requestBodyAdvice + "<" + genericCanonicalName + ">");
					}
				}
			}
			if (JavaClassValidateUtil.isCollection(genericFullyQualifiedName)
					|| JavaClassValidateUtil.isArray(genericFullyQualifiedName)) {
				if (JavaClassValidateUtil.isCollection(genericCanonicalName.toString())) {
					genericCanonicalName.append("<T>");
				}
			}
			apiJavaParameter.setGenericCanonicalName(genericCanonicalName.toString());
			apiJavaParameter.setGenericFullyQualifiedName(genericFullyQualifiedName);
			apiJavaParameterList.add(apiJavaParameter);
		}
		return apiJavaParameterList;
	}

	/**
	 * Retrieves the rewritten class name based on the provided map, full type name, and
	 * comment class.
	 * @param replacementMap The map containing replacements for class names.
	 * @param fullTypeName The fully qualified type name.
	 * @param commentClass The comment associated with the class, if any.
	 * @return The rewritten class name or the original class name if no valid rewrite is
	 * found.
	 */
	default String getRewriteClassName(Map<String, String> replacementMap, String fullTypeName, String commentClass) {
		String rewriteClassName;
		if (Objects.nonNull(commentClass) && !DocGlobalConstants.NO_COMMENTS_FOUND.equals(commentClass)) {
			String[] comments = commentClass.split("\\|");
			if (comments.length < 1) {
				return replacementMap.get(fullTypeName);
			}
			rewriteClassName = comments[comments.length - 1];
			if (JavaClassValidateUtil.isClassName(rewriteClassName)) {
				return rewriteClassName;
			}
		}
		return replacementMap.get(fullTypeName);
	}

	/**
	 * Retrieves a set of parameter names to be ignored based on the `@ignoreParams` tag
	 * in the method's documentation.
	 * @param method The Java method to inspect for the ignore parameters tag.
	 * @return A set of parameter names that should be ignored.
	 */
	default Set<String> ignoreParamsSets(Object method) {
		Set<String> ignoreSets = new HashSet<>();
		Object ignoreParam = DocUtil.getMethodTagByName(method, DocTags.IGNORE_PARAMS);
		if (Objects.nonNull(ignoreParam)) {
			String[] igParams = DocUtil.getDocletTagValue(ignoreParam).split(" ");
			Collections.addAll(ignoreSets, igParams);
		}
		return ignoreSets;
	}

	/**
	 * Retrieves the simplified return type of a Java method, handling generic types and
	 * array notations.
	 * @param javaMethod The Java method whose return type needs to be processed.
	 * @param actualTypesMap A map containing actual type mappings.
	 * @return The simplified return type as a string.
	 */
	default String getMethodReturnType(Object javaMethod, Map<String, ?> actualTypesMap) {
		String simpleReturn = this.replaceTypeName(DocUtil.getMethodReturnTypeCanonicalName(javaMethod), actualTypesMap,
				Boolean.TRUE);
		String returnClass = this.replaceTypeName(DocUtil.getMethodReturnTypeGenericCanonicalName(javaMethod),
				actualTypesMap, Boolean.TRUE);
		returnClass = returnClass.replace(simpleReturn, JavaClassUtil.getClassSimpleName(simpleReturn));
		String[] arrays = DocClassUtil.getSimpleGicName(returnClass);
		for (String str : arrays) {
			if (str.contains("[")) {
				str = str.substring(0, str.indexOf("["));
			}
			String[] generics = str.split("[<,]");
			for (String generic : generics) {
				if (generic.contains("extends")) {
					String className = generic.substring(generic.lastIndexOf(" ") + 1);
					returnClass = returnClass.replace(className, JavaClassUtil.getClassSimpleName(className));
				}
				if (generic.length() != 1 && !generic.contains("extends")) {
					returnClass = returnClass.replaceAll(generic, JavaClassUtil.getClassSimpleName(generic));
				}

			}
		}
		return returnClass;
	}

	/**
	 * Replaces type names in the given string based on the provided map of actual types.
	 * @param type The type name to be replaced.
	 * @param actualTypesMap A map containing the actual types to be used for replacement.
	 * @param simple A flag indicating whether to use simple names for replacement.
	 * @return The type name after replacement.
	 */
	default String replaceTypeName(String type, Map<String, ?> actualTypesMap, boolean simple) {
		if (Objects.isNull(actualTypesMap)) {
			return type;
		}
		for (Map.Entry<String, ?> entry : actualTypesMap.entrySet()) {
			if (type.contains(entry.getKey())) {
				if (simple) {
					return type.replace(entry.getKey(), DocUtil.getTypeGenericValue(entry.getValue()));
				}
				else {
					return type.replace(entry.getKey(), DocUtil.getTypeGenericFullyQualifiedName(entry.getValue()));
				}
			}
		}
		return type;
	}

	/**
	 * Resolve parameter type metadata without binding to a specific parser type.
	 * @param parameter parameter metadata object
	 * @return parameter type metadata object or null
	 */
	default Object invokeParameterType(Object parameter) {
		if (Objects.isNull(parameter)) {
			return null;
		}
		try {
			return parameter.getClass().getMethod("getType").invoke(parameter);
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	/**
	 * Determines whether the return object should be ignored based on its type name and a
	 * list of ignored parameters.
	 * @param typeName The name of the type to check.
	 * @param ignoreParams A list of parameter names that should be ignored.
	 * @return true if the return object should be ignored; false otherwise.
	 */
	boolean ignoreReturnObject(String typeName, List<String> ignoreParams);

}
