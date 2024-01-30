package imgui.api

import glm_.glm
import glm_.has
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginChildEx
import imgui.ImGui.currentWindow
import imgui.ImGui.end
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.renderNavHighlight
import imgui.internal.classes.Rect
import imgui.internal.sections.Axis
import imgui.internal.sections.ItemStatusFlag
import imgui.internal.sections.NavHighlightFlag
import imgui.internal.sections.shl
import imgui.WindowFlag as Wf


interface childWindows {

    // Child Windows

    /** - Use child windows to begin into a self-contained independent scrolling/clipping regions within a host window. Child windows can embed their own child.
     *  - For each independent axis of 'size': ==0.0f: use remaining host window size / >0.0f: fixed size
     *      / <0.0f: use remaining window size minus abs(size) / Each axis can use a different mode, e.g. ImVec2(0,400).
     *  - BeginChild() returns false to indicate the window is collapsed or fully clipped, so you may early out and omit submitting anything to the window.
     *    Always call a matching EndChild() for each BeginChild() call, regardless of its return value.
     *    [Important: due to legacy reason, this is inconsistent with most other functions such as BeginMenu/EndMenu,
     *     BeginPopup/EndPopup, etc. where the EndXXX call should only be called if the corresponding BeginXXX function
     *     returned true. Begin and BeginChild are the only odd ones out. Will be fixed in a future update.] */
    fun beginChild(strId: String, size: Vec2 = Vec2(), border: Boolean = false, flags: WindowFlags = none): Boolean =
            beginChildEx(strId, currentWindow.getID(strId), size, border, flags)

    /** begin a scrolling region.
     *  size == 0f: use remaining window size
     *  size < 0f: use remaining window size minus abs(size)
     *  size > 0f: fixed size. each axis can use a different mode, e.g. Vec2(0, 400).   */
    fun beginChild(id: ID, sizeArg: Vec2 = Vec2(), border: Boolean = false, flags: WindowFlags = none): Boolean {
        assert(id != 0)
        return beginChildEx("", id, sizeArg, border, flags)
    }

    /** Always call even if BeginChild() return false (which indicates a collapsed or clipping child window)    */
    fun endChild() {

        val window = currentWindow

        assert(!g.withinEndChild)
        assert(window.flags has Wf._ChildWindow) { "Mismatched BeginChild()/EndChild() callss" }

        g.withinEndChild = true
        if (window.beginCount > 1)
            end()
        else {
            val sz = Vec2(window.size) // Arbitrary minimum zero-ish child size of 4.0f causes less trouble than a 0.0f
            if (window.autoFitChildAxes has (1 shl Axis.X))
                sz.x = glm.max(4f, sz.x)
            if (window.autoFitChildAxes has (1 shl Axis.Y))
                sz.y = glm.max(4f, sz.y)
            end()

            val parentWindow = currentWindow
            val bb = Rect(parentWindow.dc.cursorPos, parentWindow.dc.cursorPos + sz)
            itemSize(sz)
            if ((window.dc.navLayersActiveMask != 0 || window.dc.navWindowHasScrollY) && window.flags hasnt Wf._NavFlattened) {
                itemAdd(bb, window.childId)
                renderNavHighlight(bb, window.childId)

                // When browsing a window that has no activable items (scroll only) we keep a highlight on the child (pass g.NavId to trick into always displaying)
                if (window.dc.navLayersActiveMask == 0 && window === g.navWindow)
                    renderNavHighlight(Rect(bb.min - 2, bb.max + 2), g.navId, NavHighlightFlag.TypeThin)
            } else {
                // Not navigable into
                itemAdd(bb, 0)

                // But when flattened we directly reach items, adjust active layer mask accordingly
                if (window.flags has Wf._NavFlattened)
                    parentWindow.dc.navLayersActiveMaskNext = parentWindow.dc.navLayersActiveMaskNext or window.dc.navLayersActiveMaskNext
            }
            if (g.hoveredWindow === window)
                g.lastItemData.statusFlags = g.lastItemData.statusFlags or ItemStatusFlag.HoveredWindow
        }
        g.withinEndChild = false
        g.logLinePosY = -Float.MAX_VALUE // To enforce a carriage return
    }
}