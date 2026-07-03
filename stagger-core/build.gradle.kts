plugins {
    id("stagger.java-conventions")
    alias(libs.plugins.mavenPublish)
}

description = "Core documentation generation engine for Stagger"

dependencies {
    implementation(libs.freemarker)
    implementation(libs.javaparserCore)
    implementation(libs.javaparserSymbolSolver)
    implementation(libs.generex)
    implementation(libs.gson)
    implementation(libs.slf4jApi)
    implementation(libs.jgit)
    implementation("org.apache.commons:commons-lang3:3.18.0")

    testImplementation(platform(libs.junitBom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.mockito)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    archiveBaseName.set("stagger")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("com.github.hsindumas", "stagger-core", rootProject.version.toString())

    pom {
        name = "stagger-core"
        description = "Core documentation generation engine for Stagger"
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
