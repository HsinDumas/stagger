# Stagger

[English Version](./README.md)

> **Let Swagger stagger. Keep your source code pristine.**

Swagger 想让你的业务代码背文档注解。
Stagger 说不。

Stagger 是一款基于 JavaParser 静态分析的 **零侵入、零注解** API 文档生成工具。
它源于 smart-doc 思路并独立维护，重点面向现代 Java 工程实践（JDK 25 toolchain、Gradle 9.x、Spring Boot 4 兼容）。

## ⚡ 一分钟理解 Stagger

- 文档不要侵入领域模型。
- 直接解析源码结构与 JavaDoc。
- 在构建期输出 OpenAPI 3.1、Markdown、离线 HTML。
- 对用户保持 Maven/Gradle 双一等支持。

## 🙏 致敬 smart-doc

Stagger 的起点来自 [smart-doc](https://github.com/smart-doc-group/smart-doc)。

- smart-doc 在 Java 生态中率先走通了“低侵入文档生成”这条路。
- Stagger 继承的是这套理念：尽量不污染业务代码，通过源码分析生成文档。
- 我们向 [shalousun](https://github.com/shalousun) 与所有 smart-doc 贡献者致敬。

Stagger 不是对 smart-doc 的否定，而是在“现代化技术栈 + 迁移透明度”方向上的延伸维护。

## 💡 为什么叫 Stagger？

当文档系统开始反向塑造领域模型时，代码就失去边界了。

Stagger 的选择是把文档生成放在构建期源码分析，而不是业务注解堆砌。

### Before / After

```java
// Before：文档框架驱动业务模型形态
@Schema(description = "创建订单请求")
public class CreateOrderRequest {
    @Schema(description = "客户ID")
    private String customerId;
}

// After（Stagger）：模型保持干净，文档交给 JavaDoc + 源码分析
/** 创建订单请求 */
public class CreateOrderRequest {
    /** 客户ID */
    private String customerId;
}
```

核心原则：

- 🚫 **零侵入** - 业务代码无需引入第三方文档注解。
- 📝 **JavaDoc + 源码元信息** - 从源码结构和注释推导文档。
- ⚡ **现代 Java 优先** - 重点支持 JDK 25 toolchain 与新框架兼容。
- 🔄 **多格式输出** - 一次构建可生成 OpenAPI 3.1、Markdown、离线 HTML。

## ✨ 功能对比

| 特性 | Stagger | Swagger | springdoc-openapi |
|------|---------|---------|------------------|
| 零代码侵入 | ✅ | ❌ | ❌ |
| 纯 JavaDoc | ✅ | ❌ | ❌ |
| 构建时生成 | ✅ | ❌ | ❌ |
| Gradle 9.x | ✅ | ✅ | ✅ |
| OpenAPI 3.1 | ✅ | ✅ | ✅ |

## 🔍 与 smart-doc 的差异化

理念一致，工程路线不同：

| 维度 | 上游基线 | Stagger（本仓库） |
|------|--------------------|-------------------|
| 项目定位 | 上游通用基线 | 独立维护分支，明确现代化路线图 |
| 构建体系 | Maven 为主 | Gradle Monorepo 为主（同时提供 Maven/Gradle 一等插件） |
| 解析架构 | QDox 体系 | JavaParser SourceModel 抽象 |
| JDK 策略 | 传统基线 | JDK 25 toolchain 构建，产物保持向下兼容目标 |
| Spring 侧重点 | 常规 Spring 生态 | 强化 Spring Boot 4 与新注解形态兼容 |
| 迁移透明度 | N/A | 公开迁移记录：`docs/CODEX_MIGRATION_PLAN.md` |

Stagger 在仓库内部使用 Gradle 构建，但不会强迫用户切换构建工具；Maven 与 Gradle 用户体验都保持一等支持。

详细迁移日志与贡献验证命令，请参考 `docs/CODEX_MIGRATION_PLAN.md` 与 `CONTRIBUTING.md`。

## 📚 Wiki

- GitHub Wiki：https://github.com/HsinDumas/stagger/wiki
- 文档索引：`docs/wiki/README.md`
- `stagger.json` 配置参考（中文）：`docs/wiki/stagger-json-cn.md`
- `stagger.json` configuration reference (EN)：`docs/wiki/stagger-json.md`

## 🚀 快速开始

请使用 https://github.com/HsinDumas/stagger/releases 中的最新发布版本（标签格式：`vX.Y.Z`），并将下面的 `REPLACE_WITH_LATEST_RELEASE` 替换为实际版本号（例如：`3.2.2`）。

### Maven

```xml
<plugin>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>stagger-maven-plugin</artifactId>
    <version>REPLACE_WITH_LATEST_RELEASE</version>
    <configuration>
        <configFile>${project.basedir}/src/main/resources/stagger.json</configFile>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>html</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

```bash
mvn -Dfile.encoding=UTF-8 stagger:html
```

### Gradle

```kotlin
plugins {
    id("com.github.hsindumas.stagger") version "REPLACE_WITH_LATEST_RELEASE"
}

stagger {
    configFile = file("src/main/resources/stagger.json")
}
```

```bash
./gradlew restHtml
# 可选：命令行覆盖配置文件路径
./gradlew restHtml -Pstagger.configFile=src/main/resources/stagger.json
```

### 最小 `stagger.json` 示例

```json
{
  "allInOne": true,
  "isStrict": false,
  "outPath": "build/stagger"
}
```

## 📦 项目结构

这是 stagger 的现代化维护分支，采用 Monorepo 结构：

```
stagger/
├── stagger-core/           # 核心文档生成引擎
├── stagger-maven-plugin/   # Maven 插件
├── stagger-gradle-plugin/  # Gradle 插件
├── build.gradle.kts        # 根 Gradle 构建
└── settings.gradle.kts     # 模块配置
```

## 🔧 构建

```bash
# 构建全部模块
./gradlew clean build -x test

# 构建特定模块
./gradlew :stagger-core:build -x test
./gradlew :stagger-maven-plugin:build -x test

# 构建 Gradle 插件
./gradlew :stagger-gradle-plugin:build -x test
```

## 🐛 主要改进

### Gradle 9.x 兼容性修复

```java
// ❌ 旧方式（Gradle 9.0 已移除）
project.getConvention()
    .getPlugin(JavaPluginConvention.class)
    .getSourceSets();

// ✅ 新方式（Gradle 7.1+）
project.getExtensions()
    .getByType(JavaPluginExtension.class)
    .getSourceSets();
```

### 统一 Monorepo 管理

原 stagger 分散在 4 个独立仓库，现已统一为单仓库，便于版本同步和维护。

## 📄 许可证

Apache License 2.0 - 详见 LICENSE 文件

## 👏 致谢

- **上游项目**: [smart-doc](https://github.com/smart-doc-group/smart-doc)
- **当前维护**: [HsinDumas](https://github.com/HsinDumas)

## 🤝 贡献

欢迎提交 Pull Request！大改动请先开 Issue 讨论。

---

**工程侧重点**: Gradle Monorepo + JDK 25 toolchain + 完整 JavaParser 集成
