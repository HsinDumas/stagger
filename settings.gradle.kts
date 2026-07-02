rootProject.name = "stagger"

include(
    "stagger-core",
    "stagger-maven-plugin",
    "stagger-gradle-plugin",
)

project(":stagger-gradle-plugin").buildFileName = "build.gradle.kts"
