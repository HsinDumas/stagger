plugins {
    id("stagger.java-conventions")
    `maven-publish`
}

description = "Core documentation generation engine for Stagger"

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(libs.freemarker)
    implementation(libs.javaparserCore)
    implementation(libs.javaparserSymbolSolver)
    implementation(libs.datafaker)
    implementation(libs.commonUtil)
    implementation(libs.gson)
    implementation(libs.slf4jApi)
    implementation(libs.jgit)

    testImplementation(platform(libs.junitBom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.mockito)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    archiveBaseName.set("stagger")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "stagger-core"
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}
