# stagger.json Configuration Reference

This page documents the `stagger.json` file consumed by both Maven and Gradle plugins.

## 1. Where `stagger.json` Is Read

- Maven plugin:
  - `<configFile>${project.basedir}/src/main/resources/stagger.json</configFile>`
  - Can also be overridden by plugin parameter `configFile`.
- Gradle plugin:
  - `stagger { configFile = file("src/main/resources/stagger.json") }`
  - Can also be overridden at runtime with `-Pstagger.configFile=...`

If no explicit file is provided, plugins fall back to `./src/main/resources/default.json`.

## 2. Minimal Working Example

```json
{
  "allInOne": true,
  "isStrict": false,
  "outPath": "build/stagger"
}
```

## 3. Recommended Starter Template

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

## 4. Field Reference

Only commonly used fields are listed first. Additional fields are grouped afterward.

### 4.1 Common Fields

| Key | Type | Default | Description |
|---|---|---|---|
| `projectName` | string | module name | Project display name in generated docs |
| `serverUrl` | string | `""` | Base URL used by rendered examples |
| `pathPrefix` | string | `""` | Global URL prefix (for servlet context path, gateway prefix, etc.) |
| `outPath` | string | required in practice | Output directory for generated docs |
| `allInOne` | boolean | `false` | Merge all API docs into a single document |
| `allInOneDocFileName` | string | auto-generated | Custom all-in-one output filename |
| `coverOld` | boolean | `false` | Overwrite existing generated docs |
| `isStrict` | boolean | `false` | Require complete JavaDoc comments on public APIs |
| `packageFilters` | string | `""` | Include package filter (comma-separated patterns) |
| `packageExcludeFilters` | string | `""` | Exclude package filter |
| `codePath` | string | Maven: `src/main/java` | Main source path if explicit source list is not used |
| `sourceCodePaths` | array | auto-resolved by plugin | Source roots used for parser scanning |
| `requestHeaders` | array | `[]` | Global request headers appended to endpoints |
| `requestParams` | array | `[]` | Global request params appended to endpoints |
| `requestExample` | boolean | `true` | Generate request examples |
| `responseExample` | boolean | `true` | Generate response examples |
| `showValidation` | boolean | `true` | Show JSR validation annotations in docs |
| `showAuthor` | boolean | `true` | Show JavaDoc `@author` |
| `language` | enum | plugin default | `ENGLISH` or `CHINESE` |

### 4.2 Java / OpenAPI Behavior

| Key | Type | Default | Notes |
|---|---|---|---|
| `openApiTagNameType` | enum | `CLASS_NAME` | `CLASS_NAME`, `DESCRIPTION`, `PACKAGE_NAME` |
| `componentType` | enum | `RANDOM` | `RANDOM` or `NORMAL` |
| `inlineEnum` | boolean | `false` | Inline enum description in fields |
| `enumConvertor` | boolean | `false` | Use enum value conversion strategy in request examples |
| `allowSelfReference` | boolean | `false` | Allow self-reference recursion models |
| `recursionLimit` | number | `7` | Max recursion depth when parsing models |
| `displayActualType` | boolean | `false` | Display generic actual type |
| `showJavaType` | boolean | `false` | Show Java type in docs |
| `paramsDataToTree` | boolean | `false` | Render params in tree structure |
| `requestFieldToUnderline` | boolean | `false` | Convert request field names to snake_case |
| `responseFieldToUnderline` | boolean | `false` | Convert response field names to snake_case |
| `sortByTitle` | boolean | `false` | Sort APIs by title |
| `urlSuffix` | string | `""` | Global URL suffix |
| `framework` | string | inferred | Framework hint (Spring, etc.) |

### 4.3 Output and Debug Experience

| Key | Type | Default | Notes |
|---|---|---|---|
| `md5EncryptedHtmlName` | boolean | `false` | MD5-hash html filename |
| `adoc` | boolean | `false` | AsciiDoc output mode |
| `style` | string | built-in | Custom style hint |
| `highlightStyleLink` | string | built-in | Override highlight.js style link |
| `createDebugPage` | boolean | `false` | Generate debug page |
| `debugEnvName` | string | `""` | Debug environment label |
| `debugEnvUrl` | string | `""` | Debug environment URL |
| `increment` | boolean | `false` | Incremental generation mode |
| `apiUploadNums` | number | `null` | OpenAPI split upload count |

### 4.4 Dictionary / Constants / Advice

| Key | Type | Description |
|---|---|---|
| `dataDictionaries` | array | Enum dictionary definitions |
| `errorCodeDictionaries` | array | Enum-based error code dictionaries |
| `errorCodes` | array | Manual error code list |
| `apiConstants` | array | Constants class mappings |
| `customResponseFields` | array | Inject or override response fields |
| `customRequestFields` | array | Inject or override request fields |
| `apiObjectReplacements` | array | Type replacement rules |
| `responseBodyAdvice` | object | Wrapper class and `dataField` for responses |
| `requestBodyAdvice` | object | Wrapper class and `dataField` for requests |
| `ignoreRequestParams` | array | Ignore request params by name |
| `groups` | array | Group APIs into multiple sections |
| `rpcApiDependencies` | array | RPC dependency scanning config |
| `jmeter` | object | JMeter generation settings |

## 5. Common Nested Structures

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

## 6. Build Commands

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

## 7. Troubleshooting

### 7.1 `outPath` empty

`outPath` must be provided. Plugin execution fails when output path is empty.

### 7.2 config file not found

- Maven warns and skips non-web modules when config file is missing.
- Gradle falls back to `./src/main/resources/default.json` when extension and property are not provided.

### 7.3 Enum class cannot be loaded

Fields like `enumClassName`, `valuesResolverClass`, and BodyAdvice `className` are loaded via runtime classloader.
Ensure classes are on compile/runtime classpath.

## 8. Source of Truth

This reference is aligned to:

- `stagger-core/src/main/java/com/github/hsindumas/stagger/model/ApiConfig.java`
- `stagger-gradle-plugin/src/main/java/com/github/hsindumas/stagger/gradle/util/GradleUtil.java`
- `stagger-maven-plugin/src/main/java/com/github/hsindumas/stagger/maven/plugin/util/MojoUtils.java`
