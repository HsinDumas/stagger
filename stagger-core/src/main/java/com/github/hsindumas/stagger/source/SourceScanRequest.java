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

package com.github.hsindumas.stagger.source;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Request object for source scanning.
 *
 * @author HsinDumas
 * @since 5.0.0
 */
public final class SourceScanRequest {

	private final List<Path> sourceRoots;

	private final Charset charset;

	private SourceScanRequest(List<Path> sourceRoots, Charset charset) {
		this.sourceRoots = Collections.unmodifiableList(new ArrayList<>(sourceRoots));
		this.charset = charset;
	}

	public static Builder builder() {
		return new Builder();
	}

	public List<Path> getSourceRoots() {
		return sourceRoots;
	}

	public Charset getCharset() {
		return charset;
	}

	/**
	 * Builder for {@link SourceScanRequest}.
	 */
	public static final class Builder {

		private final List<Path> sourceRoots = new ArrayList<>();

		private Charset charset = StandardCharsets.UTF_8;

		private Builder() {
		}

		/**
		 * Add one source root.
		 * @param sourceRoot source root path
		 * @return this builder
		 */
		public Builder addSourceRoot(Path sourceRoot) {
			this.sourceRoots.add(Objects.requireNonNull(sourceRoot, "sourceRoot"));
			return this;
		}

		/**
		 * Add multiple source roots.
		 * @param roots source root paths
		 * @return this builder
		 */
		public Builder addSourceRoots(Collection<Path> roots) {
			if (Objects.isNull(roots)) {
				return this;
			}
			for (Path root : roots) {
				this.addSourceRoot(root);
			}
			return this;
		}

		/**
		 * Set source file charset.
		 * @param charset source charset
		 * @return this builder
		 */
		public Builder setCharset(Charset charset) {
			this.charset = Objects.requireNonNull(charset, "charset");
			return this;
		}

		/**
		 * Build immutable request instance.
		 * @return source scan request
		 */
		public SourceScanRequest build() {
			return new SourceScanRequest(this.sourceRoots, this.charset);
		}

	}

}
