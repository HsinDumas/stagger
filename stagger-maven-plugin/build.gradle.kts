plugins {
    id("stagger.java-conventions")
    `maven-publish`
}

description = "Maven plugin for Stagger documentation generation"

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(project(":stagger-core"))
    implementation(libs.qdox)
    implementation(libs.commonUtil)
    implementation(libs.gson)
    implementation("org.apache.commons:commons-lang3:3.18.0")

    compileOnly(libs.mavenPluginApi)
    compileOnly(libs.mavenPluginAnnotations)

    implementation(libs.mavenCore)
    implementation(libs.mavenModel)
    implementation(libs.mavenArtifact)
    implementation(libs.mavenDependencyTree)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "stagger-maven-plugin"
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}
