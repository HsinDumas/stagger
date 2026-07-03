plugins {
    id("stagger.java-conventions")
    alias(libs.plugins.mavenPublish)
}

description = "Maven plugin for Stagger documentation generation"

dependencies {
    implementation(project(":stagger-core"))
    implementation(libs.gson)
    implementation("org.apache.commons:commons-lang3:3.18.0")

    compileOnly(libs.mavenPluginApi)
    compileOnly(libs.mavenPluginAnnotations)

    implementation(libs.mavenCore)
    implementation(libs.mavenModel)
    implementation(libs.mavenArtifact)
    implementation(libs.mavenDependencyTree)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("com.github.hsindumas", "stagger-maven-plugin", rootProject.version.toString())

    pom {
        name = "stagger-maven-plugin"
        description = "Maven plugin for Stagger documentation generation"
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
