plugins {
    base
    alias(libs.plugins.mavenPublish) apply false
}

group = "com.github.hsindumas"
val resolvedVersion = providers.gradleProperty("releaseVersion")
    .orElse(providers.environmentVariable("RELEASE_VERSION"))
    .orElse("1.0.1-SNAPSHOT")

version = resolvedVersion.get()

allprojects {
    version = rootProject.version

    repositories {
        mavenLocal()
        maven(url = "https://maven.aliyun.com/repository/public")
        mavenCentral()
    }
}

tasks.register("printVersion") {
    group = "help"
    description = "Prints the current project version"
    doLast {
        println(project.version)
    }
}

tasks.register("publishSnapshotToMavenCentral") {
    group = "publishing"
    description = "Publishes all modules to Sonatype Central snapshot repository"
    dependsOn(
        ":stagger-core:publishToMavenCentral",
        ":stagger-maven-plugin:publishToMavenCentral",
        ":stagger-gradle-plugin:publishToMavenCentral",
    )
}

tasks.register("publishReleaseToMavenCentral") {
    group = "publishing"
    description = "Publishes and releases all modules to Sonatype Central"
    dependsOn(
        ":stagger-core:publishAndReleaseToMavenCentral",
        ":stagger-maven-plugin:publishAndReleaseToMavenCentral",
        ":stagger-gradle-plugin:publishAndReleaseToMavenCentral",
    )
}
