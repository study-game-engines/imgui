package imgui.font


import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.classes.DrawList
import imgui.has
import imgui.internal.charIsBlankA
import imgui.internal.charIsBlankW
import imgui.internal.round
import imgui.internal.textCharFromUtf8
import kool.lim
import kool.pos
import org.lwjgl.system.Platform
import uno.kotlin.plusAssign
import unsigned.toUInt
import kotlin.math.floor


/** Font runtime data and rendering
 *  ImFontAtlas automatically loads a default embedded font for you when you call GetTexDataAsAlpha8() or
 *  GetTexDataAsRGBA32().   */
class Font {

    // @formatter:off

    // Members: Hot ~20/24 bytes (for CalcTextSize)

    /** Sparse. Glyphs->AdvanceX in a directly indexable way (cache-friendly for CalcTextSize functions which only this info,
     *  and are often bottleneck in large UI). */
    val indexAdvanceX = ArrayList<Float>()      // 12/16 // out //

    var fallbackAdvanceX = 0f                   // 4     // out // = FallbackGlyph->AdvanceX

    /** Height of characters, set during loading (don't change after loading)   */
    var fontSize = 0f                           // 4     // in  // <user set>

    // Members: Hot ~28/40 bytes (for CalcTextSize + render loop)

    /** Sparse. Index glyphs by Unicode code-point. */
    val indexLookup = ArrayList<Int>()          // 12-16 // out //

    /** All glyphs. */
    val glyphs = ArrayList<FontGlyph>()         // 12-16 // out //

    var fallbackGlyph: FontGlyph? = null        // 4-8   // out // = FindGlyph(FontFallbackChar)

    // Members: Cold ~32/40 bytes

    /** What we has been loaded into    */
    lateinit var containerAtlas: FontAtlas      // 4-8   // out //

    /** Pointer within ContainerAtlas->ConfigData   */
    val configData = ArrayList<FontConfig>()    // 4-8   // in  //

    /** Number of ImFontConfig involved in creating this font. Bigger than 1 when merging multiple font sources into one ImFont.    */
    var configDataCount = 0                     // 2     // in  // ~ 1

    /** Character used if a glyph isn't found. */
    var fallbackChar = '\uFFFF'                      // out // = FFFD/'?'

    /** Character used for ellipsis rendering. */
    var ellipsisChar = '\uFFFF'                 // out // = '...'/'.'

    var ellipsisCharCount = 0  // 1     // out // 1 or 3

    /** Width */
    var ellipsisWidth = 0f      // 4     // out

    /** Step between characters when EllipsisCount > 0 */
    var ellipsisCharStep = 0f   // 4     // out

    var dirtyLookupTables = true                // 1     // out //

    /** Base font scale, multiplied by the per-window font scale which you can adjust with SetWindowFontScale()   */
    var scale = 1f                              // 4     // in  // = 1.f

    /** Ascent: distance from top to bottom of e.g. 'A' [0..FontSize]   */
    var ascent = 0f                             // 4     // out

    var descent = 0f                            // 4     // out

    /** Total surface in pixels to get an idea of the font rasterization/texture cost (not exact, we approximate the cost of padding between glyphs)    */
    var metricsTotalSurface = 0                 // 4     // out

    /** 2 bytes if ImWchar=ImWchar16, 34 bytes if ImWchar==ImWchar32. Store 1-bit for each block of 4K codepoints that has one active glyph. This is mainly used to facilitate iterations across all used codepoints. */
    val used4kPagesMap = ByteArray((UNICODE_CODEPOINT_MAX + 1) / 4096 / 8)

    // @formatter:on

    fun findGlyph(c: Char): FontGlyph? = findGlyph(c.remapCodepointIfProblematic())
    fun findGlyph(c: Int): FontGlyph? = indexLookup.getOrNull(c)?.let { glyphs.getOrNull(it) } ?: fallbackGlyph

    fun findGlyphNoFallback(c: Char): FontGlyph? = findGlyphNoFallback(c.i)
    fun findGlyphNoFallback(c: Int): FontGlyph? = indexLookup.getOrNull(c)?.let { glyphs.getOrNull(it) }

    fun getCharAdvance(c: Char): Float = if (c < indexAdvanceX.size) indexAdvanceX[c.i] else fallbackAdvanceX

    val isLoaded: Boolean
        get() = ::containerAtlas.isInitialized

    val debugName: String
        get() = configData.getOrNull(0)?.name ?: "<unknown>"

    /*  'maxWidth' stops rendering after a certain width (could be turned into a 2d size). FLT_MAX to disable.
        'wrapWidth' enable automatic word-wrapping across multiple lines to fit into given width. 0.0f to disable. */
    fun calcTextSizeA(size: Float, maxWidth: Float, wrapWidth: Float, text: ByteArray, textBegin: Int = 0,
                      textEnd: Int = text.strlen(), remaining: MutableReference<Int>? = null): Vec2 { // utf8

        val lineHeight = size
        val scale = size / fontSize

        val textSize = Vec2(0)
        var lineWidth = 0f

        val wordWrapEnabled = wrapWidth > 0f
        var wordWrapEol = -1

        var s = textBegin
        while (s < textEnd) {

            if (wordWrapEnabled) {

                // Calculate how far we can render. Requires two passes on the string data but keeps the code simple and not intrusive for what's essentially an uncommon feature.
                if (wordWrapEol == -1)
                    wordWrapEol = calcWordWrapPositionA(scale, text, s, textEnd, wrapWidth - lineWidth)

                if (s >= wordWrapEol) {
                    if (textSize.x < lineWidth)
                        textSize.x = lineWidth
                    textSize.y += lineHeight
                    lineWidth = 0f
                    wordWrapEol = -1
                    s = calcWordWrapNextLineStartA(text, s, textEnd) // Wrapping skips upcoming blanks
                    continue
                }
            }

            // Decode and advance source
            val prevS = s
            var c = text[s].toUInt()
            if (c < 0x80)
                s += 1
            else {
                val (char, bytes) = textCharFromUtf8(text, s, textEnd)
                c = char
                s += bytes
            }

            if (c < 32) {
                if (c == '\n'.i) {
                    textSize.x = glm.max(textSize.x, lineWidth)
                    textSize.y += lineHeight
                    lineWidth = 0f
                    continue
                }
                if (c == '\r'.i) continue
            }

            val charWidth = indexAdvanceX.getOrElse(c) { fallbackAdvanceX } * scale
            if (lineWidth + charWidth >= maxWidth) {
                s = prevS
                break
            }
            lineWidth += charWidth
        }

        if (textSize.x < lineWidth)
            textSize.x = lineWidth

        if (lineWidth > 0 || textSize.y == 0f)
            textSize.y += lineHeight

        remaining?.set(s)

        return textSize
    }

    /** Wrapping skips upcoming blanks */
    fun calcWordWrapNextLineStartA(text: ByteArray, textBegin: Int = 0, textEnd: Int = text.size): Int {
        var p = textBegin
        while (p < textEnd && charIsBlankA(text[p].i))
            p++
        if (Char(text[p].i) == '\n')
            p++
        return p
    }

    // Simple word-wrapping for English, not full-featured. Please submit failing cases!
    // This will return the next location to wrap from. If no wrapping if necessary, this will fast-forward to e.g. text_end.
    // FIXME: Much possible improvements (don't cut things like "word !", "word!!!" but cut within "word,,,,", more sensible support for punctuations, support for Unicode punctuations, etc.)
    fun calcWordWrapPositionA(scale: Float, text: ByteArray, textBegin: Int, textEnd: Int, wrapWidth_: Float): Int {

        // For references, possible wrap point marked with ^
        //  "aaa bbb, ccc,ddd. eee   fff. ggg!"
        //      ^    ^    ^   ^   ^__    ^    ^

        // List of hardcoded separators: .,;!?'"

        // Skip extra blanks after a line returns (that includes not counting them in width computation)
        // e.g. "Hello    world" --> "Hello" "World"

        // Cut words that cannot possibly fit within one line.
        // e.g.: "The tropical fish" with ~5 characters worth of width --> "The tr" "opical" "fish"
        var lineWidth = 0f
        var wordWidth = 0f
        var blankWidth = 0f
        val wrapWidth = wrapWidth_ / scale   // We work with unscaled widths to avoid scaling every characters

        var wordEnd = textBegin
        var prevWordEnd = -1
        var insideWord = true

        var s = textBegin
        assert(textEnd != -1)
        while (s < textEnd) {
            var c = text[s].toUInt()
            val nextS = s + when {
                c < 0x80 -> 1
                else -> {
                    val (char, bytes) = textCharFromUtf8(text, s, textEnd)
                    c = char
                    bytes
                }
            }

            if (c < 32) {
                if (c == '\n'.i) {
                    lineWidth = 0f
                    wordWidth = 0f
                    blankWidth = 0f
                    insideWord = true
                    s = nextS
                    continue
                }
                if (c == '\r'.i) {
                    s = nextS
                    continue
                }
            }
            val charWidth = indexAdvanceX.getOrElse(c) { fallbackAdvanceX }
            if (charIsBlankW(c)) {
                if (insideWord) {
                    lineWidth += blankWidth
                    blankWidth = 0.0f
                    wordEnd = s
                }
                blankWidth += charWidth
                insideWord = false
            } else {
                wordWidth += charWidth
                if (insideWord)
                    wordEnd = nextS
                else {
                    prevWordEnd = wordEnd
                    lineWidth += wordWidth + blankWidth
                    wordWidth = 0f
                    blankWidth = 0f
                }
                // Allow wrapping after punctuation.
                insideWord = c != '.'.i && c != ','.i && c != ';'.i && c != '!'.i && c != '?'.i && c != '\"'.i
            }

            // We ignore blank width at the end of the line (they can be skipped)
            if (lineWidth + wordWidth > wrapWidth) {
                // Words that cannot possibly fit within an entire line will be cut anywhere.
                if (wordWidth < wrapWidth)
                    s = if (prevWordEnd != -1) prevWordEnd else wordEnd
                break
            }

            s = nextS
        }

        // Wrap_width is too small to fit anything. Force displaying 1 character to minimize the height discontinuity.
        // +1 may not be a character start point in UTF-8 but it's ok because caller loops use (text >= word_wrap_eol).
        if (s == textBegin && textBegin < textEnd)
            return s + 1
        return s
    }

    /** Note: as with every ImDrawList drawing function, this expects that the font atlas texture is bound. */
    fun renderChar(drawList: DrawList, size: Float, pos: Vec2, col_: Int, c: Char) {
        var col = col_
        val glyph = findGlyph(c)
        if (glyph == null || !glyph.visible)
            return
        if (glyph.colored)
            col = col or COL32_A_MASK.inv()
        val scale = if (size >= 0f) size / fontSize else 1f
        val x = floor(pos.x)
        val y = floor(pos.y)
        drawList.primReserve(6, 4)
        drawList.primRectUV(Vec2(x + glyph.x0 * scale, y + glyph.y0 * scale), Vec2(x + glyph.x1 * scale, y + glyph.y1 * scale), Vec2(glyph.u0, glyph.v0), Vec2(glyph.u1, glyph.v1), col)
    }

    //    const ImVec4& clipRect, const char* text, const char* textEnd, float wrapWidth = 0.0f, bool cpuFineClip = false) const;

    /** Note: as with every ImDrawList drawing function, this expects that the font atlas texture is bound. */
    fun renderText(drawList: DrawList, size: Float, pos: Vec2, col: Int, clipRect: Vec4, text: ByteArray,
                   textBegin: Int, textEnd_: Int = text.strlen(textBegin), // ImGui:: functions generally already provides a valid text_end, so this is merely to handle direct calls.
                   wrapWidth: Float = 0f, cpuFineClip: Boolean = false) {

        var textEnd = textEnd_

        // Align to be pixel perfect
        var (x, y) = pos(floor(pos.x), floor(pos.y))
        if (y > clipRect.w) return

        val startX = x
        val scale = size / fontSize
        val lineHeight = fontSize * scale
        val wordWrapEnabled = wrapWidth > 0f

        // Fast-forward to first visible line
        var s = textBegin
        if (y + lineHeight < clipRect.y)
            while (y + lineHeight < clipRect.y && s < textEnd) {
                val lineEnd = text.memchr(s, '\n')
                if (wordWrapEnabled) {
                    // FIXME-OPT: This is not optimal as do first do a search for \n before calling CalcWordWrapPositionA().
                    // If the specs for CalcWordWrapPositionA() were reworked to optionally return on \n we could combine both.
                    // However it is still better than nothing performing the fast-forward!
                    s = calcWordWrapPositionA(scale, text, s, if (lineEnd == -1) textEnd else lineEnd, wrapWidth)
                    s = calcWordWrapNextLineStartA(text, s, textEnd)
                } else
                    s = if (lineEnd != -1) lineEnd + 1 else textEnd
                y += lineHeight
            }

        /*  For large text, scan for the last visible line in order to avoid over-reserving in the call to PrimReserve()
            Note that very large horizontal line will still be affected by the issue (e.g. a one megabyte string buffer without a newline will likely crash atm)         */
        if (textEnd - s > 10000 && !wordWrapEnabled) {
            var sEnd = s
            var yEnd = y
            while (yEnd < clipRect.w && s < textEnd) {
                sEnd = text.memchr(sEnd, '\n')
                sEnd = if (sEnd == -1) sEnd + 1 else textEnd
                yEnd += lineHeight
            }
            textEnd = sEnd
        }
        if (s == textEnd)
            return


        // Reserve vertices for remaining worse case (over-reserving is useful and easily amortized)
        val vtxCountMax = (textEnd - s) * 4
        val idxCountMax = (textEnd - s) * 6
        val idxExpectedSize = drawList.idxBuffer.lim + idxCountMax
        drawList.primReserve(idxCountMax, vtxCountMax)

        // [JVM] set
        drawList.vtxBuffer.pos = drawList._vtxWritePtr
        drawList.idxBuffer.pos = drawList._idxWritePtr

        var vtxWrite = drawList._vtxWritePtr
        var idxWrite = drawList._idxWritePtr
        var vtxIndex = drawList._vtxCurrentIdx

        val colUntinted = col or COL32_A_MASK.inv()
        var wordWrapEol = 0

        while (s < textEnd) {

            if (wordWrapEnabled) {

                // Calculate how far we can render. Requires two passes on the string data but keeps the code simple and not intrusive for what's essentially an uncommon feature.
                if (wordWrapEol == 0)
                    wordWrapEol = calcWordWrapPositionA(scale, text, s, textEnd, wrapWidth - (x - startX))

                if (s >= wordWrapEol) {
                    x = startX
                    y += lineHeight
                    wordWrapEol = 0
                    s = calcWordWrapNextLineStartA(text, s, textEnd) // Wrapping skips upcoming blanks
                    continue
                }
            }
            // Decode and advance source
            var c = text[s].toUInt()
            if (c < 0x80)
                s += 1
            else {
                val (char, bytes) = textCharFromUtf8(text, s, textEnd)
                c = char
                s += bytes
            }

            if (c < 32) {
                if (c == '\n'.i) {
                    x = startX
                    y += lineHeight
                    if (y > clipRect.w)
                        break // break out of main loop
                    continue
                }
                if (c == '\r'.i) continue
            }

            val glyph = findGlyph(c) ?: continue

            val charWidth = glyph.advanceX * scale
            if (glyph.visible) {
                // We don't do a second finer clipping test on the Y axis as we've already skipped anything before clip_rect.y and exit once we pass clip_rect.w
                var x1 = x + glyph.x0 * scale
                var x2 = x + glyph.x1 * scale
                var y1 = y + glyph.y0 * scale
                var y2 = y + glyph.y1 * scale
                if (x1 <= clipRect.z && x2 >= clipRect.x) {
                    // Render a character
                    var u1 = glyph.u0
                    var v1 = glyph.v0
                    var u2 = glyph.u1
                    var v2 = glyph.v1

                    // CPU side clipping used to fit text in their frame when the frame is too small. Only does clipping for axis aligned quads.
                    if (cpuFineClip) {
                        if (x1 < clipRect.x) {
                            u1 += (1f - (x2 - clipRect.x) / (x2 - x1)) * (u2 - u1)
                            x1 = clipRect.x
                        }
                        if (y1 < clipRect.y) {
                            v1 += (1f - (y2 - clipRect.y) / (y2 - y1)) * (v2 - v1)
                            y1 = clipRect.y
                        }
                        if (x2 > clipRect.z) {
                            u2 = u1 + ((clipRect.z - x1) / (x2 - x1)) * (u2 - u1)
                            x2 = clipRect.z
                        }
                        if (y2 > clipRect.w) {
                            v2 = v1 + ((clipRect.w - y1) / (y2 - y1)) * (v2 - v1)
                            y2 = clipRect.w
                        }
                        if (y1 >= y2) {
                            x += charWidth
                            continue
                        }
                    }

                    // Support for untinted glyphs
                    val glyphCol = if (glyph.colored) colUntinted else col

                    // We are NOT calling PrimRectUV() here because non-inlined causes too much overhead in a debug builds. Inlined here:
                    drawList.apply {
                        vtxBuffer.let {
                            it += x1; it += y1; it += u1; it += v1; it += glyphCol
                            it += x2; it += y1; it += u2; it += v1; it += glyphCol
                            it += x2; it += y2; it += u2; it += v2; it += glyphCol
                            it += x1; it += y2; it += u1; it += v2; it += glyphCol
                        }
                        idxBuffer.let {
                            it += vtxIndex; it += vtxIndex + 1; it += vtxIndex + 2
                            it += vtxIndex; it += vtxIndex + 2; it += vtxIndex + 3
                        }
                        vtxWrite += 4
                        vtxIndex += 4
                        idxWrite += 6
                    }
                }
            }
            x += charWidth
        }
        // [JVM] reset
        drawList.vtxBuffer.pos = 0
        drawList.idxBuffer.pos = 0

        // Give back unused vertices (clipped ones, blanks) ~ this is essentially a PrimUnreserve() action.
        drawList.vtxBuffer.lim = vtxWrite    // Same as calling shrink()
        drawList.idxBuffer.lim = idxWrite
        drawList.cmdBuffer.last().elemCount -= (idxExpectedSize - drawList.idxBuffer.lim)
        drawList._vtxWritePtr = vtxWrite
        drawList._idxWritePtr = idxWrite
        drawList._vtxCurrentIdx = vtxIndex
    }

    // [Internal] Don't use!

    /** Until we move this to runtime and/or add proper tab support, at least allow users to compile-time override */
    var TABSIZE = 4

    fun buildLookupTable() {

        val maxCodepoint = glyphs.map { it.codepoint.i }.maxOrNull()!!

        // Build lookup table
        assert(glyphs.size < 0xFFFF) { "-1 is reserved" }
        indexAdvanceX.clear()
        indexLookup.clear()
        dirtyLookupTables = false
        used4kPagesMap.fill(0)
        growIndex(maxCodepoint + 1)
        glyphs.forEachIndexed { i, g ->
            indexAdvanceX[g.codepoint.i] = g.advanceX
            indexLookup[g.codepoint.i] = i

            // Mark 4K page as used
            val pageN = g.codepoint.i / 4096
            used4kPagesMap[pageN shr 3] = used4kPagesMap[pageN shr 3] or (1 shl (pageN and 7))
        }

        // Create a glyph to handle TAB
        // FIXME: Needs proper TAB handling but it needs to be contextualized (or we could arbitrary say that each string starts at "column 0" ?)
        if (findGlyph(' ') != null) {
            if (glyphs.last().codepoint != '\t')   // So we can call this function multiple times (FIXME: Flaky)
                glyphs += FontGlyph()
            val tabGlyph = glyphs.last()
            tabGlyph put findGlyph(' ')!!
            tabGlyph.codepoint = '\t'
            tabGlyph.advanceX *= TABSIZE
            indexAdvanceX[tabGlyph.codepoint.i] = tabGlyph.advanceX
            indexLookup[tabGlyph.codepoint.i] = glyphs.lastIndex
        }

        // Mark special glyphs as not visible (note that AddGlyph already mark as non-visible glyphs with zero-size polygons)
        setGlyphVisible(' ', false)
        setGlyphVisible('\t', false)

        // Setup Fallback character
        val fallbackChars = listOf(UNICODE_CODEPOINT_INVALID.toChar(), '?', ' ')
        fallbackGlyph = findGlyphNoFallback(fallbackChar)
        if (fallbackGlyph == null) {
            fallbackChar = findFirstExistingGlyph(fallbackChars)
            fallbackGlyph = findGlyphNoFallback(fallbackChar)
            if (fallbackGlyph == null) {
                fallbackGlyph = glyphs.last()
                fallbackChar = fallbackGlyph!!.codepoint
            }
        }
        fallbackAdvanceX = fallbackGlyph!!.advanceX
        for (i in 0..maxCodepoint)
            if (indexAdvanceX[i] < 0f)
                indexAdvanceX[i] = fallbackAdvanceX

        // Setup Ellipsis character. It is required for rendering elided text. We prefer using U+2026 (horizontal ellipsis).
        // However some old fonts may contain ellipsis at U+0085. Here we auto-detect most suitable ellipsis character.
        // FIXME: Note that 0x2026 is rarely included in our font ranges. Because of this we are more likely to use three individual dots.
        val ellipsisChars = listOf('\u2026', '\u0085')
        val dotsChars = listOf('.', '\uFF0E')
        if (ellipsisChar == '\uFFFF')
            ellipsisChar = findFirstExistingGlyph(ellipsisChars)
        val dotChar = findFirstExistingGlyph(dotsChars)
        if (ellipsisChar != '\uFFFF') {
            ellipsisCharCount = 1
            ellipsisCharStep = findGlyph(ellipsisChar)!!.x1
            ellipsisWidth = ellipsisCharStep
        } else if (dotChar != '\uFFFF') {
            val glyph = findGlyph(dotChar)!!
            ellipsisChar = dotChar
            ellipsisCharCount = 3
            ellipsisCharStep = (glyph.x1 - glyph.x0) + 1f
            ellipsisWidth = ellipsisCharStep * 3f - 1f
        }
    }

    fun clearOutputData() {
        fontSize = 0f
        fallbackAdvanceX = 0.0f
        glyphs.clear()
        indexAdvanceX.clear()
        indexLookup.clear()
        fallbackGlyph = null
        dirtyLookupTables = true
        fallbackAdvanceX = 0f
        configDataCount = 0
        configData.clear()
        containerAtlas.clearInputData()
        containerAtlas.clearTexData()
        ascent = 0f
        descent = 0f
        metricsTotalSurface = 0
    }

    private fun growIndex(newSize: Int) {
        assert(indexAdvanceX.size == indexLookup.size)
        if (newSize <= indexLookup.size)
            return
        for (i in indexLookup.size until newSize) {
            indexAdvanceX += -1f
            indexLookup += -1
        }
    }

    /** x0/y0/x1/y1 are offset from the character upper-left layout position, in pixels.
     *  Therefore x0/y0 are often fairly close to zero.
     *  Not to be mistaken with texture coordinates, which are held by u0/v0/u1/v1 in normalized format
     *  (0.0..1.0 on each texture axis).
     * 'cfg' is not necessarily == 'this->ConfigData' because multiple source fonts+configs can be used to build
     *  one target font. */
    fun addGlyph(cfg: FontConfig?, codepoint: Int, x0_: Float, y0: Float, x1_: Float, y1: Float, u0: Float, v0: Float,
                 u1: Float, v1: Float, advanceX_: Float) {

        var x0 = x0_
        var x1 = x1_
        var advanceX = advanceX_
        if (cfg != null) {
            // Clamp & recenter if needed
            val advanceXOriginal = advanceX
            advanceX = clamp(advanceX, cfg.glyphMinAdvanceX, cfg.glyphMaxAdvanceX)
            if (advanceX != advanceXOriginal) {
                val charOffX = if (cfg.pixelSnapH) floor((advanceX - advanceXOriginal) * 0.5f) else (advanceX - advanceXOriginal) * 0.5f
                x0 += charOffX
                x1 += charOffX
            }

            // Snap to pixel
            if (cfg.pixelSnapH)
                advanceX = round(advanceX)

            // Bake spacing
            advanceX += cfg.glyphExtraSpacing.x
        }

        val glyph = FontGlyph()
        glyphs += glyph
        glyph.codepoint = codepoint.c
        glyph.visible = x0 != x1 && y0 != y1
        glyph.colored = false
        glyph.x0 = x0
        glyph.y0 = y0
        glyph.x1 = x1
        glyph.y1 = y1
        glyph.u0 = u0
        glyph.v0 = v0
        glyph.u1 = u1
        glyph.v1 = v1
        glyph.advanceX = advanceX

        if (configData[0].pixelSnapH)
            glyph.advanceX = round(glyph.advanceX)
        // Compute rough surface usage metrics (+1 to account for average padding, +0.99 to round)
        // We use (U1-U0)*TexWidth instead of X1-X0 to account for oversampling.
        val pad = containerAtlas.texGlyphPadding + 0.99f
        dirtyLookupTables = true
        metricsTotalSurface += ((glyph.u1 - glyph.u0) * containerAtlas.texSize.x + pad).i *
                ((glyph.v1 - glyph.v0) * containerAtlas.texSize.y + pad).i
    }

    /** Makes 'dst' character/glyph points to 'src' character/glyph. Currently needs to be called AFTER fonts have been built.  */
    fun addRemapChar(dst: Int, src: Int, overwriteDst: Boolean = true) {
        // Currently this can only be called AFTER the font has been built, aka after calling ImFontAtlas::GetTexDataAs*() function.
        assert(indexLookup.isNotEmpty())
        val indexSize = indexLookup.size

        if (dst < indexSize && indexLookup[dst] == -1 && !overwriteDst) // 'dst' already exists
            return
        if (src >= indexSize && dst >= indexSize) // both 'dst' and 'src' don't exist -> no-op
            return

        growIndex(dst + 1)
        indexLookup[dst] = indexLookup.getOrElse(src) { -1 }
        indexAdvanceX[dst] = indexAdvanceX.getOrElse(src) { 1f }
    }

    fun setGlyphVisible(c: Char, visible: Boolean = true) {
        findGlyph(c)?.visible = visible
    }

    /** API is designed this way to avoid exposing the 4K page size
     *  e.g. use with IsGlyphRangeUnused(0, 255) */
    fun isGlyphRangeUnused(cBegin: Int, cLast: Int): Boolean {
        val pageBegin = cBegin / 4096
        val pageLast = cLast / 4096
        for (pageN in pageBegin..pageLast)
            if ((pageN ushr 3) < used4kPagesMap.size)
                if (used4kPagesMap[pageN ushr 3] has (1 shl (pageN and 7)))
                    return false
        return true
    }


    // [JVM] cant be static because we need the Font class instance to call `findGlyphNoFallback`
    fun findFirstExistingGlyph(candidateChars: List<Char>) = candidateChars.find { findGlyphNoFallback(it) != null }
            ?: '\uFFFF'

    companion object {

        internal fun Char.remapCodepointIfProblematic(): Int {
            val i = code
            return when (Platform.get()) {
                /*  https://en.wikipedia.org/wiki/Windows-1252#Character_set
                 *  manually remap the difference from  ISO-8859-1 */
                Platform.WINDOWS -> when (i) {
                    // 8_128
                    0x20AC -> 128 // €
                    0x201A -> 130 // ‚
                    0x0192 -> 131 // ƒ
                    0x201E -> 132 // „
                    0x2026 -> 133 // …
                    0x2020 -> 134 // †
                    0x2021 -> 135 // ‡
                    0x02C6 -> 136 // ˆ
                    0x2030 -> 137 // ‰
                    0x0160 -> 138 // Š
                    0x2039 -> 139 // ‹
                    0x0152 -> 140 // Œ
                    0x017D -> 142 // Ž
                    // 9_144
                    0x2018 -> 145 // ‘
                    0x2019 -> 146 // ’
                    0x201C -> 147 // “
                    0x201D -> 148 // ”
                    0x2022 -> 149 // •
                    0x2013 -> 150 // –
                    0x2014 -> 151 // —
                    0x02DC -> 152 // ˜
                    0x2122 -> 153 // ™
                    0x0161 -> 154 // š
                    0x203A -> 155 // ›
                    0x0153 -> 156 // œ
                    0x017E -> 158 // ž
                    0x0178 -> 159 // Ÿ
                    else -> i
                }

                else -> i
            }
        }
    }
}