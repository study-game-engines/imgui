import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.lwjgl.Lwjgl.Module.*
import org.lwjgl.lwjgl

plugins {
    kotlin("jvm")
    id("org.lwjgl.plugin")
    id("elect86.magik")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation(projects.core)

    api("kotlin.graphics:uno:0.7.21")
    lwjgl { implementation(jemalloc, glfw, opengl, remotery, stb) }

    testImplementation("com.github.ajalt:mordant:1.2.1")
    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
}

kotlin.jvmToolchain { languageVersion.set(JavaLanguageVersion.of(8)) }

tasks {
    withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn", "-Xallow-kotlin-package")
    }
}
