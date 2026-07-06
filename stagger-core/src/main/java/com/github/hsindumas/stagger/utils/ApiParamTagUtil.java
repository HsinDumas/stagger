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
package com.github.hsindumas.stagger.utils;

import com.github.hsindumas.stagger.common.util.CollectionUtil;
import com.github.hsindumas.stagger.common.util.StringUtil;
import com.github.hsindumas.stagger.constants.ParamTypeConstants;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiMethodDoc;
import com.github.hsindumas.stagger.model.BodyAdvice;
import java.util.List;
import java.util.Optional;

/**
 * Utility methods for tagging request/response array metadata on API methods.
 *
 * @author HsinDumas
 */
public final class ApiParamTagUtil {

    private ApiParamTagUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Set request/response array tags for an API method.
     * @param method method
     * @param apiMethodDoc api method doc
     * @param apiConfig api config
     */
    public static void setArrayTags(Object method, ApiMethodDoc apiMethodDoc, ApiConfig apiConfig) {
        String returnTypeName = DocUtil.getMethodReturnTypeCanonicalName(method);
        apiMethodDoc.setIsRequestArray(0);
        apiMethodDoc.setIsResponseArray(0);
        String responseBodyAdviceClassName = Optional.ofNullable(apiConfig)
                .map(ApiConfig::getResponseBodyAdvice)
                .map(BodyAdvice::getClassName)
                .orElse(StringUtil.EMPTY);
        String realReturnTypeName =
                StringUtil.isEmpty(responseBodyAdviceClassName) ? returnTypeName : responseBodyAdviceClassName;
        boolean respArray = JavaClassValidateUtil.isCollection(realReturnTypeName)
                || JavaClassValidateUtil.isArray(realReturnTypeName);
        if (respArray) {
            apiMethodDoc.setIsResponseArray(1);
            String className = getType(DocUtil.getMethodReturnTypeGenericCanonicalName(method));
            String arrayType =
                    JavaClassValidateUtil.isPrimitive(className) ? className : ParamTypeConstants.PARAM_TYPE_OBJECT;
            apiMethodDoc.setResponseArrayType(arrayType);
        }
        List<?> methodParameters = DocUtil.getMethodParameters(method);
        if (CollectionUtil.isNotEmpty(methodParameters)) {
            String requestBodyAdviceClassName = Optional.ofNullable(apiConfig)
                    .map(ApiConfig::getRequestBodyAdvice)
                    .map(BodyAdvice::getClassName)
                    .orElse(StringUtil.EMPTY);
            for (Object param : methodParameters) {
                String typeName = DocUtil.getParameterTypeCanonicalName(param);
                String realTypeName =
                        StringUtil.isEmpty(requestBodyAdviceClassName) ? typeName : requestBodyAdviceClassName;
                boolean reqArray =
                        JavaClassValidateUtil.isCollection(realTypeName) || JavaClassValidateUtil.isArray(realTypeName);
                if (reqArray) {
                    apiMethodDoc.setIsRequestArray(1);
                    String className = getType(DocUtil.getParameterTypeGenericCanonicalName(param));
                    String arrayType = JavaClassValidateUtil.isPrimitive(className)
                            ? className
                            : ParamTypeConstants.PARAM_TYPE_OBJECT;
                    apiMethodDoc.setRequestArrayType(arrayType);
                    break;
                }
            }
        }
    }

    private static String getType(String typeName) {
        String gicType;
        if (typeName.contains("<")) {
            gicType = typeName.substring(typeName.indexOf("<") + 1, typeName.lastIndexOf(">"));
        } else {
            gicType = typeName;
        }
        if (gicType.contains("[")) {
            gicType = gicType.substring(0, gicType.indexOf("["));
        }
        return gicType.substring(gicType.lastIndexOf(".") + 1).toLowerCase();
    }
}
