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
package com.github.hsindumas.stagger.constants;

/**
 *
 * Spring MVC Annotations
 *
 * @author yu 2019/12/21.
 * @author HsinDumas
 */
public interface SpringMvcAnnotations {

    String REQUEST_MAPPING = "RequestMapping";

    String GET_MAPPING = "GetMapping";

    String POST_MAPPING = "PostMapping";

    String PUT_MAPPING = "PutMapping";

    String PATCH_MAPPING = "PatchMapping";

    String DELETE_MAPPING = "DeleteMapping";

    String HTTP_EXCHANGE = "HttpExchange";

    String HTTP_EXCHANGE_FULLY = "org.springframework.web.service.annotation.HttpExchange";

    String GET_EXCHANGE = "GetExchange";

    String GET_EXCHANGE_FULLY = "org.springframework.web.service.annotation.GetExchange";

    String POST_EXCHANGE = "PostExchange";

    String POST_EXCHANGE_FULLY = "org.springframework.web.service.annotation.PostExchange";

    String PUT_EXCHANGE = "PutExchange";

    String PUT_EXCHANGE_FULLY = "org.springframework.web.service.annotation.PutExchange";

    String DELETE_EXCHANGE = "DeleteExchange";

    String DELETE_EXCHANGE_FULLY = "org.springframework.web.service.annotation.DeleteExchange";

    String PATCH_EXCHANGE = "PatchExchange";

    String PATCH_EXCHANGE_FULLY = "org.springframework.web.service.annotation.PatchExchange";

    @Deprecated
    String REQUEST_HERDER = "RequestHeader";

    /**
     * RequestHeader
     */
    String REQUEST_HEADER = "RequestHeader";

    String REQUEST_PARAM = "RequestParam";

    String REQUEST_PARAM_FULLY = "org.springframework.web.bind.annotation.RequestParam";

    String REQUEST_PARAM_WEB_SERVICE_FULLY = "org.springframework.web.service.annotation.RequestParam";

    String REQUEST_PART = "RequestPart";

    String REQUEST_PART_FULLY = "org.springframework.web.bind.annotation.RequestPart";

    String REQUEST_BODY = "RequestBody";

    String REQUEST_BODY_WEB_SERVICE_FULLY = "org.springframework.web.service.annotation.RequestBody";

    String CONTROLLER = "Controller";

    String CONTROLLER_FULLY = "org.springframework.stereotype.Controller";

    String REST_CONTROLLER = "RestController";

    String REST_CONTROLLER_FULLY = "org.springframework.web.bind.annotation.RestController";

    String PATH_VARIABLE = "PathVariable";

    String PATH_VARIABLE_FULLY = "org.springframework.web.bind.annotation.PathVariable";

    String PATH_VARIABLE_WEB_SERVICE_FULLY = "org.springframework.web.service.annotation.PathVariable";

    String SESSION_ATTRIBUTE = "SessionAttribute";

    String REQUEST_ATTRIBUTE = "RequestAttribute";

    String REQUEST_BODY_FULLY = "org.springframework.web.bind.annotation.RequestBody";

    String REQUEST_HEADER_FULLY = "org.springframework.web.bind.annotation.RequestHeader";

    String REQUEST_HEADER_WEB_SERVICE_FULLY = "org.springframework.web.service.annotation.RequestHeader";

    String SERVER_ENDPOINT = "ServerEndpoint";

    String REST_CONTROLLER_ADVICE = "RestControllerAdvice";

    String CONTROLLER_ADVICE = "ControllerAdvice";

    String EXCEPTION_HANDLER = "ExceptionHandler";

    String RESPONSE_STATUS = "ResponseStatus";
}
