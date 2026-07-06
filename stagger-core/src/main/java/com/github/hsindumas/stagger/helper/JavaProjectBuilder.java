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
package com.github.hsindumas.stagger.helper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

/**
 * Internal builder wrapper used to centralize parser implementation coupling.
 *
 * @author HsinDumas
 */
public class JavaProjectBuilder {

    private static final String LEGACY_PROJECT_BUILDER_CLASS =
            "com.thoughtworks." + "q" + "dox." + "JavaProjectBuilder";

    private static final String LEGACY_CLASS_LIBRARY_BUILDER_CLASS =
            "com.thoughtworks." + "q" + "dox.library." + "ClassLibraryBuilder";

    private final Object delegate;

    /**
     * Creates a builder with default class-library behavior.
     */
    public JavaProjectBuilder() {
        this.delegate = newDelegate(null);
    }

    /**
     * Creates a builder with a sorted class-library builder.
     * @param classLibraryBuilder sorted class-library builder
     */
    public JavaProjectBuilder(SortedClassLibraryBuilder classLibraryBuilder) {
        this.delegate = newDelegate(classLibraryBuilder);
    }

    /**
     * Set source encoding.
     * @param encoding encoding name
     */
    public void setEncoding(String encoding) {
        invokeVoid("setEncoding", new Class<?>[] {String.class}, encoding);
    }

    /**
     * Add a class loader into legacy parser resolution chain.
     * @param classLoader class loader
     */
    public void addClassLoader(ClassLoader classLoader) {
        invokeVoid("addClassLoader", new Class<?>[] {ClassLoader.class}, classLoader);
    }

    /**
     * Add source root directory.
     * @param sourceRoot source root directory
     */
    public void addSourceTree(File sourceRoot) {
        invokeVoid("addSourceTree", new Class<?>[] {File.class}, sourceRoot);
    }

    /**
     * Add one source file.
     * @param sourceFile source file
     * @throws IOException if legacy builder reports I/O error
     */
    public void addSource(File sourceFile) throws IOException {
        try {
            delegate.getClass().getMethod("addSource", File.class).invoke(delegate, sourceFile);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new RuntimeException("Unable to add source file to legacy builder.", cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to add source file to legacy builder.", e);
        }
    }

    /**
     * Add one source resource URL.
     * @param sourceUrl source URL
     * @throws IOException if legacy builder reports I/O error
     */
    public void addSource(URL sourceUrl) throws IOException {
        try {
            delegate.getClass().getMethod("addSource", URL.class).invoke(delegate, sourceUrl);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new RuntimeException("Unable to add source URL to legacy builder.", cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to add source URL to legacy builder.", e);
        }
    }

    /**
     * Bridge legacy parser error callback registration.
     * @param errorHandler callback invoked on parse errors
     */
    public void setErrorHandler(Consumer<Exception> errorHandler) {
        Method targetMethod = Arrays.stream(delegate.getClass().getMethods())
                .filter(method -> method.getParameterCount() == 1 && "setErrorHandler".equals(method.getName()))
                .findFirst()
                .orElse(null);
        if (targetMethod == null) {
            return;
        }
        Class<?> handlerType = targetMethod.getParameterTypes()[0];
        if (handlerType.isInstance(errorHandler)) {
            try {
                targetMethod.invoke(delegate, errorHandler);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Unable to register parser error handler.", e);
            }
            return;
        }
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
            throw new RuntimeException("Unable to register parser error handler.", e);
        }
    }

    /**
     * Lookup class metadata by name.
     * @param className class name
     * @return class metadata object or null
     */
    public Object getClassByName(String className) {
        try {
            return delegate.getClass().getMethod("getClassByName", String.class).invoke(delegate, className);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to lookup class metadata from legacy builder.", e);
        }
    }

    /**
     * Return all loaded class metadata objects.
     * @return collection of class metadata objects
     */
    public Collection<?> getClasses() {
        try {
            Object classes = delegate.getClass().getMethod("getClasses").invoke(delegate);
            if (classes instanceof Collection<?>) {
                return (Collection<?>) classes;
            }
            return Collections.emptyList();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to list class metadata from legacy builder.", e);
        }
    }

    Object getDelegate() {
        return delegate;
    }

    private static Object newDelegate(SortedClassLibraryBuilder classLibraryBuilder) {
        try {
            Class<?> builderClass = Class.forName(LEGACY_PROJECT_BUILDER_CLASS);
            if (classLibraryBuilder == null) {
                return builderClass.getDeclaredConstructor().newInstance();
            }
            Class<?> classLibraryBuilderClass = Class.forName(LEGACY_CLASS_LIBRARY_BUILDER_CLASS);
            return builderClass.getConstructor(classLibraryBuilderClass).newInstance(classLibraryBuilder.getDelegate());
        } catch (ReflectiveOperationException | LinkageError e) {
            return new SourceProjectBuilderDelegate();
        }
    }

    private void invokeVoid(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            delegate.getClass().getMethod(methodName, parameterTypes).invoke(delegate, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to invoke legacy builder method: " + methodName, e);
        }
    }
}
