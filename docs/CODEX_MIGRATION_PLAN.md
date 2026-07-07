# Stagger 迁移方案与任务清单（转交 Codex）

> 交付对象：Codex（自主执行代理）
> 输出仓库：`HsinDumas/stagger`（fork 自 smart-doc）
> 编写时间：2026-07-01（rev2）
> 主基调：
> - **顶层适配 JDK 21 + Spring Boot 4.x**，构建 toolchain=21。
> - **单 jar 向下兼容**：`--release 17` 一把梭；产物一个 jar 即可覆盖 JDK 17~25。
> - **QDox → JavaParser**（不保留 QDox provider，M5 直接删）。
> - **Maven → Gradle 多模块**。
> - **groupId / packageName 全部改为 `com.github.hsindumas`**。
> - **改动过的文件更新 `@author HsinDumas`**（保留原作者行）。

> 状态补充（2026-07-03）：core/plugin 构建配置中的 QDox 依赖声明已移除；解析兼容路径由 SourceModel 驱动的 fallback delegate 承接，不再要求运行期提供 QDox。

---

## 0. 现状快照（Codex 请先阅读）

| 维度 | 现状 | 影响 |
|---|---|---|
| 源码规模 | Java 文件 324；stagger-core 230、stagger-maven-plugin 36、stagger-gradle-plugin 39 | 迁移面较大，需要分批 PR |
| 包命名 | `com.github.hsindumas.stagger.*`（core + maven-plugin，285 文件）；`com.github.hsindumas.stagger.gradle.*`（gradle-plugin，39 文件）；已无 `smartdoc/power/ly.stagger` 遗留 | **全量 rename 到 `com.github.hsindumas.stagger.*`** |
| 目录 vs package | `stagger-core/src/main/java/io/github/smartdoc/**` 目录仍是老名，但文件顶部 `package com.github.hsindumas.stagger.*` → 目录与包不一致 | 一次性物理 rename 到 `com/github/hsindumas/stagger/**` |
| QDox 依赖 | `com.ly.stagger:qdox:2.0.3.5`（fork，包名保留 `com.thoughtworks.qdox`）；79+ 文件直接 import；散布式使用；**唯一集中封装** 只有 `JavaProjectBuilderHelper.java` | 需要新造抽象门面 + JavaParser 实现；**QDox 直接下线，不保留 provider** |
| QDox API 覆盖面 | `JavaProjectBuilder / JavaClass / JavaMethod / JavaField / JavaParameter / JavaAnnotation / JavaType / JavaTypeVariable / JavaGenericDeclaration / JavaParameterizedType / DocletTag / expression.* / impl.* / directorywalker.* / parser.ParseException / library.ClassLibraryBuilder` | 抽象层要覆盖上述全部语义 |
| EE 命名空间 | 无任何 `import javax/jakarta`；仅硬编码字符串出现在 `JavaClassValidateUtil / DefaultClassConstants / JakartaJaxrsAnnotations / JAXRSAnnotations(@Deprecated) / ValidatorAnnotations(@Deprecated)` | 已完成双写，仅需查漏补缺 |
| Spring 版本硬编码 | 通过全限定类名字符串匹配（`org.springframework.web.bind.annotation.*`） | 无版本硬编码，兼容 Boot 4 主要靠源码分析层 |
| 构建 | 根 `pom.xml` + `stagger-core/pom.xml` + `stagger-maven-plugin/pom.xml`；`stagger-gradle-plugin/build.gradle` 独立 Groovy DSL；父 pom `java.version=1.8` | 全部换 Gradle（Kotlin DSL） |
| 代码风格 | `palantir-java-format`（通过 Spotless 任务接入，`-Xdoclint:all`）；`.editorconfig` 存在 | Gradle 需集成 `com.palantir.java-format`；`@author`/Javadoc 必须完整 |
| @author 覆盖率 | 310 / 324 (95.7%)；Top 作者：`yu / linwumingshi / xingzi / yu3.sun / chenchuxin / Fio / shalousun ...` | 变更文件需追加 `@author HsinDumas`（保留原作者行） |
| groupId 现状 | `com.github.hsindumas`（Maven 侧）+ `com.ly.stagger`（Gradle 插件侧） | **统一改为 `com.github.hsindumas`** |

---

## 1. 目标

1. **单一 jar，编译目标 JDK 17**
   - Gradle toolchain 用 JDK 21（保证在最新 JDK 上编译）。
   - `tasks.withType<JavaCompile> { options.release = 17 }`。
   - 产物 **一个 jar**，可在 JDK 17 / 21 / 25 上原生运行。
   - 源码里禁用 JDK 18+ 特有 API（可用 `-Xlint:all -Werror` 兜底）。
   - stagger 能识别的用户源码 JDK 版本与自身运行 JDK **解耦**：JavaParser 语言级别设为 `JAVA_21`（含 preview 时按需切），保证能解析用户 JDK 21 语法。
   - 若日后需要下探 JDK 8，把 `options.release = 17` 改成 `8` 即可（放弃 record / var / switch pattern），无需 MRJAR。
2. **Spring Boot 4.x 主线**
   - 识别与解析 Spring Web 6.x（Boot 3.x）与 Spring Web 7.x（Boot 4.x）的注解形态。
   - JAX-RS / Servlet / Validation 兼容 Jakarta，保留 javax 分支标 `@Deprecated`。
3. **JavaParser 替换 QDox**
   - 新增 `com.github.hsindumas.stagger.source` 抽象层。
   - **只保留一份 JavaParser Provider**；QDox 依赖与所有 `com.thoughtworks.qdox.*` import 一并移除。
4. **Gradle 替换 Maven**
   - 根工程改为 Gradle 8.x 多模块（Kotlin DSL）。
   - M5 完成时删除全部 `pom.xml`；不保留 Maven 构建。
5. **groupId 统一为 `com.github.hsindumas`**
   - Maven Central 坐标：`com.github.hsindumas:stagger-core / stagger-maven-plugin / stagger-gradle-plugin`。
   - Gradle 插件 ID：`com.github.hsindumas.stagger`。
6. **包名统一为 `com.github.hsindumas.stagger.*`**
   - 全部 324 个 Java 文件目录 + package 声明同步改造。
7. **`@author` 更新**
   - 所有被本迁移**实际改动过内容**的 `.java` 文件，在原 `@author` 行下追加 `@author HsinDumas`；已有 `@author HsinDumas` 不重复。

---

## 2. 里程碑总览

| 里程碑 | 目标 | 门槛 |
|---|---|---|
| **M1 目录 + 包 + groupId 统一** | 全项目 rename 到 `com.github.hsindumas.stagger.*`；三个模块 groupId 统一 `com.github.hsindumas` | Maven 与旧 Gradle 都能构建通过；无 `smartdoc / com.github.hsindumas.stagger / com.ly.doc / com.ly.stagger / com.github.hsindumas` 残留 |
| **M2 Gradle 化** | 根 `settings.gradle.kts` + `build.gradle.kts` + `libs.versions.toml` + `buildSrc`；三模块统一 Gradle | `./gradlew build publishToMavenLocal` 通过；产物 GAV 为新的 `com.github.hsindumas:*`；旧 Maven pom 保留以便对比 |
| **M3 JDK 21 编译 + release 17 + Boot 4.x 适配** | toolchain=21，`--release 17`；补 Boot 4 注解识别；查漏补缺 jakarta 常量 | JDK 21 编译通过；产物在 JDK 17 环境下能加载运行；Boot 3/4 样例都能生成文档 |
| **M4 JavaParser 抽象层** | 新增 `com.github.hsindumas.stagger.source` 抽象；JavaParser 唯一实现；79+ 业务文件切换到抽象层 | 抽象层单测全绿；样例项目通过 |
| **M5 QDox + Maven 下线** | 移除 QDox 依赖与所有 `com.thoughtworks.qdox` import；删除全部 `pom.xml`；`@author HsinDumas` 全量落地；CI/README 更新 | 仓库内 `rg com.thoughtworks.qdox`、`rg pom.xml` 均为空；`./gradlew build` 全绿 |

---

## 3. 详细方案

### 3.1 M1 目录 + 包 + groupId 统一

**目录 rename（stagger-core）**
```
stagger-core/src/main/java/io/github/smartdoc/**   →   stagger-core/src/main/java/com/github/hsindumas/stagger/**
stagger-core/src/test/java/io/github/smartdoc/**   →   stagger-core/src/test/java/com/github/hsindumas/stagger/**   （若存在）
```
> 注意：core 里还有一个骨架目录 `stagger-core/src/main/java/com/github/stagger/**`（前次未完成迁移的残留）。若里面有文件，一并 rename 到 `com/github/hsindumas/stagger/**` 并合并；若为空目录，`git rm -r` 清理。

**目录 rename（stagger-maven-plugin）**
```
stagger-maven-plugin/src/main/java/io/github/smartdoc/**   →   stagger-maven-plugin/src/main/java/com/github/hsindumas/stagger/maven/**
stagger-maven-plugin/src/main/java/com/github/**            →   合并到上一路径（若有残留）
```

**目录 rename（stagger-gradle-plugin）**
```
stagger-gradle-plugin/src/main/java/com/ly/doc/gradle/**   →   stagger-gradle-plugin/src/main/java/com/github/hsindumas/stagger/gradle/**
```

**package 声明批量替换（在完成 `git mv` 之后执行）**

用一次性脚本（幂等）：
```bash
# 在项目根执行
rg -l 'package io\.github\.stagger'        -g '*.java' | xargs sed -i '' 's/package io\.github\.stagger/package com.github.hsindumas.stagger/g'
rg -l 'package io\.github\.smartdoc'       -g '*.java' | xargs sed -i '' 's/package io\.github\.smartdoc/package com.github.hsindumas.stagger/g'
rg -l 'package com\.github\.stagger'       -g '*.java' | xargs sed -i '' 's/package com\.github\.stagger/package com.github.hsindumas.stagger/g'
rg -l 'package com\.ly\.doc\.gradle'       -g '*.java' | xargs sed -i '' 's/package com\.ly\.doc\.gradle/package com.github.hsindumas.stagger.gradle/g'

# import 同步替换
rg -l 'import io\.github\.stagger'         -g '*.java' | xargs sed -i '' 's/import io\.github\.stagger/import com.github.hsindumas.stagger/g'
rg -l 'import io\.github\.smartdoc'        -g '*.java' | xargs sed -i '' 's/import io\.github\.smartdoc/import com.github.hsindumas.stagger/g'
rg -l 'import com\.github\.stagger'        -g '*.java' | xargs sed -i '' 's/import com.github.hsindumas.stagger/import com.github.hsindumas.stagger/g'
rg -l 'import com\.ly\.doc\.gradle'        -g '*.java' | xargs sed -i '' 's/import com\.ly\.doc\.gradle/import com.github.hsindumas.stagger.gradle/g'
```
> macOS 的 `sed` 用 `-i ''`；Linux CI 上用 `-i`。Codex 请根据实际执行环境选择。

**groupId 与其他配置同步改造**

- 根 `pom.xml`、`stagger-core/pom.xml`、`stagger-maven-plugin/pom.xml`：`<groupId>com.github.hsindumas</groupId>` → `com.github.hsindumas`；`<artifactId>` 保持不变；`<parent>` 引用同步。
- `stagger-maven-plugin/pom.xml` 里 `com.github.hsindumas:stagger-core:4.0.0` → `com.github.hsindumas:stagger-core:${revision}`。
- `stagger-gradle-plugin/build.gradle`：
  - `group 'com.ly.stagger'` → `group 'com.github.hsindumas'`。
  - `implementation group: 'com.ly.stagger', name: 'stagger', version: '3.1.2'` → `implementation "com.github.hsindumas:stagger-core:${version}"`。
  - `gradlePlugin.plugins.smartdocPlugin` 重命名为 `staggerPlugin`；`id = 'com.ly.stagger'` → `id = 'com.github.hsindumas.stagger'`；`implementationClass = 'com.github.hsindumas.stagger.gradle.plugin.SmartDocPlugin'` → `'com.github.hsindumas.stagger.gradle.plugin.StaggerPlugin'`（同时 `SmartDocPlugin.java` 类名重命名为 `StaggerPlugin`）。
- `META-INF/services/*`：更新 SPI 文件里的实现类全限定名（`com.github.hsindumas.stagger.template.IDocBuildTemplate` → `com.github.hsindumas.stagger.template.IDocBuildTemplate`），文件名也一并 rename。
- 资源模板里若引用了旧命名或模板引擎相关标识（FreeMarker / properties / html），一并搜索替换：
  ```bash
  rg 'io\.github\.smartdoc|io\.github\.stagger|com\.ly\.doc\.gradle' -g '!*.md' -g '!*.txt'
  ```
- `README.md` / `README_CN.md` / `CHANGELOG.md`：groupId、插件 ID、包名的示例代码块同步更新。

**验收**
- `rg -l 'io\.github\.smartdoc|io\.github\.stagger|com\.ly\.doc\.gradle|com\.ly\.stagger|com\.github\.shalousun'` 无结果（`.md` 历史记录除外）。
- 旧 Maven `./mvnw -f pom.xml install -pl stagger-core,stagger-maven-plugin` 通过。
- 旧 Gradle `cd stagger-gradle-plugin && ./gradlew build` 通过。

---

### 3.2 M2 Gradle 化

**目录布局**
```
stagger/
├── settings.gradle.kts
├── build.gradle.kts                 # 根，公共约定
├── gradle/
│   └── libs.versions.toml           # 版本目录
├── buildSrc/
│   └── src/main/kotlin/
│       └── stagger.java-conventions.gradle.kts
├── stagger-core/
│   └── build.gradle.kts
├── stagger-maven-plugin/
│   └── build.gradle.kts
└── stagger-gradle-plugin/
    └── build.gradle.kts
```

**关键约定（`buildSrc/.../stagger.java-conventions.gradle.kts`）**
```kotlin
plugins {
    `java-library`
    checkstyle
    id("com.palantir.java-format")
}

group = "com.github.hsindumas"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)          // ← 单 jar 向下兼容的关键
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addBooleanOption("Xdoclint:all", true)
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:",
        )
    }
    isFailOnError = true
}

checkstyle {
    toolVersion = "10.20.1"
    configFile = rootProject.file("checkstyle/google_checks.xml")
}
```

**stagger-maven-plugin 特殊处理**
- 依赖 `com.gradleup.maven-plugin` 或 `de.benediktritter.maven-plugin-development`（社区最新可用版本）在 Gradle 里生成 `plugin.xml`。
- Maven 侧依赖：`org.apache.maven:maven-plugin-api:3.9.9` / `maven-plugin-annotations:3.13.1` / `maven-core:3.9.9`。
- 产物 packaging=`maven-plugin`。

**stagger-gradle-plugin**
```kotlin
plugins {
    id("stagger.java-conventions")
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.0"
}
gradlePlugin {
    website.set("https://github.com/HsinDumas/stagger")
    vcsUrl.set("https://github.com/HsinDumas/stagger")
    plugins {
        create("staggerPlugin") {
            id = "com.github.hsindumas.stagger"
            implementationClass = "com.github.hsindumas.stagger.gradle.plugin.StaggerPlugin"
            displayName = "stagger gradle plugin"
            description = "Generate REST API docs from Java source"
            tags.set(listOf("stagger", "documentation", "openapi", "smart-doc"))
        }
    }
}
```

**libs.versions.toml（骨架）**
```toml
[versions]
javaparser        = "3.26.4"
freemarker        = "2.3.34"
gson              = "2.13.2"
datafaker         = "1.9.0"
slf4j             = "2.0.17"
jgit              = "6.9.0.202403050737-r"   # 升级到 JDK 17+ 友好版本
commonUtil        = "2.2.9"
junit             = "5.14.1"
mockito           = "4.11.0"
springBoot4Sample = "4.0.0-M2"                # 仅样例项目用

[libraries]
javaparser-core         = { module = "com.github.javaparser:javaparser-core",              version.ref = "javaparser" }
javaparser-symbolsolver = { module = "com.github.javaparser:javaparser-symbol-solver-core", version.ref = "javaparser" }
freemarker   = { module = "org.freemarker:freemarker",              version.ref = "freemarker" }
gson         = { module = "com.google.code.gson:gson",              version.ref = "gson" }
datafaker    = { module = "net.datafaker:datafaker",                version.ref = "datafaker" }
slf4j-api    = { module = "org.slf4j:slf4j-api",                    version.ref = "slf4j" }
jgit         = { module = "org.eclipse.jgit:org.eclipse.jgit",      version.ref = "jgit" }
common-util  = { module = "io.github.shalousun:common-util",        version.ref = "commonUtil" }
junit-bom    = { module = "org.junit:junit-bom",                    version.ref = "junit" }
mockito      = { module = "org.mockito:mockito-junit-jupiter",      version.ref = "mockito" }
```
> **注意**：`libs.versions.toml` 里 **不含 qdox**（M5 时删除，这里可以从头就不写；如需 M2~M4 期间平滑过渡见 3.4）。

**发布配置**
- `groupId = "com.github.hsindumas"`
- `artifactId` 保持：`stagger-core / stagger-maven-plugin / stagger-gradle-plugin`
- 发布到 Sonatype Central Portal（沿用旧的 `central-publishing-*` 思路，Gradle 侧改用 `id("io.github.gradle-nexus.publish-plugin")` 或 `com.vanniktech.maven.publish`）。

**验收**
- `./gradlew clean build --no-daemon --console=plain` 全绿。
- `./gradlew publishToMavenLocal` 生成的 pom 中 `groupId=com.github.hsindumas`。
- 保留 pom 用于旁路对比：`diff` 依赖树差异只应是 qdox → javaparser。

---

### 3.3 M3 JDK 21 编译 + release 17 + Boot 4.x 适配

**编译与运行边界**
- toolchain=JDK 21：Gradle 通过 `JavaLanguageVersion.of(21)` 自动下载或使用已装的 JDK 21。
- `--release 17`：字节码 major=61，运行时兼容 JDK 17/21/25；**源码不能使用 JDK 18+ 的 API**（如 `Instant.until` 新增 API、`Foreign Memory` 稳定版等），但可以使用 JDK 17 的 record、sealed、pattern for switch。
- **JavaParser 语言级别** 独立设置：`ParserConfiguration.setLanguageLevel(LanguageLevel.JAVA_21)`（如 JavaParser 尚未支持 JAVA_21，用最新可用的 `BLEEDING_EDGE`）。这样即使 stagger 自身跑在 JDK 17，也能解析用户 JDK 21 源码。
- 关闭对 `sun.*` / `Unsafe` 的引用（`Xlint:all` 已覆盖）。
- 三方库排查（在 JDK 21 运行时）：
  - `freemarker 2.3.x`：跑一个 smoke test，覆盖 html/md/xml 模板渲染路径。
  - `jgit 6.9.x`：升级到 6.x（旧的 5.13 在高 JDK 有告警）。
  - `common-util 2.2.9`：跑 smoke test；如遇 Unsafe 问题按需升级/替换。

**Spring Boot 4.x 识别**
- Boot 4 → Spring Framework 7；注解全限定名 `org.springframework.web.bind.annotation.*` 不变。
- 补齐以下注解识别（`SpringMvcAnnotations` / `SpringMvcRequestMappingHandler`）：
  - `org.springframework.web.service.annotation.HttpExchange`
  - `org.springframework.web.service.annotation.GetExchange` / `PostExchange` / `PutExchange` / `DeleteExchange` / `PatchExchange`
- 检查 reactive 返回类型（`Mono<T>` / `Flux<T>`）是否正确拆包（Boot 4 强化 reactive 路径）。
- 关注 `ProblemDetail` 返回体，纳入错误码/响应体处理链路。

**Jakarta 侧查漏**
```bash
rg 'javax\.(servlet|validation|persistence|ws\.rs|annotation)' -g '*.java'
```
凡是硬编码字符串的地方，必须存在同位置的 `jakarta.*` 版本；否则补齐。`JAXRSAnnotations` 与 `ValidatorAnnotations` 明确标 `@Deprecated(since = "5.0.0", forRemoval = false)`。

**验收**
- 一个位于 `samples/spring-boot-4x/` 的最小样例项目（Boot 4.0.0-M2 或最新 milestone），通过 `./gradlew :samples:spring-boot-4x:generateStaggerDoc` 产出 markdown。
- 样例项目在 JDK 21 上运行 stagger-core.jar；stagger-core.jar 自身编译目标 `--release 17`。
- 用 JDK 17 也跑一次 `samples/spring-boot-3x/`，产出与旧版基线一致。

---

### 3.4 M4 JavaParser 抽象层

**动机**
- QDox 只做词法/浅 AST，符号消解弱；对 record、sealed、pattern matching for switch 支持不完整。
- JavaParser + Symbol Solver 支持 JDK 17~25 语法；一步到位。

**抽象接口（新增包 `com.github.hsindumas.stagger.source`）**

```java
package com.github.hsindumas.stagger.source;

public interface SourceModel {
    SourceProject build(SourceScanRequest request);
}

public interface SourceProject {
    Optional<SourceClass> findClass(String qualifiedName);
    Collection<SourceClass> classes();
}

public interface SourceClass {
    String qualifiedName();
    String simpleName();
    List<SourceAnnotation> annotations();
    List<SourceMethod> methods();
    List<SourceField> fields();
    List<SourceTypeParam> typeParameters();
    Optional<SourceClass> superType();
    List<SourceClass> interfaces();
    String comment();               // javadoc 主体
    List<SourceDocletTag> docletTags();
    boolean isEnum();
    boolean isInterface();
    boolean isAnnotation();
    boolean isRecord();             // JavaParser 原生支持
    boolean isSealed();
    List<String> permittedSubtypes();
}

public interface SourceMethod { /* name, returnType, parameters, annotations, docletTags, ... */ }
public interface SourceField  { /* name, type, annotations, comment, initializer, ... */ }

public interface SourceAnnotation {
    String qualifiedName();
    Map<String, SourceAnnotationValue> members();
}
public interface SourceAnnotationValue {
    String asString();              // 统一：不带引号
    List<SourceAnnotationValue> asList();
    SourceAnnotation asAnnotation();
    SourceType asType();
    Object raw();
}
public interface SourceType {
    String qualifiedName();
    List<SourceType> typeArguments();
    boolean isArray();
    boolean isPrimitive();
    boolean isWildcard();
    Optional<SourceType> wildcardBound();
}
public interface SourceDocletTag {
    String name();
    String value();
    Map<String, String> namedParameters();
}
```

**Provider**
- **只有一个**：`com.github.hsindumas.stagger.source.javaparser.JavaParserSourceModel`。
- 通过 `ServiceLoader<SourceModel>` 注册；无需切换开关（M5 之后再无 QDox 分支）。
- JavaParser 版本 `3.26.x`；`javaparser-symbol-solver-core` 用于类型消解。
- 抽象层 **不暴露** `com.github.javaparser.*` 类型，业务代码只依赖 `com.github.hsindumas.stagger.source.*`。

**改造策略（M4 分阶段）**
1. **M4-a**：写抽象层接口 + 单测（覆盖 20+ 核心用法：注解值、泛型、Doclet、record、sealed、嵌套类、内部注解值数组等）。
2. **M4-b**：JavaParser Provider 实现，通过所有抽象层单测；写一个"平行"的 QDox 参考实现**只在过渡分支内部使用**，用于对拍验证（不入主分支，或放 `stagger-core-test-fixtures`）。
3. **M4-c**：把 79+ 业务文件从 `import com.thoughtworks.qdox.*` 改为 `import com.github.hsindumas.stagger.source.*`；调用点按附录 A 映射改造。
4. **M4-d**：`JavaProjectBuilderHelper` → `SourceProjects`（返回 `SourceProject`）。
5. **M4-e**：跑 Boot 3.x / Boot 4.x / JAX-RS 样例，对比新旧 markdown 输出，允许字段顺序/描述细节差异，字段名、类型、路径、方法列表必须一致。

**验收**
- 抽象层单测全绿。
- 样例项目输出与迁移前 QDox 基线对拍通过（可接受的 diff 列在 PR 描述里）。

---

### 3.5 M5 QDox + Maven 下线

- 从 `libs.versions.toml`、所有 `build.gradle.kts` 移除 `qdox` 与相关内部适配代码。
- `rg 'com\.thoughtworks\.qdox' -g '*.java'` 必须为空。
- 删除根 `pom.xml`、`stagger-core/pom.xml`、`stagger-maven-plugin/pom.xml`；`stagger-gradle-plugin/build.gradle`（Groovy 版）由 Kotlin DSL 版替代后删除。
- CI（`.travis.yml` 或新的 GitHub Actions）改为 Gradle。建议同时新增 `.github/workflows/build.yml`（`setup-java 21`、`./gradlew build`）。
- 全量执行 `@author HsinDumas` 追加脚本（见附录 B），覆盖 M1~M5 期间所有被改动过的 `.java` 文件。
- `README.md` / `README_CN.md`：
  - 引入方式改为 Gradle DSL 与 Maven Central 新 groupId 示例。
  - "最低 JDK" 改为 JDK 17；"最高适配" 标注 JDK 21 + Spring Boot 4.x。
- `CHANGELOG.md` 新增 `5.0.0` 条目：
  - Breaking：groupId `com.github.hsindumas` → `com.github.hsindumas`；包名 `com.github.hsindumas.stagger.*` → `com.github.hsindumas.stagger.*`；最低 JDK 提升到 17；QDox 依赖移除；Maven 构建移除。

---

## 4. `@author` 处理规则（Codex 必读）

- 只针对**本次迁移实际修改过内容**的 `.java` 文件生效；纯 rename（仅目录移动、无文本变动）**不追加**。M1 之后每个 PR 结束前跑一次追加脚本，只处理 `git diff --diff-filter=AM` 的文件。
- 位置：在**最后一个** `@author` 行下追加一行；若原文件缺 `@author`，则在类 Javadoc 末尾（`*/` 之前）追加。
- 追加内容固定：` * @author HsinDumas`（保持 palantir-java-format 风格：星号后一个空格）。
- 幂等：文件已含 `@author HsinDumas` 时跳过。
- 建议 Codex 用 JavaParser 写一个小工具处理，比 `sed` 更安全（脚本示例见附录 B）。

---

## 5. 分批 PR 建议

| PR | 内容 | 大约文件数 |
|---|---|---|
| PR-1 | M1-a 目录 rename（`git mv`，不改 package/import） | 全量 324 |
| PR-2 | M1-b package/import 同步替换 + META-INF/services 更新 | 全量 324 |
| PR-3 | M1-c groupId 统一（pom + build.gradle + README） | ~15 配置 |
| PR-4 | M2-a Gradle 骨架（settings/build.gradle.kts、buildSrc、libs.versions.toml） | ~10 |
| PR-5 | M2-b stagger-core Gradle 化 | ~3 |
| PR-6 | M2-c stagger-maven-plugin Gradle 化（plugin.xml 生成） | ~3 |
| PR-7 | M2-d stagger-gradle-plugin Kotlin DSL 化 | ~3 |
| PR-8 | M3-a toolchain=21 / `--release 17` / 三方库升级（jgit 6.x） | ~5 |
| PR-9 | M3-b Spring Boot 4 注解识别 + Jakarta 查漏 | 10~20 |
| PR-10 | M4-a 抽象层接口 + 单测 | 抽象层 20+ 类 |
| PR-11 | M4-b JavaParser Provider 实现 + 对拍 QDox 参考实现（测试域） | 20+ |
| PR-12 | M4-c 79+ 业务文件切到抽象层 | 79+ |
| PR-13 | M5 移除 QDox + Maven + 收尾文档 + `@author HsinDumas` 全量补齐 | 清理为主 |

每个 PR：
- 分支命名 `codex/mN-<slug>`。
- 描述里列出验证命令（`./gradlew build`、样例任务等）。
- 触发追加 `@author HsinDumas` 脚本，diff 里应能看到 Javadoc 变化。

---

## 6. 验证与验收

- **编译**：`./gradlew clean build --no-daemon --console=plain` 在 toolchain=21 下通过；产物 class 文件用 `javap -v` 检查 `major version: 61`（对应 JDK 17）。
- **运行时兼容**：
  - `JAVA_HOME` 切到 JDK 17：`java -jar stagger-core-<v>.jar --help`（若有 CLI）或用样例调用能启动。
  - `JAVA_HOME` 切到 JDK 21：同上，且能识别用户 JDK 21 源码语法。
- **单元测试**：`./gradlew test`。
- **集成样例**：
  - `samples/spring-boot-3x/` 生成结果与旧基线一致（QDox → JavaParser 允许字段顺序差异）。
  - `samples/spring-boot-4x/` 生成结果符合预期。
  - `samples/jaxrs-jakarta/` 覆盖 Jakarta 分支。
- **格式**：`./gradlew checkFormatMain` + `./gradlew checkstyleMain` 全绿。
- **发布**：`./gradlew publishToMavenLocal` 输出的 pom `groupId=com.github.hsindumas`；依赖树无 `com.thoughtworks.qdox`。
- **静态扫描**：`rg 'com\.thoughtworks\.qdox|io\.github\.smartdoc|io\.github\.stagger|com\.ly\.doc\.gradle|com\.ly\.stagger|com\.github\.shalousun'` 全为空。

---

## 7. 风险与注意事项

1. **JavaParser 语义 vs QDox 语义**
   - JavaParser Symbol Solver 遇到缺依赖会抛异常。抽象层需要 `bestEffort` 模式：解析失败降级为字符串类型名。
2. **注解值引号差异**
   - QDox `AnnotationValue.getParameterValue()` 返回带引号字符串（`"foo"`）；JavaParser 返回不带引号。抽象层统一为**不带引号**，并在测试里覆盖。
3. **泛型 / 通配符表示**
   - `JavaParameterizedType / JavaTypeVariable / WildcardType` 与 JavaParser 的 `ClassOrInterfaceType / TypeParameter / WildcardType` 不完全一一对应。抽象层给出统一的 `SourceType` + `SourceTypeParam`。
4. **Record / Sealed / Pattern**
   - JavaParser 原生支持；抽象层暴露 `isRecord / isSealed / permittedSubtypes`；这也是切换的核心收益。
5. **FreeMarker / JGit / Unsafe 在 JDK 21**
  - FreeMarker 2.3.x 需 smoke test，重点覆盖模板语法兼容层与输出一致性。
   - JGit 升到 6.9.x。
6. **Maven 插件产物 packaging**
   - Gradle 侧用 `de.benediktritter.maven-plugin-development` 或 `com.gradleup.maven-plugin`（选社区活跃的一个）生成 `plugin.xml`，产物 packaging 保持 `maven-plugin`。
7. **Spring Boot 4 milestone 稳定性**
   - Boot 4 目前 milestone；只做识别与样例验证，**不要**把 Boot 4 作为 stagger 自身运行时依赖。
8. **palantir-java-format 与 `@author` 追加**
   - 追加脚本要保持 `* @author HsinDumas`（单空格、无多余空行）；建议用 JavaParser AST 修改而非 sed，避免误伤字符串字面量。
9. **groupId 迁移的下游影响**
   - `com.github.hsindumas:*` 老坐标在 Central 仍存在；新 `com.github.hsindumas:*` 首次发布需要在 Sonatype Central 上完成命名空间验证（域名/GitHub 二选一，`github.com/hsindumas` 更便捷）。M2 或 M5 之前需要 HsinDumas 完成 namespace claim。
10. **单 jar 向下兼容的边界**
    - `--release 17` 意味着 **JDK 17 是运行时最低门槛**；如果 Spring Boot 2 用户仍在 JDK 8/11 上使用 stagger，会加载失败。若确认要覆盖 JDK 8/11 用户，把 `options.release = 17` 改成 `8` 或 `11`，代价是放弃 record/var/switch pattern。这一决策请在 M3 开始前明确。当前默认按 17。

---

## 附录 A：QDox → SourceModel API 映射（节选）

| QDox | SourceModel | 备注 |
|---|---|---|
| `new JavaProjectBuilder()` | `SourceProjects.create()` | 单一 JavaParser Provider |
| `builder.addSourceTree(File)` | `SourceScanRequest.builder().addSourceRoot(Path)` | |
| `builder.getClassByName(String)` | `project.findClass(String)` → `Optional<SourceClass>` | 消除 null 分支 |
| `builder.getClasses()` | `project.classes()` | |
| `JavaClass.getFullyQualifiedName()` | `SourceClass.qualifiedName()` | |
| `JavaClass.getMethods()` | `SourceClass.methods()` | |
| `JavaClass.getFields()` | `SourceClass.fields()` | |
| `JavaClass.getAnnotations()` | `SourceClass.annotations()` | |
| `JavaClass.getTagsByName("param")` | `SourceClass.docletTags("param")` | |
| `JavaClass.getComment()` | `SourceClass.comment()` | |
| `JavaAnnotation.getType().getFullyQualifiedName()` | `SourceAnnotation.qualifiedName()` | |
| `JavaAnnotation.getNamedParameter("value")` | `SourceAnnotation.members().get("value")` | 返回 `SourceAnnotationValue` |
| `AnnotationValue.getParameterValue()` | `SourceAnnotationValue.asString()` / `asList()` / `asAnnotation()` / `asType()` | 显式类型；字符串不带引号 |
| `DocletTag.getName() / getValue()` | `SourceDocletTag.name() / value()` | |
| `JavaType.getFullyQualifiedName()` | `SourceType.qualifiedName()` | |
| `JavaParameterizedType.getActualTypeArguments()` | `SourceType.typeArguments()` | |
| `JavaTypeVariable` / `JavaGenericDeclaration` | `SourceTypeParam` | |
| `parser.ParseException` | `SourceParseException` | 自定义异常 |
| `directorywalker.DirectoryScanner` + `SuffixFilter` | `SourceScanRequest` 内部封装（`Files.walk`） | |
| `library.ClassLibraryBuilder` | 不暴露；Provider 内部处理（JavaParser 用 `TypeSolver`） | |

完整映射表在 M4-a 完成时随抽象层文档一起交付。

---

## 附录 B：`@author HsinDumas` 追加脚本参考

**推荐做法**：用 JavaParser AST 处理 `ClassOrInterfaceDeclaration` 的 Javadoc，找到 `@author` block tag 后追加。伪代码：

```java
CompilationUnit cu = StaticJavaParser.parse(path);
cu.getTypes().forEach(td -> {
    Optional<Javadoc> jd = td.getJavadoc();
    if (jd.isEmpty()) {
        td.setJavadocComment(new Javadoc(JavadocDescription.parseText("")).addBlockTag("author", "HsinDumas"));
        return;
    }
    Javadoc doc = jd.get();
    boolean hasAuthor = doc.getBlockTags().stream()
        .anyMatch(t -> t.getTagName().equals("author") && t.getContent().toText().trim().equals("HsinDumas"));
    if (!hasAuthor) {
        doc.addBlockTag("author", "HsinDumas");
        td.setJavadocComment(doc);
    }
});
Files.writeString(path, cu.toString());
```

**兜底 shell 版本**（适合快速跑，但有误伤风险）：
```bash
#!/usr/bin/env bash
set -euo pipefail
BASE_REF="${1:-origin/main}"

git diff --name-only --diff-filter=AM "${BASE_REF}"...HEAD -- '*.java' | while read -r f; do
  grep -q "@author HsinDumas" "$f" && continue
  if grep -q "^\s*\*\s*@author " "$f"; then
    # 在最后一个 @author 行下追加
    awk '
      /^\s*\*\s*@author / { auth[NR]=$0 }
      { line[NR]=$0 }
      END {
        max=0; for (n in auth) if (n+0>max) max=n+0
        for (i=1;i<=NR;i++) {
          print line[i]
          if (i==max) print " * @author HsinDumas"
        }
      }
    ' "$f" > "$f.new" && mv "$f.new" "$f"
  fi
done
```

---

## 附录 C：Codex 执行须知

- **非交互命令**：Gradle 加 `--no-daemon --console=plain`；Maven 加 `-B -ntp`；`sed` 与 `xargs` 加安全边界（`-r`、`--`）。
- **不动 git 配置**、不 force push、不 amend 已推送 commit。
- **每 PR 一分支**：`codex/mN-<slug>`；PR 描述含验证命令与验收截图/日志。
- **敏感文件**：`NOTICE / LICENSE / licenses/**` 不覆写；仅在 CHANGELOG 中说明变更。
- **Gradle wrapper**：如需升级，固定到已发布的稳定版本（当前建议 8.10.2）；不要指向 unreleased。
- **验证顺序**：`:stagger-core:compileJava` → `:stagger-core:test` → 整站 `build` → 样例任务。
- **失败即停**：任一验证失败先修复再进下一 PR。

---

## 决策记录（用户已确认）

- ✅ groupId 全统一为 `com.github.hsindumas`
- ✅ QDox 不保留（M5 直接删）
- ✅ 单 jar 向下兼容（`--release 17`，不做 MRJAR，不发多 classifier jar）
- ✅ 包名统一为 `com.github.hsindumas.stagger.*`

如后续 Codex 发现新的歧义（例如 `--release` 是否需要下调至 8/11、是否发布 SNAPSHOT 到 Central Portal 的分支策略），起 issue 与 HsinDumas 对齐再动手。
