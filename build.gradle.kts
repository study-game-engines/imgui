plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.20"
    id("com.google.devtools.ksp") version "1.8.20-1.0.10" apply false
}

dependencies {
    api(project(":core"))
    api(project(":gl"))
}
