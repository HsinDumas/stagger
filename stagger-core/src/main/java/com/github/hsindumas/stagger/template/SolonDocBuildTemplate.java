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
package com.github.hsindumas.stagger.template;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.DocAnnotationConstants;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.constants.DocTags;
import com.github.hsindumas.stagger.constants.FrameworkEnum;
import com.github.hsindumas.stagger.constants.Methods;
import com.github.hsindumas.stagger.constants.SolonAnnotations;
import com.github.hsindumas.stagger.constants.SolonRequestAnnotationsEnum;
import com.github.hsindumas.stagger.handler.SolonRequestHeaderHandler;
import com.github.hsindumas.stagger.handler.SolonRequestMappingHandler;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiDoc;
import com.github.hsindumas.stagger.model.ApiExceptionStatus;
import com.github.hsindumas.stagger.model.ApiReqParam;
import com.github.hsindumas.stagger.model.ApiSchema;
import com.github.hsindumas.stagger.model.ExceptionAdviceMethod;
import com.github.hsindumas.stagger.model.WebSocketDoc;
import com.github.hsindumas.stagger.model.annotation.EntryAnnotation;
import com.github.hsindumas.stagger.model.annotation.FrameworkAnnotations;
import com.github.hsindumas.stagger.model.annotation.HeaderAnnotation;
import com.github.hsindumas.stagger.model.annotation.MappingAnnotation;
import com.github.hsindumas.stagger.model.annotation.PathVariableAnnotation;
import com.github.hsindumas.stagger.model.annotation.RequestBodyAnnotation;
import com.github.hsindumas.stagger.model.annotation.RequestParamAnnotation;
import com.github.hsindumas.stagger.model.request.RequestMapping;
import com.github.hsindumas.stagger.utils.DocClassUtil;
import com.github.hsindumas.stagger.utils.DocUtil;
import com.github.hsindumas.stagger.utils.JavaClassValidateUtil;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * solon doc build template.
 *
 * @author noear 2022/2/19 created
 * @author HsinDumas
 */
public class SolonDocBuildTemplate
        implements IDocBuildTemplate<ApiDoc>,
                IWebSocketDocBuildTemplate<WebSocketDoc>,
                IRestDocTemplate,
                IWebSocketTemplate {

    @Override
    public boolean supportsFramework(String framework) {
        return FrameworkEnum.SOLON.getFramework().equalsIgnoreCase(framework);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ApiSchema<ApiDoc> renderApi(ProjectDocConfigBuilder projectBuilder, Collection<?> candidateClasses) {
        ApiConfig apiConfig = projectBuilder.getApiConfig();
        List<ApiReqParam> configApiReqParams = Stream.of(apiConfig.getRequestHeaders(), apiConfig.getRequestParams())
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        FrameworkAnnotations frameworkAnnotations = this.registeredAnnotations();
        return this.processApiData(
                projectBuilder,
                frameworkAnnotations,
                configApiReqParams,
                new SolonRequestMappingHandler(),
                new SolonRequestHeaderHandler(),
                candidateClasses);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<WebSocketDoc> renderWebSocketApi(
            ProjectDocConfigBuilder projectBuilder, Collection<?> candidateClasses) {
        return null;
    }

    @Override
    public boolean ignoreReturnObject(String typeName, List<String> ignoreParams) {
        return JavaClassValidateUtil.isMvcIgnoreParams(typeName, ignoreParams);
    }

    @Override
    public boolean isEntryPoint(Object cls, FrameworkAnnotations frameworkAnnotations) {
        boolean isDefaultEntryPoint = this.defaultEntryPoint(cls, frameworkAnnotations);
        if (isDefaultEntryPoint) {
            return true;
        }

        for (Object annotation : DocUtil.getClassAnnotations(cls)) {
            String name = DocUtil.getAnnotationTypeValue(annotation);
            if (SolonAnnotations.REMOTING.equals(name)) {
                return true;
            }
        }
        // use custom doc tag to support Feign.
        List<?> docletTags = DocUtil.getClassTags(cls);
        for (Object docletTag : docletTags) {
            String value = DocUtil.getDocletTagName(docletTag);
            if (DocTags.REST_API.equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> listMvcRequestAnnotations() {
        return SolonRequestAnnotationsEnum.listMvcRequestAnnotations();
    }

    @Override
    public void requestMappingPostProcess(Object javaClass, Object method, RequestMapping requestMapping) {
        if (Objects.isNull(requestMapping)) {
            return;
        }
        if (DocClassUtil.isAnnotationOrEnum(javaClass)) {
            return;
        }
        boolean isRemote = false;
        for (Object annotation : DocUtil.getClassAnnotations(javaClass)) {
            String name = DocUtil.getAnnotationTypeValue(annotation);
            if (SolonAnnotations.REMOTING.equals(name)) {
                isRemote = true;
            }
        }
        if (isRemote) {
            requestMapping.setMethodType(Methods.POST.getValue());
            String shortUrl = requestMapping.getShortUrl();
            String mediaType = requestMapping.getMediaType();
            if (shortUrl == null) {
                requestMapping.setShortUrl(DocUtil.getMethodName(method));
            }
            if (mediaType == null) {
                requestMapping.setMediaType("text/json");
            }
        }
    }

    @Override
    public boolean ignoreMvcParamWithAnnotation(String annotation) {
        return JavaClassValidateUtil.ignoreSolonMvcParamWithAnnotation(annotation);
    }

    @Override
    public FrameworkAnnotations registeredAnnotations() {
        FrameworkAnnotations annotations = FrameworkAnnotations.builder();
        HeaderAnnotation headerAnnotation = HeaderAnnotation.builder()
                .setAnnotationName(SolonAnnotations.REQUEST_HERDER)
                .setValueProp(DocAnnotationConstants.VALUE_PROP)
                .setDefaultValueProp(DocAnnotationConstants.DEFAULT_VALUE_PROP)
                .setRequiredProp(DocAnnotationConstants.REQUIRED_PROP);
        // add header annotation
        annotations.setHeaderAnnotation(headerAnnotation);

        // add entry annotation
        Map<String, EntryAnnotation> entryAnnotations = new HashMap<>(16);
        EntryAnnotation controllerAnnotation = EntryAnnotation.builder()
                .setAnnotationName(SolonAnnotations.CONTROLLER)
                .setAnnotationFullyName(SolonAnnotations.CONTROLLER);
        entryAnnotations.put(controllerAnnotation.getAnnotationName(), controllerAnnotation);

        EntryAnnotation remoteController = EntryAnnotation.builder().setAnnotationName(SolonAnnotations.REMOTING);
        entryAnnotations.put(remoteController.getAnnotationName(), remoteController);

        EntryAnnotation componentController = EntryAnnotation.builder().setAnnotationName(SolonAnnotations.COMPONENT);
        entryAnnotations.put(componentController.getAnnotationName(), componentController);

        annotations.setEntryAnnotations(entryAnnotations);

        // add request body annotation
        RequestBodyAnnotation bodyAnnotation = RequestBodyAnnotation.builder()
                .setAnnotationName(SolonAnnotations.REQUEST_BODY)
                .setAnnotationFullyName(SolonAnnotations.REQUEST_BODY_FULLY);
        annotations.setRequestBodyAnnotation(bodyAnnotation);

        // request param annotation
        RequestParamAnnotation requestAnnotation = RequestParamAnnotation.builder()
                .setAnnotationName(SolonAnnotations.REQUEST_PARAM)
                .setDefaultValueProp(DocAnnotationConstants.DEFAULT_VALUE_PROP)
                .setRequiredProp(DocAnnotationConstants.REQUIRED_PROP);
        annotations.setRequestParamAnnotation(requestAnnotation);

        // add path variable annotation
        PathVariableAnnotation pathVariableAnnotation = PathVariableAnnotation.builder()
                .setAnnotationName(SolonAnnotations.PATH_VAR)
                .setDefaultValueProp(DocAnnotationConstants.DEFAULT_VALUE_PROP)
                .setRequiredProp(DocAnnotationConstants.REQUIRED_PROP);
        annotations.setPathVariableAnnotation(pathVariableAnnotation);

        // add mapping annotations
        Map<String, MappingAnnotation> mappingAnnotations = new HashMap<>(16);

        MappingAnnotation requestMappingAnnotation = MappingAnnotation.builder()
                .setAnnotationName(SolonAnnotations.REQUEST_MAPPING)
                .setProducesProp("produces")
                .setMethodProp("method")
                .setParamsProp("params")
                .setScope("class", "method")
                .setPathProps(
                        DocAnnotationConstants.VALUE_PROP,
                        DocAnnotationConstants.NAME_PROP,
                        DocAnnotationConstants.PATH_PROP);
        mappingAnnotations.put(requestMappingAnnotation.getAnnotationName(), requestMappingAnnotation);

        MappingAnnotation postMappingAnnotation = MappingAnnotation.builder()
                .setAnnotationName(SolonAnnotations.POST_MAPPING)
                .setProducesProp("produces")
                .setMethodProp("method")
                .setParamsProp("params")
                .setMethodType(Methods.POST.getValue())
                .setPathProps(
                        DocAnnotationConstants.VALUE_PROP,
                        DocAnnotationConstants.NAME_PROP,
                        DocAnnotationConstants.PATH_PROP);
        mappingAnnotations.put(postMappingAnnotation.getAnnotationName(), postMappingAnnotation);

        MappingAnnotation getMappingAnnotation = MappingAnnotation.builder()
                .setAnnotationName(SolonAnnotations.GET_MAPPING)
                .setProducesProp("produces")
                .setMethodProp("method")
                .setParamsProp("params")
                .setMethodType(Methods.GET.getValue())
                .setPathProps(
                        DocAnnotationConstants.VALUE_PROP,
                        DocAnnotationConstants.NAME_PROP,
                        DocAnnotationConstants.PATH_PROP);
        mappingAnnotations.put(getMappingAnnotation.getAnnotationName(), getMappingAnnotation);

        MappingAnnotation putMappingAnnotation = MappingAnnotation.builder()
                .setAnnotationName(SolonAnnotations.PUT_MAPPING)
                .setProducesProp("produces")
                .setParamsProp("params")
                .setMethodProp("method")
                .setMethodType(Methods.PUT.getValue())
                .setPathProps(
                        DocAnnotationConstants.VALUE_PROP,
                        DocAnnotationConstants.NAME_PROP,
                        DocAnnotationConstants.PATH_PROP);
        mappingAnnotations.put(putMappingAnnotation.getAnnotationName(), putMappingAnnotation);

        MappingAnnotation patchMappingAnnotation = MappingAnnotation.builder()
                .setAnnotationName(SolonAnnotations.PATCH_MAPPING)
                .setProducesProp("produces")
                .setMethodProp("method")
                .setParamsProp("params")
                .setMethodType(Methods.PATCH.getValue())
                .setPathProps(
                        DocAnnotationConstants.VALUE_PROP,
                        DocAnnotationConstants.NAME_PROP,
                        DocAnnotationConstants.PATH_PROP);
        mappingAnnotations.put(patchMappingAnnotation.getAnnotationName(), patchMappingAnnotation);

        MappingAnnotation deleteMappingAnnotation = MappingAnnotation.builder()
                .setAnnotationName(SolonAnnotations.DELETE_MAPPING)
                .setProducesProp("produces")
                .setMethodProp("method")
                .setParamsProp("params")
                .setMethodType(Methods.DELETE.getValue())
                .setPathProps(
                        DocAnnotationConstants.VALUE_PROP,
                        DocAnnotationConstants.NAME_PROP,
                        DocAnnotationConstants.PATH_PROP);
        mappingAnnotations.put(deleteMappingAnnotation.getAnnotationName(), deleteMappingAnnotation);

        MappingAnnotation feignClientAnnotation = MappingAnnotation.builder()
                .setAnnotationName(DocGlobalConstants.FEIGN_CLIENT)
                .setAnnotationFullyName(DocGlobalConstants.FEIGN_CLIENT_FULLY);
        mappingAnnotations.put(feignClientAnnotation.getAnnotationName(), feignClientAnnotation);

        annotations.setMappingAnnotations(mappingAnnotations);
        return annotations;
    }

    @Override
    public boolean isExceptionAdviceEntryPoint(Object javaClass, FrameworkAnnotations frameworkAnnotations) {
        return false;
    }

    @Override
    public ExceptionAdviceMethod processExceptionAdviceMethod(Object method) {
        return null;
    }

    @Override
    public List<ApiExceptionStatus> defaultHttpErrorStatuses() {
        return null;
    }
}
