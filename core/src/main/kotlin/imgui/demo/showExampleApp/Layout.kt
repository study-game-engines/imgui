package imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endTabBar
import imgui.ImGui.endTabItem
import imgui.ImGui.frameHeightWithSpacing
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import imgui.button
import imgui.child
import imgui.group
import imgui.menu
import imgui.menuBar
import imgui.menuItem
import kool.getValue
import kool.setValue
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf

object Layout {

    var selected = 0

    /** Demonstrate create a window with multiple child windows.    */
    operator fun invoke(pOpen: KMutableProperty0<Boolean>) {

        var open by pOpen

        setNextWindowSize(Vec2(500, 440), Cond.FirstUseEver)
        if (begin("Example: Simple layout", pOpen, Wf.MenuBar)) {
            menuBar {
                menu("File") {
                    menuItem("Close", "Ctrl+W") { open = false }
                }
            }

            // Left
            child("left pane", Vec2(150, 0), true) {
                for (i in 0..99) {
                    // FIXME: Good candidate to use ImGuiSelectableFlags_SelectOnNav
                    val label = "MyObject $i"
                    if (selectable(label, selected == i))
                        selected = i
                }
            }
            sameLine()

            // Right
            group {
                beginChild("item view", Vec2(0, -frameHeightWithSpacing)) // Leave room for 1 line below us
                text("MyObject: $selected")
                separator()
                if (beginTabBar("##Tabs")) {
                    if (beginTabItem("Description")) {
                        textWrapped("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ")
                        endTabItem()
                    }
                    if (beginTabItem("Details")) {
                        text("ID: 0123456789")
                        endTabItem()
                    }
                    endTabBar()
                }
                endChild()
                button("Revert") {}
                sameLine()
                button("Save") {}
            }
        }
        end()
    }
}