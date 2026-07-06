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
package com.github.hsindumas.stagger.handler;

import static com.github.hsindumas.stagger.constants.DocTags.DEPRECATED;
import static com.github.hsindumas.stagger.constants.DocTags.IGNORE;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.common.util.CollectionUtil;
import com.github.hsindumas.stagger.common.util.StringUtil;
import com.github.hsindumas.stagger.constants.DocAnnotationConstants;
import com.github.hsindumas.stagger.constants.Methods;
import com.github.hsindumas.stagger.function.RequestMappingFunc;
import com.github.hsindumas.stagger.model.annotation.FrameworkAnnotations;
import com.github.hsindumas.stagger.model.annotation.MappingAnnotation;
import com.github.hsindumas.stagger.model.request.RequestMapping;
import com.github.hsindumas.stagger.utils.DocUtil;
import com.github.hsindumas.stagger.utils.JavaClassUtil;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Spring MVC RequestMapping Handler
 *
 * @author yu 2019/12/22.
 * @author HsinDumas
 */
public class SpringMVCRequestMappingHandler implements IRequestMappingHandler, IWebSocketRequestHandler {

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
        String methodType = null;
        String shortUrl = null;
        String mediaType = null;
        boolean deprecated = Objects.nonNull(DocUtil.getMethodTagByName(method, DEPRECATED));
        Map<String, MappingAnnotation> mappingAnnotationMap = frameworkAnnotations.getMappingAnnotations();
        for (Object annotation : annotations) {
            String annotationName = JavaClassUtil.getClassSimpleName(DocUtil.getAnnotationTypeSimpleName(annotation));
            if (DocAnnotationConstants.DEPRECATED.equals(annotationName)) {
                deprecated = true;
            }
            MappingAnnotation mappingAnnotation = mappingAnnotationMap.get(annotationName);
            if (Objects.isNull(mappingAnnotation)) {
                continue;
            }
            // get consumes of annotation
            Object consumes = DocUtil.getAnnotationNamedParameter(annotation, "consumes");
            if (Objects.nonNull(consumes)) {
                mediaType = consumes.toString();
            }
            if (CollectionUtil.isNotEmpty(mappingAnnotation.getPathProps())) {
                ClassLoader classLoader = projectBuilder.getApiConfig().getClassLoader();
                shortUrl = DocUtil.getPathUrl(
                        classLoader,
                        annotation,
                        mappingAnnotation.getPathProps().toArray(new String[0]));
            }
            if (StringUtil.isNotEmpty(mappingAnnotation.getMethodType())) {
                methodType = mappingAnnotation.getMethodType();
            } else {
                Object nameParam = DocUtil.getAnnotationNamedParameter(annotation, mappingAnnotation.getMethodProp());
                if (Objects.nonNull(nameParam)) {
                    methodType = nameParam.toString();
                    methodType = DocUtil.handleHttpMethod(methodType);
                } else {
                    methodType = Methods.GET.getValue();
                }
            }
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
