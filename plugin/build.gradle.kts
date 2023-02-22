plugins {
    id("java")
}

tasks {
    shadowJar {
        archiveBaseName.set("ObsidianAuctions")
        archiveClassifier.set("")
        relocate("com.clubobsidian.wrappy", "com.gmail.virustotalop.obsidianauctions.shaded.wrappy")
        relocate("com.fasterxml.jackson", "com.gmail.virustotalop.obsidianauctions.shaded.jackson")
        relocate("com.typesafe.config", "com.gmail.virustotalop.obsidianauctions.shaded.hocon")
        relocate("io.leangen", "com.gmail.virustotalop.obsidianauctions.shaded.leangen")
        relocate("org.apache.commons.io", "com.gmail.virustotalop.obsidianauctions.shaded.commonsio")
        relocate("org.checkerframework", "com.gmail.virustotalop.obsidianauctions.shaded.checkerframework")
        relocate("org.spongepowered.configurate", "com.gmail.virustotalop.obsidianauctions.shaded.configurate")
        relocate("org.yaml.snakeyaml", "com.gmail.virustotalop.obsidianauctions.shaded.snakeyaml")
        relocate("org.aopalliance.aop", "com.gmail.virustotalop.obsidianauctions.shaded.aop")
        relocate("org.aopalliance.intercept", "com.gmail.virustotalop.obsidianauctions.shaded.intercept")
        relocate("javax.annotation", "com.gmail.virustotalop.obsidianauctions.shaded.annotation")
        relocate("javax.inject", "com.gmail.virustotalop.obsidianauctions.shaded.javaxinject")
        relocate("com.google.common", "com.gmail.virustotalop.obsidianauctions.shaded.guava")
        relocate("com.google.errorprone", "com.gmail.virustotalop.obsidianauctions.shaded.errorprone")
        relocate("com.google.inject", "com.gmail.virustotalop.obsidianauctions.shaded.guice")
        relocate("com.google.j2objc", "com.gmail.virustotalop.obsidianauctions.shaded.j2objc")
        relocate("net.kyori.adventure", "com.gmail.virustotalop.obsidianauctions.shaded.adventure")
        relocate("net.kyori.examination", "com.gmail.virustotalop.obsidianauctions.shaded.examination")
        relocate("cloud", "com.gmail.virustotalop.obsidianauctions.shaded.cloud")
        minimize()
    }
}

var junitVersion = "5.9.1"
var cloudVersion = "1.8.1"
var adventureVersion = "4.12.0"

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
    implementation("net.kyori:adventure-platform-bukkit:4.2.0")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly(":vault")
}