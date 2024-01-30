@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)

package imgui.font

import com.livefront.sealedenum.GenSealedEnum
import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec2.operators.div
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.style
import imgui.internal.*
import imgui.stb_.*
import kool.*
import uno.convert.decode85
import uno.kotlin.plusAssign
import uno.stb.stb
import unsigned.toUInt
import java.nio.ByteBuffer
import kotlin.math.sqrt

typealias FontAtlasFlags = Flag<FontAtlas.Flag>

/** Load and rasterize multiple TTF/OTF fonts into a same texture. The font atlas will build a single texture holding:
 *      - One or more fonts.
 *      - Custom graphics data needed to render the shapes needed by Dear ImGui.
 *      - Mouse cursor shapes for software cursor rendering (unless setting 'Flags |= ImFontAtlasFlags_NoMouseCursors' in the font atlas).
 *  It is the user-code responsibility to setup/build the atlas, then upload the pixel data into a texture accessible by your graphics api.
 *      - Optionally, call any of the AddFont*** functions. If you don't call any, the default font embedded in the code will be loaded for you.
 *      - Call GetTexDataAsAlpha8() or GetTexDataAsRGBA32() to build and retrieve pixels data.
 *      - Upload the pixels data into a texture within your graphics system (see imgui_impl_xxxx.cpp examples)
 *      - Call SetTexID(my_tex_id); and pass the pointer/identifier to your texture in a format natural to your graphics API.
 *  This value will be passed back to you during rendering to identify the texture. Read FAQ entry about ImTextureID for more details.
 *  Common pitfalls:
 *      - If you pass a 'glyph_ranges' array to AddFont*** functions, you need to make sure that your array persist up until the
 *          atlas is build (when calling GetTexData*** or Build()). We only copy the pointer, not the data.
 *      - Important: By default, AddFontFromMemoryTTF() takes ownership of the data. Even though we are not writing to it,
 *          we will free the pointer on destruction.
 *  You can set font_cfg->FontDataOwnedByAtlas=false to keep ownership of your data and it won't be freed,
 *      - Even though many functions are suffixed with "TTF", OTF data is supported just as well.
 *      - This is an old API and it is currently awkward for those and various other reasons! We will address them in the future! */
class FontAtlas {

    fun addFont(fontCfg: FontConfig): Font {

        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        assert(fontCfg.fontData.isNotEmpty())
        assert(fontCfg.sizePixels > 0f)

        // Create new font
        if (!fontCfg.mergeMode)
            fonts += Font()
        else
        // When using MergeMode make sure that a font has already been added before. You can use io.fonts.addFontDefault to add the default imgui font.
            assert(fonts.isNotEmpty()) { "Cannot use MergeMode for the first font" }
        configData.add(fontCfg)
        if (fontCfg.dstFont == null)
            fontCfg.dstFont = fonts.last()
        if (!fontCfg.fontDataOwnedByAtlas)
            fontCfg.fontDataOwnedByAtlas = true
        //            memcpy(new_font_cfg.FontData, font_cfg->FontData, (size_t)new_font_cfg.FontDataSize) TODO check, same object?

        if (fontCfg.dstFont!!.ellipsisChar == '\uffff')
            fontCfg.dstFont!!.ellipsisChar = fontCfg.ellipsisChar

        // Invalidate texture
        texReady = false
        clearTexData()
        return fontCfg.dstFont!!
    }

    /** Load embedded ProggyClean.ttf at size 13, disable oversampling  */
    fun addFontDefault(fontCfgTemplate: FontConfig? = null): Font {

        val fontCfg = fontCfgTemplate ?: FontConfig()
        if (fontCfgTemplate == null) {
            fontCfg.oversample put 1
            fontCfg.pixelSnapH = true
        }
        if (fontCfg.sizePixels <= 0f)
            fontCfg.sizePixels = 13f * 1f
        if (fontCfg.name == "")
            fontCfg.name = "ProggyClean.ttf, ${fontCfg.sizePixels.i}px"
        fontCfg.ellipsisChar = '\u0085'
        fontCfg.glyphOffset.y = 1f * floor(fontCfg.sizePixels / 13f)  // Add +1 offset per 13 units

        val ttfCompressedBase85 = proggyCleanTtfCompressedDataBase85
        val glyphRanges = fontCfg.glyphRanges.takeIf { it.isNotEmpty() } ?: glyphRanges.default
        return addFontFromMemoryCompressedBase85TTF(ttfCompressedBase85, fontCfg.sizePixels, fontCfg, glyphRanges)
//        return addFontFromMemoryTTF(fileLoadToMemory("fonts/ProggyClean.ttf")!!, fontCfg.sizePixels, fontCfg, glyphRanges)
    }

    fun addFontFromFileTTF(filename: String, sizePixels: Float, fontCfg: FontConfig = FontConfig(), glyphRanges: Array<IntRange> = emptyArray()): Font? {

        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        val bytes = fileLoadToMemory(filename) ?: run {
            System.err.println("Could not load font file.")
            return null
        }
        if (fontCfg.name.isEmpty())
        // Store a short copy of filename into into the font name for convenience
            fontCfg.name = "${filename.substringAfterLast('/')}, %.0fpx".format(style.locale, sizePixels)
        return addFontFromMemoryTTF(bytes, sizePixels, fontCfg, glyphRanges)
    }

    /** Note: Transfer ownership of 'ttfData' to FontAtlas! Will be deleted after destruction of the atlas.
     *  Set font_cfg->FontDataOwnedByAtlas=false to keep ownership of your data and it won't be freed. */
    fun addFontFromMemoryTTF(fontData: ByteArray, sizePixels: Float, fontCfg: FontConfig = FontConfig(), glyphRanges: Array<IntRange> = emptyArray()): Font {
        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        assert(fontCfg.fontData.isEmpty())
        fontCfg.fontData = fontData
        fontCfg.sizePixels = sizePixels
        if (glyphRanges.isNotEmpty())
            fontCfg.glyphRanges = glyphRanges
        return addFont(fontCfg)
    }

    fun addFontFromMemoryTTF(fontData: CharArray, sizePixels: Float, fontCfg: FontConfig = FontConfig(), glyphRanges: Array<IntRange> = emptyArray()): Font =
            addFontFromMemoryTTF(ByteArray(fontData.size) { fontData[it].b }, sizePixels, fontCfg, glyphRanges)

    /** @param compressedFontData still owned by caller. Compress with binary_to_compressed_c.cpp.   */
    fun addFontFromMemoryCompressedTTF(compressedFontData: CharArray, sizePixels: Float, fontCfg: FontConfig = FontConfig(), glyphRanges: Array<IntRange> = emptyArray()): Font {
        val bufDecompressedData = stb.decompress(compressedFontData)
        assert(fontCfg.fontData.isEmpty())
        fontCfg.fontDataOwnedByAtlas = true
        return addFontFromMemoryTTF(bufDecompressedData, sizePixels, fontCfg, glyphRanges)
    }

    /** @param compressedFontDataBase85 still owned by caller. Compress with binary_to_compressed_c.cpp with -base85
     *  paramaeter  */
    fun addFontFromMemoryCompressedBase85TTF(compressedFontDataBase85: String, sizePixels: Float, fontCfg: FontConfig = FontConfig(), glyphRanges: Array<IntRange> = emptyArray()): Font {
        val compressedTtf = decode85(compressedFontDataBase85)
        return addFontFromMemoryCompressedTTF(compressedTtf, sizePixels, fontCfg, glyphRanges)
    }

    /** Clear input data (all FontConfig structures including sizes, TTF data, glyph ranges, etc.) = all the data used
     *  to build the texture and fonts. */
    fun clearInputData() {
        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        for (data in configData)
            if (data.fontData.isNotEmpty() && data.fontDataOwnedByAtlas)
                data.fontData = byteArrayOf()

        // When clearing this we lose access to the font name and other information used to build the font.
        for (font in fonts)
            if (font.configData.getOrNull(0) in configData) {
                font.configData.clear()
                font.configDataCount = 0
            }
        configData.clear()
        customRects.clear()
        packIdMouseCursors = -1; packIdLines = -1
        // Important: we leave TexReady untouched
    }

    /** Clear output texture data (CPU side). Saves RAM once the texture has been copied to graphics memory. */
    fun clearTexData() {
        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        texPixelsAlpha8?.free()
        texPixelsAlpha8 = null
        texPixelsRGBA32?.free()
        texPixelsRGBA32 = null
        texPixelsUseColors = false
        // Important: we leave TexReady untouched
    }

    /** Clear output font data (glyphs storage, UV coordinates).    */
    fun clearFonts() {
        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        for (font in fonts) font.clearOutputData()
        fonts.clear()
        texReady = false
    }

    /** Clear all input and output. ~ destroy  */
    fun clear() {
        clearInputData()
        clearTexData()
        clearFonts()
        texPixelsAlpha8?.free()
    }

    /*  Build atlas, retrieve pixel data.
        User is in charge of copying the pixels into graphics memory (e.g. create a texture with your engine).
        Then store your texture handle with setTexID().ClearInputData
        The pitch is always = Width * BytesPerPixels (1 or 4)
        Building in RGBA32 format is provided for convenience and compatibility, but note that unless
        you manually manipulate or copy color data into the texture (e.g. when using the AddCustomRect*** api),
        then the RGB pixels emitted will always be white (~75% of memory/bandwidth waste.  */

    /** Build pixels data. This is called automatically for you by the GetTexData*** functions.    */
    fun build(): Boolean {
        assert(!locked) { "Cannot modify a locked ImFontAtlas between NewFrame() and EndFrame/Render()!" }

        // Default font is none are specified
        if (configData.isEmpty())
            addFontDefault()

        // Select builder
        // - Note that we do not reassign to atlas->FontBuilderIO, since it is likely to point to static data which
        //   may mess with some hot-reloading schemes. If you need to assign to this (for dynamic selection) AND are
        //   using a hot-reloading scheme that messes up static data, store your own instance of ImFontBuilderIO somewhere
        //   and point to it instead of pointing directly to return value of the GetBuilderXXX functions.
        val builderIo = fontBuilderIO ?:
        //            #ifdef IMGUI_ENABLE_FREETYPE
        // TODO builderIo = ImGuiFreeType::GetBuilderForFreeType()
        //            #elif defined(IMGUI_ENABLE_STB_TRUETYPE)
        getBuilderForStbTruetype()
        //            #else
        //            IM_ASSERT(0) // Invalid Build function

        // Build
        return builderIo.build()
    }

    /** 1 byte per-pixel    */
    fun getTexDataAsAlpha8(): Triple<ByteBuffer, Vec2i, Int> {

        // Build atlas on demand
        if (texPixelsAlpha8 == null)
            build()

        return Triple(texPixelsAlpha8!!, texSize, 1)
    }

    /** 4 bytes-per-pixel   */
    fun getTexDataAsRGBA32(): Triple<ByteBuffer, Vec2i, Int> {
        /*  Convert to RGBA32 format on demand
            Although it is likely to be the most commonly used format, our font rendering is 1 channel / 8 bpp         */
        if (texPixelsRGBA32 == null) {
            val (pixels, _, _) = getTexDataAsAlpha8()
            if (pixels.isNotEmpty()) {
                texPixelsRGBA32 = Buffer(texSize.x * texSize.y * 4)
                val dst = texPixelsRGBA32!!
                for (n in 0 until pixels.rem) {
                    dst[n * 4] = 255.b
                    dst[n * 4 + 1] = 255.b
                    dst[n * 4 + 2] = 255.b
                    dst[n * 4 + 3] = pixels[n]
                }
            }
        }

        return Triple(texPixelsRGBA32!!, texSize, 4)
    }

    val isBuilt: Boolean // Bit ambiguous: used to detect when user didn't build texture but effectively we should check TexID != 0 except that would be backend dependent...
        get() = fonts.size > 0 && texReady

    //-------------------------------------------
    // [BETA] Custom Rectangles/Glyphs API
    //-------------------------------------------

    /** You can request arbitrary rectangles to be packed into the atlas, for your own purposes.
     *  - After calling Build(), you can query the rectangle position and render your pixels.
     *  - If you render colored output, set 'atlas->TexPixelsUseColors = true' as this may help some backends decide of prefered texture format.
     *  - You can also request your rectangles to be mapped as font glyph (given a font + Unicode point),
     *  so you can render e.g. custom colorful icons and use them as regular glyphs.
     *  - Read docs/FONTS.txt for more details about using colorful icons.
     *  - Note: this API may be redesigned later in order to support multi-monitor varying DPI settings. */
    class CustomRect {

        /** Input    // Desired rectangle dimension */
        var width = 0

        /** Input    // Desired rectangle dimension */
        var height = 0

        /** Output   // Packed position in Atlas  */
        var x = 0xFFFF

        /** Output   // Packed position in Atlas  */
        var y = 0xFFFF

        /** Input, For custom font glyphs only (ID < 0x110000) */
        var glyphID = 0

        /** Input, For custom font glyphs only: glyph xadvance */
        var glyphAdvanceX = 0f

        /** Input, For custom font glyphs only: glyph display offset */
        val glyphOffset = Vec2()

        /** Input, For custom font glyphs only: target font */
        var font: Font? = null

        val isPacked: Boolean
            get() = x != 0xFFFF

        constructor()

        constructor(r: CustomRect) {
            width = r.width
            height = r.height
            x = r.x
            y = r.y
            glyphID = r.glyphID
            glyphAdvanceX = r.glyphAdvanceX
            glyphOffset put r.glyphOffset
            font = r.font
        }
    }

    fun addCustomRectRegular(width: Int, height: Int): Int {
        assert(width in 0..0xFFFF && height in 0..0xFFFF)
        val r = CustomRect()
        r.width = width
        r.height = height
        customRects += r
        return customRects.lastIndex
    }

    fun addCustomRectFontGlyph(font: Font, id: Int, width: Int, height: Int, advanceX: Float, offset: Vec2 = Vec2()): Int {
        //        #ifdef IMGUI_USE_WCHAR32
        //                IM_ASSERT(id <= IM_UNICODE_CODEPOINT_MAX);
        //        #endif
        //        IM_ASSERT(font != NULL);
        assert(width in 1..0xFFFF)
        assert(height in 1..0xFFFF)
        val r = CustomRect()
        r.width = width
        r.height = height
        r.glyphID = id
        r.glyphAdvanceX = advanceX
        r.glyphOffset put offset
        r.font = font
        customRects.add(r)
        return customRects.lastIndex // Return index
    }

    fun getCustomRectByIndex(index: Int): CustomRect {
        assert(index >= 0)
        return customRects[index]
    }

    // Internals

    fun calcCustomRectUV(rect: CustomRect, outUvMin: Vec2, outUvMax: Vec2) {
        assert(texSize allGreaterThan 0) { "Font atlas needs to be built before we can calculate UV coordinates" }
        assert(rect.isPacked) { "Make sure the rectangle has been packed" }
        outUvMin.put(rect.x.f * texUvScale.x, rect.y.f * texUvScale.y)
        outUvMax.put((rect.x + rect.width).f * texUvScale.x, (rect.y + rect.height).f * texUvScale.y)
    }

    fun getMouseCursorTexData(cursor: MouseCursor, outOffset: Vec2, outSize: Vec2, outUv: Array<Vec2>): Boolean {

        if (cursor == MouseCursor.None) return false

        if (flags has Flag.NoMouseCursors) return false

        assert(packIdMouseCursors != -1)
        val r = getCustomRectByIndex(packIdMouseCursors)
        val pos = DefaultTexData.cursorDatas[cursor.i][0] + Vec2(r.x, r.y)
        val size = DefaultTexData.cursorDatas[cursor.i][1]
        outSize put size
        outOffset put DefaultTexData.cursorDatas[cursor.i][2]
        // JVM border
        outUv[0] = pos * texUvScale
        outUv[1] = (pos + size) * texUvScale
        pos.x += DefaultTexData.w + 1
        // JVM fill
        outUv[2] = pos * texUvScale
        outUv[3] = (pos + size) * texUvScale
        return true
    }

    //-------------------------------------------
    // Members
    //-------------------------------------------

    /** Flags: for ImFontAtlas build */
    sealed class Flag : FlagBase<Flag>() {
        /** Don't round the height to next power of two */
        object NoPowerOfTwoHeight : Flag()

        /** Don't build software mouse cursors into the atlas (save a little texture memory) */
        object NoMouseCursors : Flag()

        /** Don't build thick line textures into the atlas (save a little texture memory, allow support for point/nearest filtering). The AntiAliasedLinesUseTex features uses them, otherwise they will be rendered using polygons (more expensive for CPU/GPU). */
        object NoBakedLines : Flag()

        override val i: Int = 1 shl ordinal

        @GenSealedEnum
        companion object
    }

    /** Build flags (see ImFontAtlasFlags_) */
    var flags: FontAtlasFlags = none

    /** User data to refer to the texture once it has been uploaded to user's graphic systems. It is passed back to you
    during rendering via the DrawCmd structure.   */
    var texID: TextureID = 0

    /** Texture width desired by user before Build(). Must be a power-of-two. If have many glyphs your graphics API have
     *  texture size restrictions you may want to increase texture width to decrease height.    */
    var texDesiredWidth = 0

    /** Padding between glyphs within texture in pixels. Defaults to 1. If your rendering method doesn't rely on bilinear filtering you may set this to 0 (will also need to set AntiAliasedLinesUseTex = false). */
    var texGlyphPadding = 1

    /** Marked as Locked by ImGui::NewFrame() so attempt to modify the atlas will assert. */
    var locked = false

    /** Store your own atlas related user-data (if e.g. you have multiple font atlas). */
    var userData: Any? = null


    // [Internal]
    // NB: Access texture data via GetTexData*() calls! Which will setup a default font for you.

    /** Set when texture was built matching current font input */
    var texReady = false

    /** Tell whether our texture data is known to use colors (rather than just alpha channel), in order to help backend select a format. */
    var texPixelsUseColors = false

    /** 1 component per pixel, each component is unsigned 8-bit. Total size = texSize.x * texSize.y  */
    var texPixelsAlpha8: ByteBuffer? = null

    /** 4 component per pixel, each component is unsigned 8-bit. Total size = texSize.x * texSize.y * 4  */
    var texPixelsRGBA32: ByteBuffer? = null

    /** Texture size calculated during Build(). */
    var texSize = Vec2i()
    val texWidth get() = texSize.x

    val texHeight get() = texSize.y


    /** = (1.0f/TexWidth, 1.0f/TexHeight)   */
    var texUvScale = Vec2()

    /** Texture coordinates to a white pixel    */
    var texUvWhitePixel = Vec2()

    /** Hold all the fonts returned by AddFont*. Fonts[0] is the default font upon calling ImGui::NewFrame(), use
     *  ImGui::PushFont()/PopFont() to change the current font. */
    val fonts = ArrayList<Font>()

    /** Rectangles for packing custom texture data into the atlas.  */
    private val customRects = ArrayList<CustomRect>()

    /** Configuration data */
    private val configData = ArrayList<FontConfig>()

    /** UVs for baked anti-aliased lines */
    val texUvLines = Array(DRAWLIST_TEX_LINES_WIDTH_MAX + 1) { Vec4() }

    // [Internal] Font builder

    /** Opaque interface to a font builder (default to stb_truetype, can be changed to use FreeType by defining IMGUI_ENABLE_FREETYPE). */
    var fontBuilderIO: FontBuilderIO? = null

    /** Shared flags (for all fonts) for custom font builder. THIS IS BUILD IMPLEMENTATION DEPENDENT. Per-font override is also available in ImFontConfig. */
    var fontBuilderFlags = 0

    // [Internal] Packing data

    /** Custom texture rectangle ID for white pixel and mouse cursors */
    private var packIdMouseCursors = -1

    /** Custom texture rectangle ID for baked anti-aliased lines */
    private var packIdLines = -1


    private fun customRectCalcUV(rect: CustomRect, outUvMin: Vec2, outUvMax: Vec2) {
        assert(texSize.x > 0 && texSize.y > 0) { "Font atlas needs to be built before we can calculate UV coordinates" }
        assert(rect.isPacked) { "Make sure the rectangle has been packed " }
        outUvMin.put(rect.x / texSize.x, rect.y / texSize.y)
        outUvMax.put((rect.x + rect.width) / texSize.x, (rect.y + rect.height) / texSize.y)
    }


    // Helper: ImBitArray

    /** Helper: ImBoolVector. Store 1-bit per value.
     *  Note that Resize() currently clears the whole vector. */
    class BitVector(sz: Int) { // ~create
        var storage = IntArray((sz + 31) ushr 5)

        // Helpers: Bit arrays
        infix fun IntArray.testBit(n: Int): Boolean {
            val mask = 1 shl (n and 31); return (this[n ushr 5] and mask).bool; }

        infix fun IntArray.clearBit(n: Int) {
            val mask = 1 shl (n and 31); this[n ushr 5] = this[n ushr 5] wo mask; }

        infix fun IntArray.setBit(n: Int) {
            val mask = 1 shl (n and 31); this[n ushr 5] = this[n ushr 5] or mask; }

        fun IntArray.setBitRange(n_: Int, n2: Int) {
            var n = n_
            while (n <= n2) {
                val aMod = n and 31
                val bMod = (if (n2 > (n or 31)) 31 else n2 and 31) + 1
                val mask = ((1L shl bMod) - 1).toUInt() wo ((1L shl aMod) - 1).toUInt()
                this[n ushr 5] = this[n ushr 5] or mask
                n = (n + 32) wo 31
            }
        }

        fun clear() {
            storage = IntArray(0)
        }

        infix fun testBit(n: Int): Boolean {; assert(n < storage.size shl 5); return storage testBit n; }

        infix fun setBit(n: Int) {; assert(n < storage.size shl 5); storage setBit n; }

        infix fun clearBit(n: Int) {; assert(n < storage.size shl 5); storage clearBit n; }

        fun unpack(): ArrayList<Int> {
            val res = arrayListOf<Int>()
            storage.forEachIndexed { index, entries32 ->
                if (entries32 != 0)
                    for (bitN in 0..31)
                        if (entries32 has (1 shl bitN))
                            res += (index shl 5) + bitN
            }
            return res
        }
    }

    /** Temporary data for one source font (multiple source fonts can be merged into one destination ImFont)
     *  (C++03 doesn't allow instancing ImVector<> with function-local types so we declare the type here.) */
    class FontBuildSrcData {

        val fontInfo = FontInfo()

        /** Hold the list of codepoints to pack (essentially points to Codepoints.Data) */
        val packRange = PackRange()

        /** Rectangle to pack. We first fill in their size and the packer will give us their position. */
        var rects: Array<rectpack.Rect> = emptyArray()

        /** Output glyphs */
        var packedChars: Array<PackedChar> = emptyArray()

        /** Ranges as requested by user (user is allowed to request too much, e.g. 0x0020..0xFFFF) */
        lateinit var srcRanges: Array<IntRange>

        /** Index into atlas->Fonts[] and dst_tmp_array[] */
        var dstIndex = 0

        /** Highest requested codepoint */
        var glyphsHighest = 0

        /** Glyph count (excluding missing glyphs and glyphs already set by an earlier source font) */
        var glyphsCount = 0

        /** Glyph bit map (random access, 1-bit per codepoint. This will be a maximum of 8KB) */
        lateinit var glyphsSet: BitVector

        /** Glyph codepoints list (flattened version of GlyphsSet) */
        lateinit var glyphsList: ArrayList<Int>

        fun free() {
//            fontInfo.free()
//            packRange.arrayOfUnicodeCodepoints?.free()
//            packRange.free()
            // dummies
            //            rects.free()
            //            packedChars.free()
        }
    }

    /** Temporary data for one destination ImFont* (multiple source fonts can be merged into one destination ImFont) */
    class FontBuildDstData {
        /** Number of source fonts targeting this destination font. */
        var srcCount = 0
        var glyphsHighest = 0
        var glyphsCount = 0

        /** This is used to resolve collision when multiple sources are merged into a same destination font. */
        var glyphsSet: BitVector? = null
    }

    //-----------------------------------------------------------------------------
    // [SECTION] ImFontAtlas internal API
    //-----------------------------------------------------------------------------

    // This structure is likely to evolve as we add support for incremental atlas updates
    /** Opaque interface to a font builder (stb_truetype or FreeType). */
    abstract inner class FontBuilderIO {
        abstract fun build(): Boolean
    }

    fun buildWithStbTrueType(): Boolean {

        assert(configData.isNotEmpty())

        buildInit()

        // Clear atlas
        texID = 0
        texSize put 0
        texUvScale put 0f
        texUvWhitePixel put 0f
        clearTexData()

        // Temporary storage for building
        val srcTmpArray = Array(configData.size) { FontBuildSrcData() }
        val dstTmpArray = Array(fonts.size) { FontBuildDstData() }

        // 1. Initialize font loading structure, check font data validity
        for (srcIdx in configData.indices) {

            val srcTmp = srcTmpArray[srcIdx]
            val cfg = configData[srcIdx]
            assert(cfg.dstFont?.isLoaded == false || cfg.dstFont?.containerAtlas === this)

            // Find index from cfg.DstFont (we allow the user to set cfg.DstFont. Also it makes casual debugging nicer than when storing indices)
            srcTmp.dstIndex = -1
            for (outputIdx in fonts.indices) {
                if (srcTmp.dstIndex != -1)
                    break
                if (cfg.dstFont == fonts[outputIdx])
                    srcTmp.dstIndex = outputIdx
            }
            if (srcTmp.dstIndex == -1) {
                System.err.println("cfg.DstFont not pointing within atlas->Fonts[] array?")
                return false
            }
            // Initialize helper structure for font loading and verify that the TTF/OTF data is correct
            val fontOffset = getFontOffsetForIndex(cfg.fontData.asUByteArray(), cfg.fontNo)
            assert(fontOffset >= 0) { "FontData is incorrect, or FontNo cannot be found." }
            if (!srcTmp.fontInfo.initFont(cfg.fontData.asUByteArray(), fontOffset))
                return false

            // Measure highest codepoints
            val dstTmp = dstTmpArray[srcTmp.dstIndex]
            srcTmp.srcRanges = cfg.glyphRanges.takeIf { it.isNotEmpty() } ?: glyphRanges.default
            for (srcRange in srcTmp.srcRanges) {
                // Check for valid range. This may also help detect *some* dangling pointers, because a common
                // user error is to setup ImFontConfig::GlyphRanges with a pointer to data that isn't persistent.
                assert(srcRange.first <= srcRange.last)
                srcTmp.glyphsHighest = srcTmp.glyphsHighest max srcRange.last
            }
            dstTmp.srcCount++
            dstTmp.glyphsHighest = dstTmp.glyphsHighest max srcTmp.glyphsHighest
        }

        // 2. For every requested codepoint, check for their presence in the font data, and handle redundancy or overlaps between source fonts to avoid unused glyphs.
        var totalGlyphsCount = 0
        for (srcIdx in srcTmpArray.indices) {
            val srcTmp = srcTmpArray[srcIdx]
            val dstTmp = dstTmpArray[srcTmp.dstIndex]
            srcTmp.glyphsSet = BitVector(srcTmp.glyphsHighest + 1)
            if (dstTmp.glyphsSet == null)
                dstTmp.glyphsSet = BitVector(dstTmp.glyphsHighest + 1)

            for (srcRange in srcTmp.srcRanges)
                for (codepoint in srcRange) {
                    if (dstTmp.glyphsSet!! testBit codepoint)   // Don't overwrite existing glyphs. We could make this an option for MergeMode (e.g. MergeOverwrite==true)
                        continue

                    if (srcTmp.fontInfo findGlyphIndex codepoint == 0)    // It is actually in the font?
                        continue

                    // Add to avail set/counters
                    srcTmp.glyphsCount++
                    dstTmp.glyphsCount++
                    srcTmp.glyphsSet setBit codepoint
                    dstTmp.glyphsSet!! setBit codepoint
                    totalGlyphsCount++
                }
        }

        // 3. Unpack our bit map into a flat list (we now have all the Unicode points that we know are requested _and_ available _and_ not overlapping another)
        for (srcIdx in srcTmpArray.indices) {
            val srcTmp = srcTmpArray[srcIdx]
            srcTmp.glyphsList = srcTmp.glyphsSet.unpack()
            srcTmp.glyphsSet.clear()
            assert(srcTmp.glyphsList.size == srcTmp.glyphsCount)
        }
        dstTmpArray.forEach { it.glyphsSet!!.clear() }
        //        dstTmpArray.clear()

        // Allocate packing character data and flag packed characters buffer as non-packed (x0=y0=x1=y1=0)
        // (We technically don't need to zero-clear buf_rects, but let's do it for the sake of sanity)
        val bufRects = Array(totalGlyphsCount) { rectpack.Rect().apply { id = it } }
        val bufPackedchars = Array(totalGlyphsCount) { PackedChar() }

        // 4. Gather glyphs sizes so we can pack them in our virtual canvas.
        var totalSurface = 0
        var bufRectsOutN = 0
        var bufPackedcharsOutN = 0
        for (srcIdx in srcTmpArray.indices) {
            val srcTmp = srcTmpArray[srcIdx]
            if (srcTmp.glyphsCount == 0)
                continue

            srcTmp.rects = bufRects.copyOfRange(bufRectsOutN, bufRectsOutN + srcTmp.glyphsCount)
            srcTmp.packedChars = bufPackedchars.copyOfRange(bufPackedcharsOutN, bufPackedcharsOutN + srcTmp.glyphsCount)
            bufRectsOutN += srcTmp.glyphsCount
            bufPackedcharsOutN += srcTmp.glyphsCount

            // Convert our ranges in the format stb_truetype wants
            val cfg = configData[srcIdx]
            srcTmp.packRange.apply {
                fontSize = cfg.sizePixels
                firstUnicodeCodepointInRange = 0
                arrayOfUnicodeCodepoints = srcTmp.glyphsList.toIntArray()
                numChars = srcTmp.glyphsList.size
                chardataForRange = srcTmp.packedChars
                hOversample = cfg.oversample.x.ui
                vOversample = cfg.oversample.y.ui
            }
            // Gather the sizes of all rectangles we will need to pack (this loop is based on stbtt_PackFontRangesGatherRects)
            val scale = when {
                cfg.sizePixels > 0 -> srcTmp.fontInfo scaleForPixelHeight cfg.sizePixels
                else -> srcTmp.fontInfo scaleForMappingEmToPixels -cfg.sizePixels
            }
            val padding = texGlyphPadding
            for (glyphIdx in srcTmp.glyphsList.indices) {
                val glyphIndexInFont = srcTmp.fontInfo findGlyphIndex srcTmp.glyphsList[glyphIdx]
                assert(glyphIndexInFont != 0)
                val (x0, y0, x1, y1) = srcTmp.fontInfo.getGlyphBitmapBoxSubpixel(glyphIndexInFont, scale * cfg.oversample.x, scale * cfg.oversample.y)
                srcTmp.rects[glyphIdx].apply {
                    w = x1 - x0 + padding + cfg.oversample.x - 1
                    h = y1 - y0 + padding + cfg.oversample.y - 1
                    totalSurface += w * h
                }
            }
        }

        // We need a width for the skyline algorithm, any width!
        // The exact width doesn't really matter much, but some API/GPU have texture size limitations and increasing width can decrease height.
        // User can override TexDesiredWidth and TexGlyphPadding if they wish, otherwise we use a simple heuristic to select the width based on expected surface.
        texSize.put(x = when {
            texDesiredWidth > 0 -> texDesiredWidth
            else -> {
                val surfaceSqrt = sqrt(totalSurface.f).i + 1
                when {
                    surfaceSqrt >= 4096 * 0.7f -> 4096
                    else -> when {
                        surfaceSqrt >= 2048 * 0.7f -> 2048
                        else -> when {
                            surfaceSqrt >= 1024 * 0.7f -> 1024
                            else -> 512
                        }
                    }
                }
            }
        }, y = 0)

        // 5. Start packing
        // Pack our extra data rectangles first, so it will be on the upper-left corner of our texture (UV will have small values).
        val TEX_HEIGHT_MAX = 1024 * 32
        val spc = PackContext(null, texSize.x, TEX_HEIGHT_MAX, 0, texGlyphPadding)
        buildPackCustomRects(spc.packInfo)

        // 6. Pack each source font. No rendering yet, we are working with rectangles in an infinitely tall texture at this point.
        for (srcTmp in srcTmpArray) {
            if (srcTmp.glyphsCount == 0)
                continue

            spc.packInfo.packRects(srcTmp.rects)

            // Extend texture height and mark missing glyphs as non-packed so we won't render them.
            // FIXME: We are not handling packing failure here (would happen if we got off TEX_HEIGHT_MAX or if a single if larger than TexWidth?)
            for (glyphIdx in 0..<srcTmp.glyphsCount)
                if (srcTmp.rects[glyphIdx].wasPacked != 0)
                    texSize.y = texSize.y max (srcTmp.rects[glyphIdx].y + srcTmp.rects[glyphIdx].h)
        }

        // 7. Allocate texture
        texSize.y = when {
            flags has Flag.NoPowerOfTwoHeight -> texSize.y + 1
            else -> texSize.y.upperPowerOfTwo
        }
        texUvScale = 1f / Vec2(texSize)
//        texPixelsAlpha8 = Buffer(texSize.x * texSize.y)
        val pixels = UByteArray(texSize.x * texSize.y)
        spc.pixels = pixels
        spc.height = texSize.y
        spc.width = texSize.x

        // 8. Render/rasterize font characters into the texture
        for (srcIdx in srcTmpArray.indices) {
            val cfg = configData[srcIdx]
            val srcTmp = srcTmpArray[srcIdx]
            if (srcTmp.glyphsCount == 0)
                continue

            spc.packFontRangesRenderIntoRects(srcTmp.fontInfo, arrayOf(srcTmp.packRange), srcTmp.rects)

            // Apply multiply operator
            if (cfg.rasterizerMultiply != 1f) {
                val multiplyTable = buildMultiplyCalcLookupTable(cfg.rasterizerMultiply)
                for (glyphIdx in 0 until srcTmp.glyphsCount) {
                    val r = srcTmp.rects[glyphIdx]
                    if (r.wasPacked != 0)
                        buildMultiplyRectAlpha8(multiplyTable, pixels, r, texSize.x)
                }
            }
            //            srcTmp.rects = NULL // JVM dont free, it's a dummy container custom offset'ed
        }


        // End packing
//        STBTruetype.stbtt_PackEnd(spc)
//        spc.free()
//        bufRects.free()

        // 9. Setup ImFont and glyphs for runtime
        for (srcIdx in srcTmpArray.indices) {

            // When merging fonts with MergeMode=true:
            // - We can have multiple input fonts writing into a same destination font.
            // - dst_font->ConfigData is != from cfg which is our source configuration.
            val srcTmp = srcTmpArray[srcIdx]
            val cfg = configData[srcIdx]
            val dstFont = cfg.dstFont!!

            val fontScale = srcTmp.fontInfo scaleForPixelHeight cfg.sizePixels
            val (unscaledAscent, unscaledDescent, _) = srcTmp.fontInfo.fontVMetrics

            val ascent = floor(unscaledAscent * fontScale + if (unscaledAscent > 0f) +1 else -1)
            val descent = floor(unscaledDescent * fontScale + if (unscaledDescent > 0f) +1 else -1)
            buildSetupFont(dstFont, cfg, ascent, descent)
            val fontOff = Vec2(cfg.glyphOffset).apply { y += round(dstFont.ascent) }

            for (glyphIdx in 0 until srcTmp.glyphsCount) {
                // Register glyph
                val codepoint = srcTmp.glyphsList[glyphIdx]
                val pc = srcTmp.packedChars[glyphIdx]
                val q = AlignedQuad()
                getPackedQuad(srcTmp.packedChars, texSize.x, texSize.y, glyphIdx, q = q)
                dstFont.addGlyph(cfg, codepoint, q.x0 + fontOff.x, q.y0 + fontOff.y,
                        q.x1 + fontOff.x, q.y1 + fontOff.y, q.s0, q.t0, q.s1, q.t1, pc.xAdvance)
            }
        }
//        bufPackedchars.free()

        // Cleanup temporary (ImVector doesn't honor destructor)
        srcTmpArray.forEach { it.free() }
//        q.free()

        texPixelsAlpha8 = Buffer(texSize.x * texSize.y) { spc.pixels!![it].toByte() }

        buildFinish()
        return true
    }

    // Helper for font builder
    fun getBuilderForStbTruetype(): FontBuilderIO = object : FontBuilderIO() {
        override fun build(): Boolean = buildWithStbTrueType()
    }

    /** ~ImFontAtlasBuildInit
     *  Note: this is called / shared by both the stb_truetype and the FreeType builder */
    fun buildInit() {
        // Register texture region for mouse cursors or standard white pixels
        if (packIdMouseCursors < 0)
            packIdMouseCursors = when {
                flags hasnt Flag.NoMouseCursors -> addCustomRectRegular(DefaultTexData.w * 2 + 1, DefaultTexData.h)
                else -> addCustomRectRegular(2, 2)
            }

        // Register texture region for thick lines
        // The +2 here is to give space for the end caps, whilst height +1 is to accommodate the fact we have
        // a zero-width row
        if (packIdLines < 0)
            if (flags hasnt Flag.NoBakedLines)
                packIdLines = addCustomRectRegular(DRAWLIST_TEX_LINES_WIDTH_MAX + 2, DRAWLIST_TEX_LINES_WIDTH_MAX + 1)
    }

    fun buildSetupFont(font: Font, fontConfig: FontConfig, ascent: Float, descent: Float) {
        if (!fontConfig.mergeMode)
            font.apply {
                containerAtlas = this@FontAtlas
                configData += fontConfig
                configDataCount = 0
                fontSize = fontConfig.sizePixels
                this.ascent = ascent
                this.descent = descent
                glyphs.clear()
                metricsTotalSurface = 0
            }
        font.configDataCount++
    }

    /** ~ ImFontAtlasBuildPackCustomRects */
    fun buildPackCustomRects(stbrpContext: Context) {

        val userRects = customRects
        // We expect at least the default custom rects to be registered, else something went wrong.
        assert(userRects.isNotEmpty())
        val packRects = Array(userRects.size) {
            rectpack.Rect().apply {
                w = userRects[it].width
                h = userRects[it].height
            }
        }
        stbrpContext.packRects(packRects)
        for (i in userRects.indices)
            if (packRects[i].wasPacked != 0) {
                userRects[i].x = packRects[i].x
                userRects[i].y = packRects[i].y
                assert(packRects[i].w == userRects[i].width && packRects[i].h == userRects[i].height)
                texSize.y = glm.max(texSize.y, packRects[i].y + packRects[i].h)
            }
    }

    /** ~ImFontAtlasBuildFinish
     *
     *  This is called/shared by both the stb_truetype and the FreeType builder. */
    fun buildFinish() {
        // Render into our custom data blocks
        assert(texPixelsAlpha8 != null || texPixelsRGBA32 != null)
        buildRenderDefaultTexData()
        buildRenderLinesTexData(this)

        // Register custom rectangle glyphs
        for (r in customRects) {
            val font = r.font
            if (font == null || r.glyphID == 0)
                continue

            // Will ignore ImFontConfig settings: GlyphMinAdvanceX, GlyphMinAdvanceY, GlyphExtraSpacing, PixelSnapH
            assert(font.containerAtlas === this)
            val uv0 = Vec2()
            val uv1 = Vec2()
            calcCustomRectUV(r, uv0, uv1)
            font.addGlyph(null, r.glyphID, r.glyphOffset.x, r.glyphOffset.y, r.glyphOffset.x + r.width, r.glyphOffset.y + r.height,
                    uv0.x, uv0.y, uv1.x, uv1.y, r.glyphAdvanceX)
        }
        // Build all fonts lookup tables
        fonts.filter { it.dirtyLookupTables }.forEach { it.buildLookupTable() }

        texReady = true
    }

    fun buildRender8bppRectFromString(x: Int, y: Int, w: Int, h: Int, inStr: CharArray,
                                      inMarkerChar: Char, inMarkerPixelValue: Byte) {
        assert(x >= 0 && x + w <= texWidth)
        assert(y >= 0 && y + h <= texHeight)
        val outPixel = texPixelsAlpha8!!
        var ptr = x + y * texSize.x
        var ptr2 = 0
        var offY = 0
        while (offY < h) {
            for (offX in 0 until w)
                outPixel[ptr + offX] = if (inStr[ptr2 + offX] == inMarkerChar) inMarkerPixelValue else 0x00
            offY++
            ptr += texSize.x
            ptr2 += w
        }
    }

    fun buildRender32bppRectFromString(x: Int, y: Int, w: Int, h: Int, str: CharArray, /*pStr_: Int,*/
                                       inMarkerChar: Char, inMarkerPixelValue: Int) {
        assert(x >= 0 && x + w <= texWidth)
        assert(y >= 0 && y + h <= texHeight)
        var pStr = 0 //pStr_
        fun inStr(i: Int) = str[pStr + i]
        var outPixel = Ptr<Int>(texPixelsRGBA32!!.adr.L + x + (y * texWidth))
        var offY = 0
        while (offY < h) {
            for (offX in 0 until w)
                outPixel[offX] = if (inStr(offX) == inMarkerChar) inMarkerPixelValue else COL32_BLACK_TRANS
            offY++
            outPixel += texWidth
            pStr += w
        }
    }

    /** ~ImFontAtlasBuildRenderDefaultTexData */
    fun buildRenderDefaultTexData() {

        val r = getCustomRectByIndex(packIdMouseCursors)
        assert(r.isPacked)

        val w = texSize.x
        if (flags hasnt Flag.NoMouseCursors) {
            // Render/copy pixels
            assert(r.width == DefaultTexData.w * 2 + 1 && r.height == DefaultTexData.h)
            val xForWhite = r.x
            val xForBlack = r.x + DefaultTexData.w + 1
            texPixelsAlpha8?.run {
                buildRender8bppRectFromString(xForWhite, r.y, DefaultTexData.w, DefaultTexData.h, DefaultTexData.pixels, '.', 0xFF.b)
                buildRender8bppRectFromString(xForBlack, r.y, DefaultTexData.w, DefaultTexData.h, DefaultTexData.pixels, 'X', 0xFF.b)
            } ?: run {
                buildRender32bppRectFromString(xForWhite, r.y, DefaultTexData.w, DefaultTexData.h, DefaultTexData.pixels, '.', COL32_WHITE)
                buildRender32bppRectFromString(xForBlack, r.y, DefaultTexData.w, DefaultTexData.h, DefaultTexData.pixels, 'X', COL32_WHITE)
            }
        } else {
            // Render 4 white pixels
            assert(r.width == 2 && r.height == 2)
            val offset = r.x.i + r.y.i * w
            with(texPixelsAlpha8!!) {
                put(offset + w + 1, 0xFF.b)
                put(offset + w, 0xFF.b)
                put(offset + 1, 0xFF.b)
                put(offset, 0xFF.b)
            }
        }
        texUvWhitePixel = (Vec2(r.x, r.y) + 0.5f) * texUvScale
    }

    fun buildMultiplyCalcLookupTable(inBrightenFactor: Float) = CharArray(256) {
        val value = (it * inBrightenFactor).i
        (if (value > 255) 255 else (value and 0xFF)).c
    }

    fun buildMultiplyRectAlpha8(table: CharArray, pixels: UByteArray, rect: rectpack.Rect, stride: Int) {
        check(rect.w <= stride)
        var data = rect.x + rect.y * stride
        var j = rect.h
        while (j > 0) {
            var i = rect.w
            while (i > 0) {
                pixels[data] = table[pixels[data].i].code.ub
                i--; data++
            }
            j--; data += stride - rect.w
        }
    }

    companion object {

        fun buildRenderLinesTexData(atlas: FontAtlas) {

            if (atlas.flags has Flag.NoBakedLines)
                return

            // This generates a triangular shape in the texture, with the various line widths stacked on top of each other to allow interpolation between them
            val r = atlas.getCustomRectByIndex(atlas.packIdLines)
            assert(r.isPacked)
            for (n in 0 until DRAWLIST_TEX_LINES_WIDTH_MAX + 1) { // +1 because of the zero-width row { // +1 because of the zero-width row
                // Each line consists of at least two empty pixels at the ends, with a line of solid pixels in the middle
                val y = n
                val lineWidth = n
                val padLeft = (r.width - lineWidth) / 2
                val padRight = r.width - (padLeft + lineWidth)

                // Write each slice
                assert(padLeft + lineWidth + padRight == r.width && y < r.height) { "Make sure we're inside the texture bounds before we start writing pixels" }
                atlas.texPixelsAlpha8?.let {
                    val writePtr = Ptr<Byte>(it.adr.L + r.x + ((r.y + y) * atlas.texWidth))
                    for (i in 0 until padLeft)
                        writePtr[i] = 0x00

                    for (i in 0 until lineWidth)
                        writePtr[padLeft + i] = 0xFF.b

                    for (i in 0 until padRight)
                        writePtr[padLeft + lineWidth + i] = 0x00
                } ?: run {
                    val writePtr = Ptr<Int>(atlas.texPixelsRGBA32!!.adr.L + r.x + (r.y + y) * atlas.texWidth)
                    for (i in 0 until padLeft)
                        writePtr[i] = COL32(255, 255, 255, 0)

                    for (i in 0 until lineWidth)
                        writePtr[padLeft + i] = COL32_WHITE

                    for (i in 0 until padRight)
                        writePtr[padLeft + lineWidth + i] = COL32(255, 255, 255, 0)
                }

                // Calculate UVs for this line
                val uv0 = Vec2(r.x + padLeft - 1, r.y + y) * atlas.texUvScale
                val uv1 = Vec2(r.x + padLeft + lineWidth + 1, r.y + y + 1) * atlas.texUvScale
                val halfV = (uv0.y + uv1.y) * 0.5f // Calculate a constant V in the middle of the row to avoid sampling artifacts
                atlas.texUvLines[n].put(uv0.x, halfV, uv1.x, halfV)
            }
        }
    }

    // A work of art lies ahead! (. = white layer, X = black layer, others are blank)
    // The 2x2 white texels on the top left are the ones we'll use everywhere in Dear ImGui to render filled shapes.
    object DefaultTexData {
        /** ~FONT_ATLAS_DEFAULT_TEX_DATA_W */
        val w = 122 // Actual texture will be 2 times that + 1 spacing.

        /** ~FONT_ATLAS_DEFAULT_TEX_DATA_H */
        val h = 27
        val pixels = run {
            val s = StringBuilder()
            s += "..-         -XXXXXXX-    X    -           X           -XXXXXXX          -          XXXXXXX-     XX          - XX       XX "
            s += "..-         -X.....X-   X.X   -          X.X          -X.....X          -          X.....X-    X..X         -X..X     X..X"
            s += "---         -XXX.XXX-  X...X  -         X...X         -X....X           -           X....X-    X..X         -X...X   X...X"
            s += "X           -  X.X  - X.....X -        X.....X        -X...X            -            X...X-    X..X         - X...X X...X "
            s += "XX          -  X.X  -X.......X-       X.......X       -X..X.X           -           X.X..X-    X..X         -  X...X...X  "
            s += "X.X         -  X.X  -XXXX.XXXX-       XXXX.XXXX       -X.X X.X          -          X.X X.X-    X..XXX       -   X.....X   "
            s += "X..X        -  X.X  -   X.X   -          X.X          -XX   X.X         -         X.X   XX-    X..X..XXX    -    X...X    "
            s += "X...X       -  X.X  -   X.X   -    XX    X.X    XX    -      X.X        -        X.X      -    X..X..X..XX  -     X.X     "
            s += "X....X      -  X.X  -   X.X   -   X.X    X.X    X.X   -       X.X       -       X.X       -    X..X..X..X.X -    X...X    "
            s += "X.....X     -  X.X  -   X.X   -  X..X    X.X    X..X  -        X.X      -      X.X        -XXX X..X..X..X..X-   X.....X   "
            s += "X......X    -  X.X  -   X.X   - X...XXXXXX.XXXXXX...X -         X.X   XX-XX   X.X         -X..XX........X..X-  X...X...X  "
            s += "X.......X   -  X.X  -   X.X   -X.....................X-          X.X X.X-X.X X.X          -X...X...........X- X...X X...X "
            s += "X........X  -  X.X  -   X.X   - X...XXXXXX.XXXXXX...X -           X.X..X-X..X.X           - X..............X-X...X   X...X"
            s += "X.........X -XXX.XXX-   X.X   -  X..X    X.X    X..X  -            X...X-X...X            -  X.............X-X..X     X..X"
            s += "X..........X-X.....X-   X.X   -   X.X    X.X    X.X   -           X....X-X....X           -  X.............X- XX       XX "
            s += "X......XXXXX-XXXXXXX-   X.X   -    XX    X.X    XX    -          X.....X-X.....X          -   X............X--------------"
            s += "X...X..X    ---------   X.X   -          X.X          -          XXXXXXX-XXXXXXX          -   X...........X -             "
            s += "X..X X..X   -       -XXXX.XXXX-       XXXX.XXXX       -------------------------------------    X..........X -             "
            s += "X.X  X..X   -       -X.......X-       X.......X       -    XX           XX    -           -    X..........X -             "
            s += "XX    X..X  -       - X.....X -        X.....X        -   X.X           X.X   -           -     X........X  -             "
            s += "      X..X  -       -  X...X  -         X...X         -  X..X           X..X  -           -     X........X  -             "
            s += "       XX   -       -   X.X   -          X.X          - X...XXXXXXXXXXXXX...X -           -     XXXXXXXXXX  -             "
            s += "-------------       -    X    -           X           -X.....................X-           -------------------             "
            s += "                    ----------------------------------- X...XXXXXXXXXXXXX...X -                                           "
            s += "                                                      -  X..X           X..X  -                                           "
            s += "                                                      -   X.X           X.X   -                                           "
            s += "                                                      -    XX           XX    -                                           "
            s.toString().toCharArray()
        }

        val cursorDatas = arrayOf(
                // Pos ........ Size ......... Offset ......
                arrayOf(Vec2(0, 3), Vec2(12, 19), Vec2(0)),         // MouseCursor.Arrow
                arrayOf(Vec2(13, 0), Vec2(7, 16), Vec2(1, 8)),   // MouseCursor.TextInput
                arrayOf(Vec2(31, 0), Vec2(23), Vec2(11)),              // MouseCursor.Move
                arrayOf(Vec2(21, 0), Vec2(9, 23), Vec2(4, 11)),  // MouseCursor.ResizeNS
                arrayOf(Vec2(55, 18), Vec2(23, 9), Vec2(11, 4)), // MouseCursor.ResizeEW
                arrayOf(Vec2(73, 0), Vec2(17), Vec2(8)),               // MouseCursor.ResizeNESW
                arrayOf(Vec2(55, 0), Vec2(17), Vec2(8)),               // MouseCursor.ResizeNWSE
                arrayOf(Vec2(91, 0), Vec2(17, 22), Vec2(5, 0)),  // ImGuiMouseCursor_Hand
                arrayOf(Vec2(109, 0), Vec2(13, 15), Vec2(6, 7))) // ImGuiMouseCursor_NotAllowed
    }
}