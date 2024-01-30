import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(projects.core)
    api("kotlin.graphics:uno:0.7.21")

    // lwjgl
    implementation("org.lwjgl:lwjgl:3.3.3")
    implementation("org.lwjgl:lwjgl:3.3.3:natives-windows")
    implementation("org.lwjgl:lwjgl:3.3.3:natives-macos")
    implementation("org.lwjgl:lwjgl-opengl:3.3.3")
    implementation("org.lwjgl:lwjgl-opengl:3.3.3:natives-windows")
    implementation("org.lwjgl:lwjgl-opengl:3.3.3:natives-macos")
    implementation("org.lwjgl:lwjgl-glfw:3.3.3")
    implementation("org.lwjgl:lwjgl-glfw:3.3.3:natives-windows")
    implementation("org.lwjgl:lwjgl-jawt:3.3.3")
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

tasks.create("helloWorldJar", Jar::class) {
    manifest {
        attributes["Main-Class"] = "examples.HelloWorldKt"
    }
    archiveBaseName.set("helloWorld")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.getByName("examples").runtimeClasspath.filter { file -> file.exists() }.map { if (it.isDirectory()) it else zipTree(it) })
    from(sourceSets.getByName("examples").output) // https://stackoverflow.com/a/44815444/909169
    with(tasks.jar.get())
}
