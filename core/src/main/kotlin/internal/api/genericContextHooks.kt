package imgui.internal.api

import imgui.ID
import imgui.classes.Context
import imgui.classes.ContextHook
import imgui.classes.ContextHookType

interface genericContextHooks {

    // No specific ordering/dependency support, will see as needed
    fun Context.addHook(hook: ContextHook): ID {
        assert(hook.callback != null && hook.hookId == 0 && hook.type != ContextHookType.PendingRemoval)
        hooks += hook
        hook.hookId = ++hookIdNext
        return hookIdNext
    }

    fun Context.removeContextHook(hookId: ID) {
        assert(hookId != 0)
        for (hook in hooks) {
            if (hook.hookId == hookId) {
                hook.type = ContextHookType.PendingRemoval
            }
        }
    }

    // Call context hooks (used by e.g. test engine). We assume a small number of hooks so all stored in same array
    fun Context.callHooks(hookType: ContextHookType) {
        for (hook in hooks) {
            if (hook.type == hookType) {
                hook.callback!!.invoke(this, hook)
            }
        }
    }

}