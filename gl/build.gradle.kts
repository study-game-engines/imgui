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
}

kotlin.jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
}

tasks {
    withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn", "-Xallow-kotlin-package")
    }
}

/*examples*/

sourceSets.create("examples") {
    java.srcDirs(File("D:\\workspace\\imgui\\gl\\src\\examples\\kotlin"))
    compileClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().compileClasspath
}

tasks.create("demoJar", Jar::class) {
    manifest {
        attributes["Main-Class"] = "examples.DemoKt"
    }
    archiveBaseName.set("demo")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.getByName("examples").runtimeClasspath.filter { file -> file.exists() }.map { if (it.isDirectory()) it else zipTree(it) })
    from(sourceSets.getByName("examples").output) // https://stackoverflow.com/a/44815444/909169
    with(tasks.jar.get())
}
