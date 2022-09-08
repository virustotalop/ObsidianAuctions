plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.cadixdev.licenser") version "0.6.1"
    id("eclipse")
    id("idea")
}

allprojects {
    group = "com.gmail.virustotalop"
    version = "5.0.1"

    repositories {
        flatDir { dirs("libs") }
        mavenCentral()
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

        var junitVersion = "5.9.0"
        var cloudVersion = "1.7.1"
        var adventureVersion = "4.11.0"

        dependencies {
            testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
            testImplementation("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
            testImplementation("org.mockito:mockito-core:4.8.0")
            implementation("com.github.clubobsidian:wrappy:2.4.0")
            implementation("com.google.inject:guice:5.1.0")
            implementation("cloud.commandframework:cloud-paper:$cloudVersion")
            implementation("cloud.commandframework:cloud-annotations:$cloudVersion")
            implementation("net.kyori:adventure-text-minimessage:$adventureVersion")
            implementation("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
            implementation("net.kyori:adventure-platform-bukkit:4.1.2")
            compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
            compileOnly("org.jetbrains:annotations:23.0.0")
            compileOnly(":vault")
        }
    }
}