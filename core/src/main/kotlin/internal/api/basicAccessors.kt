package imgui.internal.api

import imgui.DataType
import imgui.ID
import imgui.ImGui.debugHookIdInfo
import imgui.ImGui.inputTextDeactivateHook
import imgui.ImGui.navClearPreferredPosForAxis
import imgui.ImGui.rectAbsToRel
import imgui.ImGui.setNavWindow
import imgui.MouseButton
import imgui.api.g
import imgui.api.gImGui
import imgui.div
import imgui.internal.classes.Window
import imgui.internal.hashData
import imgui.internal.hashStr
import imgui.internal.sections.*

internal interface basicAccessors {

    /** ~GetItemStatusFlags */
    val itemStatusFlags: ItemStatusFlags
        get() = g.lastItemData.statusFlags

    val itemFlags: ItemFlags
        get() = g.lastItemData.inFlags

    /** ~GetActiveID */
    val activeID: ID
        get() = g.activeId

    /** ~GetFocusID */
    val focusID: ID
        get() = g.navId

    fun setActiveID(id: ID, window: Window?) {

        val g = gImGui

        // Clear previous active id
        if (g.activeId != 0) {
            // While most behaved code would make an effort to not steal active id during window move/drag operations,
            // we at least need to be resilient to it. Canceling the move is rather aggressive and users of 'master' branch
            // may prefer the weird ill-defined half working situation ('docking' did assert), so may need to rework that.
            if (g.movingWindow != null && g.activeId == g.movingWindow!!.moveId) {
                IMGUI_DEBUG_LOG_ACTIVEID("SetActiveID() cancel MovingWindow\n")
                g.movingWindow = null
            }

            // This could be written in a more general way (e.g associate a hook to ActiveId),
            // but since this is currently quite an exception we'll leave it as is.
            // One common scenario leading to this is: pressing Key ->NavMoveRequestApplyResult() -> ClearActiveId()
            if (g.inputTextState.id == g.activeId)
                inputTextDeactivateHook(g.activeId)
        }

        // Set active id
        g.activeIdIsJustActivated = (g.activeId != id)
        if (g.activeIdIsJustActivated) {
            IMGUI_DEBUG_LOG_ACTIVEID("SetActiveID() old:0x%08X (window \"${g.activeIdWindow?.name ?: ""}\") -> new:0x%08X (window \"${window?.name ?: ""}\")", g.activeId, id)
            g.activeIdTimer = 0f
            g.activeIdHasBeenPressedBefore = false
            g.activeIdHasBeenEditedBefore = false
            g.activeIdMouseButton = MouseButton.None
            if (id != 0) {
                g.lastActiveId = id
                g.lastActiveIdTimer = 0f
            }
        }
        g.activeId = id
        g.activeIdAllowOverlap = false
        g.activeIdNoClearOnFocusLoss = false
        g.activeIdWindow = window
        g.activeIdHasBeenEditedThisFrame = false
        if (id != 0) {
            g.activeIdIsAlive = id
            g.activeIdSource = if (g.navActivateId == id || g.navJustMovedToId == id) g.navInputSource else InputSource.Mouse
            assert(g.activeIdSource != InputSource.None)
        }
        g.activeIdUsingNavDirMask = 0x00
        g.activeIdUsingAllKeyboardKeys = false
    }

    /** FIXME-NAV: The existence of SetNavID/SetNavIDWithRectRel/SetFocusID is incredibly messy and confusing and needs some explanation or refactoring. */
    fun setFocusID(id: ID, window: Window) {

        assert(id != 0)

        if (g.navWindow !== window)
            setNavWindow(window)

        val navLayer = window.dc.navLayerCurrent
        g.navId = id
        g.navLayer = navLayer
        g.navFocusScopeId = g.currentFocusScopeId
        window.navLastIds[navLayer] = id
        if (g.lastItemData.id == id)
            window.navRectRel[navLayer] = window rectAbsToRel g.lastItemData.navRect

        if (g.activeIdSource == InputSource.Keyboard || g.activeIdSource == InputSource.Gamepad)
            g.navDisableMouseHover = true
        else
            g.navDisableHighlight = true

        // Clear preferred scoring position (NavMoveRequestApplyResult() will tend to restore it)
        navClearPreferredPosForAxis(Axis.X)
        navClearPreferredPosForAxis(Axis.Y)
    }

    fun clearActiveID() = setActiveID(0, null) // g.ActiveId = 0;

    var hoveredId: ID
        /** ~GetHoveredID */
        get() = if (g.hoveredId != 0) g.hoveredId else g.hoveredIdPreviousFrame
        /** ~SetHoveredID */
        set(value) {
            g.hoveredId = value
            g.hoveredIdAllowOverlap = false
            if (value != 0 && g.hoveredIdPreviousFrame != value) {
                g.hoveredIdTimer = 0f; g.hoveredIdNotActiveTimer = 0f
            }
        }


    /** This is called by ItemAdd().
     *  Code not using ItemAdd() may need to call this manually otherwise ActiveId will be cleared. In IMGUI_VERSION_NUM < 18717 this was called by GetID(). */
    fun keepAliveID(id: ID) {
        if (g.activeId == id)
            g.activeIdIsAlive = id
        if (g.activeIdPreviousFrame == id)
            g.activeIdPreviousFrameIsAlive = true
    }

    /** Mark data associated to given item as "edited", used by IsItemDeactivatedAfterEdit() function. */
    fun markItemEdited(id: ID) {
        // This marking is solely to be able to provide info for IsItemDeactivatedAfterEdit().
        // ActiveId might have been released by the time we call this (as in the typical press/release button behavior) but still need to fill the data.
        val g = gImGui
        if (g.activeId == id || g.activeId == 0) {
            g.activeIdHasBeenEditedThisFrame = true
            g.activeIdHasBeenEditedBefore = true
        }

        // We accept a MarkItemEdited() on drag and drop targets (see https://github.com/ocornut/imgui/issues/1875#issuecomment-978243343)
        // We accept 'ActiveIdPreviousFrame == id' for InputText() returning an edit after it has been taken ActiveId away (#4714)
        assert(g.dragDropActive || g.activeId == id || g.activeId == 0 || g.activeIdPreviousFrame == id)

        //IM_ASSERT(g.CurrentWindow->DC.LastItemId == id);
        g.lastItemData.statusFlags /= ItemStatusFlag.Edited
    }

    fun pushOverrideID(id: ID) {
        val window = g.currentWindow!!
        if (g.debugHookIdInfo == id)
            debugHookIdInfo(id, DataType._ID, null)
        window.idStack += id
    }

    fun getIDWithSeed(str: String, strEnd: Int = -1, seed: ID): ID {
        val id = hashStr(str, if (strEnd != -1) strEnd else 0, seed)
        if (g.debugHookIdInfo == id)
            debugHookIdInfo(id, DataType._String, str, strEnd)
        return id
    }

    fun getIDWithSeed(n: Int, seed: ID): ID {
        val id = hashData(n, seed)
        if (g.debugHookIdInfo == id)
            debugHookIdInfo(id, DataType.Int, n)
        return id
    }

}