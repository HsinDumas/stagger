plugins {
    java
    id("com.github.hsindumas.stagger") version "3.2.1"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.springframework:spring-web:6.2.8")
}

stagger {
    configFile = file("src/main/resources/stagger.json")
}
