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
import com.github.hsindumas.stagger.constants.DubboAnnotationConstants;
import com.github.hsindumas.stagger.constants.FrameworkEnum;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiSchema;
import com.github.hsindumas.stagger.model.RpcJavaMethod;
import com.github.hsindumas.stagger.model.WebSocketDoc;
import com.github.hsindumas.stagger.model.annotation.FrameworkAnnotations;
import com.github.hsindumas.stagger.model.rpc.RpcApiDoc;
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
 * (Apache Dubbo) rpc doc build template.
 *
 * @author yu 2020/1/29.
 * @author HsinDumas
 */
public class RpcDocBuildTemplate implements IDocBuildTemplate<RpcApiDoc>, IWebSocketDocBuildTemplate<WebSocketDoc>,
		IJavadocDocTemplate<RpcJavaMethod> {

	/**
	 * api index
	 */
	private final AtomicInteger atomicInteger = new AtomicInteger(1);

	@Override
	public boolean supportsFramework(String framework) {
		return FrameworkEnum.DUBBO.getFramework().equalsIgnoreCase(framework);
	}

	@Override
	public boolean addMethodModifiers() {
		return false;
	}

	@Override
	public RpcJavaMethod createEmptyJavadocJavaMethod() {
		return new RpcJavaMethod();
	}

	@Override
	public ApiSchema<RpcApiDoc> renderApi(ProjectDocConfigBuilder projectBuilder, Collection<?> candidateClasses) {
		ApiConfig apiConfig = projectBuilder.getApiConfig();
		List<RpcApiDoc> apiDocList = new ArrayList<>();

		boolean setCustomOrder = false;
		int maxOrder = 0;
		for (Object candidateClass : candidateClasses) {
			Object cls = candidateClass;
			if (skipClass(apiConfig, cls, null)) {
				continue;
			}
			String strOrder = JavaClassUtil.getClassTagsValue(cls, DocTags.ORDER, Boolean.TRUE);
			int order = 0;
			if (ValidateUtil.isNonNegativeInteger(strOrder)) {
				order = Integer.parseInt(strOrder);
				setCustomOrder = true;
				maxOrder = Math.max(maxOrder, order);
			}
			List<RpcJavaMethod> apiMethodDocs = this.buildServiceMethod(cls, apiConfig, projectBuilder);
			this.handleJavaApiDoc(cls, apiDocList, apiMethodDocs, order, projectBuilder);
		}
		ApiSchema<RpcApiDoc> apiSchema = new ApiSchema<>();
		if (apiConfig.isSortByTitle()) {
			// sort by title
			Collections.sort(apiDocList);
			apiSchema.setApiDatas(apiDocList);
			return apiSchema;
		}
		else if (setCustomOrder) {
			atomicInteger.getAndAdd(maxOrder);
			// while set custom oder
			final List<RpcApiDoc> tempList = new ArrayList<>(apiDocList);
			tempList.forEach(p -> {
				if (p.getOrder() == 0) {
					p.setOrder(atomicInteger.getAndAdd(1));
				}
			});
			apiSchema.setApiDatas(
					tempList.stream().sorted(Comparator.comparing(RpcApiDoc::getOrder)).collect(Collectors.toList()));
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
		// Exclude DubboSwaggerService from dubbo 2.7.x
		if (DubboAnnotationConstants.DUBBO_SWAGGER.equals(DocUtil.getClassCanonicalName(cls))) {
			return false;
		}
		List<?> classAnnotations = DocUtil.getClassAnnotations(cls);
		for (Object annotation : classAnnotations) {
			String name = DocUtil.getAnnotationTypeFullyQualifiedName(annotation);
			if (DubboAnnotationConstants.SERVICE.equals(name) || DubboAnnotationConstants.DUBBO_SERVICE.equals(name)
					|| DubboAnnotationConstants.ALI_DUBBO_SERVICE.equals(name)) {
				return true;
			}
		}
		List<?> docletTags = DocUtil.getClassTags(cls);
		for (Object docletTag : docletTags) {
			String value = DocUtil.getDocletTagName(docletTag);
			if (DocTags.DUBBO.equals(value)) {
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
	 * processing a Java class to generate API documentation details, including setting up
	 * the API documentation list and method details.
	 * @param cls The JavaClass object representing the class being documented.
	 * @param apiDocList The list to store generated RpcApiDoc objects.
	 * @param apiMethodDocs The list containing documentation for methods within the
	 * class.
	 * @param order The order or priority of the API documentation.
	 * @param builder The ProjectDocConfigBuilder used to configure and retrieve class
	 * information.
	 */
	private void handleJavaApiDoc(Object cls, List<RpcApiDoc> apiDocList, List<RpcJavaMethod> apiMethodDocs, int order,
			ProjectDocConfigBuilder builder) {
		String className = DocUtil.getClassCanonicalName(cls);
		String shortName = DocUtil.getClassSimpleName(cls);
		String comment = DocUtil.getClassComment(cls);
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
		RpcApiDoc apiDoc = new RpcApiDoc();
		apiDoc.setOrder(order);
		apiDoc.setName(className);
		apiDoc.setShortName(shortName);
		apiDoc.setAlias(className);
		apiDoc.setUri(builder.getServerUrl() + DocGlobalConstants.PATH_DELIMITER + className);
		apiDoc.setProtocol(FrameworkEnum.DUBBO.getFramework());
		if (builder.getApiConfig().isMd5EncryptedHtmlName()) {
			String name = DocUtil.generateId(apiDoc.getName());
			apiDoc.setAlias(name);
		}
		apiDoc.setDesc(DocUtil.getEscapeAndCleanComment(comment));
		apiDoc.setList(apiMethodDocs);

		List<?> annotations = DocUtil.getClassAnnotations(cls);
		for (Object annotation : annotations) {
			String name = DocUtil.getAnnotationTypeFullyQualifiedName(annotation);
			if (!DubboAnnotationConstants.DUBBO_SERVICE.equals(name)) {
				continue;
			}
			Object versionValue = DocUtil.getAnnotationProperty(annotation, "version");
			if (Objects.nonNull(versionValue)) {
				String version = DocUtil.resolveAnnotationValue(builder.getApiConfig().getClassLoader(), versionValue);
				apiDoc.setVersion(StringUtil.removeDoubleQuotes(version));
			}
			Object protocolValue = DocUtil.getAnnotationProperty(annotation, "protocol");
			if (Objects.nonNull(protocolValue)) {
				String protocol = DocUtil.resolveAnnotationValue(builder.getApiConfig().getClassLoader(),
						protocolValue);
				apiDoc.setProtocol(StringUtil.removeDoubleQuotes(protocol));
			}
			Object interfaceNameValue = DocUtil.getAnnotationProperty(annotation, "interfaceName");
			if (Objects.nonNull(interfaceNameValue)) {
				String interfaceName = DocUtil.resolveAnnotationValue(builder.getApiConfig().getClassLoader(),
						interfaceNameValue);
				apiDoc.setName(StringUtil.removeDoubleQuotes(interfaceName));
			}
		}
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
			// set rpc protocol
			if (DocTags.PROTOCOL.equals(name)) {
				apiDoc.setProtocol(DocUtil.getDocletTagValue(docletTag));
			}
			// set rpc service name
			if (DocTags.SERVICE.equals(name)) {
				apiDoc.setName(DocUtil.getDocletTagValue(docletTag));
			}
		}
		apiDoc.setAuthor(String.join(", ", authorList));
		apiDocList.add(apiDoc);
	}

}
