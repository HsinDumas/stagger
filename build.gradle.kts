import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment

plugins {
    base
    alias(libs.plugins.mavenPublish) apply false
}

group = "com.github.hsindumas"
val resolvedVersion = providers.gradleProperty("releaseVersion")
    .orElse(providers.environmentVariable("RELEASE_VERSION"))
    .orElse("3.3.1-SNAPSHOT")

version = resolvedVersion.get()

fun Project.enforceTagDrivenRelease() {
    val isCi = providers.environmentVariable("CI").orNull == "true"
    val refType = providers.environmentVariable("GITHUB_REF_TYPE").orNull
    val refName = providers.environmentVariable("GITHUB_REF_NAME").orNull
    val releaseVersion = providers.gradleProperty("releaseVersion")
        .orElse(providers.environmentVariable("RELEASE_VERSION"))
        .orNull

    if (!isCi || refType != "tag" || refName.isNullOrBlank()) {
        throw org.gradle.api.GradleException(
            "Release publishing is restricted to GitHub Actions tag builds (push tag vX.Y.Z)."
        )
    }

    if (releaseVersion.isNullOrBlank()) {
        throw org.gradle.api.GradleException(
            "releaseVersion must be provided from tag workflow and match the pushed tag version."
        )
    }

    val expectedVersion = refName.removePrefix("v")
    if (releaseVersion != expectedVersion) {
        throw org.gradle.api.GradleException(
            "releaseVersion ($releaseVersion) does not match tag version ($expectedVersion)."
        )
    }
}

allprojects {
    version = rootProject.version

    repositories {
        mavenLocal()
        maven(url = "https://maven.aliyun.com/repository/public")
        mavenCentral()
    }
}

configurations.matching { it.name == "palantirJavaFormat" }.configureEach {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            objects.named(TargetJvmEnvironment.STANDARD_JVM),
        )
    }
}

gradle.taskGraph.whenReady {
    val releasePublishRequested = allTasks.any {
        it.name == "publishReleaseToMavenCentral" || it.name == "publishAndReleaseToMavenCentral"
    }
    if (releasePublishRequested) {
        rootProject.enforceTagDrivenRelease()
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
