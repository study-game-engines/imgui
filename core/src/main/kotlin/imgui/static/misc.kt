package imgui.static

import glm_.glm
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.io
import imgui.ImGui.isActiveIdUsingKey
import imgui.ImGui.isMousePosValid
import imgui.ImGui.loadIniSettingsFromDisk
import imgui.ImGui.parseFormatFindEnd
import imgui.ImGui.parseFormatFindStart
import imgui.ImGui.saveIniSettingsToDisk
import imgui.api.g
import imgui.internal.*
import imgui.internal.classes.Window


// Misc

/** Called by NewFrame() */
fun updateSettings() {
    // Load settings on first frame (if not explicitly loaded manually before)
    if (!g.settingsLoaded) {
        assert(g.settingsWindows.isEmpty())
        io.iniFilename?.let(::loadIniSettingsFromDisk)
        g.settingsLoaded = true
    }

    // Save settings (with a delay after the last modification, so we don't spam disk too much)
    if (g.settingsDirtyTimer > 0f) {
        g.settingsDirtyTimer -= io.deltaTime
        if (g.settingsDirtyTimer <= 0f) {
            io.iniFilename.let {
                if (it != null)
                    saveIniSettingsToDisk(it)
                else
                    io.wantSaveIniSettings = true  // Let user know they can call SaveIniSettingsToMemory(). user will need to clear io.WantSaveIniSettings themselves.
            }
            g.settingsDirtyTimer = 0f
        }
    }
}

fun updateMouseInputs() {

    with(io) {

        // Round mouse position to avoid spreading non-rounded position (e.g. UpdateManualResize doesn't support them well)
        if (isMousePosValid(mousePos)) {
            g.mouseLastValidPos put floor(mousePos)
            mousePos = Vec2(g.mouseLastValidPos)
        }

        // If mouse just appeared or disappeared (usually denoted by -FLT_MAX component) we cancel out movement in MouseDelta
        if (isMousePosValid(mousePos) && isMousePosValid(mousePosPrev))
            mouseDelta = mousePos - mousePosPrev
        else
            mouseDelta put 0f

        // If mouse moved we re-enable mouse hovering in case it was disabled by gamepad/keyboard. In theory should use a >0.0f threshold but would need to reset in everywhere we set this to true.
        if (mouseDelta.x != 0f || mouseDelta.y != 0f)
            g.navDisableMouseHover = false

        mousePosPrev put mousePos
        for (i in mouseDown.indices) {
            mouseClicked[i] = mouseDown[i] && mouseDownDuration[i] < 0f
            mouseClickedCount[i] = 0 // Will be filled below
            mouseReleased[i] = !mouseDown[i] && mouseDownDuration[i] >= 0f
            mouseDownDurationPrev[i] = mouseDownDuration[i]
            mouseDownDuration[i] = when {
                mouseDown[i] -> when {
                    mouseDownDuration[i] < 0f -> 0f
                    else -> mouseDownDuration[i] + deltaTime
                }
                else -> -1f
            }
            if (mouseClicked[i]) {
                var isRepeatedClick = false
                if (g.time - mouseClickedTime[i] < mouseDoubleClickTime) {
                    val deltaFromClickPos = when {
                        isMousePosValid(mousePos) -> mousePos - mouseClickedPos[i]
                        else -> Vec2()
                    }
                    if (deltaFromClickPos.lengthSqr < mouseDoubleClickMaxDist * mouseDoubleClickMaxDist)
                        isRepeatedClick = true
                }
                if (isRepeatedClick)
                    mouseClickedLastCount[i]++
                else
                    mouseClickedLastCount[i] = 1
                mouseClickedTime[i] = g.time
                mouseClickedPos[i] put mousePos
                mouseClickedCount[i] = mouseClickedLastCount[i]
                mouseDragMaxDistanceAbs[i] put 0f
                mouseDragMaxDistanceSqr[i] = 0f
            } else if (mouseDown[i]) {
                // Maintain the maximum distance we reaching from the initial click position, which is used with dragging threshold
                val deltaFromClickPos = when {
                    isMousePosValid(io.mousePos) -> io.mousePos - io.mouseClickedPos[i]
                    else -> Vec2()
                }
                io.mouseDragMaxDistanceSqr[i] = io.mouseDragMaxDistanceSqr[i] max deltaFromClickPos.lengthSqr
                io.mouseDragMaxDistanceAbs[i].x = io.mouseDragMaxDistanceAbs[i].x max when {
                    deltaFromClickPos.x < 0f -> -deltaFromClickPos.x
                    else -> deltaFromClickPos.x
                }
                io.mouseDragMaxDistanceAbs[i].y = io.mouseDragMaxDistanceAbs[i].y max when {
                    deltaFromClickPos.y < 0f -> -deltaFromClickPos.y
                    else -> deltaFromClickPos.y
                }
                val mouseDelta = mousePos - mouseClickedPos[i]
                mouseDragMaxDistanceAbs[i].x = mouseDragMaxDistanceAbs[i].x max if (mouseDelta.x < 0f) -mouseDelta.x else mouseDelta.x
                mouseDragMaxDistanceAbs[i].y = mouseDragMaxDistanceAbs[i].y max if (mouseDelta.y < 0f) -mouseDelta.y else mouseDelta.y
                mouseDragMaxDistanceSqr[i] = mouseDragMaxDistanceSqr[i] max mouseDelta.lengthSqr
            }

            // We provide io.MouseDoubleClicked[] as a legacy service
            mouseDoubleClicked[i] = mouseClickedCount[i] == 2

            // Clicking any mouse button reactivate mouse hovering which may have been deactivated by gamepad/keyboard navigation
            if (mouseClicked[i])
                g.navDisableMouseHover = false
        }
    }
}

fun updateMouseWheel() {

    // Reset the locked window if we move the mouse or after the timer elapses
    if (g.wheelingWindow != null) {
        g.wheelingWindowTimer -= io.deltaTime
        if (isMousePosValid() && (io.mousePos - g.wheelingWindowRefMousePos).lengthSqr > io.mouseDragThreshold * io.mouseDragThreshold)
            g.wheelingWindowTimer = 0f
        if (g.wheelingWindowTimer <= 0f) {
            g.wheelingWindow = null
            g.wheelingWindowTimer = 0f
        }
    }

    if (io.mouseWheel == 0f && io.mouseWheelH == 0f)
        return

    if ((g.activeId != 0 && g.activeIdUsingMouseWheel) || (g.hoveredIdPreviousFrame != 0 && g.hoveredIdPreviousFrameUsingMouseWheel))
        return
    var window = g.wheelingWindow ?: g.hoveredWindow
    if (window == null || window.collapsed)
        return

    // Zoom / Scale window
    // FIXME-OBSOLETE: This is an old feature, it still works but pretty much nobody is using it and may be best redesigned.
    if (io.mouseWheel != 0f && io.keyCtrl && io.fontAllowUserScaling) {
        window.startLockWheeling()
        val newFontScale = glm.clamp(window.fontWindowScale + io.mouseWheel * 0.1f, 0.5f, 2.5f)
        val scale = newFontScale / window.fontWindowScale
        window.fontWindowScale = newFontScale
        if (window === window.rootWindow) {
            val offset = window.size * (1f - scale) * (io.mousePos - window.pos) / window.size
            window.setPos(window.pos + offset)
            window.size = floor(window.size * scale)
            window.sizeFull = floor(window.sizeFull * scale)
        }
        return
    }

    // Mouse wheel scrolling
    // If a child window has the ImGuiWindowFlags_NoScrollWithMouse flag, we give a chance to scroll its parent
    if (g.io.keyCtrl)
        return

    // As a standard behavior holding SHIFT while using Vertical Mouse Wheel triggers Horizontal scroll instead
    // (we avoid doing it on OSX as it the OS input layer handles this already)
    val swapAxis = g.io.keyShift && !g.io.configMacOSXBehaviors
    val wheelY = if (swapAxis) 0f else g.io.mouseWheel
    val wheelX = if (swapAxis) g.io.mouseWheel else g.io.mouseWheelH

    // Vertical Mouse Wheel scrolling
    if (wheelY != 0f) {
        window.startLockWheeling()
        tailrec fun Window.getParent(): Window = when {
            flags has WindowFlag._ChildWindow && (scrollMax.y == 0f || (flags has WindowFlag.NoScrollWithMouse && flags hasnt WindowFlag.NoMouseInputs)) -> parentWindow!!.getParent()
            else -> this
        }
        window = g.hoveredWindow!!.getParent()
        if (window.flags hasnt WindowFlag.NoScrollWithMouse && window.flags hasnt WindowFlag.NoMouseInputs) {
            val maxStep = window.innerRect.height * 0.67f
            val scrollStep = floor((5 * window.calcFontSize()) min maxStep)
            window.setScrollY(window.scroll.y - wheelY * scrollStep)
        }
    }

    // Horizontal Mouse Wheel scrolling, or Vertical Mouse Wheel w/ Shift held
    if (wheelX != 0f) {
        window.startLockWheeling()
        tailrec fun Window.getParent(): Window = when {
            flags has WindowFlag._ChildWindow && (scrollMax.x == 0f || (flags has WindowFlag.NoScrollWithMouse && flags hasnt WindowFlag.NoMouseInputs)) -> parentWindow!!.getParent()
            else -> this
        }
        window = g.hoveredWindow!!.getParent()
        if (window.flags hasnt WindowFlag.NoScrollWithMouse && window.flags hasnt WindowFlag.NoMouseInputs) {
            val maxStep = window.innerRect.width * 0.67f
            val scrollStep = floor((2 * window.calcFontSize()) min maxStep)
            window.setScrollX(window.scroll.x - wheelX * scrollStep)
        }
    }
}

// UpdateWindowManualResize,
// RenderWindowOuterBorders,
// RenderWindowDecorations,
// RenderWindowTitleBarContents -> window class

// truly miscellaneous

// FIXME-OPT O(N)
fun findWindowNavFocusable(iStart: Int, iStop: Int, dir: Int): Window? {
    var i = iStart
    while (i in g.windowsFocusOrder.indices && i != iStop) {
        if (g.windowsFocusOrder[i].isNavFocusable)
            return g.windowsFocusOrder[i]
        i += dir
    }
    return null
}

fun navUpdateWindowingHighlightWindow(focusChangeDir: Int) {

    val target = g.navWindowingTarget!!
    if (target.flags has WindowFlag._Modal) return

    val iCurrent = findWindowFocusIndex(target)
    val windowTarget = findWindowNavFocusable(iCurrent + focusChangeDir, -Int.MAX_VALUE, focusChangeDir)
        ?: findWindowNavFocusable(if (focusChangeDir < 0) g.windowsFocusOrder.lastIndex else 0, iCurrent, focusChangeDir)
    // Don't reset windowing target if there's a single window in the list
    windowTarget?.let {
        g.navWindowingTarget = it
        g.navWindowingTargetAnim = it
    }
    g.navWindowingToggleLayer = false
}

/** FIXME-LEGACY: Prior to 1.61 our DragInt() function internally used floats and because of this the compile-time default value
 *  for format was "%.0f".
 *  Even though we changed the compile-time default, we expect users to have carried %f around, which would break
 *  the display of DragInt() calls.
 *  To honor backward compatibility we are rewriting the format string, unless IMGUI_DISABLE_OBSOLETE_FUNCTIONS is enabled.
 *  What could possibly go wrong?! */
fun patchFormatStringFloatToInt(fmt: String): String {
    if (fmt == "%.0f") // Fast legacy path for "%.0f" which is expected to be the most common case.
        return "%d"
    val fmtStart = parseFormatFindStart(fmt)    // Find % (if any, and ignore %%)
    // Find end of format specifier, which itself is an exercise of confidence/recklessness (because snprintf is dependent on libc or user).
    val fmtEnd = parseFormatFindEnd(fmt, fmtStart)
    if (fmtEnd > fmtStart && fmt[fmtEnd - 1] == 'f') {
        if (fmtStart == 0 && fmtEnd == fmt.length)
            return "%d"
        return fmt.substring(0, fmtStart) + "%d" + fmt.substring(fmtEnd, fmt.length)
    }
    return fmt
}