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
package com.github.hsindumas.stagger.builder.javadoc;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.helper.JavaProjectBuilderHelper;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.javadoc.JavadocApiDoc;

import java.util.List;

/**
 * Javadoc Asciidoc Builder
 *
 * @author chenchuxin
 * @author HsinDumas
 * @since 3.0.5
 */
public class JavadocAdocBuilder {

	/**
	 * api extension
	 */
	private static final String API_EXTENSION = "JavadocApi.adoc";

	/**
	 * index extension
	 */
	private static final String INDEX_DOC = "javadoc-index.adoc";

	/**
	 * private constructor
	 */
	private JavadocAdocBuilder() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * build adoc
	 * @param config ApiConfig
	 */
	public static void buildApiDoc(ApiConfig config) {
		buildApiDoc(config, new ProjectDocConfigBuilder(config, JavaProjectBuilderHelper.create()));
	}

	/**
	 * Only for stagger maven plugin and gradle plugin.
	 * @param config ApiConfig
	 * @param configBuilder ProjectDocConfigBuilder
	 */
	public static void buildApiDoc(ApiConfig config, ProjectDocConfigBuilder configBuilder) {
		JavadocDocBuilderTemplate builderTemplate = new JavadocDocBuilderTemplate();
		List<JavadocApiDoc> apiDocList = builderTemplate.getApiDoc(true, true, false, config, configBuilder);
		if (config.isAllInOne()) {
			String docName = builderTemplate.allInOneDocName(config, INDEX_DOC, DocGlobalConstants.ASCIIDOC_EXTENSION);
			builderTemplate.buildAllInOne(apiDocList, config, configBuilder,
					DocGlobalConstants.JAVADOC_ALL_IN_ONE_ADOC_TPL, docName);
		}
		else {
			builderTemplate.buildApiDoc(apiDocList, config, DocGlobalConstants.JAVADOC_API_DOC_ADOC_TPL, API_EXTENSION);
			builderTemplate.buildErrorCodeDoc(config, DocGlobalConstants.ERROR_CODE_LIST_ADOC_TPL,
					DocGlobalConstants.ERROR_CODE_LIST_ADOC, configBuilder);
		}
	}

}
