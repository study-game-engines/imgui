package imgui.api

import imgui.*
import imgui.ImGui.beginTooltip
import imgui.ImGui.clearDragDrop
import imgui.ImGui.endTooltip
import imgui.ImGui.focusWindow
import imgui.ImGui.io
import imgui.ImGui.isDown
import imgui.ImGui.isDragging
import imgui.ImGui.itemHoverable
import imgui.ImGui.keepAliveID
import imgui.ImGui.setActiveID
import imgui.ImGui.setActiveIdUsingAllKeyboardKeys
import imgui.classes.Payload
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.hashStr
import imgui.internal.sections.ItemStatusFlag
import imgui.DragDropFlag as Ddf

// Drag and Drop
// - On source items, call BeginDragDropSource(), if it returns true also call SetDragDropPayload() + EndDragDropSource().
// - On target candidates, call BeginDragDropTarget(), if it returns true also call AcceptDragDropPayload() + EndDragDropTarget().
// - If you stop calling BeginDragDropSource() the payload is preserved however it won't have a preview tooltip (we currently display a fallback "..." tooltip, see #1725)
// - An item can be both drag source and drop target.
interface dragAndDrop {
    /** call after submitting an item which may be dragged. when this return true, you can call SetDragDropPayload() + EndDragDropSource()
     *
     *  When this returns true you need to: a) call SetDragDropPayload() exactly once, b) you may render the payload visual/description, c) call EndDragDropSource()
     *  If the item has an identifier:
     *  - This assume/require the item to be activated (typically via ButtonBehavior).
     *  - Therefore if you want to use this with a mouse button other than left mouse button, it is up to the item itself to activate with another button.
     *  - We then pull and use the mouse button that was used to activate the item and use it to carry on the drag.
     *  If the item has no identifier:
     *  - Currently always assume left mouse button. */
    fun beginDragDropSource(flags: DragDropFlags = none): Boolean {

        var window: Window? = g.currentWindow!!

        // FIXME-DRAGDROP: While in the common-most "drag from non-zero active id" case we can tell the mouse button,
        // in both SourceExtern and id==0 cases we may requires something else (explicit flags or some heuristic).
        var mouseButton = MouseButton.Left

        val sourceDragActive: Boolean
        var sourceId: ID
        var sourceParentId: ID = 0
        if (flags hasnt Ddf.SourceExtern) {
            sourceId = g.lastItemData.id
            if (sourceId != 0) {
                // Common path: items with ID
                if (g.activeId != sourceId)
                    return false
                if (g.activeIdMouseButton != MouseButton.None)
                    mouseButton = g.activeIdMouseButton
                if (!g.io.mouseDown[mouseButton.i] || window!!.skipItems)
                    return false
                g.activeIdAllowOverlap = false
            } else {
                // Uncommon path: items without ID
                if (!g.io.mouseDown[mouseButton.i] || window!!.skipItems)
                    return false
                if (g.lastItemData.statusFlags hasnt ItemStatusFlag.HoveredRect && (g.activeId == 0 || g.activeIdWindow !== window))
                    return false

                // If you want to use BeginDragDropSource() on an item with no unique identifier for interaction, such as Text() or Image(), you need to:
                // A) Read the explanation below, B) Use the ImGuiDragDropFlags_SourceAllowNullID flag
                if (flags hasnt Ddf.SourceAllowNullID)
                    throw Error()

                // Magic fallback to handle items with no assigned ID, e.g. Text(), Image()
                // We build a throwaway ID based on current ID stack + relative AABB of items in window.
                // THE IDENTIFIER WON'T SURVIVE ANY REPOSITIONING/RESIZING OF THE WIDGET, so if your widget moves your dragging operation will be canceled.
                // We don't need to maintain/call ClearActiveID() as releasing the button will early out this function and trigger !ActiveIdIsAlive.
                // Rely on keeping other window->LastItemXXX fields intact.
                sourceId = window.getIDFromRectangle(g.lastItemData.rect); g.lastItemData.id = sourceId
                keepAliveID(sourceId)
                val isHovered = itemHoverable(g.lastItemData.rect, sourceId, g.lastItemData.inFlags)
                if (isHovered && io.mouseClicked[mouseButton.i]) {
                    setActiveID(sourceId, window)
                    focusWindow(window)
                }
                if (g.activeId == sourceId) // Allow the underlying widget to display/return hovered during the mouse release frame, else we would get a flicker.
                    g.activeIdAllowOverlap = isHovered
            }
            if (g.activeId != sourceId)
                return false
            sourceParentId = window.idStack.last()
            sourceDragActive = mouseButton.isDragging()

            // Disable navigation and key inputs while dragging + cancel existing request if any
            setActiveIdUsingAllKeyboardKeys()
        } else {
            window = null
            sourceId = hashStr("#SourceExtern")
            sourceDragActive = true
        }

        if (sourceDragActive) {
            if (!g.dragDropActive) {
                assert(sourceId != 0)
                clearDragDrop()
                val payload = g.dragDropPayload
                payload.sourceId = sourceId
                payload.sourceParentId = sourceParentId
                g.dragDropActive = true
                g.dragDropSourceFlags = flags
                g.dragDropMouseButton = mouseButton
                if (payload.sourceId == g.activeId)
                    g.activeIdNoClearOnFocusLoss = true
            }

            g.dragDropSourceFrameCount = g.frameCount
            g.dragDropWithinSource = true

            if (flags hasnt Ddf.SourceNoPreviewTooltip) {
                // Target can request the Source to not display its tooltip (we use a dedicated flag to make this request explicit)
                // We unfortunately can't just modify the source flags and skip the call to BeginTooltip, as caller may be emitting contents.
                beginTooltip()
                if (g.dragDropAcceptIdPrev != 0 && g.dragDropAcceptFlags has Ddf.AcceptNoPreviewTooltip)
                    g.currentWindow!!.apply {
                        // tooltipWindow
                        hidden = true; skipItems = true
                        hiddenFramesCanSkipItems = 1
                    }
            }

            if (flags hasnt Ddf.SourceNoDisableHover && flags hasnt Ddf.SourceExtern)
                g.lastItemData.statusFlags -= ItemStatusFlag.HoveredRect

            return true
        }
        return false
    }

    @Deprecated("Replaced by setDragDropPayload without size argument", ReplaceWith("setDragDropPayload(type, data, cond_)"))
    fun setDragDropPayload(type: String, data: Any, size: Int, cond_: Cond = Cond.None): Boolean = setDragDropPayload(type, data, cond_)

    //    /** Type is a user defined string of maximum 32 characters. Strings starting with '_' are reserved for dear imgui internal types.
    //     *  Data is copied and held by imgui.
    /** Type is a user defined string. Types starting with '_' are reserved for dear imgui internal types.
     *  Data is held by imgui. Return true when payload has been accepted.
     *  Use 'cond' to choose to submit payload on drag start or every frame */
    fun setDragDropPayload(type: String, data: Any?, cond_: Cond = Cond.None): Boolean {
        val payload = g.dragDropPayload
        val cond = if (cond_ == Cond.None) Cond.Always else cond_

        assert(type.isNotEmpty())
        //        assert(type.length < 32) { "Payload type can be at most 32 characters long" }
        //        assert((data != NULL && data_size > 0) || (data == NULL && data_size == 0))
        assert(cond == Cond.Always || cond == Cond.Once)
        assert(payload.sourceId != 0) { "Not called between beginDragDropSource() and endDragDropSource()" }

        if (cond == Cond.Always || payload.dataFrameCount == -1) {
            // Copy payload
            payload.dataType = type
            payload.data = data
        }
        payload.dataFrameCount = g.frameCount

        // Return whether the payload has been accepted
        return g.dragDropAcceptFrameCount == g.frameCount || g.dragDropAcceptFrameCount == g.frameCount - 1
    }

    /** Only call EndDragDropSource() if BeginDragDropSource() returns true!    */
    fun endDragDropSource() {
        assert(g.dragDropActive)
        assert(g.dragDropWithinSource) { "Not after a BeginDragDropSource()?" }

        if (g.dragDropSourceFlags hasnt Ddf.SourceNoPreviewTooltip)
            endTooltip()

        // Discard the drag if have not called setDragDropPayload()
        if (g.dragDropPayload.dataFrameCount == -1)
            clearDragDrop()
        g.dragDropWithinSource = false
    }

    /** Call after submitting an item that may receive an item.
     *  If this returns true, you can call acceptDragDropPayload() + endDragDropTarget()
     *
     *  We don't use beginDragDropTargetCustom() and duplicate its code because:
     *  1) we use lastItemRectHoveredRect which handles items that push a temporarily clip rectangle in their code.
     *      Calling beginDragDropTargetCustom(LastItemRect) would not handle them.
     *  2) and it's faster. as this code may be very frequently called, we want to early out as fast as we can.
     *  Also note how the HoveredWindow test is positioned differently in both functions (in both functions we optimize
     *  for the cheapest early out case)    */
    fun beginDragDropTarget(): Boolean {
        if (!g.dragDropActive) return false

        val window = g.currentWindow!!
        if (g.lastItemData.statusFlags hasnt ItemStatusFlag.HoveredRect)
            return false
        val hoveredWindow = g.hoveredWindowUnderMovingWindow
        if (hoveredWindow == null || window.rootWindow !== hoveredWindow.rootWindow || window.skipItems)
            return false

        val displayRect = when {
            g.lastItemData.statusFlags has ItemStatusFlag.HasDisplayRect -> g.lastItemData.displayRect
            else -> g.lastItemData.rect
        }
        var id = g.lastItemData.id
        if (id == 0) {
            id = window.getIDFromRectangle(displayRect) // [JVM] safe to pass the reference
            keepAliveID(id)
        }
        if (g.dragDropPayload.sourceId == id) return false

        assert(!g.dragDropWithinTarget)
        g.dragDropTargetRect put displayRect
        g.dragDropTargetId = id
        g.dragDropWithinTarget = true
        return true
    }

    /** Accept contents of a given type. If DragDropFlag.AcceptBeforeDelivery is set you can peek into the payload
     *  before the mouse button is released. */
    fun acceptDragDropPayload(type: String, flags_: DragDropFlags = none): Payload? {
        var flags = flags_
        val window = g.currentWindow!!
        val payload = g.dragDropPayload
        assert(g.dragDropActive) { "Not called between BeginDragDropTarget() and EndDragDropTarget() ?" }
        assert(payload.dataFrameCount != -1) { "Forgot to call EndDragDropTarget() ?" }
        if (type.isNotEmpty() && !payload.isDataType(type)) return null

        // Accept smallest drag target bounding box, this allows us to nest drag targets conveniently without ordering constraints.
        // NB: We currently accept NULL id as target. However, overlapping targets requires a unique ID to function!
        val wasAcceptedPreviously = g.dragDropAcceptIdPrev == g.dragDropTargetId
        val r = Rect(g.dragDropTargetRect)
        val rSurface = r.width * r.height
        if (rSurface > g.dragDropAcceptIdCurrRectSurface)
            return null

        g.dragDropAcceptFlags = flags
        g.dragDropAcceptIdCurr = g.dragDropTargetId
        g.dragDropAcceptIdCurrRectSurface = rSurface
        //IMGUI_DEBUG_LOG("AcceptDragDropPayload(): %08X: accept\n", g.DragDropTargetId);

        // Render default drop visuals
        payload.preview = wasAcceptedPreviously
        flags /= g.dragDropSourceFlags and Ddf.AcceptNoDrawDefaultRect // Source can also inhibit the preview (useful for external sources that live for 1 frame)
        if (flags hasnt Ddf.AcceptNoDrawDefaultRect && payload.preview)
            window.drawList.addRect(r.min - 3.5f, r.max + 3.5f, Col.DragDropTarget.u32, thickness = 2f)

        g.dragDropAcceptFrameCount = g.frameCount
        // For extern drag sources affecting OS window focus, it's easier to just test !isMouseDown() instead of isMouseReleased()
        payload.delivery = wasAcceptedPreviously && !g.dragDropMouseButton.isDown
        if (!payload.delivery && flags hasnt Ddf.AcceptBeforeDelivery)
            return null

        //IMGUI_DEBUG_LOG("AcceptDragDropPayload(): %08X: return payload\n", g.DragDropTargetId);
        return payload
    }

    /** Only call EndDragDropTarget() if BeginDragDropTarget() returns true!    */
    fun endDragDropTarget() {
        assert(g.dragDropActive)
        assert(g.dragDropWithinTarget)
        g.dragDropWithinTarget = false

        // Clear drag and drop state payload right after delivery
        if (g.dragDropPayload.delivery)
            clearDragDrop()
    }

    /** ~GetDragDropPayload */
    val dragDropPayload: Payload?
        get() = if (g.dragDropActive && g.dragDropPayload.dataFrameCount != -1) g.dragDropPayload else null
}