import com.vanniktech.maven.publish.GradlePublishPlugin

plugins {
    id("stagger.java-conventions")
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.mavenPublish)
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

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    configure(GradlePublishPlugin())

    pom {
        name = "stagger-gradle-plugin"
        description = "Gradle plugin for Stagger documentation generation"
        inceptionYear = "2018"
        url = "https://github.com/HsinDumas/stagger"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "HsinDumas"
                name = "HsinDumas"
                url = "https://github.com/HsinDumas"
            }
        }
        scm {
            url = "https://github.com/HsinDumas/stagger"
            connection = "scm:git:git://github.com/HsinDumas/stagger.git"
            developerConnection = "scm:git:ssh://git@github.com/HsinDumas/stagger.git"
        }
    }
}
