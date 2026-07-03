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
import com.github.hsindumas.stagger.constants.DocTags;
import com.github.hsindumas.stagger.constants.FrameworkEnum;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiSchema;
import com.github.hsindumas.stagger.model.JavadocJavaMethod;
import com.github.hsindumas.stagger.model.WebSocketDoc;
import com.github.hsindumas.stagger.model.annotation.FrameworkAnnotations;
import com.github.hsindumas.stagger.model.javadoc.JavadocApiDoc;
import com.github.hsindumas.stagger.utils.DocClassUtil;
import com.github.hsindumas.stagger.utils.DocUtil;
import com.github.hsindumas.stagger.utils.JavaClassUtil;
import com.power.common.util.StringUtil;
import com.power.common.util.ValidateUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * javadoc doc build template.
 *
 * @author chenchuxin
 * @author HsinDumas
 * @since 3.0.5
 */
public class JavadocDocBuildTemplate implements IDocBuildTemplate<JavadocApiDoc>,
		IWebSocketDocBuildTemplate<WebSocketDoc>, IJavadocDocTemplate<JavadocJavaMethod> {

	/**
	 * api index
	 */
	private final AtomicInteger atomicInteger = new AtomicInteger(1);

	@Override
	public boolean supportsFramework(String framework) {
		return FrameworkEnum.JAVADOC.getFramework().equalsIgnoreCase(framework);
	}

	@Override
	public boolean addMethodModifiers() {
		return true;
	}

	@Override
	public JavadocJavaMethod createEmptyJavadocJavaMethod() {
		return new JavadocJavaMethod();
	}

	@Override
	public ApiSchema<JavadocApiDoc> renderApi(ProjectDocConfigBuilder projectBuilder, Collection<?> candidateClasses) {
		ApiConfig apiConfig = projectBuilder.getApiConfig();
		List<JavadocApiDoc> apiDocList = new ArrayList<>();
		int maxOrder = 0;
		boolean setCustomOrder = false;
		for (Object candidateClass : candidateClasses) {
			Object cls = candidateClass;
			if (skipClass(apiConfig, cls, null)) {
				continue;
			}
			String strOrder = JavaClassUtil.getClassTagsValue(cls, DocTags.ORDER, Boolean.TRUE);
			int order = 0;
			if (ValidateUtil.isNonNegativeInteger(strOrder)) {
				order = Integer.parseInt(strOrder);
				maxOrder = Math.max(maxOrder, order);
				setCustomOrder = true;
			}
			List<JavadocJavaMethod> apiMethodDocs = this.buildServiceMethod(cls, apiConfig, projectBuilder);
			this.handleJavaApiDoc(cls, apiDocList, apiMethodDocs, order, projectBuilder);
		}
		ApiSchema<JavadocApiDoc> apiSchema = new ApiSchema<>();
		if (apiConfig.isSortByTitle()) {
			// sort by title
			Collections.sort(apiDocList);
			apiSchema.setApiDatas(apiDocList);
			return apiSchema;
		}
		else if (setCustomOrder) {
			atomicInteger.getAndAdd(maxOrder);
			// while set custom oder
			final List<JavadocApiDoc> tempList = new ArrayList<>(apiDocList);
			tempList.forEach(p -> {
				if (p.getOrder() == 0) {
					p.setOrder(atomicInteger.getAndAdd(1));
				}
			});
			apiSchema.setApiDatas(tempList.stream()
				.sorted(Comparator.comparing(JavadocApiDoc::getOrder))
				.collect(Collectors.toList()));
		}
		else {
			apiDocList.forEach(p -> p.setOrder(atomicInteger.getAndAdd(1)));
			apiSchema.setApiDatas(apiDocList);
		}
		return apiSchema;
	}

	@Override
	public List<WebSocketDoc> renderWebSocketApi(ProjectDocConfigBuilder projectBuilder,
			Collection<?> candidateClasses) {
		return null;
	}

	@Override
	public boolean ignoreReturnObject(String typeName, List<String> ignoreParams) {
		return false;
	}

	@Override
	public boolean isEntryPoint(Object cls, FrameworkAnnotations frameworkAnnotations) {
		List<?> docletTags = DocUtil.getClassTags(cls);
		for (Object docletTag : docletTags) {
			String value = DocUtil.getDocletTagName(docletTag);
			if (DocTags.JAVA_DOC.equals(value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public FrameworkAnnotations registeredAnnotations() {
		return null;
	}

	/**
	 * Handles the generation of Java API documentation. This method is responsible for
	 * creating an API documentation object based on the provided Java class information
	 * and populating its properties.
	 * @param cls The Java class from which to extract documentation information.
	 * @param apiDocList A list where the generated API documentation objects will be
	 * added.
	 * @param apiMethodDocs A list containing documentation for methods within the class.
	 * @param order The order in which the API documentation should be listed.
	 * @param builder A builder used to retrieve class information and configurations.
	 */
	private void handleJavaApiDoc(Object cls, List<JavadocApiDoc> apiDocList, List<JavadocJavaMethod> apiMethodDocs,
			int order, ProjectDocConfigBuilder builder) {
		String className = DocUtil.getClassCanonicalName(cls);
		String comment = DocUtil.getClassComment(cls);
		String shortName = DocUtil.getClassSimpleName(cls);
		List<String> interfaceNames = builder.getImplementedInterfaceNames(className);
		if (!interfaceNames.isEmpty() && !DocUtil.isClassInterface(cls)) {
			String interfaceName = interfaceNames.get(0);
			className = DocClassUtil.getSimpleName(interfaceName);
			shortName = className;
			Object javaClass = builder.getClassByName(interfaceName);
			if (Objects.isNull(javaClass)) {
				javaClass = builder.getClassByName(className);
			}
			if (StringUtil.isEmpty(comment) && Objects.nonNull(javaClass)) {
				comment = DocUtil.getClassComment(javaClass);
			}
		}
		JavadocApiDoc apiDoc = new JavadocApiDoc();
		apiDoc.setOrder(order);
		apiDoc.setName(className);
		apiDoc.setShortName(shortName);
		apiDoc.setAlias(className);
		if (builder.getApiConfig().isMd5EncryptedHtmlName()) {
			String name = DocUtil.generateId(apiDoc.getName());
			apiDoc.setAlias(name);
		}
		apiDoc.setDesc(DocUtil.getEscapeAndCleanComment(comment));
		apiDoc.setList(apiMethodDocs);

		List<?> docletTags = DocUtil.getClassTags(cls);
		List<String> authorList = new ArrayList<>();
		for (Object docletTag : docletTags) {
			String name = DocUtil.getDocletTagName(docletTag);
			if (DocTags.VERSION.equals(name)) {
				apiDoc.setVersion(DocUtil.getDocletTagValue(docletTag));
			}
			if (DocTags.AUTHOR.equals(name)) {
				authorList.add(DocUtil.getDocletTagValue(docletTag));
			}
		}
		apiDoc.setAuthor(String.join(", ", authorList));
		apiDocList.add(apiDoc);
	}

}
