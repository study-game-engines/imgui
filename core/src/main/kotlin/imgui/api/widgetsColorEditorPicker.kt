package imgui.api

import glm_.func.cos
import glm_.func.sin
import glm_.glm
import glm_.i
import glm_.max
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import glm_.wo
import imgui.*
import imgui.ImGui.acceptDragDropPayload
import imgui.ImGui.beginDragDropSource
import imgui.ImGui.beginDragDropTarget
import imgui.ImGui.beginGroup
import imgui.ImGui.beginPopup
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.colorConvertHSVtoRGB
import imgui.ImGui.colorConvertRGBtoHSV
import imgui.ImGui.colorEdit4
import imgui.ImGui.colorEditOptionsPopup
import imgui.ImGui.colorPickerOptionsPopup
import imgui.ImGui.colorTooltip
import imgui.ImGui.currentWindow
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.endDragDropSource
import imgui.ImGui.endDragDropTarget
import imgui.ImGui.endGroup
import imgui.ImGui.endPopup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.fontTexUvWhitePixel
import imgui.ImGui.frameHeight
import imgui.ImGui.getColorU32
import imgui.ImGui.hsvToRGB
import imgui.ImGui.inputText
import imgui.ImGui.invisibleButton
import imgui.ImGui.io
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemHovered
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.markItemEdited
import imgui.ImGui.openPopup
import imgui.ImGui.openPopupOnItemClick
import imgui.ImGui.popID
import imgui.ImGui.popItemFlag
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushID
import imgui.ImGui.pushItemFlag
import imgui.ImGui.pushItemWidth
import imgui.ImGui.renderArrowPointingAt
import imgui.ImGui.renderColorRectWithAlphaCheckerboard
import imgui.ImGui.renderFrameBorder
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.rgbToHSV
import imgui.ImGui.sameLine
import imgui.ImGui.scanHex
import imgui.ImGui.setDragDropPayload
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.shadeVertsLinearColorGradientKeepAlpha
import imgui.ImGui.spacing
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textEx
import imgui.api.widgetsColorEditorPicker.Companion.colorEditRestoreH
import imgui.api.widgetsColorEditorPicker.Companion.colorEditRestoreHS
import imgui.api.widgetsColorEditorPicker.Companion.fmtTableFloat
import imgui.api.widgetsColorEditorPicker.Companion.fmtTableInt
import imgui.api.widgetsColorEditorPicker.Companion.ids
import imgui.api.widgetsColorEditorPicker.Companion.renderArrowsForVerticalBar
import imgui.classes.DrawList
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.sections.DrawFlag
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.ItemStatusFlag
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf


/** Widgets: Color Editor/Picker (tip: the ColorEdit* functions have a little color square that can be
 *  left-clicked to open a picker, and right-clicked to open an option menu.)
 *  - Note that in C++ a 'float v[X]' function argument is the _same_ as 'float* v', the array syntax is just a way to
 *      document the number of elements that are expected to be accessible.
 *  - You can pass the address of a first float element out of a contiguous structure, e.g. &myvector.x  */
interface widgetsColorEditorPicker {
    /** 3-4 components color edition. Click on colored squared to open a color picker, right-click for options.
     *  Hint: 'float col[3]' function argument is same as 'float* col'.
     *  You can pass address of first element out of a contiguous set, e.g. &myvector.x */
    fun colorEdit3(label: String, col: Vec3, flags: ColorEditFlags = none): Boolean = colorEdit4(label, col.x, col.y, col.z, 0f, flags or Cef.NoAlpha, col::put)

    /** 3-4 components color edition. Click on colored squared to open a color picker, right-click for options.
     *  Hint: 'float col[3]' function argument is same as 'float* col'.
     *  You can pass address of first element out of a contiguous set, e.g. &myvector.x */
    fun colorEdit3(label: String, col: Vec4, flags: ColorEditFlags = none): Boolean = colorEdit4(label, col.x, col.y, col.z, col.w, flags or Cef.NoAlpha, col::put)

    /** Edit colors components (each component in 0.0f..1.0f range).
     *  See enum ImGuiColorEditFlags_ for available options. e.g. Only access 3 floats if ColorEditFlags.NoAlpha flag is set.
     *  With typical options: Left-click on color square to open color picker. Right-click to open option menu.
     *  CTRL-Click over input fields to edit them and TAB to go to next item.   */

    fun String.scanHex(ints: IntArray, precision: Int, count: Int = ints.size) {
        var c = 0
        for (i in 0 until count) {
            val end = glm.min((i + 1) * precision, length)
            if (c >= end) break
            ints[i] = substring(c, end).toInt(16)
            c += precision
        }
    }

    fun colorEdit4(label: String, col: Vec3, flags: ColorEditFlags = none): Boolean = colorEdit4(label, col.x, col.y, col.z, 1f, flags, col::put)

    fun colorEdit4(label: String, col: Vec4, flags: ColorEditFlags = none): Boolean = colorEdit4(label, col.x, col.y, col.z, col.w, flags, col::put)

    fun colorPicker3(label: String, col: Vec3, flags: ColorEditFlags = none): Boolean = colorPicker3(label, col.x, col.y, col.z, flags, col::put)

    fun colorPicker3(label: String, col: Vec4, flags: ColorEditFlags = none): Boolean = colorPicker3(label, col.x, col.y, col.z, flags, col::put)

    /** ColorPicker
     *  Note: only access 3 floats if ImGuiColorEditFlags_NoAlpha flag is set.
     *  (In C++ the 'float col[4]' notation for a function argument is equivalent to 'float* col', we only specify a size to facilitate understanding of the code.)
     *  FIXME: we adjust the big color square height based on item width, which may cause a flickering feedback loop
     *  (if automatic height makes a vertical scrollbar appears, affecting automatic width..)
     *  FIXME: this is trying to be aware of style.Alpha but not fully correct. Also, the color wheel will have overlapping glitches with (style.Alpha < 1.0)   */
    fun colorPicker4(label: String, col: Vec3, flags: ColorEditFlags = none, refCol: Vec4?): Boolean =
            colorPicker4(label, col.x, col.y, col.z, 1f, flags, refCol, col::put)

    /** ColorPicker
     *  Note: only access 3 floats if ImGuiColorEditFlags_NoAlpha flag is set.
     *  (In C++ the 'float col[4]' notation for a function argument is equivalent to 'float* col', we only specify a size to facilitate understanding of the code.)
     *  FIXME: we adjust the big color square height based on item width, which may cause a flickering feedback loop
     *  (if automatic height makes a vertical scrollbar appears, affecting automatic width..)
     *  FIXME: this is trying to be aware of style.Alpha but not fully correct. Also, the color wheel will have overlapping glitches with (style.Alpha < 1.0)   */
    fun colorPicker4(label: String, col: Vec4, flags: ColorEditFlags = none, refCol: Vec4? = null): Boolean =
            colorPicker4(label, col.x, col.y, col.z, col.w, flags, refCol, col::put)

    fun colorButton(descId: String, col: Vec3, flags: ColorEditFlags = none, sizeArg: Vec2 = Vec2()): Boolean = colorButton(descId, col.x, col.y, col.z, 1f, flags, sizeArg)

    fun colorButton(descId: String, col: Vec4, flags: ColorEditFlags = none, sizeArg: Vec2 = Vec2()): Boolean = colorButton(descId, col.x, col.y, col.z, col.w, flags, sizeArg)

    /** initialize current options (generally on application startup) if you want to select a default format, picker
     *  type, etc. User will be able to change many settings, unless you pass the _NoOptions flag to your calls.    */
    fun setColorEditOptions(flags_: ColorEditFlags) {
        var flags = flags_
        if (flags hasnt Cef._DisplayMask) flags /= Cef.DefaultOptions and Cef._DisplayMask
        if (flags hasnt Cef._DataTypeMask) flags /= Cef.DefaultOptions and Cef._DataTypeMask
        if (flags hasnt Cef._PickerMask) flags /= Cef.DefaultOptions and Cef._PickerMask
        if (flags hasnt Cef._InputMask) flags /= Cef.DefaultOptions and Cef._InputMask
        assert((flags and Cef._DisplayMask).isPowerOfTwo) { "Check only 1 option is selected" }
        assert((flags and Cef._DataTypeMask).isPowerOfTwo) { "Check only 1 option is selected" }
        assert((flags and Cef._PickerMask).isPowerOfTwo) { "Check only 1 option is selected" }
        assert((flags and Cef._InputMask).isPowerOfTwo) { "Check only 1 option is selected" }
        g.colorEditOptions = flags
    }

    companion object {
        val ids = arrayOf("##X", "##Y", "##Z", "##W")
        val fmtTableInt = arrayOf(arrayOf("%3d", "%3d", "%3d", "%3d"),             // Short display
                arrayOf("R:%3d", "G:%3d", "B:%3d", "A:%3d"),     // Long display for RGBA
                arrayOf("H:%3d", "S:%3d", "V:%3d", "A:%3d"))     // Long display for HSVA
        val fmtTableFloat = arrayOf(arrayOf("%.3f", "%.3f", "%.3f", "%.3f"),            // Short display
                arrayOf("R:%.3f", "G:%.3f", "B:%.3f", "A:%.3f"),    // Long display for RGBA
                arrayOf("H:%.3f", "S:%.3f", "V:%.3f", "A:%.3f"))    // Long display for HSVA

        fun DrawList.renderArrowsForVerticalBar(pos: Vec2, halfSz: Vec2, barW: Float, alpha: Float) {
            val alpha8 = F32_TO_INT8_SAT(alpha)
            renderArrowPointingAt(Vec2(pos.x + halfSz.x + 1, pos.y), Vec2(halfSz.x + 2, halfSz.y + 1), Dir.Right, COL32(0, 0, 0, alpha8))
            renderArrowPointingAt(Vec2(pos.x + halfSz.x, pos.y), halfSz, Dir.Right, COL32(255, 255, 255, alpha8))
            renderArrowPointingAt(Vec2(pos.x + barW - halfSz.x - 1, pos.y), Vec2(halfSz.x + 2, halfSz.y + 1), Dir.Left, COL32(0, 0, 0, alpha8))
            renderArrowPointingAt(Vec2(pos.x + barW - halfSz.x, pos.y), halfSz, Dir.Left, COL32(255, 255, 255, alpha8))
        }

        fun colorEditRestoreH(col: Vec4, pH: MutableProperty<Float>) {
            assert(g.colorEditCurrentID != 0)
            if (g.colorEditSavedID != g.colorEditCurrentID || g.colorEditSavedColor != floatsToU32(col[0], col[1], col[2], 0f))
                return
            var H by pH
            H = g.colorEditSavedHue
        }

        /** ColorEdit supports RGB and HSV inputs. In case of RGB input resulting color may have undefined hue and/or saturation.
         *  Since widget displays both RGB and HSV values we must preserve hue and saturation to prevent these values resetting. */
        fun colorEditRestoreHS(rgb: Vec3, hsv: Vec3) = colorEditRestoreHS(rgb.r, rgb.g, rgb.b, hsv.x, hsv.y, hsv.z, hsv::put)

        /** ColorEdit supports RGB and HSV inputs. In case of RGB input resulting color may have undefined hue and/or saturation.
         *  Since widget displays both RGB and HSV values we must preserve hue and saturation to prevent these values resetting. */
        fun colorEditRestoreHS(rgb: Vec4, hsv: Vec3) = colorEditRestoreHS(rgb.r, rgb.g, rgb.b, hsv.x, hsv.y, hsv.z, hsv::put)

        /** ColorEdit supports RGB and HSV inputs. In case of RGB input resulting color may have undefined hue and/or saturation.
         *  Since widget displays both RGB and HSV values we must preserve hue and saturation to prevent these values resetting. */
        fun colorEditRestoreHS(x: Float, y: Float, z: Float, hsv: Vec3) = colorEditRestoreHS(x, y, z, hsv.x, hsv.y, hsv.z, hsv::put)

        /** ColorEdit supports RGB and HSV inputs. In case of RGB input resulting color may have undefined hue and/or saturation.
         *  Since widget displays both RGB and HSV values we must preserve hue and saturation to prevent these values resetting. */
        fun colorEditRestoreHS(x: Float, y: Float, z: Float, hsv: Vec4) = colorEditRestoreHS(x, y, z, hsv.x, hsv.y, hsv.z, hsv::put)

        /** ColorEdit supports RGB and HSV inputs. In case of RGB input resulting color may have undefined hue and/or saturation.
         *  Since widget displays both RGB and HSV values we must preserve hue and saturation to prevent these values resetting. */
        fun colorEditRestoreHS(x: Float, y: Float, z: Float, h_: Float, s_: Float, v: Float, hsvSetter: Vec3Setter) {
            assert(g.colorEditCurrentID != 0)
            var h = h_
            var s = s_
            if (g.colorEditSavedID != g.colorEditCurrentID || g.colorEditSavedColor != floatsToU32(x, y, z, 0f))
                return

            // When s == 0, h is undefined.
            // When h == 1 it wraps around to 0.
            if (s == 0f || (h == 0f && g.colorEditSavedHue == 1f)) h = g.colorEditSavedHue

            // When v == 0, s is undefined.
            if (v == 0f) s = g.colorEditSavedSat
            hsvSetter(h, s, v)
        }
    }
}

inline fun colorEdit4(label: String, x: Float, y: Float, z: Float, w: Float, flags_: ColorEditFlags = none, colSetter: Vec4Setter): Boolean {
    val window = currentWindow
    if (window.skipItems) return false

    val squareSz = frameHeight
    val wFull = calcItemWidth()
    val wButton = if (flags_ has Cef.NoSmallPreview) 0f else squareSz + style.itemInnerSpacing.x
    val wInputs = wFull - wButton
    val labelDisplayEnd = findRenderedTextEnd(label)
    g.nextItemData.clearFlags()

    beginGroup()
    pushID(label)
    val setCurrentColorEditId = g.colorEditCurrentID == 0
    if (setCurrentColorEditId)
        g.colorEditCurrentID = window.idStack.last()

    var flags = flags_

    // If we're not showing any slider there's no point in doing any HSV conversions
    if (flags has Cef.NoInputs) flags = (flags wo Cef._DisplayMask) or Cef.DisplayRGB or Cef.NoOptions

    // Context menu: display and modify options (before defaults are applied)
    if (flags hasnt Cef.NoOptions) colorEditOptionsPopup(x, y, z, w, flags)

    // Read stored options
    if (flags hasnt Cef._DisplayMask) flags /= g.colorEditOptions and Cef._DisplayMask
    if (flags hasnt Cef._DataTypeMask) flags /= g.colorEditOptions and Cef._DataTypeMask
    if (flags hasnt Cef._PickerMask) flags /= g.colorEditOptions and Cef._PickerMask
    if (flags hasnt Cef._InputMask) flags /= g.colorEditOptions and Cef._InputMask
    flags /= g.colorEditOptions wo (Cef._DisplayMask or Cef._DataTypeMask or Cef._PickerMask or Cef._InputMask)
    assert((flags and Cef._DisplayMask).isPowerOfTwo) { "Check that only 1 is selected" }
    assert((flags and Cef._InputMask).isPowerOfTwo) { "Check that only 1 is selected" }

    val alpha = flags hasnt Cef.NoAlpha
    val hdr = flags has Cef.HDR
    val components = if (alpha) 4 else 3

    // Convert to the formats we need
    val f = Vec4(x, y, z, if (alpha) w else 1f)
    if (flags has Cef.InputHSV && flags has Cef.DisplayRGB) f.hsvToRGB()
    else if (flags has Cef.InputRGB && flags has Cef.DisplayHSV) {
        // Hue is lost when converting from grayscale rgb (saturation=0). Restore it.
        f.rgbToHSV()
        colorEditRestoreHS(x, y, z, f)
    }

    val i = IntArray(4) { F32_TO_INT8_UNBOUND(f[it]) }

    var valueChanged = false
    var valueChangedAsFloat = false

    val pos = Vec2(window.dc.cursorPos)
    val inputsOffsetX = if (style.colorButtonPosition == Dir.Left) wButton else 0f
    window.dc.cursorPos.x = pos.x + inputsOffsetX

    if (flags has (Cef.DisplayRGB or Cef.DisplayHSV) && flags hasnt Cef.NoInputs) {

        // RGB/HSV 0..255 Sliders
        val wItemOne = 1f max floor((wInputs - style.itemInnerSpacing.x * (components - 1)) / components)
        val wItemLast = 1f max floor(wInputs - (wItemOne + style.itemInnerSpacing.x) * (components - 1))

        val hidePrefix = wItemOne <= calcTextSize(if (flags has Cef.Float) "M:0.000" else "M:000").x
        val fmtIdx = if (hidePrefix) 0 else if (flags has Cef.DisplayHSV) 2 else 1

        repeat(components) { n ->
            if (n > 0) sameLine(0f, style.itemInnerSpacing.x)
            setNextItemWidth(if (n + 1 < components) wItemOne else wItemLast)

            // Disable Hue edit when Saturation is zero
            // FIXME: When ImGuiColorEditFlags_HDR flag is passed HS values snap in weird ways when SV values go below 0.
            if (flags has Cef.Float) {
                valueChanged /= drag(ids[n], f mutablePropertyAt n, 1f / 255f, 0f, if (hdr) 0f else 1f, fmtTableFloat[fmtIdx][n])
                valueChangedAsFloat /= valueChanged
            } else valueChanged /= drag(ids[n], i mutablePropertyAt n, 1f, 0, if (hdr) 0 else 255, fmtTableInt[fmtIdx][n])
            if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context", PopupFlag.MouseButtonRight)
        }

    } else if (flags has Cef.DisplayHEX && flags hasnt Cef.NoInputs) {
        // RGB Hexadecimal Input
        val buf = when {
            alpha -> "#%02X%02X%02X%02X".format(style.locale, glm.clamp(i[0], 0, 255), glm.clamp(i[1], 0, 255), glm.clamp(i[2], 0, 255), glm.clamp(i[3], 0, 255))

            else -> "#%02X%02X%02X".format(style.locale, glm.clamp(i[0], 0, 255), glm.clamp(i[1], 0, 255), glm.clamp(i[2], 0, 255))
        }.toByteArray(64)
        setNextItemWidth(wInputs)
        if (inputText("##Text", buf, Itf.CharsHexadecimal or Itf.CharsUppercase)) {
            valueChanged = true
            var p = 0
            val str = buf.cStr
            while (str[p] == '#' || str[p].isBlankA) p++
            i[0] = 0; i[1] = 0; i[2] = 0
            i[3] = 0xFF // alpha default to 255 is not parsed by scanf (e.g. inputting #FFFFFF omitting alpha)
            str.substring(p).scanHex(i, 2, if (alpha) 4 else 3)   // Treat at unsigned (%X is unsigned)
        }
        if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context", PopupFlag.MouseButtonRight)
    }

    var pickerActiveWindow: Window? = null
    if (flags hasnt Cef.NoSmallPreview) {
        val buttonOffsetX = when {
            flags has Cef.NoInputs || style.colorButtonPosition == Dir.Left -> 0f
            else -> wInputs + style.itemInnerSpacing.x
        }
        window.dc.cursorPos.put(pos.x + buttonOffsetX, pos.y)
        if (colorButton("##ColorButton", x, y, z, if (alpha) w else 1f, flags)) if (flags hasnt Cef.NoPicker) {
            // Store current color and open a picker
            g.colorPickerRef.put(x, y, z, if (alpha) w else 1f)
            openPopup("picker")
            setNextWindowPos(g.lastItemData.rect.bl + Vec2(0f, style.itemSpacing.y))
        }
        if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context", PopupFlag.MouseButtonRight)

        if (beginPopup("picker")) if (g.currentWindow!!.beginCount == 1) {
            pickerActiveWindow = g.currentWindow
            if (0 != labelDisplayEnd) {
                textEx(label, labelDisplayEnd)
                spacing()
            }
            val pickerFlagsToForward = Cef._DataTypeMask or Cef._PickerMask or Cef._InputMask or Cef.HDR or Cef.NoAlpha or Cef.AlphaBar
            val pickerFlags = (flags_ and pickerFlagsToForward) or Cef._DisplayMask or Cef._DisplayMask or Cef.NoLabel or Cef.AlphaPreviewHalf
            setNextItemWidth(squareSz * 12f)   // Use 256 + bar sizes?
            valueChanged /= colorPicker4("##picker", x, y, z, w, pickerFlags, g.colorPickerRef, colSetter)
            endPopup()
        }
    }

    if (0 != labelDisplayEnd && flags hasnt Cef.NoLabel) {
        // Position not necessarily next to last submitted button (e.g. if style.ColorButtonPosition == ImGuiDir_Left),
        // but we need to use SameLine() to setup baseline correctly. Might want to refactor SameLine() to simplify this.
        sameLine(0f, style.itemInnerSpacing.x)
        window.dc.cursorPos.x = pos.x + if (flags has Cef.NoInputs) wButton else wFull + style.itemInnerSpacing.x
        textEx(label, labelDisplayEnd)
    }

    // Convert back
    if (valueChanged && pickerActiveWindow == null) {
        if (!valueChangedAsFloat) for (n in 0..3) f[n] = i[n] / 255f
        if (flags has Cef.DisplayHSV && flags has Cef.InputRGB) {
            g.colorEditSavedHue = f[0]
            g.colorEditSavedSat = f[1]
            f.hsvToRGB()
            g.colorEditSavedID = g.colorEditCurrentID
            g.colorEditSavedColor = floatsToU32(f[0], f[1], f[2], 0f)
        }
        if (flags has Cef.DisplayRGB && flags has Cef.InputHSV) f.rgbToHSV()
        f.into(colSetter, if (alpha) f.w else w)
    }

    if (setCurrentColorEditId)
        g.colorEditCurrentID = 0
    popID()
    endGroup()

    // Drag and Drop Target
    // NB: The flag test is merely an optional micro-optimization, BeginDragDropTarget() does the same test.
    if (g.lastItemData.statusFlags has ItemStatusFlag.HoveredRect && beginDragDropTarget()) {
        acceptDragDropPayload(PAYLOAD_TYPE_COLOR_3F)?.let {
            val data = it.data!! as Vec4
            // Drag-drop payloads are always RGB
            if (flags has Cef.InputHSV) colorConvertRGBtoHSV(data) { h, s, v ->
                colSetter(h, s, v, w)
            } else data.into(colSetter, w)
            valueChanged = true
        }
        acceptDragDropPayload(PAYLOAD_TYPE_COLOR_4F)?.let {
            val data = it.data!! as Vec4
            val w = if (alpha) data.w else w
            // Drag-drop payloads are always RGB
            if (flags has Cef.InputHSV) colorConvertRGBtoHSV(data) { h, s, v ->
                colSetter(h, s, v, w)
            } else data.into(colSetter, w)
            valueChanged = true
        }

        endDragDropTarget()
    }

    // When picker is being actively used, use its active id so IsItemActive() will function on ColorEdit4().
    if (pickerActiveWindow != null && g.activeId != 0 && g.activeIdWindow === pickerActiveWindow) g.lastItemData.id = g.activeId

    if (valueChanged && g.lastItemData.id != 0) // In case of ID collision, the second EndGroup() won't catch g.ActiveId
        markItemEdited(g.lastItemData.id)

    return valueChanged
}

inline fun colorPicker3(label: String, r: Float, g: Float, b: Float, flags: ColorEditFlags = none, colSetter: Vec3Setter): Boolean = colorPicker4(label, r, g, b, 1f, flags or Cef.NoAlpha) { x, y, z, _ -> colSetter(x, y, z) }

/** ColorPicker
 *  Note: only access 3 floats if ImGuiColorEditFlags_NoAlpha flag is set.
 *  (In C++ the 'float col[4]' notation for a function argument is equivalent to 'float* col', we only specify a size to facilitate understanding of the code.)
 *  FIXME: we adjust the big color square height based on item width, which may cause a flickering feedback loop
 *  (if automatic height makes a vertical scrollbar appears, affecting automatic width..)
 *  FIXME: this is trying to be aware of style.Alpha but not fully correct. Also, the color wheel will have overlapping glitches with (style.Alpha < 1.0)   */
inline fun colorPicker4(label: String, x: Float, y: Float, z: Float, w: Float, flags: ColorEditFlags = none, refCol: Vec4? = null, colSetter: Vec4Setter = { _, _, _, _ -> }): Boolean {
    val col = Vec4(x, y, z, w)
    return colorPicker4(label, col, flags, refCol).also { col into colSetter }
}

/** ColorPicker
 *  Note: only access 3 floats if ImGuiColorEditFlags_NoAlpha flag is set.
 *  (In C++ the 'float col[4]' notation for a function argument is equivalent to 'float* col', we only specify a size to facilitate understanding of the code.)
 *  FIXME: we adjust the big color square height based on item width, which may cause a flickering feedback loop
 *  (if automatic height makes a vertical scrollbar appears, affecting automatic width..)
 *  FIXME: this is trying to be aware of style.Alpha but not fully correct. Also, the color wheel will have overlapping glitches with (style.Alpha < 1.0)   */
fun colorPicker4(label: String, col: Vec4, flags_: ColorEditFlags = none, refCol: Vec4? = null): Boolean {
    val window = currentWindow
    if (window.skipItems) return false

    val drawList = window.drawList

    val width = calcItemWidth()
    g.nextItemData.clearFlags()

    pushID(label)
    val setCurrentColorEditId = g.colorEditCurrentID == 0
    if (setCurrentColorEditId)
        g.colorEditCurrentID = window.idStack.last()
    beginGroup()

    var flags = flags_
    if (flags hasnt Cef.NoSidePreview) flags /= Cef.NoSmallPreview

    // Context menu: display and store options.
    if (flags hasnt Cef.NoOptions) colorPickerOptionsPopup(col, flags)

    // Read stored options
    if (flags hasnt Cef._PickerMask) flags /= (if (g.colorEditOptions has Cef._PickerMask) g.colorEditOptions else Cef.DefaultOptions) and Cef._PickerMask
    if (flags hasnt Cef._InputMask) flags /= (if (g.colorEditOptions has Cef._InputMask) g.colorEditOptions else Cef.DefaultOptions) and Cef._InputMask
    assert((flags and Cef._PickerMask).isPowerOfTwo) { "Check that only 1 is selected" }
    assert((flags and Cef._InputMask).isPowerOfTwo)  // Check that only 1 is selected
    if (flags hasnt Cef.NoOptions) flags /= g.colorEditOptions and Cef.AlphaBar

    // Setup
    val components = if (flags has Cef.NoAlpha) 3 else 4
    val alphaBar = flags has Cef.AlphaBar && flags hasnt Cef.NoAlpha
    val pickerPos = Vec2(window.dc.cursorPos)
    val squareSz = frameHeight
    val barsWidth = squareSz     // Arbitrary smallish width of Hue/Alpha picking bars
    // Saturation/Value picking box
    val svPickerSize = glm.max(barsWidth * 1, width - (if (alphaBar) 2 else 1) * (barsWidth + style.itemInnerSpacing.x))
    val bar0PosX = pickerPos.x + svPickerSize + style.itemInnerSpacing.x
    val bar1PosX = bar0PosX + barsWidth + style.itemInnerSpacing.x
    val barsTrianglesHalfSz = floor(barsWidth * 0.2f)

    val backupInitialCol = Vec4(col)

    val wheelThickness = svPickerSize * 0.08f
    val wheelROuter = svPickerSize * 0.50f
    val wheelRInner = wheelROuter - wheelThickness
    val wheelCenter = Vec2(pickerPos.x + (svPickerSize + barsWidth) * 0.5f, pickerPos.y + svPickerSize * 0.5f)

    // Note: the triangle is displayed rotated with trianglePa pointing to Hue, but most coordinates stays unrotated for logic.
    val triangleR = wheelRInner - (svPickerSize * 0.027f).i
    val trianglePa = Vec2(triangleR, 0f)   // Hue point.
    val trianglePb = Vec2(triangleR * -0.5f, triangleR * -0.866025f) // Black point.
    val trianglePc = Vec2(triangleR * -0.5f, triangleR * +0.866025f) // White point.

    val hsv = Vec3(col)
    val rgb = Vec3(col)
    if (flags has Cef.InputRGB) {
        // Hue is lost when converting from grayscale rgb (saturation=0). Restore it.
        colorConvertRGBtoHSV(rgb, hsv)
        colorEditRestoreHS(rgb, hsv)
    } else if (flags has Cef.InputHSV) colorConvertHSVtoRGB(hsv, rgb)
    var (H, S, V) = hsv
    var (R, G, B) = rgb // turn to capital as cpp to avoid clashing with ImGui `g`

    var valueChanged = false
    var valueChangedH = false
    var valueChangedSv = false

    pushItemFlag(ItemFlag.NoNav, true)
    if (flags has Cef.PickerHueWheel) {
        // Hue wheel + SV triangle logic
        invisibleButton("hsv", Vec2(svPickerSize + style.itemInnerSpacing.x + barsWidth, svPickerSize))
        if (isItemActive) {
            val initialOff = io.mouseClickedPos[0] - wheelCenter
            val currentOff = io.mousePos - wheelCenter
            val initialDist2 = initialOff.lengthSqr
            if (initialDist2 >= (wheelRInner - 1) * (wheelRInner - 1) && initialDist2 <= (wheelROuter + 1) * (wheelROuter + 1)) {
                // Interactive with Hue wheel
                H = glm.atan(currentOff.y, currentOff.x) / glm.PIf * 0.5f
                if (H < 0f) H += 1f
                valueChanged = true
                valueChangedH = true
            }
            val cosHueAngle = glm.cos(-H * 2f * glm.PIf)
            val sinHueAngle = glm.sin(-H * 2f * glm.PIf)
            if (triangleContainsPoint(trianglePa, trianglePb, trianglePc, initialOff.rotate(cosHueAngle, sinHueAngle))) {
                // Interacting with SV triangle
                val currentOffUnrotated = currentOff.rotate(cosHueAngle, sinHueAngle)
                if (!triangleContainsPoint(trianglePa, trianglePb, trianglePc, currentOffUnrotated)) {
                    currentOffUnrotated put triangleClosestPoint(trianglePa, trianglePb, trianglePc, currentOffUnrotated)
                }
                triangleBarycentricCoords(trianglePa, trianglePb, trianglePc, currentOffUnrotated) { uu, vv, _ ->
                    V = glm.clamp(1f - vv, 0.0001f, 1f)
                    S = glm.clamp(uu / V, 0.0001f, 1f)
                }
                valueChangedSv = true
                valueChanged = true
            }
        }
        if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context", PopupFlag.MouseButtonRight)

    } else if (flags has Cef.PickerHueBar) {
        // SV rectangle logic
        invisibleButton("sv", Vec2(svPickerSize))
        if (isItemActive) {
            S = saturate((io.mousePos.x - pickerPos.x) / (svPickerSize - 1))
            V = 1f - saturate((io.mousePos.y - pickerPos.y) / (svPickerSize - 1))

            // Greatly reduces hue jitter and reset to 0 when hue == 255 and color is rapidly modified using SV square.
            colorEditRestoreH(col, H.mutableReference) // Greatly reduces hue jitter and reset to 0 when hue == 255 and color is rapidly modified using SV square.
            if (g.colorEditSavedColor == Vec4(col[0], col[1], col[2], 0f).u32)
                H = g.colorEditSavedHue
            valueChangedSv = true; valueChanged = true
        }
        if (flags hasnt Cef.NoOptions) openPopupOnItemClick("context", PopupFlag.MouseButtonRight)
        // Hue bar logic
        cursorScreenPos = Vec2(bar0PosX, pickerPos.y)
        invisibleButton("hue", Vec2(barsWidth, svPickerSize))
        if (isItemActive) {
            H = saturate((io.mousePos.y - pickerPos.y) / (svPickerSize - 1))
            valueChangedH = true
            valueChanged = true
        }
    }

    // Alpha bar logic
    if (alphaBar) {
        cursorScreenPos = Vec2(bar1PosX, pickerPos.y)
        invisibleButton("alpha", Vec2(barsWidth, svPickerSize))
        if (isItemActive) {
            col[3] = 1f - saturate((io.mousePos.y - pickerPos.y) / (svPickerSize - 1))
            valueChanged = true
        }
    }
    popItemFlag() // ItemFlag.NoNav

    if (flags hasnt Cef.NoSidePreview) {
        sameLine(0f, style.itemInnerSpacing.x)
        beginGroup()
    }

    if (flags hasnt Cef.NoLabel) {
        val labelDisplayEnd = findRenderedTextEnd(label)
        if (0 != labelDisplayEnd) {
            if (flags has Cef.NoSidePreview) sameLine(0f, style.itemInnerSpacing.x)
            textEx(label, labelDisplayEnd)
        }
    }
    if (flags hasnt Cef.NoSidePreview) {
        pushItemFlag(ItemFlag.NoNavDefaultFocus, true)
        if (flags has Cef.NoLabel) text("Current")

        val subFlagsToForward = Cef._InputMask or Cef.HDR or Cef.AlphaPreview or Cef.AlphaPreviewHalf or Cef.NoTooltip
        colorButton("##current", col[0], col[1], col[2], if (flags has Cef.NoAlpha) 1f else col[3], flags and subFlagsToForward, Vec2(squareSz * 3, squareSz * 2))
        if (refCol != null) {
            text("Original")
            if (colorButton("##original", refCol[0], refCol[1], refCol[2], if (flags has Cef.NoAlpha) 1f else refCol[3], flags and subFlagsToForward, Vec2(squareSz * 3, squareSz * 2))) {
                repeat(components) { i -> col[i] = refCol[i] }
                valueChanged = true
            }
        }
        popItemFlag()
        endGroup()
    }

    // Convert back color to RGB
    if (valueChangedH || valueChangedSv) if (flags has Cef.InputRGB) {
        colorConvertHSVtoRGB(H, S, V, col)
        g.colorEditSavedHue = H
        g.colorEditSavedSat = S
        g.colorEditSavedID = g.colorEditCurrentID
        g.colorEditSavedColor = floatsToU32(col.x, col.y, col.z, 0f)
    } else if (flags has Cef.InputHSV) {
        col[0] = H
        col[1] = S
        col[2] = V
    }

    // R,G,B and H,S,V slider color editor
    var valueChangedFixHueWrap = false
    if (flags hasnt Cef.NoInputs) {
        pushItemWidth((if (alphaBar) bar1PosX else bar0PosX) + barsWidth - pickerPos.x)
        val subFlagsToForward = Cef._DataTypeMask or Cef._InputMask or Cef.HDR or Cef.NoAlpha or Cef.NoOptions or Cef.NoSmallPreview or Cef.AlphaPreview or Cef.AlphaPreviewHalf
        val subFlags = (flags and subFlagsToForward) or Cef.NoPicker
        if (flags has Cef.DisplayRGB || flags hasnt Cef._DisplayMask) if (colorEdit4("##rgb", col, subFlags or Cef.DisplayRGB)) {
            // FIXME: Hackily differentiating using the DragInt (ActiveId != 0 && !ActiveIdAllowOverlap) vs. using the InputText or DropTarget.
            // For the later we don't want to run the hue-wrap canceling code. If you are well versed in HSV picker please provide your input! (See #2050)
            valueChangedFixHueWrap = g.activeId != 0 && !g.activeIdAllowOverlap
            valueChanged = true
        }
        if (flags has Cef.DisplayHSV || flags hasnt Cef._DisplayMask) valueChanged /= colorEdit4("##hsv", col, subFlags or Cef.DisplayHSV)
        if (flags has Cef.DisplayHEX || flags hasnt Cef._DisplayMask) valueChanged /= colorEdit4("##hex", col, subFlags or Cef.DisplayHEX)
        popItemWidth()
    }

    // Try to cancel hue wrap (after ColorEdit4 call), if any
    if (valueChangedFixHueWrap && flags has Cef.InputRGB) {
        val (newH, newS, newV) = colorConvertRGBtoHSV(col)
        if (newH <= 0 && H > 0) {
            if (newV <= 0 && V != newV) colorConvertHSVtoRGB(H, S, V * 0.5f, col)
            else if (newS <= 0) colorConvertHSVtoRGB(H, S * 0.5f, newV, col)
        }
    }

    if (valueChanged) {
        if (flags has Cef.InputRGB) {
            R = col[0]
            G = col[1]
            B = col[2]
            colorConvertRGBtoHSV(R, G, B) { h, s, v ->
                H = h
                S = s
                V = v
            }
            hsv[0] = H; hsv[1] = S; hsv[2] = V
            colorEditRestoreHS(col, hsv) // Fix local Hue as display below will use it immediately.
            H = hsv[0]; S = hsv[1]; V = hsv[2]
        } else if (flags has Cef.InputHSV) {
            H = col[0]
            S = col[1]
            V = col[2]
            colorConvertHSVtoRGB(H, S, V) { r, g, b ->
                R = r
                G = g
                B = b
            }
        }
    }

    val styleAlpha8 = F32_TO_INT8_SAT(style.alpha)
    val colBlack = COL32(0, 0, 0, styleAlpha8)
    val colWhite = COL32(255, 255, 255, styleAlpha8)
    val colMidgrey = COL32(128, 128, 128, styleAlpha8)
    val colHues = arrayOf(COL32(255, 0, 0, styleAlpha8), COL32(255, 255, 0, styleAlpha8), COL32(0, 255, 0, styleAlpha8), COL32(0, 255, 255, styleAlpha8), COL32(0, 0, 255, styleAlpha8), COL32(255, 0, 255, styleAlpha8), COL32(255, 0, 0, styleAlpha8))

    val hueColorF = Vec4(1f, 1f, 1f, style.alpha); colorConvertHSVtoRGB(H, 1f, 1f, hueColorF::put)
    val hueColor32 = hueColorF.u32
    val userCol32StripedOfAlpha = floatsToU32(R, G, B, style.alpha) // Important: this is still including the main rendering/style alpha!!

    val svCursorPos = Vec2()

    if (flags has Cef.PickerHueWheel) {
        // Render Hue Wheel
        val aeps = 0.5f / wheelROuter   // Half a pixel arc length in radians (2pi cancels out).
        val segmentPerArc = glm.max(4, (wheelROuter / 12).i)
        for (n in 0..5) {
            val a0 = n / 6f * 2f * glm.PIf - aeps
            val a1 = (n + 1f) / 6f * 2f * glm.PIf + aeps
            val vertStartIdx = drawList.vtxBuffer.size
            drawList.pathArcTo(wheelCenter, (wheelRInner + wheelROuter) * 0.5f, a0, a1, segmentPerArc)
            drawList.pathStroke(colWhite, thickness = wheelThickness)
            val vertEndIdx = drawList.vtxBuffer.size

            // Paint colors over existing vertices
            val gradientP0 = Vec2(wheelCenter.x + a0.cos * wheelRInner, wheelCenter.y + a0.sin * wheelRInner)
            val gradientP1 = Vec2(wheelCenter.x + a1.cos * wheelRInner, wheelCenter.y + a1.sin * wheelRInner)
            drawList.shadeVertsLinearColorGradientKeepAlpha(vertStartIdx, vertEndIdx, gradientP0, gradientP1, colHues[n], colHues[n + 1])
        }

        // Render Cursor + preview on Hue Wheel
        val cosHueAngle = glm.cos(H * 2f * glm.PIf)
        val sinHueAngle = glm.sin(H * 2f * glm.PIf)
        val hueCursorPos = Vec2(wheelCenter.x + cosHueAngle * (wheelRInner + wheelROuter) * 0.5f, wheelCenter.y + sinHueAngle * (wheelRInner + wheelROuter) * 0.5f)
        val hueCursorRad = wheelThickness * if (valueChangedH) 0.65f else 0.55f
        val hueCursorSegments = drawList._calcCircleAutoSegmentCount(hueCursorRad) // Lock segment count so the +1 one matches others.
        drawList.addCircleFilled(hueCursorPos, hueCursorRad, hueColor32, hueCursorSegments)
        drawList.addCircle(hueCursorPos, hueCursorRad + 1, colMidgrey, hueCursorSegments)
        drawList.addCircle(hueCursorPos, hueCursorRad, colWhite, hueCursorSegments)

        // Render SV triangle (rotated according to hue)
        val tra = wheelCenter + trianglePa.rotate(cosHueAngle, sinHueAngle)
        val trb = wheelCenter + trianglePb.rotate(cosHueAngle, sinHueAngle)
        val trc = wheelCenter + trianglePc.rotate(cosHueAngle, sinHueAngle)
        val uvWhite = fontTexUvWhitePixel
        drawList.primReserve(3, 3)
        drawList.primVtx(tra, uvWhite, hueColor32)
        drawList.primVtx(trb, uvWhite, colBlack)
        drawList.primVtx(trc, uvWhite, colWhite)
        drawList.addTriangle(tra, trb, trc, colMidgrey, 1.5f)
        svCursorPos put trc.lerp(tra, saturate(S)).lerp(trb, saturate(1 - V))
    } else if (flags has Cef.PickerHueBar) {
        // Render SV Square
        drawList.addRectFilledMultiColor(pickerPos, pickerPos + svPickerSize, colWhite, hueColor32, hueColor32, colWhite)
        drawList.addRectFilledMultiColor(pickerPos, pickerPos + svPickerSize, 0, 0, colBlack, colBlack)
        renderFrameBorder(pickerPos, pickerPos + svPickerSize, 0f)
        // Sneakily prevent the circle to stick out too much
        svCursorPos.x = glm.clamp(floor(pickerPos.x + saturate(S) * svPickerSize + 0.5f), pickerPos.x + 2, pickerPos.x + svPickerSize - 2)
        svCursorPos.y = glm.clamp(floor(pickerPos.y + saturate(1 - V) * svPickerSize + 0.5f), pickerPos.y + 2, pickerPos.y + svPickerSize - 2)

        // Render Hue Bar
        for (i in 0..5) {
            val a = Vec2(bar0PosX, pickerPos.y + i * (svPickerSize / 6))
            val c = Vec2(bar0PosX + barsWidth, pickerPos.y + (i + 1) * (svPickerSize / 6))
            drawList.addRectFilledMultiColor(a, c, colHues[i], colHues[i], colHues[i + 1], colHues[i + 1])
        }
        val bar0LineY = round(pickerPos.y + H * svPickerSize)
        renderFrameBorder(Vec2(bar0PosX, pickerPos.y), Vec2(bar0PosX + barsWidth, pickerPos.y + svPickerSize), 0f)
        drawList.renderArrowsForVerticalBar(Vec2(bar0PosX - 1, bar0LineY), Vec2(barsTrianglesHalfSz + 1, barsTrianglesHalfSz), barsWidth + 2f, style.alpha)
    }

    // Render cursor/preview circle (clamp S/V within 0..1 range because floating points colors may lead HSV values to be out of range)
    val svCursorRad = if (valueChangedSv) 10f else 6f
    val svCursorSegments = drawList._calcCircleAutoSegmentCount(svCursorRad) // Lock segment count so the +1 one matches others.
    drawList.addCircleFilled(svCursorPos, svCursorRad, userCol32StripedOfAlpha, svCursorSegments)
    drawList.addCircle(svCursorPos, svCursorRad + 1, colMidgrey, svCursorSegments)
    drawList.addCircle(svCursorPos, svCursorRad, colWhite, svCursorSegments)

    // Render alpha bar
    if (alphaBar) {
        val alpha = saturate(col[3])
        val bar1Bb = Rect(bar1PosX, pickerPos.y, bar1PosX + barsWidth, pickerPos.y + svPickerSize)
        renderColorRectWithAlphaCheckerboard(drawList, bar1Bb.min, bar1Bb.max, 0, bar1Bb.width / 2f, Vec2())
        drawList.addRectFilledMultiColor(bar1Bb.min, bar1Bb.max, userCol32StripedOfAlpha, userCol32StripedOfAlpha, userCol32StripedOfAlpha wo COL32_A_MASK, userCol32StripedOfAlpha wo COL32_A_MASK)
        val bar1LineY = round(pickerPos.y + (1f - alpha) * svPickerSize)
        renderFrameBorder(bar1Bb.min, bar1Bb.max, 0f)
        drawList.renderArrowsForVerticalBar(Vec2(bar1PosX - 1, bar1LineY), Vec2(barsTrianglesHalfSz + 1, barsTrianglesHalfSz), barsWidth + 2f, style.alpha)
    }

    endGroup()

    var compare = true
    repeat(components) { if (backupInitialCol[it] != col[it]) compare = false }
    if (valueChanged && compare) valueChanged = false
    if (valueChanged && g.lastItemData.id != 0) // In case of ID collision, the second EndGroup() won't catch g.ActiveId
        markItemEdited(g.lastItemData.id)

    if (setCurrentColorEditId)
        g.colorEditCurrentID = 0
    popID()

    return valueChanged
}

/**  A little color square. Return true when clicked.
 *  FIXME: May want to display/ignore the alpha component in the color display? Yet show it in the tooltip.
 *  'desc_id' is not called 'label' because we don't display it next to the button, but only in the tooltip.
 *  Note that 'col' may be encoded in HSV if ImGuiColorEditFlags_InputHSV is set.   */
fun colorButton(descId: String, x: Float, y: Float, z: Float, w: Float, flags_: ColorEditFlags = none, sizeArg: Vec2 = Vec2()): Boolean {

    val window = currentWindow
    if (window.skipItems) return false

    val id = window.getID(descId)
    val defaultSize = frameHeight
    val size = Vec2(if (sizeArg.x == 0f) defaultSize else sizeArg.x, if (sizeArg.y == 0f) defaultSize else sizeArg.y)
    val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
    itemSize(bb, if (size.y >= defaultSize) style.framePadding.y else 0f)
    if (!itemAdd(bb, id)) return false

    val (pressed, hovered, _) = buttonBehavior(bb, id)

    var flags = flags_
    if (flags has Cef.NoAlpha) flags -= Cef.AlphaPreview or Cef.AlphaPreviewHalf

    val colRgb = Vec4(x, y, z, w)
    if (flags has Cef.InputHSV) colorConvertHSVtoRGB(colRgb)

    val colRgbWithoutAlpha = Vec4(colRgb.x, colRgb.y, colRgb.z, 1f)
    val gridStep = glm.min(size.x, size.y) / 2.99f
    val rounding = glm.min(style.frameRounding, gridStep * 0.5f)
    val bbInner = Rect(bb)
    var off = 0f
    if (flags hasnt Cef.NoBorder) {
        off = -0.75f // The border (using Col_FrameBg) tends to look off when color is near-opaque and rounding is enabled. This offset seemed like a good middle ground to reduce those artifacts.
        bbInner expand off
    }
    if (flags has Cef.AlphaPreviewHalf && colRgb.w < 1f) {
        val midX = round((bbInner.min.x + bbInner.max.x) * 0.5f)
        renderColorRectWithAlphaCheckerboard(window.drawList, Vec2(bbInner.min.x + gridStep, bbInner.min.y), bbInner.max, getColorU32(colRgb), gridStep, Vec2(-gridStep + off, off), rounding, DrawFlag.RoundCornersRight)
        window.drawList.addRectFilled(bbInner.min, Vec2(midX, bbInner.max.y), getColorU32(colRgbWithoutAlpha), rounding, DrawFlag.RoundCornersLeft)
    } else {/*  Because getColorU32() multiplies by the global style alpha and we don't want to display a checkerboard
            if the source code had no alpha */
        val colSource = if (flags has Cef.AlphaPreview) colRgb else colRgbWithoutAlpha
        if (colSource.w < 1f) renderColorRectWithAlphaCheckerboard(window.drawList, bbInner.min, bbInner.max, colSource.u32, gridStep, Vec2(off), rounding)
        else window.drawList.addRectFilled(bbInner.min, bbInner.max, getColorU32(colSource), rounding)
    }
    renderNavHighlight(bb, id)
    // Color button are often in need of some sort of border
    if (flags hasnt Cef.NoBorder) if (g.style.frameBorderSize > 0f) renderFrameBorder(bb.min, bb.max, rounding)
    else window.drawList.addRect(bb.min, bb.max, Col.FrameBg.u32, rounding)

    // Drag and Drop Source
    // NB: The ActiveId test is merely an optional micro-optimization, BeginDragDropSource() does the same test.
    if (g.activeId == id && flags hasnt Cef.NoDragDrop && beginDragDropSource()) {

        if (flags has Cef.NoAlpha) setDragDropPayload(PAYLOAD_TYPE_COLOR_3F, colRgb, Cond.Once)
        else setDragDropPayload(PAYLOAD_TYPE_COLOR_4F, colRgb, Cond.Once)
        colorButton(descId, x, y, z, w, flags, sizeArg)
        sameLine()
        textEx("Color")
        endDragDropSource()
    }
    // Tooltip
    if (flags hasnt Cef.NoTooltip && hovered && isItemHovered(HoveredFlag.ForTooltip))
        colorTooltip(descId, x, y, z, w, flags and (Cef._InputMask or Cef.NoAlpha or Cef.AlphaPreview or Cef.AlphaPreviewHalf))

    return pressed
}
