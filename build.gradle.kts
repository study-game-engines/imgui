import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm") version "1.8.20"
    id("org.lwjgl.plugin") version "0.0.34"
    id("elect86.magik") version "0.3.2"
//    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.google.devtools.ksp") version "1.8.20-1.0.10" apply false
}

dependencies {
    api(projects.core)
    api(projects.gl)
}

kotlin.jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
}

tasks {
    withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn", "-Xallow-kotlin-package")
    }
}
