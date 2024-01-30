plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(project(":core"))
    api("kotlin.graphics:uno:0.7.21")
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
