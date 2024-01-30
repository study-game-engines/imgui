pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include("core")
include("gl")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
        maven("https://jitpack.io")
    }
}