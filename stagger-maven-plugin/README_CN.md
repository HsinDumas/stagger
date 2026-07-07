# Stagger Maven Plugin

![maven](https://img.shields.io/maven-central/v/com.github.hsindumas/stagger-maven-plugin)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

`stagger-maven-plugin` 是 Stagger 官方 Maven 插件，用于在构建阶段生成 API 文档。

- 无侵入：不需要在业务代码中加入文档框架注解。
- 多格式输出：支持 HTML、Markdown、OpenAPI、Postman、Word、JMeter。
- 易集成：直接通过 Maven goal 生成，无需额外测试启动器。

项目仓库：<https://github.com/HsinDumas/stagger>

## 环境要求

- 构建基线：JDK 21
- Release 产物运行时：JDK 17 及以上
- Maven：3.8+

## 快速开始

### 1) 在项目中添加插件

```xml
<plugin>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>stagger-maven-plugin</artifactId>
    <version>最新版本</version>
    <configuration>
        <configFile>${project.basedir}/src/main/resources/stagger.json</configFile>
        <!-- 可选：仅子模块禁用 -->
        <!-- <skip>true</skip> -->

        <!-- 可选：排除依赖源码 -->
        <excludes>
            <exclude>com.google.guava:guava</exclude>
        </excludes>

        <!-- 可选：显式包含依赖源码（配置后按 includes 精确加载） -->
        <includes>
            <include>com.baomidou:mybatis-plus-extension</include>
            <include>org.springframework.data:spring-data-commons</include>
        </includes>
    </configuration>
    <executions>
        <execution>
            <!-- 如不希望在 compile 自动执行，可移除 phase -->
            <phase>compile</phase>
            <goals>
                <goal>html</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 2) 创建配置文件 stagger.json

在项目中创建 `src/main/resources/stagger.json`。

最小可用配置：

```json
{
  "allInOne": true,
  "isStrict": false,
  "outPath": "./src/main/resources/static/doc"
}
```

完整配置参考：
- <https://github.com/HsinDumas/stagger/wiki>

## 常用命令

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

# Dubbo RPC 文档
mvn -Dfile.encoding=UTF-8 stagger:rpc-html
mvn -Dfile.encoding=UTF-8 stagger:rpc-markdown
```

## IDEA 使用

在 IDEA 的 Maven 工具窗口中，展开插件 goals，直接点击对应目标即可生成文档。

## 构建插件

```bash
mvn clean install -Dmaven.test.skip=true
```

## 发布记录

- [CHANGELOG](./CHANGELOG.md)

## 许可证

Apache License 2.0：
- <https://www.apache.org/licenses/LICENSE-2.0>
