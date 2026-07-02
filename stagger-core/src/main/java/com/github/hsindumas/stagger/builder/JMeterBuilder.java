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
import com.power.common.util.DateTimeUtil;
import com.power.common.util.StringUtil;

import java.util.List;
import java.util.Objects;

/**
 * used to generate jmx file for Jmeter
 *
 * @author Lansg
 * @author HsinDumas
 */
public class JMeterBuilder {

	/**
	 * Jmeter script extension
	 */
	private static final String JMETER_SCRIPT_EXTENSION = ".jmx";

	/**
	 * private constructor
	 */
	private JMeterBuilder() {
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
	 * @param config ApiConfig
	 * @param configBuilder ProjectDocConfigBuilder
	 */
	public static void buildApiDoc(ApiConfig config, ProjectDocConfigBuilder configBuilder) {
		DocBuilderTemplate builderTemplate = new DocBuilderTemplate();
		builderTemplate.checkAndInit(config, Boolean.TRUE);
		config.setAdoc(false);
		config.setShowJavaType(true);
		config.setParamsDataToTree(Boolean.FALSE);
		IDocBuildTemplate<ApiDoc> docBuildTemplate = BuildTemplateFactory.getDocBuildTemplate(config.getFramework(),
				config.getClassLoader());
		Objects.requireNonNull(docBuildTemplate, "doc build template is null");
		List<ApiDoc> apiDocList = docBuildTemplate.getApiData(configBuilder).getApiDatas();
		String version = config.isCoverOld() ? "" : "-V"
				+ DateTimeUtil.long2Str(System.currentTimeMillis(), DocGlobalConstants.DATE_FORMAT_YYYY_MM_DD_HH_MM);
		String docName;
		if (StringUtil.isNotEmpty(config.getProjectName())) {
			docName = config.getProjectName() + version + JMETER_SCRIPT_EXTENSION;
		}
		else {
			docName = "jmeter-script" + version + JMETER_SCRIPT_EXTENSION;
		}
		builderTemplate.buildAllInOne(apiDocList, config, configBuilder, DocGlobalConstants.JMETER_TPL, docName);
	}

}
