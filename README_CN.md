# Stagger

[English Version](./README.md)

> **Let Swagger stagger. Keep your source code pristine.**

Stagger 是一款基于 JavaParser 静态分析的 **零侵入、零注解** API 文档自动化生成工具。
它面向现代 Java 技术栈（JDK 25+ / Spring Boot 4.x+），同时保持源码洁净。

## 💡 为什么叫 Stagger？

当 Swagger 用满屏幕的 `@Schema` 和 `@Operation` 注解将你的业务代码搅得一团糟时，它已经背离了"优雅"的初衷。在现代 Clean Code 的趋势下，这样的方案只能**步履蹒跚（Stagger）**向前。

Stagger 继承了优秀的静态解析理念，彻底摒弃注解流：

- 🚫 **零侵入** - 你的代码里不需要任何第三方注解。
- 📝 **纯 JavaDoc 解析** - 直接读取标准 JavaDoc 注释与源码元信息。
- ⚡ **面向未来** - 原生支持 JDK 25 新特性（Records、Pattern Matching）与 Spring Boot 4.x。
- 🔄 **多样输出** - 一键生成 OpenAPI 3.1、Markdown、离线 HTML 等多种格式。

## ✨ 功能对比

| 特性 | Stagger | Swagger | springdoc-openapi |
|------|---------|---------|------------------|
| 零代码侵入 | ✅ | ❌ | ❌ |
| 纯 JavaDoc | ✅ | ❌ | ❌ |
| 构建时生成 | ✅ | ❌ | ❌ |
| Gradle 9.x | ✅ | ✅ | ✅ |
| OpenAPI 3.1 | ✅ | ✅ | ✅ |

## 🔍 与 smart-doc 的差异

本仓库是一个现代化维护分支，在保持稳定性的同时做增量架构升级。和 smart-doc 相比，当前差异主要是：

| 维度 | smart-doc（上游） | Stagger（本仓库） |
|------|--------------------|-------------------|
| 构建体系 | Maven 为主 | Gradle Monorepo 为主（对用户同时提供 Maven 与 Gradle 一等插件） |
| JDK 策略 | 传统基线 | 使用 JDK 25 toolchain 构建，产物保持向下兼容目标 |
| 解析架构 | QDox 为中心 | 基于 JavaParser 的 SourceModel 抽象 |
| Spring 侧重点 | 常规 Spring 生态 | 更强调 Spring Boot 4 与新注解形态兼容 |
| 迁移透明度 | N/A | 公开迁移记录：`docs/CODEX_MIGRATION_PLAN.md` |

Stagger 在仓库内部使用 Gradle 构建 monorepo，但对 Maven 与 Gradle 用户都提供可直接用于生产的一等插件支持。

我们已完成从 QDox 到 JavaParser SourceModel 的替换，以提升对现代 Java 特性的兼容性。

详细迁移日志与面向贡献者的验证命令，请参考 `docs/CODEX_MIGRATION_PLAN.md` 与 `CONTRIBUTING.md`。

## 🚀 快速开始

### Maven

```xml
<plugin>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>stagger-maven-plugin</artifactId>
    <version>4.0.1</version>
    <executions>
        <execution>
            <goals>
                <goal>html</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Gradle

```gradle
plugins {
    id 'com.github.hsindumas.stagger' version '4.0.1'
}

stagger {
    sourceCodePath = "src/main/java"
    classAbsPath = "build/classes/java/main"
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
- **特别致敬**: 感谢 [shalousun](https://github.com/shalousun) 与所有 smart-doc 贡献者开创了零侵入文档生成这条路。
- **当前维护**: [HsinDumas](https://github.com/HsinDumas)

## 🤝 贡献

欢迎提交 Pull Request！大改动请先开 Issue 讨论。

---

**基于**: [smart-doc](https://github.com/smart-doc-group/smart-doc)  
**增强**: Gradle Monorepo + JDK 25 toolchain + 完整 JavaParser 集成
