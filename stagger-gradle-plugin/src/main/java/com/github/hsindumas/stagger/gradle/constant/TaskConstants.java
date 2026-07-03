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

import com.github.hsindumas.stagger.gradle.task.DocBaseTask;
import com.github.hsindumas.stagger.gradle.task.GrpcHtmlTask;
import com.github.hsindumas.stagger.gradle.task.GrpcMarkdownTask;
import com.github.hsindumas.stagger.gradle.task.JMeterTask;
import com.github.hsindumas.stagger.gradle.task.JavadocHtmlTask;
import com.github.hsindumas.stagger.gradle.task.JavadocMarkdownTask;
import com.github.hsindumas.stagger.gradle.task.OpenApiTask;
import com.github.hsindumas.stagger.gradle.task.PostmanTask;
import com.github.hsindumas.stagger.gradle.task.RestHtmlTask;
import com.github.hsindumas.stagger.gradle.task.RestMarkdownTask;
import com.github.hsindumas.stagger.gradle.task.RpcHtmlTask;
import com.github.hsindumas.stagger.gradle.task.RpcMarkdownTask;
import com.github.hsindumas.stagger.gradle.task.SwaggerTask;
import com.github.hsindumas.stagger.gradle.task.WebSocketHtmlTask;
import com.github.hsindumas.stagger.gradle.task.WebSocketMarkdownTask;
import com.github.hsindumas.stagger.gradle.task.WordTask;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yu 2020/11/23.
 * @author HsinDumas
 */
public class TaskConstants {

	/**
	 * Map of Gradle Task
	 */
	public static Map<String, Class<? extends DocBaseTask>> taskMap = new HashMap<>();

	static {
		// create html
		taskMap.put(GlobalConstants.REST_HTML_TASK, RestHtmlTask.class);
		// create markdown
		taskMap.put(GlobalConstants.REST_MARKDOWN_TASK, RestMarkdownTask.class);
		// create jmeter
		taskMap.put(GlobalConstants.JMETER_TASK, JMeterTask.class);
		// create postman collection
		taskMap.put(GlobalConstants.POSTMAN_TASK, PostmanTask.class);
		// create open api
		taskMap.put(GlobalConstants.OPEN_API_TASK, OpenApiTask.class);
		// create rpc html
		taskMap.put(GlobalConstants.RPC_HTML_TASK, RpcHtmlTask.class);
		// create rpc Markdown
		taskMap.put(GlobalConstants.RPC_MARKDOWN_TASK, RpcMarkdownTask.class);
		// create word rest
		taskMap.put(GlobalConstants.WORD_TASK, WordTask.class);
		// create Swagger
		taskMap.put(GlobalConstants.SWAGGER_TASK, SwaggerTask.class);
		// create websocket html
		taskMap.put(GlobalConstants.WEBSOCKET_HTML_TASK, WebSocketHtmlTask.class);
		// create websocket markdown
		taskMap.put(GlobalConstants.WEBSOCKET_MARKDOWN_TASK, WebSocketMarkdownTask.class);
		// create javadoc html
		taskMap.put(GlobalConstants.JAVADOC_HTML_TASK, JavadocHtmlTask.class);
		// create javadoc markdown
		taskMap.put(GlobalConstants.JAVADOC_MARKDOWN_TASK, JavadocMarkdownTask.class);
		// create gRPC html
		taskMap.put(GlobalConstants.GRPC_HTML_TASK, GrpcHtmlTask.class);
		// create gRPC markdown
		taskMap.put(GlobalConstants.GRPC_MARKDOWN_TASK, GrpcMarkdownTask.class);

	}

}
