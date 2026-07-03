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
import com.github.hsindumas.stagger.constants.TornaConstants;
import com.github.hsindumas.stagger.helper.DocBuildHelper;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiDoc;
import com.github.hsindumas.stagger.model.ApiGroup;
import com.github.hsindumas.stagger.model.ApiMethodDoc;
import com.github.hsindumas.stagger.model.annotation.EntryAnnotation;
import com.github.hsindumas.stagger.model.annotation.FrameworkAnnotations;
import com.github.hsindumas.stagger.model.dependency.FileDiff;
import com.github.hsindumas.stagger.source.SourceAnnotation;
import com.github.hsindumas.stagger.source.SourceClass;
import com.github.hsindumas.stagger.utils.DocClassUtil;
import com.github.hsindumas.stagger.utils.DocPathUtil;
import com.github.hsindumas.stagger.utils.DocUtil;
import com.github.hsindumas.stagger.utils.JavaClassUtil;
import com.power.common.util.CollectionUtil;
import com.power.common.util.StringUtil;
import com.github.hsindumas.stagger.helper.JavaProjectBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * doc build base template interface.
 *
 * @author linwumingshi
 */
public interface IDocBuildBaseTemplate {

	/**
	 * support framework.
	 * @param framework framework
	 * @return boolean
	 */
	boolean supportsFramework(String framework);

	/**
	 * pre render.
	 * @param docBuildHelper docBuildHelper
	 */
	default void preRender(DocBuildHelper docBuildHelper) {

	}

	/**
	 * handle group api docs.
	 * @param apiDocList list of apiDocList
	 * @param apiConfig ApiConfig apiConfig
	 * @return List of ApiDoc
	 * @author cqmike
	 * @author HsinDumas
	 */
	default List<ApiDoc> handleApiGroup(List<ApiDoc> apiDocList, ApiConfig apiConfig) {
		if (CollectionUtil.isEmpty(apiDocList) || apiConfig == null) {
			return apiDocList;
		}
		List<ApiGroup> groups = apiConfig.getGroups();
		List<ApiDoc> finalApiDocs = new ArrayList<>();

		ApiDoc defaultGroup = ApiDoc.buildGroupApiDoc(TornaConstants.DEFAULT_GROUP_CODE);
		// show default group
		AtomicInteger order = new AtomicInteger(1);
		finalApiDocs.add(defaultGroup);

		if (CollectionUtil.isEmpty(groups)) {
			defaultGroup.setOrder(order.getAndIncrement());
			defaultGroup.getChildrenApiDocs().addAll(apiDocList);
			return finalApiDocs;
		}
		Map<String, String> hasInsert = new HashMap<>(16);
		for (ApiGroup group : groups) {
			ApiDoc groupApiDoc = ApiDoc.buildGroupApiDoc(group.getName());
			finalApiDocs.add(groupApiDoc);
			for (ApiDoc doc : apiDocList) {
				if (hasInsert.containsKey(doc.getAlias())) {
					continue;
				}
				if (!DocUtil.isMatch(group.getApis(), doc.getPackageName() + "." + doc.getName())) {
					continue;
				}
				hasInsert.put(doc.getAlias(), null);
				groupApiDoc.getChildrenApiDocs().add(doc);
				doc.setOrder(groupApiDoc.getChildrenApiDocs().size());
				doc.setGroup(group.getName());
				if (StringUtil.isEmpty(group.getPaths())) {
					continue;
				}
				List<ApiMethodDoc> methodDocs = doc.getList()
					.stream()
					.filter(l -> DocPathUtil.matches(l.getPath(), group.getPaths(), null))
					.collect(Collectors.toList());
				doc.setList(methodDocs);
			}
		}
		// Ungrouped join the default group
		for (ApiDoc doc : apiDocList) {
			String key = doc.getAlias();
			if (!hasInsert.containsKey(key)) {
				defaultGroup.getChildrenApiDocs().add(doc);
				doc.setOrder(defaultGroup.getChildrenApiDocs().size());
				hasInsert.put(doc.getAlias(), null);
			}
		}
		if (CollectionUtil.isEmpty(defaultGroup.getChildrenApiDocs())) {
			finalApiDocs.remove(defaultGroup);
		}
		finalApiDocs.forEach(group -> group.setOrder(order.getAndIncrement()));
		return finalApiDocs;
	}

	/**
	 * If build doc incrementally, we will filter the classes changed from the commit-id
	 * in increment-config-file. If not, we will return all classes.
	 * @param docBuilder docBuilder
	 * @param docBuildHelper incrementHelper
	 * @return the candidate classes
	 */
	default Collection<?> getCandidateClasses(ProjectDocConfigBuilder docBuilder, DocBuildHelper docBuildHelper) {
		ApiConfig apiConfig = docBuilder.getApiConfig();
		Collection<?> allClasses = docBuilder.getClassFilesMap().values();

		if (!apiConfig.isIncrement()) {
			return allClasses;
		}

		if (docBuildHelper.notGitRepo() || StringUtil.isEmpty(docBuildHelper.getDependencyTree().getCommitId())) {
			// There is no commit-id, which means the user haven't built the whole
			// project.
			// We need to build the whole project this time,
			// and record the latest commit-id and the newest api dependency tree.
			return allClasses;
		}

		Set<FileDiff> fileDiffList = docBuildHelper.getChangedFilesFromVCS(s -> isEntryPoint(docBuilder, s));
		if (CollectionUtil.isEmpty(fileDiffList)) {
			return Collections.emptyList();
		}

		Collection<Object> result = new ArrayList<>(fileDiffList.size());
		fileDiffList.forEach(item -> {
			try {
				Object javaClass = docBuilder.getClassByName(item.getNewQualifiedName());
				if (Objects.nonNull(javaClass)) {
					result.add(javaClass);
				}
			}
			catch (Exception ignore) {
				// Ignore unresolved changed class and continue processing remaining
				// entries.
			}
		});

		return result;
	}

	/**
	 * registered annotations.
	 * @return registered annotations
	 */
	FrameworkAnnotations registeredAnnotations();

	/**
	 * is entry point.
	 * @param javaProjectBuilder javaProjectBuilder
	 * @param javaClassName javaClassName
	 * @return is entry point
	 */
	default boolean isEntryPoint(JavaProjectBuilder javaProjectBuilder, String javaClassName) {
		if (StringUtil.isEmpty(javaClassName)) {
			return false;
		}

		Object javaClass = null;
		try {
			javaClass = javaProjectBuilder.getClassByName(javaClassName);
		}
		catch (Exception ignore) {
			// Ignore lookup failures and fall back to SourceProject metadata checks.
		}

		if (javaClass == null) {
			return false;
		}

		return this.isEntryPoint(javaClass, this.registeredAnnotations());
	}

	/**
	 * Determine whether a changed class should be treated as entry-point candidate. Falls
	 * back to SourceProject metadata when legacy parser lookup is unavailable.
	 * @param docBuilder doc builder
	 * @param javaClassName class name
	 * @return true if class is entry point
	 */
	default boolean isEntryPoint(ProjectDocConfigBuilder docBuilder, String javaClassName) {
		if (StringUtil.isEmpty(javaClassName)) {
			return false;
		}

		Object javaClass = null;
		try {
			javaClass = docBuilder.getClassByName(javaClassName);
		}
		catch (Exception ignore) {
			// Ignore lookup failures and use SourceProject fallback below.
		}

		if (Objects.nonNull(javaClass)) {
			return this.isEntryPoint(javaClass, this.registeredAnnotations());
		}

		SourceClass sourceClass = docBuilder.findSourceClass(javaClassName).orElse(null);
		return this.isSourceEntryPoint(sourceClass, this.registeredAnnotations());
	}

	/**
	 * Determine whether a SourceClass is entry point by registered entry annotations.
	 * @param sourceClass source class
	 * @param frameworkAnnotations framework annotations
	 * @return true when source class matches entry annotation definitions
	 */
	default boolean isSourceEntryPoint(SourceClass sourceClass, FrameworkAnnotations frameworkAnnotations) {
		if (Objects.isNull(sourceClass) || DocClassUtil.isAnnotationOrEnum(sourceClass)
				|| Objects.isNull(frameworkAnnotations)) {
			return false;
		}
		Map<String, EntryAnnotation> entryAnnotationMap = frameworkAnnotations.getEntryAnnotations();
		if (Objects.isNull(entryAnnotationMap)) {
			return false;
		}
		return sourceClass.annotations().stream().map(SourceAnnotation::qualifiedName).anyMatch(annotationName -> {
			String simpleName = JavaClassUtil.getClassSimpleName(annotationName);
			return entryAnnotationMap.containsKey(annotationName) || entryAnnotationMap.containsKey(simpleName);
		});
	}

	/**
	 * Determines if a class should be skipped based on configuration and class
	 * annotations. This method is used to decide whether a class should be documented or
	 * ignored during the documentation generation process. It primarily checks the class
	 * against the configured package filters and exclusion filters, as well as checks for
	 * the presence of an ignore annotation.
	 * @param apiConfig The API configuration object, containing package filter and
	 * exclusion filter settings.
	 * @param javaClass The class object to be checked.
	 * @param frameworkAnnotations The framework annotation object, used to check if the
	 * class is an entry point.
	 * @return true if the class should be skipped, otherwise false.
	 */
	default boolean skipClass(ApiConfig apiConfig, Object javaClass, FrameworkAnnotations frameworkAnnotations) {
		if (StringUtil.isNotEmpty(apiConfig.getPackageFilters())) {
			// from smart config
			if (!DocUtil.isMatch(apiConfig.getPackageFilters(), javaClass)) {
				return true;
			}
		}
		if (StringUtil.isNotEmpty(apiConfig.getPackageExcludeFilters())) {
			if (DocUtil.isMatch(apiConfig.getPackageExcludeFilters(), javaClass)) {
				return true;
			}
		}
		// from tag
		return !this.isEntryPoint(javaClass, frameworkAnnotations)
				|| Objects.nonNull(DocUtil.getClassTagByName(javaClass, DocTags.IGNORE));
	}

	/**
	 * is entry point.
	 * @param javaClass javaClass
	 * @param frameworkAnnotations frameworkAnnotations
	 * @return is entry point
	 */
	boolean isEntryPoint(Object javaClass, FrameworkAnnotations frameworkAnnotations);

}
