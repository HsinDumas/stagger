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
import com.power.common.util.DateTimeUtil;

import java.util.List;

/**
 * Javadoc Markdown Builder
 *
 * @author chenchuxin
 * @author HsinDumas
 * @since 3.0.5
 */
public class JavadocMarkdownBuilder {

	/**
	 * private constructor
	 */
	private JavadocMarkdownBuilder() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * @param config ApiConfig
	 */
	public static void buildApiDoc(ApiConfig config) {
		buildApiDoc(config, new ProjectDocConfigBuilder(config, JavaProjectBuilderHelper.create()));
	}

	/**
	 * Only for stagger maven plugin and gradle plugin.
	 * @param apiConfig ApiConfig
	 * @param configBuilder ProjectDocConfigBuilder
	 */
	public static void buildApiDoc(ApiConfig apiConfig, ProjectDocConfigBuilder configBuilder) {
		JavadocDocBuilderTemplate builderTemplate = new JavadocDocBuilderTemplate();
		List<JavadocApiDoc> apiDocList = builderTemplate.getApiDoc(false, true, false, apiConfig, configBuilder);
		if (apiConfig.isAllInOne()) {
			String version = apiConfig.isCoverOld() ? "" : "-V" + DateTimeUtil.long2Str(System.currentTimeMillis(),
					DocGlobalConstants.DATE_FORMAT_YYYY_MM_DD_HH_MM);
			String docName = builderTemplate.allInOneDocName(apiConfig, "javadoc-all" + version,
					DocGlobalConstants.MARKDOWN_EXTENSION);
			builderTemplate.buildAllInOne(apiDocList, apiConfig, configBuilder,
					DocGlobalConstants.JAVADOC_ALL_IN_ONE_MD_TPL, docName);
		}
		else {
			builderTemplate.buildApiDoc(apiDocList, apiConfig, DocGlobalConstants.JAVADOC_API_DOC_MD_TPL,
					DocGlobalConstants.MARKDOWN_API_FILE_EXTENSION);
			builderTemplate.buildErrorCodeDoc(apiConfig, DocGlobalConstants.ERROR_CODE_LIST_MD_TPL,
					DocGlobalConstants.ERROR_CODE_LIST_MD, configBuilder);
		}
	}

}
