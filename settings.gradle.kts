pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

include("core")
include("gl")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven(url = "https://raw.githubusercontent.com/kotlin-graphics/mary/master")
        maven(url = "https://jitpack.io")
    }
}