/*
 * stagger
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
package com.github.hsindumas.stagger.gradle.constant;

/**
 * @author yu 2019/12/13.
 * @author HsinDumas
 */
public interface GlobalConstants {

	/**
	 * error message
	 */
	String ERROR_MSG = "Failed to build ApiConfig, check if the configuration file is correct.";

	/**
	 * default config file
	 */
	String DEFAULT_CONFIG = "./src/main/resources/default.json";

	/**
	 * Task group
	 */
	String TASK_GROUP = "stagger";

	/**
	 * Generate Rest html document
	 */
	String REST_HTML_TASK = "restHtml";

	/**
	 * Generate Rest markdown document
	 */
	String REST_MARKDOWN_TASK = "restMarkdown";

	/**
	 * Generate JMeter test document
	 */
	String JMETER_TASK = "jmeter";

	/**
	 * Generate Postman document
	 */
	String POSTMAN_TASK = "postman";

	/**
	 * Generate OpenAPI document
	 */
	String OPEN_API_TASK = "openApi";

	/**
	 * Generate Rpc html document
	 */
	String RPC_HTML_TASK = "rpcHtml";

	/**
	 * Generate Rpc markdown document
	 */
	String RPC_MARKDOWN_TASK = "rpcMarkdown";

	/**
	 * Generate rest document push to word
	 */
	String WORD_TASK = "word";

	/**
	 * Generate Swagger document
	 */
	String SWAGGER_TASK = "swagger";

	/**
	 * Generate WebSocket markdown document
	 */
	String WEBSOCKET_MARKDOWN_TASK = "webSocketMarkdown";

	/**
	 * Generate WebSocket html document
	 */
	String WEBSOCKET_HTML_TASK = "webSocketHtml";

	/**
	 * Generate gRPC html document
	 */
	String GRPC_HTML_TASK = "grpcHtml";

	/**
	 * Generate gRPC markdown document
	 */
	String GRPC_MARKDOWN_TASK = "grpcMarkdown";

	/**
	 * Plugin extension name
	 */
	String EXTENSION_NAME = "stagger";

	/**
	 * default java source dir
	 */
	String SRC_MAIN_JAVA_PATH = "src/main/java";

	String CONFIG_FILE = "stagger.configFile";

}
