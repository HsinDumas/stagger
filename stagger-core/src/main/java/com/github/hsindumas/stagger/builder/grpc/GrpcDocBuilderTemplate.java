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
package com.github.hsindumas.stagger.builder.grpc;

import com.github.hsindumas.stagger.builder.IRpcDocBuilderTemplate;
import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.common.util.CollectionUtil;
import com.github.hsindumas.stagger.common.util.FileUtil;
import com.github.hsindumas.stagger.common.util.StringUtil;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.constants.FrameworkEnum;
import com.github.hsindumas.stagger.constants.TemplateVariable;
import com.github.hsindumas.stagger.helper.JavaProjectBuilder;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.ApiDocDict;
import com.github.hsindumas.stagger.model.ApiErrorCode;
import com.github.hsindumas.stagger.model.grpc.GrpcApiDoc;
import com.github.hsindumas.stagger.template.engine.Template;
import com.github.hsindumas.stagger.utils.BeetlTemplateUtil;
import com.github.hsindumas.stagger.utils.DocUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * gRPC doc builder template.
 *
 * @author linwumingshi
 * @author HsinDumas
 * @since 3.0.7
 */
public class GrpcDocBuilderTemplate implements IRpcDocBuilderTemplate<GrpcApiDoc> {

    @Override
    public void checkAndInit(ApiConfig config, boolean checkOutPath) {
        if (StringUtil.isEmpty(config.getFramework())) {
            config.setFramework(FrameworkEnum.GRPC.getFramework());
        }
        IRpcDocBuilderTemplate.super.checkAndInit(config, checkOutPath);
        config.setOutPath(config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + DocGlobalConstants.GRPC_OUT_DIR);
    }

    @Override
    public GrpcApiDoc createEmptyApiDoc() {
        return new GrpcApiDoc();
    }

    @Override
    public void writeApiDocFile(Template mapper, ApiConfig config, GrpcApiDoc rpcDoc, String fileExtension) {
        FileUtil.nioWriteFile(
                mapper.render(),
                config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + rpcDoc.getName() + fileExtension);
    }

    @Override
    public void buildSearchJs(
            List<GrpcApiDoc> apiDocList,
            ApiConfig config,
            JavaProjectBuilder javaProjectBuilder,
            String template,
            String outPutFileName) {
        List<ApiErrorCode> errorCodeList = DocUtil.errorCodeDictToList(config, javaProjectBuilder);
        Template tpl = BeetlTemplateUtil.getByName(template);
        // add order
        List<GrpcApiDoc> apiDocs = new ArrayList<>();
        for (GrpcApiDoc apiDoc1 : apiDocList) {
            apiDoc1.setOrder(apiDocs.size());
            apiDocs.add(apiDoc1);
        }
        Map<String, String> titleMap = this.setDirectoryLanguageVariable(config, tpl);
        if (CollectionUtil.isNotEmpty(errorCodeList)) {
            GrpcApiDoc apiDoc1 = new GrpcApiDoc();
            apiDoc1.setOrder(apiDocs.size());
            apiDoc1.setDesc(titleMap.get(TemplateVariable.ERROR_LIST_TITLE.getVariable()));
            apiDoc1.setList(new ArrayList<>(0));
            apiDocs.add(apiDoc1);
        }

        // set dict list
        List<ApiDocDict> apiDocDictList = DocUtil.buildDictionary(config, javaProjectBuilder);
        tpl.binding(TemplateVariable.DICT_LIST.getVariable(), apiDocDictList);
        tpl.binding(TemplateVariable.DIRECTORY_TREE.getVariable(), apiDocs);
        FileUtil.nioWriteFile(tpl.render(), config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + outPutFileName);
    }

    /**
     * Build search js.
     * @param apiDocList list data of Api doc
     * @param config api config
     * @param configBuilder project doc config builder
     * @param template template
     * @param outPutFileName output file
     */
    public void buildSearchJs(
            List<GrpcApiDoc> apiDocList,
            ApiConfig config,
            ProjectDocConfigBuilder configBuilder,
            String template,
            String outPutFileName) {
        List<ApiErrorCode> errorCodeList = DocUtil.errorCodeDictToList(config, configBuilder);
        Template tpl = BeetlTemplateUtil.getByName(template);
        // add order
        List<GrpcApiDoc> apiDocs = new ArrayList<>();
        for (GrpcApiDoc apiDoc1 : apiDocList) {
            apiDoc1.setOrder(apiDocs.size());
            apiDocs.add(apiDoc1);
        }
        Map<String, String> titleMap = this.setDirectoryLanguageVariable(config, tpl);
        if (CollectionUtil.isNotEmpty(errorCodeList)) {
            GrpcApiDoc apiDoc1 = new GrpcApiDoc();
            apiDoc1.setOrder(apiDocs.size());
            apiDoc1.setDesc(titleMap.get(TemplateVariable.ERROR_LIST_TITLE.getVariable()));
            apiDoc1.setList(new ArrayList<>(0));
            apiDocs.add(apiDoc1);
        }

        List<ApiDocDict> apiDocDictList = DocUtil.buildDictionary(config, configBuilder);
        tpl.binding(TemplateVariable.DICT_LIST.getVariable(), apiDocDictList);
        tpl.binding(TemplateVariable.DIRECTORY_TREE.getVariable(), apiDocs);
        FileUtil.nioWriteFile(tpl.render(), config.getOutPath() + DocGlobalConstants.FILE_SEPARATOR + outPutFileName);
    }
}
