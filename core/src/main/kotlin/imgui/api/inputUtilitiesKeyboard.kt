package imgui.api

import glm_.i
import imgui.ImGui
import imgui.Key


// Without IMGUI_DISABLE_OBSOLETE_KEYIO:
//   - For 'ImGuiKey key' you can use your own indices/enums according to how your backend/engine stored them in io.KeysDown[].
//   - We don't know the meaning of those value. You can use GetKeyIndex() to map a ImGuiKey_ value into the user index.
// With: IMGUI_DISABLE_OBSOLETE_KEYIO:
//   - `ImGuiKey key` will assert when key < 512 will be passed, previously reserved as user keys indices
//   - GetKeyIndex() is pass-through and therefore deprecated (gone if IMGUI_DISABLE_OBSOLETE_KEYIO is defined)
interface inputUtilitiesKeyboard {

    /** ~IsKeyDown
     *
     *  is key being held.
     *
     *  Note that Dear ImGui doesn't know the meaning/semantic of ImGuiKey from 0..511: they are legacy native keycodes.
     *  Consider transitioning from 'IsKeyDown(MY_ENGINE_KEY_A)' (<1.87) to IsKeyDown(A) (>= 1.87) */
    val Key.isDown: Boolean
        get() = g.io.keysData[i].down

    /** [JVM] ~IsKeyPressed
     *
     *  uses user's key indices as stored in the keys_down[] array. if repeat=true.
     *  uses io.KeyRepeatDelay / KeyRepeatRate
     *
     *  was key pressed (went from !Down to Down)? if repeat=true, uses io.KeyRepeatDelay / KeyRepeatRate */
    infix fun Key.isPressed(repeat: Boolean): Boolean {
        assert(i in Key.BEGIN until Key.END) { "Support for user key indices was dropped in favor of ImGuiKey. Please update backend & user code." }

        assert(i in g.io.keysData.indices)
        val t = g.io.keysData[i].downDuration
        if (t == 0f)
            return true
        if (repeat && t > ImGui.io.keyRepeatDelay)
            return getPressedAmount(ImGui.io.keyRepeatDelay, ImGui.io.keyRepeatRate) > 0
        return false
    }

    /** ~IsKeyPressed() */
    val Key.isPressed: Boolean
        get() = isPressed(true)

    /** ~IsKeyReleased
     *
     *  was key released (went from Down to !Down)?    */
    val Key.isReleased: Boolean
        get() = g.io.keysData[i].run { downDurationPrev >= 0f && !down }

    /** ~getKeyPressedAmount
     *
     *  Uses provided repeat rate/delay. return a count, most often 0 or 1 but might be >1 if RepeatRate is small enough
     *  that DeltaTime > RepeatRate
     *
     *  Return value representing the number of presses in the last time period, for the given repeat rate
     *  (most often returns 0 or 1. The result is generally only >1 when RepeatRate is smaller than DeltaTime, aka large DeltaTime or fast RepeatRate) */
    fun Key.getPressedAmount(repeatDelay: Float, repeatRate: Float): Int {
        if (!data.down) // In theory this should already be encoded as (DownDuration < 0.0f), but testing this facilitates eating mechanism (until we finish work on input ownership)
            return 0
        val t = ImGui.io.keysData[i].downDuration
        return ImGui.calcTypematicRepeatAmount(t - ImGui.io.deltaTime, t, repeatDelay, repeatRate)
    }

    /** Override io.WantCaptureKeyboard flag next frame (said flag is left for your application to handle, typically when true it instructs your app to ignore inputs). e.g. force capture keyboard when your widget is being hovered. This is equivalent to setting "io.WantCaptureKeyboard = want_capture_keyboard"; after the next NewFrame() call.  */
    fun Key.setNextFrameWantCaptureKeyboard(wantCaptureKeyboard: Boolean = true) {
        g.wantCaptureKeyboardNextFrame = wantCaptureKeyboard.i
    }
}