plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.0"
    id("org.cadixdev.licenser") version "0.6.1"
    id("eclipse")
    id("idea")
}

allprojects {
    group = "com.gmail.virustotalop"
    version = "5.1.0"

    repositories {
        flatDir { dirs("libs") }
        mavenCentral()
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://jitpack.io") }
    }
}



java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "org.cadixdev.licenser")

    tasks {
        assemble {
            dependsOn(shadowJar)
        }

        license {
            include("**/*.java")
            header(rootProject.file("HEADER.txt"))
        }

        compileJava {
            options.encoding = Charsets.UTF_8.name()
            options.release.set(11)
        }

        processResources {
            filteringCharset = Charsets.UTF_8.name()
            filesMatching("plugin.yml") {
                expand("pluginVersion" to project.version)
            }
        }

        test {
            useJUnitPlatform()
        }
    }
}