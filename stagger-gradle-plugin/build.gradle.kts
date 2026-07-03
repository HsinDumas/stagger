plugins {
    id("stagger.java-conventions")
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
    `maven-publish`
    signing
}

group = "com.github.hsindumas"
version = rootProject.version

description = "stagger gradle plugin"

dependencies {
    implementation(project(":stagger-core"))
    implementation(libs.gson)
}

gradlePlugin {
    website = "https://github.com/HsinDumas/stagger"
    vcsUrl = "https://github.com/HsinDumas/stagger"
    plugins {
        create("staggerPlugin") {
            id = "com.github.hsindumas.stagger"
            implementationClass = "com.github.hsindumas.stagger.gradle.plugin.StaggerPlugin"
            displayName = "stagger gradle plugin"
            description = "stagger gradle plugin"
            tags.set(listOf("stagger", "gradle-plugin"))
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}

signing {
    isRequired = false
}
