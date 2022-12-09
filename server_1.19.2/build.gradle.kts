plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.3.11"
    id("xyz.jpenilla.run-paper") version "2.0.1"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19.2-R0.1-SNAPSHOT")
    paperDevBundle("1.19.2-R0.1-SNAPSHOT")
    implementation(project(":plugin"))
}