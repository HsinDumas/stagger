/*
 * stagger https://github.com/HsinDumas/stagger
 *
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

package com.github.hsindumas.stagger.maven.plugin.chain;

import java.util.Objects;
import org.apache.maven.artifact.Artifact;

/**
 * @author yu 2020/1/13.
 * @author HsinDumas
 */
public interface FilterChain {

	void setNext(FilterChain nextInChain);

	boolean ignoreArtifactById(Artifact artifact);

	/**
	 * Delegate to next chain element when present.
	 * @param nextInChain next chain element
	 * @param artifact artifact metadata
	 * @return true when artifact should be ignored
	 */
	default boolean ignore(FilterChain nextInChain, Artifact artifact) {
		if (Objects.nonNull(nextInChain)) {
			return nextInChain.ignoreArtifactById(artifact);
		}
		else {
			return false;
		}
	}

}
