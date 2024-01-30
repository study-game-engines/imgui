package imgui.api

import imgui.ImGui.button
import imgui.ImGui.clipboardText
import imgui.ImGui.logBegin
import imgui.ImGui.popID
import imgui.ImGui.popTabStop
import imgui.ImGui.pushID
import imgui.ImGui.pushTabStop
import imgui.ImGui.sameLine
import imgui.ImGui.setNextItemWidth
import imgui.classes.Context
import imgui.internal.sections.LogType
import java.io.File

/** Logging/Capture
 *  - All text output from the interface can be captured into tty/file/clipboard.
 *  By default, tree nodes are automatically opened during logging.     */
interface loggingCapture {

    /** Start logging/capturing text output to TTY */
    fun logToTTY(autoOpenDepth: Int = -1): Nothing = TODO()

    /** Start logging/capturing text output to given file */
    fun logToFile(maxDepth: Int = -1, file_: File? = null) {
        if (g.logEnabled)
            return
        val window = g.currentWindow!!

        g.logFile = file_ ?: g.logFile ?: return

        g.logEnabled = true
        g.logDepthRef = window.dc.treeDepth
        if (maxDepth >= 0)
            g.logDepthToExpandDefault = maxDepth
    }

    /** start logging ImGui output to OS clipboard   */
    fun logToClipboard(autoOpenDepth: Int = -1) {

        if (g.logEnabled) return

        logBegin(LogType.Clipboard, autoOpenDepth)
    }

    /** stop logging (close file, etc.) */
    fun logFinish() {

        if (!g.logEnabled)
            return

        logText("\n")

        when (g.logType) {
            LogType.TTY -> TODO() //fflush(g.LogFile)
            LogType.File -> TODO() //fclose(g.LogFile)
            LogType.Buffer -> Unit
            LogType.Clipboard -> {
                if (g.logBuffer.length > 1) { // TODO 1? maybe 0?
                    clipboardText = g.logBuffer.toString()
                    g.logBuffer.clear()
                }
            }
            else -> error("Invalid log type")
        }

        g.logEnabled = false
        g.logType = LogType.None
        g.logFile = null
        g.logBuffer.clear()
    }

    /** Helper to display buttons for logging to tty/file/clipboard
     *  FIXME-OBSOLETE: We should probably obsolete this and let the user have their own helper (this is one of the oldest function alive!) */
    fun logButtons() {
        pushID("LogButtons")
        val logToTty = button("Log To TTY"); sameLine()
        val logToFile = button("Log To File"); sameLine()
        val logToClipboard = button("Log To Clipboard"); sameLine()
        pushTabStop(false)
        setNextItemWidth(80f)
        slider("Default Depth", g::logDepthToExpandDefault, 0, 9)
        popTabStop()
        popID()

        // Start logging at the end of the function so that the buttons don't appear in the log
        if (logToTty) logToTTY()
        if (logToFile) logToFile()
        if (logToClipboard) logToClipboard()
    }

    /** pass text data straight to log (without being displayed)    */
    fun logTextV(g: Context, fmt: String, vararg args: Any) {
        val text = if (args.isEmpty()) fmt else fmt.format(*args)
        g.logFile?.appendText(text) ?: g.logBuffer.append(text)
    }

    fun logText(fmt: String, vararg args: Any) {
        if (!g.logEnabled)
            return

        logTextV(g, fmt, *args)
    }
}