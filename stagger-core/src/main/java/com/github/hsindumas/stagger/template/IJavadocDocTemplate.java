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
package com.github.hsindumas.stagger.template;

import static com.github.hsindumas.stagger.constants.DocTags.DEPRECATED;
import static com.github.hsindumas.stagger.constants.DocTags.IGNORE;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.common.util.StringUtil;
import com.github.hsindumas.stagger.constants.DocAnnotationConstants;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.constants.DocTags;
import com.github.hsindumas.stagger.constants.JavaTypeConstants;
import com.github.hsindumas.stagger.constants.ParamTypeConstants;
import com.github.hsindumas.stagger.helper.ParamsBuildHelper;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiParam;
import com.github.hsindumas.stagger.model.DocJavaMethod;
import com.github.hsindumas.stagger.model.JavadocJavaMethod;
import com.github.hsindumas.stagger.utils.ApiParamTreeUtil;
import com.github.hsindumas.stagger.utils.DocClassUtil;
import com.github.hsindumas.stagger.utils.DocUtil;
import com.github.hsindumas.stagger.utils.JavaClassUtil;
import com.github.hsindumas.stagger.utils.JavaClassValidateUtil;
import com.github.hsindumas.stagger.utils.JavaFieldUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * java doc template
 *
 * @param <T> extends JavadocJavaMethod
 * @author shalousun
 * @author HsinDumas
 * @since 3.0.5
 */
public interface IJavadocDocTemplate<T extends JavadocJavaMethod> extends IBaseDocBuildTemplate {

    /**
     * Add method modifiers
     * @return boolean
     */
    boolean addMethodModifiers();

    /**
     * Create empty JavadocJavaMethod.
     * @return empty JavadocJavaMethod
     */
    T createEmptyJavadocJavaMethod();

    /**
     * Convert method metadata to JavadocJavaMethod
     * @param apiConfig ApiConfig
     * @param method method metadata object
     * @param actualTypesMap Map
     * @return JavadocJavaMethod
     */
    default T convertToJavadocJavaMethod(ApiConfig apiConfig, Object method, Map<String, ?> actualTypesMap) {
        Object cls = DocUtil.getMethodDeclaringClass(method);
        T javadocJavaMethod = this.createEmptyJavadocJavaMethod();
        javadocJavaMethod.setJavaMethod(method);
        javadocJavaMethod.setName(DocUtil.getMethodName(method));
        javadocJavaMethod.setActualTypesMap(actualTypesMap);
        String methodDefine = this.methodDefinition(method, actualTypesMap);
        String scapeMethod = methodDefine.replaceAll("<", "&lt;");
        scapeMethod = scapeMethod.replaceAll(">", "&gt;");

        javadocJavaMethod.setMethodDefinition(methodDefine);
        javadocJavaMethod.setEscapeMethodDefinition(scapeMethod);
        javadocJavaMethod.setDesc(DocUtil.getEscapeAndCleanComment(DocUtil.getMethodComment(method)));
        // set detail
        String apiNoteValue = DocUtil.getNormalTagComments(method, DocTags.API_NOTE, DocUtil.getClassSimpleName(cls));
        if (StringUtil.isEmpty(apiNoteValue)) {
            apiNoteValue = DocUtil.getMethodComment(method);
        }
        String version = DocUtil.getNormalTagComments(method, DocTags.SINCE, DocUtil.getClassSimpleName(cls));
        javadocJavaMethod.setVersion(version);
        javadocJavaMethod.setDetail(apiNoteValue != null ? apiNoteValue : "");
        // set author
        String authorValue = DocUtil.getNormalTagComments(method, DocTags.AUTHOR, DocUtil.getClassSimpleName(cls));
        if (apiConfig.isShowAuthor() && StringUtil.isNotEmpty(authorValue)) {
            javadocJavaMethod.setAuthor(authorValue);
        }

        // Deprecated
        List<?> annotations = DocUtil.getMethodAnnotations(method);
        for (Object annotation : annotations) {
            String annotationName = DocUtil.getAnnotationTypeValue(annotation);
            if (DocAnnotationConstants.DEPRECATED.equals(annotationName)) {
                javadocJavaMethod.setDeprecated(true);
            }
        }
        if (Objects.nonNull(DocUtil.getMethodTagByName(method, DEPRECATED))) {
            javadocJavaMethod.setDeprecated(true);
        }
        return javadocJavaMethod;
    }

    /**
     * Get method definition
     * @param method method metadata object
     * @param actualTypesMap Map
     * @return String
     */
    default String methodDefinition(Object method, Map<String, ?> actualTypesMap) {
        StringBuilder methodBuilder = new StringBuilder();
        if (this.addMethodModifiers()) {
            // append method modifiers
            DocUtil.getMethodModifiers(method)
                    .forEach(item -> methodBuilder.append(item).append(" "));
        }
        String returnType = this.getMethodReturnType(method, actualTypesMap);
        // append method return type
        methodBuilder.append(returnType).append(" ");
        List<String> params = new ArrayList<>();
        List<?> parameters = DocUtil.getMethodParameters(method);
        for (Object parameter : parameters) {
            String typeName =
                    this.replaceTypeName(DocUtil.getParameterTypeValue(parameter), actualTypesMap, Boolean.TRUE);
            params.add(typeName + " " + DocUtil.getParameterName(parameter));
        }
        methodBuilder
                .append(DocUtil.getMethodName(method))
                .append("(")
                .append(String.join(", ", params))
                .append(")");
        return methodBuilder.toString();
    }

    /**
     * Processes methods of a given class and its parent or interfaces.
     * @param cls class metadata object
     * @param methodProcessor Function to process methods from a class
     * @return List of documented Java methods
     */
    default List<T> processClassHierarchy(Object cls, Function<Object, List<T>> methodProcessor) {
        List<T> docJavaMethods = new ArrayList<>();
        Set<Object> classesToProcess = new LinkedHashSet<>();
        classesToProcess.add(cls);

        while (!classesToProcess.isEmpty()) {
            Object currentClass = classesToProcess.iterator().next();
            classesToProcess.remove(currentClass);

            // Process methods
            docJavaMethods.addAll(methodProcessor.apply(currentClass));

            // Add parent class if not Object
            Object parentClass = DocUtil.getClassSuperJavaClass(currentClass);
            if (Objects.nonNull(parentClass)
                    && !JavaTypeConstants.OBJECT_SIMPLE_NAME.equals(DocUtil.getClassSimpleName(parentClass))) {
                classesToProcess.add(parentClass);
            }

            // Add interfaces
            classesToProcess.addAll(JavaClassUtil.getInterfaceClasses(currentClass));
        }

        return docJavaMethods;
    }

    /**
     * Get parent class and interface methods
     * @param apiConfig ApiConfig
     * @param cls class metadata object
     * @return List
     */
    default List<T> getParentsClassAndInterfaceMethods(ApiConfig apiConfig, Object cls) {
        return this.processClassHierarchy(cls, currentClass -> {
            List<T> docJavaMethods = new ArrayList<>();
            Map<String, ?> actualTypesMap = JavaClassUtil.getActualTypesMap(currentClass);
            List<?> methodList = DocUtil.getClassMethods(currentClass);
            for (Object method : methodList) {
                docJavaMethods.add(this.convertToJavadocJavaMethod(apiConfig, method, actualTypesMap));
            }
            return docJavaMethods;
        });
    }

    /**
     * Constructs a list of request parameters.
     * @param javaMethod The method metadata object, used to extract method information.
     * @param builder The ProjectDocConfigBuilder object, containing project configuration
     * details.
     * @param atomicInteger An AtomicInteger, used to generate unique parameter IDs.
     * @param actualTypesMap A map of actual types, used for type replacement.
     * @return A List of ApiParam objects representing the request parameters or null if
     * no parameters exist.
     */
    default List<ApiParam> requestParams(
            final Object javaMethod,
            ProjectDocConfigBuilder builder,
            AtomicInteger atomicInteger,
            Map<String, ?> actualTypesMap) {
        boolean isStrict = builder.getApiConfig().isStrict();
        boolean isShowJavaType = builder.getApiConfig().getShowJavaType();
        String className = DocUtil.getMethodDeclaringClassCanonicalName(javaMethod);
        Map<String, String> paramTagMap = DocUtil.getCommentsByTag(javaMethod, DocTags.PARAM, className);
        List<?> parameterList = DocUtil.getMethodParameters(javaMethod);
        if (parameterList.isEmpty()) {
            return null;
        }
        ClassLoader classLoader = builder.getApiConfig().getClassLoader();
        List<ApiParam> paramList = new ArrayList<>();
        for (Object parameter : parameterList) {
            boolean required = false;
            String paramName = DocUtil.getParameterName(parameter);
            String typeName = this.replaceTypeName(
                    DocUtil.getParameterTypeGenericCanonicalName(parameter), actualTypesMap, Boolean.FALSE);
            String simpleName = this.replaceTypeName(
                            DocUtil.getParameterTypeValue(parameter), actualTypesMap, Boolean.FALSE)
                    .toLowerCase();
            String fullTypeName = this.replaceTypeName(
                    DocUtil.getParameterFullyQualifiedName(parameter), actualTypesMap, Boolean.FALSE);
            String paramPre = paramName + ".";
            if (!paramTagMap.containsKey(paramName) && JavaClassValidateUtil.isPrimitive(fullTypeName) && isStrict) {
                throw new RuntimeException("ERROR: Unable to find javadoc @param for actual param \"" + paramName
                        + "\" in method " + DocUtil.getMethodName(javaMethod) + " from " + className);
            }
            StringBuilder comment = new StringBuilder(this.paramCommentResolve(paramTagMap.get(paramName)));
            List<?> annotations = DocUtil.getParameterAnnotations(parameter);
            String mockValue = JavaFieldUtil.createMockValue(paramTagMap, paramName, typeName, typeName, annotations);
            boolean enumType = builder.isEnumType(fullTypeName);
            for (Object annotation : annotations) {
                if (JavaClassValidateUtil.isJSR303Required(DocUtil.getAnnotationTypeValue(annotation))) {
                    required = true;
                }
            }
            Set<String> groupClasses = JavaClassUtil.getParamGroupJavaClass(annotations, builder);
            Set<String> paramJsonViewClasses = JavaClassUtil.getParamJsonViewClasses(annotations, builder);
            if (JavaClassValidateUtil.isCollection(fullTypeName) || JavaClassValidateUtil.isArray(fullTypeName)) {
                if (JavaClassValidateUtil.isCollection(typeName)) {
                    typeName = typeName + "<T>";
                }
                String[] gicNameArr = DocClassUtil.getSimpleGicName(typeName);
                String gicName = gicNameArr[0];
                if (JavaClassValidateUtil.isArray(gicName)) {
                    gicName = gicName.substring(0, gicName.indexOf("["));
                }
                if (JavaClassValidateUtil.isPrimitive(gicName)) {
                    String processedType = isShowJavaType
                            ? JavaClassUtil.getClassSimpleName(typeName)
                            : DocClassUtil.processTypeNameForParams(simpleName);
                    ApiParam param = ApiParam.of()
                            .setId(atomicInteger.incrementAndGet())
                            .setField(paramName)
                            .setDesc(comment + "   (children type : " + gicName + ")")
                            .setRequired(required)
                            .setType(processedType);
                    paramList.add(param);
                } else {
                    paramList.addAll(ParamsBuildHelper.buildParams(
                            gicNameArr[0],
                            paramPre,
                            0,
                            "true",
                            Boolean.FALSE,
                            new HashMap<>(16),
                            builder,
                            groupClasses,
                            paramJsonViewClasses,
                            0,
                            Boolean.FALSE,
                            atomicInteger));
                }
            } else if (JavaClassValidateUtil.isPrimitive(fullTypeName)) {
                ApiParam param = ApiParam.of()
                        .setId(atomicInteger.incrementAndGet())
                        .setField(paramName)
                        .setType(JavaClassUtil.getClassSimpleName(typeName))
                        .setDesc(comment.toString())
                        .setRequired(required)
                        .setMaxLength(JavaFieldUtil.getParamMaxLength(classLoader, annotations))
                        .setMinLength(JavaFieldUtil.getParamMinLength(classLoader, annotations))
                        .setValue(mockValue)
                        .setVersion(DocGlobalConstants.DEFAULT_VERSION);
                paramList.add(param);
            } else if (JavaClassValidateUtil.isMap(fullTypeName)) {
                if (JavaClassValidateUtil.isMap(typeName)) {
                    ApiParam apiParam = ApiParam.of()
                            .setId(atomicInteger.incrementAndGet())
                            .setField(paramName)
                            .setType(typeName)
                            .setDesc(comment.toString())
                            .setRequired(required)
                            .setVersion(DocGlobalConstants.DEFAULT_VERSION);
                    paramList.add(apiParam);
                    continue;
                }
                String[] gicNameArr = DocClassUtil.getSimpleGicName(typeName);
                paramList.addAll(ParamsBuildHelper.buildParams(
                        gicNameArr[1],
                        paramPre,
                        0,
                        "true",
                        Boolean.FALSE,
                        new HashMap<>(16),
                        builder,
                        groupClasses,
                        paramJsonViewClasses,
                        0,
                        Boolean.FALSE,
                        atomicInteger));
            } else if (enumType) {
                ApiParam param = ApiParam.of()
                        .setId(atomicInteger.incrementAndGet())
                        .setField(paramName)
                        .setType(ParamTypeConstants.PARAM_TYPE_ENUM)
                        .setRequired(required)
                        .setDesc(comment.toString())
                        .setVersion(DocGlobalConstants.DEFAULT_VERSION);
                paramList.add(param);
            } else {
                paramList.addAll(ParamsBuildHelper.buildParams(
                        typeName,
                        paramPre,
                        0,
                        "true",
                        Boolean.FALSE,
                        new HashMap<>(16),
                        builder,
                        groupClasses,
                        paramJsonViewClasses,
                        0,
                        Boolean.FALSE,
                        atomicInteger));
            }
        }
        return paramList;
    }

    /**
     * Builds a list of service methods. This method parses the given Java class, extracts
     * methods that meet certain criteria, and generates corresponding JavadocJavaMethod
     * objects for them.
     * @param cls The Java class to parse.
     * @param apiConfig The API configuration object, containing rules for documentation
     * generation.
     * @param projectBuilder The project documentation configuration builder, used to
     * construct project-level documentation configurations.
     * @return A list containing documented methods represented as JavadocJavaMethod
     * objects.
     */
    default List<T> buildServiceMethod(final Object cls, ApiConfig apiConfig, ProjectDocConfigBuilder projectBuilder) {
        if (StringUtil.isEmpty(DocUtil.getClassCanonicalName(cls))) {
            return new ArrayList<>(0);
        }
        String clsCanonicalName = DocUtil.getClassCanonicalName(cls);
        List<?> methods = DocUtil.getClassMethods(cls);
        List<T> methodDocList = new ArrayList<>(methods.size());

        Set<String> filterMethods = DocUtil.findFilterMethods(clsCanonicalName);
        boolean needAllMethods = filterMethods.contains(DocGlobalConstants.DEFAULT_FILTER_METHOD);

        for (Object method : methods) {
            if (DocUtil.isMethodPrivate(method)) {
                continue;
            }
            if (Objects.nonNull(DocUtil.getMethodTagByName(method, IGNORE))) {
                continue;
            }

            // skip method
            boolean skipMethod = this.skipMethod(cls, method, apiConfig, projectBuilder);
            if (skipMethod) {
                continue;
            }

            if (StringUtil.isEmpty(DocUtil.getMethodComment(method)) && apiConfig.isStrict()) {
                throw new RuntimeException("Unable to find comment for method " + DocUtil.getMethodName(method) + " in "
                        + DocUtil.getClassCanonicalName(cls));
            }
            if (needAllMethods || filterMethods.contains(DocUtil.getMethodName(method))) {
                T apiMethodDoc = this.convertToJavadocJavaMethod(apiConfig, method, null);
                methodDocList.add(apiMethodDoc);
            }
        }
        // Add parent class And interface methods
        methodDocList.addAll(this.getParentsClassAndInterfaceMethods(apiConfig, cls));

        Map<T, List<ApiParam>> methodRequestParams = new HashMap<>(16);
        Map<T, List<ApiParam>> methodResponseParams = new HashMap<>(16);

        // Construct the method map
        Map<String, T> methodMap = methodDocList.stream()
                .collect(Collectors.toMap(
                        method -> {
                            // Build request params
                            List<ApiParam> requestParams = this.requestParams(
                                    method.getJavaMethod(),
                                    projectBuilder,
                                    new AtomicInteger(0),
                                    method.getActualTypesMap());
                            methodRequestParams.put(method, requestParams);
                            // Build response params
                            List<ApiParam> responseParams = this.buildReturnApiParams(
                                    DocJavaMethod.builder()
                                            .setJavaMethod(method.getJavaMethod())
                                            .setActualTypesMap(method.getActualTypesMap()),
                                    projectBuilder);
                            methodResponseParams.put(method, responseParams);
                            String requestParamsStr = Objects.isNull(requestParams)
                                    ? "null"
                                    : requestParams.stream()
                                            .map(ApiParam::getFullyTypeName)
                                            .collect(Collectors.joining("|"));

                            String responseParamsString = (Objects.isNull(responseParams)
                                    ? "null"
                                    : responseParams.stream()
                                            .map(ApiParam::getFullyTypeName)
                                            .collect(Collectors.joining("|")));
                            return requestParamsStr + " " + method.getName() + "(" + responseParamsString + ")";
                        },
                        Function.identity(),
                        this::mergeJavadocMethods,
                        LinkedHashMap::new));

        int methodOrder = 0;
        List<T> javadocJavaMethods = new ArrayList<>(methodMap.size());
        for (T method : methodMap.values()) {
            methodOrder++;
            method.setOrder(methodOrder);
            String methodUid = DocUtil.generateId(clsCanonicalName + method.getName() + methodOrder);
            method.setMethodId(methodUid);
            // build request params
            List<ApiParam> requestParams = methodRequestParams.get(method);
            // build response params
            List<ApiParam> responseParams = methodResponseParams.get(method);
            if (apiConfig.isParamsDataToTree()) {
                method.setRequestParams(ApiParamTreeUtil.apiParamToTree(requestParams));
                method.setResponseParams(ApiParamTreeUtil.apiParamToTree(responseParams));
            } else {
                method.setRequestParams(requestParams);
                method.setResponseParams(responseParams);
            }
            javadocJavaMethods.add(method);
        }
        return javadocJavaMethods;
    }

    /**
     * Merges two JavadocJavaMethod objects. If the existing method lacks certain details
     * (description, detail, author, version) that the replacement method has, those
     * details are copied from the replacement method to the existing method.
     * @param existing The existing JavadocJavaMethod object.
     * @param replacement The replacement JavadocJavaMethod object.
     * @return The merged JavadocJavaMethod object, with details filled in from the
     * replacement method if necessary.
     */
    default T mergeJavadocMethods(T existing, T replacement) {
        // if existing info is empty and replacement info has desc,replace the info
        if (StringUtil.isEmpty(existing.getDesc()) && StringUtil.isNotEmpty(replacement.getDesc())) {
            existing.setDesc(replacement.getDesc());
        }
        if (StringUtil.isEmpty(existing.getDetail()) && StringUtil.isNotEmpty(replacement.getDetail())) {
            existing.setDetail(replacement.getDetail());
        }
        if (StringUtil.isEmpty(existing.getAuthor()) && StringUtil.isNotEmpty(replacement.getAuthor())) {
            existing.setAuthor(replacement.getAuthor());
        }
        if (StringUtil.isEmpty(existing.getVersion()) && StringUtil.isNotEmpty(replacement.getVersion())) {
            existing.setVersion(replacement.getVersion());
        }
        return existing;
    }

    /**
     * Determines whether the specified method should be skipped during documentation
     * generation.
     * <p>
     * If this method returns {@code true}, the method will be excluded from the generated
     * documentation. If it returns {@code false}, the method will be included.
     * </p>
     * <p>
     * The default implementation always returns {@code false}, meaning no methods are
     * skipped by default. Subclasses may override this method to provide custom logic for
     * excluding certain methods.
     * </p>
     * @param cls The Java class containing the method.
     * @param method The Java method to check.
     * @param apiConfig The API configuration object, used to control documentation
     * behavior.
     * @param projectBuilder The project documentation configuration builder.
     * @return {@code true} if the method should be skipped (not documented),
     * {@code false} if it should be included.
     */
    default boolean skipMethod(
            final Object cls, final Object method, ApiConfig apiConfig, ProjectDocConfigBuilder projectBuilder) {
        return false;
    }
}
