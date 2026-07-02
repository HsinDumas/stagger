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
package com.github.hsindumas.stagger.builder.rpc;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.TornaConstants;
import com.github.hsindumas.stagger.helper.JavaProjectBuilderHelper;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.rpc.RpcApiDoc;
import com.github.hsindumas.stagger.model.torna.Apis;
import com.github.hsindumas.stagger.model.torna.DubboInfo;
import com.github.hsindumas.stagger.model.torna.TornaApi;
import com.github.hsindumas.stagger.utils.TornaUtil;
import com.thoughtworks.qdox.JavaProjectBuilder;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dubbo RPC Torna Builder.
 *
 * @author xingzi 2021/4/28 16:14
 * @author HsinDumas
 */
public class RpcTornaBuilder {

	/**
	 * private constructor
	 */
	private RpcTornaBuilder() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * build controller api
	 * @param config config
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
		RpcDocBuilderTemplate builderTemplate = new RpcDocBuilderTemplate();
		List<RpcApiDoc> apiDocList = builderTemplate.getApiDoc(false, true, true, config,
				configBuilder.getJavaProjectBuilder());
		buildTorna(apiDocList, config, configBuilder.getJavaProjectBuilder());
	}

	/**
	 * Build Torna data and push it to the Torna server.
	 * @param apiDocs List of RpcApiDoc
	 * @param apiConfig ApiConfig
	 * @param builder JavaProjectBuilder
	 */
	public static void buildTorna(List<RpcApiDoc> apiDocs, ApiConfig apiConfig, JavaProjectBuilder builder) {
		TornaApi tornaApi = convertToTornaApi(apiDocs, apiConfig, builder);
		// Push to torna
		TornaUtil.pushToTorna(tornaApi, apiConfig, builder);
	}

	/**
	 * Get Torna API documentation.
	 * @param config ApiConfig
	 * @return TornaApi
	 */
	public static TornaApi getTornaApi(ApiConfig config) {
		RpcDocBuilderTemplate builderTemplate = new RpcDocBuilderTemplate();
		JavaProjectBuilder javaProjectBuilder = JavaProjectBuilderHelper.create();
		List<RpcApiDoc> apiDocList = builderTemplate.getApiDoc(false, true, true, config, javaProjectBuilder);
		return convertToTornaApi(apiDocList, config, javaProjectBuilder);
	}

	/**
	 * Convert a list of RpcApiDoc to TornaApi.
	 * @param apiDocs List of RpcApiDoc
	 * @param apiConfig ApiConfig
	 * @param builder JavaProjectBuilder
	 * @return TornaApi
	 */
	public static TornaApi convertToTornaApi(List<RpcApiDoc> apiDocs, ApiConfig apiConfig, JavaProjectBuilder builder) {
		TornaApi tornaApi = new TornaApi();
		tornaApi.setAuthor(apiConfig.getAuthor());
		tornaApi.setIsReplace(BooleanUtils.toInteger(apiConfig.getReplace()));

		List<Apis> apisList = new ArrayList<>();
		// Add data
		for (RpcApiDoc rpcApiDoc : apiDocs) {
			Apis api = createApi(rpcApiDoc, apiConfig, tornaApi);
			apisList.add(api);
		}

		tornaApi.setCommonErrorCodes(TornaUtil.buildErrorCode(apiConfig, builder));
		tornaApi.setApis(apisList);
		return tornaApi;
	}

	/**
	 * Helper method to create Apis object from RpcApiDoc.
	 * @param rpcApiDoc RpcApiDoc
	 * @param apiConfig ApiConfig
	 * @param tornaApi TornaApi
	 * @return Apis
	 */
	private static Apis createApi(RpcApiDoc rpcApiDoc, ApiConfig apiConfig, TornaApi tornaApi) {
		Apis api = new Apis();
		api.setName(StringUtils.isBlank(rpcApiDoc.getDesc()) ? rpcApiDoc.getName() : rpcApiDoc.getDesc());
		TornaUtil.setDebugEnv(apiConfig, tornaApi);
		api.setItems(TornaUtil.buildDubboApis(rpcApiDoc.getList()));
		api.setIsFolder(TornaConstants.YES);
		api.setAuthor(rpcApiDoc.getAuthor());
		api.setDubboInfo(new DubboInfo().setAuthor(rpcApiDoc.getAuthor())
			.setProtocol(rpcApiDoc.getProtocol())
			.setVersion(rpcApiDoc.getVersion())
			.setDependency(TornaUtil.buildDependencies(apiConfig.getRpcApiDependencies()))
			.setInterfaceName(rpcApiDoc.getName()));
		api.setOrderIndex(rpcApiDoc.getOrder());
		return api;
	}

}
