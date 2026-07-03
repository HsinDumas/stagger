/*
 * stagger
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

package com.github.hsindumas.stagger.maven.plugin.util;

import com.github.hsindumas.stagger.maven.plugin.chain.CommonArtifactFilterChain;
import com.github.hsindumas.stagger.maven.plugin.chain.ContainsFilterChain;
import com.github.hsindumas.stagger.maven.plugin.chain.FilterChain;
import com.github.hsindumas.stagger.maven.plugin.chain.GroupIdFilterChain;
import com.github.hsindumas.stagger.maven.plugin.chain.SpringBootArtifactFilterChain;
import com.github.hsindumas.stagger.maven.plugin.chain.StartsWithFilterChain;
import org.apache.maven.artifact.Artifact;

/**
 * Artifact filter util
 *
 * @author yu 2020/1/11.
 * @author HsinDumas
 */
public class ArtifactFilterUtil {

	/**
	 * ignoreArtifact
	 * @param artifact Artifact
	 * @return boolean
	 */
	public static boolean ignoreArtifact(Artifact artifact) {
		if ("test".equals(artifact.getScope())) {
			return true;
		}
		FilterChain groupFilterChain = new GroupIdFilterChain();
		FilterChain startsWithFilterChain = new StartsWithFilterChain();
		FilterChain containsFilterChain = new ContainsFilterChain();
		FilterChain commonArtifactFilterChain = new CommonArtifactFilterChain();

		groupFilterChain.setNext(startsWithFilterChain);
		startsWithFilterChain.setNext(containsFilterChain);
		containsFilterChain.setNext(commonArtifactFilterChain);
		FilterChain springBootArtifactFilterChain = new SpringBootArtifactFilterChain();
		commonArtifactFilterChain.setNext(springBootArtifactFilterChain);
		return groupFilterChain.ignoreArtifactById(artifact);
	}

	public static boolean ignoreSpringBootArtifactById(Artifact artifact) {
		FilterChain springBootArtifactFilterChain = new SpringBootArtifactFilterChain();
		return springBootArtifactFilterChain.ignoreArtifactById(artifact);
	}

}
