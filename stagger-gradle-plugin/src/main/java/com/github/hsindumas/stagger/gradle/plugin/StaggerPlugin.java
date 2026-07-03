/*
 * stagger
 *
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
package com.github.hsindumas.stagger.gradle.plugin;

import com.github.hsindumas.stagger.gradle.constant.GlobalConstants;
import com.github.hsindumas.stagger.gradle.constant.TaskConstants;
import com.github.hsindumas.stagger.gradle.extension.StaggerPluginExtension;
import com.github.hsindumas.stagger.gradle.task.DocBaseTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;

/**
 * @author yu 2020/2/16.
 * @author HsinDumas
 */
public class StaggerPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(JavaPlugin.class);
		TaskProvider<Task> javaCompileTask = project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME);
		TaskConstants.taskMap
			.forEach((taskName, taskClass) -> this.createTask(project, taskName, taskClass, javaCompileTask));

		// extend project-model to get our settings/configuration via nice configuration
		project.getExtensions().create(GlobalConstants.EXTENSION_NAME, StaggerPluginExtension.class);
	}

	private <T extends Task> void createTask(Project project, String taskName, Class<T> taskClass,
			TaskProvider<Task> javaCompileTask) {
		project.getTasks().register(taskName, taskClass, task -> {
			task.setGroup(GlobalConstants.TASK_GROUP);
			task.dependsOn(javaCompileTask);
			if (task instanceof DocBaseTask) {
				((DocBaseTask) task).setTaskProject(project);
			}
		});
	}

}
