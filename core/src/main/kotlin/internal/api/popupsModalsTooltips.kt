package imgui.internal.api

import glm_.glm
import glm_.i
import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.clearActiveID
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.endPopup
import imgui.ImGui.findWindowByName
import imgui.ImGui.focusTopMostWindowUnderOne
import imgui.ImGui.focusWindow
import imgui.ImGui.io
import imgui.ImGui.isMousePosValid
import imgui.ImGui.isWithinBeginStackOf
import imgui.ImGui.navInitWindow
import imgui.ImGui.setActiveID
import imgui.ImGui.setHiddendAndSkipItemsForCurrentFrame
import imgui.ImGui.setNextWindowBgAlpha
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.style
import imgui.WindowFlag
import imgui.api.g
import imgui.internal.classes.FocusRequestFlag
import imgui.internal.classes.PopupData
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.floor
import imgui.internal.hashStr
import imgui.internal.sections.*
import imgui.statics.navCalcPreferredRefPos
import uno.kotlin.getValue
import uno.kotlin.setValue
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf

/** Popups, Modals, Tooltips */
internal interface popupsModalsTooltips {

    fun beginChildEx(name: String, id: ID, sizeArg: Vec2, border: Boolean, flags_: WindowFlags): Boolean {

        val parentWindow = g.currentWindow!!
        var flags = flags_ or Wf.NoTitleBar or Wf.NoResize or Wf.NoSavedSettings or Wf._ChildWindow
        flags /= parentWindow.flags and Wf.NoMove  // Inherit the NoMove flag

        // Size
        val contentAvail = contentRegionAvail
        val size = floor(sizeArg)
        val autoFitAxes = (if (size.x == 0f) 1 shl Axis.X else 0x00) or (if (size.y == 0f) 1 shl Axis.Y else 0x00)
        if (size.x <= 0f)   // Arbitrary minimum child size (0.0f causing too many issues)
            size.x = glm.max(contentAvail.x + size.x, 4f)
        if (size.y <= 0f)
            size.y = glm.max(contentAvail.y + size.y, 4f)
        setNextWindowSize(size)

        // Build up name. If you need to append to a same child from multiple location in the ID stack, use BeginChild(ImGuiID id) with a stable value.
        val maybePostfix = if (name.isEmpty()) "" else "_"
        val tempWindowName = "${parentWindow.name}/$name$maybePostfix%08X".format(style.locale, id)

        val backupBorderSize = style.childBorderSize
        if (!border)
            style.childBorderSize = 0f
        val ret = begin(tempWindowName, null, flags)
        style.childBorderSize = backupBorderSize

        val childWindow = g.currentWindow!!.apply {
            childId = id
            autoFitChildAxes = autoFitAxes
        }

        // Set the cursor to handle case where the user called SetNextWindowPos()+BeginChild() manually.
        // While this is not really documented/defined, it seems that the expected thing to do.
        if (childWindow.beginCount == 1) parentWindow.dc.cursorPos put childWindow.pos

        // Process navigation-in immediately so NavInit can run on first frame
        // Can enter a child if (A) it has navigatable items or (B) it can be scrolled.
        val tempIdForActivation: ID = hashStr("##Child", seed_ = id)
        if (g.activeId == tempIdForActivation)
            clearActiveID()
        if (g.navActivateId == id && flags hasnt Wf._NavFlattened && (childWindow.dc.navLayersActiveMask != 0 || childWindow.dc.navWindowHasScrollY)) {
            focusWindow(childWindow)
            navInitWindow(childWindow, false)
            setActiveID(tempIdForActivation, childWindow) // Steal ActiveId with another arbitrary id so that key-press won't activate child item
            g.activeIdSource = g.navInputSource
        }

        return ret
    }

    /** Mark popup as open (toggle toward open state).
     *  Popups are closed when user click outside, or activate a pressable item, or CloseCurrentPopup() is called within
     *  a BeginPopup()/EndPopup() block.
     *  Popup identifiers are relative to the current ID-stack (so OpenPopup and BeginPopup needs to be at the same
     *  level).
     *  One open popup per level of the popup hierarchy (NB: when assigning we reset the Window member of ImGuiPopupRef
     *  to NULL)    */
    fun openPopupEx(id: ID, popupFlags: PopupFlags = none) {

        val parentWindow = g.currentWindow!!
        val currentStackSize = g.beginPopupStack.size

        if (popupFlags has PopupFlag.NoOpenOverExistingPopup)
            if (isPopupOpen(0, PopupFlag.AnyPopupId))
                return

        // Tagged as new ref as Window will be set back to NULL if we write this into OpenPopupStack.
        val openPopupPos = navCalcPreferredRefPos()
        val popupRef = PopupData(popupId = id, window = null, backupNavWindow = g.navWindow, // When popup closes focus may be restored to NavWindow (depend on window type).
                openFrameCount = g.frameCount, openParentId = parentWindow.idStack.last(), openPopupPos = openPopupPos,
                openMousePos = if (isMousePosValid(io.mousePos)) Vec2(io.mousePos) else Vec2(openPopupPos))

        IMGUI_DEBUG_LOG_POPUP("[popup] OpenPopupEx(0x%08X)", id)
        if (g.openPopupStack.size < currentStackSize + 1) g.openPopupStack += popupRef
        else {/*  Gently handle the user mistakenly calling OpenPopup() every frame. It is a programming mistake!
                However, if we were to run the regular code path, the ui would become completely unusable because
                the popup will always be in hidden-while-calculating-size state _while_ claiming focus.
                Which would be a very confusing situation for the programmer. Instead, we silently allow the popup
                to proceed, it will keep reappearing and the programming error will be more obvious to understand.  */
            if (g.openPopupStack[currentStackSize].popupId == id && g.openPopupStack[currentStackSize].openFrameCount == g.frameCount - 1) g.openPopupStack[currentStackSize].openFrameCount =
                    popupRef.openFrameCount
            else { // Close child popups if any, then flag popup for open/reopen
                closePopupToLevel(currentStackSize, false)
                g.openPopupStack += popupRef
            }/*  When reopening a popup we first refocus its parent, otherwise if its parent is itself a popup
                it would get closed by closePopupsOverWindow().  This is equivalent to what ClosePopupToLevel() does. */ //if (g.openPopupStack[currentStackSize].popupId == id) sourceWindow.focus()
        }
    }

    fun closePopupToLevel(remaining: Int, restoreFocusToWindowUnderPopup: Boolean) {

        IMGUI_DEBUG_LOG_POPUP("[popup] ClosePopupToLevel($remaining), restore_focus_to_window_under_popup=${restoreFocusToWindowUnderPopup.i}")
        assert(remaining in g.openPopupStack.indices)

        // Trim open popup stack
        val popupWindow = g.openPopupStack[remaining].window
        val popupBackupNavWindow = g.openPopupStack[remaining].backupNavWindow
        for (i in remaining until g.openPopupStack.size) // resize(remaining)
            g.openPopupStack.pop()

        if (restoreFocusToWindowUnderPopup) {
            val focusWindow = if (popupWindow != null && popupWindow.flags has Wf._ChildMenu) popupWindow.parentWindow else popupBackupNavWindow
            if (focusWindow?.wasActive == false && popupWindow != null)
                focusTopMostWindowUnderOne(popupWindow, flags = FocusRequestFlag.RestoreFocusedChild) // Fallback, [JVM] default args
            else
                focusWindow(focusWindow, if (g.navLayer == NavLayer.Main) FocusRequestFlag.RestoreFocusedChild else none)
        }
    }

    /** When popups are stacked, clicking on a lower level popups puts focus back to it and close popups above it.
     *  This function closes any popups that are over 'ref_window'. */
    fun closePopupsOverWindow(refWindow: Window?, restoreFocusToWindowUnderPopup: Boolean) {

        if (g.openPopupStack.empty()) return

        // Don't close our own child popup windows.
        var popupCountToKeep = 0
        if (refWindow != null) // Find the highest popup which is a descendant of the reference window (generally reference window = NavWindow)
            while (popupCountToKeep < g.openPopupStack.size) {
                val popup = g.openPopupStack[popupCountToKeep]
                if (popup.window == null) {
                    popupCountToKeep++
                    continue
                }
                assert(popup.window!!.flags has Wf._Popup)
                if (popup.window!!.flags has Wf._ChildWindow) {
                    popupCountToKeep++
                    continue
                }

                // Trim the stack unless the popup is a direct parent of the reference window (the reference window is often the NavWindow)
                // - With this stack of window, clicking/focusing Popup1 will close Popup2 and Popup3:
                //     Window -> Popup1 -> Popup2 -> Popup3
                // - Each popups may contain child windows, which is why we compare ->RootWindow!
                //     Window -> Popup1 -> Popup1_Child -> Popup2 -> Popup2_Child
                var refWindowIsDescendentOfPopup = false
                for (n in popupCountToKeep until g.openPopupStack.size) {
                    val popupWindow = g.openPopupStack[n].window
                    if (refWindow isWithinBeginStackOf popupWindow!!) {
                        refWindowIsDescendentOfPopup = true
                        break
                    }
                }
                if (!refWindowIsDescendentOfPopup)
                    break
                popupCountToKeep++
            }

        if (popupCountToKeep < g.openPopupStack.size) { // This test is not required but it allows to set a convenient breakpoint on the statement below
            IMGUI_DEBUG_LOG_POPUP("[popup] ClosePopupsOverWindow(\"${refWindow?.name ?: "<NULL>"}\")")
            closePopupToLevel(popupCountToKeep, restoreFocusToWindowUnderPopup)
        }
    }

    fun closePopupsExceptModals() {
        var popupCountToKeep = g.openPopupStack.size
        while (popupCountToKeep > 0) {
            val window = g.openPopupStack[popupCountToKeep - 1].window
            if (window == null || window.flags has Wf._Modal)
                break
            popupCountToKeep--
        }
        if (popupCountToKeep < g.openPopupStack.size) // This test is not required but it allows to set a convenient breakpoint on the statement below
            closePopupToLevel(popupCountToKeep, true)
    }

    /** Supported flags: ImGuiPopupFlags_AnyPopupId, ImGuiPopupFlags_AnyPopupLevel
     *
     *  Test for id at the current BeginPopup() level of the popup stack (this doesn't scan the whole popup stack!) */
    fun isPopupOpen(id: ID, popupFlags: PopupFlags = none): Boolean = when {
        popupFlags has PopupFlag.AnyPopupId -> { // Return true if any popup is open at the current BeginPopup() level of the popup stack
            // This may be used to e.g. test for another popups already opened to handle popups priorities at the same level.
            assert(id == 0)
            when {
                popupFlags has PopupFlag.AnyPopupLevel -> g.openPopupStack.isNotEmpty()
                else -> g.openPopupStack.size > g.beginPopupStack.size
            }
        } // Return true if the popup is open anywhere in the popup stack
        popupFlags has PopupFlag.AnyPopupLevel -> g.openPopupStack.any { it.popupId == id } // Return true if the popup is open at the current BeginPopup() level of the popup stack (this is the most-common query)
        else -> g.openPopupStack.size > g.beginPopupStack.size && g.openPopupStack[g.beginPopupStack.size].popupId == id
    }

    /** Attention! BeginPopup() adds default flags which BeginPopupEx()! */
    fun beginPopupEx(id: ID, flags_: WindowFlags): Boolean {

        if (!isPopupOpen(id)) {
            g.nextWindowData.clearFlags() // We behave like Begin() and need to consume those values
            return false
        }

        val name = when {
            flags_ has Wf._ChildMenu -> "##Menu_%02d".format(style.locale, g.beginMenuCount)    // Recycle windows based on depth
            else -> "##Popup_%08x".format(style.locale, id)     // Not recycling, so we can close/open during the same frame
        }
        val flags = flags_ or Wf._Popup
        val isOpen = begin(name, null, flags)
        if (!isOpen) // NB: Begin can return false when the popup is completely clipped (e.g. zero size display)
            endPopup()

        return isOpen
    }

    /** Not exposed publicly as BeginTooltip() because bool parameters are evil. Let's see if other needs arise first.
     *  @param extraWindowFlags WindowFlag   */
    fun beginTooltipEx(tooltipFlags_: TooltipFlags = none, extraWindowFlags: WindowFlags = none): Boolean {
        var tooltipFlags = tooltipFlags_
        if (g.dragDropWithinSource || g.dragDropWithinTarget) {
            // Drag and Drop tooltips are positioning differently than other tooltips:
            // - offset visibility to increase visibility around mouse.
            // - never clamp within outer viewport boundary.
            // We call SetNextWindowPos() to enforce position and disable clamping.
            // See FindBestWindowPosForPopup() for positionning logic of other tooltips (not drag and drop ones).
            //ImVec2 tooltip_pos = g.IO.MousePos - g.ActiveIdClickOffset - g.Style.WindowPadding;
            val tooltipPos = io.mousePos + TOOLTIP_DEFAULT_OFFSET * style.mouseCursorScale
            setNextWindowPos(tooltipPos)
            setNextWindowBgAlpha(
                    style.colors[Col.PopupBg].w * 0.6f
            ) //PushStyleVar(ImGuiStyleVar_Alpha, g.Style.Alpha * 0.60f); // This would be nice but e.g ColorButton with checkboard has issue with transparent colors :(
            tooltipFlags = tooltipFlags or TooltipFlag.OverridePrevious
        }

        var windowName = "##Tooltip_%02d".format(g.tooltipOverrideCount)
        if (tooltipFlags has TooltipFlag.OverridePrevious) findWindowByName(windowName)?.let { window ->
            if (window.active) {
                // Hide previous tooltip from being displayed. We can't easily "reset" the content of a window so we create a new one.
                window.setHiddendAndSkipItemsForCurrentFrame()
                windowName = "##Tooltip_%02d".format(++g.tooltipOverrideCount)
            }
        }
        val flags =
                Wf._Tooltip or Wf.NoMouseInputs or Wf.NoTitleBar or Wf.NoMove or Wf.NoResize or Wf.NoSavedSettings or Wf.AlwaysAutoResize
        begin(windowName, null, flags or extraWindowFlags)
        // 2023-03-09: Added bool return value to the API, but currently always returning true.
        // If this ever returns false we need to update BeginDragDropSource() accordingly.
        //if (!ret)
        //    End();
        //return ret;
        return true
    }

    /** ~GetPopupAllowedExtentRect
     *  Note that this is used for popups, which can overlap the non work-area of individual viewports. */
    val Window.popupAllowedExtentRect: Rect
        get() {
            val rScreen = (ImGui.mainViewport as ViewportP).mainRect
            val padding = g.style.displaySafeAreaPadding
            rScreen expand Vec2(if (rScreen.width > padding.x * 2) -padding.x else 0f, if (rScreen.height > padding.y * 2) -padding.y else 0f)
            return rScreen
        }

    /** ~GetTopMostPopupModal */
    // Also see FindBlockingModal(NULL)
    val topMostPopupModal: Window?
        get() {
            for (n in g.openPopupStack.size - 1 downTo 0)
                g.openPopupStack[n].window?.let {
                    if (it.flags has Wf._Modal)
                        return it
                }
            return null
        }

    /** ~GetTopMostAndVisiblePopupModal */
    // See Demo->Stacked Modal to confirm what this is for.
    val topMostAndVisiblePopupModal: Window?
        get() {
            for (n in g.openPopupStack.lastIndex downTo 0)
                g.openPopupStack[n].window?.let { popup ->
                    if (popup.flags has Wf._Modal && popup.isActiveAndVisible)
                        return popup
                }
            return null
        }

    // When a modal popup is open, newly created windows that want focus (i.e. are not popups and do not specify ImGuiWindowFlags_NoFocusOnAppearing)
    // should be positioned behind that modal window, unless the window was created inside the modal begin-stack.
    // In case of multiple stacked modals newly created window honors begin stack order and does not go below its own modal parent.
    // - WindowA            // FindBlockingModal() returns Modal1
    //   - WindowB          //                  .. returns Modal1
    //   - Modal1           //                  .. returns Modal2
    //      - WindowC       //                  .. returns Modal2
    //          - WindowD   //                  .. returns Modal2
    //          - Modal2    //                  .. returns Modal2
    //            - WindowE //                  .. returns NULL
    // Notes:
    // - FindBlockingModal(NULL) == NULL is generally equivalent to GetTopMostPopupModal() == NULL.
    //   Only difference is here we check for ->Active/WasActive but it may be unecessary.
    fun findBlockingModal(window: Window?): Window? {

        if (g.openPopupStack.isEmpty())
            return null

        // Find a modal that has common parent with specified window. Specified window should be positioned behind that modal.
        for (i in g.openPopupStack.indices) {
            val popupWindow = g.openPopupStack[i].window
            if (popupWindow == null || popupWindow.flags hasnt WindowFlag._Modal)
                continue
            if (!popupWindow.active && !popupWindow.wasActive) // Check WasActive, because this code may run before popup renders on current frame, also check Active to handle newly created windows.
                continue
            if (window == null)                                         // FindBlockingModal(NULL) test for if FocusWindow(NULL) is naturally possible via a mouse click.
                return popupWindow
            if (window isWithinBeginStackOf popupWindow)       // Window may be over modal
                continue
            return popupWindow                                        // Place window right below first block modal
        }
        return null
    }


    fun findBestWindowPosForPopup(window: Window): Vec2 {

        val rOuter = window.popupAllowedExtentRect
        if (window.flags has Wf._ChildMenu) {/*  Child menus typically request _any_ position within the parent menu item,
                and then we move the new menu outside the parent bounds.
                This is how we end up with child menus appearing (most-commonly) on the right of the parent menu. */
            assert(g.currentWindow === window)
            val parentWindow = g.currentWindowStack[g.currentWindowStack.size - 2].window
            val horizontalOverlap = style.itemInnerSpacing.x
            val rAvoid = parentWindow.run {
                when {
                    dc.menuBarAppending -> Rect(-Float.MAX_VALUE, clipRect.min.y, Float.MAX_VALUE, clipRect.max.y) // Avoid parent menu-bar. If we wanted multi-line menu-bar, we may instead want to have the calling window setup e.g. a NextWindowData.PosConstraintAvoidRect field
                    else -> Rect(pos.x + horizontalOverlap, -Float.MAX_VALUE,
                            pos.x + size.x - horizontalOverlap - scrollbarSizes.x, Float.MAX_VALUE)
                }
            }
            return findBestWindowPosForPopupEx(window.pos, window.size, window::autoPosLastDirection, rOuter, rAvoid, PopupPositionPolicy.Default)
        }
        if (window.flags has Wf._Popup)
            return findBestWindowPosForPopupEx(window.pos, window.size, window::autoPosLastDirection, rOuter, Rect(window.pos, window.pos), PopupPositionPolicy.Default) // Ideally we'd disable r_avoid here
        if (window.flags has Wf._Tooltip) {
            // Position tooltip (always follows mouse + clamp within outer boundaries)
            // Note that drag and drop tooltips are NOT using this path: BeginTooltipEx() manually sets their position.
            // In theory we could handle both cases in same location, but requires a bit of shuffling as drag and drop tooltips are calling SetWindowPos() leading to 'window_pos_set_by_api' being set in Begin()
            assert(g.currentWindow === window)
            val scale = g.style.mouseCursorScale
            val refPos = navCalcPreferredRefPos()
            val tooltipPos = refPos + TOOLTIP_DEFAULT_OFFSET * scale
            val rAvoid = when {
                !g.navDisableHighlight && g.navDisableMouseHover && !(io.configFlags has ConfigFlag.NavEnableSetMousePos) -> Rect(refPos.x - 16, refPos.y - 8, refPos.x + 16, refPos.y + 8)
                else -> Rect(refPos.x - 16, refPos.y - 8, refPos.x + 24 * scale, refPos.y + 24 * scale) // FIXME: Hard-coded based on mouse cursor shape expectation. Exact dimension not very important.
            }
            //GetForegroundDrawList()->AddRect(r_avoid.Min, r_avoid.Max, IM_COL32(255, 0, 255, 255));
            return findBestWindowPosForPopupEx(tooltipPos, window.size, window::autoPosLastDirection, rOuter, rAvoid, PopupPositionPolicy.Default)
        }
        assert(false)
        return Vec2(window.pos)
    }

    /** rAvoid = the rectangle to avoid (e.g. for tooltip it is a rectangle around the mouse cursor which we want to avoid. for popups it's a small point around the cursor.)
     *  rOuter = the visible area rectangle, minus safe area padding. If our popup size won't fit because of safe area padding we ignore it.
     *  (r_outer is usually equivalent to the viewport rectangle minus padding, but when multi-viewports are enabled and monitor
     *  information are available, it may represent the entire platform monitor from the frame of reference of the current viewport.
     *  this allows us to have tooltips/popups displayed out of the parent viewport.)*/
    fun findBestWindowPosForPopupEx(refPos: Vec2, size: Vec2, lastDirPtr: KMutableProperty0<Dir>, rOuter: Rect,
                                    rAvoid: Rect, policy: PopupPositionPolicy): Vec2 {

        var lastDir by lastDirPtr
        val basePosClamped = glm.clamp(refPos, rOuter.min,
                rOuter.max - size) //GImGui->OverlayDrawList.AddRect(r_avoid.Min, r_avoid.Max, IM_COL32(255,0,0,255)); //GImGui->OverlayDrawList.AddRect(rOuter.Min, rOuter.Max, IM_COL32(0,255,0,255));

        // Combo Box policy (we want a connecting edge)
        if (policy == PopupPositionPolicy.ComboBox) {
            val dirPreferedOrder = arrayOf(Dir.Down, Dir.Right, Dir.Left, Dir.Up)
            for (n in (if (lastDir != Dir.None) -1 else 0) until Dir.COUNT) {
                val dir = if (n == -1) lastDir else dirPreferedOrder[n]
                if (n != -1 && dir == lastDir) continue // Already tried this direction?
                val pos = Vec2()
                if (dir == Dir.Down) pos.put(rAvoid.min.x, rAvoid.max.y)          // Below, Toward Right (default)
                if (dir == Dir.Right) pos.put(rAvoid.min.x, rAvoid.min.y - size.y) // Above, Toward Right
                if (dir == Dir.Left) pos.put(rAvoid.max.x - size.x, rAvoid.max.y) // Below, Toward Left
                if (dir == Dir.Up) pos.put(rAvoid.max.x - size.x, rAvoid.min.y - size.y) // Above, Toward Left
                if (Rect(pos, pos + size) !in rOuter) continue
                lastDir = dir
                return pos
            }
        }

        // Tooltip and Default popup policy
        // (Always first try the direction we used on the last frame, if any)
        if (policy == PopupPositionPolicy.Tooltip || policy == PopupPositionPolicy.Default) {
            val dirPreferredOrder = arrayOf(Dir.Right, Dir.Down, Dir.Up, Dir.Left)
            for (n in (if (lastDir != Dir.None) -1 else 0) until Dir.COUNT) {
                val dir = if (n == -1) lastDir else dirPreferredOrder[n]
                if (n != -1 && dir == lastDir) // Already tried this direction?
                    continue

                val availW =
                        (if (dir == Dir.Left) rAvoid.min.x else rOuter.max.x) - if (dir == Dir.Right) rAvoid.max.x else rOuter.min.x
                val availH =
                        (if (dir == Dir.Up) rAvoid.min.y else rOuter.max.y) - if (dir == Dir.Down) rAvoid.max.y else rOuter.min.y

                // If there's not enough room on one axis, there's no point in positioning on a side on this axis (e.g. when not enough width, use a top/bottom position to maximize available width)
                if (availW < size.x && (dir == Dir.Left || dir == Dir.Right)) continue
                if (availH < size.y && (dir == Dir.Up || dir == Dir.Down)) continue

                val pos = Vec2(
                        if (dir == Dir.Left) rAvoid.min.x - size.x else if (dir == Dir.Right) rAvoid.max.x else basePosClamped.x,
                        if (dir == Dir.Up) rAvoid.min.y - size.y else if (dir == Dir.Down) rAvoid.max.y else basePosClamped.y)

                // Clamp top-left corner of popup
                pos.x = pos.x max rOuter.min.x
                pos.y = pos.y max rOuter.min.y

                lastDir = dir
                return pos
            }
        } // Fallback when not enough room:
        lastDir = Dir.None

        return when (policy) { // For tooltip we prefer avoiding the cursor at all cost even if it means that part of the tooltip won't be visible.
            PopupPositionPolicy.Tooltip -> refPos + 2 // Otherwise try to keep within display
            else -> Vec2(refPos).apply {
                x = max(min(x + size.x, rOuter.max.x) - size.x, rOuter.min.x)
                y = max(min(y + size.y, rOuter.max.y) - size.y, rOuter.min.y)
            }
        }
    }
}