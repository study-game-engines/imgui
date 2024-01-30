package imgui

import glm_.i
import glm_.vec2.Vec2
import imgui.api.*
import imgui.api.dragAndDrop
import imgui.api.loggingCapture
import imgui.internal.api.*
import imgui.internal.api.focusActivation

const val IMGUI_BUILD: Int = 0
const val IMGUI_VERSION: String = "1.89.7-1"

/** get the compiled version string e.g. "1.80 WIP" (essentially the value for IMGUI_VERSION from the compiled version of imgui.cpp) */
const val IMGUI_VERSION_BUILD: String = "$IMGUI_VERSION.$IMGUI_BUILD"
const val IMGUI_VERSION_NUM: Int = 18971
const val USE_BGRA_PACKED_COLOR: Boolean = false
val COL32_R_SHIFT: Int = lazy { if (USE_BGRA_PACKED_COLOR) 16 else 0 }.value
val COL32_G_SHIFT: Int = 8
val COL32_B_SHIFT: Int = lazy { if (USE_BGRA_PACKED_COLOR) 0 else 16 }.value
const val COL32_A_SHIFT: Int = 24
val COL32_A_MASK: Int = 0xFF000000.i
val COL32_WHITE: Int = COL32(255) // Opaque white = 0xFFFFFFFF
val COL32_BLACK: Int = COL32(0, 0, 0, 255)       // Opaque black
val COL32_BLACK_TRANS: Int = COL32(0, 0, 0, 0)   // Transparent black = 0x00000000
const val MOUSE_INVALID: Float = -256000f

fun COL32(value: Int): Int = COL32(value, value, value, value)
fun COL32(r: Int, g: Int, b: Int, a: Int): Int = (a shl COL32_A_SHIFT) or (b shl COL32_B_SHIFT) or (g shl COL32_G_SHIFT) or (r shl COL32_R_SHIFT)

/*debug options*/

const val IMGUI_DEBUG_NAV_SCORING: Boolean = false // Display navigation scoring preview when hovering items. Display last moving direction matches when holding CTRL
const val IMGUI_DEBUG_NAV_RECTS: Boolean = false // Display the reference navigation rectangle for each window
const val NAV_WINDOWING_HIGHLIGHT_DELAY: Float = 0.2f // Time before the highlight and screen dimming starts fading in
const val NAV_WINDOWING_LIST_APPEAR_DELAY: Float = 0.15f // Time before the window list starts to appear
const val WINDOWS_HOVER_PADDING: Float = 4f // Extend outside window for hovering/resizing (maxxed with TouchPadding) and inside windows for borders. Affect FindHoveredWindow().
const val WINDOWS_RESIZE_FROM_EDGES_FEEDBACK_TIMER: Float = 0.04f // Reduce visual noise by only highlighting the border after a certain time.
const val WINDOWS_MOUSE_WHEEL_SCROLL_LOCK_TIMER: Float = 0.7f // Lock scrolled window (so it doesn't pick child windows that are scrolling through) for a certain time, unless mouse moved.
val TOOLTIP_DEFAULT_OFFSET: Vec2 = Vec2(16, 10)            // Multiplied by g.Style.MouseCursorScale
const val IMGUI_ENABLE_TEST_ENGINE: Boolean = true // Test engine hooks (imgui-test)
const val IMGUI_DEBUG_TOOL_ITEM_PICKER_EX: Boolean = false
const val IMGUI_DISABLE_DEBUG_TOOLS: Boolean = false
const val UNICODE_CODEPOINT_MAX: Int = 0xFFFF // Last Unicode code point supported by this build.
const val UNICODE_CODEPOINT_INVALID: Int = 0xFFFD // Standard invalid Unicode code point.
const val MINECRAFT_BEHAVIORS: Boolean = false

object ImGui :
        main,
        demoDebugInformations,
        styles,
        imgui.api.windows,
        childWindows,
        windowsUtilities,
        contentRegion,
        windowScrolling,
        parametersStacks,
        styleReadAccess,
        cursorLayout,
        idStackScopes,
        imgui.api.viewports,
        widgetsText,
        widgetsMain,
        widgetsImages,
        widgetsComboBox,
        widgetsDrags,
        widgetsSliders,
        widgetsInputWithKeyboard,
        widgetsColorEditorPicker,
        widgetsTrees,
        widgetsSelectables,
        widgetsListBoxes,
        widgetsDataPlotting,
        widgetsMenus,
        tooltips,
        popupsModals,
        tables,
        columns,
        tabBarsTabs,
        loggingCapture,
        dragAndDrop,
        clipping,
        imgui.api.focusActivation,
        overlappingMode,
        itemWidgetsUtilities,
        backgroundForegroundDrawLists,
        miscellaneousUtilities,
        textUtilities,
        colorUtilities,
        inputsUtilitiesKeyboardMouseGamepad,
        inputsUtilitiesShortcutRouting,
        inputUtilitiesMouse,
        clipboardUtilities,
        settingsIniUtilities,
        debugUtilities,

        /*internals without forward compatibility*/
        imgui.internal.api.windows,
        windowsDisplayAndFocusOrder,
        fontsDrawing,
        newFrame,
        genericContextHooks,
        imgui.internal.api.viewports,
        settings,
        settingsWindows,
        localization,
        scrolling,
        basicAccessors,
        basicHelpersForWidgetCode,
        parameterStacks,
        imgui.internal.api.loggingCapture,
        popupsModalsTooltips,
        menus,
        combos,
        gamepadKeyboardNavigation,
        focusActivation,
        inputs,
        focusScope,
        imgui.internal.api.dragAndDrop,
        disabling,
        internalColumnsAPI,
        tablesCandidatesForPublicAPI,
        tablesInternal,
        tableSettings,
        tabBars,
        renderHelpers,
        widgets,
        widgetsWindowDecorations,
        widgetsLowLevelBehaviors,
        templateFunctions,
        inputText,
        color,
        shadeFunctions,
        garbageCollection,
        debugLog,
        debugTools


const val DEBUG: Boolean = false

fun IM_DEBUG_BREAK() {}