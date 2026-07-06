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
package com.github.hsindumas.stagger.template.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Minimal template abstraction that keeps the previous API surface stable.
 */
public class Template {

    private final Function<Map<String, Object>, String> renderer;

    private final Map<String, Object> bindingValues = new HashMap<>(16);

    /**
     * Create a template wrapper.
     * @param renderer template renderer
     */
    public Template(Function<Map<String, Object>, String> renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    /**
     * Bind a single value.
     * @param key key
     * @param value value
     */
    public void binding(String key, Object value) {
        bindingValues.put(key, value);
    }

    /**
     * Bind multiple values.
     * @param values values
     */
    public void binding(Map<String, Object> values) {
        if (values != null) {
            bindingValues.putAll(values);
        }
    }

    /**
     * Render output.
     * @return rendered content
     */
    public String render() {
        return renderer.apply(new HashMap<>(bindingValues));
    }
}
