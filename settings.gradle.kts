pluginManagement {
    val kotlinVersion: String by settings
    val dokkaPluginVersion: String by settings
    val koverPluginVersion: String by settings
    val releasePluginVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("org.jetbrains.dokka") version dokkaPluginVersion
        id("org.jetbrains.kotlinx.kover") version koverPluginVersion
        id("net.researchgate.release") version releasePluginVersion
    }
}
