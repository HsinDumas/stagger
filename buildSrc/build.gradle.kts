plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.diffplug.spotless:com.diffplug.spotless.gradle.plugin:8.7.0")
    implementation("com.palantir.java-format:com.palantir.java-format.gradle.plugin:2.94.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.7.0")
}
