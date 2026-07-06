# stagger.json 配置参考

本页说明 Maven/Gradle 插件共同使用的 `stagger.json` 配置文件。

## 1. `stagger.json` 从哪里读取

- Maven 插件：
  - `<configFile>${project.basedir}/src/main/resources/stagger.json</configFile>`
  - 也可通过插件参数 `configFile` 覆盖。
- Gradle 插件：
  - `stagger { configFile = file("src/main/resources/stagger.json") }`
  - 也可在命令行使用 `-Pstagger.configFile=...` 覆盖。

如果都未显式指定，插件会回退到 `./src/main/resources/default.json`。

## 2. 最小可用示例

```json
{
  "allInOne": true,
  "isStrict": false,
  "outPath": "build/stagger"
}
```

## 3. 推荐起步模板

```json
{
  "projectName": "demo-service",
  "serverUrl": "https://api.example.com",
  "pathPrefix": "/api",
  "outPath": "build/stagger",
  "allInOne": true,
  "coverOld": true,
  "isStrict": false,
  "requestExample": true,
  "responseExample": true,
  "showValidation": true,
  "sourceCodePaths": [
    {
      "path": "src/main/java",
      "desc": "main sources"
    }
  ],
  "requestHeaders": [
    {
      "name": "Authorization",
      "type": "string",
      "desc": "Bearer token",
      "required": false,
      "since": "-",
      "paramIn": "header"
    }
  ]
}
```

## 4. 字段说明

先列常用字段，再列进阶字段。

### 4.1 常用字段

| 键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `projectName` | string | 模块名 | 文档中的项目显示名 |
| `serverUrl` | string | `""` | 示例请求的基础 URL |
| `pathPrefix` | string | `""` | 全局 URL 前缀（网关前缀、context-path 等） |
| `outPath` | string | 实际上必填 | 文档输出目录 |
| `allInOne` | boolean | `false` | 将所有接口合并为一个文档 |
| `allInOneDocFileName` | string | 自动生成 | 自定义合并文档文件名 |
| `coverOld` | boolean | `false` | 覆盖历史生成文件 |
| `isStrict` | boolean | `false` | 严格模式：要求公共 API 注释完整 |
| `packageFilters` | string | `""` | 包含包过滤（逗号分隔） |
| `packageExcludeFilters` | string | `""` | 排除包过滤 |
| `codePath` | string | Maven: `src/main/java` | 未显式配置 source 列表时的源码目录 |
| `sourceCodePaths` | array | 插件自动解析 | 参与解析的源码根目录 |
| `requestHeaders` | array | `[]` | 全局请求头 |
| `requestParams` | array | `[]` | 全局请求参数 |
| `requestExample` | boolean | `true` | 生成请求示例 |
| `responseExample` | boolean | `true` | 生成响应示例 |
| `showValidation` | boolean | `true` | 展示 JSR 校验注解信息 |
| `showAuthor` | boolean | `true` | 展示 JavaDoc `@author` |
| `language` | enum | 插件默认 | `ENGLISH` 或 `CHINESE` |

### 4.2 Java / OpenAPI 行为

| 键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `openApiTagNameType` | enum | `CLASS_NAME` | `CLASS_NAME`、`DESCRIPTION`、`PACKAGE_NAME` |
| `componentType` | enum | `RANDOM` | `RANDOM` 或 `NORMAL` |
| `inlineEnum` | boolean | `false` | 字段内联枚举说明 |
| `enumConvertor` | boolean | `false` | 请求示例中枚举值转换策略 |
| `allowSelfReference` | boolean | `false` | 允许自引用递归模型 |
| `recursionLimit` | number | `7` | 模型递归解析深度上限 |
| `displayActualType` | boolean | `false` | 展示泛型实际类型 |
| `showJavaType` | boolean | `false` | 文档显示 Java 类型 |
| `paramsDataToTree` | boolean | `false` | 参数树形渲染 |
| `requestFieldToUnderline` | boolean | `false` | 请求字段转 snake_case |
| `responseFieldToUnderline` | boolean | `false` | 响应字段转 snake_case |
| `sortByTitle` | boolean | `false` | 按标题排序接口 |
| `urlSuffix` | string | `""` | 全局 URL 后缀 |
| `framework` | string | 自动推断 | 框架提示（如 Spring） |

### 4.3 输出与调试

| 键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `md5EncryptedHtmlName` | boolean | `false` | HTML 文件名使用 MD5 |
| `adoc` | boolean | `false` | AsciiDoc 输出模式 |
| `style` | string | 内置 | 自定义样式 |
| `highlightStyleLink` | string | 内置 | 覆盖 highlight.js 样式地址 |
| `createDebugPage` | boolean | `false` | 生成调试页面 |
| `debugEnvName` | string | `""` | 调试环境名称 |
| `debugEnvUrl` | string | `""` | 调试环境 URL |
| `increment` | boolean | `false` | 增量生成模式 |
| `apiUploadNums` | number | `null` | OpenAPI 分片上传数量 |

### 4.4 字典 / 常量 / 包装

| 键 | 类型 | 说明 |
|---|---|---|
| `dataDictionaries` | array | 枚举数据字典 |
| `errorCodeDictionaries` | array | 基于枚举的错误码字典 |
| `errorCodes` | array | 手工错误码列表 |
| `apiConstants` | array | 常量类映射 |
| `customResponseFields` | array | 注入或覆盖响应字段 |
| `customRequestFields` | array | 注入或覆盖请求字段 |
| `apiObjectReplacements` | array | 类型替换规则 |
| `responseBodyAdvice` | object | 响应包装类与 `dataField` |
| `requestBodyAdvice` | object | 请求包装类与 `dataField` |
| `ignoreRequestParams` | array | 忽略的请求参数名 |
| `groups` | array | API 分组 |
| `rpcApiDependencies` | array | RPC 依赖扫描配置 |
| `jmeter` | object | JMeter 生成配置 |

## 5. 常见嵌套结构

### 5.1 `sourceCodePaths[]`

```json
{
  "path": "src/main/java",
  "desc": "main sources"
}
```

### 5.2 `requestHeaders[]` / `requestParams[]`

```json
{
  "name": "Authorization",
  "type": "string",
  "desc": "Bearer token",
  "required": false,
  "since": "-",
  "paramIn": "header",
  "pathPatterns": "/api/.*",
  "excludePathPatterns": "/api/internal/.*"
}
```

### 5.3 `revisionLogs[]`

```json
{
  "version": "1.0",
  "status": "released",
  "author": "team",
  "revisionTime": "2026-07-06",
  "remarks": "initial release"
}
```

### 5.4 `dataDictionaries[]`

```json
{
  "title": "Order Status",
  "enumClassName": "com.example.enums.OrderStatus",
  "codeField": "code",
  "descField": "desc"
}
```

### 5.5 `errorCodeDictionaries[]`

```json
{
  "enumClassName": "com.example.enums.ErrorCode",
  "codeField": "code",
  "descField": "message"
}
```

### 5.6 `responseBodyAdvice` / `requestBodyAdvice`

```json
{
  "className": "com.example.common.ApiResponse",
  "dataField": "data"
}
```

## 6. 运行命令

### Maven

```bash
mvn -Dfile.encoding=UTF-8 stagger:html
mvn -Dfile.encoding=UTF-8 stagger:openapi
```

### Gradle

```bash
./gradlew restHtml
./gradlew openApi
./gradlew restHtml -Pstagger.configFile=src/main/resources/stagger.json
```

## 7. 排查建议

### 7.1 `outPath` 为空

`outPath` 不能为空，否则插件执行失败。

### 7.2 找不到配置文件

- Maven 在多模块里遇到缺失配置时会 warn 并跳过非 Web 模块。
- Gradle 在扩展和命令行都未设置时，回退到 `./src/main/resources/default.json`。

### 7.3 枚举类或包装类加载失败

`enumClassName`、`valuesResolverClass`、BodyAdvice 的 `className` 会通过运行时 classloader 解析。
请确保相关类在当前模块的 classpath 上。

## 8. 权威来源

本页对齐以下源码：

- `stagger-core/src/main/java/com/github/hsindumas/stagger/model/ApiConfig.java`
- `stagger-gradle-plugin/src/main/java/com/github/hsindumas/stagger/gradle/util/GradleUtil.java`
- `stagger-maven-plugin/src/main/java/com/github/hsindumas/stagger/maven/plugin/util/MojoUtils.java`
