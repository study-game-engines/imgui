package imgui.internal.api

import imgui.api.g
import imgui.internal.classes.LocEntry
import imgui.internal.classes.LocKey

// Localization
internal interface localization {

    fun localizeRegisterEntries(entries: List<LocEntry>) {
        for (entry in entries)
            g.localizationTable[entry.key] = entry.text
    }

    /** ~LocalizeGetMsg */
    val LocKey.msg
        get() = g.localizationTable[this] ?: "*Missing Text*"
}