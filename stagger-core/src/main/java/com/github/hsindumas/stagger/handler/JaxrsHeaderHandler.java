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
package com.github.hsindumas.stagger.handler;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.DocTags;
import com.github.hsindumas.stagger.constants.JAXRSAnnotations;
import com.github.hsindumas.stagger.constants.JakartaJaxrsAnnotations;
import com.github.hsindumas.stagger.constants.ParamTypeConstants;
import com.github.hsindumas.stagger.model.ApiReqParam;
import com.github.hsindumas.stagger.model.torna.EnumInfoAndValues;
import com.github.hsindumas.stagger.utils.DocClassUtil;
import com.github.hsindumas.stagger.utils.DocUtil;
import com.github.hsindumas.stagger.utils.JavaClassUtil;
import com.power.common.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Jaxrs Header Handler This class is responsible for handling JAX-RS HTTP header
 * parameter annotations. It parses the parameters and their annotations within a method
 * to extract header information.
 *
 * @author Zxq
 * @author HsinDumas
 */
public class JaxrsHeaderHandler {

	/**
	 * Handles JAX-RS headers for a given method.
	 * @param method The JavaMethod object representing the method whose headers are to be
	 * handled.
	 * @param projectBuilder The ProjectDocConfigBuilder object used to build project
	 * documentation configurations.
	 * @return A list of ApiReqParam objects representing the parsed header parameters.
	 */
	public List<ApiReqParam> handle(Object method, ProjectDocConfigBuilder projectBuilder) {
		Map<String, String> constantsMap = projectBuilder.getConstantsMap();

		ClassLoader classLoader = projectBuilder.getApiConfig().getClassLoader();
		List<ApiReqParam> apiReqHeaders = new ArrayList<>();
		List<?> parameters = DocUtil.getMethodParameters(method);
		for (Object javaParameter : parameters) {
			List<?> annotations = DocUtil.getParameterAnnotations(javaParameter);
			String paramName = DocUtil.getParameterName(javaParameter);

			// hit target head annotation
			ApiReqParam apiReqHeader = new ApiReqParam();

			String defaultValue = "";
			for (Object annotation : annotations) {
				String annotationName = DocUtil.getAnnotationTypeFullyQualifiedName(annotation);
				// Obtain header default value
				if (JakartaJaxrsAnnotations.JAX_DEFAULT_VALUE_FULLY.equals(annotationName)
						|| JAXRSAnnotations.JAX_DEFAULT_VALUE_FULLY.equals(annotationName)) {
					defaultValue = StringUtil.removeQuotes(DocUtil.getRequestHeaderValue(classLoader, annotation));
					defaultValue = DocUtil.handleConstants(constantsMap, defaultValue);
				}
				apiReqHeader.setValue(defaultValue);

				// Obtain header value
				if (JakartaJaxrsAnnotations.JAX_HEADER_PARAM_FULLY.equals(annotationName)
						|| JAXRSAnnotations.JAX_HEADER_PARAM_FULLY.equals(annotationName)) {
					String name = StringUtil.removeQuotes(DocUtil.getRequestHeaderValue(classLoader, annotation));
					name = DocUtil.handleConstants(constantsMap, name);
					apiReqHeader.setName(name);

					String typeName = DocUtil.getParameterTypeValue(javaParameter).toLowerCase();
					String genericFullyQualifiedName = DocUtil.getParameterGenericFullyQualifiedName(javaParameter);
					Object javaClass = projectBuilder.getClassByName(genericFullyQualifiedName);
					boolean enumType = projectBuilder.isEnumType(genericFullyQualifiedName);
					if (enumType) {
						apiReqHeader.setType(ParamTypeConstants.PARAM_TYPE_ENUM);
						boolean hasJavaEnumClass = DocUtil.isClassEnum(javaClass);
						if (hasJavaEnumClass) {
							EnumInfoAndValues enumInfoAndValue = JavaClassUtil.getEnumInfoAndValue(javaClass,
									projectBuilder, Boolean.FALSE);
							if (Objects.nonNull(enumInfoAndValue)) {
								String enumValue = StringUtil
									.removeDoubleQuotes(String.valueOf(enumInfoAndValue.getValue()));
								if (StringUtils.isBlank(apiReqHeader.getValue())) {
									apiReqHeader.setValue(enumValue);
								}
								apiReqHeader.setEnumInfoAndValues(enumInfoAndValue).setType(enumInfoAndValue.getType());
							}
						}
						else {
							String enumSampleValue = projectBuilder.getEnumSampleValue(genericFullyQualifiedName);
							if (StringUtil.isNotEmpty(enumSampleValue)) {
								String sampleValue = StringUtil.removeDoubleQuotes(enumSampleValue);
								if (StringUtils.isBlank(apiReqHeader.getValue())) {
									apiReqHeader.setValue(sampleValue);
								}
								apiReqHeader.setEnumInfoAndValues(EnumInfoAndValues.builder()
									.setEnumValues(Collections.singletonList(sampleValue)));
							}
						}
					}
					else {
						apiReqHeader.setType(DocClassUtil.processTypeNameForParams(typeName));
					}

					String className = DocUtil.getMethodDeclaringClassCanonicalName(method);
					Map<String, String> paramMap = DocUtil.getCommentsByTag(method, DocTags.PARAM, className);
					String paramComments = paramMap.get(paramName);
					apiReqHeader.setDesc(getComments(defaultValue, paramComments));
					apiReqHeaders.add(apiReqHeader);
				}
			}
		}
		return apiReqHeaders;
	}

	/**
	 * Generates a description string for a header parameter including its default value
	 * if provided.
	 * @param defaultValue The default value of the parameter.
	 * @param paramComments Any comments or descriptions associated with the parameter.
	 * @return A string containing the parameter description and default value.
	 */
	private String getComments(String defaultValue, String paramComments) {
		if (Objects.nonNull(paramComments)) {
			StringBuilder desc = new StringBuilder();
			desc.append(paramComments);
			if (StringUtils.isNotBlank(defaultValue)) {
				desc.append("(defaultValue: ").append(defaultValue).append(")");
			}
			return desc.toString();
		}
		return "";
	}

}
