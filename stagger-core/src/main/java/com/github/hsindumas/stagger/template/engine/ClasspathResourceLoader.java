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
package com.github.hsindumas.stagger.template.engine;

/**
 * Classpath resource loader compatible with the previous Beetl API usage.
 */
public class ClasspathResourceLoader {

    private final String root;

    /**
     * Build loader.
     * @param root classpath root
     */
    public ClasspathResourceLoader(String root) {
        this.root = normalizeRoot(root);
    }

    /**
     * Resolve a classpath resource.
     * @param source source path under root
     * @return resource
     */
    public Resource<?> getResource(String source) {
        String path = normalizePath(source);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ClasspathResourceLoader.class.getClassLoader();
        }
        return new Resource<>(path, loader);
    }

    private String normalizeRoot(String input) {
        if (input == null || input.isEmpty() || "/".equals(input)) {
            return "";
        }
        String normalized = input.startsWith("/") ? input.substring(1) : input;
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private String normalizePath(String source) {
        if (source == null || source.isEmpty()) {
            return root;
        }
        String normalizedSource = source.startsWith("/") ? source.substring(1) : source;
        return root + normalizedSource;
    }
}
