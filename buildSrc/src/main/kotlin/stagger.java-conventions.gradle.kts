import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    `java-library`
    checkstyle
    id("com.diffplug.spotless")
    id("com.palantir.java-format")
}

group = "com.github.hsindumas"

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addBooleanOption("Xdoclint:all", true)
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:",
        )
    }
    isFailOnError = true
}

checkstyle {
    toolVersion = "10.20.1"
    configFile = rootProject.file("checkstyle/google_checks.xml")
    configProperties["org.checkstyle.google.suppressionfilter.config"] =
        rootProject.file("checkstyle/checkstyle-suppressions.xml").absolutePath
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// Keep historical task names used by CI and local scripts.
if (tasks.findByName("checkFormatMain") == null) {
    tasks.register("checkFormatMain") {
        dependsOn("spotlessJavaCheck")
    }
}

if (tasks.findByName("checkFormatTest") == null) {
    tasks.register("checkFormatTest") {
        dependsOn("spotlessJavaCheck")
    }
}

if (tasks.findByName("formatMain") == null) {
    tasks.register("formatMain") {
        dependsOn("spotlessJavaApply")
    }
}

if (tasks.findByName("formatTest") == null) {
    tasks.register("formatTest") {
        dependsOn("spotlessJavaApply")
    }
}

if (tasks.findByName("format") == null) {
    tasks.register("format") {
        dependsOn("spotlessApply")
    }
}
