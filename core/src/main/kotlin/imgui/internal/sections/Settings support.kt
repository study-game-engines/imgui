package imgui.internal.sections

import glm_.vec2.Vec2
import imgui.ID
import imgui.internal.classes.*
import imgui.internal.hashStr

/** Storage for a window .ini settings (we keep one of those even if the actual window wasn't instanced during this session)
 *
 *  Because we never destroy or rename ImGuiWindowSettings, we can store the names in a separate buffer easily.
 *  [JVM] We prefer keeping the `name` variable
 */
class WindowSettings(val name: String = "") {
    var id: ID = hashStr(name)
    var pos = Vec2()
    var size = Vec2()
    var collapsed = false
    var wantApply = false // Set when loaded from .ini data (to enable merging/loading .ini data into an already running context)
    var wantDelete = false // Set to invalidate/delete the settings entry

    fun clear() {
        id = hashStr(name)
        pos put 0f
        size put 0f
        collapsed = false
        wantApply = false
        wantDelete = false
    }
}

/** Storage for one type registered in the .ini file */
class SettingsHandler {
    /** Short description stored in .ini file. Disallowed characters: '[' ']' */
    var typeName = ""

    /** == ImHashStr(TypeName) */
    var typeHash: ID = 0

    var clearAllFn: ClearAllFn? = null
    var readInitFn: ReadInitFn? = null
    var readOpenFn: ReadOpenFn? = null
    var readLineFn: ReadLineFn? = null
    var applyAllFn: ApplyAllFn? = null
    var writeAllFn: WriteAllFn? = null
    var userData: Any? = null
}