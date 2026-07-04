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
package com.github.hsindumas.stagger.builder.websocket;

import com.github.hsindumas.stagger.builder.IBaseDocBuilderTemplate;
import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.constants.HighlightStyle;
import com.github.hsindumas.stagger.constants.TemplateVariable;
import com.github.hsindumas.stagger.factory.BuildTemplateFactory;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiDocDict;
import com.github.hsindumas.stagger.model.ApiErrorCode;
import com.github.hsindumas.stagger.model.WebSocketDoc;
import com.github.hsindumas.stagger.template.IWebSocketDocBuildTemplate;
import com.github.hsindumas.stagger.utils.BeetlTemplateUtil;
import com.github.hsindumas.stagger.utils.DocUtil;
import com.github.hsindumas.stagger.common.util.CollectionUtil;
import com.github.hsindumas.stagger.common.util.FileUtil;
import com.github.hsindumas.stagger.helper.JavaProjectBuilder;
import com.github.hsindumas.stagger.template.engine.Template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * WebSocket doc builder template.
 *
 * @author linwumingshi
 * @author HsinDumas
 * @since 3.0.7
 */
public class WebSocketDocBuilderTemplate implements IBaseDocBuilderTemplate<WebSocketDoc> {

	/**
	 * get all websocket api data.
	 * @param isAsciidoc is Asciidoc
	 * @param config ApiConfig
	 * @param javaProjectBuilder JavaProjectBuilder
	 * @return ApiAllData
	 */
	public List<WebSocketDoc> getWebSocketApiDoc(boolean isAsciidoc, ApiConfig config,
			JavaProjectBuilder javaProjectBuilder) {
		config.setAdoc(isAsciidoc);
		this.checkAndInit(config, Boolean.TRUE);
		config.setParamsDataToTree(false);
		config
			.setOutPath(config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + DocGlobalConstants.WEBSOCKET_OUT_DIR);
		return this.getWebSocketApiDoc(config, javaProjectBuilder);

	}

	/**
	 * get all websocket api data.
	 * @param isAsciidoc is Asciidoc
	 * @param config ApiConfig
	 * @param configBuilder project doc config builder
	 * @return ApiAllData
	 */
	public List<WebSocketDoc> getWebSocketApiDoc(boolean isAsciidoc, ApiConfig config,
			ProjectDocConfigBuilder configBuilder) {
		config.setAdoc(isAsciidoc);
		this.checkAndInit(config, Boolean.TRUE);
		config.setParamsDataToTree(false);
		config
			.setOutPath(config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + DocGlobalConstants.WEBSOCKET_OUT_DIR);
		return this.getWebSocketApiDoc(config, configBuilder);

	}

	/**
	 * get all websocket api data.
	 * @param config ApiConfig
	 * @param javaProjectBuilder JavaProjectBuilder
	 * @return ApiAllData
	 */
	public List<WebSocketDoc> getWebSocketApiDoc(ApiConfig config, JavaProjectBuilder javaProjectBuilder) {
		config.setShowJavaType(false);
		ProjectDocConfigBuilder configBuilder = new ProjectDocConfigBuilder(config, javaProjectBuilder);
		IWebSocketDocBuildTemplate<WebSocketDoc> webSocketDocBuildTemplate = BuildTemplateFactory
			.getWebSocketDocBuildTemplate(config.getFramework(), config.getClassLoader());
		Objects.requireNonNull(webSocketDocBuildTemplate, "doc build websocket template is null");
		return webSocketDocBuildTemplate.getWebSocketData(configBuilder);
	}

	/**
	 * get all websocket api data.
	 * @param config ApiConfig
	 * @param configBuilder project doc config builder
	 * @return ApiAllData
	 */
	public List<WebSocketDoc> getWebSocketApiDoc(ApiConfig config, ProjectDocConfigBuilder configBuilder) {
		config.setShowJavaType(false);
		IWebSocketDocBuildTemplate<WebSocketDoc> webSocketDocBuildTemplate = BuildTemplateFactory
			.getWebSocketDocBuildTemplate(config.getFramework(), config.getClassLoader());
		Objects.requireNonNull(webSocketDocBuildTemplate, "doc build websocket template is null");
		return webSocketDocBuildTemplate.getWebSocketData(configBuilder);
	}

	/**
	 * Merge all websocket api doc into one document.
	 * @param webSocketDocList list data of Api doc
	 * @param config api config
	 * @param javaProjectBuilder JavaProjectBuilder
	 * @param template template
	 * @param outPutFileName output file
	 */
	public void buildWebSocketAllInOne(List<WebSocketDoc> webSocketDocList, ApiConfig config,
			JavaProjectBuilder javaProjectBuilder, String template, String outPutFileName) {
		String outPath = config.getOutPath();
		FileUtil.mkdirs(outPath);
		Template tpl = BeetlTemplateUtil.getByName(template);
		String style = config.getStyle();
		tpl.binding(TemplateVariable.STYLE.getVariable(), style);
		tpl.binding(TemplateVariable.HIGH_LIGHT_CSS_LINK.getVariable(), config.getHighlightStyleLink());
		tpl.binding(TemplateVariable.BACKGROUND.getVariable(), HighlightStyle.getBackgroundColor(style));
		tpl.binding(TemplateVariable.WEBSOCKET_DOC_LIST.getVariable(), webSocketDocList);
		tpl.binding(TemplateVariable.LANGUAGE.getVariable(), config.getLanguage());
		this.setCssCDN(config, tpl);
		// binding common variable
		this.bindingCommonVariable(config, javaProjectBuilder, tpl, webSocketDocList.isEmpty());
		FileUtil.nioWriteFile(tpl.render(), outPath + DocGlobalConstants.FILE_SEPARATOR + outPutFileName);
	}

	/**
	 * Merge all websocket api doc into one document.
	 * @param webSocketDocList list data of Api doc
	 * @param config api config
	 * @param configBuilder project doc config builder
	 * @param template template
	 * @param outPutFileName output file
	 */
	public void buildWebSocketAllInOne(List<WebSocketDoc> webSocketDocList, ApiConfig config,
			ProjectDocConfigBuilder configBuilder, String template, String outPutFileName) {
		String outPath = config.getOutPath();
		FileUtil.mkdirs(outPath);
		Template tpl = BeetlTemplateUtil.getByName(template);
		String style = config.getStyle();
		tpl.binding(TemplateVariable.STYLE.getVariable(), style);
		tpl.binding(TemplateVariable.HIGH_LIGHT_CSS_LINK.getVariable(), config.getHighlightStyleLink());
		tpl.binding(TemplateVariable.BACKGROUND.getVariable(), HighlightStyle.getBackgroundColor(style));
		tpl.binding(TemplateVariable.WEBSOCKET_DOC_LIST.getVariable(), webSocketDocList);
		tpl.binding(TemplateVariable.LANGUAGE.getVariable(), config.getLanguage());
		this.setCssCDN(config, tpl);
		this.bindingCommonVariable(config, configBuilder, tpl, webSocketDocList.isEmpty());
		FileUtil.nioWriteFile(tpl.render(), outPath + DocGlobalConstants.FILE_SEPARATOR + outPutFileName);
	}

	/**
	 * Generate websocket api documentation for all controllers.
	 * @param apiDocList list of api doc
	 * @param config api config
	 * @param template template
	 * @param fileExtension file extension
	 */
	public void buildWebSocketApiDoc(List<WebSocketDoc> apiDocList, ApiConfig config, String template,
			String fileExtension) {
		FileUtil.mkdirs(config.getOutPath());
		for (WebSocketDoc doc : apiDocList) {
			Template mapper = this.buildWebSocketApiDocTemplate(doc, config, template);
			FileUtil.nioWriteFile(mapper.render(),
					config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + doc.getName() + fileExtension);
		}
	}

	/**
	 * build websocket api doc template
	 * @param webSocketDoc websocket api doc
	 * @param config api config
	 * @param template template
	 * @return Template
	 */
	public Template buildWebSocketApiDocTemplate(WebSocketDoc webSocketDoc, ApiConfig config, String template) {
		Template mapper = BeetlTemplateUtil.getByName(template);
		mapper.binding(TemplateVariable.DEPRECATED.getVariable(), webSocketDoc.getDeprecated());
		mapper.binding(TemplateVariable.DESC.getVariable(), webSocketDoc.getDesc());
		mapper.binding(TemplateVariable.URI.getVariable(), webSocketDoc.getUri());
		mapper.binding(TemplateVariable.AUTHOR.getVariable(), webSocketDoc.getAuthor());
		mapper.binding(TemplateVariable.SUB_PROTOCOLS.getVariable(), webSocketDoc.getSubProtocols());
		mapper.binding(TemplateVariable.WEBSOCKET_PATH_PARAMS.getVariable(), webSocketDoc.getPathParams());
		mapper.binding(TemplateVariable.WEBSOCKET_MESSAGE_PARAMS.getVariable(), webSocketDoc.getMessageParams());
		mapper.binding(TemplateVariable.WEBSOCKET_RESPONSE_PARAMS.getVariable(), webSocketDoc.getResponseParams());
		return mapper;
	}

	/**
	 * Build search js.
	 * @param apiDocList list data of Api doc
	 * @param config api config
	 * @param javaProjectBuilder projectBuilder
	 * @param template template
	 * @param outPutFileName output file
	 */
	public void buildSearchJs(List<WebSocketDoc> apiDocList, ApiConfig config, JavaProjectBuilder javaProjectBuilder,
			String template, String outPutFileName) {
		List<ApiErrorCode> errorCodeList = DocUtil.errorCodeDictToList(config, javaProjectBuilder);
		Template tpl = BeetlTemplateUtil.getByName(template);
		// add order
		List<WebSocketDoc> apiDocs = new ArrayList<>();
		for (WebSocketDoc apiDoc1 : apiDocList) {
			apiDoc1.setOrder(apiDocs.size() + 1);
			apiDocs.add(apiDoc1);
		}
		Map<String, String> titleMap = this.setDirectoryLanguageVariable(config, tpl);
		if (CollectionUtil.isNotEmpty(errorCodeList)) {
			WebSocketDoc apiDoc1 = new WebSocketDoc();
			apiDoc1.setOrder(apiDocs.size() + 1);
			apiDoc1.setDesc(titleMap.get(TemplateVariable.ERROR_LIST_TITLE.getVariable()));
			apiDoc1.setList(new ArrayList<>(0));
			apiDocs.add(apiDoc1);
		}

		// set dict list
		List<ApiDocDict> apiDocDictList = DocUtil.buildDictionary(config, javaProjectBuilder);
		tpl.binding(TemplateVariable.DICT_LIST.getVariable(), apiDocDictList);
		tpl.binding(TemplateVariable.DIRECTORY_TREE.getVariable(), apiDocs);
		FileUtil.nioWriteFile(tpl.render(), config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + outPutFileName);
	}

	/**
	 * Build search js.
	 * @param apiDocList list data of Api doc
	 * @param config api config
	 * @param configBuilder project doc config builder
	 * @param template template
	 * @param outPutFileName output file
	 */
	public void buildSearchJs(List<WebSocketDoc> apiDocList, ApiConfig config, ProjectDocConfigBuilder configBuilder,
			String template, String outPutFileName) {
		List<ApiErrorCode> errorCodeList = DocUtil.errorCodeDictToList(config, configBuilder);
		Template tpl = BeetlTemplateUtil.getByName(template);
		List<WebSocketDoc> apiDocs = new ArrayList<>();
		for (WebSocketDoc apiDoc1 : apiDocList) {
			apiDoc1.setOrder(apiDocs.size() + 1);
			apiDocs.add(apiDoc1);
		}
		Map<String, String> titleMap = this.setDirectoryLanguageVariable(config, tpl);
		if (CollectionUtil.isNotEmpty(errorCodeList)) {
			WebSocketDoc apiDoc1 = new WebSocketDoc();
			apiDoc1.setOrder(apiDocs.size() + 1);
			apiDoc1.setDesc(titleMap.get(TemplateVariable.ERROR_LIST_TITLE.getVariable()));
			apiDoc1.setList(new ArrayList<>(0));
			apiDocs.add(apiDoc1);
		}

		List<ApiDocDict> apiDocDictList = DocUtil.buildDictionary(config, configBuilder);
		tpl.binding(TemplateVariable.DICT_LIST.getVariable(), apiDocDictList);
		tpl.binding(TemplateVariable.DIRECTORY_TREE.getVariable(), apiDocs);
		FileUtil.nioWriteFile(tpl.render(), config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + outPutFileName);
	}

}
