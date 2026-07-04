/*
 *
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
package com.github.hsindumas.stagger.builder.grpc;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.helper.JavaProjectBuilderHelper;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.grpc.GrpcApiDoc;
import com.github.hsindumas.stagger.common.util.DateTimeUtil;

import java.util.List;

/**
 * grpc markdown builder.
 *
 * @author linwumingshi
 * @author HsinDumas
 * @since 3.0.7
 */
public class GrpcMarkdownBuilder {

	/**
	 * private constructor
	 */
	private GrpcMarkdownBuilder() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * build api doc.
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
		GrpcDocBuilderTemplate grpcDocBuilderTemplate = new GrpcDocBuilderTemplate();
		List<GrpcApiDoc> apiDocList = grpcDocBuilderTemplate.getApiDoc(false, true, false, apiConfig, configBuilder);
		if (apiConfig.isAllInOne()) {
			String version = apiConfig.isCoverOld() ? "" : "-V" + DateTimeUtil.long2Str(System.currentTimeMillis(),
					DocGlobalConstants.DATE_FORMAT_YYYY_MM_DD_HH_MM);
			String docName = grpcDocBuilderTemplate.allInOneDocName(apiConfig, "grpc-all" + version,
					DocGlobalConstants.MARKDOWN_EXTENSION);
			grpcDocBuilderTemplate.buildAllInOne(apiDocList, apiConfig, configBuilder,
					DocGlobalConstants.GRPC_ALL_IN_ONE_MD_TPL, docName);
		}
		else {
			grpcDocBuilderTemplate.buildApiDoc(apiDocList, apiConfig, DocGlobalConstants.GRPC_API_MD_TPL,
					DocGlobalConstants.MARKDOWN_API_FILE_EXTENSION);

			grpcDocBuilderTemplate.buildErrorCodeDoc(apiConfig, DocGlobalConstants.ERROR_CODE_LIST_MD_TPL,
					DocGlobalConstants.ERROR_CODE_LIST_MD, configBuilder);
		}
	}

}
