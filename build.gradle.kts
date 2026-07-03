plugins {
    base
}

group = "com.github.hsindumas"
version = "1.0.0"

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
