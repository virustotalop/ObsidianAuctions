plugins {
    id("java")
}

group = "com.gmail.virustotalop"
version = "5.0.1"

repositories {
    mavenCentral()
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

