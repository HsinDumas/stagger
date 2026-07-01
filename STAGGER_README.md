# Stagger

> 🎯 A fork of smart-doc with **Gradle 9.x compatibility** fixes.

**Stagger** is a Java REST API documentation generation tool based on JavaDoc. Unlike Swagger/OpenAPI-based approaches, Stagger generates documentation **at build time** with **zero code intrusion**.

## Key Features

- ✅ **Zero Intrusion** - Pure JavaDoc based, no annotations needed
- ✅ **Build-time Generation** - Generates documentation during Maven/Gradle build
- ✅ **Gradle 9.x Compatible** - Fixed deprecated `JavaPluginConvention` API
- ✅ **Multiple Output Formats** - HTML, Markdown, AsciiDoc, OpenAPI/Swagger JSON, Postman JSON
- ✅ **Monorepo Structure** - Core library + Maven/Gradle plugins in one repository

## Why Stagger?

**Smart-doc** was an excellent tool, but stopped maintenance. The main issue: it uses deprecated Gradle APIs (`JavaPluginConvention`) that were **removed in Gradle 9.0**.

This fork (**Stagger**) maintains compatibility with modern Gradle versions while preserving the elegant zero-intrusion approach:

| Feature | Stagger | Swagger/OpenAPI | springdoc-openapi |
|---------|---------|-----------------|------------------|
| Build time generation | ✅ | ❌ | ❌ |
| Code intrusion | ❌ | ❌ | ✅ (annotations) |
| Gradle 9.x | ✅ | ✅ | ✅ |
| Runtime dependency | ❌ | ✅ | ✅ |

## Monorepo Structure

```
stagger/
├── stagger-core/           # Core documentation engine
├── stagger-maven-plugin/   # Maven plugin
├── stagger-gradle-plugin/  # Gradle plugin (Gradle build)
└── pom.xml                 # Parent POM
```

## Installation

### Maven

Add to your `pom.xml`:

```xml
<plugin>
    <groupId>com.github.shalousun</groupId>
    <artifactId>stagger-maven-plugin</artifactId>
    <version>4.0.1-SNAPSHOT</version>
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

Add to your `build.gradle`:

```gradle
plugins {
    id 'com.ly.doc' version '4.0.1-SNAPSHOT'
}

stagger {
    sourceCodePath = "src/main/java"
    classAbsPath = "build/classes/java/main"
}
```

## Building

```bash
# Build all modules
mvn clean install

# Build specific module
cd stagger-core && mvn clean install
cd stagger-maven-plugin && mvn clean install

# Build Gradle plugin
cd stagger-gradle-plugin && ./gradlew build
```

## Key Changes from Smart-doc

### ✅ Fixed Gradle API Compatibility

**Before (Deprecated in Gradle 7.1, Removed in 9.0):**
```java
SourceSetContainer ssc = project.getConvention()
    .getPlugin(JavaPluginConvention.class)
    .getSourceSets();
```

**After (Gradle 7.1+):**
```java
JavaPluginExtension javaExt = project.getExtensions()
    .getByType(JavaPluginExtension.class);
SourceSetContainer ssc = javaExt.getSourceSets();
```

### 📦 Monorepo Structure

- **Before**: 4 separate repositories (smart-doc, smart-doc-gradle-plugin, smart-doc-maven-plugin, smart-doc-idea-plugin)
- **After**: Single monorepo for easier maintenance and version synchronization

## License

Apache License 2.0 - See LICENSE file

## Credits

- **Original Author**: [shalousun](https://github.com/shalousun) (smart-doc)
- **Maintainer**: [HsinDumas](https://github.com/HsinDumas) (Stagger fork)

## Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you'd like to change.

---

**Based on**: [smart-doc](https://github.com/smart-doc-group/smart-doc)  
**Modified for**: Gradle 9.x compatibility and improved maintenance
