package imgui.api

import glm_.vec2.Vec2
import imgui.ImGui
import imgui.internal.floor

// Text Utilities
interface textUtilities {

    /** Calculate text size. Text can be multi-line. Optionally ignore text after a ## marker.
     *  CalcTextSize("") should return ImVec2(0.0f, g.FontSize)   */
    fun calcTextSize(text: String, hideTextAfterDoubleHash: Boolean = false, wrapWidth: Float = -1f): Vec2 {
        val bytes = text.toByteArray()
        return calcTextSize(bytes, 0, bytes.size, hideTextAfterDoubleHash, wrapWidth)
    }

    /** Calculate text size. Text can be multi-line. Optionally ignore text after a ## marker.
     *  CalcTextSize("") should return ImVec2(0.0f, g.FontSize)   */
    fun calcTextSize(text: ByteArray, textBegin: Int, textEnd: Int = text.size, hideTextAfterDoubleHash: Boolean = false, wrapWidth: Float = -1f): Vec2 {

        val textDisplayEnd = when {
            hideTextAfterDoubleHash -> ImGui.findRenderedTextEnd(text, 0, textEnd)  // Hide anything after a '##' string
            else -> textEnd
        }

        val font = g.font
        val fontSize = g.fontSize
        return when (textDisplayEnd) {
            0 -> Vec2(0f, fontSize)
            else -> font.calcTextSizeA(fontSize, Float.MAX_VALUE, wrapWidth, text, textEnd = textDisplayEnd).apply {
                // Round
                // FIXME: This has been here since Dec 2015 (7b0bf230) but down the line we want this out.
                // FIXME: Investigate using ceilf or e.g.
                // - https://git.musl-libc.org/cgit/musl/tree/src/math/ceilf.c
                // - https://embarkstudios.github.io/rust-gpu/api/src/libm/math/ceilf.rs.html
                x = floor(x + 0.99999f)
            }
        }
    }
}