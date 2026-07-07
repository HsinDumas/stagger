# Stagger

[English Version](./README.md)

[![CI](https://github.com/HsinDumas/stagger/actions/workflows/ci.yml/badge.svg)](https://github.com/HsinDumas/stagger/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.hsindumas/stagger-core)](https://central.sonatype.com/artifact/com.github.hsindumas/stagger-core)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.github.hsindumas.stagger)](https://plugins.gradle.org/plugin/com.github.hsindumas.stagger)
[![License](https://img.shields.io/github/license/HsinDumas/stagger)](./LICENSE)

> **Let Swagger stagger. Keep your source code pristine.**

Stagger 是一款 **零侵入、零注解** 的 API 文档生成工具。它通过静态分析（JavaParser）读取你的 Java
源码与 JavaDoc，在构建期输出 OpenAPI 3.1、Markdown 和离线 HTML —— 文档元信息永远不会渗进你的领域模型。

源于 smart-doc 思路并独立维护，重点面向现代工具链（JDK 25、Gradle 9.x、Spring Boot 4）。

## 👀 生成效果

仓库内置 `example/` 示例项目生成的离线 HTML：

![Stagger 生成 HTML 效果](./images/stagger-example-html.png)

## ✍️ Before / After

模型永远是你的模型 —— 不需要任何文档框架注解：

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

## ✨ 对比

| 特性 | Stagger | Swagger | springdoc-openapi |
|------|---------|---------|-------------------|
| 零代码侵入 | ✅ | ❌ | ❌ |
| 纯 JavaDoc | ✅ | ❌ | ❌ |
| 构建时生成 | ✅ | ❌ | ❌ |
| Gradle 9.x | ✅ | ✅ | ✅ |
| OpenAPI 3.1 | ✅ | ✅ | ✅ |

## 🚀 快速试用

克隆仓库并生成示例文档（需要 JDK 25）：

```bash
git clone https://github.com/HsinDumas/stagger.git
cd stagger
./gradlew :example:restHtml
# 生成结果在：example/build/stagger/
```

## 🔧 在你的项目中使用

示例固定使用 `3.2.1`。最新版本：https://github.com/HsinDumas/stagger/releases

### Maven

```xml
<plugin>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>stagger-maven-plugin</artifactId>
    <version>3.2.1</version>
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
    id("com.github.hsindumas.stagger") version "3.2.1"
}

stagger {
    configFile = file("src/main/resources/stagger.json")
}
```

```bash
./gradlew restHtml
```

### 最小 `stagger.json`

```json
{
  "allInOne": true,
  "isStrict": false,
  "outPath": "build/stagger"
}
```

完整配置参考：[`docs/wiki/stagger-json-cn.md`](./docs/wiki/stagger-json-cn.md) ·
[English](./docs/wiki/stagger-json.md) · [Wiki](https://github.com/HsinDumas/stagger/wiki)

## 🙏 关于 smart-doc

Stagger 延续了 [smart-doc](https://github.com/smart-doc-group/smart-doc) 开创的"尽量不侵入业务代码"理念，
向 [shalousun](https://github.com/shalousun) 与所有贡献者致敬。工程路线上的差异与迁移路径记录在
[`docs/CODEX_MIGRATION_PLAN.md`](./docs/CODEX_MIGRATION_PLAN.md)。

## 🤝 贡献

欢迎提交 Pull Request —— 大改动请先开 Issue 讨论。
基于 [Apache 2.0](./LICENSE) 许可证开源。
