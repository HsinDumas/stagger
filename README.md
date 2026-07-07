# Stagger

[中文版本](./README_CN.md)

> **Let Swagger stagger. Keep your source code pristine.**

Swagger asks your business code to carry docs metadata.
Stagger says no.

Stagger is a **zero-intrusion, zero-annotation** API documentation generator based on JavaParser static analysis.
It is maintained independently from the smart-doc lineage, with explicit focus on modern Java engineering (JDK 25 toolchain, Gradle 9.x, Spring Boot 4 compatibility).

## 👀 Generated Result Preview

Generated offline HTML from the built-in sample project:

![Stagger generated HTML preview](./images/stagger-example-html.png)

Before/After code style at a glance:

![Before and after clean domain model](./images/before-after-clean-code.svg)

## 🎯 Who Stagger Is For

- Teams that want API docs without Swagger annotations in domain code.
- Existing smart-doc users who need a modern toolchain path.
- Java teams upgrading toward newer JDK/Spring Boot baselines.

## ⚡ One-Minute Pitch

- Keep docs out of domain code.
- Parse Java sources and JavaDoc directly.
- Generate OpenAPI 3.1, Markdown, and offline HTML in build time.
- Keep Maven and Gradle both first-class for users.

## 💡 Why Stagger?

When docs start owning your model, your model stops being your model.

Stagger keeps documentation generation in build-time analysis, not in controller-level annotation sprawl.

### Before / After

```java
// Before: doc framework drives your business code shape
@Schema(description = "Create order request")
public class CreateOrderRequest {
    @Schema(description = "Customer id")
    private String customerId;
}

// After with Stagger: keep model clean, let JavaDoc + source analysis do the job
/** Create order request */
public class CreateOrderRequest {
    /** Customer id */
    private String customerId;
}
```

Core principles:

- 🚫 **Zero Intrusion** - No third-party API doc annotations in your domain code.
- 📝 **JavaDoc + Source Metadata** - Derive docs from source structure and comments.
- ⚡ **Modern Java First** - Focus on JDK 25 toolchain and newer framework compatibility.
- 🔄 **Multi-format Output** - Generate OpenAPI 3.1, Markdown, and offline HTML.

## ✨ Feature Comparison

| Feature | Stagger | Swagger | springdoc-openapi |
|---------|---------|---------|------------------|
| Zero Code Intrusion | ✅ | ❌ | ❌ |
| Pure JavaDoc | ✅ | ❌ | ❌ |
| Build-time Generation | ✅ | ❌ | ❌ |
| Gradle 9.x Support | ✅ | ✅ | ✅ |
| OpenAPI 3.1 | ✅ | ✅ | ✅ |

## 🚀 5-Second Demo (Clone and Run)

```bash
git clone https://github.com/HsinDumas/stagger.git
cd stagger
./gradlew :example:restHtml
open example/build/stagger/index.html
```

What you get:

- A generated offline HTML doc at `example/build/stagger/index.html`.
- A minimal controller + DTO sample under `example/src/main/java`.
- Ready-to-edit `stagger.json` under `example/src/main/resources`.

## 🔍 How Stagger Differs from smart-doc

Same spirit, different engineering choices:

| Area | Upstream Baseline | Stagger (this repository) |
|------|-----------------------|----------------------------|
| Project Positioning | Broad upstream baseline | Independent maintenance fork with explicit modernization roadmap |
| Build System | Maven-centric | Gradle monorepo-first (while still shipping first-class Maven + Gradle plugins) |
| Parser Architecture | QDox-centric lineage | JavaParser-based SourceModel abstraction |
| JDK Strategy | Traditional baseline | Built with JDK 25 toolchain, release-compatible output target |
| Spring Focus | Mainstream Spring stack | Additional focus on Spring Boot 4 and modern annotation compatibility |
| Migration Transparency | N/A | Public migration records in `docs/CODEX_MIGRATION_PLAN.md` |

Stagger uses Gradle internally, but it does not force users to switch build tools. Maven and Gradle plugin experiences remain first-class.

## 🙏 Tribute to smart-doc

Stagger would not exist without [smart-doc](https://github.com/smart-doc-group/smart-doc).
We continue the same non-intrusive documentation philosophy and thank [shalousun](https://github.com/shalousun) and all contributors.

## 📚 Wiki

- GitHub Wiki: https://github.com/HsinDumas/stagger/wiki
- Documentation index: `docs/wiki/README.md`
- `stagger.json` reference (EN): `docs/wiki/stagger-json.md`
- `stagger.json` reference (中文): `docs/wiki/stagger-json-cn.md`

## 🚀 Quick Start

Current examples use `3.2.1` for copy-paste convenience.
Latest release: https://github.com/HsinDumas/stagger/releases (tag format: `vX.Y.Z`).

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
# Optional: override config path at runtime
./gradlew restHtml -Pstagger.configFile=src/main/resources/stagger.json
```

### Minimal `stagger.json`

```json
{
  "allInOne": true,
  "isStrict": false,
  "outPath": "build/stagger"
}
```

## 🔧 Building

```bash
# Build all modules
./gradlew clean build -x test

# Build specific modules
./gradlew :stagger-core:build -x test
./gradlew :stagger-maven-plugin:build -x test
./gradlew :stagger-gradle-plugin:build -x test
```

## 📄 License

Apache License 2.0 - See LICENSE file

## 🤝 Contributing

Pull requests welcome! Please open an issue first for major changes.

## 👏 Acknowledgments

- **Upstream Project**: [smart-doc](https://github.com/smart-doc-group/smart-doc)
- **Current Maintainer**: [HsinDumas](https://github.com/HsinDumas)
