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
package com.github.hsindumas.stagger.utils;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.DefaultClassConstants;
import com.github.hsindumas.stagger.constants.DocAnnotationConstants;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.constants.DocTags;
import com.github.hsindumas.stagger.constants.DocValidatorAnnotationEnum;
import com.github.hsindumas.stagger.constants.JSRAnnotationConstants;
import com.github.hsindumas.stagger.constants.JavaTypeConstants;
import com.github.hsindumas.stagger.constants.ParamTypeConstants;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiDataDictionary;
import com.github.hsindumas.stagger.model.DocJavaField;
import com.github.hsindumas.stagger.model.torna.EnumInfo;
import com.github.hsindumas.stagger.model.torna.EnumInfoAndValues;
import com.github.hsindumas.stagger.model.torna.Item;
import com.power.common.model.EnumDictionary;
import com.power.common.util.CollectionUtil;
import com.power.common.util.EnumUtil;
import com.power.common.util.StringUtil;
import com.github.hsindumas.stagger.helper.JavaProjectBuilder;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Handle JavaClass
 *
 * @author yu 2019/12/21.
 */
public class JavaClassUtil {

	/**
	 * logger
	 */
	private static final Logger logger = Logger.getLogger(JavaClassUtil.class.getName());

	/**
	 * dot
	 */
	private static final String DOT = ".";

	/**
	 * private constructor
	 */
	private JavaClassUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Get fields
	 * @param cls1 The class metadata object
	 * @param counter Recursive counter
	 * @param addedFields added fields,Field deduplication
	 * @param classLoader classLoader
	 * @return list of JavaField
	 */
	public static List<DocJavaField> getFields(Object cls1, int counter, Map<String, DocJavaField> addedFields,
			ClassLoader classLoader) {
		Map<String, Object> actualJavaTypes = new HashMap<>(10);
		List<DocJavaField> fields = getFields(cls1, counter, addedFields, actualJavaTypes, classLoader);

		for (DocJavaField field : fields) {
			String genericCanonicalName = field.getTypeGenericCanonicalName();
			if (Objects.isNull(genericCanonicalName)) {
				continue;
			}
			Object actualJavaType = actualJavaTypes.get(genericCanonicalName);
			if (Objects.isNull(actualJavaType)) {
				continue;
			}
			String actualGenericCanonicalName = DocUtil.getTypeGenericCanonicalName(actualJavaType);
			String actualFullyQualifiedName = DocUtil.getTypeFullyQualifiedName(actualJavaType);
			if (StringUtil.isNotEmpty(actualGenericCanonicalName)) {
				field.setTypeGenericCanonicalName(
						genericCanonicalName.replace(genericCanonicalName, actualGenericCanonicalName));
			}
			if (StringUtil.isNotEmpty(actualFullyQualifiedName)) {
				field.setTypeFullyQualifiedName(
						field.getTypeFullyQualifiedName().replace(genericCanonicalName, actualFullyQualifiedName));
				field.setActualJavaType(actualFullyQualifiedName);
			}
		}
		return fields;
	}

	/**
	 * Get fields
	 * @param cls1 The class metadata object
	 * @param counter Recursive counter
	 * @param addedFields added fields,Field deduplication
	 * @param actualJavaTypes collected actualJavaTypes
	 * @return list of JavaField
	 */
	private static List<DocJavaField> getFields(Object cls1, int counter, Map<String, DocJavaField> addedFields,
			Map<String, Object> actualJavaTypes, ClassLoader classLoader) {
		List<DocJavaField> fieldList = new ArrayList<>();
		if (Objects.isNull(cls1)) {
			return fieldList;
		}
		// ignore enum class
		if (DocUtil.isClassEnum(cls1)) {
			return fieldList;
		}
		// ignore class in jdk
		String className = DocUtil.getTypeFullyQualifiedName(cls1);
		if (JavaClassValidateUtil.isJdkClass(className)) {
			return fieldList;
		}
		if (DocUtil.isClassInterface(cls1)) {
			List<?> methods = DocUtil.getClassMethods(cls1);
			for (Object javaMethod : methods) {
				String methodName = DocUtil.getMethodName(javaMethod);
				int paramSize = DocUtil.getMethodParameters(javaMethod).size();
				boolean enable = false;
				if (methodName.startsWith("get") && !"get".equals(methodName) && paramSize == 0) {
					methodName = StringUtil.firstToLowerCase(methodName.substring(3));
					enable = true;
				}
				else if (methodName.startsWith("is") && !"is".equals(methodName) && paramSize == 0) {
					methodName = StringUtil.firstToLowerCase(methodName.substring(2));
					enable = true;
				}
				if (!enable || addedFields.containsKey(methodName)) {
					continue;
				}
				String comment = DocUtil.getMethodComment(javaMethod);
				if (StringUtil.isEmpty(comment)) {
					comment = DocGlobalConstants.NO_COMMENTS_FOUND;
				}
				Object javaField = createSyntheticJavaField(DocUtil.getMethodReturnTypeCanonicalName(javaMethod),
						methodName);
				if (Objects.isNull(javaField)) {
					continue;
				}
				List<?> methodTags = toList(invokeMethod(javaMethod, "getTags"));
				Object fieldType = invokeMethod(javaField, "getType");
				String fieldTypeFullyQualifiedName = DocUtil.getTypeFullyQualifiedName(fieldType);
				DocJavaField docJavaField = DocJavaField.builder()
					.setDeclaringClassName(className)
					.setFieldName(methodName)
					.setJavaField(javaField)
					.setComment(comment)
					.setDocletTags(methodTags)
					.setAnnotations(DocUtil.getMethodAnnotations(javaMethod))
					.setTypeFullyQualifiedName(fieldTypeFullyQualifiedName)
					.setTypeGenericCanonicalName(getReturnGenericType(javaMethod, classLoader))
					.setTypeGenericFullyQualifiedName(DocUtil.getTypeGenericFullyQualifiedName(fieldType))
					.setTypeSimpleName(getClassSimpleName(fieldTypeFullyQualifiedName));
				addedFields.put(methodName, docJavaField);
			}
		}

		Object parentClass = DocUtil.getClassSuperJavaClass(cls1);
		if (Objects.nonNull(parentClass)) {
			getFields(parentClass, counter, addedFields, actualJavaTypes, classLoader);
		}

		List<?> implClasses = getImplementedInterfaces(cls1);
		for (Object type : implClasses) {
			getFields(type, counter, addedFields, actualJavaTypes, classLoader);
		}

		for (Map.Entry<String, ?> actualTypeEntry : getActualTypesMap(cls1).entrySet()) {
			actualJavaTypes.put(actualTypeEntry.getKey(), actualTypeEntry.getValue());
		}
		for (Object method : DocUtil.getClassMethods(cls1)) {
			String methodName = DocUtil.getMethodName(method);
			if (DocUtil.getMethodAnnotations(method).isEmpty()) {
				continue;
			}
			int paramSize = DocUtil.getMethodParameters(method).size();
			if (methodName.startsWith("get") && !"get".equals(methodName) && paramSize == 0) {
				methodName = StringUtil.firstToLowerCase(methodName.substring(3));
			}
			else if (methodName.startsWith("is") && !"is".equals(methodName) && paramSize == 0) {
				methodName = StringUtil.firstToLowerCase(methodName.substring(2));
			}
			if (addedFields.containsKey(methodName)) {
				String comment = DocUtil.getMethodComment(method);
				if (Objects.isNull(comment)) {
					comment = addedFields.get(methodName).getComment();
				}
				if (StringUtil.isEmpty(comment)) {
					comment = DocGlobalConstants.NO_COMMENTS_FOUND;
				}
				DocJavaField docJavaField = addedFields.get(methodName);
				docJavaField.setAnnotations(DocUtil.getMethodAnnotations(method));
				docJavaField.setComment(comment);
				docJavaField.setFieldName(methodName);
				docJavaField.setDeclaringClassName(className);
				addedFields.put(methodName, docJavaField);
			}
		}
		if (!DocUtil.isClassInterface(cls1)) {
			Map<String, String> recordComments = new HashMap<>(0);
			if (Boolean.TRUE.equals(invokeMethod(cls1, "isRecord"))) {
				recordComments = DocUtil.getRecordCommentsByTag(cls1, DocTags.PARAM);
			}
			for (Object javaField : DocUtil.getClassFields(cls1)) {
				String fieldName = DocUtil.getFieldName(javaField);
				String subTypeName = DocUtil.getFieldTypeFullyQualifiedName(javaField);

				if (DocUtil.isFieldStatic(javaField) || "this$0".equals(fieldName)
						|| JavaClassValidateUtil.isIgnoreFieldTypes(subTypeName)) {
					continue;
				}
				if (fieldName.startsWith("is") && ("boolean".equals(subTypeName))) {
					fieldName = StringUtil.firstToLowerCase(fieldName.substring(2));
				}
				long count = DocUtil.getFieldAnnotations(javaField)
					.stream()
					.filter(annotation -> DocAnnotationConstants.SHORT_JSON_IGNORE
						.equals(DocUtil.getAnnotationTypeSimpleName(annotation)))
					.count();
				if (count > 0) {
					addedFields.remove(fieldName);
					continue;
				}

				DocJavaField docJavaField = DocJavaField.builder();
				boolean typeChecked = false;
				Object fieldType = invokeMethod(javaField, "getType");
				String gicName = DocUtil.getTypeGenericCanonicalName(fieldType);

				String actualType = null;
				if (JavaClassValidateUtil.isCollection(subTypeName) && !JavaClassValidateUtil.isCollection(gicName)) {
					String[] gNameArr = DocClassUtil.getSimpleGicName(gicName);
					actualType = JavaClassUtil.getClassSimpleName(gNameArr[0]);
					docJavaField.setArray(true);
					typeChecked = true;
				}
				if (JavaClassValidateUtil.isPrimitive(subTypeName) && !typeChecked) {
					docJavaField.setPrimitive(true);
					typeChecked = true;
				}
				if (JavaClassValidateUtil.isFile(subTypeName) && !typeChecked) {
					docJavaField.setFile(true);
					typeChecked = true;
				}
				if (DocUtil.isClassEnum(fieldType) && !typeChecked) {
					docJavaField.setEnum(true);
				}
				String comment = DocUtil.getFieldComment(javaField);
				if (Boolean.TRUE.equals(invokeMethod(cls1, "isRecord"))) {
					comment = recordComments.get(fieldName);
				}
				if (Objects.isNull(comment)) {
					comment = DocGlobalConstants.NO_COMMENTS_FOUND;
				}
				// Getting the Original Defined Type of Field
				if (!docJavaField.isFile() || !docJavaField.isEnum() || !docJavaField.isPrimitive()
						|| JavaTypeConstants.JAVA_OBJECT_FULLY.equals(gicName)) {
					String genericFieldTypeName = getFieldGenericType(javaField, classLoader);
					if (StringUtil.isNotEmpty(genericFieldTypeName)) {
						gicName = genericFieldTypeName;
					}
				}
				docJavaField.setComment(comment)
					.setJavaField(javaField)
					.setTypeFullyQualifiedName(subTypeName)
					.setTypeGenericCanonicalName(gicName)
					.setTypeGenericFullyQualifiedName(DocUtil.getFieldGenericFullyQualifiedName(javaField))
					.setActualJavaType(actualType)
					.setAnnotations(DocUtil.getFieldAnnotations(javaField))
					.setFieldName(fieldName)
					.setDeclaringClassName(className)
					.setTypeSimpleName(getClassSimpleName(subTypeName));
				if (addedFields.containsKey(fieldName)) {
					addedFields.remove(fieldName);
					addedFields.put(fieldName, docJavaField);
					continue;
				}
				addedFields.put(fieldName, docJavaField);
			}
		}
		List<DocJavaField> parentFieldList = addedFields.values()
			.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		fieldList.addAll(parentFieldList);

		return fieldList;
	}

	/**
	 * Get Common for methods with the same signature from interfaces
	 * @param cls cls
	 * @param method method
	 * @return common
	 */
	public static String getSameSignatureMethodCommonFromInterface(Object cls, Object method) {
		if (Objects.isNull(cls) || Objects.isNull(method)) {
			return null;
		}
		Object methodsBySignature = invokeMethod(cls, "getMethodsBySignature", DocUtil.getMethodName(method),
				DocUtil.getMethodParameterTypes(method), true, DocUtil.isMethodVarArgs(method));
		if (!(methodsBySignature instanceof List)) {
			return null;
		}
		for (Object sameSignatureMethod : (List<?>) methodsBySignature) {
			if (sameSignatureMethod == method) {
				continue;
			}
			Object declaringClass = DocUtil.getMethodDeclaringClass(sameSignatureMethod);
			if (Objects.isNull(declaringClass) || !DocUtil.isClassInterface(declaringClass)) {
				continue;
			}
			String comment = DocUtil.getMethodComment(sameSignatureMethod);
			if (StringUtil.isNotEmpty(comment)) {
				return comment;
			}
		}
		return null;
	}

	/**
	 * Get the value of an enum from parser-agnostic class metadata.
	 * @param javaClass class metadata object
	 * @param builder builder
	 * @param jsonEnum enum is json or not
	 * @return enum value
	 */
	public static Object getEnumValue(Object javaClass, ProjectDocConfigBuilder builder, boolean jsonEnum) {
		EnumInfoAndValues enumInfoAndValue = getEnumInfoAndValue(javaClass, builder, jsonEnum);
		if (enumInfoAndValue == null) {
			return null;
		}
		return enumInfoAndValue.getValue();
	}

	/**
	 * Object-metadata overload for enum constant value resolution.
	 * @param javaClass enum class metadata object
	 * @param builder configuration builder
	 * @param enumConstant enum constant metadata object
	 * @return resolved enum value or null when unavailable
	 */
	public static Object getDefaultEnumValue(Object javaClass, ProjectDocConfigBuilder builder, Object enumConstant) {
		Object enumValue = getEnumValueWithJsonValue(javaClass, builder, enumConstant);
		if (enumValue != null) {
			return enumValue;
		}
		return processDefaultEnumFields(enumConstant, builder);
	}

	/**
	 * Object overload for enum JsonValue extraction.
	 * @param javaClass enum class metadata object
	 * @param builder configuration builder
	 * @param enumConstant enum constant metadata object
	 * @return resolved enum value or null
	 */
	public static Object getEnumValueWithJsonValue(Object javaClass, ProjectDocConfigBuilder builder,
			Object enumConstant) {
		String enumConstantName = DocUtil.getFieldName(enumConstant);
		if (StringUtil.isEmpty(enumConstantName)) {
			return null;
		}
		String methodName = findMethodWithJsonValue(javaClass);
		if (Objects.nonNull(methodName)) {
			Class<?> enumClass = loadEnumClass(javaClass, builder);
			if (Objects.nonNull(enumClass)) {
				return EnumUtil.getFieldValueByMethod(enumClass, methodName, enumConstantName);
			}
			return null;
		}

		Optional<Object> fieldWithJsonValue = findFieldWithJsonValue(javaClass);
		if (fieldWithJsonValue.isPresent()) {
			Class<?> enumClass = loadEnumClass(javaClass, builder);
			if (Objects.nonNull(enumClass)) {
				return EnumUtil.getFieldValue(enumClass, DocUtil.getFieldName(fieldWithJsonValue.get()),
						enumConstantName);
			}
		}
		return null;
	}

	/**
	 * Loads enum class metadata in an implementation-agnostic way.
	 * @param javaClass enum class metadata object
	 * @param builder configuration builder
	 * @return loaded enum class or null when unavailable
	 */
	private static Class<?> loadEnumClass(Object javaClass, ProjectDocConfigBuilder builder) {
		ApiConfig apiConfig = builder.getApiConfig();
		ClassLoader classLoader = apiConfig.getClassLoader();
		String binaryName = DocUtil.getTypeBinaryName(javaClass);
		if (StringUtil.isEmpty(binaryName)) {
			return null;
		}
		try {
			if (Objects.nonNull(classLoader)) {
				return classLoader.loadClass(binaryName);
			}
			else {
				return Class.forName(binaryName);
			}
		}
		catch (ClassNotFoundException e) {
			logger.warning(e.getMessage());
			return null;
		}
	}

	/**
	 * Finds a method marked with {@code JsonValue} annotation from class metadata.
	 * @param javaClass class metadata object
	 * @return method name if found, otherwise null
	 */
	private static String findMethodWithJsonValue(Object javaClass) {
		for (Object method : DocUtil.getClassMethods(javaClass)) {
			for (Object annotation : DocUtil.getMethodAnnotations(method)) {
				String annotationName = DocUtil.getAnnotationTypeValue(annotation);
				if (DocAnnotationConstants.JSON_VALUE.equals(annotationName)) {
					Object property = DocUtil.getAnnotationProperty(annotation, DocAnnotationConstants.VALUE_PROP);
					if (property == null || isTrueAnnotationProperty(property)) {
						return DocUtil.getMethodName(method);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Finds a field marked with {@code JsonValue} annotation from class metadata.
	 * @param javaClass class metadata object
	 * @return first matching field metadata when present
	 */
	private static Optional<Object> findFieldWithJsonValue(Object javaClass) {
		return DocUtil.getClassFields(javaClass)
			.stream()
			.filter(field -> DocUtil.getFieldAnnotations(field).stream().anyMatch(annotation -> {
				if (DocAnnotationConstants.JSON_VALUE.equals(DocUtil.getAnnotationTypeValue(annotation))) {
					Object property = DocUtil.getAnnotationProperty(annotation, DocAnnotationConstants.VALUE_PROP);
					return property == null || isTrueAnnotationProperty(property);
				}
				return false;
			}))
			.findFirst()
			.map(field -> (Object) field);
	}

	private static boolean isTrueAnnotationProperty(Object property) {
		String value = DocUtil.resolveAnnotationValue(Thread.currentThread().getContextClassLoader(), property);
		return Boolean.parseBoolean(StringUtil.removeQuotes(value));
	}

	/**
	 * Handles default enum field processing from field metadata objects.
	 * @param javaField enum constant metadata object
	 * @param builder configuration builder
	 * @return default enum value derived from name/initializer
	 */
	private static Object processDefaultEnumFields(Object javaField, ProjectDocConfigBuilder builder) {
		ApiConfig apiConfig = builder.getApiConfig();
		String fieldName = DocUtil.getFieldName(javaField);
		if (!apiConfig.isEnumConvertor()) {
			return fieldName;
		}

		String initializationExpression = DocUtil.getFieldInitializationExpression(javaField);
		if (StringUtils.isBlank(initializationExpression)) {
			return fieldName;
		}

		String[] result = initializationExpression.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
		if (result.length == 0) {
			return fieldName;
		}

		String str = result[0];
		return StringUtils.isNumeric(str) ? Integer.parseInt(str) : str;
	}

	/**
	 * Object-metadata overload for enum parameter rendering.
	 * @param javaClass enum class metadata object
	 * @return rendered enum values
	 */
	public static String getEnumParams(Object javaClass) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Object enumConstant : DocUtil.getClassEnumConstants(javaClass)) {
			if (stringBuilder.length() > 0) {
				stringBuilder.append(", ");
			}
			stringBuilder.append(DocUtil.getFieldName(enumConstant));
			String exception = DocUtil.getFieldInitializationExpression(enumConstant);
			if (StringUtil.isNotEmpty(exception)) {
				stringBuilder.append("(").append(exception).append(")").append("<br/>");
			}
		}
		return stringBuilder.toString();
	}

	/**
	 * Object-metadata overload for enum values.
	 * @param javaClass enum class metadata object
	 * @return enum values
	 */
	public static List<String> getEnumValues(Object javaClass) {
		List<String> enums = new ArrayList<>();
		for (Object enumConstant : DocUtil.getClassEnumConstants(javaClass)) {
			String enumName = DocUtil.getFieldName(enumConstant);
			if (StringUtil.isNotEmpty(enumName)) {
				enums.add(enumName);
			}
		}
		return enums;
	}

	/**
	 * Retrieves the associated enum class for a given Java field.
	 * <p>
	 * This method aims to obtain the enum type (JavaClass) associated with the provided
	 * Java field object (JavaField). If the field does not associate with an enum type or
	 * if there is no appropriate @see tag providing enum information, it returns null.
	 * The splitting logic is implemented to handle the case where the @see tag might
	 * contain additional description information after the class name (e.g., `@see
	 * GenderEnum descriptionGender`).
	 * @param javaField The Java field object to inspect
	 * @param builder The builder used to retrieve project documentation configuration
	 * @return The enum class object associated with the field, or null if not found
	 */
	public static Object getSeeEnum(Object javaField, ProjectDocConfigBuilder builder) {
		if (Objects.isNull(javaField)) {
			return null;
		}

		// If the field type is already an enum, return it directly
		Object javaClass = invokeMethod(javaField, "getType");
		if (DocUtil.isClassEnum(javaClass)) {
			return javaClass;
		}

		// Process the @see tag if present
		Object see = invokeMethod(javaField, "getTagByName", DocTags.SEE);
		if (Objects.isNull(see)) {
			return null;
		}

		// Extract the enum class name from @see tag
		String value = extractEnumClassName(DocUtil.getDocletTagValue(see));
		if (value == null) {
			return null;
		}

		// Resolve the class name
		// (handles imports, nested classes, and fully qualified names)
		Object declaringClass = invokeMethod(javaField, "getDeclaringClass");
		value = resolveClassName(value, declaringClass);

		// Check if the value corresponds to a valid class name
		if (!JavaClassValidateUtil.isClassName(value)) {
			// Fixed #995: If the value is not a valid class name,
			// attempt to resolve it by adding the current package name prefix.
			value = DocUtil.getClassPackageName(declaringClass) + DOT + value;
			// Check again
			if (!JavaClassValidateUtil.isClassName(value)) {
				return null;
			}
		}

		// Retrieve the JavaClass by name and check if it is an enum
		Object enumClass = builder.getClassByName(value);
		if (Objects.isNull(enumClass)) {
			// Fixed #995: If the class cannot be found, attempt to resolve the class name
			// by adding the package prefix of the declaring class. This approach is used
			// when the enum is defined in the same package as the declaring class.
			enumClass = builder.getClassByName(DocUtil.getClassPackageName(declaringClass) + DOT + value);
		}
		if (DocUtil.isClassEnum(enumClass)) {
			return enumClass;
		}
		return null;
	}

	/**
	 * Extracts the enum class name from the @see tag value. Handles cases where the @see
	 * tag contains additional description info after the class name.<br>
	 * e.g. {@code @see TestEnum test}
	 * @param seeValue The value of the @see tag
	 * @return The extracted enum class name or null if not found
	 */
	private static String extractEnumClassName(String seeValue) {
		if (seeValue == null || seeValue.trim().isEmpty()) {
			return null;
		}
		// Split the value to extract the class name (first part before any whitespace)
		return seeValue.trim().split("\\s+")[0];
	}

	/**
	 * Resolves the class name by checking imports and nested classes. Handles both fully
	 * qualified class names and short class names (e.g., class names without package
	 * information).
	 * <p>
	 * This method first checks if the given class name is fully qualified (i.e., contains
	 * a dot). If it's not fully qualified, it will attempt to resolve it using imports
	 * and nested classes. If the class name is fully qualified, it will check if it can
	 * be resolved using imports or nested classes. The method handles cases where the
	 * class name is not directly available or when it's a nested class.
	 * @param value The class name to resolve. This can either be a fully qualified class
	 * name (e.g., "com.example.MyClass") or a short class name (e.g., "MyClass").
	 * @param declaringClass The declaring class that may contain nested classes or
	 * imports that could be used to resolve the class name.
	 * @return The resolved class name, which may be a fully qualified name or the
	 * original value if it cannot be resolved. If a match is found in imports or nested
	 * classes, the resolved class name will be returned; otherwise, the original value is
	 * returned.
	 */
	private static String resolveClassName(String value, Object declaringClass) {
		List<String> imports = getClassImports(declaringClass);

		// If it's not a fully qualified class name
		// try resolving from imports or nested classes
		if (Objects.isNull(value) || !value.contains(DOT)) {
			value = resolveFromImports(value, imports, declaringClass);
		}
		// Handle fully qualified names (with a dot) or inner classes
		else {
			value = resolveFullyQualifiedClass(value, declaringClass, imports);
		}
		return value;
	}

	/**
	 * Resolves the class name from imports or nested classes for short class names.
	 * <p>
	 * This method looks for the class name in the list of imports of the declaring class.
	 * If the class name is not found in the imports, it checks if the class is a nested
	 * class of the declaring class. If a match is found, the full class name is returned;
	 * otherwise, the original short class name is returned.
	 * @param value The short class name (e.g., "MyClass") to resolve.
	 * @param imports A list of import statements in the declaring class that may contain
	 * the class name.
	 * @param declaringClass The declaring class that may contain nested classes and
	 * imports that could be used to resolve the class name.
	 * @return The fully qualified class name if found in imports or as a nested class,
	 * otherwise the original short class name.
	 */
	private static String resolveFromImports(String value, List<String> imports, Object declaringClass) {
		Optional<String> importClass = imports.stream().filter(i -> i.endsWith(value)).findFirst();

		if (importClass.isPresent()) {
			return importClass.get();
		}

		// Check for nested class if not found in imports
		for (Object nestedClass : DocUtil.getClassNestedClasses(declaringClass)) {
			String nestedClassName = DocUtil.getClassCanonicalName(nestedClass);
			if (nestedClassName.endsWith(DOT + value)) {
				return nestedClassName;
			}
		}

		// Return original if no match found
		return value;
	}

	/**
	 * Resolves the class name for fully qualified class names or nested classes.
	 * <p>
	 * This method processes fully qualified class names (i.e., names with package
	 * information) by checking if the class is present in the imports list of the
	 * declaring class. If it is not found in the imports, it checks if the class is a
	 * nested class of the declaring class. The method can also handle cases where the
	 * class name contains inner class references (e.g., "OuterClass$InnerClass").
	 * @param value The fully qualified class name to resolve (e.g., "com.example.MyClass"
	 * or "OuterClass$InnerClass").
	 * @param declaringClass The declaring class that may contain nested classes and
	 * imports that could be used to resolve the class name.
	 * @param imports A list of import statements in the declaring class that may contain
	 * the class name.
	 * @return The fully qualified class name if found in imports or as a nested class,
	 * otherwise the original class name is returned.
	 */
	private static String resolveFullyQualifiedClass(String value, Object declaringClass, List<String> imports) {
		String[] parts = value.split("\\.", 2);
		String classNamePart = parts[0];
		String restPart = (parts.length > 1) ? parts[1] : "";

		// Try to resolve the class from imports
		Optional<String> importClass = imports.stream().filter(i -> i.endsWith(DOT + classNamePart)).findFirst();

		if (importClass.isPresent()) {
			return importClass.get() + (restPart.isEmpty() ? "" : DOT + restPart);
		}

		// If not found in imports, check if it's a nested class
		for (Object nestedClass : DocUtil.getClassNestedClasses(declaringClass)) {
			if (DocUtil.getClassSimpleName(nestedClass).equals(classNamePart)) {
				return DocUtil.getClassCanonicalName(declaringClass) + DOT + value;
			}
		}

		// Return original if no match found
		return value;
	}

	@SuppressWarnings("unchecked")
	private static List<String> getClassImports(Object declaringClass) {
		if (Objects.isNull(declaringClass)) {
			return Collections.emptyList();
		}
		Object source = invokeMethod(declaringClass, "getSource");
		if (Objects.isNull(source)) {
			return Collections.emptyList();
		}
		Object imports = invokeMethod(source, "getImports");
		if (imports instanceof List) {
			return (List<String>) imports;
		}
		return Collections.emptyList();
	}

	private static Object invokeMethod(Object target, String methodName, Object... args) {
		if (Objects.isNull(target) || StringUtil.isEmpty(methodName)) {
			return null;
		}
		try {
			if (Objects.isNull(args) || args.length == 0) {
				Method method = target.getClass().getMethod(methodName);
				return method.invoke(target);
			}
			Method method = findCompatibleMethod(target.getClass(), methodName, args);
			if (Objects.isNull(method)) {
				return null;
			}
			return method.invoke(target, args);
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			logger.fine(ignored.getMessage());
		}
		return null;
	}

	private static Method findCompatibleMethod(Class<?> type, String methodName, Object[] args) {
		for (Method method : type.getMethods()) {
			if (!methodName.equals(method.getName()) || method.getParameterCount() != args.length) {
				continue;
			}
			Class<?>[] parameterTypes = method.getParameterTypes();
			boolean compatible = true;
			for (int i = 0; i < parameterTypes.length; i++) {
				if (!isCompatible(parameterTypes[i], args[i])) {
					compatible = false;
					break;
				}
			}
			if (compatible) {
				return method;
			}
		}
		return null;
	}

	private static boolean isCompatible(Class<?> parameterType, Object arg) {
		if (Objects.isNull(arg)) {
			return !parameterType.isPrimitive();
		}
		Class<?> argType = arg.getClass();
		if (parameterType.isPrimitive()) {
			Class<?> wrapperType = primitiveToWrapper(parameterType);
			return Objects.nonNull(wrapperType) && wrapperType.isAssignableFrom(argType);
		}
		return parameterType.isAssignableFrom(argType);
	}

	private static Class<?> primitiveToWrapper(Class<?> primitiveType) {
		if (boolean.class == primitiveType) {
			return Boolean.class;
		}
		if (byte.class == primitiveType) {
			return Byte.class;
		}
		if (char.class == primitiveType) {
			return Character.class;
		}
		if (short.class == primitiveType) {
			return Short.class;
		}
		if (int.class == primitiveType) {
			return Integer.class;
		}
		if (long.class == primitiveType) {
			return Long.class;
		}
		if (float.class == primitiveType) {
			return Float.class;
		}
		if (double.class == primitiveType) {
			return Double.class;
		}
		return null;
	}

	/**
	 * Gets enum information from class metadata.
	 * @param javaClass class metadata object
	 * @param builder configuration builder
	 * @return enum info or null when unavailable
	 */
	public static EnumInfo getEnumInfo(Object javaClass, ProjectDocConfigBuilder builder) {
		if (Objects.isNull(javaClass) || !DocUtil.isClassEnum(javaClass)) {
			return null;
		}
		if (Objects.nonNull(DocUtil.getClassTagByName(javaClass, DocTags.IGNORE))) {
			return null;
		}

		EnumInfo enumInfo = new EnumInfo();
		enumInfo.setName(DocUtil.getClassComment(javaClass));
		Object apiNoteTag = DocUtil.getClassTagByName(javaClass, DocTags.API_NOTE);
		enumInfo.setDescription(DocUtil.getEscapeAndCleanComment(
				Optional.ofNullable(apiNoteTag).map(DocUtil::getDocletTagValue).orElse(StringUtil.EMPTY)));

		ApiConfig apiConfig = builder.getApiConfig();
		ApiDataDictionary dataDictionary = apiConfig.getDataDictionary(DocUtil.getTypeBinaryName(javaClass));
		enumInfo.setItems(getEnumItemList(javaClass, dataDictionary, builder));
		if (Objects.nonNull(dataDictionary) && StringUtils.isNotEmpty(dataDictionary.getTitle())) {
			enumInfo.setName(dataDictionary.getTitle());
		}
		return enumInfo;
	}

	/**
	 * Get annotation simpleName
	 * @param annotationName annotationName
	 * @return String
	 */
	public static String getAnnotationSimpleName(String annotationName) {
		return getClassSimpleName(annotationName);
	}

	/**
	 * Get className
	 * @param className className
	 * @return String
	 */
	public static String getClassSimpleName(String className) {
		if (className.contains(DOT)) {
			if (className.contains("<")) {
				className = className.substring(0, className.indexOf("<"));
			}
			int index = className.lastIndexOf(DOT);
			className = className.substring(index + 1);
		}
		if (className.contains("[")) {
			int index = className.indexOf("[");
			className = className.substring(0, index);
		}
		return className;
	}

	/**
	 * get Actual type list
	 * @param javaType type metadata object
	 * @return JavaType list
	 */
	public static List<?> getActualTypes(Object javaType) {
		if (Objects.isNull(javaType)) {
			return Collections.emptyList();
		}
		String typeName = DocUtil.getTypeGenericFullyQualifiedName(javaType);
		if (typeName.contains("<")) {
			return getParameterizedTypeArguments(javaType);
		}
		return Collections.emptyList();
	}

	/**
	 * get Actual type map
	 * @param javaClass class metadata object
	 * @return Map
	 */
	public static Map<String, ?> getActualTypesMap(Object javaClass) {
		Map<String, Object> genericMap = new HashMap<>(10);
		List<?> variables = getTypeParameters(javaClass);
		if (variables.isEmpty()) {
			return genericMap;
		}
		List<?> javaTypes = getActualTypes(javaClass);
		for (int i = 0; i < variables.size() && i < javaTypes.size(); i++) {
			String typeParameterName = getTypeParameterName(variables.get(i));
			if (StringUtil.isNotEmpty(typeParameterName)) {
				genericMap.put(typeParameterName, javaTypes.get(i));
			}
		}
		return genericMap;
	}

	/**
	 * Obtain the validation group classes from controller method parameter annotations.
	 * <p>
	 * This method processes a list of annotations associated with a controller method
	 * parameter to identify validation groups. It checks if any of the annotations are
	 * validation-related and retrieves their group classes. If the @Validated annotation
	 * is present and no group classes are specified, the default group class is added.
	 * The @Valid annotation is treated as equivalent to the default group since it does
	 * not have group parameters.
	 * @param annotations the list of annotations on the controller method parameter.
	 * @param builder the JavaProjectBuilder instance used to resolve annotation values.
	 * @return a set of group class names identified from the annotations, or an empty set
	 * if none are found.
	 */
	public static Set<String> getParamGroupJavaClass(List<?> annotations, JavaProjectBuilder builder) {
		if (CollectionUtil.isEmpty(annotations)) {
			return new HashSet<>(0);
		}
		Set<String> javaClassList = new HashSet<>();
		for (Object javaAnnotation : annotations) {
			List<Object> annotationValueList = getValidatedAnnotationValues(javaAnnotation);
			addGroupClass(annotationValueList, javaClassList, builder);
			// When using @Validated and group class is empty, add the Default group
			// class;
			// Note: @Valid does not have group parameters and is equivalent to the
			// default group.
			String simpleAnnotationName = DocUtil.getAnnotationTypeValue(javaAnnotation);
			if (javaClassList.isEmpty() && (JSRAnnotationConstants.VALIDATED.equals(simpleAnnotationName)
					|| JSRAnnotationConstants.VALID.equals(simpleAnnotationName))) {
				javaClassList.addAll(DefaultClassConstants.DEFAULT_CLASSES);
			}
		}
		return javaClassList;
	}

	/**
	 * Obtain validation group classes from controller method parameter annotations.
	 * @param annotations the list of annotations on the controller method parameter
	 * @param builder the ProjectDocConfigBuilder used to resolve class metadata
	 * @return a set of group class names identified from the annotations
	 */
	public static Set<String> getParamGroupJavaClass(List<?> annotations, ProjectDocConfigBuilder builder) {
		if (CollectionUtil.isEmpty(annotations)) {
			return new HashSet<>(0);
		}
		Set<String> javaClassList = new HashSet<>();
		for (Object javaAnnotation : annotations) {
			List<Object> annotationValueList = getValidatedAnnotationValues(javaAnnotation);
			addGroupClass(annotationValueList, javaClassList, builder);
			String simpleAnnotationName = DocUtil.getAnnotationTypeValue(javaAnnotation);
			if (javaClassList.isEmpty() && (JSRAnnotationConstants.VALIDATED.equals(simpleAnnotationName)
					|| JSRAnnotationConstants.VALID.equals(simpleAnnotationName))) {
				javaClassList.addAll(DefaultClassConstants.DEFAULT_CLASSES);
			}
		}
		return javaClassList;
	}

	/**
	 * Obtain Validate Group classes
	 * @param javaAnnotation the annotation of controller method param
	 * @return the group annotation value
	 */
	public static Set<String> getParamGroupJavaClass(Object javaAnnotation) {
		if (Objects.isNull(javaAnnotation)) {
			return new HashSet<>(0);
		}
		Set<String> javaClassList = new HashSet<>();
		List<Object> annotationValueList = getValidatedAnnotationValues(javaAnnotation);
		addGroupClass(annotationValueList, javaClassList);
		String simpleAnnotationName = DocUtil.getAnnotationTypeValue(javaAnnotation);
		// add default group
		if (javaClassList.isEmpty() && JavaClassValidateUtil.isJSR303Required(simpleAnnotationName)) {
			// fix bug #819 https://github.com/HsinDumas/stagger/issues/819
			javaClassList.addAll(DefaultClassConstants.DEFAULT_CLASSES);
		}
		return javaClassList;
	}

	/**
	 * Retrieves Javadoc tag values for a specified tag name using parser-agnostic class
	 * metadata.
	 * @param cls The class metadata object to inspect.
	 * @param tagName The name of the tag to search for.
	 * @param checkComments Indicates whether to validate the presence of comments for
	 * empty tag values.
	 * @return A comma-separated string of all values found for the specified tag, or an
	 * empty string if the tag name is empty.
	 */
	public static String getClassTagsValue(final Object cls, final String tagName, boolean checkComments) {
		if (StringUtil.isEmpty(tagName) || Objects.isNull(cls)) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		List<?> tags = DocUtil.getClassTags(cls);
		String className = DocUtil.getClassSimpleName(cls);
		for (Object tag : tags) {
			String tagSimpleName = DocUtil.getDocletTagName(tag);
			if (!tagName.equals(tagSimpleName)) {
				continue;
			}
			String value = DocUtil.getDocletTagValue(tag);
			if (StringUtil.isEmpty(value) && checkComments) {
				throw new RuntimeException("ERROR: #" + className + "() - bad @" + tagName + " Javadoc tag usage from "
						+ className + ", must be add comment if you use it.");
			}
			if (result.length() > 0) {
				result.append(",");
			}
			result.append(value);
		}
		return result.toString();
	}

	/**
	 * Get Map of final field and value
	 * @param clazz Java class
	 * @return Map
	 * @throws IllegalAccessException IllegalAccessException
	 */
	public static Map<String, String> getFinalFieldValue(Class<?> clazz) throws IllegalAccessException {
		String className = getClassSimpleName(clazz.getName());
		Field[] fields = clazz.getDeclaredFields();
		Map<String, String> constants = new HashMap<>(16);
		for (Field field : fields) {
			if (Modifier.isPrivate(field.getModifiers())) {
				continue;
			}
			if (Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
				String name = field.getName();
				constants.put(className + DOT + name, String.valueOf(field.get(null)));
			}
		}
		return constants;
	}

	/**
	 * Add group class
	 * @param annotationValueList annotation value list
	 * @param javaClassList java class list
	 */
	private static void addGroupClass(List<Object> annotationValueList, Set<String> javaClassList) {
		if (CollectionUtil.isEmpty(annotationValueList)) {
			return;
		}
		for (Object annotationValue : annotationValueList) {
			Object annotationValueType = extractTypeFromAnnotationValue(annotationValue);
			if (Objects.isNull(annotationValueType)) {
				continue;
			}
			String genericFullyQualifiedName = DocUtil.getTypeGenericFullyQualifiedName(annotationValueType);
			if (StringUtil.isNotEmpty(genericFullyQualifiedName)) {
				javaClassList.add(genericFullyQualifiedName);
			}
		}
	}

	/**
	 * Add group class
	 * @param annotationValueList annotation value list
	 * @param javaClassList java class list
	 * @param builder JavaProjectBuilder
	 */
	private static void addGroupClass(List<Object> annotationValueList, Set<String> javaClassList,
			JavaProjectBuilder builder) {
		if (CollectionUtil.isEmpty(annotationValueList)) {
			return;
		}
		for (Object annotationValue : annotationValueList) {
			Object annotationValueType = extractTypeFromAnnotationValue(annotationValue);
			if (Objects.isNull(annotationValueType)) {
				continue;
			}
			String genericCanonicalName = DocUtil.getTypeGenericFullyQualifiedName(annotationValueType);
			Object classByName = builder.getClassByName(genericCanonicalName);
			recursionGetAllValidInterface(classByName, javaClassList, builder);
			javaClassList.add(genericCanonicalName);
		}
	}

	/**
	 * Add group classes from annotation values using ProjectDocConfigBuilder lookup.
	 * @param annotationValueList annotation value list
	 * @param javaClassList java class list
	 * @param builder project doc configuration builder
	 */
	private static void addGroupClass(List<Object> annotationValueList, Set<String> javaClassList,
			ProjectDocConfigBuilder builder) {
		if (CollectionUtil.isEmpty(annotationValueList)) {
			return;
		}
		for (Object annotationValue : annotationValueList) {
			Object annotationValueType = extractTypeFromAnnotationValue(annotationValue);
			if (Objects.isNull(annotationValueType)) {
				continue;
			}
			String genericCanonicalName = DocUtil.getTypeGenericFullyQualifiedName(annotationValueType);
			Object classByName = builder.getClassByName(genericCanonicalName);
			recursionGetAllValidInterface(classByName, javaClassList, builder);
			javaClassList.add(genericCanonicalName);
		}
	}

	/**
	 * Recursively adds all valid interfaces to the provided set.
	 * @param classByName The Java class to start the recursion from.
	 * @param javaClassSet The set to which valid interfaces will be added.
	 * @param builder The JavaProjectBuilder instance used for class lookup.
	 */
	private static void recursionGetAllValidInterface(Object classByName, Set<String> javaClassSet,
			JavaProjectBuilder builder) {
		if (Objects.isNull(classByName)) {
			return;
		}
		List<?> anImplements = getImplementedInterfaces(classByName);
		if (CollectionUtil.isEmpty(anImplements)) {
			return;
		}
		for (Object javaType : anImplements) {
			String genericFullyQualifiedName = DocUtil.getTypeGenericFullyQualifiedName(javaType);
			javaClassSet.add(genericFullyQualifiedName);
			// skip default group
			if (DefaultClassConstants.DEFAULT_CLASSES.contains(genericFullyQualifiedName)) {
				continue;
			}
			Object implementJavaClass = builder.getClassByName(genericFullyQualifiedName);
			recursionGetAllValidInterface(implementJavaClass, javaClassSet, builder);
		}
	}

	/**
	 * Recursively adds all valid interfaces with ProjectDocConfigBuilder lookup.
	 * @param classByName source class metadata object
	 * @param javaClassSet target set for collected interface names
	 * @param builder project doc configuration builder
	 */
	private static void recursionGetAllValidInterface(Object classByName, Set<String> javaClassSet,
			ProjectDocConfigBuilder builder) {
		if (Objects.isNull(classByName)) {
			return;
		}
		List<?> anImplements = getImplementedInterfaces(classByName);
		if (CollectionUtil.isEmpty(anImplements)) {
			return;
		}
		for (Object javaType : anImplements) {
			String genericFullyQualifiedName = DocUtil.getTypeGenericFullyQualifiedName(javaType);
			javaClassSet.add(genericFullyQualifiedName);
			if (DefaultClassConstants.DEFAULT_CLASSES.contains(genericFullyQualifiedName)) {
				continue;
			}
			Object implementJavaClass = builder.getClassByName(genericFullyQualifiedName);
			recursionGetAllValidInterface(implementJavaClass, javaClassSet, builder);
		}
	}

	/**
	 * Get implemented interfaces for a class metadata object.
	 * @param javaClass source class metadata object
	 * @return implemented interfaces, empty when unavailable
	 */
	public static List<?> getImplementedInterfaces(Object javaClass) {
		List<?> implementedInterfaces = invokeInterfaceAccessor(javaClass, "getImplements");
		if (CollectionUtil.isEmpty(implementedInterfaces)) {
			return Collections.emptyList();
		}
		return implementedInterfaces;
	}

	/**
	 * Get interface classes declared by a class metadata object.
	 * @param javaClass source class metadata object
	 * @return interface classes, empty when unavailable
	 */
	public static List<?> getInterfaceClasses(Object javaClass) {
		List<?> interfaceClasses = invokeInterfaceAccessor(javaClass, "getInterfaces");
		if (CollectionUtil.isEmpty(interfaceClasses)) {
			return Collections.emptyList();
		}
		return interfaceClasses;
	}

	@SuppressWarnings("unchecked")
	private static List<?> invokeInterfaceAccessor(Object javaClass, String accessorName) {
		if (Objects.isNull(javaClass)) {
			return Collections.emptyList();
		}
		try {
			Object result = javaClass.getClass().getMethod(accessorName).invoke(javaClass);
			if (result instanceof List) {
				return (List<?>) result;
			}
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			logger.fine(ignored.getMessage());
		}
		return Collections.emptyList();
	}

	private static Object createSyntheticJavaField(String typeName, String fieldName) {
		try {
			String javaClassImplName = "com.thoughtworks.q" + "dox.model.impl.DefaultJavaClass";
			Class<?> javaClassImplType = Class.forName(javaClassImplName);
			Object owner = javaClassImplType.getConstructor(String.class).newInstance(typeName);

			String javaFieldImplName = "com.thoughtworks.q" + "dox.model.impl.DefaultJavaField";
			String javaClassTypeName = "com.thoughtworks.q" + "dox.model.JavaClass";
			Class<?> javaClassType = Class.forName(javaClassTypeName);
			Class<?> javaFieldImplType = Class.forName(javaFieldImplName);
			Object javaField = javaFieldImplType.getConstructor(javaClassType, String.class)
				.newInstance(owner, fieldName);
			return javaField;
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			logger.fine(ignored.getMessage());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static List<?> toList(Object value) {
		if (value instanceof List) {
			return (List<?>) value;
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	private static List<?> getParameterizedTypeArguments(Object javaType) {
		if (Objects.isNull(javaType)) {
			return Collections.emptyList();
		}
		try {
			Object result = javaType.getClass().getMethod("getActualTypeArguments").invoke(javaType);
			if (result instanceof List) {
				return (List<?>) result;
			}
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			logger.fine(ignored.getMessage());
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	private static List<?> getTypeParameters(Object javaClass) {
		if (Objects.isNull(javaClass)) {
			return Collections.emptyList();
		}
		try {
			Object result = javaClass.getClass().getMethod("getTypeParameters").invoke(javaClass);
			if (result instanceof List) {
				return (List<?>) result;
			}
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			logger.fine(ignored.getMessage());
		}
		return Collections.emptyList();
	}

	private static String getTypeParameterName(Object typeParameter) {
		if (Objects.isNull(typeParameter)) {
			return StringUtils.EMPTY;
		}
		try {
			Object result = typeParameter.getClass().getMethod("getName").invoke(typeParameter);
			if (result instanceof String) {
				return (String) result;
			}
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			logger.fine(ignored.getMessage());
		}
		return StringUtils.EMPTY;
	}

	/**
	 * Retrieves a list of validated annotation values.
	 * <p>
	 * This method processes and extracts validation-related information from a given
	 * annotation object, based on the list of validation annotation names provided. It
	 * first determines the annotation type, then identifies the property name to extract
	 * based on the annotation type, and finally retrieves the corresponding annotation
	 * values according to that property name.
	 * </p>
	 * @param validates A list containing the names of annotations that need validation,
	 * used to identify valid annotations.
	 * @param javaAnnotation A JavaAnnotation object representing the annotation being
	 * processed.
	 * @return Returns a list of annotation value objects containing valid validation
	 * annotation values. If the property name cannot be determined or the annotation does
	 * not contain valid validation information, an empty list is returned.
	 */
	private static List<Object> getValidatedAnnotationValues(Set<String> validates, Object javaAnnotation) {
		String simpleName = DocUtil.getAnnotationTypeValue(javaAnnotation);

		// Determine the property name based on the annotation type
		String propertyName = null;
		if (JSRAnnotationConstants.VALIDATED.equalsIgnoreCase(simpleName)) {
			propertyName = DocAnnotationConstants.VALUE_PROP;
		}
		else if (validates.contains(simpleName)) {
			propertyName = DocAnnotationConstants.GROUP_PROP;
		}

		// If propertyName is determined, extract the annotation values
		if (propertyName != null) {
			return getAnnotationValues(javaAnnotation, propertyName);
		}

		return Collections.emptyList();
	}

	/**
	 * Retrieves a list of validated annotation values.
	 * <p>
	 * This method processes and extracts validation-related information from a given
	 * annotation object, based on the list of validation annotation names provided. It
	 * first determines the annotation type, then identifies the property name to extract
	 * based on the annotation type, and finally retrieves the corresponding annotation
	 * values according to that property name.
	 * </p>
	 * @param javaAnnotation A JavaAnnotation object representing the annotation being
	 * processed.
	 * @return Returns a list of annotation value objects containing valid validation
	 * annotation values. If the property name cannot be determined or the annotation does
	 * not contain valid validation information, an empty list is returned.
	 */
	private static List<Object> getValidatedAnnotationValues(Object javaAnnotation) {
		return getValidatedAnnotationValues(DocValidatorAnnotationEnum.VALIDATOR_ANNOTATIONS, javaAnnotation);
	}

	/**
	 * Generic parameter map.
	 * @param genericMap generic map
	 * @param cls Java class
	 * @param globGicName generic name array
	 */
	public static void genericParamMap(Map<String, String> genericMap, Object cls, String[] globGicName) {
		if (Objects.isNull(cls)) {
			return;
		}
		List<?> variables = getTypeParameters(cls);
		if (!variables.isEmpty()) {
			for (int i = 0; i < variables.size() && i < globGicName.length; i++) {
				String typeParameterName = getTypeParameterName(variables.get(i));
				if (StringUtil.isNotEmpty(typeParameterName)) {
					genericMap.put(typeParameterName, globGicName[i]);
				}
			}
			return;
		}
		try {
			Class<?> c = Class.forName(DocUtil.getClassCanonicalName(cls));
			TypeVariable<?>[] tValue = c.getTypeParameters();
			for (int i = 0; i < tValue.length && i < globGicName.length; i++) {
				genericMap.put(tValue[i].getName(), globGicName[i]);
			}
		}
		catch (ClassNotFoundException e) {
			// skip
		}
	}

	/**
	 * Formats a Java type string. This method processes and formats the return type
	 * string, removing generic uncertainty indicators and spaces.
	 * @param returnType The original return type string, which may contain generic
	 * uncertainty indicators.
	 * @return The formatted return type string, with '?', ' ', and 'extends' characters
	 * removed.
	 */
	public static String javaTypeFormat(String returnType) {
		// Check if the return type string contains '?'. If it does, format the string.
		if (returnType.contains("?")) {
			// Use regex to remove '?', spaces, and 'extends' from the string and return
			// the formatted string.
			return returnType.replaceAll("[?\\s]", "").replaceAll("extends", "");
		}
		// If the return type string does not contain '?', return the original string.
		return returnType;
	}

	/**
	 * Determines if one class is a child of another class.
	 * <p>
	 * This method checks whether the class represented by `sourceClass` is a subclass of
	 * the class represented by `targetClass`. It returns true if `sourceClass` is a
	 * subclass or identical to `targetClass`, and false otherwise. If either
	 * `sourceClass` or `targetClass` cannot be found, it returns false and logs a warning
	 * message.
	 * </p>
	 * @param sourceClass The name of the class to check.
	 * @param targetClass The name of the target class to determine if `sourceClass` is a
	 * subclass of.
	 * @return true if `sourceClass` is a subclass or identical to `targetClass`; false
	 * otherwise.
	 */
	public static boolean isTargetChildClass(String sourceClass, String targetClass) {
		try {
			// If the sourceClass is the same as the targetClass, return true.
			if (sourceClass.equals(targetClass)) {
				return true;
			}
			// Obtain the Class object for the sourceClass.
			Class<?> c = Class.forName(sourceClass);
			// Loop through the inheritance hierarchy until a match is found or the top of
			// the hierarchy is reached.
			while (c != null) {
				// If the current class matches the targetClass, return true.
				if (c.getName().equals(targetClass)) {
					return true;
				}
				// Get the superclass of the current class.
				c = c.getSuperclass();
			}
		}
		// Catch the exception when the class cannot be found.
		catch (ClassNotFoundException e) {
			// Log a warning message.
			logger.warning("JavaClass.isTargetChildClass() Unable to find class " + sourceClass);
			return false;
		}
		// Return false if no match was found or an exception occurred.
		return false;
	}

	/**
	 * Retrieves all fields marked for JSON serialization ignoring from a given class.
	 * This method inspects the annotations of the class to find field names that are
	 * marked as ignored for JSON serialization.
	 * @param cls The JavaClass object representing the class to inspect its annotations.
	 * @return A map containing field names and their ignore properties. Returns an empty
	 * map if not found or if input is null.
	 */
	public static Map<String, String> getClassJsonIgnoreFields(Object cls) {
		if (Objects.isNull(cls)) {
			return Collections.emptyMap();
		}
		List<?> classAnnotation = DocUtil.getClassAnnotations(cls);
		Map<String, String> ignoreFields = new HashMap<>(16);

		for (Object annotation : classAnnotation) {
			String simpleAnnotationName = DocUtil.getAnnotationTypeValue(annotation);
			if (DocAnnotationConstants.SHORT_JSON_IGNORE_PROPERTIES.equalsIgnoreCase(simpleAnnotationName)) {
				return JavaClassUtil.getJsonIgnoresProp(annotation, DocAnnotationConstants.VALUE_PROP);
			}
			if (DocAnnotationConstants.SHORT_JSON_TYPE.equals(simpleAnnotationName)) {
				return JavaClassUtil.getJsonIgnoresProp(annotation, DocAnnotationConstants.IGNORE_PROP);
			}
		}
		return ignoreFields;
	}

	/**
	 * Retrieves specified ignored properties from a Java annotation in JSON format. This
	 * method parses a specific parameter from an annotation and converts it into a map
	 * format for further processing. It handles three scenarios: the parameter does not
	 * exist, is a single value, or is multiple values.
	 * @param annotation The input Java annotation.
	 * @param propName The name of the parameter in the annotation that indicates which
	 * properties should be ignored.
	 * @return A Map where keys are property names to be ignored and values are null.
	 */
	@SuppressWarnings({ "unchecked" })
	public static Map<String, String> getJsonIgnoresProp(Object annotation, String propName) {
		Map<String, String> ignoreFields = new HashMap<>(16);
		Object ignoresObject = DocUtil.getAnnotationNamedParameter(annotation, propName);
		if (Objects.isNull(ignoresObject)) {
			return ignoreFields;
		}
		if (ignoresObject instanceof String) {
			String prop = StringUtil.removeQuotes(ignoresObject.toString());
			ignoreFields.put(prop, null);
			return ignoreFields;
		}
		LinkedList<String> ignorePropList = (LinkedList<String>) ignoresObject;
		for (String str : ignorePropList) {
			String prop = StringUtil.removeQuotes(str);
			ignoreFields.put(prop, null);
		}
		return ignoreFields;
	}

	/**
	 * getFieldGenericType by ClassLoader
	 * @param javaField JavaField
	 * @param classLoader ClassLoader
	 * @return fieldGenericType
	 */
	private static String getFieldGenericType(Object javaField, ClassLoader classLoader) {
		Object javaType = invokeMethod(javaField, "getType");
		if (JavaClassValidateUtil.isPrimitive(DocUtil.getTypeGenericCanonicalName(javaType))
				|| (Boolean.TRUE.equals(invokeMethod(javaField, "isFinal"))
						&& Boolean.TRUE.equals(invokeMethod(javaField, "isPrivate")))) {
			return null;
		}
		String name = DocUtil.getFieldName(javaField);
		try {
			Class<?> c;
			String binaryName = DocUtil.getTypeBinaryName(invokeMethod(javaField, "getDeclaringClass"));
			if (Objects.nonNull(classLoader)) {
				c = classLoader.loadClass(binaryName);
			}
			else {
				c = Class.forName(binaryName);
			}
			Field f = c.getDeclaredField(name);
			f.setAccessible(true);
			Type t = f.getGenericType();
			return StringUtil.trim(t.getTypeName());
		}
		catch (NoSuchFieldException | ClassNotFoundException | NoClassDefFoundError e) {
			return null;
		}
	}

	/**
	 * Retrieves the generic return type name of a Java method.
	 * @param javaMethod The Java method object from which to extract the return type
	 * information.
	 * @param classLoader The class loader used to load the class. If null, uses
	 * Class.forName to load the class.
	 * @return The string representation of the generic return type name, or null if the
	 * generic type cannot be determined.
	 */
	private static String getReturnGenericType(Object javaMethod, ClassLoader classLoader) {
		String methodName = DocUtil.getMethodName(javaMethod);
		// `BinaryName` is the correct name for inner classes required by
		// `ClassLoader.loadClass`
		// and `Class.forName`, as inner class paths use `$` instead of `.`.
		String binaryName = DocUtil.getTypeBinaryName(DocUtil.getMethodDeclaringClass(javaMethod));
		try {
			Class<?> c;
			if (Objects.nonNull(classLoader)) {
				c = classLoader.loadClass(binaryName);
			}
			else {
				c = Class.forName(binaryName);
			}

			Method m = c.getDeclaredMethod(methodName);
			Type t = m.getGenericReturnType();
			return StringUtil.trim(t.getTypeName());
		}
		catch (ClassNotFoundException | NoSuchMethodException e) {
			return null;
		}
	}

	/**
	 * Replaces generic type parameters in a given type name with their corresponding
	 * actual types, based on the provided mapping.
	 * @param originalName the original type name containing generic type parameters
	 * @param actualTypesMap a mapping of generic type parameter names to their
	 * corresponding actual type metadata
	 * @return the type name with generic type parameters replaced by their actual types
	 */
	public static String getGenericsNameByActualTypesMap(String originalName, Map<String, ?> actualTypesMap) {
		// Find the index of the last left angle bracket '<' and the first right angle
		// bracket '>'
		int typeNameLastLeftIndex = originalName.lastIndexOf('<');
		int typeNameFirstRightIndex = originalName.indexOf('>', typeNameLastLeftIndex);

		// If both angle brackets are found
		if (typeNameLastLeftIndex > 0 && typeNameFirstRightIndex > 0) {
			// Extract the substring containing the generics
			String genericsString = originalName.substring(typeNameLastLeftIndex + 1, typeNameFirstRightIndex);
			String[] generics = genericsString.split(",");

			// StringBuilder to build the replaced string
			StringBuilder resultString = new StringBuilder();
			// Append the portion of originalName before the generics, including the '<'
			resultString.append(originalName, 0, typeNameLastLeftIndex + 1);

			// Replace each generic type
			for (String generic : generics) {
				// Trim the generic type to remove leading/trailing whitespaces
				String trimmedGeneric = generic.trim();
				// Look up the mapped type in the actualTypesMap
				Object mappedType = actualTypesMap.get(trimmedGeneric);
				String mappedTypeName = trimmedGeneric;
				if (Objects.nonNull(mappedType)) {
					try {
						Object canonicalName = mappedType.getClass().getMethod("getCanonicalName").invoke(mappedType);
						if (canonicalName instanceof String && StringUtil.isNotEmpty((String) canonicalName)) {
							mappedTypeName = (String) canonicalName;
						}
					}
					catch (ReflectiveOperationException | RuntimeException ignored) {
						mappedTypeName = String.valueOf(mappedType);
					}
				}
				// If a mapping is found, append the mapped type; otherwise, keep the
				// original generic type
				resultString.append(mappedTypeName);
				// Append a comma after each replaced generic type
				resultString.append(",");
			}
			// Remove the trailing comma
			resultString.setLength(resultString.length() - 1);
			// Append the portion of originalName after the generics, including the '>'
			resultString.append(originalName, typeNameFirstRightIndex, originalName.length());

			return resultString.toString();
		}
		// Return originalName unchanged if no generics are found
		return originalName;
	}

	/**
	 * Extracts `@JsonView` value classes from a collection of annotations.
	 * @param annotations the collection of Java annotations to process
	 * @param builder the project documentation configuration builder, used to access
	 * configuration details
	 * @return a set containing all the JSON view classes extracted from the annotations.
	 * If the input annotation collection is empty, an empty set is returned.
	 */
	public static Set<String> getParamJsonViewClasses(Collection<?> annotations, ProjectDocConfigBuilder builder) {
		if (CollectionUtil.isEmpty(annotations)) {
			return Collections.emptySet();
		}
		Set<String> result = new HashSet<>();
		for (Object annotation : annotations) {
			return getJsonViewClasses(annotation, builder, true);
		}
		return result;
	}

	/**
	 * Retrieves a set of fully qualified class names associated with the JsonView.
	 * @param javaAnnotation The Java annotation containing the JsonView information,
	 * which
	 * @param builder The project configuration builder used to retrieve class information
	 * by name.
	 * @param isParam if isParam,just return the type;if isParam is false, return the
	 * super class and interface
	 * @return A set of fully qualified class names related to the JsonView.
	 */
	public static Set<String> getJsonViewClasses(Object javaAnnotation, ProjectDocConfigBuilder builder,
			boolean isParam) {

		String annotationName = DocUtil.getAnnotationTypeValue(javaAnnotation);
		if (!DocAnnotationConstants.SHORT_JSON_VIEW.equals(annotationName)) {
			return Collections.emptySet();
		}

		// Retrieve fully qualified class names from the annotation property
		List<String> classNames = getAnnotationValueClassNames(javaAnnotation, DocAnnotationConstants.VALUE_PROP);

		// If class names are present, process them to get class and its super
		// classes/interfaces
		Set<String> result = new HashSet<>();
		if (CollectionUtil.isNotEmpty(classNames)) {
			if (isParam) {
				result.addAll(classNames);
			}
			else {
				classNames.forEach(typeName -> {
					Object clazzObj = builder.getClassByName(typeName);
					if (Objects.nonNull(clazzObj)) {
						result.addAll(getSuperJavaClassAndInterface(clazzObj));
					}
				});
			}
		}

		return result;

	}

	/**
	 * Determines whether a field should be excluded from the JSON output based on the
	 * {@code @JsonView} annotation present on the method.
	 * <p>
	 * This method uses the {@code shouldIncludeFieldInJsonView} method to determine if
	 * the field should be included and negates the result to determine if it should be
	 * excluded.
	 * @param annotation the annotation present on the field, typically {@code @JsonView}.
	 * @param methodJsonViewClasses the set of {@code JsonView} classes specified on the
	 * method.
	 * @param isResp a boolean indicating whether the current context is a response.
	 * @param projectBuilder the project configuration builder.
	 * @return {@code true} if the field should be excluded from the JSON output,
	 * otherwise {@code false}.
	 */
	public static boolean shouldExcludeFieldFromJsonView(Object annotation, Set<String> methodJsonViewClasses,
			boolean isResp, ProjectDocConfigBuilder projectBuilder) {
		return !shouldIncludeFieldInJsonView(annotation, methodJsonViewClasses, isResp, projectBuilder);
	}

	/**
	 * Determines if a field should be included in the JSON response based on the presence
	 * of {@code @JsonView} annotations on the method and field. This method checks if the
	 * field's annotation matches any of the method's JsonView classes when the context is
	 * a response.
	 * @param annotation The annotation on the field being checked.
	 * @param methodJsonViewClasses The set of JsonView classes associated with the
	 * method.
	 * @param isResponse Whether the current context is for a response (true) or a request
	 * (false).
	 * @param projectBuilder The project configuration builder used to resolve classes.
	 * @return {@code true} if the field should be included in the JSON view;
	 * {@code false} otherwise.
	 */
	public static boolean shouldIncludeFieldInJsonView(Object annotation, Set<String> methodJsonViewClasses,
			boolean isResponse, ProjectDocConfigBuilder projectBuilder) {
		String simpleAnnotationName = DocUtil.getAnnotationTypeValue(annotation);

		// If context is not a response or no JsonView classes are defined for the method,
		// include the field
		if (!isResponse || methodJsonViewClasses.isEmpty()) {
			return true;
		}

		// If the annotation is not JsonView, exclude the field
		if (!DocAnnotationConstants.SHORT_JSON_VIEW.equals(simpleAnnotationName)) {
			return false;
		}

		// Check if the field's JsonView classes match any of the method's JsonView
		// classes
		Set<String> paramJsonViewClasses = getJsonViewClasses(annotation, projectBuilder, true);
		return !Collections.disjoint(methodJsonViewClasses, paramJsonViewClasses);
	}

	/**
	 * Recursively retrieves the fully qualified names of superclasses and interfaces of a
	 * given class metadata object.
	 * @param clazz class metadata object
	 * @return a set of fully qualified names of superclasses and interfaces
	 */
	public static Set<String> getSuperJavaClassAndInterface(Object clazz) {
		if (Objects.isNull(clazz)) {
			return Collections.emptySet();
		}
		Set<String> superClassesAndInterfaces = new HashSet<>();
		String fullyQualifiedName = DocUtil.getTypeFullyQualifiedName(clazz);
		if (StringUtil.isNotEmpty(fullyQualifiedName)) {
			superClassesAndInterfaces.add(fullyQualifiedName);
		}

		Object parentClass = DocUtil.getClassSuperJavaClass(clazz);
		String parentSimpleName = DocUtil.getClassSimpleName(parentClass);
		if (Objects.nonNull(parentClass) && !JavaTypeConstants.OBJECT_SIMPLE_NAME.equals(parentSimpleName)) {
			String parentFullyQualifiedName = DocUtil.getTypeFullyQualifiedName(parentClass);
			if (StringUtil.isNotEmpty(parentFullyQualifiedName)) {
				superClassesAndInterfaces.add(parentFullyQualifiedName);
			}
			superClassesAndInterfaces.addAll(getSuperJavaClassAndInterface(parentClass));
		}

		for (Object anInterface : getInterfaceClasses(clazz)) {
			String interfaceFullyQualifiedName = DocUtil.getTypeFullyQualifiedName(anInterface);
			if (StringUtil.isNotEmpty(interfaceFullyQualifiedName)) {
				superClassesAndInterfaces.add(interfaceFullyQualifiedName);
			}
			superClassesAndInterfaces.addAll(getSuperJavaClassAndInterface(anInterface));
		}

		return superClassesAndInterfaces;
	}

	/**
	 * Retrieves a list of fully qualified class names from a specified annotation
	 * property.
	 * <p>
	 * This method extracts the fully qualified names of classes referenced by a specific
	 * property within a Java annotation. It handles both single class references and
	 * lists of class references. If the property is not found or has no valid class
	 * references, an empty list is returned.
	 * </p>
	 * @param javaAnnotation the annotation containing the property
	 * @param propertyName the name of the property to retrieve
	 * @return a list of fully qualified class names or an empty list if not present
	 */
	public static List<String> getAnnotationValueClassNames(Object javaAnnotation, String propertyName) {
		Object propertyValue = DocUtil.getAnnotationProperty(javaAnnotation, propertyName);
		if (propertyValue != null) {
			List<Object> annotationValueList = extractAnnotationValueList(propertyValue);
			if (CollectionUtil.isNotEmpty(annotationValueList)) {
				return annotationValueList.stream()
					.map(JavaClassUtil::extractTypeFromAnnotationValue)
					.filter(Objects::nonNull)
					.map(DocUtil::getTypeFullyQualifiedName)
					.filter(StringUtil::isNotEmpty)
					.collect(Collectors.toList());
			}
			Object annotationType = extractTypeFromAnnotationValue(propertyValue);
			if (Objects.nonNull(annotationType)) {
				String fullyQualifiedName = DocUtil.getTypeFullyQualifiedName(annotationType);
				if (StringUtil.isNotEmpty(fullyQualifiedName)) {
					return Collections.singletonList(fullyQualifiedName);
				}
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Retrieves a list of string values from a specified annotation property.
	 * <p>
	 * This method extracts string values referenced by a specific property within a Java
	 * annotation. It handles both single string values and lists of string values. If the
	 * property is not found or has no valid string values, an empty list is returned.
	 * </p>
	 * @param projectBuilder the project configuration builder
	 * @param javaAnnotation the annotation containing the property
	 * @param propertyName the name of the property to retrieve
	 * @return a list of string values or an empty list if not present
	 */
	public static List<String> getAnnotationValueStrings(ProjectDocConfigBuilder projectBuilder, Object javaAnnotation,
			String propertyName) {
		Object propertyValue = DocUtil.getAnnotationProperty(javaAnnotation, propertyName);
		if (propertyValue != null) {
			ClassLoader classLoader = projectBuilder.getApiConfig().getClassLoader();
			List<Object> annotationValueList = extractAnnotationValueList(propertyValue);
			if (CollectionUtil.isNotEmpty(annotationValueList)) {
				return annotationValueList.stream()
					.map(temp -> DocUtil.resolveAnnotationValue(classLoader, temp))
					.filter(StringUtil::isNotEmpty)
					.collect(Collectors.toList());
			}
			String value = DocUtil.resolveAnnotationValue(classLoader, propertyValue);
			return Collections.singletonList(value);
		}
		return Collections.emptyList();
	}

	/**
	 * Retrieves annotation value objects from a specified annotation property.
	 * <p>
	 * This method extracts value objects referenced by a specific property within a Java
	 * annotation. It handles both single value references and lists of value references.
	 * </p>
	 * @param javaAnnotation the annotation containing the property
	 * @param propertyName the name of the property to retrieve
	 * @return a list of annotation value objects if present, otherwise an empty list
	 */
	public static List<Object> getAnnotationValues(Object javaAnnotation, String propertyName) {
		Object annotationValue = DocUtil.getAnnotationProperty(javaAnnotation, propertyName);
		if (Objects.isNull(annotationValue)) {
			return Collections.emptyList();
		}
		List<Object> annotationValueList = extractAnnotationValueList(annotationValue);
		if (CollectionUtil.isNotEmpty(annotationValueList)) {
			return annotationValueList;
		}
		Object annotationType = extractTypeFromAnnotationValue(annotationValue);
		if (Objects.nonNull(annotationType)) {
			return Collections.singletonList(annotationValue);
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	private static List<Object> extractAnnotationValueList(Object annotationValue) {
		if (Objects.isNull(annotationValue)) {
			return Collections.emptyList();
		}
		try {
			Object result = annotationValue.getClass().getMethod("getValueList").invoke(annotationValue);
			if (result instanceof List) {
				return (List<Object>) result;
			}
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			logger.fine(ignored.getMessage());
		}
		return Collections.emptyList();
	}

	private static Object extractTypeFromAnnotationValue(Object annotationValue) {
		if (Objects.isNull(annotationValue)) {
			return null;
		}
		try {
			Object result = annotationValue.getClass().getMethod("getType").invoke(annotationValue);
			if (Objects.nonNull(result)) {
				return result;
			}
		}
		catch (ReflectiveOperationException | RuntimeException ignored) {
			logger.fine(ignored.getMessage());
		}
		return null;
	}

	/**
	 * Retrieves enum information and values for a given Java class.
	 * <p>
	 * This method first gathers general information about the enum, then collects all
	 * enum values and their descriptions. If the enum is used in a form, it only collects
	 * names and sets the type as string.
	 * @param javaClass The Java class object representing the enum.
	 * @param builder The project documentation configuration builder, used to access
	 * project-specific documentation settings.
	 * @param jsonEnum Whether it is an enum in JSON
	 * @return An EnumInfoAndValues object containing both the enum information and its
	 * values.
	 */
	public static EnumInfoAndValues getEnumInfoAndValue(Object javaClass, ProjectDocConfigBuilder builder,
			boolean jsonEnum) {
		EnumInfo enumInfo = getEnumInfo(javaClass, builder);
		if (enumInfo == null) {
			return null;
		}
		return generateEnumInfoAndValues(enumInfo, javaClass, builder, jsonEnum);
	}

	/**
	 * Gets enum item list from class metadata objects.
	 * @param enumClass enum class metadata object
	 * @param dataDictionary data dictionary
	 * @param builder builder
	 * @return enum item list
	 */
	private static List<Item> getEnumItemList(Object enumClass, ApiDataDictionary dataDictionary,
			ProjectDocConfigBuilder builder) {
		ApiConfig apiConfig = builder.getApiConfig();
		if (Objects.nonNull(dataDictionary)) {
			Class<?> dataDictionaryEnumClass = dataDictionary.getEnumClass();
			if (dataDictionaryEnumClass.isInterface()) {
				try {
					ClassLoader classLoader = apiConfig.getClassLoader();
					dataDictionaryEnumClass = classLoader.loadClass(DocUtil.getTypeBinaryName(enumClass));
				}
				catch (ClassNotFoundException e) {
					return null;
				}
			}
			List<EnumDictionary> enumInformation = EnumUtil.getEnumInformation(dataDictionaryEnumClass,
					dataDictionary.getCodeField(), dataDictionary.getDescField());
			return enumInformation.stream()
				.map(i -> new Item(i.getName(), i.getType(), i.getValue(), i.getDesc()))
				.collect(Collectors.toList());
		}

		List<?> enumConstants = DocUtil.getClassEnumConstants(enumClass);
		return enumConstants.stream().map(cons -> {
			Item item = new Item();
			String name = DocUtil.getFieldName(cons);
			String enumComment = DocUtil.getFieldComment(cons);
			item.setName(name);
			item.setValue(name);
			item.setType(ParamTypeConstants.PARAM_TYPE_ENUM);
			item.setDescription(enumComment);

			Object defaultEnumValue = getDefaultEnumValue(enumClass, builder, cons);
			if (defaultEnumValue == null) {
				item.setValue(StringUtil.removeDoubleQuotes(name));
				return item;
			}
			String stringValue = StringUtil.removeQuotes(String.valueOf(defaultEnumValue));

			if (!Objects.equals(name, stringValue)) {
				item.setValue(stringValue);
				item.setType(DocClassUtil.processTypeNameForParams(defaultEnumValue.getClass().getCanonicalName()));
			}
			return item;
		}).collect(Collectors.toList());
	}

	/**
	 * Generates enum info/value view from class metadata objects.
	 * @param enumInfo enum info
	 * @param javaClass enum class metadata object
	 * @param builder builder
	 * @param jsonEnum whether enum is used in json
	 * @return enum info/value model
	 */
	private static EnumInfoAndValues generateEnumInfoAndValues(EnumInfo enumInfo, Object javaClass,
			ProjectDocConfigBuilder builder, boolean jsonEnum) {
		ApiConfig apiConfig = builder.getApiConfig();
		List<Item> items = enumInfo.getItems();
		String enumValue = null;
		List<String> enumValues = null;
		String type = null;

		if (jsonEnum || apiConfig.isEnumConvertor()) {
			List<?> enumConstants = DocUtil.getClassEnumConstants(javaClass);
			List<Object> enumValueList = enumConstants.stream()
				.map(cons -> getEnumValueWithJsonValue(javaClass, builder, cons))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

			if (CollectionUtil.isNotEmpty(enumValueList)) {
				Object enumValueWithJsonValue = enumValueList.get(0);
				type = DocClassUtil.processTypeNameForParams(enumValueWithJsonValue.getClass().getCanonicalName());
				enumValue = String.valueOf(enumValueWithJsonValue);
				enumValues = enumValueList.stream().map(String::valueOf).collect(Collectors.toList());
			}
		}

		if (enumValue == null) {
			enumValues = items.stream().map(Item::getName).collect(Collectors.toList());
			if (CollectionUtil.isNotEmpty(enumValues)) {
				enumValue = enumValues.get(0);
			}
			type = ParamTypeConstants.PARAM_TYPE_ENUM;
		}

		return EnumInfoAndValues.builder()
			.setEnumInfo(enumInfo)
			.setEnumValues(enumValues)
			.setType(type)
			.setValue(enumValue);
	}

}
