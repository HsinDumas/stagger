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
package com.github.hsindumas.stagger.builder.rpc;

import com.github.hsindumas.stagger.builder.ProjectDocConfigBuilder;
import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import com.github.hsindumas.stagger.helper.JavaProjectBuilderHelper;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.rpc.RpcApiDoc;
import com.github.hsindumas.stagger.template.engine.Template;
import com.github.hsindumas.stagger.utils.DocUtil;
import java.util.List;

/**
 * Dubbo Word doc builder
 *
 * @author <a href="dm131718@163.com">wangaiping</a>
 * @author HsinDumas
 * @since 3.0.10
 */
public class RpcWordDocBuilder {

    /**
     * template docx
     */
    private static final String TEMPLATE_DOCX = "template/word/template.docx";

    /**
     * build docx file name
     */
    private static final String INDEX_DOC = "rpc-index.docx";

    /**
     * build error code docx file name
     */
    private static final String BUILD_ERROR_DOCX = "error.docx";

    /**
     * build directory data docx file name
     */
    private static final String BUILD_DICT_DOCX = "dict.docx";

    /**
     * private constructor
     */
    private RpcWordDocBuilder() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * build dubbo api
     * @param config config
     * @throws Exception exception
     */
    public static void buildApiDoc(ApiConfig config) throws Exception {
        buildApiDoc(config, new ProjectDocConfigBuilder(config, JavaProjectBuilderHelper.create()));
    }

    /**
     * build dubbo api
     * @param config config
     * @param configBuilder ProjectDocConfigBuilder
     * @throws Exception exception
     */
    public static void buildApiDoc(ApiConfig config, ProjectDocConfigBuilder configBuilder) throws Exception {
        RpcDocBuilderTemplate rpcDocBuilderTemplate = new RpcDocBuilderTemplate();
        List<RpcApiDoc> apiDocList = rpcDocBuilderTemplate.getApiDoc(false, true, false, config, configBuilder);

        if (config.isAllInOne()) {
            String docName =
                    rpcDocBuilderTemplate.allInOneDocName(config, INDEX_DOC, DocGlobalConstants.WORD_DOC_EXTENSION);
            apiDocList = rpcDocBuilderTemplate.handleApiGroup(apiDocList, config);
            Template tpl = rpcDocBuilderTemplate.buildAllInOneWord(
                    apiDocList, config, configBuilder, DocGlobalConstants.RPC_ALL_IN_ONE_WORD_TPL, docName);

            String outPath = config.getOutPath();
            DocUtil.copyAndReplaceDocx(
                    tpl.render(), outPath + DocGlobalConstants.FILE_SEPARATOR + docName, TEMPLATE_DOCX);
        }
    }
}
