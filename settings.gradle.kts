pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}
include("plugin")
include("server_1.19.2")

rootProject.name = "ObsidianAuctions"