package imgui.api

import glm_.vec2.Vec2
import imgui.*
import imgui.HoveredFlag
import imgui.ImGui.isClicked
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.ItemStatusFlag
import imgui.HoveredFlag as Hf

// Item/Widgets Utilities and Query Functions
// - Most of the functions are referring to the previous Item that has been submitted.
// - See Demo Window under "Widgets->Querying Status" for an interactive visualization of most of those functions.
interface itemWidgetsUtilities {

    // This is roughly matching the behavior of internal-facing ItemHoverable()
    // - we allow hovering to be true when ActiveId==window->MoveID, so that clicking on non-interactive items such as a Text() item still returns true with IsItemHovered()
    // - this should work even for non-interactive items that have no ID, so we cannot use LastItemId
    fun isItemHovered(flags_: HoveredFlags = none): Boolean {

        var flags = flags_
        val window = g.currentWindow!!
        assert((flags wo HoveredFlag.AllowedMaskForIsItemHovered) == none) { "Invalid flags for IsItemHovered()!" }

        if (g.navDisableMouseHover && !g.navDisableHighlight && flags hasnt Hf.NoNavOverride) {

            if (g.lastItemData.inFlags has ItemFlag.Disabled && flags hasnt Hf.AllowWhenDisabled)
                return false
            if (!isItemFocused)
                return false

            if (flags has HoveredFlag.ForTooltip)
                flags /= g.style.hoverFlagsForTooltipNav
        } else {
            // Test for bounding box overlap, as updated as ItemAdd()
            val statusFlags = g.lastItemData.statusFlags
            if (statusFlags hasnt ItemStatusFlag.HoveredRect)
                return false

            if (flags has HoveredFlag.ForTooltip)
                flags /= g.style.hoverFlagsForTooltipMouse

            assert(flags hasnt (HoveredFlag.AnyWindow / HoveredFlag.RootWindow / HoveredFlag.ChildWindows / HoveredFlag.NoPopupHierarchy)) { "Flags not supported by this function" }

            // Done with rectangle culling so we can perform heavier checks now
            // Test if we are hovering the right window (our window could be behind another window)
            // [2021/03/02] Reworked / reverted the revert, finally. Note we want e.g. BeginGroup/ItemAdd/EndGroup to work as well. (#3851)
            // [2017/10/16] Reverted commit 344d48be3 and testing RootWindow instead. I believe it is correct to NOT test for RootWindow but this leaves us unable
            // to use IsItemHovered() after EndChild() itself. Until a solution is found I believe reverting to the test from 2017/09/27 is safe since this was
            // the test that has been running for a long while.
            if (g.hoveredWindow !== window && statusFlags hasnt ItemStatusFlag.HoveredWindow)
                if (flags hasnt Hf.AllowWhenOverlappedByWindow)
                    return false

            // Test if another item is active (e.g. being dragged)
            val id = g.lastItemData.id
            if (flags hasnt Hf.AllowWhenBlockedByActiveItem)
                if (g.activeId != 0 && g.activeId != id && !g.activeIdAllowOverlap && g.activeId != window.moveId)
                    return false

            // Test if interactions on this window are blocked by an active popup or modal.
            // The ImGuiHoveredFlags_AllowWhenBlockedByPopup flag will be tested here.
            if (!window.isContentHoverable(flags) && g.lastItemData.inFlags hasnt ItemFlag.NoWindowHoverableCheck)
                return false

            // Test if the item is disabled
            if (g.lastItemData.inFlags has ItemFlag.Disabled && flags hasnt Hf.AllowWhenDisabled)
                return false

            // Special handling for calling after Begin() which represent the title bar or tab.
            // When the window is skipped/collapsed (SkipItems==true) that last item will never be overwritten so we need to detect the case.
            if (id == window.moveId && window.writeAccessed)
                return false

            // Test if using AllowOverlap and overlapped
            if (g.lastItemData.inFlags has ItemFlag.AllowOverlap && id != 0)
                if (flags hasnt HoveredFlag.AllowWhenOverlappedByItem)
                    if (g.hoveredIdPreviousFrame != g.lastItemData.id)
                        return false
        }

        // Handle hover delay
        // (some ideas: https://www.nngroup.com/articles/timing-exposing-content)
        val delay = when {
            flags has Hf.DelayShort -> g.style.hoverDelayShort
            flags has Hf.DelayNormal -> g.style.hoverDelayNormal
            else -> 0f
        }
        if (delay > 0f || flags has HoveredFlag.Stationary) {
            val hoverDelayId = if (g.lastItemData.id != 0) g.lastItemData.id else window.getIDFromRectangle(g.lastItemData.rect)
            if (flags has Hf.NoSharedDelay && g.hoverItemDelayIdPreviousFrame != hoverDelayId)
                g.hoverItemDelayTimer = 0f
            g.hoverItemDelayId = hoverDelayId

            // When changing hovered item we requires a bit of stationary delay before activating hover timer,
            // but once unlocked on a given item we also moving.
            //if (g.HoverDelayTimer >= delay && (g.HoverDelayTimer - g.IO.DeltaTime < delay || g.MouseStationaryTimer - g.IO.DeltaTime < g.Style.HoverStationaryDelay)) { IMGUI_DEBUG_LOG("HoverDelayTimer = %f/%f, MouseStationaryTimer = %f\n", g.HoverDelayTimer, delay, g.MouseStationaryTimer); }
            if (flags has HoveredFlag.Stationary && g.hoverItemUnlockedStationaryId != hoverDelayId)
                return false

            if (g.hoverItemDelayTimer < delay)
                return false
        }

        return true
    }

    val isItemHovered: Boolean
        get() = isItemHovered()

    /** Is the last item active? (e.g. button being held, text field being edited.
     *  This will continuously return true while holding mouse button on an item. Items that don't interact will always return false) */
    val isItemActive: Boolean
        get() = g.activeId != 0 && g.activeId == g.lastItemData.id

    /** Is the last item focused for keyboard/gamepad navigation?
     *
     *  == GetItemID() == GetFocusID() */
    val isItemFocused: Boolean
        get() = g.navId == g.lastItemData.id && g.navId != 0

    /** is the last item hovered and mouse clicked on? (**)  == IsMouseClicked(mouse_button) && IsItemHovered()Important. (**) this is NOT equivalent to the behavior of e.g. Button(). Read comments in function definition.
     *
     *  Important: this can be useful but it is NOT equivalent to the behavior of e.g.Button()!
     *  Most widgets have specific reactions based on mouse-up/down state, mouse position etc. */
    fun isItemClicked(mouseButton: MouseButton = MouseButton.Left): Boolean = mouseButton.isClicked && isItemHovered()

    /** Is the last item visible? (items may be out of sight because of clipping/scrolling)    */
    val isItemVisible: Boolean
        get() = g.lastItemData.statusFlags has ItemStatusFlag.Visible

    val isItemEdited: Boolean
        get() = g.lastItemData.statusFlags has ItemStatusFlag.Edited

    /** was the last item just made active (item was previously inactive). */
    val isItemActivated: Boolean
        get() = g.activeId != 0 && g.activeId == g.lastItemData.id && g.activeIdPreviousFrame != g.lastItemData.id

    /** was the last item just made inactive (item was previously active). Useful for Undo/Redo patterns with widgets that require continuous editing. */
    val isItemDeactivated: Boolean
        get() = when {
            g.lastItemData.statusFlags has ItemStatusFlag.HasDeactivated -> g.lastItemData.statusFlags has ItemStatusFlag.Deactivated
            else -> g.activeIdPreviousFrame == g.lastItemData.id && g.activeIdPreviousFrame != 0 && g.activeId != g.lastItemData.id
        }

    /** was the last item just made inactive and made a value change when it was active? (e.g. Slider/Drag moved). Useful for Undo/Redo patterns with widgets that require continuous editing. Note that you may get false positives (some widgets such as Combo()/ListBox()/Selectable() will return true even when clicking an already selected item). */
    val isItemDeactivatedAfterEdit: Boolean
        get() = isItemDeactivated && (g.activeIdPreviousFrameHasBeenEdited || (g.activeId == 0 && g.activeIdHasBeenEditedBefore))

    /** was the last item open state toggled? set by TreeNode(). */
    val isItemToggledOpen: Boolean
        get() = g.lastItemData.statusFlags has ItemStatusFlag.ToggledOpen

    /** is any item hovered? */
    val isAnyItemHovered: Boolean
        get() = g.hoveredId != 0 || g.hoveredIdPreviousFrame != 0

    /** is any item active? */
    val isAnyItemActive: Boolean
        get() = g.activeId != 0

    /** is any item focused? */
    val isAnyItemFocused: Boolean
        get() = g.navId != 0 && !g.navDisableHighlight

    /** ~GetItemID
     *  get ID of last item (~~ often same ImGui::GetID(label) beforehand)     */
    val itemID: ID
        get() = g.lastItemData.id

    /** get upper-left bounding rectangle of the last item (screen space)
     *  ~GetItemRectMin */
    val itemRectMin: Vec2
        get() = g.lastItemData.rect.min

    /** get lower-right bounding rectangle of the last item (screen space)
     *  ~GetItemRectMax */
    val itemRectMax: Vec2
        get() = g.lastItemData.rect.max

    /** get size of last item
     *  ~GetItemRectSize    */
    val itemRectSize: Vec2
        get() = g.lastItemData.rect.size
}