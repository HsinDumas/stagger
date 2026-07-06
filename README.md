# Stagger

[中文版本](./README_CN.md)

> **Let Swagger stagger. Keep your source code pristine.**

Swagger asks your business code to carry docs metadata.
Stagger says no.

Stagger is a **zero-intrusion, zero-annotation** API documentation generator based on JavaParser static analysis.
It is maintained independently from the smart-doc lineage, with explicit focus on modern Java engineering (JDK 25 toolchain, Gradle 9.x, Spring Boot 4 compatibility).

## ⚡ One-Minute Pitch

- Keep docs out of domain code.
- Parse Java sources and JavaDoc directly.
- Generate OpenAPI 3.1, Markdown, and offline HTML in build time.
- Keep Maven and Gradle both first-class for users.

## 🙏 Tribute to smart-doc

Stagger would not exist without [smart-doc](https://github.com/smart-doc-group/smart-doc).

- smart-doc pioneered the non-intrusive API doc path in the Java ecosystem.
- Stagger inherits that philosophy: keep business code clean, use source analysis over annotation flooding.
- We sincerely respect [shalousun](https://github.com/shalousun) and all smart-doc contributors.

Stagger is not a rejection of smart-doc. It is a focused continuation for teams that want a modernized toolchain and transparent migration records.

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

Detailed migration notes and contributor validation commands are documented in `docs/CODEX_MIGRATION_PLAN.md` and `CONTRIBUTING.md`.

## 📚 Wiki

- GitHub Wiki: https://github.com/HsinDumas/stagger/wiki
- Documentation index: `docs/wiki/README.md`
- `stagger.json` reference (EN): `docs/wiki/stagger-json.md`
- `stagger.json` reference (中文): `docs/wiki/stagger-json-cn.md`

## 🚀 Quick Start

Use the latest release from https://github.com/HsinDumas/stagger/releases (tag format: `vX.Y.Z`), then replace `REPLACE_WITH_LATEST_RELEASE` below (for example: `3.2.2`).

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

## 📦 Monorepo Structure

This is a modern maintenance fork of stagger, organized as a monorepo:

```
stagger/
├── stagger-core/           # Core documentation engine
├── stagger-maven-plugin/   # Maven plugin
├── stagger-gradle-plugin/  # Gradle plugin
├── build.gradle.kts        # Root Gradle build
└── settings.gradle.kts     # Module settings
```

## 🔧 Building

```bash
# Build all modules
./gradlew clean build -x test

# Build specific module
./gradlew :stagger-core:build -x test
./gradlew :stagger-maven-plugin:build -x test

# Build Gradle plugin
./gradlew :stagger-gradle-plugin:build -x test
```

## 🐛 Key Improvements

### Gradle 9.x Compatibility Fix

```java
// ❌ Old (Removed in Gradle 9.0)
project.getConvention()
    .getPlugin(JavaPluginConvention.class)
    .getSourceSets();

// ✅ New (Gradle 7.1+)
project.getExtensions()
    .getByType(JavaPluginExtension.class)
    .getSourceSets();
```

### Unified Monorepo

Original stagger was scattered across 4 separate repositories. Now unified in a single repo for easier version management and maintenance.

## 📄 License

Apache License 2.0 - See LICENSE file

## 👏 Acknowledgments

- **Upstream Project**: [smart-doc](https://github.com/smart-doc-group/smart-doc)
- **Current Maintainer**: [HsinDumas](https://github.com/HsinDumas)

## 🤝 Contributing

Pull requests welcome! Please open an issue first for major changes.

---

**Engineering Focus**: Gradle monorepo + JDK 25 toolchain + complete JavaParser integration
