# Stagger Maven Plugin

![maven](https://img.shields.io/maven-central/v/com.github.hsindumas/stagger-maven-plugin)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

`stagger-maven-plugin` is the official Maven plugin for Stagger API documentation generation.

- Non-invasive: no need to add extra documentation annotations to business code.
- Multiple outputs: HTML, Markdown, OpenAPI, Postman, Word, and JMeter.
- Easy integration: generate docs directly via Maven goals.

Project repository: <https://github.com/HsinDumas/stagger>

## Requirements

- Build baseline: JDK 21
- Runtime for released artifacts: JDK 17+
- Maven: 3.8+

## Getting Started

### 1) Add the plugin

```xml
<plugin>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>stagger-maven-plugin</artifactId>
    <version>latest</version>
    <configuration>
        <configFile>${project.basedir}/src/main/resources/stagger.json</configFile>
        <!-- Optional: disable in child module only -->
        <!-- <skip>true</skip> -->

        <!-- Optional: exclude dependency sources -->
        <excludes>
            <exclude>com.google.guava:guava</exclude>
        </excludes>

        <!-- Optional: include dependency sources explicitly -->
        <includes>
            <include>com.baomidou:mybatis-plus-extension</include>
            <include>org.springframework.data:spring-data-commons</include>
        </includes>
    </configuration>
    <executions>
        <execution>
            <!-- Remove phase if you don't want auto execution during compile -->
            <phase>compile</phase>
            <goals>
                <goal>html</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 2) Create stagger.json

Create `src/main/resources/stagger.json` in your project.

Minimum config:

```json
{
  "allInOne": true,
  "isStrict": false,
  "outPath": "./src/main/resources/static/doc"
}
```

Full configuration reference:
- <https://github.com/HsinDumas/stagger/wiki>

## Common Commands

```bash
# HTML
mvn -Dfile.encoding=UTF-8 stagger:html

# Markdown
mvn -Dfile.encoding=UTF-8 stagger:markdown

# OpenAPI
mvn -Dfile.encoding=UTF-8 stagger:openapi

# Postman
mvn -Dfile.encoding=UTF-8 stagger:postman

# Word
mvn -Dfile.encoding=UTF-8 stagger:word

# JMeter
mvn -Dfile.encoding=UTF-8 stagger:jmeter

# Dubbo RPC docs
mvn -Dfile.encoding=UTF-8 stagger:rpc-html
mvn -Dfile.encoding=UTF-8 stagger:rpc-markdown
```

## IntelliJ IDEA

Open the Maven tool window in IntelliJ IDEA, expand plugin goals, and run the goal you need.

## Build Plugin Locally

```bash
mvn clean install -Dmaven.test.skip=true
```

## Release Notes

- [CHANGELOG](./CHANGELOG.md)

## License

Apache License 2.0:
- <https://www.apache.org/licenses/LICENSE-2.0>
