<h1 align="center">stagger Gradle Plugin</h1>

![gradle](https://img.shields.io/gradle-plugin-portal/v/com.github.hsindumas)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
![number of issues closed](https://img.shields.io/github/issues-closed-raw/shalousun/stagger-gradle-plugin)
![closed pull requests](https://img.shields.io/github/issues-pr-closed-raw/shalousun/stagger-gradle-plugin)
![java version](https://img.shields.io/badge/JAVA-1.8+-green.svg)
[![chinese](https://img.shields.io/badge/chinese-中文文档-brightgreen)](https://github.com/HsinDumas/stagger-gradle-plugin/blob/master/README_CN.md)

## Introduce

`stagger-gradle-plugin` is a `gradle` plugin developed by the `stagger` official team.
This plugin is available from `stagger 1.8.6`.
Using `stagger-gradle-plugin` makes it easier to integrate `stagger` into your project, and integration is more
lightweight.
You no longer need to write unit tests in your project to
Start `stagger` to scan source code analysis and generate API documents.
You can run the `gradle` command directly or click on the preset` goal` of the `stagger-maven-plugin` in the IDE to
generate API documentation.
stagger-gradle-plugin will also make stagger's ability to generate API documentation more powerful.
[About stagger](https://HsinDumas.github.io)

## Getting started

### Add plugin

Using the plugins DSL:

```
plugins {
  id "com.github.hsindumas.stagger" version "[latest]"
}
```

Using legacy plugin application:

```
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "com.github.hsindumas:stagger-gradle-plugin:[latest]"
  }
}

apply plugin: "com.github.hsindumas.stagger"
```

### Plugin options

| Option     | Default value                   | Description                                                                                                               |
|------------|---------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| configFile | src/main/resources/default.json |                                                                                                                           |
| exclude    |                                 | exclude artifact,usage:exclude 'org.springframework.boot:spring-boot-starter-tomcat'                                      |
| include    |                                 | Let the plugin download the specified java lib source,usage:include 'org.springframework.boot:spring-boot-starter-tomcat' |

Example setting of options:

```
stagger {
    configFile = file("src/main/resources/default.json")
    
    // exclude example
    // exclude artifact
    exclude 'org.springframework.boot:spring-boot-starter-tomcat'
    // exclude artifact use pattern
    exclude 'org.springframework.boot.*'
    // You can use the include configuration to let the plugin automatically load the specified source.
    // include example
    include 'org.springframework.boot:spring-boot-starter-tomcat'
}
```

For the configuration of `configFile`, you can also dynamically override it through the `gradle` command line. Before
version `3.0.3`, you can add dynamic configuration to get the `configFile` in `build.gradle`, for example:

```groovy
stagger {
	configFile = project.hasProperty('stagger.configFile') ? file(project.getProperty('stagger.configFile')) : file("src/main/resources/stagger.json")
}
```

After configuring, you can directly override it through the command line:

```shell
gradle staggerRestHtml -Pstagger.configFile=src/main/resources/stagger.json
```

From version `3.0.3` onwards, the configuration of dynamically configuring `configFile` in `build.gradle` is very
simple, and the plugin has the ability to completely override it.

```groovy
stagger {
	configFile = file("src/main/resources/stagger.json")
}
```

After configuration, you can directly use `-Pstagger.configFile` to override it.

For multi-module gradle projects, if you do not want to configure in each module, you can put the `stagger` plugin
related configuration into subprojects.

```
subprojects{
    apply plugin: 'com.github.hsindumas.stagger'
    stagger {
        //
        configFile = file("src/main/resources/stagger.json")
        // exclude artifact
        exclude 'org.springframework.boot:xx'
        exclude 'org.springframework.boot:ddd'
        // You can use the include configuration to let the plugin automatically load the specified source.
        // include example
        include 'org.springframework.boot:spring-boot-starter-tomcat'
    }
}
```

### Create a json config

Create a json configuration file in your project. If it is multiple modules, put them in the modules that need to
generate documents.
The `stagger-gradle-plugin` plugin will use this configuration information.
For example, create `/src/main/resources/stagger.json` in the project.
The configuration contents are as follows.

**Minimize configuration:**

```
{
   "allInOne": true, // whether to merge documents into one file, generally recommended as true
   "isStrict": false,//If the strict mode is set to true, Smart-doc forces that the public method in each interface in the code has a comment.
   "outPath": "/src/main/resources" //Set the api document output path.
}
```

Only three configuration items are required to use the `stagger-gradle-plugin` to generate API documentation. In fact,
only `outPath` must be configured.

**Detailed configuration content:**

`stagger` provides a lot of configuration options. For more configuration options,
please refer to the [official documentation](https://HsinDumas.github.io/guide/advanced/config)

### Generated document

#### Use Gradle command

```
// Generate documentation into HTML
gradle staggerRestHtml
// Generate markdown
gradle staggerRestMarkdown
// Generate adoc
gradle staggerRestAdoc
// Generate Postman JSON data
gradle staggerPostman
// Generate Open API 3.0 + specification JSON documentation, since stagger-gradle-plugin 1.1.4
gradle staggerOpenApi
// Generate JMeter performance test scripts, since 3.0.0
gradle staggerJmeter
// Generate documentation output to Word, since 3.0.0
gradle word

// Apache Dubbo RPC generation
// Generate html
gradle staggerRpcHtml
// Generate markdown
gradle staggerRpcMarkdown
// Generate adoc
gradle staggerRpcAdoc
```

#### Use In IntelliJ IDEA

On Use IntelliJ IDE, if you have added `stagger-gradle-plugin` to the project,
you can directly find the plugin `stagger` plugin and click to generate API documentation.

![stagger-gradle-plugin](https://raw.githubusercontent.com/HsinDumas/stagger-gradle-plugin/master/images/idea.png)

### Generated document example

#### Interface header rendering

![header](https://images.gitee.com/uploads/images/2019/1231/223538_be45f8a9_144669.png "header.png")

#### Request parameter example rendering

![request-params](https://images.gitee.com/uploads/images/2019/1231/223710_88933f55_144669.png "request.png")

#### Response parameter example renderings

![response-fields](https://images.gitee.com/uploads/images/2019/1231/223817_32bea6dc_144669.png "response.png")

## Building and publish

you can build with the following commands. (`JDK 1.8+`, `Gradle 7.6+`is required to build the master branch)

### Publish to Maven local

Install the gradle plugin to the local Maven repository, which is located at `~/.m2/repository`.
If your local Maven repository path is not `~/.m2/repository`, it is recommended to set a global `M2_HOME` (Maven
installation path) system variable first.
Gradle will then automatically search for it.

```groovy
gradle publishToMavenLocal
```

### Publish to Nexus

Publish the gradle plugin to your own `Nexus` repository by modifying the repository address configuration in
`build.gradle`.

```groovy
gradle publish
```

### Publish to Gradle Plugin Portal

publish to https://plugins.gradle.org/

```groovy
gradlew publishPlugins - PskipSigning
// or
make publishToPortal
```

## Other reference

- [stagger manual](https://HsinDumas.github.io)

## License

stagger-gradle-plugin is under the Apache 2.0 license. See
the[LICENSE](https://raw.githubusercontent.com/shalousun/stagger-maven-plugin/master/LICENSE)file for details.

## Contact

Email： opensource@ly.com
