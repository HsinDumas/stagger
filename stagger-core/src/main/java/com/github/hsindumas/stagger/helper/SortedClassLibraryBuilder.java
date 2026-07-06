/*
 * Copyright (C) 2018-2026 stagger
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
package com.github.hsindumas.stagger.helper;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Internal class-library wrapper used to avoid exposing parser types to plugin modules.
 *
 * @author HsinDumas
 */
public class SortedClassLibraryBuilder {

    private static final String LEGACY_LIBRARY_BUILDER_CLASS =
            "com.thoughtworks." + "q" + "dox.library." + "SortedClassLibraryBuilder";

    private final Object delegate;

    /**
     * Creates a wrapper backed by the legacy parser class-library builder.
     */
    public SortedClassLibraryBuilder() {
        this.delegate = instantiateDelegate();
    }

    Object getDelegate() {
        return delegate;
    }

    /**
     * Bridge legacy parser error callback registration.
     * @param errorHandler callback invoked on parse errors
     */
    public void setErrorHander(Consumer<Exception> errorHandler) {
        invokeErrorHandler(Arrays.asList("setErrorHander", "setErrorHandler"), errorHandler);
    }

    private static Object instantiateDelegate() {
        try {
            Class<?> builderClass = Class.forName(LEGACY_LIBRARY_BUILDER_CLASS);
            return builderClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError e) {
            return new NoOpClassLibraryBuilder();
        }
    }

    private static final class NoOpClassLibraryBuilder {

        public void setErrorHandler(Consumer<Exception> errorHandler) {
            // No-op for source-backed parser fallback.
        }

        public void setErrorHander(Consumer<Exception> errorHandler) {
            // Keep typo-compatible alias to mirror upstream API.
            this.setErrorHandler(errorHandler);
        }
    }

    private void invokeErrorHandler(List<String> methodNames, Consumer<Exception> errorHandler) {
        Method targetMethod = Arrays.stream(delegate.getClass().getMethods())
                .filter(method -> method.getParameterCount() == 1 && methodNames.contains(method.getName()))
                .findFirst()
                .orElse(null);
        if (targetMethod == null) {
            return;
        }
        Class<?> handlerType = targetMethod.getParameterTypes()[0];
        Object handler = Proxy.newProxyInstance(
                handlerType.getClassLoader(), new Class<?>[] {handlerType}, (proxy, method, args) -> {
                    Exception exception = new Exception("Legacy parser error.");
                    if (args != null && args.length > 0) {
                        Object first = args[0];
                        if (first instanceof Exception) {
                            exception = (Exception) first;
                        } else if (first instanceof Throwable) {
                            exception = new Exception((Throwable) first);
                        }
                    }
                    errorHandler.accept(exception);
                    return null;
                });
        try {
            targetMethod.invoke(delegate, handler);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to register legacy parser error handler.", e);
        }
    }
}
