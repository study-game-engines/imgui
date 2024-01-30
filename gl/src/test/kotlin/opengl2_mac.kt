package examples


//import org.lwjgl.util.remotery.Remotery
//import org.lwjgl.util.remotery.RemoteryGL
import glm_.vec4.Vec4
import gln.checkError
import gln.glClearColor
import gln.glViewport
import imgui.DEBUG
import imgui.ImGui
import imgui.classes.Context
import imgui.impl.gl.ImplGL2_mac
import imgui.impl.glfw.ImplGlfw
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack
import uno.gl.GlWindow
import uno.glfw.GlfwWindow
import uno.glfw.VSync
import uno.glfw.glfw

// TODO refresh
fun main() {
    ImGuiOpenGL2_Mac()
}

private class ImGuiOpenGL2_Mac {

    val window: GlWindow
    val ctx: Context

    var f = 0f
    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)

    // Our state
    var showAnotherWindow = false
    var showDemo = true
    var counter = 0

    var implGlfw: ImplGlfw
    var implGl2_mac: ImplGL2_mac

//    val rmt = MemoryUtil.memAllocPointer(1).also { Remotery.rmt_CreateGlobalInstance(it) }

    init {

        // Setup window
        glfw {
            errorCB = { error, description -> println("Glfw Error $error: $description") }
            GLFW.glfwInit()
            hints.context {
                debug = DEBUG
                version = "2.0"
            }
        }

        // Create window with graphics context
        val glfwWindow = GlfwWindow(1280, 720, "Dear ImGui GLFW+OpenGL3 OpenGL example")
        window = GlWindow(glfwWindow)
        window.makeCurrent()
        glfw.swapInterval = VSync.ON   // Enable vsync
        // Initialize OpenGL loader
        GL.createCapabilities()
        // Setup Dear ImGui context
        ctx = Context()
        //io.configFlags = io.configFlags or ConfigFlag.NavEnableKeyboard  // Enable Keyboard Controls
        //io.configFlags = io.configFlags or ConfigFlag.NavEnableGamepad   // Enable Gamepad Controls

        // Setup Dear ImGui style
        ImGui.styleColorsDark()
//        ImGui.styleColorsClassic()

        // Setup Platform/Renderer bindings
        implGlfw = ImplGlfw(window, true)
        implGl2_mac = ImplGL2_mac()
//
//        RemoteryGL.rmt_BindOpenGL()

        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        val fh = 0.5
        val fw = fh * window.size.aspect
        glFrustum(-fw, fw, -fh, fh, 1.0, 1_000.0)
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
        glTranslatef(0.0f, 0.0f, -5.0f)

        // Load Fonts
        /*  - If no fonts are loaded, dear imgui will use the default font. You can also load multiple fonts and use
                pushFont()/popFont() to select them.
            - addFontFromFileTTF() will return the Font so you can store it if you need to select the font among multiple.
            - If the file cannot be loaded, the function will return null. Please handle those errors in your application
                (e.g. use an assertion, or display an error and quit).
            - The fonts will be rasterized at a given size (w/ oversampling) and stored into a texture when calling
                FontAtlas.build()/getTexDataAsXXXX(), which ImGui_ImplXXXX_NewFrame below will call.
            - Read 'docs/FONTS.txt' for more instructions and details.
            - Remember that in C/C++ if you want to include a backslash \ in a string literal you need to write
                a double backslash \\ ! */
        //io.Fonts->AddFontDefault();
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Roboto-Medium.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Cousine-Regular.ttf", 15.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/DroidSans.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/ProggyTiny.ttf", 10.0f);
//        ImGui.io.fonts.addFontFromFileTTF("extraFonts/ArialUni.ttf", 16f, glyphRanges = imgui.font.glyphRanges.japanese)!!
//        val a = IO.fonts.addFontFromFileTTF("misc/fonts/ArialUni.ttf", 18f)!!
//        val b = IO.fonts.addFontFromFileTTF("misc/fonts/ArialUni.ttf", 30f)!!

        /*  Main loop
            This automatically also polls events, swaps buffers and resets the appBuffer

            Poll and handle events (inputs, window resize, etc.)
            You can read the io.wantCaptureMouse, io.wantCaptureKeyboard flags to tell if dear imgui wants to use your inputs.
            - When io.wantCaptureMouse is true, do not dispatch mouse input data to your main application.
            - When io.wantCaptureKeyboard is true, do not dispatch keyboard input data to your main application.
            Generally you may always pass all inputs to dear imgui, and hide them from your application based on those two flags.          */
        window.loop(::mainLoop)

        implGl2_mac.shutdown()
        implGlfw.shutdown()
        ctx.destroy()

//        Remotery.rmt_DestroyGlobalInstance(rmt.get(0))

        window.destroy()
        glfw.terminate()
    }

    fun mainLoop(stack: MemoryStack) {

//        RemoteryGL.rmt_BeginOpenGLSample("imgui", null)

        // Start the Dear ImGui frame
        implGl2_mac.newFrame()
        implGlfw.newFrame()

        // Rendering
        ImGui.render()
        glViewport(window.framebufferSize)
        glClearColor(clearColor)
        glClear(GL_COLOR_BUFFER_BIT)

        // Draw A Quad
        glBegin(GL_QUADS)
        glColor3f(0.0f, 1.0f, 1.0f)   // set the color of the quad
        glVertex3f(-1.0f, 1.0f, 0.0f)   // Top Left
        glVertex3f(1.0f, 1.0f, 0.0f)   // Top Right
        glVertex3f(1.0f, -1.0f, 0.0f)   // Bottom Right
        glVertex3f(-1.0f, -1.0f, 0.0f)   // Bottom Left
        // Done Drawing The Quad
        glEnd()

//        implGl2.renderDrawData(ImGui.drawData!!)

        if (DEBUG)
            checkError("mainLoop")

//        RemoteryGL.rmt_EndOpenGLSample()
    }
}