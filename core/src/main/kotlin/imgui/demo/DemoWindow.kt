package imgui.demo

import glm_.f
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginDisabled
import imgui.ImGui.beginTable
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.checkboxFlags
import imgui.ImGui.end
import imgui.ImGui.endDisabled
import imgui.ImGui.endTable
import imgui.ImGui.fontSize
import imgui.ImGui.io
import imgui.ImGui.isPressed
import imgui.ImGui.logButtons
import imgui.ImGui.logFinish
import imgui.ImGui.logText
import imgui.ImGui.logToClipboard
import imgui.ImGui.mainViewport
import imgui.ImGui.menuItem
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushItemWidth
import imgui.ImGui.sameLine
import imgui.ImGui.separatorText
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.showDebugLogWindow
import imgui.ImGui.showMetricsWindow
import imgui.ImGui.showStackToolWindow
import imgui.ImGui.showUserGuide
import imgui.ImGui.spacing
import imgui.ImGui.tableNextColumn
import imgui.ImGui.text
import imgui.ImGui.time
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.api.demoDebugInformations.ShowAboutWindow
import imgui.classes.TextFilter
import imgui.demo.showExampleApp.*
import imgui.collapsingHeader
import imgui.menu
import imgui.menuBar
import imgui.treeNode
import imgui.window
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf

// Demonstrate most Dear ImGui features (this is big function!)
// You may execute this function to experiment with the UI and understand what it does.
// You may then search for keywords in the code when you are interested by a specific feature.
object DemoWindow {

    object show {
        // Examples Apps (accessible from the "Examples" menu)
        var mainMenuBar = false
        var documents = false
        var console = false
        var log = false
        var layout = false
        var propertyEditor = false
        var longText = false
        var autoResize = false
        var constrainedResize = false
        var simpleOverlay = false
        var fullscreen = false
        var windowTitles = false
        var customRendering = false

        // Dear ImGui Tools/Apps (accessible from the "Tools" menu)
        var metrics = false
        var debugLog = false
        var stackTool = false
        var about = false
        var styleEditor = false
    }

    // Demonstrate the various window flags. Typically you would just use the default!
    var noTitlebar = false
    var noScrollbar = false
    var noMenu = false
    var noMove = false
    var noResize = false
    var noCollapse = false
    var noClose = false
    var noNav = false
    var noBackground = false
    var noBringToFront = false
    var unsavedDocument = false

    var filter = TextFilter()
    var lineWidth = 1f

    operator fun invoke(open_: KMutableProperty0<Boolean>?) {

        // Exceptionally add an extra assert here for people confused about initial Dear ImGui setup
        // Most functions would normally just crash if the context is missing.
        check(ImGui.currentContext != null) { "Missing dear imgui context. Refer to examples app!" }

        var open = open_

        if (show.mainMenuBar) MainMenuBar()
        if (show.documents) Documents(show::documents)
        if (show.console) Console(show::console)
        if (show.log) Log(show::log)
        if (show.layout) Layout(show::layout)
        if (show.propertyEditor) PropertyEditor(show::propertyEditor)
        if (show.longText) LongText(show::longText)
        if (show.autoResize) AutoResize(show::autoResize)
        if (show.constrainedResize) ConstrainedResize(show::constrainedResize)
        if (show.simpleOverlay) SimpleOverlay(show::simpleOverlay)
        if (show.fullscreen) Fullscreen(show::fullscreen)
        if (show.windowTitles) WindowTitles()
        if (show.customRendering) CustomRendering(show::customRendering)

        if (show.metrics)
            showMetricsWindow(show::metrics)
        if (show.debugLog)
            showDebugLogWindow(show::debugLog)
        if (show.stackTool)
            showStackToolWindow(show::stackTool)
        if (show.about)
            ShowAboutWindow(show::about)
        if (show.styleEditor)
            window("Dear ImGui Style Editor", show::styleEditor) {
                StyleEditor()
            }

        var windowFlags: WindowFlags = none
        if (noTitlebar) windowFlags /= Wf.NoTitleBar
        if (noScrollbar) windowFlags /= Wf.NoScrollbar
        if (!noMenu) windowFlags /= Wf.MenuBar
        if (noMove) windowFlags /= Wf.NoMove
        if (noResize) windowFlags /= Wf.NoResize
        if (noCollapse) windowFlags /= Wf.NoCollapse
        if (noNav) windowFlags /= Wf.NoNav
        if (noBackground) windowFlags /= Wf.NoBackground
        if (noBringToFront) windowFlags /= Wf.NoBringToFrontOnFocus
        if (unsavedDocument) windowFlags /= Wf.UnsavedDocument
        if (noClose) open = null // Don't pass our bool* to Begin

        // We specify a default position/size in case there's no data in the .ini file.
        // We only do it to make the demo applications a little more welcoming, but typically this isn't required.
        val mainViewport = mainViewport
        setNextWindowPos(Vec2(mainViewport.workPos.x + 650, mainViewport.workPos.y + 20), Cond.FirstUseEver)
        setNextWindowSize(Vec2(550, 680), Cond.FirstUseEver)

        // Main body of the Demo window starts here.
        if (!begin("Dear ImGui Demo", open, windowFlags)) {
            // Early out if the window is collapsed, as an optimization.
            end()
            return
        }

        // Most "big" widgets share a common width settings by default. See 'Demo->Layout->Widgets Width' for details.
        // e.g. Use 2/3 of the space for widgets and 1/3 for labels (right align)
        //ImGui::PushItemWidth(ImGui::GetWindowWidth() * 0.35f);
        // e.g. Leave a fixed amount of width for labels (by passing a negative value), the rest goes to widgets.
        pushItemWidth(fontSize * -12)

        // Menu Bar
        menuBar {
            menu("Menu") { MenuFile() }
            //            stop = true
            //            println("nav window name " + g.navWindow?.rootWindow?.name)
            //            println("Examples")
            menu("Examples") {
                menuItem("Main menu bar", "", show::mainMenuBar)
                menuItem("Console", "", show::console)
                menuItem("Log", "", show::log)
                menuItem("Simple layout", "", show::layout)
                menuItem("Property editor", "", show::propertyEditor)
                menuItem("Long text display", "", show::longText)
                menuItem("Auto-resizing window", "", show::autoResize)
                menuItem("Constrained-resizing window", "", show::constrainedResize)
                menuItem("Simple overlay", "", show::simpleOverlay)
                menuItem("Fullscreen window", "", show::fullscreen)
                menuItem("Manipulating window titles", "", show::windowTitles)
                menuItem("Custom rendering", "", show::customRendering)
                menuItem("Documents", "", show::documents)
            }
            //if (ImGui::MenuItem("MenuItem")) {} // You can also use MenuItem() inside a menu bar!
            menu("Tools") {

                val hasDebugTools = !IMGUI_DISABLE_DEBUG_TOOLS

                menuItem("Metrics/Debugger", "", show::metrics, hasDebugTools)
                menuItem("Debug Log", "", show::debugLog, hasDebugTools)
                menuItem("Stack Tool", "", show::stackTool, hasDebugTools)
                menuItem("Style Editor", "", show::styleEditor)
                menuItem("About Dear ImGui", "", show::about)
            }
        }

        text("dear imgui says hello! ($IMGUI_VERSION) ($IMGUI_VERSION_NUM)")
        spacing()

        collapsingHeader("Help") {

            separatorText("ABOUT THIS DEMO:")
            bulletText("Sections below are demonstrating many aspects of the library.")
            bulletText("The \"Examples\" menu above leads to more demo contents.")
            bulletText("The \"Tools\" menu above gives access to: About Box, Style Editor,\n" +
                    "and Metrics/Debugger (general purpose Dear ImGui debugging tool).")

            separatorText("PROGRAMMER GUIDE:")
            bulletText("See the ShowDemoWindow() code in imgui_demo.cpp. <- you are here!")
            bulletText("See comments in imgui.cpp.")
            bulletText("See example applications in the examples/ folder.")
            bulletText("Read the FAQ at http://www.dearimgui.org/faq/")
            bulletText("Set 'io.ConfigFlags |= NavEnableKeyboard' for keyboard controls.")
            bulletText("Set 'io.ConfigFlags |= NavEnableGamepad' for gamepad controls.")

            separatorText("USER GUIDE:")
            showUserGuide()
        }

        collapsingHeader("Configuration") {

            treeNode("Configuration##2") {
                separatorText("General")
                checkboxFlags("io.ConfigFlags: NavEnableKeyboard", io::configFlags, ConfigFlag.NavEnableKeyboard)
                sameLine(); helpMarker("Enable keyboard controls.")
                checkboxFlags("io.ConfigFlags: NavEnableGamepad", io::configFlags, ConfigFlag.NavEnableGamepad)
                sameLine(); helpMarker("Enable gamepad controls. Require backend to feed in gamepad inputs in io.NavInputs[] and set io.BackendFlags |= ImGuiBackendFlags_HasGamepad.\n\nRead instructions in imgui.cpp for details.")
                checkboxFlags("io.ConfigFlags: NavEnableSetMousePos", io::configFlags, ConfigFlag.NavEnableSetMousePos)
                sameLine(); helpMarker("Instruct navigation to move the mouse cursor. See comment for ImGuiConfigFlags_NavEnableSetMousePos.")
                checkboxFlags("io.ConfigFlags: NoMouse", io::configFlags, ConfigFlag.NoMouse)
                if (io.configFlags has ConfigFlag.NoMouse) {
                    // The "NoMouse" option can get us stuck with a disabled mouse! Let's provide an alternative way to fix it:
                    if ((time.f % 0.4f) < 0.2f) {
                        sameLine()
                        text("<<PRESS SPACE TO DISABLE>>")
                    }
                    if (Key.Space.isPressed)
                        io.configFlags = io.configFlags wo ConfigFlag.NoMouse
                }
                checkboxFlags("io.ConfigFlags: NoMouseCursorChange", io::configFlags, ConfigFlag.NoMouseCursorChange)
                sameLine(); helpMarker("Instruct backend to not alter mouse cursor shape and visibility.")
                checkbox("io.ConfigInputTrickleEventQueue", io::configInputTrickleEventQueue)
                sameLine(); helpMarker("Enable input queue trickling: some types of events submitted during the same frame (e.g. button down + up) will be spread over multiple frames, improving interactions with low framerates.")
                checkbox("io.MouseDrawCursor", io::mouseDrawCursor)
                sameLine(); helpMarker("Instruct Dear ImGui to render a mouse cursor itself. Note that a mouse cursor rendered via your application GPU rendering path will feel more laggy than hardware cursor, but will be more in sync with your other visuals.\n\nSome desktop applications may use both kinds of cursors (e.g. enable software cursor only when resizing/dragging something).")

                separatorText("Widgets")
                checkbox("io.ConfigCursorBlink", io::configInputTextCursorBlink)
                sameLine(); helpMarker("Enable blinking cursor (optional as some users consider it to be distracting).")
                checkbox("io.ConfigInputTextEnterKeepActive", io::configInputTextEnterKeepActive)
                sameLine(); helpMarker("Pressing Enter will keep item active and select contents (single-line only).")
                checkbox("io.ConfigDragClickToInputText", io::configDragClickToInputText)
                sameLine(); helpMarker("Enable turning DragXXX widgets into text input with a simple mouse click-release (without moving).")
                checkbox("io.ConfigWindowsResizeFromEdges", io::configWindowsResizeFromEdges)
                sameLine(); helpMarker("Enable resizing of windows from their edges and from the lower-left corner.\nThis requires (io.BackendFlags & ImGuiBackendFlags_HasMouseCursors) because it needs mouse cursor feedback.")
                checkbox("io.configWindowsMoveFromTitleBarOnly", io::configWindowsMoveFromTitleBarOnly)
                checkbox("io.ConfigMacOSXBehaviors", io::configMacOSXBehaviors)
                text("Also see Style->Rendering for rendering options.")

                separatorText("Debug")
                beginDisabled()
                checkbox("io.ConfigDebugBeginReturnValueOnce", io::configDebugBeginReturnValueOnce) // .
                endDisabled()
                sameLine(); helpMarker("First calls to Begin()/BeginChild() will return false.\n\nTHIS OPTION IS DISABLED because it needs to be set at application boot-time to make sense. Showing the disabled option is a way to make this feature easier to discover")
                checkbox("io.ConfigDebugBeginReturnValueLoop", io::configDebugBeginReturnValueLoop)
                sameLine(); helpMarker("Some calls to Begin()/BeginChild() will return false.\n\nWill cycle through window depths then repeat. Windows should be flickering while running.")
                checkbox("io.ConfigDebugIgnoreFocusLoss", io::configDebugIgnoreFocusLoss)
                sameLine(); helpMarker("Option to deactivate io.AddFocusEvent(false) handling. May facilitate interactions with a debugger when focus loss leads to clearing inputs data.")
                checkbox("io.ConfigDebugIniSettings", io::configDebugIniSettings);
                sameLine(); helpMarker("Option to save .ini data with extra comments (particularly helpful for Docking, but makes saving slower).")

                spacing()
            }
            treeNode("Backend Flags") {
                helpMarker("""
                    Those flags are set by the backends (imgui_impl_xxx files) to specify their capabilities.
                    Here we expose them as read-only fields to avoid breaking interactions with your backend.""".trimIndent())

                // FIXME: Maybe we need a BeginReadonly() equivalent to keep label bright?
                beginDisabled()
                checkboxFlags("io.BackendFlags: HasGamepad", io::backendFlags, BackendFlag.HasGamepad)
                checkboxFlags("io.BackendFlags: HasMouseCursors", io::backendFlags, BackendFlag.HasMouseCursors)
                checkboxFlags("io.BackendFlags: HasSetMousePos", io::backendFlags, BackendFlag.HasSetMousePos)
                checkboxFlags("io.BackendFlags: RendererHasVtxOffset", io::backendFlags, BackendFlag.RendererHasVtxOffset)
                endDisabled()
                spacing()
            }

            treeNode("Style") {
                helpMarker("The same contents can be accessed in 'Tools->Style Editor' or by calling the ShowStyleEditor() function.")
                StyleEditor()
                spacing()
            }

            treeNode("Capture/Logging") {
                helpMarker("""
                    The logging API redirects all text output so you can easily capture the content of a window or a block. Tree nodes can be automatically expanded.
                    Try opening any of the contents below in this window and then click one of the "Log To" button.""".trimIndent())
                logButtons()

                helpMarker("You can also call ImGui::LogText() to output directly to the log without a visual output.")
                if (button("""Copy "Hello, world!" to clipboard""")) {
                    logToClipboard()
                    logText("Hello, world!")
                    logFinish()
                }
            }
        }

        collapsingHeader("Window options") {
            if (beginTable("split", 3)) {
                tableNextColumn(); checkbox("No titlebar", ::noTitlebar); sameLine(150)
                tableNextColumn(); checkbox("No scrollbar", ::noScrollbar); sameLine(300)
                tableNextColumn(); checkbox("No menu", ::noMenu)
                tableNextColumn(); checkbox("No move", ::noMove); sameLine(150)
                tableNextColumn(); checkbox("No resize", ::noResize); sameLine(300)
                tableNextColumn(); checkbox("No collapse", ::noCollapse)
                tableNextColumn(); checkbox("No close", ::noClose); sameLine(150)
                tableNextColumn(); checkbox("No nav", ::noNav); sameLine(300)
                tableNextColumn(); checkbox("No background", ::noBackground)
                tableNextColumn(); checkbox("No bring to front", ::noBringToFront)
                tableNextColumn(); checkbox("Unsaved document", ::unsavedDocument)
                endTable()
            }
        }

        // All demo contents
        ShowDemoWindowWidgets()
        ShowDemoWindowLayout()
        ShowDemoWindowPopups()
        ShowDemoWindowTables()
        ShowDemoWindowInputs()

        // End of ShowDemoWindow()
        popItemWidth()
        end()
    }
}