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
package com.github.hsindumas.stagger.handler;

import static com.github.hsindumas.stagger.constants.DocTags.DEPRECATED;
import static com.github.hsindumas.stagger.constants.DocTags.IGNORE;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.DocAnnotationConstants;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.constants.Methods;
import com.github.hsindumas.stagger.constants.SolonAnnotations;
import com.github.hsindumas.stagger.function.RequestMappingFunc;
import com.github.hsindumas.stagger.model.annotation.FrameworkAnnotations;
import com.github.hsindumas.stagger.model.request.RequestMapping;
import com.github.hsindumas.stagger.utils.DocUtil;
import java.util.List;
import java.util.Objects;

/**
 * Solon RequestMapping Handler
 *
 * @author noear 2022/2/19 created
 * @author HsinDumas
 */
public class SolonRequestMappingHandler implements IRequestMappingHandler, IWebSocketRequestHandler {

    /**
     * handle solon request mapping
     * @param projectBuilder projectBuilder
     * @param controllerBaseUrl solon mvc controller base url
     * @param method JavaMethod
     * @return RequestMapping
     */
    @Override
    public RequestMapping handle(
            ProjectDocConfigBuilder projectBuilder,
            String controllerBaseUrl,
            Object method,
            FrameworkAnnotations frameworkAnnotations,
            RequestMappingFunc requestMappingFunc) {
        if (Objects.nonNull(DocUtil.getMethodTagByName(method, IGNORE))) {
            return null;
        }
        List<?> annotations = getAnnotations(method);
        String methodType = "GET"; // default is get
        String shortUrl = null;
        String mediaType = null;
        boolean deprecated = false;
        for (Object annotation : annotations) {
            String annotationName = DocUtil.getAnnotationTypeValue(annotation);
            String annotationFullyName = DocUtil.getAnnotationTypeFullyQualifiedName(annotation);
            if (DocAnnotationConstants.DEPRECATED.equals(annotationName)) {
                deprecated = true;
            }
            if (SolonAnnotations.REQUEST_MAPPING.equals(annotationName)
                    || SolonAnnotations.REQUEST_MAPPING_FULLY.equals(annotationName)
                    || SolonAnnotations.REQUEST_MAPPING_FULLY.equals(annotationFullyName)) {
                ClassLoader classLoader = projectBuilder.getApiConfig().getClassLoader();
                shortUrl = DocUtil.handleMappingValue(classLoader, annotation);
                // There is no need to add '/' to the end
                shortUrl = shortUrl.equals(DocGlobalConstants.PATH_DELIMITER) ? "" : shortUrl;
                Object produces = DocUtil.getAnnotationNamedParameter(annotation, "produces");
                if (Objects.nonNull(produces)) {
                    mediaType = produces.toString();
                }
            }
            if (SolonAnnotations.GET_MAPPING.equals(annotationName)
                    || SolonAnnotations.GET_MAPPING_FULLY.equals(annotationName)
                    || SolonAnnotations.GET_MAPPING_FULLY.equals(annotationFullyName)) {
                methodType = Methods.GET.getValue();
            } else if (SolonAnnotations.POST_MAPPING.equals(annotationName)
                    || SolonAnnotations.POST_MAPPING_FULLY.equals(annotationName)
                    || SolonAnnotations.POST_MAPPING_FULLY.equals(annotationFullyName)) {
                methodType = Methods.POST.getValue();
            } else if (SolonAnnotations.PUT_MAPPING.equals(annotationName)
                    || SolonAnnotations.PUT_MAPPING_FULLY.equals(annotationName)
                    || SolonAnnotations.PUT_MAPPING_FULLY.equals(annotationFullyName)) {
                methodType = Methods.PUT.getValue();
            } else if (SolonAnnotations.PATCH_MAPPING.equals(annotationName)
                    || SolonAnnotations.PATCH_MAPPING_FULLY.equals(annotationName)
                    || SolonAnnotations.PATCH_MAPPING_FULLY.equals(annotationFullyName)) {
                methodType = Methods.PATCH.getValue();
            } else if (SolonAnnotations.DELETE_MAPPING.equals(annotationName)
                    || SolonAnnotations.DELETE_MAPPING_FULLY.equals(annotationName)
                    || SolonAnnotations.DELETE_MAPPING_FULLY.equals(annotationFullyName)) {
                methodType = Methods.DELETE.getValue();
            }
        }
        if (Objects.nonNull(DocUtil.getMethodTagByName(method, DEPRECATED))) {
            deprecated = true;
        }
        RequestMapping requestMapping = RequestMapping.builder()
                .setMediaType(mediaType)
                .setMethodType(methodType)
                .setDeprecated(deprecated)
                .setShortUrl(shortUrl);
        requestMapping = formatMappingData(projectBuilder, controllerBaseUrl, requestMapping);
        requestMappingFunc.process(DocUtil.getMethodDeclaringClassCanonicalName(method), requestMapping);
        return requestMapping;
    }
}
