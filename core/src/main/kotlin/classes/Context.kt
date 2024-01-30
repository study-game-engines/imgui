package imgui.classes

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.addSettingsHandler
import imgui.ImGui.callHooks
import imgui.ImGui.localizeRegisterEntries
import imgui.ImGui.saveIniSettingsToDisk
import imgui.ImGui.tableSettingsAddSettingsHandler
import imgui.api.g
import imgui.api.gImGui
import imgui.api.gImGuiNullable
import imgui.font.Font
import imgui.font.FontAtlas
import imgui.internal.DrawChannel
import imgui.internal.classes.*
import imgui.internal.hashStr
import imgui.internal.sections.*
import imgui.statics.*
import org.lwjgl.system.Platform
import java.io.File
import java.nio.ByteBuffer
import java.util.*

class Context(sharedFontAtlas: FontAtlas? = null) {

    var initialized: Boolean = false
    var fontAtlasOwnedByContext: Boolean = sharedFontAtlas == null // Io.Fonts-> is owned by the ImGuiContext and will be destructed along with it.
    var io: IO = IO(sharedFontAtlas).apply { ctx = this@Context }
    var style: Style = Style()
    lateinit var font: Font
    var fontSize: Float = 0f // (Shortcut) == FontBaseSize * g.CurrentWindow->FontWindowScale == window->FontSize(). Text height for current window.
    var fontBaseSize: Float = 0f // (Shortcut) == IO.FontGlobalScale * Font->Scale * Font->FontSize. Base text height
    var drawListSharedData: DrawListSharedData = DrawListSharedData()
    var time: Double = 0.0
    var frameCount: Int = 0
    var frameCountEnded: Int = -1
    var frameCountRendered: Int = -1
    var withinFrameScope: Boolean = false  // Set by NewFrame(), cleared by EndFrame()
    var withinFrameScopeWithImplicitWindow: Boolean = false // Set by NewFrame(), cleared by EndFrame() when the implicit debug window has been pushed
    var withinEndChild: Boolean = false // Set within EndChild()
    var gcCompactAll: Boolean = false // Request full GC
    var testEngineHookItems: Boolean = false // Will call test engine hooks: ImGuiTestEngineHook_ItemAdd(), ImGuiTestEngineHook_ItemInfo(), ImGuiTestEngineHook_Log()
    val inputEventsQueue: ArrayList<InputEvent> = ArrayList<InputEvent>() // Input events which will be tricked/written into IO structure.
    val inputEventsTrail: ArrayList<InputEvent> = ArrayList<InputEvent>() // Past input events processed in NewFrame(). This is to allow domain-specific application to access e.g mouse/pen trail.
    var inputEventsNextMouseSource: MouseSource = MouseSource.Mouse
    var inputEventsNextEventId: UInt = 1u
    val windows: ArrayList<Window> = ArrayList<Window>() // Windows, sorted in display order, back to front
    val windowsFocusOrder: ArrayList<Window> = ArrayList<Window>() // Root windows, sorted in focus order, back to front.
    val windowsTempSortBuffer: ArrayList<Window> = ArrayList<Window>()
    val currentWindowStack: Stack<WindowStackData> = Stack<WindowStackData>()
    val windowsById: MutableMap<Int, Window> = mutableMapOf<Int, Window>() // Map window's ImGuiID to ImGuiWindow
    var windowsActiveCount: Int = 0 // Number of unique windows submitted by frame
    var windowsHoverPadding: Vec2 = Vec2() // Padding around resizable windows for which hovering on counts as hovering the window == ImMax(style.TouchExtraPadding, WINDOWS_HOVER_PADDING)
    var currentWindow: Window? = null // Window being drawn into
    var hoveredWindow: Window? = null // Window the mouse is hovering. Will typically catch mouse inputs
    var hoveredWindowUnderMovingWindow: Window? = null // Hovered window ignoring MovingWindow. Only set if MovingWindow is set
    var movingWindow: Window? = null // Track the window we clicked on (in order to preserve focus). The actual window that is moved is generally MovingWindow->RootWindow
    var wheelingWindow: Window? = null // Track the window we started mouse-wheeling on. Until a timer elapse or mouse has moved, generally keep scrolling the same window even if during the course of scrolling the mouse ends up hovering a child window
    val wheelingWindowRefMousePos: Vec2 = Vec2()
    var wheelingWindowStartFrame: Int = -1 // This may be set one frame before WheelingWindow is != NULL
    var wheelingWindowReleaseTimer: Float = 0f
    val wheelingWindowWheelRemainder: Vec2 = Vec2()
    val wheelingAxisAvg: Vec2 = Vec2()
    var debugHookIdInfo: ID = 0 // Will call core hooks: DebugHookIdInfo() from GetID functions, used by Stack Tool [next HoveredId/ActiveId to not pull in an extra cache-line]
    var hoveredId: ID = 0 // Hovered widget, filled during the frame
    var hoveredIdPreviousFrame: ID = 0
    var hoveredIdAllowOverlap: Boolean = false
    var hoveredIdDisabled: Boolean = false // At least one widget passed the rect test, but has been discarded by disabled flag or popup inhibit. May be true even if HoveredId == 0.
    var hoveredIdTimer: Float = 0f // Measure contiguous hovering time
    var hoveredIdNotActiveTimer: Float = 0f // Measure contiguous hovering time where the item has not been active
    var activeId: ID = 0 // Active widget
    var activeIdIsAlive: ID = 0 // Active widget has been seen this frame (we can't use a bool as the ActiveId may change within the frame)
    var activeIdTimer: Float = 0f
    var activeIdIsJustActivated: Boolean = false // Set at the time of activation for one frame
    var activeIdAllowOverlap: Boolean = false // Active widget allows another widget to steal active id (generally for overlapping widgets, but not always)
    var activeIdNoClearOnFocusLoss: Boolean = false // Disable losing active id if the active id window gets unfocused
    var activeIdHasBeenPressedBefore: Boolean = false // Track whether the active id led to a press (this is to allow changing between PressOnClick and PressOnRelease without pressing twice). Used by range_select branch
    var activeIdHasBeenEditedBefore: Boolean = false // Was the value associated to the widget edited over the course of the Active state
    var activeIdHasBeenEditedThisFrame: Boolean = false
    var activeIdClickOffset: Vec2 = Vec2(-1) // Clicked offset from upper-left corner, if applicable (currently only set by ButtonBehavior)
    var activeIdWindow: Window? = null
    var activeIdSource: InputSource = InputSource.None // Activating source: ImGuiInputSource_Mouse OR ImGuiInputSource_Keyboard OR ImGuiInputSource_Gamepad
    var activeIdMouseButton: MouseButton = MouseButton.None
    var activeIdPreviousFrame: ID = 0
    var activeIdPreviousFrameIsAlive: Boolean = false
    var activeIdPreviousFrameHasBeenEdited: Boolean = false
    var activeIdPreviousFrameWindow: Window? = null
    var lastActiveId: ID = 0 // Store the last non-zero ActiveId, useful for animation
    var lastActiveIdTimer: Float = 0f // Store the last non-zero ActiveId timer since the beginning of activation, useful for animation
    var keysRoutingTable: KeyRoutingTable = KeyRoutingTable()
    var activeIdUsingNavDirMask: Int = 0 // Active widget will want to read those nav move requests (e.g. can activate a button and move away from it) */
    var activeIdUsingAllKeyboardKeys: Boolean = false // Active widget will want to read all keyboard keys inputs. (FIXME: This is a shortcut for not taking ownership of 100+ keys but perhaps best to not have the inconsistency)
    var currentFocusScopeId: ID = 0 // == g.FocusScopeStack.back()
    var currentItemFlags: ItemFlags = none // == g.ItemFlagsStack.back()
    var debugLocateId: ID = 0 // Storage for DebugLocateItemOnHover() feature: this is read by ItemAdd() so we keep it in a hot/cached location
    var nextItemData: NextItemData = NextItemData() // Storage for SetNextItem** functions
    val lastItemData: LastItemData = LastItemData() // Storage for last submitted item (setup by ItemAdd)
    val nextWindowData: NextWindowData = NextWindowData() // Storage for SetNextWindow** functions
    var colorStack: Stack<ColorMod> = Stack<ColorMod>() // Stack for PushStyleColor()/PopStyleColor() - inherited by Begin()
    val styleVarStack: Stack<StyleMod> = Stack<StyleMod>() // Stack for PushStyleVar()/PopStyleVar() - inherited by Begin()
    val fontStack: Stack<Font> = Stack<Font>() // Stack for PushFont()/PopFont() - inherited by Begin()
    val focusScopeStack: Stack<ID> = Stack<ID>() // Stack for PushFocusScope()/PopFocusScope() - inherited by BeginChild(), pushed into by Begin()
    val itemFlagsStack: Stack<Flag<ItemFlag>> = Stack<ItemFlags>() // Stack for PushItemFlag()/PopItemFlag() - inherited by Begin()
    val groupStack: Stack<GroupData> = Stack<GroupData>() // Stack for BeginGroup()/EndGroup() - not inherited by Begin()
    val openPopupStack: Stack<PopupData> = Stack<PopupData>() // Which popups are open (persistent)
    val beginPopupStack: Stack<PopupData> = Stack<PopupData>() // Which level of BeginPopup() we are in (reset every frame)
    var beginMenuCount: Int = 0
    val viewports: ArrayList<ViewportP> = ArrayList<ViewportP>() // Active viewports (Size==1 in 'master' branch). Each viewports hold their copy of ImDrawData.
    var navWindow: Window? = null // Focused window for navigation. Could be called 'FocusedWindow'
    var navId: ID = 0 // Focused item for navigation
    var navFocusScopeId: Int = 0 // Identify a selection scope (selection code often wants to "clear other items" when landing on an item of the selection set)
    var navActivateId: ID = 0 // ~~ (g.ActiveId == 0) && (IsKeyPressed(ImGuiKey_Space) || IsKeyDown(ImGuiKey_Enter) || IsKeyPressed(ImGuiKey_NavGamepadActivate)) ? NavId : 0, also set when calling ActivateItem()
    var navActivateDownId: ID = 0 // ~~ IsKeyDown(ImGuiKey_Space) || IsKeyDown(ImGuiKey_Enter) || IsKeyDown(ImGuiKey_NavGamepadActivate) ? NavId : 0
    var navActivatePressedId: ID = 0 // ~~ IsKeyPressed(ImGuiKey_Space) || IsKeyDown(ImGuiKey_Enter) || IsKeyPressed(ImGuiKey_NavGamepadActivate) ? NavId : 0 (no repeat)
    var navActivateFlags: ActivateFlags = none
    var navJustMovedToId: ID = 0 // Just navigated to this id (result of a successfully MoveRequest)
    var navJustMovedToFocusScopeId: ID = 0 // Just navigated to this focus scope id (result of a successfully MoveRequest).
    var navJustMovedToKeyMods: KeyChord = Key.Mod_None
    var navNextActivateId: ID = 0 // Set by ActivateItem(), queued until next frame
    var navNextActivateFlags: ActivateFlags = none
    var navInputSource: InputSource = InputSource.Keyboard // Keyboard or Gamepad mode? THIS CAN ONLY BE ImGuiInputSource_Keyboard or ImGuiInputSource_Mouse
    var navLayer: NavLayer = NavLayer.Main // Layer we are navigating on. For now the system is hard-coded for 0 = main contents and 1 = menu/title bar, may expose layers later.
    var navIdIsAlive: Boolean = false // Nav widget has been seen this frame ~~ NavRectRel is valid
    var navMousePosDirty: Boolean = false // When set we will update mouse position if (io.ConfigFlag & ConfigFlag.NavMoveMouse) if set (NB: this not enabled by default)
    var navDisableHighlight: Boolean = true // When user starts using mouse, we hide gamepad/keyboard highlight (NB: but they are still available, which is why NavDisableHighlight isn't always != NavDisableMouseHover)
    var navDisableMouseHover: Boolean = false // When user starts using gamepad/keyboard, we hide mouse hovering highlight until mouse is touched again.
    var navAnyRequest: Boolean = false     // ~~ navMoveRequest || navInitRequest this is to perform early out in ItemAdd()
    var navInitRequest: Boolean = false // Init request for appearing window to select first item
    var navInitRequestFromMove: Boolean = false
    val navInitResult: NavItemData = NavItemData() // Init request result (first item of the window, or one for which SetItemDefaultFocus() was called)
    var navMoveSubmitted: Boolean = false // Move request submitted, will process result on next NewFrame()
    var navMoveScoringItems: Boolean = false // Move request submitted, still scoring incoming items
    var navMoveForwardToNextFrame = false
    var navMoveFlags: NavMoveFlags = none
    var navMoveScrollFlags: ScrollFlags = none
    var navMoveKeyMods: KeyChord = Key.Mod_None
    var navMoveDir: Dir = Dir.None // Direction of the move request (left/right/up/down), direction of the previous move request
    var navMoveDirForDebug: Dir = Dir.None
    var navMoveClipDir: Dir = Dir.None // FIXME-NAV: Describe the purpose of this better. Might want to rename?
    val navScoringRect: Rect = Rect() // Rectangle used for scoring, in screen space. Based of window.NavRectRel[], modified for directional navigation scoring.
    val navScoringNoClipRect: Rect = Rect() // Some nav operations (such as PageUp/PageDown) enforce a region which clipper will attempt to always keep submitted
    var navScoringDebugCount: Int = 0 // Metrics for debugging
    var navTabbingDir: Int = 0 // Generally -1 or +1, 0 when tabbing without a nav id
    var navTabbingCounter: Int = 0 // >0 when counting items for tabbing
    var navMoveResultLocal: NavItemData = NavItemData() // Best move request candidate within NavWindow
    val navMoveResultLocalVisible: NavItemData = NavItemData() // Best move request candidate within NavWindow that are mostly visible (when using NavMoveFlags.AlsoScoreVisibleSet flag)
    var navMoveResultOther: NavItemData = NavItemData() // Best move request candidate within NavWindow's flattened hierarchy (when using WindowFlags.NavFlattened flag)
    val navTabbingResultFirst: NavItemData = NavItemData() // First tabbing request candidate within NavWindow and flattened hierarchy
    var configNavWindowingKeyNext: KeyChord = Key.Mod_Ctrl or Key.Tab // = ImGuiMod_Ctrl | ImGuiKey_Tab, for reconfiguration (see #4828)
    var configNavWindowingKeyPrev: KeyChord = Key.Mod_Ctrl or Key.Mod_Shift or Key.Tab // = ImGuiMod_Ctrl | ImGuiMod_Shift | ImGuiKey_Tab
    var navWindowingTarget: Window? = null // Target window when doing CTRL+Tab (or Pad Menu + FocusPrev/Next), this window is temporarily displayed top-most!
    var navWindowingTargetAnim: Window? = null // Record of last valid NavWindowingTarget until DimBgRatio and NavWindowingHighlightAlpha becomes 0.0f, so the fade-out can stay on it.
    var navWindowingListWindow: Window? = null // Internal window actually listing the CTRL+Tab contents
    var navWindowingTimer: Float = 0f
    var navWindowingHighlightAlpha: Float = 0f
    var navWindowingToggleLayer: Boolean = false
    val navWindowingAccumDeltaPos: Vec2 = Vec2()
    val navWindowingAccumDeltaSize: Vec2 = Vec2()
    val drawDataBuilder: DrawDataBuilder = DrawDataBuilder()
    var dimBgRatio: Float = 0f // 0.0..1.0 animation when fading in a dimming background (for modal window and CTRL+TAB list)
    var dragDropActive: Boolean = false
    var dragDropWithinSource: Boolean = false // Set when within a BeginDragDropXXX/EndDragDropXXX block for a drag source.
    var dragDropWithinTarget: Boolean = false // Set when within a BeginDragDropXXX/EndDragDropXXX block for a drag target.
    var dragDropSourceFlags: DragDropFlags = none
    var dragDropSourceFrameCount: Int = -1
    var dragDropMouseButton: MouseButton = MouseButton.None // -1 at start
    var dragDropPayload: Payload = Payload()
    var dragDropTargetRect: Rect = Rect() // Store rectangle of current target candidate (we favor small targets when overlapping)
    var dragDropTargetId: ID = 0
    var dragDropAcceptFlags: DragDropFlags = none
    var dragDropAcceptIdCurrRectSurface: Float = 0f // Target item surface (we resolve overlapping targets by prioritizing the smaller surface)
    var dragDropAcceptIdCurr: ID = 0 // Target item id (set at the time of accepting the payload)
    var dragDropAcceptIdPrev: ID = 0 // Target item id from previous frame (we need to store this to allow for overlapping drag and drop targets)
    var dragDropAcceptFrameCount: Int = -1 // Last time a target expressed a desire to accept the source
    var dragDropHoldJustPressedId: ID = 0 // Set when holding a payload just made ButtonBehavior() return a press.
    var dragDropPayloadBufHeap: ByteBuffer = ByteBuffer.allocate(0) // We don't expose the ImVector<> directly, ImGuiPayload only holds pointer+size
    var dragDropPayloadBufLocal: ByteBuffer = ByteBuffer.allocate(16) // Local buffer for small payloads
    var clipperTempDataStacked: Int = 0
    val clipperTempData: ArrayList<ListClipperData> = ArrayList<ListClipperData>()
    var currentTable: Table? = null
    var tablesTempDataStacked: Int = 0 // Temporary table data size (because we leave previous instances undestructed, we generally don't use TablesTempData.Size)
    val tablesTempData: ArrayList<TableTempData> = ArrayList<TableTempData>() // Temporary table data (buffers reused/shared across instances, support nesting)
    val tables: Pool<Table> = Pool { Table() } // Persistent table data
    val tablesLastTimeActive: ArrayList<Float> = ArrayList<Float>() // Last used timestamp of each tables (SOA, for efficient GC)
    val drawChannelsTempMergeBuffer: ArrayList<DrawChannel> = ArrayList<DrawChannel>()
    var currentTabBar: TabBar? = null
    val tabBars: Pool<TabBar> = Pool { TabBar() }
    val currentTabBarStack: Stack<PtrOrIndex> = Stack<PtrOrIndex>()
    val shrinkWidthBuffer: ArrayList<ShrinkWidthItem> = ArrayList<ShrinkWidthItem>()
    var hoverItemDelayId: ID = 0
    var hoverItemDelayIdPreviousFrame: ID = 0
    var hoverItemDelayTimer: Float = 0f // Currently used by IsItemHovered()
    var hoverItemDelayClearTimer: Float = 0f // Currently used by IsItemHovered(): grace time before g.TooltipHoverTimer gets cleared.
    var hoverItemUnlockedStationaryId: ID = 0 // Mouse has once been stationary on this item. Only reset after departing the item.
    var hoverWindowUnlockedStationaryId: ID = 0 // Mouse has once been stationary on this window. Only reset after departing the window.
    var mouseCursor: MouseCursor = MouseCursor.Arrow
    var mouseStationaryTimer: Float = 0f // Time the mouse has been stationary (with some loose heuristic)
    val mouseLastValidPos: Vec2 = Vec2()
    var inputTextState: InputTextState = InputTextState(this)
    val inputTextDeactivatedState: InputTextDeactivatedState = InputTextDeactivatedState()
    var inputTextPasswordFont: Font = Font()
    var tempInputId: ID = 0 // Temporary text input when CTRL+clicking on a slider, etc.
    var colorEditOptions: ColorEditFlags = ColorEditFlag.DefaultOptions // Store user options for color edit widgets
    var colorEditCurrentID: ID = 0 // Set temporarily while inside of the parent-most ColorEdit4/ColorPicker4 (because they call each others).
    var colorEditSavedID: ID = 0 // ID we are saving/restoring HS for
    var colorEditSavedHue: Float = 0f // Backup of last Hue associated to LastColor, so we can restore Hue in lossy RGB<>HSV round trips
    var colorEditSavedSat: Float = 0f // Backup of last Saturation associated to LastColor, so we can restore Saturation in lossy RGB<>HSV round trips
    var colorEditSavedColor: Int = 0
    val colorPickerRef: Vec4 = Vec4() // Initial/reference color at the time of opening the color picker.
    val comboPreviewData: ComboPreviewData = ComboPreviewData()
    var sliderGrabClickOffset: Float = 0f
    var sliderCurrentAccum: Float = 0f // Accumulated slider delta when using navigation controls.
    var sliderCurrentAccumDirty: Boolean = false // Has the accumulated slider delta changed since last time we tried to apply it?
    var dragCurrentAccumDirty: Boolean = false
    var dragCurrentAccum: Float = 0f // Accumulator for dragging modification. Always high-precision, not rounded by end-user precision settings
    var dragSpeedDefaultRatio: Float = 1f / 100f // If speed == 0.0f, uses (max-min) * DragSpeedDefaultRatio
    var scrollbarClickDeltaToGrabCenter: Float = 0f // Distance between mouse and center of grab box, normalized in parent space. Use storage?
    var disabledAlphaBackup: Float = 0f // Backup for style.Alpha for BeginDisabled()
    var disabledStackSize: Int = 0
    var tooltipOverrideCount: Int = 0
    val menusIdSubmittedThisFrame: ArrayList<ID> = ArrayList<ID>() // A list of menu IDs that were rendered at least once
    var platformImeData: PlatformImeData = PlatformImeData() // Data updated by current frame
    var platformImeDataPrev: PlatformImeData = PlatformImeData(inputPos = Vec2(-1f)) // Previous frame data (when changing we will call io.SetPlatformImeDataFn
    var platformLocaleDecimalPoint: Char = '.' // '.' or *localeconv()->decimal_point
    var settingsLoaded: Boolean = false
    var settingsDirtyTimer: Float = 0f // Save .ini Settings to memory when time reaches zero
    var settingsIniData: String = "" // In memory .ini Settings for Window
    val settingsHandlers: ArrayList<SettingsHandler> = ArrayList<SettingsHandler>() // List of .ini settings handlers
    val settingsWindows: ArrayList<WindowSettings> = ArrayList<WindowSettings>() // ImGuiWindow .ini settings entries (parsed from the last loaded .ini file and maintained on saving)
    val settingsTables = ArrayList<TableSettings>() // ImGuiTable .ini settings entries
    val hooks: ArrayList<ContextHook> = ArrayList<ContextHook>() // Hooks for extensions (e.g. test engine)
    var hookIdNext: ID = 0 // Next available HookId
    val localizationTable: MutableMap<LocKey, String> = mutableMapOf<LocKey, String>()
    var logEnabled: Boolean = false // Currently capturing
    var logType: LogType = LogType.None // Capture target
    var logFile: File? = null // If != NULL log to stdout/ file
    var logBuffer: StringBuilder = StringBuilder() // Accumulation buffer when log to clipboard. This is pointer so our GImGui static constructor doesn't call heap allocators.
    var logNextPrefix: String = ""
    var logNextSuffix: String = ""
    var logLinePosY: Float = Float.MAX_VALUE
    var logLineFirstItem: Boolean = false
    var logDepthRef: Int = 0
    var logDepthToExpand: Int = 2
    var logDepthToExpandDefault: Int = 2
    var debugLogFlags: DebugLogFlags = if (DEBUG) DebugLogFlag.OutputToTTY or DebugLogFlag.EventMask wo DebugLogFlag.EventClipper else none
    val debugLogBuf: StringBuilder = StringBuilder()
    val debugLogIndex: TextIndex = TextIndex()
    var debugLogClipperAutoDisableFrames: Int = 0
    var debugLocateFrames: Int = 0 // For DebugLocateItemOnHover(). This is used together with DebugLocateId which is in a hot/cached spot above.
    var debugBeginReturnValueCullDepth: Int = -1 // Cycle between 0..9 then wrap around.
    var debugItemPickerActive: Boolean = false // Item picker is active (started with DebugStartItemPicker())
    var debugItemPickerMouseButton: MouseButton = MouseButton.Left
    var debugItemPickerBreakId: ID = 0 // Will call IM_DEBUG_BREAK() when encountering this ID
    var debugMetricsConfig: MetricsConfig = MetricsConfig()
    val debugStackTool: StackTool = StackTool()
    val framerateSecPerFrame: FloatArray = FloatArray(60) // Calculate estimate of framerate for user over the last 60 frames..
    var framerateSecPerFrameIdx: Int = 0
    var framerateSecPerFrameCount: Int = 0
    var framerateSecPerFrameAccum: Float = 0f
    var wantCaptureMouseNextFrame: Int = -1 // Explicit capture override via SetNextFrameWantCaptureMouse()/SetNextFrameWantCaptureKeyboard(). Default to -1.
    var wantCaptureKeyboardNextFrame: Int = -1 // Explicit capture override via SetNextFrameWantCaptureMouse()/SetNextFrameWantCaptureKeyboard(). Default to -1.
    var wantTextInputNextFrame: Int = -1
    var tempBuffer: ByteArray = ByteArray(1024 * 3) // Temporary text buffer

    /*EXPERIMENTAL*/

    // Key/Input Ownership + Shortcut Routing system
    // - The idea is that instead of "eating" a given key, we can link to an owner.
    // - Input query can then read input by specifying ImGuiKeyOwner_Any (== 0), ImGuiKeyOwner_None (== -1) or a custom ID.
    // - Routing is requested ahead of time for a given chord (Key + Mods) and granted in NewFrame().
    val keysOwnerData = Array(Key.COUNT) { KeyOwnerData() }

    init {
        val prevCtx = ImGui.currentContext
        setCurrent()
        initialize()
        prevCtx?.setCurrent() // Restore previous context if any, else keep new one.
    }

    fun initialize() {
        assert(!initialized && !g.settingsLoaded)

        // Add .ini handle for ImGuiWindow and ImGuiTable types
        val iniHandler = SettingsHandler().apply {
            typeName = "Window"
            typeHash = hashStr("Window")
            clearAllFn = ::windowSettingsHandler_ClearAll
            readOpenFn = ::windowSettingsHandler_ReadOpen
            readLineFn = ::windowSettingsHandler_ReadLine
            applyAllFn = ::windowSettingsHandler_ApplyAll
            writeAllFn = ::windowSettingsHandler_WriteAll
        }
        addSettingsHandler(iniHandler)
        tableSettingsAddSettingsHandler()
        localizeRegisterEntries(gLocalizationEntriesEnUS) // Setup default localization table
        g.io.getClipboardTextFn = getClipboardTextFn_DefaultImpl // Platform dependent default implementations
        g.io.setClipboardTextFn = setClipboardTextFn_DefaultImpl
        g.io.clipboardUserData = g // Default implementation use the ImGuiContext as user data (ideally those would be arguments to the function)
        if (Platform.get() == Platform.WINDOWS) {
            g.io.setPlatformImeDataFn = setPlatformImeDataFn_DefaultImpl
        }
        g.viewports += ViewportP()         // Create default viewport
        initialized = true
    }

    // free heap allocations
    fun shutdown() {
        // The fonts atlas can be used prior to calling NewFrame(), so we clear it even if g.Initialized is FALSE (which would happen if we never called NewFrame)
        if (fontAtlasOwnedByContext) {
            io.fonts.locked = false
        }
        io.fonts.clear()
        drawListSharedData.tempBuffer.clear()

        // Cleanup of other data are conditional on actually having initialized Dear ImGui.
        if (!initialized)
            return

        // Save settings (unless we haven't attempted to load them: CreateContext/DestroyContext without a call to NewFrame shouldn't save an empty file)
        if (settingsLoaded)
            io.iniFilename?.let(::saveIniSettingsToDisk)

        // Notify hooked test engine, if any
        g.callHooks(ContextHookType.Shutdown)

        // Clear everything else
        windows.forEach { it.destroy() }
        windows.clear()
        windowsFocusOrder.clear()
        windowsTempSortBuffer.clear()
        currentWindow = null
        currentWindowStack.clear()
        windowsById.clear()
        navWindow = null
        hoveredWindow = null
        hoveredWindowUnderMovingWindow = null
        activeIdWindow = null
        activeIdPreviousFrameWindow = null
        movingWindow = null

        keysRoutingTable.clear()

        settingsWindows.clear()
        colorStack.clear()
        styleVarStack.clear()
        fontStack.clear()
        openPopupStack.clear()
        beginPopupStack.clear()
        drawDataBuilder.clear()

        viewports.clear()

        tabBars.clear()
        currentTabBarStack.clear()
        shrinkWidthBuffer.clear()

        clipperTempData.clear()

        tables.clear()
        tablesTempData.clear()
        drawChannelsTempMergeBuffer.clear() // TODO check if this needs proper deallocation

        menusIdSubmittedThisFrame.clear()
        inputTextState.textW = CharArray(0)
        inputTextState.initialTextA = ByteArray(0)
        inputTextState.textA = ByteArray(0)

        if (logFile != null) {
            logFile = null
        }
        logBuffer.setLength(0)
        debugLogBuf.clear()

        initialized = false
    }

    fun setCurrent() {
        gImGui = this
    }

    fun destroy() {
        val prevCtx = ImGui.currentContext
        setCurrent()
        shutdown()
        gImGuiNullable = if (prevCtx !== this) prevCtx else null
    }

    companion object {
        val gLocalizationEntriesEnUS: List<LocEntry> = listOf(
                LocEntry(LocKey.VersionStr, "Dear ImGui $IMGUI_VERSION ($IMGUI_VERSION_NUM)"),
                LocEntry(LocKey.TableSizeOne, "Size column to fit###SizeOne"),
                LocEntry(LocKey.TableSizeAllFit, "Size all columns to fit###SizeAll"),
                LocEntry(LocKey.TableSizeAllDefault, "Size all columns to default###SizeAll"),
                LocEntry(LocKey.TableResetOrder, "Reset order###ResetOrder"),
                LocEntry(LocKey.WindowingMainMenuBar, "(Main menu bar)"),
                LocEntry(LocKey.WindowingPopup, "(Popup)"),
                LocEntry(LocKey.WindowingUntitled, "(Untitled)")
        )
    }
}

typealias ContextHookCallback = (ctx: Context, hook: ContextHook) -> Unit

enum class ContextHookType {
    NewFramePre,
    NewFramePost,
    EndFramePre,
    EndFramePost,
    RenderPre,
    RenderPost,
    Shutdown,
    PendingRemoval
}

class ContextHook(
        var hookId: ID = 0,
        var type: ContextHookType = ContextHookType.NewFramePre,
        var owner: ID = 0,
        var callback: ContextHookCallback? = null,
        var userData: Any? = null
)