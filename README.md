# Stagger

[中文版本](./README_CN.md)

[![CI](https://github.com/HsinDumas/stagger/actions/workflows/ci.yml/badge.svg)](https://github.com/HsinDumas/stagger/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.hsindumas/stagger-core)](https://central.sonatype.com/artifact/com.github.hsindumas/stagger-core)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.github.hsindumas.stagger)](https://plugins.gradle.org/plugin/com.github.hsindumas.stagger)
[![License](https://img.shields.io/github/license/HsinDumas/stagger)](./LICENSE)

> **Let Swagger stagger. Keep your source code pristine.**

Stagger is a **zero-intrusion, zero-annotation** API documentation generator. It reads your Java
sources and JavaDoc via static analysis (JavaParser) and produces OpenAPI 3.1, Markdown, and
offline HTML at build time — so docs metadata never leaks into your domain code.

Maintained independently from the smart-doc lineage, with focus on a modern toolchain (JDK 21,
Gradle 9.x, Spring Boot 4).

Current release artifacts run on JDK 17 and above.

## 👀 What You Get

Offline HTML generated from the built-in `example/` project:

![Stagger generated HTML preview](./images/stagger-example-html.png)

## ✍️ Before / After

Your model stays yours — no doc-framework annotations:

```java
// Before: the doc framework drives your business code shape
@Schema(description = "Create order request")
public class CreateOrderRequest {
    @Schema(description = "Customer id")
    private String customerId;
}

// After with Stagger: plain model, JavaDoc + source analysis do the job
/** Create order request */
public class CreateOrderRequest {
    /** Customer id */
    private String customerId;
}
```

## ✨ Comparison

| Feature | Stagger | Swagger | springdoc-openapi |
|---------|---------|---------|-------------------|
| Zero code intrusion | ✅ | ❌ | ❌ |
| Pure JavaDoc | ✅ | ❌ | ❌ |
| Build-time generation | ✅ | ❌ | ❌ |
| Gradle 9.x support | ✅ | ✅ | ✅ |
| OpenAPI 3.1 | ✅ | ✅ | ✅ |

## 🚀 Try It

Clone and generate the sample docs (requires JDK 21):

```bash
git clone https://github.com/HsinDumas/stagger.git
cd stagger
./gradlew :example:restHtml
# open the generated HTML at: example/build/stagger/
```

## 🔧 Use It in Your Project

Examples pin `3.3.0`. Latest release: https://github.com/HsinDumas/stagger/releases

### Maven

```xml
<plugin>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>stagger-maven-plugin</artifactId>
    <version>3.3.0</version>
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
    id("com.github.hsindumas.stagger") version "3.3.0"
}

stagger {
    configFile = file("src/main/resources/stagger.json")
}
```

```bash
./gradlew restHtml
```

### Minimal `stagger.json`

```json
{
  "allInOne": true,
  "isStrict": false,
  "outPath": "build/stagger"
}
```

Full config reference: [`docs/wiki/stagger-json.md`](./docs/wiki/stagger-json.md) ·
[中文](./docs/wiki/stagger-json-cn.md) · [Wiki](https://github.com/HsinDumas/stagger/wiki)

## 🙏 smart-doc

Stagger continues the non-intrusive documentation philosophy pioneered by
[smart-doc](https://github.com/smart-doc-group/smart-doc) — thanks to
[shalousun](https://github.com/shalousun) and all contributors. Engineering differences and the
migration path are recorded in [`docs/CODEX_MIGRATION_PLAN.md`](./docs/CODEX_MIGRATION_PLAN.md).

### Key Differences From smart-doc

- Independent evolution path with Stagger-owned roadmap and implementation choices.
- Modernized build baseline for contributors (JDK 21, Gradle 9.x), while published artifacts keep
    runtime compatibility for JDK 17+ users.
- Refined offline HTML UX focused on API browsing efficiency: single-endpoint focus, response tree
    expand/collapse, and faster copy-oriented interactions.

## 🤝 Contributing

Pull requests welcome — please open an issue first for major changes.
Licensed under [Apache 2.0](./LICENSE).
