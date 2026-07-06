/*
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
package com.github.hsindumas.stagger.template;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.common.util.StringUtil;
import com.github.hsindumas.stagger.common.util.ValidateUtil;
import com.github.hsindumas.stagger.constants.DocAnnotationConstants;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.constants.DocTags;
import com.github.hsindumas.stagger.constants.JavaTypeConstants;
import com.github.hsindumas.stagger.handler.DefaultWebSocketRequestHandler;
import com.github.hsindumas.stagger.handler.IWebSocketRequestHandler;
import com.github.hsindumas.stagger.helper.ParamsBuildHelper;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiParam;
import com.github.hsindumas.stagger.model.WebSocketDoc;
import com.github.hsindumas.stagger.model.annotation.FrameworkAnnotations;
import com.github.hsindumas.stagger.model.annotation.ServerEndpointAnnotation;
import com.github.hsindumas.stagger.model.request.ServerEndpoint;
import com.github.hsindumas.stagger.utils.DocClassUtil;
import com.github.hsindumas.stagger.utils.DocUtil;
import com.github.hsindumas.stagger.utils.JavaClassUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * the WebSocket Template
 *
 * @author linwumingshi
 * @author HsinDumas
 * @since 3.0.3
 */
public interface IWebSocketTemplate {

    /**
     * param order AtomicInteger
     */
    AtomicInteger ATOMIC_INTEGER = new AtomicInteger(1);

    /**
     * logger
     */
    Logger log = Logger.getLogger(IWebSocketTemplate.class.getName());

    /**
     * Processes the provided Java classes and generates WebSocket documentation.
     * @param projectBuilder The project configuration builder.
     * @param frameworkAnnotations The framework annotations to look for.
     * @param webSocketRequestHandler The handler for processing WebSocket requests.
     * @param candidateClasses The collection of Java classes to process.
     * @return A list of WebSocketDoc containing the documentation for the WebSocket
     * endpoints.
     */
    default List<WebSocketDoc> processWebSocketData(
            ProjectDocConfigBuilder projectBuilder,
            FrameworkAnnotations frameworkAnnotations,
            IWebSocketRequestHandler webSocketRequestHandler,
            Collection<?> candidateClasses) {
        ApiConfig apiConfig = projectBuilder.getApiConfig();
        List<WebSocketDoc> apiDocList = new ArrayList<>();
        int maxOrder = 0;
        boolean setCustomOrder = false;
        // exclude class is ignore
        for (Object javaClass : candidateClasses) {
            if (StringUtil.isNotEmpty(apiConfig.getPackageFilters())) {
                // from smart config
                if (!DocUtil.isMatch(apiConfig.getPackageFilters(), javaClass)) {
                    continue;
                }
            }
            // exclude class is ignore
            if (StringUtil.isNotEmpty(apiConfig.getPackageExcludeFilters())) {
                if (DocUtil.isMatch(apiConfig.getPackageExcludeFilters(), javaClass)) {
                    continue;
                }
            }
            // ignore tag is ignore
            if (Objects.nonNull(DocUtil.getClassTagByName(javaClass, DocTags.IGNORE))) {
                continue;
            }
            // if the class is websocket
            Optional<Object> optionalAnnotation = this.getOptionalWebSocketAnnotation(javaClass, frameworkAnnotations);
            if (!optionalAnnotation.isPresent()) {
                continue;
            }
            String strOrder = JavaClassUtil.getClassTagsValue(javaClass, DocTags.ORDER, Boolean.TRUE);
            int order = 0;
            if (ValidateUtil.isNonNegativeInteger(strOrder)) {
                setCustomOrder = true;
                order = Integer.parseInt(strOrder);
                maxOrder = Math.max(maxOrder, order);
            }
            WebSocketDoc webSocketDoc = this.buildEntryPointWebSocketDoc(
                    javaClass, projectBuilder, webSocketRequestHandler, order, optionalAnnotation.get());
            if (Objects.nonNull(webSocketDoc)) {
                apiDocList.add(webSocketDoc);
            }
        }
        if (apiConfig.isSortByTitle()) {
            // sort by title
            Collections.sort(apiDocList);
        } else if (setCustomOrder) {
            ATOMIC_INTEGER.getAndAdd(maxOrder);
            // while set custom oder
            final List<WebSocketDoc> tempList = new ArrayList<>(apiDocList);
            tempList.forEach(p -> {
                if (p.getOrder() == 0) {
                    p.setOrder(ATOMIC_INTEGER.getAndAdd(1));
                }
            });
            return tempList.stream()
                    .sorted(Comparator.comparing(WebSocketDoc::getOrder))
                    .collect(Collectors.toList());
        } else {
            apiDocList.forEach(p -> p.setOrder(ATOMIC_INTEGER.getAndAdd(1)));
        }
        return apiDocList;
    }

    /**
     * build websocket doc
     * @param javaClass JavaClass
     * @param projectBuilder ProjectDocConfigBuilder
     * @param webSocketRequestHandler WebSocketRequestHandler
     * @param order order
     * @param serverEndpointAnnotation ServerEndpointAnnotation
     * @return WebSocketDoc
     */
    default WebSocketDoc buildEntryPointWebSocketDoc(
            final Object javaClass,
            ProjectDocConfigBuilder projectBuilder,
            IWebSocketRequestHandler webSocketRequestHandler,
            int order,
            Object serverEndpointAnnotation) {

        ApiConfig apiConfig = projectBuilder.getApiConfig();

        webSocketRequestHandler = Objects.isNull(webSocketRequestHandler)
                ? DefaultWebSocketRequestHandler.getInstance()
                : webSocketRequestHandler;
        ServerEndpoint serverEndpoint =
                webSocketRequestHandler.handleServerEndpoint(projectBuilder, javaClass, serverEndpointAnnotation);

        WebSocketDoc webSocketDoc = new WebSocketDoc();
        // if it does not have subProtocols
        if (!serverEndpoint.getSubProtocols().isEmpty()) {
            webSocketDoc.setSubProtocols(String.join(",", serverEndpoint.getSubProtocols()));
        }
        // populate websocket params
        this.populateWebSocketParams(projectBuilder, webSocketDoc, javaClass, serverEndpoint);

        // build websocket doc
        webSocketDoc.setName(DocUtil.getClassSimpleName(javaClass));
        webSocketDoc.setUri(replaceHttpPrefixToWebSocketPrefix(apiConfig.getServerUrl()) + serverEndpoint.getUrl());
        webSocketDoc.setPackageName(DocUtil.getClassPackageName(javaClass));
        webSocketDoc.setDesc(DocUtil.getEscapeAndCleanComment(DocUtil.getClassComment(javaClass)));
        webSocketDoc.setAuthor(JavaClassUtil.getClassTagsValue(javaClass, DocTags.AUTHOR, Boolean.TRUE));
        webSocketDoc.setOrder(order);
        boolean isDeprecated = Objects.nonNull(DocUtil.getClassTagByName(javaClass, DocTags.DEPRECATED))
                || DocUtil.getClassAnnotations(javaClass).stream()
                        .map(DocUtil::getAnnotationTypeFullyQualifiedName)
                        .anyMatch(JavaTypeConstants.JAVA_DEPRECATED_FULLY::equals);
        webSocketDoc.setDeprecated(isDeprecated);
        return webSocketDoc;
    }

    /**
     * Get WebSocket annotations from a JavaClass based on FrameworkAnnotations.
     * @param javaClass The JavaClass to retrieve annotations from.
     * @param frameworkAnnotations The FrameworkAnnotations containing specific framework
     * annotation information.
     * @return An Optional annotation object containing the WebSocket annotation, or
     * Optional.empty() if not found.
     */
    default Optional<Object> getOptionalWebSocketAnnotation(
            Object javaClass, FrameworkAnnotations frameworkAnnotations) {
        // Check for null inputs
        if (Objects.isNull(frameworkAnnotations)
                || Objects.isNull(javaClass)
                || Objects.isNull(frameworkAnnotations.getServerEndpointAnnotation())) {
            return Optional.empty();
        }

        ServerEndpointAnnotation serverEndpointAnnotation = frameworkAnnotations.getServerEndpointAnnotation();
        String annotationName = serverEndpointAnnotation.getAnnotationName();

        // Filter and find the WebSocket annotation
        return DocUtil.getClassAnnotations(javaClass).stream()
                .filter(annotation -> Objects.equals(annotationName, DocUtil.getAnnotationTypeSimpleName(annotation))
                        || Objects.equals(annotationName, DocUtil.getAnnotationTypeValue(annotation))
                        || Objects.equals(annotationName, DocUtil.getAnnotationTypeFullyQualifiedName(annotation)))
                .findFirst()
                .map(annotation -> (Object) annotation);
    }

    /**
     * Populates the WebSocketDoc object with both path and message parameters from the
     * provided JavaClass and ServerEndpoint.
     * @param projectBuilder The project configuration builder.
     * @param webSocketDoc The WebSocketDoc object to be populated with parameters.
     * @param javaClass The JavaClass containing the WebSocket endpoint.
     * @param serverEndpoint The ServerEndpoint containing the URL and parameters.
     */
    default void populateWebSocketParams(
            ProjectDocConfigBuilder projectBuilder,
            WebSocketDoc webSocketDoc,
            final Object javaClass,
            ServerEndpoint serverEndpoint) {
        List<ApiParam> pathParams = new ArrayList<>();
        String url = serverEndpoint.getUrl();
        Set<String> pathParamsSet = extractPathParams(url);

        Map<String, Object> parameterMap = new HashMap<>(16);

        Map<String, String> commentsByTag = new HashMap<>(16);
        // @OnMessage Method flag
        boolean onMessageMethod = false;

        for (Object javaMethod : DocUtil.getClassMethods(javaClass)) {
            List<?> annotations = DocUtil.getMethodAnnotations(javaMethod);
            List<?> parameters = DocUtil.getMethodParameters(javaMethod);
            commentsByTag = DocUtil.getCommentsByTag(javaMethod, DocTags.PARAM, DocUtil.getClassSimpleName(javaClass));

            // if the method does not have @OnOpen
            boolean hasOnOpenAnnotation = annotations.stream()
                    .anyMatch(annotation -> DocAnnotationConstants.ON_OPEN.equals(
                                    DocUtil.getAnnotationTypeSimpleName(annotation))
                            || DocAnnotationConstants.ON_OPEN.equals(DocUtil.getAnnotationTypeValue(annotation)));
            if (hasOnOpenAnnotation) {
                // Collect parameters annotated with @PathParam
                for (Object parameter : parameters) {
                    for (Object annotation : DocUtil.getParameterAnnotations(parameter)) {
                        if (DocAnnotationConstants.PATH_PARAM.equals(DocUtil.getAnnotationTypeSimpleName(annotation))
                                || DocAnnotationConstants.PATH_PARAM.equals(
                                        DocUtil.getAnnotationTypeValue(annotation))) {
                            parameterMap.put(DocUtil.getParameterName(parameter), parameter);
                        }
                    }
                }
                continue;
            }
            // if the method does not have @OnMessage
            boolean hasOnMessageAnnotation = annotations.stream()
                    .anyMatch(annotation -> DocAnnotationConstants.ON_MESSAGE.equals(
                                    DocUtil.getAnnotationTypeSimpleName(annotation))
                            || DocAnnotationConstants.ON_MESSAGE.equals(DocUtil.getAnnotationTypeValue(annotation)));
            if (hasOnMessageAnnotation) {
                if (onMessageMethod) {
                    log.warning("@OnMessage can only on one method");
                } else {
                    onMessageMethod = true;
                    if (!parameters.isEmpty() && parameters.size() > 1) {
                        log.warning("@OnMessage method can only have one parameter");
                    }
                    Object first = parameters.get(0);
                    List<ApiParam> apiParams = ParamsBuildHelper.buildParams(
                            DocUtil.getParameterFullyQualifiedName(first),
                            "",
                            0,
                            "false",
                            false,
                            new HashMap<>(16),
                            projectBuilder,
                            new HashSet<>(16),
                            new HashSet<>(16),
                            0,
                            false,
                            new AtomicInteger(0));
                    webSocketDoc.setMessageParams(apiParams);
                }
            }
        }

        // Reorder path parameters according to the order in pathParamsSet
        for (String item : pathParamsSet) {
            ApiParam pathApiParam = ApiParam.of()
                    .setId(0)
                    .setField(item)
                    .setType("string")
                    .setDesc(item)
                    .setVersion(commentsByTag.getOrDefault(DocTags.SINCE, DocGlobalConstants.DEFAULT_VERSION))
                    .setRequired(true);
            Object javaParameter = parameterMap.get(item);
            if (Objects.nonNull(javaParameter)) {
                pathApiParam
                        .setType(DocClassUtil.processTypeNameForParams(
                                DocUtil.getParameterGenericFullyQualifiedName(javaParameter)))
                        .setDesc(commentsByTag.get(DocUtil.getParameterName(javaParameter)));
            }
            pathParams.add(pathApiParam);
        }
        webSocketDoc.setPathParams(pathParams);

        this.setResponseParam(projectBuilder, webSocketDoc, serverEndpoint);
    }

    /**
     * Sets the response parameters for the WebSocketDoc object based on the provided
     * ServerEndpoint.
     * @param projectBuilder The project configuration builder.
     * @param webSocketDoc The WebSocketDoc object to be populated with response
     * parameters.
     * @param serverEndpoint The ServerEndpoint containing the URL and parameters.
     */
    default void setResponseParam(
            ProjectDocConfigBuilder projectBuilder, WebSocketDoc webSocketDoc, ServerEndpoint serverEndpoint) {
        if (serverEndpoint.getEncoders().isEmpty()) {
            return;
        }
        List<List<ApiParam>> result = new ArrayList<>();
        for (String encoder : serverEndpoint.getEncoders()) {
            Optional<String> responseType = projectBuilder.getImplementedInterfaceTypeArgument(
                    encoder, 0, "jakarta.websocket.Encoder$Text", "javax.websocket.Encoder$Text");
            if (responseType.isPresent()) {
                List<ApiParam> apiParams = ParamsBuildHelper.buildParams(
                        responseType.get(),
                        "",
                        0,
                        "false",
                        true,
                        new HashMap<>(16),
                        projectBuilder,
                        new HashSet<>(16),
                        new HashSet<>(16),
                        0,
                        false,
                        new AtomicInteger(0));
                result.add(apiParams);
            }
        }
        webSocketDoc.setResponseParams(result);
    }

    /**
     * Replaces the HTTP prefix with a WebSocket prefix in a given URL.
     * @param url The URL to modify.
     * @return The modified URL with a WebSocket prefix.
     */
    static String replaceHttpPrefixToWebSocketPrefix(String url) {
        return url.replace("http", "ws");
    }

    /**
     * Extracts the path parameters from a URL.
     * @param url The URL containing path parameters.
     * @return A set of extracted path parameter names.
     */
    static Set<String> extractPathParams(String url) {
        Set<String> pathParams = new LinkedHashSet<>();
        String regex = "\\{(\\w+)}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        while (matcher.find()) {
            pathParams.add(matcher.group(1));
        }

        return pathParams;
    }
}
