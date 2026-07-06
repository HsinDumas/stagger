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
package com.github.hsindumas.stagger.constants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Shared constants used across multiple document generators.
 *
 * @author HsinDumas
 */
public interface CommonConstants {

    /**
     * Default code used for the fallback API group.
     */
    String DEFAULT_GROUP_CODE = "default";

    /**
     * Shared Gson instance for deterministic JSON serialization.
     */
    Gson GSON = new GsonBuilder().setPrettyPrinting().create();
}
