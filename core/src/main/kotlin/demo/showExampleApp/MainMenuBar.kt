package imgui.demo.showExampleApp

import imgui.ImGui.menuItem
import imgui.ImGui.separator
import imgui.mainMenuBar
import imgui.menu

/** Demonstrate creating a "main" fullscreen menu bar and populating it.
 *  Note the difference between BeginMainMenuBar() and BeginMenuBar():
 *  - BeginMenuBar() = menu-bar inside current window (which needs the ImGuiWindowFlags_MenuBar flag!)
 *  - BeginMainMenuBar() = helper to create menu-bar-sized window at the top of the main viewport + call BeginMenuBar() into it.   */
object MainMenuBar {

    operator fun invoke() = mainMenuBar {
        menu("File") { MenuFile() }
        menu("Edit") {
            menuItem("Undo", "CTRL+Z")
            menuItem("Redo", "CTRL+Y", false, false) // Disabled item
            separator()
            menuItem("Cut", "CTRL+X")
            menuItem("Copy", "CTRL+C")
            menuItem("Paste", "CTRL+V")
        }
    }
}