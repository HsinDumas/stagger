/*
 * Copyright (C) 2018-2025 stagger
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
import com.github.hsindumas.stagger.common.util.DateTimeUtil;
import com.github.hsindumas.stagger.common.util.StringUtil;
import com.github.hsindumas.stagger.constants.DocAnnotationConstants;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.constants.DocTags;
import com.github.hsindumas.stagger.constants.FrameworkEnum;
import com.github.hsindumas.stagger.constants.Methods;
import com.github.hsindumas.stagger.constants.SpringMvcAnnotations;
import com.github.hsindumas.stagger.constants.SpringMvcRequestAnnotationsEnum;
import com.github.hsindumas.stagger.handler.SpringMVCRequestHeaderHandler;
import com.github.hsindumas.stagger.handler.SpringMVCRequestMappingHandler;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiDoc;
import com.github.hsindumas.stagger.model.ApiExceptionStatus;
import com.github.hsindumas.stagger.model.ApiParam;
import com.github.hsindumas.stagger.model.ApiReqParam;
import com.github.hsindumas.stagger.model.ApiSchema;
import com.github.hsindumas.stagger.model.ExceptionAdviceMethod;
import com.github.hsindumas.stagger.model.WebSocketDoc;
import com.github.hsindumas.stagger.model.annotation.EntryAnnotation;
import com.github.hsindumas.stagger.model.annotation.ExceptionAdviceAnnotation;
import com.github.hsindumas.stagger.model.annotation.FrameworkAnnotations;
import com.github.hsindumas.stagger.model.annotation.HeaderAnnotation;
import com.github.hsindumas.stagger.model.annotation.MappingAnnotation;
import com.github.hsindumas.stagger.model.annotation.PathVariableAnnotation;
import com.github.hsindumas.stagger.model.annotation.RequestBodyAnnotation;
import com.github.hsindumas.stagger.model.annotation.RequestParamAnnotation;
import com.github.hsindumas.stagger.model.annotation.RequestPartAnnotation;
import com.github.hsindumas.stagger.model.annotation.ServerEndpointAnnotation;
import com.github.hsindumas.stagger.model.request.RequestMapping;
import com.github.hsindumas.stagger.utils.DocClassUtil;
import com.github.hsindumas.stagger.utils.DocUtil;
import com.github.hsindumas.stagger.utils.JavaClassUtil;
import com.github.hsindumas.stagger.utils.JavaClassValidateUtil;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * spring boot doc build template.
 *
 * @author yu 2019/12/21.
 * @author HsinDumas
 */
public class SpringBootDocBuildTemplate
        implements IDocBuildTemplate<ApiDoc>,
                IWebSocketDocBuildTemplate<WebSocketDoc>,
                IRestDocTemplate,
                IWebSocketTemplate {

    @Override
    public boolean supportsFramework(String framework) {
        return FrameworkEnum.SPRING.getFramework().equalsIgnoreCase(framework);
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
                new SpringMVCRequestMappingHandler(),
                new SpringMVCRequestHeaderHandler(),
                candidateClasses);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<WebSocketDoc> renderWebSocketApi(
            ProjectDocConfigBuilder projectBuilder, Collection<?> candidateClasses) {
        FrameworkAnnotations frameworkAnnotations = this.registeredAnnotations();
        return this.processWebSocketData(
                projectBuilder, frameworkAnnotations, new SpringMVCRequestMappingHandler(), candidateClasses);
    }

    @Override
    public boolean ignoreReturnObject(String typeName, List<String> ignoreParams) {
        return JavaClassValidateUtil.isMvcIgnoreParams(typeName, ignoreParams);
    }

    @Override
    public FrameworkAnnotations registeredAnnotations() {
        FrameworkAnnotations annotations = FrameworkAnnotations.builder();

        // Header annotation
        HeaderAnnotation headerAnnotation = HeaderAnnotation.builder()
                .setAnnotationName(SpringMvcAnnotations.REQUEST_HEADER)
                .setValueProp(DocAnnotationConstants.VALUE_PROP)
                .setDefaultValueProp(DocAnnotationConstants.DEFAULT_VALUE_PROP)
                .setRequiredProp(DocAnnotationConstants.REQUIRED_PROP);
        // add header annotation
        annotations.setHeaderAnnotation(headerAnnotation);

        // Entry annotations (Controller, RestController)
        Map<String, EntryAnnotation> entryAnnotations = new HashMap<>(16);
        EntryAnnotation controllerAnnotation = EntryAnnotation.builder()
                .setAnnotationName(SpringMvcAnnotations.CONTROLLER)
                .setAnnotationFullyName(SpringMvcAnnotations.CONTROLLER_FULLY);
        entryAnnotations.put(controllerAnnotation.getAnnotationName(), controllerAnnotation);
        entryAnnotations.put(controllerAnnotation.getAnnotationFullyName(), controllerAnnotation);

        EntryAnnotation restController = EntryAnnotation.builder()
                .setAnnotationName(SpringMvcAnnotations.REST_CONTROLLER)
                .setAnnotationFullyName(SpringMvcAnnotations.REST_CONTROLLER_FULLY);
        entryAnnotations.put(restController.getAnnotationName(), restController);
        entryAnnotations.put(restController.getAnnotationFullyName(), restController);
        annotations.setEntryAnnotations(entryAnnotations);

        // RequestBody annotation
        RequestBodyAnnotation bodyAnnotation = RequestBodyAnnotation.builder()
                .setAnnotationName(SpringMvcAnnotations.REQUEST_BODY)
                .setAnnotationFullyName(SpringMvcAnnotations.REQUEST_BODY_FULLY);
        annotations.setRequestBodyAnnotation(bodyAnnotation);

        // RequestParam annotation
        RequestParamAnnotation requestParamAnnotation = RequestParamAnnotation.builder()
                .setAnnotationName(SpringMvcAnnotations.REQUEST_PARAM)
                .setAnnotationFullyName(SpringMvcAnnotations.REQUEST_PARAM_FULLY)
                .setDefaultValueProp(DocAnnotationConstants.DEFAULT_VALUE_PROP)
                .setRequiredProp(DocAnnotationConstants.REQUIRED_PROP);
        annotations.setRequestParamAnnotation(requestParamAnnotation);

        // RequestPart annotation
        RequestPartAnnotation requestPartAnnotation = RequestPartAnnotation.builder()
                .setAnnotationName(SpringMvcAnnotations.REQUEST_PART)
                .setAnnotationFullyName(SpringMvcAnnotations.REQUEST_PART_FULLY)
                .setDefaultValueProp(DocAnnotationConstants.DEFAULT_VALUE_PROP)
                .setRequiredProp(DocAnnotationConstants.REQUIRED_PROP);
        annotations.setRequestPartAnnotation(requestPartAnnotation);

        // PathVariable annotation
        PathVariableAnnotation pathVariableAnnotation = PathVariableAnnotation.builder()
                .setAnnotationName(SpringMvcAnnotations.PATH_VARIABLE)
                .setAnnotationFullyName(SpringMvcAnnotations.PATH_VARIABLE_FULLY)
                .setDefaultValueProp(DocAnnotationConstants.DEFAULT_VALUE_PROP)
                .setRequiredProp(DocAnnotationConstants.REQUIRED_PROP);
        annotations.setPathVariableAnnotation(pathVariableAnnotation);

        // ServerEndpoint annotation (WebSocket)
        ServerEndpointAnnotation serverEndpointAnnotation =
                ServerEndpointAnnotation.builder().setAnnotationName(SpringMvcAnnotations.SERVER_ENDPOINT);
        annotations.setServerEndpointAnnotation(serverEndpointAnnotation);

        // add mapping annotations
        Map<String, MappingAnnotation> mappingAnnotations = this.buildSpringMappingAnnotations();
        annotations.setMappingAnnotations(mappingAnnotations);

        // Exception advice annotations
        Map<String, ExceptionAdviceAnnotation> exceptionAdviceAnnotations = new HashMap<>(16);

        ExceptionAdviceAnnotation controllerAdviceAnnotation =
                ExceptionAdviceAnnotation.builder().setAnnotationName(SpringMvcAnnotations.CONTROLLER_ADVICE);
        exceptionAdviceAnnotations.put(controllerAdviceAnnotation.getAnnotationName(), controllerAdviceAnnotation);
        exceptionAdviceAnnotations.put(
                "org.springframework.web.bind.annotation.ControllerAdvice", controllerAdviceAnnotation);

        ExceptionAdviceAnnotation restControllerAdviceAnnotation =
                ExceptionAdviceAnnotation.builder().setAnnotationName(SpringMvcAnnotations.REST_CONTROLLER_ADVICE);
        exceptionAdviceAnnotations.put(
                restControllerAdviceAnnotation.getAnnotationName(), restControllerAdviceAnnotation);
        exceptionAdviceAnnotations.put(
                "org.springframework.web.bind.annotation.RestControllerAdvice", restControllerAdviceAnnotation);

        annotations.setExceptionAdviceAnnotations(exceptionAdviceAnnotations);

        return annotations;
    }

    @Override
    public boolean isEntryPoint(Object javaClass, FrameworkAnnotations frameworkAnnotations) {
        boolean isDefaultEntryPoint = this.defaultEntryPoint(javaClass, frameworkAnnotations);
        if (isDefaultEntryPoint) {
            return true;
        }

        if (DocClassUtil.isAnnotationOrEnum(javaClass)) {
            return false;
        }
        // use custom doc tag to support Feign.
        List<?> docletTags = DocUtil.getClassTags(javaClass);
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
        return SpringMvcRequestAnnotationsEnum.listSpringMvcRequestAnnotations();
    }

    @Override
    public void requestMappingPostProcess(Object javaClass, Object method, RequestMapping requestMapping) {
        // do nothing
    }

    @Override
    public boolean ignoreMvcParamWithAnnotation(String annotation) {
        return JavaClassValidateUtil.ignoreSpringMvcParamWithAnnotation(annotation);
    }

    @Override
    public boolean isExceptionAdviceEntryPoint(Object javaClass, FrameworkAnnotations frameworkAnnotations) {
        return this.defaultExceptionAdviceEntryPoint(javaClass, frameworkAnnotations);
    }

    @Override
    public ExceptionAdviceMethod processExceptionAdviceMethod(Object method) {
        if (Objects.isNull(method)) {
            return ExceptionAdviceMethod.builder()
                    .setExceptionHandlerMethod(false)
                    .setStatus(null);
        }
        List<?> annotations = DocUtil.getMethodAnnotations(method);
        boolean isExceptionHandlerMethod = false;
        String status = null;
        for (Object annotation : annotations) {
            String annotationName = JavaClassUtil.getClassSimpleName(DocUtil.getAnnotationTypeValue(annotation));
            if (SpringMvcAnnotations.EXCEPTION_HANDLER.equals(annotationName)) {
                isExceptionHandlerMethod = true;
            }
            if (SpringMvcAnnotations.RESPONSE_STATUS.equals(annotationName)) {
                Object consumes = DocUtil.getAnnotationNamedParameter(annotation, DocAnnotationConstants.VALUE_PROP);
                if (Objects.nonNull(consumes)) {
                    status = consumes.toString();
                }
            }
        }
        return ExceptionAdviceMethod.builder()
                .setExceptionHandlerMethod(isExceptionHandlerMethod)
                .setStatus(status);
    }

    @Override
    public List<ApiExceptionStatus> defaultHttpErrorStatuses() {
        ZonedDateTime now = ZonedDateTime.now();
        String strDateTime = DateTimeUtil.zonedDateTimeToStr(now, DateTimeUtil.DATE_FORMAT_ZONED_DATE_TIME);

        ApiParam errorParam = ApiParam.of()
                .setClassName("HttpErrorStatusResponse")
                .setField("error")
                .setType("string")
                .setValue("error")
                .setDesc("error message");
        ApiParam pathParam = ApiParam.of()
                .setClassName("HttpErrorStatusResponse")
                .setField("path")
                .setType("string")
                .setValue("")
                .setDesc("request path");

        ApiParam timestampParam = ApiParam.of()
                .setClassName("HttpErrorStatusResponse")
                .setField("timestamp")
                .setType("string")
                .setValue("")
                .setDesc("timestamp");

        ApiParam status500Param = ApiParam.of()
                .setClassName("HttpErrorStatusResponse")
                .setField("status")
                .setType("int")
                .setValue("500")
                .setDesc("Internal Server Error")
                .setRequired(true);
        ApiParam status400Param = ApiParam.of()
                .setClassName("HttpErrorStatusResponse")
                .setField("status")
                .setType("int")
                .setValue("400")
                .setDesc("Bad Request")
                .setRequired(true);

        ApiParam status404Param = ApiParam.of()
                .setClassName("HttpErrorStatusResponse")
                .setField("status")
                .setType("int")
                .setValue("404")
                .setDesc("Not Found")
                .setRequired(true);
        ApiParam status401Param = ApiParam.of()
                .setClassName("HttpErrorStatusResponse")
                .setField("status")
                .setType("int")
                .setValue("401")
                .setDesc("Unauthorized")
                .setRequired(true);
        ApiParam status403Param = ApiParam.of()
                .setClassName("HttpErrorStatusResponse")
                .setField("status")
                .setType("int")
                .setValue("403")
                .setDesc("Forbidden")
                .setRequired(true);
        ApiParam status405Param = ApiParam.of()
                .setClassName("HttpErrorStatusResponse")
                .setField("status")
                .setType("int")
                .setValue("405")
                .setDesc("Method Not Allowed")
                .setRequired(true);
        ApiParam status415Param = ApiParam.of()
                .setClassName("HttpErrorStatusResponse")
                .setField("status")
                .setType("int")
                .setValue("415")
                .setDesc("Unsupported Media Type")
                .setRequired(true);

        List<ApiExceptionStatus> exceptionStatusList = new ArrayList<>();
        exceptionStatusList.add(ApiExceptionStatus.of()
                .setStatus("500")
                .setDesc("Internal Server Error")
                .setResponseUsage("{\n" + "  \"timestamp\": \"" + strDateTime + "\",\n" + "  \"status\": 500,\n"
                        + "  \"error\": \"Internal Server Error\",\n" + "  \"path\": \"/api/v1/xx\"\n" + "}")
                .setExceptionResponseParams(Arrays.asList(errorParam, pathParam, timestampParam, status500Param)));
        exceptionStatusList.add(ApiExceptionStatus.of()
                .setStatus("400")
                .setDesc("Bad Request")
                .setResponseUsage("{\n" + "  \"timestamp\": \"" + strDateTime + "\",\n" + "  \"status\": 400,\n"
                        + "  \"error\": \"Bad Request\",\n" + "  \"path\": \"/api/v1/xx\"\n" + "}")
                .setExceptionResponseParams(Arrays.asList(errorParam, pathParam, timestampParam, status400Param)));

        exceptionStatusList.add(ApiExceptionStatus.of()
                .setStatus("404")
                .setDesc("Not Found")
                .setResponseUsage("{\n" + "  \"timestamp\": \"" + strDateTime + "\",\n" + "  \"status\": 404,\n"
                        + "  \"error\": \"Not Found\",\n" + "  \"path\": \"/api/v1/xx\"\n" + "}")
                .setExceptionResponseParams(Arrays.asList(errorParam, pathParam, timestampParam, status404Param)));

        exceptionStatusList.add(ApiExceptionStatus.of()
                .setStatus("401")
                .setDesc("Unauthorized")
                .setResponseUsage("{\n" + "  \"timestamp\": \"" + strDateTime + "\",\n" + "  \"status\": 401,\n"
                        + "  \"error\": \"Unauthorized\",\n" + "  \"path\": \"/api/v1/xx\"\n" + "} ")
                .setExceptionResponseParams(Arrays.asList(errorParam, pathParam, timestampParam, status401Param)));
        exceptionStatusList.add(ApiExceptionStatus.of()
                .setStatus("403")
                .setDesc("Forbidden")
                .setResponseUsage("{\n" + "  \"timestamp\": \"" + strDateTime + "\",\n" + "  \"status\": 403,\n"
                        + "  \"error\": \"Forbidden\",\n" + "  \"path\": \"/api/v1/xx\"\n" + "} ")
                .setExceptionResponseParams(Arrays.asList(errorParam, pathParam, timestampParam, status403Param)));

        exceptionStatusList.add(ApiExceptionStatus.of()
                .setStatus("405")
                .setDesc("Method Not Allowed")
                .setResponseUsage("{\n" + "  \"timestamp\": \"" + strDateTime + "\",\n" + "  \"status\": 405,\n"
                        + "  \"error\": \"Method Not Allowed\",\n" + "  \"path\": \"/api/v1/xx\"\n" + "} ")
                .setExceptionResponseParams(Arrays.asList(errorParam, pathParam, timestampParam, status405Param)));
        exceptionStatusList.add(ApiExceptionStatus.of()
                .setStatus("415")
                .setDesc("Unsupported Media Type")
                .setResponseUsage("{\n" + "  \"timestamp\": \"" + strDateTime + "\",\n" + "  \"status\": 415,\n"
                        + "  \"error\": \"Unsupported Media Type\",\n" + "  \"path\": \"/api/v1/xx\"\n" + "} ")
                .setExceptionResponseParams(Arrays.asList(errorParam, pathParam, timestampParam, status415Param)));
        return exceptionStatusList;
    }

    /**
     * Builds and returns all Spring MVC request mapping annotations
     * including @RequestMapping, @GetMapping, @PostMapping, etc., with consistent
     * attribute configurations.
     * @return a map of annotation name to {@link MappingAnnotation}
     */
    private Map<String, MappingAnnotation> buildSpringMappingAnnotations() {
        Map<String, MappingAnnotation> mappingAnnotations = new HashMap<>(24);

        // Common properties
        String consumes = DocAnnotationConstants.CONSUMES;
        String produces = DocAnnotationConstants.PRODUCES;
        String method = DocAnnotationConstants.METHOD;
        String params = DocAnnotationConstants.PARAMS;
        String[] pathProps = DocAnnotationConstants.PATH_MAPPING_PROPS;
        String[] exchangePathProps = {DocAnnotationConstants.VALUE_PROP, "url"};

        // @RequestMapping
        MappingAnnotation requestMapping = MappingAnnotation.builder()
                .setAnnotationName(SpringMvcAnnotations.REQUEST_MAPPING)
                .setAnnotationFullyName("org.springframework.web.bind.annotation.RequestMapping")
                .setConsumesProp(consumes)
                .setProducesProp(produces)
                .setMethodProp(method)
                .setParamsProp(params)
                .setScope("class", "method")
                .setPathProps(pathProps);
        this.putMappingAnnotation(mappingAnnotations, requestMapping);

        // @PostMapping
        MappingAnnotation postMapping = this.createMapping(
                SpringMvcAnnotations.POST_MAPPING,
                Methods.POST.getValue(),
                "org.springframework.web.bind.annotation.PostMapping",
                pathProps,
                consumes,
                produces,
                method,
                params);
        this.putMappingAnnotation(mappingAnnotations, postMapping);

        // @GetMapping
        MappingAnnotation getMapping = this.createMapping(
                SpringMvcAnnotations.GET_MAPPING,
                Methods.GET.getValue(),
                "org.springframework.web.bind.annotation.GetMapping",
                pathProps,
                consumes,
                produces,
                method,
                params);
        this.putMappingAnnotation(mappingAnnotations, getMapping);

        // @PutMapping
        MappingAnnotation putMapping = this.createMapping(
                SpringMvcAnnotations.PUT_MAPPING,
                Methods.PUT.getValue(),
                "org.springframework.web.bind.annotation.PutMapping",
                pathProps,
                consumes,
                produces,
                method,
                params);
        this.putMappingAnnotation(mappingAnnotations, putMapping);

        // @PatchMapping
        MappingAnnotation patchMapping = this.createMapping(
                SpringMvcAnnotations.PATCH_MAPPING,
                Methods.PATCH.getValue(),
                "org.springframework.web.bind.annotation.PatchMapping",
                pathProps,
                consumes,
                produces,
                method,
                params);
        this.putMappingAnnotation(mappingAnnotations, patchMapping);

        // @DeleteMapping
        MappingAnnotation deleteMapping = this.createMapping(
                SpringMvcAnnotations.DELETE_MAPPING,
                Methods.DELETE.getValue(),
                "org.springframework.web.bind.annotation.DeleteMapping",
                pathProps,
                consumes,
                produces,
                method,
                params);
        this.putMappingAnnotation(mappingAnnotations, deleteMapping);

        // @HttpExchange and typed variants for Spring HTTP interfaces
        MappingAnnotation httpExchange = this.createMapping(
                SpringMvcAnnotations.HTTP_EXCHANGE,
                null,
                SpringMvcAnnotations.HTTP_EXCHANGE_FULLY,
                exchangePathProps,
                "contentType",
                "accept",
                method,
                params);
        this.putMappingAnnotation(mappingAnnotations, httpExchange);

        MappingAnnotation getExchange = this.createMapping(
                SpringMvcAnnotations.GET_EXCHANGE,
                Methods.GET.getValue(),
                SpringMvcAnnotations.GET_EXCHANGE_FULLY,
                exchangePathProps,
                "contentType",
                "accept",
                method,
                params);
        this.putMappingAnnotation(mappingAnnotations, getExchange);

        MappingAnnotation postExchange = this.createMapping(
                SpringMvcAnnotations.POST_EXCHANGE,
                Methods.POST.getValue(),
                SpringMvcAnnotations.POST_EXCHANGE_FULLY,
                exchangePathProps,
                "contentType",
                "accept",
                method,
                params);
        this.putMappingAnnotation(mappingAnnotations, postExchange);

        MappingAnnotation putExchange = this.createMapping(
                SpringMvcAnnotations.PUT_EXCHANGE,
                Methods.PUT.getValue(),
                SpringMvcAnnotations.PUT_EXCHANGE_FULLY,
                exchangePathProps,
                "contentType",
                "accept",
                method,
                params);
        this.putMappingAnnotation(mappingAnnotations, putExchange);

        MappingAnnotation patchExchange = this.createMapping(
                SpringMvcAnnotations.PATCH_EXCHANGE,
                Methods.PATCH.getValue(),
                SpringMvcAnnotations.PATCH_EXCHANGE_FULLY,
                exchangePathProps,
                "contentType",
                "accept",
                method,
                params);
        this.putMappingAnnotation(mappingAnnotations, patchExchange);

        MappingAnnotation deleteExchange = this.createMapping(
                SpringMvcAnnotations.DELETE_EXCHANGE,
                Methods.DELETE.getValue(),
                SpringMvcAnnotations.DELETE_EXCHANGE_FULLY,
                exchangePathProps,
                "contentType",
                "accept",
                method,
                params);
        this.putMappingAnnotation(mappingAnnotations, deleteExchange);

        // @FeignClient
        MappingAnnotation feignClient = MappingAnnotation.builder()
                .setAnnotationName(DocGlobalConstants.FEIGN_CLIENT)
                .setAnnotationFullyName(DocGlobalConstants.FEIGN_CLIENT_FULLY)
                .setPathProps(DocAnnotationConstants.PATH_PROP);
        this.putMappingAnnotation(mappingAnnotations, feignClient);

        return mappingAnnotations;
    }

    /**
     * Helper method to create common HTTP method-based mappings
     * (e.g., @GetMapping, @PostMapping).
     * @param annotationName the annotation name
     * @param methodType the method type
     * @param pathProps the path properties
     * @param consumes the consumes property
     * @param produces the produces property
     * @param method the HTTP method
     * @param params the params property
     */
    private MappingAnnotation createMapping(
            String annotationName,
            String methodType,
            String annotationFullyName,
            String[] pathProps,
            String consumes,
            String produces,
            String method,
            String params) {
        return MappingAnnotation.builder()
                .setAnnotationName(annotationName)
                .setAnnotationFullyName(annotationFullyName)
                .setConsumesProp(consumes)
                .setProducesProp(produces)
                .setMethodProp(method)
                .setParamsProp(params)
                .setMethodType(methodType)
                .setPathProps(pathProps);
    }

    private void putMappingAnnotation(
            Map<String, MappingAnnotation> mappingAnnotations, MappingAnnotation mappingAnnotation) {
        mappingAnnotations.put(mappingAnnotation.getAnnotationName(), mappingAnnotation);
        if (StringUtil.isNotEmpty(mappingAnnotation.getAnnotationFullyName())) {
            mappingAnnotations.put(mappingAnnotation.getAnnotationFullyName(), mappingAnnotation);
        }
    }
}
