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
package com.github.hsindumas.stagger.builder;

import com.github.hsindumas.stagger.constants.TornaConstants;
import com.github.hsindumas.stagger.factory.BuildTemplateFactory;
import com.github.hsindumas.stagger.helper.JavaProjectBuilderHelper;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiDoc;
import com.github.hsindumas.stagger.model.ApiSchema;
import com.github.hsindumas.stagger.model.torna.Apis;
import com.github.hsindumas.stagger.model.torna.TornaApi;
import com.github.hsindumas.stagger.template.IDocBuildTemplate;
import com.github.hsindumas.stagger.utils.TornaUtil;
import com.github.hsindumas.stagger.helper.JavaProjectBuilder;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.github.hsindumas.stagger.constants.TornaConstants.DEFAULT_GROUP_CODE;

/**
 * Rest Api Torna Builder
 *
 * @author xingzi 2021/2/2 18:05
 * @author HsinDumas
 **/
public class TornaBuilder {

	/**
	 * private constructor
	 */
	private TornaBuilder() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * build controller api,for unit testing
	 * @param config config
	 */
	public static void buildApiDoc(ApiConfig config) {
		buildApiDoc(config, new ProjectDocConfigBuilder(config, JavaProjectBuilderHelper.create()));
	}

	/**
	 * Only for stagger Maven plugin and Gradle plugin.
	 * @param config ApiConfig
	 * @param configBuilder ProjectDocConfigBuilder
	 */
	public static void buildApiDoc(ApiConfig config, ProjectDocConfigBuilder configBuilder) {
		config.setParamsDataToTree(true);
		List<ApiDoc> apiDocList = generateApiDocs(config, configBuilder);
		buildTorna(apiDocList, config, configBuilder);
	}

	/**
	 * build torna Data
	 * @param apiDocs apiData
	 * @param apiConfig ApiConfig
	 * @param builder JavaProjectBuilder
	 */
	public static void buildTorna(List<ApiDoc> apiDocs, ApiConfig apiConfig, JavaProjectBuilder builder) {
		// Convert ApiDoc to TornaApi
		TornaApi tornaApi = convertToTornaApi(apiDocs, apiConfig, builder);
		// Push to torna
		TornaUtil.pushToTorna(tornaApi, apiConfig, builder);
	}

	/**
	 * build torna Data
	 * @param apiDocs apiData
	 * @param apiConfig ApiConfig
	 * @param configBuilder project doc config builder
	 */
	public static void buildTorna(List<ApiDoc> apiDocs, ApiConfig apiConfig, ProjectDocConfigBuilder configBuilder) {
		// Convert ApiDoc to TornaApi
		TornaApi tornaApi = convertToTornaApi(apiDocs, apiConfig, configBuilder);
		// Push to torna
		TornaUtil.pushToTorna(tornaApi, apiConfig, configBuilder);
	}

	/**
	 * Generate Torna API.
	 * @param config ApiConfig
	 * @return TornaApi
	 */
	public static TornaApi getTornaApi(ApiConfig config) {
		ProjectDocConfigBuilder configBuilder = new ProjectDocConfigBuilder(config, JavaProjectBuilderHelper.create());
		List<ApiDoc> apiDocs = generateApiDocs(config, configBuilder);
		return convertToTornaApi(apiDocs, config, configBuilder);
	}

	/**
	 * Convert List of ApiDoc to TornaApi
	 * @param apiDocs apiData
	 * @param apiConfig ApiConfig
	 * @param builder JavaProjectBuilder
	 * @return TornaApi
	 */
	private static TornaApi convertToTornaApi(List<ApiDoc> apiDocs, ApiConfig apiConfig, JavaProjectBuilder builder) {
		TornaApi tornaApi = new TornaApi();
		tornaApi.setAuthor(apiConfig.getAuthor());
		tornaApi.setIsReplace(BooleanUtils.toInteger(apiConfig.getReplace()));
		Apis api;
		List<Apis> groupApiList = new ArrayList<>();
		// Convert ApiDoc to Apis
		for (ApiDoc groupApi : apiDocs) {
			List<Apis> apisList = new ArrayList<>();
			List<ApiDoc> childrenApiDocs = groupApi.getChildrenApiDocs();
			for (ApiDoc a : childrenApiDocs) {
				api = new Apis();
				api.setName(StringUtils.isBlank(a.getDesc()) ? a.getName() : a.getDesc());
				api.setItems(TornaUtil.buildApis(a.getList(), TornaUtil.setDebugEnv(apiConfig, tornaApi)));
				api.setIsFolder(TornaConstants.YES);
				api.setAuthor(a.getAuthor());
				api.setDescription(a.getDetail());
				api.setOrderIndex(a.getOrder());
				apisList.add(api);
			}
			api = new Apis();
			api.setName(StringUtils.isBlank(groupApi.getDesc()) ? groupApi.getName() : groupApi.getDesc());
			api.setDescription(groupApi.getDetail());
			api.setAuthor(tornaApi.getAuthor());
			api.setOrderIndex(groupApi.getOrder());
			api.setIsFolder(TornaConstants.YES);
			api.setItems(apisList);
			groupApiList.add(api);

		}
		tornaApi.setCommonErrorCodes(TornaUtil.buildErrorCode(apiConfig, builder));
		// delete default group when only default group
		tornaApi.setApis(groupApiList.size() == 1 && DEFAULT_GROUP_CODE.equals(groupApiList.get(0).getName())
				? groupApiList.get(0).getItems() : groupApiList);
		return tornaApi;
	}

	/**
	 * Convert List of ApiDoc to TornaApi
	 * @param apiDocs apiData
	 * @param apiConfig ApiConfig
	 * @param configBuilder project doc config builder
	 * @return TornaApi
	 */
	private static TornaApi convertToTornaApi(List<ApiDoc> apiDocs, ApiConfig apiConfig,
			ProjectDocConfigBuilder configBuilder) {
		TornaApi tornaApi = new TornaApi();
		tornaApi.setAuthor(apiConfig.getAuthor());
		tornaApi.setIsReplace(BooleanUtils.toInteger(apiConfig.getReplace()));
		Apis api;
		List<Apis> groupApiList = new ArrayList<>();
		// Convert ApiDoc to Apis
		for (ApiDoc groupApi : apiDocs) {
			List<Apis> apisList = new ArrayList<>();
			List<ApiDoc> childrenApiDocs = groupApi.getChildrenApiDocs();
			for (ApiDoc a : childrenApiDocs) {
				api = new Apis();
				api.setName(StringUtils.isBlank(a.getDesc()) ? a.getName() : a.getDesc());
				api.setItems(TornaUtil.buildApis(a.getList(), TornaUtil.setDebugEnv(apiConfig, tornaApi)));
				api.setIsFolder(TornaConstants.YES);
				api.setAuthor(a.getAuthor());
				api.setDescription(a.getDetail());
				api.setOrderIndex(a.getOrder());
				apisList.add(api);
			}
			api = new Apis();
			api.setName(StringUtils.isBlank(groupApi.getDesc()) ? groupApi.getName() : groupApi.getDesc());
			api.setDescription(groupApi.getDetail());
			api.setAuthor(tornaApi.getAuthor());
			api.setOrderIndex(groupApi.getOrder());
			api.setIsFolder(TornaConstants.YES);
			api.setItems(apisList);
			groupApiList.add(api);

		}
		tornaApi.setCommonErrorCodes(TornaUtil.buildErrorCode(apiConfig, configBuilder));
		// delete default group when only default group
		tornaApi.setApis(groupApiList.size() == 1 && DEFAULT_GROUP_CODE.equals(groupApiList.get(0).getName())
				? groupApiList.get(0).getItems() : groupApiList);
		return tornaApi;
	}

	/**
	 * Generate API docs using the provided configuration and builder.
	 * @param config ApiConfig
	 * @param javaProjectBuilder JavaProjectBuilder
	 * @return List of ApiDoc
	 */
	private static List<ApiDoc> generateApiDocs(ApiConfig config, JavaProjectBuilder javaProjectBuilder) {
		return generateApiDocs(config, new ProjectDocConfigBuilder(config, javaProjectBuilder));
	}

	/**
	 * Generate API docs using the provided configuration and builder.
	 * @param config ApiConfig
	 * @param configBuilder project doc config builder
	 * @return List of ApiDoc
	 */
	private static List<ApiDoc> generateApiDocs(ApiConfig config, ProjectDocConfigBuilder configBuilder) {
		DocBuilderTemplate builderTemplate = new DocBuilderTemplate();
		builderTemplate.checkAndInit(config, Boolean.FALSE);
		IDocBuildTemplate<ApiDoc> docBuildTemplate = BuildTemplateFactory.getDocBuildTemplate(config.getFramework(),
				config.getClassLoader());
		Objects.requireNonNull(docBuildTemplate, "doc build template is null");
		ApiSchema<ApiDoc> apiSchema = docBuildTemplate.getApiData(configBuilder);
		return docBuildTemplate.handleApiGroup(apiSchema.getApiDatas(), config);
	}

}
