# Stagger

[English Version](./README.md)

> **让 Swagger 摇晃去吧，保持你的代码纯净无瑕。**

Stagger 是一款面向未来（JDK 25+ / Spring Boot 4.x+）的 **零侵入、零注解** API 文档自动化生成工具。

## 💡 为什么叫 Stagger？

当 Swagger 用满屏幕的 `@Schema` 和 `@Operation` 注解将你的业务代码搅得一团糟时，它已经背离了"优雅"的初衷。在现代 Clean Code 的趋势下，这样的方案只能**步履蹒跚（Stagger）**向前。

Stagger 继承了优秀的静态解析理念，彻底摒弃注解流：

- 🚫 **零侵入** - 你的代码里不需要任何第三方注解。
- 📝 **标准 JavaDoc + AI** - 只读标准 JavaDoc 注释，结合大模型自动补全。
- ⚡ **面向未来** - 原生支持 JDK 25 新特性（Records、Pattern Matching）与 Spring Boot 4.x。
- 🔄 **多样输出** - 一键生成 OpenAPI 3.1、Markdown、离线 HTML 等多种格式。

## ✨ 功能对比

| 特性 | Stagger | Swagger | springdoc-openapi |
|------|---------|---------|------------------|
| 零代码侵入 | ✅ | ❌ | ❌ |
| 纯 JavaDoc | ✅ | ❌ | ❌ |
| 构建时生成 | ✅ | ❌ | ❌ |
| Gradle 9.x | ✅ | ✅ | ✅ |
| AI 增强 | ✅ | ❌ | ❌ |
| OpenAPI 3.1 | ✅ | ✅ | ✅ |

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
└── pom.xml                 # 父 POM
```

## 🔧 构建

```bash
# 构建全部模块
mvn clean install

# 构建特定模块
cd stagger-core && mvn clean install
cd stagger-maven-plugin && mvn clean install

# 构建 Gradle 插件
cd stagger-gradle-plugin && ./gradlew build
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

- **原作者**: [shalousun](https://github.com/shalousun) ([stagger](https://github.com/HsinDumas/stagger))
- **当前维护**: [HsinDumas](https://github.com/HsinDumas)

## 🤝 贡献

欢迎提交 Pull Request！大改动请先开 Issue 讨论。

---

**基于**: [stagger](https://github.com/HsinDumas/stagger)  
**增强**: Gradle 9.x 兼容性 + 现代化维护
