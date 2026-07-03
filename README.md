# Stagger

[中文版本](./README_CN.md)

> **Let Swagger stagger. Keep your source code pristine.**

Stagger is a **zero-intrusion, zero-annotation** API documentation generator built on JavaParser-powered static analysis.
It targets modern Java stacks (JDK 25+ / Spring Boot 4.x+) while keeping your source code pristine.

## 💡 Why Stagger?

When Swagger floods your business code with `@Schema` and `@Operation` annotations, it defeats the purpose of elegance. In the modern Clean Code era, such approaches **stagger** forward clumsily.

Stagger embraces the elegant static analysis approach and completely abandons annotation hell:

- 🚫 **Zero Intrusion** - No third-party annotations in your codebase.
- 📝 **Pure JavaDoc Parsing** - Reads standard JavaDoc comments and source metadata directly.
- ⚡ **Future-Ready** - Native support for JDK 25 features (Records, Pattern Matching) & Spring Boot 4.x.
- 🔄 **Multiple Formats** - Generate OpenAPI 3.1, Markdown, offline HTML in one command.

## ✨ Feature Comparison

| Feature | Stagger | Swagger | springdoc-openapi |
|---------|---------|---------|------------------|
| Zero Code Intrusion | ✅ | ❌ | ❌ |
| Pure JavaDoc | ✅ | ❌ | ❌ |
| Build-time Generation | ✅ | ❌ | ❌ |
| Gradle 9.x Support | ✅ | ✅ | ✅ |
| OpenAPI 3.1 | ✅ | ✅ | ✅ |

## 🔍 Differences From smart-doc

This repository is a modern maintenance fork with a gradual architecture upgrade path. The main differences from smart-doc are:

| Area | smart-doc (upstream) | Stagger (this repository) |
|------|-----------------------|----------------------------|
| Build System | Maven-centric | Gradle monorepo-first (with first-class Maven and Gradle plugins for users) |
| JDK Strategy | Traditional baseline | Built with JDK 25 toolchain, release-compatible output target |
| Parser Architecture | QDox-centric | JavaParser-based SourceModel abstraction |
| Spring Focus | Mainstream Spring stack | Extra focus on Spring Boot 4 / modern annotation compatibility |
| Migration Transparency | N/A | Public migration notes in `docs/CODEX_MIGRATION_PLAN.md` |

Stagger uses Gradle internally to build its monorepo, but provides first-class, fully production-ready plugins for both Maven and Gradle users.

We have fully replaced QDox with a JavaParser-based source model to ensure better compatibility with modern Java features.

Detailed migration logs and contributor-oriented validation commands are documented in `docs/CODEX_MIGRATION_PLAN.md` and `CONTRIBUTING.md`.

## 🚀 Quick Start

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

- **Upstream Inspiration**: [smart-doc](https://github.com/smart-doc-group/smart-doc)
- **Tribute**: Respect to [shalousun](https://github.com/shalousun) and all smart-doc contributors for pioneering non-intrusive documentation generation.
- **Current Maintainer**: [HsinDumas](https://github.com/HsinDumas)

## 🤝 Contributing

Pull requests welcome! Please open an issue first for major changes.

---

**Based on**: [smart-doc](https://github.com/smart-doc-group/smart-doc)  
**Enhanced with**: Gradle monorepo + JDK 25 toolchain + complete JavaParser integration
