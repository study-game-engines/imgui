plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(kotlin("reflect"))
    api("kotlin.graphics:uno-core:0.7.21") // todo drop dependency
    implementation("com.github.livefront.sealed-enum:runtime:0.7.0")
    ksp("com.github.livefront.sealed-enum:ksp:0.7.0")

    // lwjgl
    api("org.lwjgl:lwjgl:3.3.3")
    api("org.lwjgl:lwjgl:3.3.3:natives-windows")
    api("org.lwjgl:lwjgl:3.3.3:natives-macos")
    api("org.lwjgl:lwjgl-opengl:3.3.3")
    api("org.lwjgl:lwjgl-opengl:3.3.3:natives-windows")
    api("org.lwjgl:lwjgl-opengl:3.3.3:natives-macos")
    api("org.lwjgl:lwjgl-glfw:3.3.3")
    api("org.lwjgl:lwjgl-glfw:3.3.3:natives-windows")
    api("org.lwjgl:lwjgl-jawt:3.3.3")
}
