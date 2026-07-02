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
package com.github.hsindumas.stagger.builder;

import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.constants.HighLightJsConstants;
import com.github.hsindumas.stagger.constants.HighlightStyle;
import com.github.hsindumas.stagger.helper.JavaProjectBuilderHelper;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiConstant;
import com.github.hsindumas.stagger.model.ApiDataDictionary;
import com.github.hsindumas.stagger.model.ApiErrorCodeDictionary;
import com.github.hsindumas.stagger.model.ApiObjectReplacement;
import com.github.hsindumas.stagger.model.BodyAdvice;
import com.github.hsindumas.stagger.model.CustomField;
import com.github.hsindumas.stagger.model.DocJavaField;
import com.github.hsindumas.stagger.model.SourceCodePath;
import com.github.hsindumas.stagger.source.SourceClass;
import com.github.hsindumas.stagger.source.SourceProject;
import com.github.hsindumas.stagger.source.SourceProjects;
import com.github.hsindumas.stagger.source.SourceScanRequest;
import com.github.hsindumas.stagger.source.SourceType;
import com.github.hsindumas.stagger.utils.JavaClassUtil;
import com.power.common.constants.Charset;
import com.power.common.util.CollectionUtil;
import com.power.common.util.StringUtil;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaParameterizedType;
import com.thoughtworks.qdox.model.JavaType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * ProjectDocConfigBuilder
 *
 * @author yu 2019/12/21.
 * @author HsinDumas
 * @since 1.8.0
 */
public class ProjectDocConfigBuilder {

	/**
	 * Logger
	 */
	private static final Logger log = Logger.getLogger(ProjectDocConfigBuilder.class.getName());

	/**
	 * JavaProjectBuilder
	 */
	private final JavaProjectBuilder javaProjectBuilder;

	/**
	 * classFilesMap
	 */
	private final Map<String, JavaClass> classFilesMap = new ConcurrentHashMap<>();

	/**
	 * Source project view used by JavaParser abstraction.
	 */
	private final SourceProject sourceProject;

	/**
	 * enumClassMap
	 */
	private final Map<String, Class<? extends Enum<?>>> enumClassMap = new ConcurrentHashMap<>();

	/**
	 * customRespFieldMap
	 */
	private final Map<CustomField.Key, CustomField> customRespFieldMap = new ConcurrentHashMap<>();

	/**
	 * customReqFieldMap
	 */
	private final Map<CustomField.Key, CustomField> customReqFieldMap = new ConcurrentHashMap<>();

	/**
	 * replaceClassMap
	 */
	private final Map<String, String> replaceClassMap = new ConcurrentHashMap<>();

	/**
	 * constantsMap
	 */
	private final Map<String, String> constantsMap = new ConcurrentHashMap<>();

	/**
	 * serverUrl
	 */
	private final String serverUrl;

	/**
	 * ApiConfig
	 */
	private final ApiConfig apiConfig;

	/**
	 * Constructor
	 * @param apiConfig ApiConfig
	 * @param javaProjectBuilder JavaProjectBuilder
	 */
	public ProjectDocConfigBuilder(ApiConfig apiConfig, JavaProjectBuilder javaProjectBuilder) {
		if (null == apiConfig) {
			throw new NullPointerException("ApiConfig can't be null.");
		}
		this.apiConfig = apiConfig;
		if (Objects.isNull(javaProjectBuilder)) {
			javaProjectBuilder = JavaProjectBuilderHelper.create();
		}

		if (StringUtil.isEmpty(apiConfig.getServerUrl())) {
			this.serverUrl = DocGlobalConstants.DEFAULT_SERVER_URL;
		}
		else {
			this.serverUrl = apiConfig.getServerUrl();
		}
		this.setHighlightStyle();
		javaProjectBuilder.setEncoding(Charset.DEFAULT_CHARSET);
		this.javaProjectBuilder = javaProjectBuilder;
		try {
			this.loadJavaSource(apiConfig, this.javaProjectBuilder);
		}
		catch (Exception e) {
			log.warning(e.getMessage());
		}
		SourceProject builtSourceProject;
		try {
			builtSourceProject = this.loadSourceProject(apiConfig);
		} catch (RuntimeException e) {
			log.warning(e.getMessage());
			builtSourceProject = SourceProjects.create().build(SourceScanRequest.builder().build());
		}
		this.sourceProject = builtSourceProject;
		this.initClassFilesMap();
		this.initCustomResponseFieldsMap(apiConfig);
		this.initCustomRequestFieldsMap(apiConfig);
		this.initReplaceClassMap(apiConfig);
		this.initConstants(apiConfig);
		this.initDict(apiConfig);
		this.checkBodyAdvice(apiConfig.getRequestBodyAdvice());
		this.checkBodyAdvice(apiConfig.getResponseBodyAdvice());
	}

	/**
	 * Init data dictionary.
	 * @param apiConfig apiConfig
	 */
	private void initDict(ApiConfig apiConfig) {
		if (enumClassMap.isEmpty()) {
			return;
		}
		List<ApiDataDictionary> dataDictionaries = apiConfig.getDataDictionaries();
		if (Objects.isNull(dataDictionaries)) {
			dataDictionaries = new ArrayList<>();
		}

		for (ApiDataDictionary dataDictionary : dataDictionaries) {
			dataDictionary.setEnumImplementSet(getEnumImplementsByInterface(dataDictionary.getEnumClass()));
		}

		List<ApiErrorCodeDictionary> errorCodeDictionaries = apiConfig.getErrorCodeDictionaries();
		if (Objects.isNull(errorCodeDictionaries)) {
			errorCodeDictionaries = new ArrayList<>();
		}

		for (ApiErrorCodeDictionary errorCodeDictionary : errorCodeDictionaries) {
			errorCodeDictionary
				.setEnumImplementSet(this.getEnumImplementsByInterface(errorCodeDictionary.getEnumClass()));
		}
	}

	/**
	 * Get enum implements by interface.
	 * @param enumClass enumClass
	 * @return enum implements
	 */
	private Set<Class<? extends Enum<?>>> getEnumImplementsByInterface(Class<?> enumClass) {
		if (!enumClass.isInterface()) {
			return Collections.emptySet();
		}
		Set<Class<? extends Enum<?>>> set = new HashSet<>();
		enumClassMap.forEach((k, v) -> {
			if (enumClass.isAssignableFrom(v)) {
				set.add(v);
			}
		});
		return set;
	}

	/**
	 * Get class by name.
	 * @param simpleName simpleName
	 * @return JavaClass
	 */
	public JavaClass getClassByName(String simpleName) {
		JavaClass cls = javaProjectBuilder.getClassByName(simpleName);
		if (Objects.isNull(cls)) {
			return classFilesMap.get(simpleName);
		}

		if (!cls.isEnum()) {
			List<DocJavaField> fieldList = JavaClassUtil.getFields(cls, 0, new LinkedHashMap<>(), null);
			// handle inner class
			if (Objects.isNull(cls.getFields()) || fieldList.isEmpty()) {
				cls = classFilesMap.get(simpleName);
				return cls;
			}
		}

		List<JavaClass> classList = cls.getNestedClasses();
		for (JavaClass javaClass : classList) {
			classFilesMap.put(javaClass.getFullyQualifiedName(), javaClass);
		}
		return cls;
	}

	/**
	 * Determine whether type is enum.
	 *
	 * @param typeName type name
	 *
	 * @return true if enum
	 */
	public boolean isEnumType(String typeName) {
		if (StringUtil.isEmpty(typeName)) {
			return false;
		}
		JavaClass javaClass = this.getClassByName(typeName);
		if (Objects.nonNull(javaClass)) {
			return javaClass.isEnum();
		}
		return this.findSourceClass(typeName).map(SourceClass::isEnum).orElse(false);
	}

	/**
	 * Get sample enum value for type.
	 *
	 * @param typeName type name
	 *
	 * @return sample enum value, or null when unavailable
	 */
	public String getEnumSampleValue(String typeName) {
		if (StringUtil.isEmpty(typeName)) {
			return null;
		}
		JavaClass javaClass = this.getClassByName(typeName);
		if (Objects.nonNull(javaClass) && javaClass.isEnum()) {
			Object value = JavaClassUtil.getEnumValue(javaClass, this, Boolean.FALSE);
			return StringUtil.removeDoubleQuotes(String.valueOf(value));
		}
		return this.findSourceClass(typeName)
			.filter(SourceClass::isEnum)
			.map(SourceClass::enumConstants)
			.filter(CollectionUtil::isNotEmpty)
			.map(constants -> constants.get(0))
			.orElse(null);
	}

	private Optional<SourceClass> findSourceClass(String typeName) {
		Optional<SourceClass> sourceClass = this.sourceProject.findClass(typeName);
		if (sourceClass.isPresent()) {
			return sourceClass;
		}
		return this.sourceProject.classes().stream().filter(clazz -> typeName.equals(clazz.simpleName())).findFirst();
	}

	/**
	 * Resolve implemented interface names for a class.
	 *
	 * @param className class name
	 *
	 * @return implemented interface names, preserving insertion order
	 */
	public List<String> getImplementedInterfaceNames(String className) {
		if (StringUtil.isEmpty(className)) {
			return Collections.emptyList();
		}
		Set<String> interfaceNames = new LinkedHashSet<>();
		JavaClass javaClass = this.getClassByName(className);
		if (Objects.nonNull(javaClass)) {
			List<JavaType> interfaces = JavaClassUtil.getImplementedInterfaces(javaClass);
			if (CollectionUtil.isNotEmpty(interfaces)) {
				for (JavaType javaType : interfaces) {
					if (Objects.isNull(javaType)) {
						continue;
					}
					String canonicalName = javaType.getCanonicalName();
					if (StringUtil.isNotEmpty(canonicalName)) {
						interfaceNames.add(canonicalName);
					}
					String fullyQualifiedName = javaType.getFullyQualifiedName();
					if (StringUtil.isNotEmpty(fullyQualifiedName)) {
						interfaceNames.add(fullyQualifiedName);
					}
				}
			}
		}
		if (interfaceNames.isEmpty()) {
			this.findSourceClass(className)
				.ifPresent(sourceClass -> sourceClass.interfaces()
					.stream()
					.map(SourceType::qualifiedName)
					.filter(StringUtil::isNotEmpty)
					.forEach(interfaceNames::add));
		}
		if (interfaceNames.isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(interfaceNames);
	}

	/**
	 * Resolve generic type argument for an implemented interface.
	 *
	 * @param className class name
	 * @param argIndex generic argument index
	 * @param interfaceNames candidate interface names
	 *
	 * @return resolved type argument name
	 */
	public Optional<String> getImplementedInterfaceTypeArgument(String className, int argIndex,
			String... interfaceNames) {
		if (StringUtil.isEmpty(className) || argIndex < 0) {
			return Optional.empty();
		}
		Set<String> normalizedInterfaceNames = this.normalizeInterfaceNames(interfaceNames);
		if (normalizedInterfaceNames.isEmpty()) {
			return Optional.empty();
		}

		JavaClass javaClass = this.getClassByName(className);
		if (Objects.nonNull(javaClass)) {
			List<JavaType> interfaces = JavaClassUtil.getImplementedInterfaces(javaClass);
			if (CollectionUtil.isNotEmpty(interfaces)) {
				for (JavaType javaType : interfaces) {
					if (Objects.isNull(javaType)
							|| !this.matchesImplementedInterface(javaType, normalizedInterfaceNames)) {
						continue;
					}
					Optional<String> typeArgument = this.resolveQdoxImplementedInterfaceTypeArgument(javaType, argIndex);
					if (typeArgument.isPresent()) {
						return typeArgument;
					}
				}
			}
		}

		Optional<SourceClass> sourceClassOptional = this.findSourceClass(className);
		if (sourceClassOptional.isEmpty()) {
			return Optional.empty();
		}
		for (SourceType sourceType : sourceClassOptional.get().interfaces()) {
			if (!this.matchesImplementedInterface(sourceType, normalizedInterfaceNames)) {
				continue;
			}
			Optional<String> typeArgument = this.resolveSourceImplementedInterfaceTypeArgument(sourceType, argIndex);
			if (typeArgument.isPresent()) {
				return typeArgument;
			}
		}
		return Optional.empty();
	}

	private Set<String> normalizeInterfaceNames(String... interfaceNames) {
		if (Objects.isNull(interfaceNames) || interfaceNames.length == 0) {
			return Collections.emptySet();
		}
		Set<String> normalizedNames = new LinkedHashSet<>();
		for (String interfaceName : interfaceNames) {
			String normalizedName = this.normalizeTypeNameForCompare(interfaceName);
			if (StringUtil.isNotEmpty(normalizedName)) {
				normalizedNames.add(normalizedName);
			}
		}
		return normalizedNames;
	}

	private Optional<String> resolveQdoxImplementedInterfaceTypeArgument(JavaType interfaceType, int argIndex) {
		if (!(interfaceType instanceof JavaParameterizedType)) {
			return Optional.empty();
		}
		List<JavaType> typeArguments = ((JavaParameterizedType) interfaceType).getActualTypeArguments();
		if (CollectionUtil.isEmpty(typeArguments) || typeArguments.size() <= argIndex) {
			return Optional.empty();
		}
		JavaType javaType = typeArguments.get(argIndex);
		if (Objects.isNull(javaType)) {
			return Optional.empty();
		}
		String resolvedTypeName = this.extractTypeName(javaType);
		if (StringUtil.isEmpty(resolvedTypeName)) {
			return Optional.empty();
		}
		return Optional.of(resolvedTypeName);
	}

	private Optional<String> resolveSourceImplementedInterfaceTypeArgument(SourceType interfaceType, int argIndex) {
		List<SourceType> typeArguments = interfaceType.typeArguments();
		if (CollectionUtil.isEmpty(typeArguments) || typeArguments.size() <= argIndex) {
			return Optional.empty();
		}
		SourceType typeArgument = typeArguments.get(argIndex);
		if (Objects.isNull(typeArgument)) {
			return Optional.empty();
		}
		if (typeArgument.isWildcard()) {
			typeArgument = typeArgument.wildcardBound().orElse(null);
			if (Objects.isNull(typeArgument)) {
				return Optional.of(Object.class.getName());
			}
		}
		String resolvedTypeName = this.extractSourceTypeName(typeArgument);
		if (StringUtil.isEmpty(resolvedTypeName)) {
			return Optional.empty();
		}
		return Optional.of(resolvedTypeName);
	}

	private boolean matchesImplementedInterface(JavaType interfaceType, Set<String> normalizedInterfaceNames) {
		return this.matchesNormalizedName(interfaceType.getBinaryName(), normalizedInterfaceNames)
				|| this.matchesNormalizedName(interfaceType.getFullyQualifiedName(), normalizedInterfaceNames)
				|| this.matchesNormalizedName(interfaceType.getCanonicalName(), normalizedInterfaceNames)
				|| this.matchesNormalizedName(interfaceType.getGenericFullyQualifiedName(), normalizedInterfaceNames);
	}

	private boolean matchesImplementedInterface(SourceType interfaceType, Set<String> normalizedInterfaceNames) {
		return this.matchesNormalizedName(interfaceType.qualifiedName(), normalizedInterfaceNames);
	}

	private boolean matchesNormalizedName(String typeName, Set<String> normalizedInterfaceNames) {
		String normalizedTypeName = this.normalizeTypeNameForCompare(typeName);
		if (StringUtil.isEmpty(normalizedTypeName)) {
			return false;
		}
		for (String normalizedInterfaceName : normalizedInterfaceNames) {
			if (normalizedTypeName.equals(normalizedInterfaceName)
					|| normalizedTypeName.endsWith("." + normalizedInterfaceName)
					|| normalizedInterfaceName.endsWith("." + normalizedTypeName)) {
				return true;
			}
		}
		return false;
	}

	private String normalizeTypeNameForCompare(String typeName) {
		String strippedTypeName = this.stripGenericType(typeName);
		if (StringUtil.isEmpty(strippedTypeName)) {
			return strippedTypeName;
		}
		return strippedTypeName.replace('$', '.');
	}

	private String stripGenericType(String typeName) {
		if (StringUtil.isEmpty(typeName)) {
			return typeName;
		}
		int genericStart = typeName.indexOf('<');
		if (genericStart > -1) {
			typeName = typeName.substring(0, genericStart);
		}
		return typeName.trim();
	}

	private String extractTypeName(JavaType javaType) {
		String[] candidates = { javaType.getBinaryName(), javaType.getFullyQualifiedName(), javaType.getCanonicalName(),
				javaType.getGenericFullyQualifiedName() };
		for (String candidate : candidates) {
			if (StringUtil.isNotEmpty(candidate)) {
				return this.stripGenericType(candidate);
			}
		}
		return null;
	}

	private String extractSourceTypeName(SourceType sourceType) {
		String qualifiedName = sourceType.qualifiedName();
		if (StringUtil.isEmpty(qualifiedName)) {
			return null;
		}
		return this.stripGenericType(qualifiedName);
	}

	/**
	 * Load java source.
	 * @param config ApiConfig
	 * @param builder JavaProjectBuilder
	 */
	private void loadJavaSource(ApiConfig config, JavaProjectBuilder builder) {
		if (CollectionUtil.isNotEmpty(config.getJarSourcePaths())) {
			for (SourceCodePath path : config.getJarSourcePaths()) {
				loadJarJavaSource(path.getPath(), builder);
			}
		}
		if (CollectionUtil.isEmpty(config.getSourceCodePaths())) {
			builder.addSourceTree(new File(DocGlobalConstants.PROJECT_CODE_PATH));
		}
		else {
			for (SourceCodePath path : config.getSourceCodePaths()) {
				if (null == path) {
					continue;
				}
				String strPath = path.getPath();
				if (StringUtil.isNotEmpty(strPath)) {
					strPath = strPath.replace("\\", DocGlobalConstants.PATH_DELIMITER);
					loadJavaSource(strPath, builder);
				}
			}
		}
	}

	/**
	 * Load jar java source.
	 * @param strPath path
	 * @param builder builder
	 */
	private void loadJavaSource(String strPath, JavaProjectBuilder builder) {
		Path sourceRoot = Path.of(strPath);
		if (Files.notExists(sourceRoot)) {
			return;
		}
		try (Stream<Path> sourceFiles = Files.walk(sourceRoot)) {
			sourceFiles.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java")).forEach(path -> {
				try {
					builder.addSource(path.toFile());
				} catch (IOException | RuntimeException e) {
					log.warning(e.getMessage());
				}
			});
		} catch (IOException e) {
			log.warning(e.getMessage());
		}
	}

	/**
	 * Build JavaParser-backed source project from configured source roots.
	 *
	 * @param config api config
	 *
	 * @return source project
	 */
	private SourceProject loadSourceProject(ApiConfig config) {
		SourceScanRequest.Builder requestBuilder = SourceScanRequest.builder().setCharset(StandardCharsets.UTF_8);
		if (CollectionUtil.isEmpty(config.getSourceCodePaths())) {
			this.addSourceRoot(requestBuilder, DocGlobalConstants.PROJECT_CODE_PATH);
		} else {
			for (SourceCodePath sourceCodePath : config.getSourceCodePaths()) {
				if (Objects.isNull(sourceCodePath) || StringUtil.isEmpty(sourceCodePath.getPath())) {
					continue;
				}
				this.addSourceRoot(requestBuilder, sourceCodePath.getPath());
			}
		}
		return SourceProjects.create().build(requestBuilder.build());
	}

	private void addSourceRoot(SourceScanRequest.Builder requestBuilder, String sourceRoot) {
		if (StringUtil.isEmpty(sourceRoot)) {
			return;
		}
		String normalizedPath = sourceRoot.replace("\\", DocGlobalConstants.PATH_DELIMITER);
		requestBuilder.addSourceRoot(Path.of(normalizedPath));
	}

	/**
	 * Load jar java source.
	 * @param path path
	 * @param builder builder
	 */
	public void loadJarJavaSource(String path, JavaProjectBuilder builder) {
		OutputStream out;
		if (!path.endsWith(".jar")) {
			return;
		}
		try (JarFile jarFile = new JarFile(path)) {
			builder.setEncoding(Charset.DEFAULT_CHARSET);
			Enumeration<JarEntry> entryEnumeration = jarFile.entries();
			while (entryEnumeration.hasMoreElements()) {
				JarEntry entry = entryEnumeration.nextElement();
				if (entry.getName().endsWith(".java")) {
					InputStream is = jarFile.getInputStream(entry);
					File file = new File(DocGlobalConstants.JAR_TEMP + entry.getName());
					if (!file.exists()) {
						file.getParentFile().mkdirs();
					}
					out = Files.newOutputStream(file.toPath());
					int len;
					while ((len = is.read()) != -1) {
						out.write(len);
					}
					is.close();
					out.close();
				}
			}
			File file = new File(DocGlobalConstants.JAR_TEMP);
			builder.addSourceTree(file);
			deleteDir(file);
		}
		catch (IOException e) {
			log.info("jar" + path + " load  error ,e :" + e);
		}
	}

	/**
	 * Delete dir.
	 * @param file file
	 */
	public static void deleteDir(File file) {
		File[] files = file.listFiles();
		if (file.isFile() || Objects.isNull(files) || files.length == 0) {
			file.delete();
		}
		else {
			for (File f : files) {
				deleteDir(f);
			}
		}
		file.delete();
	}

	/**
	 * Init class files map.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initClassFilesMap() {
		Collection<JavaClass> javaClasses = javaProjectBuilder.getClasses();
		for (JavaClass cls : javaClasses) {
			if (cls.isEnum()) {
				Class enumClass;
				ClassLoader classLoader = apiConfig.getClassLoader();
				try {
					if (Objects.isNull(classLoader)) {
						enumClass = Class.forName(cls.getBinaryName());
					}
					else {
						enumClass = classLoader.loadClass(cls.getBinaryName());
					}
					enumClassMap.put(cls.getFullyQualifiedName(), enumClass);
				}
				catch (ClassNotFoundException | NoClassDefFoundError e) {
					continue;
				}
			}
			classFilesMap.put(cls.getFullyQualifiedName(), cls);
		}
	}

	/**
	 * Init custom response fields map.
	 * @param config config
	 */
	private void initCustomResponseFieldsMap(ApiConfig config) {
		if (CollectionUtil.isNotEmpty(config.getCustomResponseFields())) {
			for (CustomField field : config.getCustomResponseFields()) {
				CustomField.Key key = CustomField.Key.create(field.getOwnerClassName(), field.getName());
				customRespFieldMap.put(key, field);
			}
		}
	}

	/**
	 * Init custom request fields map.
	 * @param config config
	 */
	private void initCustomRequestFieldsMap(ApiConfig config) {
		if (CollectionUtil.isNotEmpty(config.getCustomRequestFields())) {
			for (CustomField field : config.getCustomRequestFields()) {
				CustomField.Key key = CustomField.Key.create(field.getOwnerClassName(), field.getName());
				customReqFieldMap.put(key, field);
			}
		}
	}

	/**
	 * Init replace class map.
	 * @param config config
	 */
	private void initReplaceClassMap(ApiConfig config) {
		if (CollectionUtil.isNotEmpty(config.getApiObjectReplacements())) {
			for (ApiObjectReplacement replace : config.getApiObjectReplacements()) {
				replaceClassMap.put(replace.getClassName(), replace.getReplacementClassName());
			}
		}
	}

	/**
	 * Init constants.
	 * @param config config
	 */
	private void initConstants(ApiConfig config) {
		List<ApiConstant> apiConstants;
		if (CollectionUtil.isEmpty(config.getApiConstants())) {
			apiConstants = new ArrayList<>();
		}
		else {
			apiConstants = config.getApiConstants();
		}
		try {
			for (ApiConstant apiConstant : apiConstants) {
				Class<?> clzz = apiConstant.getConstantsClass();
				if (Objects.isNull(clzz)) {
					if (StringUtil.isEmpty(apiConstant.getConstantsClassName())) {
						throw new RuntimeException("Enum class name can't be null.");
					}
					clzz = Class.forName(apiConstant.getConstantsClassName());
				}
				constantsMap.putAll(JavaClassUtil.getFinalFieldValue(clzz));
			}
		}
		catch (ClassNotFoundException | IllegalAccessException e) {
			log.warning(e.getMessage());
		}
	}

	/**
	 * Check body advice.
	 * @param bodyAdvice body advice
	 */
	private void checkBodyAdvice(BodyAdvice bodyAdvice) {
		if (Objects.nonNull(bodyAdvice) && StringUtil.isNotEmpty(bodyAdvice.getClassName())) {
			if (Objects.nonNull(bodyAdvice.getWrapperClass())) {
				return;
			}
			try {
				Class.forName(bodyAdvice.getClassName());
			}
			catch (ClassNotFoundException e) {
				throw new RuntimeException(
						"Can't find class " + bodyAdvice.getClassName() + " for ResponseBodyAdvice.");
			}
		}
	}

	/**
	 * Set highlight style.
	 */
	private void setHighlightStyle() {
		String style = apiConfig.getStyle();
		if (HighLightJsConstants.HIGH_LIGHT_DEFAULT_STYLE.equals(style)) {
			// use local css file
			apiConfig.setHighlightStyleLink(HighLightJsConstants.HIGH_LIGHT_CSS_DEFAULT);
			return;
		}
		if (HighlightStyle.containsStyle(style)) {
			apiConfig.setHighlightStyleLink(String.format(HighLightJsConstants.HIGH_LIGHT_CSS_URL_FORMAT, style));
			return;
		}
		Random random = new Random();
		if (HighLightJsConstants.HIGH_LIGHT_CSS_RANDOM_LIGHT.equals(style)) {
			// Eliminate styles that do not match the template
			style = HighlightStyle.randomLight(random);
			if (HighlightStyle.containsStyle(style)) {
				apiConfig.setStyle(style);
				apiConfig.setHighlightStyleLink(String.format(HighLightJsConstants.HIGH_LIGHT_CSS_URL_FORMAT, style));
			}
			else {
				apiConfig.setStyle(null);
			}
		}
		else if (HighLightJsConstants.HIGH_LIGHT_CSS_RANDOM_DARK.equals(style)) {
			style = HighlightStyle.randomDark(random);
			if (HighLightJsConstants.HIGH_LIGHT_DEFAULT_STYLE.equals(style)) {
				apiConfig.setHighlightStyleLink(HighLightJsConstants.HIGH_LIGHT_CSS_DEFAULT);
			}
			else {
				apiConfig.setHighlightStyleLink(String.format(HighLightJsConstants.HIGH_LIGHT_CSS_URL_FORMAT, style));
			}
			apiConfig.setStyle(style);
		}
		else {
			// Eliminate styles that do not match the template
			apiConfig.setStyle(null);

		}
	}

	public JavaProjectBuilder getJavaProjectBuilder() {
		return javaProjectBuilder;
	}

	public SourceProject getSourceProject() {
		return sourceProject;
	}

	public Map<String, JavaClass> getClassFilesMap() {
		return classFilesMap;
	}

	public Map<CustomField.Key, CustomField> getCustomRespFieldMap() {
		return customRespFieldMap;
	}

	public Map<CustomField.Key, CustomField> getCustomReqFieldMap() {
		return customReqFieldMap;
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public ApiConfig getApiConfig() {
		return apiConfig;
	}

	public Map<String, String> getReplaceClassMap() {
		return replaceClassMap;
	}

	public Map<String, Class<? extends Enum<?>>> getEnumClassMap() {
		return enumClassMap;
	}

	public Map<String, String> getConstantsMap() {
		return constantsMap;
	}

}
