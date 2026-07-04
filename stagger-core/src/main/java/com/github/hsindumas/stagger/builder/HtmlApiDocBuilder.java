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

import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.factory.BuildTemplateFactory;
import com.github.hsindumas.stagger.helper.JavaProjectBuilderHelper;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiDoc;
import com.github.hsindumas.stagger.template.IDocBuildTemplate;
import com.github.hsindumas.stagger.utils.BeetlTemplateUtil;
import com.github.hsindumas.stagger.common.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import com.github.hsindumas.stagger.template.engine.Template;

import java.util.List;
import java.util.Objects;

/**
 * HTML API Doc Builder
 *
 * @author yu 2019/9/20.
 * @author HsinDumas
 * @since 1.7+
 */
public class HtmlApiDocBuilder {

	/**
	 * error code html
	 */
	private static final String ERROR_CODE_HTML = "error.html";

	/**
	 * dict html
	 */
	private static final String DICT_HTML = "dict.html";

	/**
	 * index html
	 */
	private static String INDEX_HTML = "index.html";

	/**
	 * private constructor
	 */
	private HtmlApiDocBuilder() {
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
		DocBuilderTemplate builderTemplate = new DocBuilderTemplate();
		builderTemplate.checkAndInit(config, Boolean.TRUE);
		config.setParamsDataToTree(false);
		IDocBuildTemplate<ApiDoc> docBuildTemplate = BuildTemplateFactory.getDocBuildTemplate(config.getFramework(),
				config.getClassLoader());
		Objects.requireNonNull(docBuildTemplate, "doc build template is null");
		List<ApiDoc> apiDocList = docBuildTemplate.getApiData(configBuilder).getApiDatas();
		builderTemplate.copyJQueryAndCss(config);
		if (config.isAllInOne()) {
			apiDocList = docBuildTemplate.handleApiGroup(apiDocList, config);
			if (config.isCreateDebugPage()) {
				INDEX_HTML = DocGlobalConstants.DEBUG_PAGE_ALL_TPL;
				if (StringUtils.isNotEmpty(config.getAllInOneDocFileName())) {
					INDEX_HTML = config.getAllInOneDocFileName();
				}
				builderTemplate.buildAllInOne(apiDocList, config, configBuilder, DocGlobalConstants.DEBUG_PAGE_ALL_TPL,
						INDEX_HTML);
				Template mockJs = BeetlTemplateUtil.getByName(DocGlobalConstants.DEBUG_JS_TPL);
				FileUtil.nioWriteFile(mockJs.render(),
						config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + DocGlobalConstants.DEBUG_JS_OUT);
			}
			else {
				if (StringUtils.isNotEmpty(config.getAllInOneDocFileName())) {
					INDEX_HTML = config.getAllInOneDocFileName();
				}
				builderTemplate.buildAllInOne(apiDocList, config, configBuilder, DocGlobalConstants.ALL_IN_ONE_HTML_TPL,
						INDEX_HTML);
			}
			builderTemplate.buildSearchJs(config, configBuilder, apiDocList, DocGlobalConstants.SEARCH_ALL_JS_TPL);
		}
		else {
			String indexAlias;
			if (config.isCreateDebugPage()) {
				indexAlias = "debug";
				buildDoc(builderTemplate, apiDocList, config, configBuilder, DocGlobalConstants.DEBUG_PAGE_SINGLE_TPL,
						indexAlias);
				Template mockJs = BeetlTemplateUtil.getByName(DocGlobalConstants.DEBUG_JS_TPL);
				FileUtil.nioWriteFile(mockJs.render(),
						config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + DocGlobalConstants.DEBUG_JS_OUT);
			}
			else {
				indexAlias = "api";
				buildDoc(builderTemplate, apiDocList, config, configBuilder, DocGlobalConstants.SINGLE_INDEX_HTML_TPL,
						indexAlias);
			}
			builderTemplate.buildErrorCodeDoc(config, configBuilder, apiDocList,
					DocGlobalConstants.SINGLE_ERROR_HTML_TPL, ERROR_CODE_HTML, indexAlias);
			builderTemplate.buildDirectoryDataDoc(config, configBuilder, apiDocList,
					DocGlobalConstants.SINGLE_DICT_HTML_TPL, DICT_HTML, indexAlias);
			builderTemplate.buildSearchJs(config, configBuilder, apiDocList, DocGlobalConstants.SEARCH_JS_TPL);
		}

	}

	/**
	 * build ever controller api
	 * @param builderTemplate DocBuilderTemplate
	 * @param apiDocList list of api doc
	 * @param config ApiConfig
	 * @param configBuilder ProjectDocConfigBuilder
	 * @param template template
	 * @param indexHtml indexHtml
	 */
	private static void buildDoc(DocBuilderTemplate builderTemplate, List<ApiDoc> apiDocList, ApiConfig config,
			ProjectDocConfigBuilder configBuilder, String template, String indexHtml) {
		FileUtil.mkdirs(config.getOutPath());
		int index = 0;
		for (ApiDoc doc : apiDocList) {
			if (index == 0) {
				doc.setAlias(indexHtml);
			}
			builderTemplate.buildDoc(apiDocList, config, configBuilder, template, doc.getAlias() + ".html", doc,
					indexHtml);
			index++;
		}
	}

}
