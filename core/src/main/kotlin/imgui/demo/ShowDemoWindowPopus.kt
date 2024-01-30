package imgui.demo

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginMenu
import imgui.ImGui.beginMenuBar
import imgui.ImGui.beginPopupModal
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.collapsingHeader
import imgui.ImGui.colorEdit4
import imgui.ImGui.combo
import imgui.ImGui.endMenu
import imgui.ImGui.endMenuBar
import imgui.ImGui.endPopup
import imgui.ImGui.inputText
import imgui.ImGui.mainViewport
import imgui.ImGui.menuItem
import imgui.ImGui.openPopup
import imgui.ImGui.openPopupOnItemClick
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.separatorText
import imgui.ImGui.setItemDefaultFocus
import imgui.ImGui.setItemTooltip
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.text
import imgui.ImGui.textEx
import imgui.ImGui.textWrapped
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.api.drag
import imgui.demo.showExampleApp.MenuFile
import imgui.dsl.button
import imgui.dsl.menu
import imgui.dsl.menuBar
import imgui.dsl.popup
import imgui.dsl.popupContextItem
import imgui.dsl.popupModal
import imgui.dsl.treeNode
import imgui.dsl.withStyleVar
import imgui.WindowFlag as Wf

object ShowDemoWindowPopups {

    operator fun invoke() {

        if (!collapsingHeader("Popups & Modal windows"))
            return

        Popus()

        `Context Menus`()

        Modals()

        treeNode("Menus inside a regular window") {
            textWrapped("Below we are testing adding menu items to a regular window. It's rather unusual but should work!")
            separator()

            menuItem("Menu item", "CTRL+M")
            menu("Menu inside a regular window") {
                MenuFile()
            }
            separator()
        }
    }

    object Popus {
        var selectedFish = -1
        val toggles = booleanArrayOf(true, false, false, false, false)
        operator fun invoke() {
            // The properties of popups windows are:
            // - They block normal mouse hovering detection outside them. (*)
            // - Unless modal, they can be closed by clicking anywhere outside them, or by pressing ESCAPE.
            // - Their visibility state (~bool) is held internally by Dear ImGui instead of being held by the programmer as
            //   we are used to with regular Begin() calls. User can manipulate the visibility state by calling OpenPopup().
            // (*) One can use IsItemHovered(ImGuiHoveredFlags_AllowWhenBlockedByPopup) to bypass it and detect hovering even
            //     when normally blocked by a popup.
            // Those three properties are connected. The library needs to hold their visibility state BECAUSE it can close
            // popups at any time.

            // Typical use for regular windows:
            //   bool my_tool_is_active = false; if (ImGui::Button("Open")) my_tool_is_active = true; [...] if (my_tool_is_active) Begin("My Tool", &my_tool_is_active) { [...] } End();
            // Typical use for popups:
            //   if (ImGui::Button("Open")) ImGui::OpenPopup("MyPopup"); if (ImGui::BeginPopup("MyPopup") { [...] EndPopup(); }
            // With popups we have to go through a library call (here OpenPopup) to manipulate the visibility state.
            // This may be a bit confusing at first but it should quickly make sense. Follow on the examples below.
            treeNode("Popups") {

                textWrapped("When a popup is active, it inhibits interacting with windows that are behind the popup. " +
                        "Clicking outside the popup closes it.")

                val names = arrayOf("Bream", "Haddock", "Mackerel", "Pollock", "Tilefish")

                // Simple selection popup (if you want to show the current selection inside the Button itself,
                // you may want to build a string using the "###" operator to preserve a constant ID with a variable label)
                if (button("Select.."))
                    openPopup("my_select_popup")
                sameLine()
                textEx(names.getOrElse(selectedFish) { "<None>" })
                popup("my_select_popup") {
                    separatorText("Aquarium")
                    names.forEachIndexed { i, n -> if (selectable(n)) selectedFish = i }
                }

                // Showing a menu with toggles
                if (button("Toggle.."))
                    openPopup("my_toggle_popup")
                popup("my_toggle_popup") {
                    names.forEachIndexed { i, n -> menuItem(n, "", toggles mutablePropertyAt i) }

                    menu("Sub-menu") { menuItem("Click me") }

                    separator()
                    text("Tooltip here")
                    setItemTooltip("I am a tooltip over a popup")

                    if (button("Stacked Popup")) openPopup("another popup")
                    popup("another popup") {
                        names.forEachIndexed { i, n -> menuItem(n, "", toggles mutablePropertyAt i) }
                        menu("Sub-menu") {
                            menuItem("Click me")
                            button("Stacked Popup") { openPopup("another popup") }
                            popup("another popup") { text("I am the last one here.") }
                        }
                    }
                }

                // Call the more complete ShowExampleMenuFile which we use in various places of this demo
                if (button("With a menu.."))
                    openPopup("my_file_popup")
                popup("my_file_popup") {
                    menuBar {
                        menu("File") {
                            MenuFile()
                        }
                        menu("Edit") {
                            menuItem("Dummy")
                        }
                    }
                    text("Hello from popup!")
                    button("This is a dummy button..")
                }
            }
        }
    }

    object `Context Menus` {
        var selected = -1
        var value = 0.5f

        // [JVM] this needs to by a ByteArray to hold a reference, since Strings are final by design
        var name = "Label1"
        operator fun invoke() {
            treeNode("Context menus") {

                helpMarker(""""Context" functions are simple helpers to associate a Popup to a given Item or Window identifier.""")

                // BeginPopupContextItem() is a helper to provide common/simple popup behavior of essentially doing:
                //     if (id == 0)
                //         id = GetItemID(); // Use last item id
                //     if (IsItemHovered() && IsMouseReleased(ImGuiMouseButton_Right))
                //         OpenPopup(id);
                //     return BeginPopup(id);
                // For advanced uses you may want to replicate and customize this code.
                // See more details in BeginPopupContextItem().

                // Example 1
                // When used after an item that has an ID (e.g. Button), we can skip providing an ID to BeginPopupContextItem(),
                // and BeginPopupContextItem() will use the last item ID as the popup ID.
                run {
                    val names = listOf("Label1", "Label2", "Label3", "Label4", "Label5")
                    for (n in names.indices) {
                        val name = names[n]
                        if (selectable(name, selected == n))
                            selected = n
                        popupContextItem { // <-- use last item id as popup id
                            selected = n
                            text("""This a popup for "$name"!""")
                            if (button("Close"))
                                closeCurrentPopup()
                        }
                        setItemTooltip("Right-click to open popup")
                    }
                }

                // Example 2
                // Popup on a Text() element which doesn't have an identifier: we need to provide an identifier to BeginPopupContextItem().
                // Using an explicit identifier is also convenient if you want to activate the popups from different locations.
                run {
                    helpMarker("Text() elements don't have stable identifiers so we need to provide one.")
                    text("Value = %.3f <-- (1) right-click this text", value)
                    popupContextItem("my popup") {
                        if (selectable("Set to zero")) value = 0.0f
                        if (selectable("Set to PI")) value = 3.1415f
                        setNextItemWidth(-Float.MIN_VALUE)
                        drag("##Value", ::value, 0.1f, 0f, 0f)
                    }

                    // We can also use OpenPopupOnItemClick() to toggle the visibility of a given popup.
                    // Here we make it that right-clicking this other text element opens the same popup as above.
                    // The popup itself will be submitted by the code above.
                    text("(2) Or right-click this text")
                    openPopupOnItemClick("my popup", PopupFlag.MouseButtonRight)

                    // Back to square one: manually open the same popup.
                    if (button("(3) Or click this button"))
                        openPopup("my popup")
                }

                // Example 3
                // When using BeginPopupContextItem() with an implicit identifier (NULL == use last item ID),
                // we need to make sure your item identifier is stable.
                // In this example we showcase altering the item label while preserving its identifier, using the ### operator (see FAQ).
                run {
                    helpMarker("Showcase using a popup ID linked to item ID, with the item having a changing label + stable ID using the ### operator.")
                    val buf = "Button: $name###Button" // ### operator override ID ignoring the preceding label
                    button(buf)
                    popupContextItem {
                        text("Edit name:")
                        inputText("##edit", ::name)
                        if (button("Close"))
                            closeCurrentPopup()
                    }
                    sameLine(); text("(<-- right-click here)")
                }
            }
        }
    }

    object Modals {
        var dontAskMeNextTime = false
        var item = 1
        var color = Vec4(0.4f, 0.7f, 0f, 0.5f)
        operator fun invoke() {
            treeNode("Modals") {

                textWrapped("Modal windows are like popups but the user cannot close them by clicking outside.")

                if (button("Delete.."))
                    openPopup("Delete?")

                // Always center this window when appearing
                val center = mainViewport.center
                setNextWindowPos(center, Cond.Appearing, Vec2(0.5f))

                popupModal("Delete?", null, Wf.AlwaysAutoResize) {

                    text("All those beautiful files will be deleted.\nThis operation cannot be undone!")
                    separator()

                    //static int unused_i = 0;
                    //ImGui::Combo("Combo", &unused_i, "Delete\0Delete harder\0");

                    withStyleVar(StyleVar.FramePadding, Vec2()) {
                        checkbox("Don't ask me next time", ::dontAskMeNextTime)
                    }

                    button("OK", Vec2(120, 0)) { closeCurrentPopup() }
                    setItemDefaultFocus()
                    sameLine()
                    button("Cancel", Vec2(120, 0)) { closeCurrentPopup() }
                }

                button("Stacked modals..") { openPopup("Stacked 1") }
                popupModal("Stacked 1", null, Wf.MenuBar) {

                    if (beginMenuBar()) {
                        if (beginMenu("File")) {
                            if (menuItem("Some menu item")) {
                            }
                            endMenu()
                        }
                        endMenuBar()
                    }

                    text("Hello from Stacked The First\nUsing style.Colors[Col.ModalWindowDimBg] behind it.")

                    // Testing behavior of widgets stacking their own regular popups over the modal.
                    combo("Combo", ::item, listOf("aaaa", "bbbb", "cccc", "dddd", "eeee"))
                    colorEdit4("color", color)

                    button("Add another modal..") { openPopup("Stacked 2") }
                    // Also demonstrate passing a bool* to BeginPopupModal(), this will create a regular close button which
                    // will close the popup. Note that the visibility state of popups is owned by imgui, so the input value
                    // of the bool actually doesn't matter here.
                    val unusedOpen = true.mutableReference
                    if (beginPopupModal("Stacked 2", unusedOpen)) {
                        text("Hello from Stacked The Second!")
                        button("Close") { closeCurrentPopup() }
                        endPopup()
                    }
                    button("Close") { closeCurrentPopup() }
                }
            }
        }
    }
}