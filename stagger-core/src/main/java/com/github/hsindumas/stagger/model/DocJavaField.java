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
package com.github.hsindumas.stagger.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The doc of java field
 *
 * @author yu 2020/3/19.
 * @author HsinDumas
 */
public class DocJavaField {

    /**
     * field info
     */
    private Object javaField;

    /**
     * comment
     */
    private String comment;

    /**
     * tags
     */
    private List<?> docletTags;

    /**
     * annotations
     */
    private List<?> annotations;

    /**
     * field fullyQualifiedName
     */
    private String typeFullyQualifiedName;

    /**
     * field genericCanonicalName
     */
    private String typeGenericCanonicalName;

    /**
     * genericFullyQualifiedName
     */
    private String typeGenericFullyQualifiedName;

    /**
     * field generic actualJavaType;
     */
    private String actualJavaType;

    /**
     * field name
     */
    private String fieldName;

    private boolean array;

    private boolean primitive;

    private boolean collection;

    private boolean file;

    private boolean isEnum;

    /**
     * owner class
     */
    private String declaringClassName;

    private String typeSimpleName;

    public static DocJavaField builder() {
        return new DocJavaField();
    }

    @SuppressWarnings("unchecked")
    public <T> T getJavaField() {
        return (T) javaField;
    }

    public DocJavaField setJavaField(Object javaField) {
        this.javaField = javaField;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public DocJavaField setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getTypeFullyQualifiedName() {
        return typeFullyQualifiedName;
    }

    public DocJavaField setTypeFullyQualifiedName(String typeFullyQualifiedName) {
        this.typeFullyQualifiedName = typeFullyQualifiedName;
        return this;
    }

    public String getTypeGenericCanonicalName() {
        return typeGenericCanonicalName;
    }

    public DocJavaField setTypeGenericCanonicalName(String typeGenericCanonicalName) {
        this.typeGenericCanonicalName = typeGenericCanonicalName;
        return this;
    }

    public String getActualJavaType() {
        return actualJavaType;
    }

    public DocJavaField setActualJavaType(String actualJavaType) {
        this.actualJavaType = actualJavaType;
        return this;
    }

    public List<?> getDocletTags() {
        if (docletTags == null) {
            return new ArrayList<>();
        }
        return docletTags;
    }

    public DocJavaField setDocletTags(List<?> docletTags) {
        this.docletTags = docletTags;
        return this;
    }

    public List<?> getAnnotations() {
        List<?> fieldAnnotations = getJavaFieldAnnotations(javaField);
        if (fieldAnnotations != null && !fieldAnnotations.isEmpty()) {
            return fieldAnnotations;
        }
        if (annotations == null) {
            return new ArrayList<>();
        }
        return this.annotations;
    }

    @SuppressWarnings("unchecked")
    private static List<?> getJavaFieldAnnotations(Object field) {
        if (field == null) {
            return null;
        }
        try {
            Object value = field.getClass().getMethod("getAnnotations").invoke(field);
            if (value instanceof List<?>) {
                return (List<?>) value;
            }
        } catch (ReflectiveOperationException ignore) {
            // Keep compatibility with parser implementations that do not expose
            // annotations.
        }
        return null;
    }

    public DocJavaField setAnnotations(List<?> annotations) {
        this.annotations = annotations;
        return this;
    }

    public boolean isArray() {
        return array;
    }

    public DocJavaField setArray(boolean array) {
        this.array = array;
        return this;
    }

    public boolean isPrimitive() {
        return primitive;
    }

    public DocJavaField setPrimitive(boolean primitive) {
        this.primitive = primitive;
        return this;
    }

    public boolean isCollection() {
        return collection;
    }

    public DocJavaField setCollection(boolean collection) {
        this.collection = collection;
        return this;
    }

    public boolean isFile() {
        return file;
    }

    public DocJavaField setFile(boolean file) {
        this.file = file;
        return this;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public void setEnum(boolean anEnum) {
        isEnum = anEnum;
    }

    public String getFieldName() {
        return fieldName;
    }

    public DocJavaField setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public String getDeclaringClassName() {
        return declaringClassName;
    }

    public DocJavaField setDeclaringClassName(String declaringClassName) {
        this.declaringClassName = declaringClassName;
        return this;
    }

    public String getTypeGenericFullyQualifiedName() {
        return typeGenericFullyQualifiedName;
    }

    public DocJavaField setTypeGenericFullyQualifiedName(String typeGenericFullyQualifiedName) {
        this.typeGenericFullyQualifiedName = typeGenericFullyQualifiedName;
        return this;
    }

    public String getTypeSimpleName() {
        return typeSimpleName;
    }

    public DocJavaField setTypeSimpleName(String typeSimpleName) {
        this.typeSimpleName = typeSimpleName;
        return this;
    }
}
