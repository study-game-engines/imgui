package imgui.internal.api

import glm_.func.common.max
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcItemSize
import imgui.ImGui.calcTextSize
import imgui.ImGui.calcWrapWidthForPos
import imgui.ImGui.currentWindow
import imgui.ImGui.frameHeight
import imgui.ImGui.getColorU32
import imgui.ImGui.isClippedEx
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.logRenderedText
import imgui.ImGui.logText
import imgui.ImGui.popColumnsBackground
import imgui.ImGui.pushColumnsBackground
import imgui.ImGui.renderArrow
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.renderTextWrapped
import imgui.ImGui.style
import imgui.api.g
import imgui.internal.classes.Rect
import imgui.internal.sections.*
import kotlin.math.max

/** Widgets */
internal interface widgets {

    /** Raw text without formatting. Roughly equivalent to text("%s", text) but:
     *  A) doesn't require null terminated string if 'textEnd' is specified
     *  B) it's faster, no memory copy is done, no buffer size limits, recommended for long chunks of text. */
    fun textEx(text: String, textEnd: Int = -1, flags: TextFlags = emptyFlags) {
        val bytes = text.toByteArray()
        textEx(bytes, if (textEnd != -1) textEnd else bytes.strlen(), flags)
    }

    /** Raw text without formatting. Roughly equivalent to text("%s", text) but:
     *  A) doesn't require null terminated string if 'textEnd' is specified
     *  B) it's faster, no memory copy is done, no buffer size limits, recommended for long chunks of text. */
    fun textEx(text: ByteArray, textEnd: Int = text.strlen(), flags: TextFlags = emptyFlags) {

        val window = currentWindow
        if (window.skipItems)
            return

        // Accept null ranges
        if (textEnd == 0) // ~text == text_end
            if (text.isNotEmpty())
                text[0] = 0 // ~text_end = "";

        // Calculate length

        val textPos = Vec2(window.dc.cursorPos.x, window.dc.cursorPos.y + window.dc.currLineTextBaseOffset)
        val wrapPosX = window.dc.textWrapPos
        val wrapEnabled = wrapPosX >= 0f
        if (textEnd <= 2000 || wrapEnabled) {
            // Common case
            val wrapWidth = if (wrapEnabled) calcWrapWidthForPos(window.dc.cursorPos, wrapPosX) else 0f
            val textSize = calcTextSize(text, 0, textEnd, false, wrapWidth)

            val bb = Rect(textPos, textPos + textSize)
            itemSize(textSize, 0f)
            if (!itemAdd(bb, 0))
                return

            // Render (we don't hide text after ## in this end-user function)
            renderTextWrapped(bb.min, text, textEnd, wrapWidth)
        } else {
            // Long text!
            // Perform manual coarse clipping to optimize for long multi-line text
            // - From this point we will only compute the width of lines that are visible. Optimization only available when word-wrapping is disabled.
            // - We also don't vertically center the text within the line full height, which is unlikely to matter because we are likely the biggest and only item on the line.
            // - We use memchr(), pay attention that well optimized versions of those str/mem functions are much faster than a casually written loop.
            var line = 0
            val lineHeight = ImGui.textLineHeight
            val textSize = Vec2()

            // Lines to skip (can't skip when logging text)
            val pos = Vec2(textPos)
            if (!g.logEnabled) {
                val linesSkippable = ((window.clipRect.min.y - textPos.y) / lineHeight).i
                if (linesSkippable > 0) {
                    var linesSkipped = 0
                    while (line < textEnd && linesSkipped < linesSkippable) {
                        var lineEnd = text.memchr(line, '\n', textEnd - line)
                        if (lineEnd == -1)
                            lineEnd = textEnd
                        if (flags hasnt TextFlag.NoWidthForLargeClippedText)
                            textSize.x = textSize.x max calcTextSize(text, line, lineEnd).x
                        line = lineEnd + 1
                        linesSkipped++
                    }
                    pos.y += linesSkipped * lineHeight
                }
            }

            // Lines to render
            if (line < textEnd) {
                val lineRect = Rect(pos, pos + Vec2(Float.MAX_VALUE, lineHeight))
                while (line < textEnd) {
                    if (isClippedEx(lineRect, 0))
                        break

                    var lineEnd = text.memchr(line, '\n', textEnd - line)
                    if (lineEnd == -1)
                        lineEnd = textEnd
                    textSize.x = textSize.x max calcTextSize(text, line, lineEnd).x
                    renderText(pos, text, line, lineEnd, false)
                    line = lineEnd + 1
                    lineRect.min.y += lineHeight
                    lineRect.max.y += lineHeight
                    pos.y += lineHeight
                }

                // Count remaining lines
                var linesSkipped = 0
                while (line < textEnd) {
                    var lineEnd = text.memchr(line, '\n', textEnd - line)
                    if (lineEnd == -1)
                        lineEnd = textEnd
                    if (flags hasnt TextFlag.NoWidthForLargeClippedText)
                        textSize.x = textSize.x max calcTextSize(text, line, lineEnd).x
                    line = lineEnd + 1
                    linesSkipped++
                }
                pos.y += linesSkipped * lineHeight
            }
            textSize.y = pos.y - textPos.y

            val bb = Rect(textPos, textPos + textSize)
            itemSize(textSize, 0f)
            itemAdd(bb, 0)
        }
    }

    fun buttonEx(label: String, sizeArg: Vec2 = Vec2(), flags_: ButtonFlags = emptyFlags): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)

        val pos = Vec2(window.dc.cursorPos)
        if (flags_ has ButtonFlag.AlignTextBaseLine && style.framePadding.y < window.dc.currLineTextBaseOffset) // Try to vertically align buttons that are smaller/have no padding so that text baseline matches (bit hacky, since it shouldn't be a flag)
            pos.y += window.dc.currLineTextBaseOffset - style.framePadding.y
        val size = calcItemSize(sizeArg, labelSize.x + style.framePadding.x * 2f, labelSize.y + style.framePadding.y * 2f)

        val bb = Rect(pos, pos + size)
        itemSize(size, style.framePadding.y)
        if (!itemAdd(bb, id))
            return false

        var flags = flags_
        if (g.lastItemData.inFlags has ItemFlag.ButtonRepeat)
            flags /= ButtonFlag.Repeat

        val (pressed, hovered, held) = buttonBehavior(bb, id, flags)

        // Render
        val col = if (hovered && held) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, col.u32, true, style.frameRounding)

        val renderTextPos = Rect(bb.min + style.framePadding, bb.max - style.framePadding)
        if (g.logEnabled)
            logRenderedText(renderTextPos.min, "[")
        renderTextClipped(renderTextPos.min, renderTextPos.max, label, labelSize, style.buttonTextAlign, bb)
        if (g.logEnabled)
            logRenderedText(renderTextPos.min, "]")

        // Automatically close popups
        //if (pressed && !(flags & ImGuiButtonFlags_DontClosePopups) && (window->Flags & ImGuiWindowFlags_Popup))
        //    CloseCurrentPopup();

        IMGUI_TEST_ENGINE_ITEM_INFO(id, label, g.lastItemData.statusFlags)
        return pressed
    }

    /** square button with an arrow shape */
    fun arrowButtonEx(strId: String, dir: Dir, size: Vec2, flags_: ButtonFlags = emptyFlags): Boolean {

        var flags = flags_

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getID(strId)
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        val defaultSize = frameHeight
        itemSize(size, if (size.y >= defaultSize) style.framePadding.y else -1f)
        if (!itemAdd(bb, id)) return false

        if (g.lastItemData.inFlags has ItemFlag.ButtonRepeat)
            flags = flags or ButtonFlag.Repeat

        val (pressed, hovered, held) = buttonBehavior(bb, id, flags)

        // Render
        val bgCol = if (held && hovered) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button
        val textCol = Col.Text
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, bgCol.u32, true, g.style.frameRounding)
        window.drawList.renderArrow(bb.min + Vec2(max(0f, (size.x - g.fontSize) * 0.5f), max(0f, (size.y - g.fontSize) * 0.5f)), textCol.u32, dir)

        IMGUI_TEST_ENGINE_ITEM_INFO(id, strId, g.lastItemData.statusFlags)
        return pressed
    }

    /** ImageButton() is flawed as 'id' is always derived from 'texture_id' (see #2464 #1390)
     *  We provide this internal helper to write your own variant while we figure out how to redesign the public ImageButton() API. */
    fun imageButtonEx(id: ID, textureId: TextureID, size: Vec2, uv0: Vec2, uv1: Vec2, bgCol: Vec4, tintCol: Vec4): Boolean {

        val window = currentWindow
        if (window.skipItems)
            return false

        val padding = g.style.framePadding
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size + padding * 2)

        itemSize(bb)
        if (!itemAdd(bb, id))
            return false
        val (pressed, hovered, held) = buttonBehavior(bb, id)

        // Render
        val col = getColorU32(if (held && hovered) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button)
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, col, true, clamp(min(padding.x, padding.y), 0f, style.frameRounding))
        if (bgCol.w > 0f)
            window.drawList.addRectFilled(bb.min + padding, bb.max - padding, bgCol.u32)
        window.drawList.addImage(textureId, bb.min + padding, bb.max - padding, uv0, uv1, tintCol.u32)

        return pressed
    }

    /** Horizontal/vertical separating line
     *  Separator, generally horizontal. inside a menu bar or in horizontal layout mode, this becomes a vertical separator. */
    fun separatorEx(flags: SeparatorFlags) {

        val window = currentWindow
        if (window.skipItems) return

        assert((flags and (SeparatorFlag.Horizontal or SeparatorFlag.Vertical)).isPowerOfTwo) { "Check that only 1 option is selected" }

        val thicknessDraw = 1f
        val thicknessLayout = 0f
        if (flags has SeparatorFlag.Vertical) {
            // Vertical separator, for menu bars (use current line height). Not exposed because it is misleading and it doesn't have an effect on regular layout.
            val y1 = window.dc.cursorPos.y
            val y2 = window.dc.cursorPos.y + window.dc.currLineSize.y
            val bb = Rect(Vec2(window.dc.cursorPos.x, y1), Vec2(window.dc.cursorPos.x + thicknessDraw, y2))
            itemSize(Vec2(thicknessLayout, 0f))
            if (!itemAdd(bb, 0))
                return
            // Draw
            window.drawList.addLine(Vec2(bb.min.x, bb.min.y), Vec2(bb.min.x, bb.max.y), Col.Separator.u32)
            if (g.logEnabled)
                logText(" |")

        } else if (flags has SeparatorFlag.Horizontal) {
            // Horizontal Separator
            var x1 = window.pos.x
            var x2 = window.pos.x + window.size.x
            // FIXME-WORKRECT: old hack (#205) until we decide of consistent behavior with WorkRect/Indent and Separator
            if (g.groupStack.isNotEmpty() && g.groupStack.last().windowID == window.id)
                x1 += window.dc.indent

            // FIXME-WORKRECT: In theory we should simply be using WorkRect.Min.x/Max.x everywhere but it isn't aesthetically what we want,
            // need to introduce a variant of WorkRect for that purpose. (#4787)
            g.currentTable?.let { table ->
                x1 = table.columns[table.currentColumn].minX
                x2 = table.columns[table.currentColumn].maxX
            }

            val columns = window.dc.currentColumns.takeIf { flags has SeparatorFlag.SpanAllColumns }
            if (columns != null)
                pushColumnsBackground()

            // We don't provide our width to the layout so that it doesn't get feed back into AutoFit
            // FIXME: This prevents ->CursorMaxPos based bounding box evaluation from working (e.g. TableEndCell)
            val bb = Rect(Vec2(x1, window.dc.cursorPos.y), Vec2(x2, window.dc.cursorPos.y + thicknessDraw))
            itemSize(Vec2(0f, thicknessLayout))
            val itemVisible = itemAdd(bb, 0)
            if (itemVisible) {
                // Draw
                window.drawList.addLine(bb.min, Vec2(bb.max.x, bb.min.y), Col.Separator.u32)
                if (g.logEnabled) {
                    logRenderedText(bb.min, "--------------------------------")
                }
            }
            columns?.let {
                popColumnsBackground()
                it.lineMinY = window.dc.cursorPos.y
            }
        }
    }

    companion object {
        val isRootOfOpenMenuSet: Boolean
            get() {
                val window = g.currentWindow!!
                if ((g.openPopupStack.size <= g.beginPopupStack.size) || window.flags has WindowFlag._ChildMenu)
                    return false

                // Initially we used 'upper_popup->OpenParentId == window->IDStack.back()' to differentiate multiple menu sets from each others
                // (e.g. inside menu bar vs loose menu items) based on parent ID.
                // This would however prevent the use of e.g. PuhsID() user code submitting menus.
                // Previously this worked between popup and a first child menu because the first child menu always had the _ChildWindow flag,
                // making  hovering on parent popup possible while first child menu was focused - but this was generally a bug with other side effects.
                // Instead we don't treat Popup specifically (in order to consistently support menu features in them), maybe the first child menu of a Popup
                // doesn't have the _ChildWindow flag, and we rely on this IsRootOfOpenMenuSet() check to allow hovering between root window/popup and first child menu.
                // In the end, lack of ID check made it so we could no longer differentiate between separate menu sets. To compensate for that, we at least check parent window nav layer.
                // This fixes the most common case of menu opening on hover when moving between window content and menu bar. Multiple different menu sets in same nav layer would still
                // open on hover, but that should be a lesser problem, because if such menus are close in proximity in window content then it won't feel weird and if they are far apart
                // it likely won't be a problem anyone runs into.
                val upperPopup = g.openPopupStack[g.beginPopupStack.size]
                return window.dc.navLayerCurrent.ordinal == upperPopup.parentNavLayer && upperPopup.window?.flags?.has(WindowFlag._ChildMenu) == true
            }
    }
}