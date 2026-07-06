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

import com.github.hsindumas.stagger.constants.DocAnnotationConstants;
import com.github.hsindumas.stagger.constants.SpringMvcAnnotations;
import com.github.hsindumas.stagger.model.annotation.HeaderAnnotation;
import com.github.hsindumas.stagger.utils.JavaClassUtil;

/**
 * SpringMVC RequestHeaderHandler
 *
 * @author yu 2019/12/22.
 * @author HsinDumas
 */
public class SpringMVCRequestHeaderHandler implements IHeaderHandler {

    @Override
    public boolean isMapping(String annotationName) {
        String simpleName = JavaClassUtil.getClassSimpleName(annotationName);
        switch (simpleName) {
            case SpringMvcAnnotations.GET_MAPPING:
            case SpringMvcAnnotations.REQUEST_MAPPING:
            case SpringMvcAnnotations.POST_MAPPING:
            case SpringMvcAnnotations.PUT_MAPPING:
            case SpringMvcAnnotations.PATCH_MAPPING:
            case SpringMvcAnnotations.DELETE_MAPPING:
            case SpringMvcAnnotations.HTTP_EXCHANGE:
            case SpringMvcAnnotations.GET_EXCHANGE:
            case SpringMvcAnnotations.POST_EXCHANGE:
            case SpringMvcAnnotations.PUT_EXCHANGE:
            case SpringMvcAnnotations.PATCH_EXCHANGE:
            case SpringMvcAnnotations.DELETE_EXCHANGE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public HeaderAnnotation getHeaderAnnotation() {
        return HeaderAnnotation.builder()
                .setAnnotationName(SpringMvcAnnotations.REQUEST_HEADER)
                .setValueProp(DocAnnotationConstants.VALUE_PROP)
                .setDefaultValueProp(DocAnnotationConstants.DEFAULT_VALUE_PROP)
                .setRequiredProp(DocAnnotationConstants.REQUIRED_PROP);
    }
}
