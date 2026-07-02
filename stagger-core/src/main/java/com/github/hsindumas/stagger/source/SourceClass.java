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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Source class abstraction.
 *
 * @author HsinDumas
 * @since 5.0.0
 */
public interface SourceClass {

	String qualifiedName();

	String simpleName();

	List<SourceAnnotation> annotations();

	List<SourceMethod> methods();

	List<SourceField> fields();

	List<SourceTypeParam> typeParameters();

	Optional<SourceType> superType();

	List<SourceType> interfaces();

	String comment();

	List<SourceDocletTag> docletTags();

	boolean isEnum();

	default List<String> enumConstants() {
		return Collections.emptyList();
	}

	boolean isInterface();

	boolean isAnnotation();

	boolean isRecord();

	boolean isSealed();

	List<String> permittedSubtypes();

}
