package imgui.demo.showExampleApp

import glm_.has
import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui
import imgui.ImGui.io
import imgui.ImGui.isMousePosValid
import imgui.ImGui.mainViewport
import imgui.ImGui.menuItem
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowBgAlpha
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.text
import imgui.div
import imgui.dsl.menuItem
import imgui.dsl.popupContextWindow
import imgui.dsl.window
import imgui.or
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf


//-----------------------------------------------------------------------------
// [SECTION] Example App: Simple overlay / ShowExampleAppSimpleOverlay()
//-----------------------------------------------------------------------------

// Demonstrate creating a simple static window with no decoration
// + a context-menu to choose which corner of the screen to use.
object SimpleOverlay {

    var location = 0

    // Demonstrate creating a simple static window with no decoration
    // + a context-menu to choose which corner of the screen to use.
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        var windowFlags = Wf.NoDecoration or Wf.AlwaysAutoResize or Wf.NoSavedSettings or Wf.NoFocusOnAppearing or Wf.NoNav
        if (location >= 0) {
            val PAD = 10f
            val viewport = mainViewport
            val workPos = viewport.workPos // Use work area to avoid menu-bar/task-bar, if any!
            val workSize = viewport.workSize
            val windowPos = Vec2(workPos.x + if (location has 1) workSize.x - PAD else PAD,
                    workPos.y + if (location has 2) workSize.y - PAD else PAD)
            val windowPosPivot = Vec2(if (location has 1) 1f else 0f,
                    if (location has 2) 1f else 0f)
            setNextWindowPos(windowPos, Cond.Always, windowPosPivot)
            windowFlags = windowFlags or Wf.NoMove
        } else if (location == -2) {
            // Center window
            setNextWindowPos(ImGui.mainViewport.center, Cond.Always, Vec2(0.5f))
            windowFlags /= Wf.NoMove
        }
        setNextWindowBgAlpha(0.35f)  // Transparent background
        window("Example: Simple overlay", open, windowFlags) {
            text("Simple overlay\n(right-click to change position)")
            separator()
            text("Mouse Position: " + when {
                isMousePosValid() -> "(%.1f,%.1f)".format(io.mousePos.x, io.mousePos.y)
                else -> "<invalid>"
            })
            popupContextWindow {
                menuItem("Custom", "", location == -1) { location = -1 }
                menuItem("Center", "", location == -2) { location = -2 }
                menuItem("Top-left", "", location == 0) { location = 0 }
                menuItem("Top-right", "", location == 1) { location = 1 }
                menuItem("Bottom-left", "", location == 2) { location = 2 }
                menuItem("Bottom-right", "", location == 3) { location = 3 }
                if (open() && menuItem("Close")) open.set(false)
            }
        }
    }
}