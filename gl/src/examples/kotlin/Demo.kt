package examples

import glm_.vec4.Vec4
import gln.checkError
import gln.glViewport
import imgui.*
import imgui.api.slider
import imgui.classes.Context
import imgui.gl.ImGuiGL3
import imgui.gl.ImplGlfw
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.Platform
import uno.gl.GlWindow
import uno.glfw.GlfwWindow
import uno.glfw.Hints
import uno.glfw.VSync
import uno.glfw.glfw

// Data
lateinit var gAppWindow: GlWindow
lateinit var implGlfw: ImplGlfw
lateinit var gl3: ImGuiGL3

// Our state
// (we use static, which essentially makes the variable globals, as a convenience to keep the example code easy to follow)
var showDemoWindow = true
var showAnotherWindow = false
var clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)
var f = 0f
var counter = 0

class Demo {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = main()
    }
}

private fun main() {

    glfw {
        errorCB = { error, description -> println("Glfw Error $error: $description") }
        init()
        hints.context {
            debug = DEBUG

            // Decide GL+GLSL versions
            when (Platform.get()) {
                // TODO Opengl_es2? GL ES 2.0 + GLSL 100
                Platform.MACOSX -> {    // GL 3.2 + GLSL 150
                    ImGuiGL3.data.glslVersion = 150
                    version = "3.2"
                    profile = Hints.Context.Profile.Core      // 3.2+ only
                    forwardComp = true  // Required on Mac
                }

                else -> {   // GL 3.0 + GLSL 130
                    ImGuiGL3.data.glslVersion = 130
                    version = "3.0"
                    //profile = core      // 3.2+ only
                    //forwardComp = true  // 3.0+ only
                }
            }
        }
    }

    // Create window with graphics context
    val glfwWindow = GlfwWindow(1280, 720, "Dear ImGui GLFW+OpenGL3 OpenGL example")
    gAppWindow = GlWindow(glfwWindow)
    gAppWindow.makeCurrent()
    glfw.swapInterval = VSync.ON   // Enable vsync

    // >> TODO COPY
    // Setup Dear ImGui context
    val ctx = Context()
    val io = ctx.io
    io.configFlags /= ConfigFlag.NavEnableKeyboard  // Enable Keyboard Controls
    io.configFlags /= ConfigFlag.NavEnableGamepad   // Enable Gamepad Controls

    // Setup Dear ImGui style
    ImGui.styleColorsDark()
//        ImGui.styleColorsLight()

    // Setup Platform/Renderer backend
    implGlfw = ImplGlfw(gAppWindow, true)
    gl3 = ImGuiGL3()
    // <<

    gAppWindow.loop {

        // Start the Dear ImGui frame
        gl3.newFrame()
        implGlfw.newFrame()

        ImGui.newFrame()

        // 1. Show the big demo window (Most of the sample code is in ImGui::ShowDemoWindow()! You can browse its code to learn more about Dear ImGui!).
        if (showDemoWindow)
            ImGui.showDemoWindow(::showDemoWindow)

        // 2. Show a simple window that we create ourselves. We use a Begin/End pair to create a named window.
        run {

            ImGui.begin("Hello, world!")                          // Create a window called "Hello, world!" and append into it.

//                if(comboFilter("my combofilter", buf, hints, s) )
//                    println("picking occured")

            ImGui.text("This is some useful text.")                // Display some text (you can use a format strings too)
            ImGui.checkbox("Demo Window", ::showDemoWindow)             // Edit bools storing our window open/close state
            ImGui.checkbox("Another Window", ::showAnotherWindow)

            ImGui.slider("float", ::f, 0f, 1f)   // Edit 1 float using a slider from 0.0f to 1.0f
            ImGui.colorEdit3("clear color", clearColor)           // Edit 3 floats representing a color

            if (ImGui.button("Button"))                           // Buttons return true when clicked (most widgets return true when edited/activated)
                counter++

            ImGui.text("counter = $counter")

            ImGui.text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / ImGui.io.framerate, ImGui.io.framerate)

            ImGui.end()

            // 3. Show another simple window.
            if (showAnotherWindow) {
                // Pass a pointer to our bool variable (the window will have a closing button that will clear the bool when clicked)
                ImGui.begin("Another Window", ::showAnotherWindow)
                ImGui.text("Hello from another window!")
                button("Close Me") { //  this takes advantage of functional programming and pass directly a lambda as last parameter
                    showAnotherWindow = false
                }
                ImGui.end()
            }
        }

        // Rendering
        ImGui.render()
        glViewport(gAppWindow.framebufferSize)
        glClearColor(clearColor.x * clearColor.w, clearColor.y * clearColor.w, clearColor.z * clearColor.w, clearColor.w)
        glClear(GL_COLOR_BUFFER_BIT)

        gl3.renderDrawData(ImGui.drawData!!)

        if (DEBUG)
            checkError("mainLoop")
    }

    gl3.shutdown()
    implGlfw.shutdown()
    ctx.destroy()

    GL.destroy() // TODO -> uno
    gAppWindow.destroy()
    glfw.terminate()
}
