package imgui

import glm_.f
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.ImGui.arrowButton
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.beginChildFrame
import imgui.ImGui.beginColumns
import imgui.ImGui.beginCombo
import imgui.ImGui.beginDragDropSource
import imgui.ImGui.beginDragDropTarget
import imgui.ImGui.beginGroup
import imgui.ImGui.beginListBox
import imgui.ImGui.beginMainMenuBar
import imgui.ImGui.beginMenu
import imgui.ImGui.beginMenuBar
import imgui.ImGui.beginPopup
import imgui.ImGui.beginPopupContextItem
import imgui.ImGui.beginPopupContextVoid
import imgui.ImGui.beginPopupContextWindow
import imgui.ImGui.beginPopupModal
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
import imgui.ImGui.beginTable
import imgui.ImGui.beginTooltip
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.checkboxFlags
import imgui.ImGui.collapsingHeader
import imgui.ImGui.combo
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endChildFrame
import imgui.ImGui.endColumns
import imgui.ImGui.endCombo
import imgui.ImGui.endDragDropSource
import imgui.ImGui.endDragDropTarget
import imgui.ImGui.endGroup
import imgui.ImGui.endListBox
import imgui.ImGui.endMainMenuBar
import imgui.ImGui.endMenu
import imgui.ImGui.endMenuBar
import imgui.ImGui.endPopup
import imgui.ImGui.endTabBar
import imgui.ImGui.endTabItem
import imgui.ImGui.endTable
import imgui.ImGui.endTooltip
import imgui.ImGui.imageButton
import imgui.ImGui.indent
import imgui.ImGui.invisibleButton
import imgui.ImGui.menuItem
import imgui.ImGui.popButtonRepeat
import imgui.ImGui.popClipRect
import imgui.ImGui.popFont
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.popTabStop
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushButtonRepeat
import imgui.ImGui.pushClipRect
import imgui.ImGui.pushFont
import imgui.ImGui.pushID
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.pushTabStop
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.radioButton
import imgui.ImGui.selectable
import imgui.ImGui.smallButton
import imgui.ImGui.treeNode
import imgui.ImGui.treeNodeEx
import imgui.ImGui.treePop
import imgui.ImGui.unindent
import imgui.font.Font
import imgui.internal.sections.OldColumnFlags
import kotlin.reflect.KMutableProperty0

// Tables

inline fun table(strId: String, columns: Int, flags: TableFlags = none,
                 outerSize: Vec2 = Vec2(), innerWidth: Float = 0f, block: () -> Unit) {
    if (beginTable(strId, columns, flags, outerSize, innerWidth)) { // ~open
        block()
        endTable()
    }
}

// Windows

inline fun window(name: String, open: KMutableProperty0<Boolean>? = null, flags: WindowFlags = none, block: () -> Unit) {
    if (begin(name, open, flags)) // ~open
        block()
    end()
}

// Child Windows

inline fun child(strId: String, size: Vec2 = Vec2(), border: Boolean = false, extraFlags: WindowFlags = none, block: () -> Unit) {
    if (beginChild(strId, size, border, extraFlags)) // ~open
        block()
    endChild()
}

// Parameters stacks (shared)

inline fun withFont(font: Font = ImGui.defaultFont, block: () -> Unit) {
    pushFont(font)
    block()
    popFont()
}

fun _push(idx: Col, col: Any) {
    if (col is Int)
        pushStyleColor(idx, col)
    else
        pushStyleColor(idx, col as Vec4)
}

inline fun withStyleColor(idx: Col, col: Any, block: () -> Unit) {
    _push(idx, col)
    block()
    popStyleColor()
}

inline fun withStyleColor(idx0: Col, col0: Any, idx1: Col, col1: Any, block: () -> Unit) {
    _push(idx0, col0)
    _push(idx1, col1)
    block()
    popStyleColor(2)
}

inline fun withStyleColor(idx0: Col, col0: Any, idx1: Col, col1: Any, idx2: Col, col2: Any, block: () -> Unit) {
    _push(idx0, col0)
    _push(idx1, col1)
    _push(idx2, col2)
    block()
    popStyleColor(3)
}

inline fun withStyleColor(idx0: Col, col0: Any, idx1: Col, col1: Any, idx2: Col, col2: Any, idx3: Col, col3: Any, block: () -> Unit) {
    _push(idx0, col0)
    _push(idx1, col1)
    _push(idx2, col2)
    _push(idx3, col3)
    block()
    popStyleColor(4)
}

inline fun withStyleColor(idx0: Col, col0: Any, idx1: Col, col1: Any, idx2: Col, col2: Any, idx3: Col, col3: Any, idx4: Col, col4: Any, block: () -> Unit) {
    _push(idx0, col0)
    _push(idx1, col1)
    _push(idx2, col2)
    _push(idx3, col3)
    _push(idx4, col4)
    block()
    popStyleColor(5)
}

inline fun withStyleVar(idx: StyleVar, value: Any, block: () -> Unit) {
    pushStyleVar(idx, value)
    block()
    popStyleVar()
}

// Parameters stacks (current window)

inline fun withItemWidth(itemWidth: Int, block: () -> Unit) = withItemWidth(itemWidth.f, block)
inline fun withItemWidth(itemWidth: Float, block: () -> Unit) {
    pushItemWidth(itemWidth)
    block()
    popItemWidth()
}

inline fun withTextWrapPos(wrapPosX: Float = 0f, block: () -> Unit) {
    pushTextWrapPos(wrapPosX)
    block()
    popTextWrapPos()
}

inline fun withAllowKeyboardFocus(allowKeyboardFocus: Boolean, block: () -> Unit) {
    pushTabStop(allowKeyboardFocus)
    block()
    popTabStop()
}

inline fun <R> withButtonRepeat(repeat: Boolean, block: () -> R): R {
    pushButtonRepeat(repeat)
    return block().also { popButtonRepeat() }
}


// Cursor / Layout

inline fun indent(indentW: Float = 0f, block: () -> Unit) { // TODO indented?
    indent(indentW)
    block()
    unindent(indentW)
}

inline fun group(block: () -> Unit) {
    beginGroup()
    block()
    endGroup()
}


// ID stack/scopes

inline fun withID(id: Int, block: () -> Unit) {
    pushID(id)
    block()
    popID()
}

inline fun withID(id: String, block: () -> Unit) {
    pushID(id)
    block()
    popID()
}

inline fun withID(id: Any, block: () -> Unit) {
    pushID(id)
    block()
    popID()
}


// Widgets: Main

inline fun button(label: String, sizeArg: Vec2 = Vec2(), block: () -> Unit) {
    if (button(label, sizeArg))
        block()
}

inline fun smallButton(label: String, block: () -> Unit) {
    if (smallButton(label))
        block()
}

inline fun invisibleButton(strId: String, sizeArg: Vec2, block: () -> Unit) {
    if (invisibleButton(strId, sizeArg))
        block()
}

inline fun arrowButton(id: String, dir: Dir, block: () -> Unit) {
    if (arrowButton(id, dir))
        block()
}

inline fun imageButton(strId: String, userTextureId: TextureID, size: Vec2, uv0: Vec2 = Vec2(), uv1: Vec2 = Vec2(),
                       bgCol: Vec4 = Vec4(), tintCol: Vec4 = Vec4(1), block: () -> Unit) {
    if (imageButton(strId, userTextureId, size, uv0, uv1, bgCol, tintCol))
        block()
}

inline fun checkbox(label: String, vPtr: KMutableProperty0<Boolean>, block: () -> Unit) {
    if (checkbox(label, vPtr))
        block()
}

inline fun <F : Flag<F>> checkboxFlags(label: String, vPtr: KMutableProperty0<Flag<F>>, flagsValue: Flag<F>, block: () -> Unit) {
    if (checkboxFlags(label, vPtr, flagsValue))
        block()
}

inline fun radioButton(label: String, active: Boolean, block: () -> Unit) {
    if (radioButton(label, active))
        block()
}

inline fun radioButton(label: String, v: KMutableProperty0<Int>, vButton: Int, block: () -> Unit) {
    if (radioButton(label, v, vButton))
        block()
}


// Widgets: Combo Box


inline fun useCombo(label: String, previewValue: String?, flags: ComboFlags = none, block: () -> Unit) {
    if (beginCombo(label, previewValue, flags)) {
        block()
        endCombo()
    }
}

inline fun combo(label: String, currentItem: KMutableProperty0<Int>, itemsSeparatedByZeros: String,
                 heightInItems: Int = -1, block: () -> Unit) {
    if (combo(label, currentItem, itemsSeparatedByZeros, heightInItems))
        block()
}


// Widgets: Trees

inline fun treeNode(label: String, block: () -> Unit) {
    if (treeNode(label)) {
        block()
        treePop()
    }
}

inline fun treeNode(strId: String, fmt: String, block: () -> Unit) {
    if (treeNode(strId, fmt)) {
        block()
        treePop()
    }
}

inline fun treeNode(intPtr: Long, fmt: String, block: () -> Unit) {
    if (treeNode(intPtr, fmt)) {
        block()
        treePop()
    }
}

inline fun treeNodeEx(strID: String, flags: TreeNodeFlags = none, block: () -> Unit) {
    if (treeNodeEx(strID, flags)) {
        block()
        treePop()
    }
}

inline fun treeNodeEx(strID: String, flags: TreeNodeFlags, fmt: String, vararg args: Any, block: () -> Unit) {
    if (treeNodeEx(strID, flags, fmt, args)) {
        block()
        treePop()
    }
}

inline fun treeNodeEx(ptrID: Any, flags: TreeNodeFlags, fmt: String, vararg args: Any, block: () -> Unit) {
    if (treeNodeEx(ptrID, flags, fmt, args)) {
        block()
        treePop()
    }
}

inline fun treeNodeEx(intPtr: Long, flags: TreeNodeFlags, fmt: String, vararg args: Any, block: () -> Unit) {
    if (treeNodeEx(intPtr, flags, fmt, args)) {
        block()
        treePop()
    }
}

//    inline fun treePushed(intPtr: Long?, block: () -> Unit) { TODO check me
//        treePush(intPtr)
//        try { block() } finally { treePop() }
//    }

inline fun collapsingHeader(label: String, flags: TreeNodeFlags = none, block: () -> Unit) {
    if (collapsingHeader(label, flags))
        block()
}

inline fun collapsingHeader(label: String, open: KMutableProperty0<Boolean>, flags: TreeNodeFlags = none, block: () -> Unit) {
    if (collapsingHeader(label, open, flags))
        block()
}


// Widgets: Selectables

inline fun selectable(label: String, selected: Boolean = false, flags: SelectableFlags = none, sizeArg: Vec2 = Vec2(), block: () -> Unit) {
    if (selectable(label, selected, flags, sizeArg))
        block()
}


// Widgets: Menus

inline fun mainMenuBar(block: () -> Unit) {
    if (beginMainMenuBar()) {
        block()
        endMainMenuBar()
    }
}

inline fun menuBar(block: () -> Unit) {
    if (beginMenuBar()) {
        block()
        endMenuBar()
    }
}

inline fun menu(label: String, enabled: Boolean = true, block: () -> Unit) {
    if (beginMenu(label, enabled)) {
        block()
        endMenu()
    }
}

inline fun menuItem(label: String, shortcut: String = "", selected: Boolean = false, enabled: Boolean = true, block: () -> Unit) {
    if (menuItem(label, shortcut, selected, enabled))
        block()
}


// Tooltips

inline fun tooltip(block: () -> Unit) {
    if (beginTooltip()) {
        block()
        endTooltip()
    }
}


// Popups, Modals

inline fun popup(strId: String, flags: WindowFlags = none, block: () -> Unit) {
    if (beginPopup(strId, flags)) {
        block()
        endPopup()
    }
}

inline fun popupContextItem(strId: String = "", popupFlags: PopupFlags = PopupFlag.MouseButtonRight, block: () -> Unit) {
    if (beginPopupContextItem(strId, popupFlags)) {
        block()
        endPopup()
    }
}

inline fun popupContextWindow(strId: String = "", popupFlags: PopupFlags = PopupFlag.MouseButtonRight, block: () -> Unit) {
    if (beginPopupContextWindow(strId, popupFlags)) {
        block()
        endPopup()
    }
}

inline fun popupContextVoid(strId: String = "", popupFlags: PopupFlags = PopupFlag.MouseButtonRight, block: () -> Unit) {
    if (beginPopupContextVoid(strId, popupFlags)) {
        block()
        endPopup()
    }
}

inline fun popupModal(name: String, pOpen: KMutableProperty0<Boolean>? = null, extraFlags: WindowFlags = none, block: () -> Unit) {
    if (beginPopupModal(name, pOpen, extraFlags)) {
        block()
        endPopup()
    }
}


// Tab Bars, Tabs

inline fun tabBar(strId: String, flags: TabBarFlags = none, block: () -> Unit) {
    if (beginTabBar(strId, flags)) {
        block()
        endTabBar()
    }
}

inline fun tabItem(label: String, pOpen: KMutableProperty0<Boolean>? = null, flags: TabItemOnlyFlags = none, block: () -> Unit) {
    if (beginTabItem(label, pOpen, flags)) {
        block()
        endTabItem()
    }
}


// Drag and Drop

inline fun dragDropSource(flags: DragDropFlags = none, block: () -> Unit) {
    if (beginDragDropSource(flags)) {
        block()
        endDragDropSource()
    }
}

inline fun dragDropTarget(block: () -> Unit) {
    if (beginDragDropTarget()) {
        block()
        endDragDropTarget()
    }
}


// Clipping

inline fun withClipRect(clipRectMin: Vec2, clipRectMax: Vec2, intersectWithCurrentClipRect: Boolean, block: () -> Unit) {
    pushClipRect(clipRectMin, clipRectMax, intersectWithCurrentClipRect)
    block()
    popClipRect()
}


// Miscellaneous Utilities

inline fun childFrame(id: ID, size: Vec2, extraFlags: WindowFlags = none, block: () -> Unit) {
    if (beginChildFrame(id, size, extraFlags))
        block()
    endChildFrame()
}

// Columns

inline fun columns(strId: String = "", columnsCount: Int, flags: OldColumnFlags = none, block: () -> Unit) {
    beginColumns(strId, columnsCount, flags)
    block()
    endColumns()
}

// listBox

inline fun listBox(label: String, sizeArg: Vec2 = Vec2(), block: () -> Unit) {
    if (beginListBox(label, sizeArg)) {
        block()
        endListBox()
    }
}