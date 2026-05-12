package dev.tuandoan.expensetracker.ui.screen.widgetcategories

import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.widget.PinnedCategorySlot

/**
 * State for the Settings → Widget Categories screen (T6.1 / T6.3).
 *
 * The screen lets the user pick up to three EXPENSE categories to pin on
 * the home-screen widget's quick-add tile strip. [pinnedSlots] drives the
 * 3-slot preview row; [availableCategories] drives the pickable list
 * below.
 *
 * @property pinnedSlots resolved 3-slot view of the current pin list;
 *   always exactly three entries in ascending index (1..3). Filled slots
 *   carry the resolved [Category]; empty slots are placeholder-ready.
 * @property availableCategories every EXPENSE [Category] the user could
 *   pick, ordered by name. Rendered as the pickable list below the
 *   preview; rows already pinned are decorated with a "pinned" affordance.
 * @property pinnedCount count of *live* filled slots — raw pin IDs that
 *   still resolve to an existing [Category]. Orphan pins (IDs whose
 *   category was deleted from elsewhere in the app) are excluded so the
 *   cap count matches what the user sees. Drives the "N of 3 pinned"
 *   label and the [isAtMaxPins] check.
 * @property isAtMaxPins true when [pinnedCount] == 3. UI uses this to
 *   grey out the "pin" affordance on not-yet-pinned rows. The VM also
 *   guards every mutation against this.
 * @property overLimitMessage one-shot user-facing hint emitted when the
 *   user taps to pin a 4th category. UI clears via
 *   `WidgetCategoriesViewModel.onOverLimitMessageShown` after rendering
 *   the snackbar — one-shot semantics keep the message from re-firing on
 *   config change.
 * @property error one-shot user-facing error from a failed persistence
 *   write. Cleared via `WidgetCategoriesViewModel.onErrorShown`.
 * @property isLoading true until the first emission of both the prefs
 *   flow and the categories flow lands.
 */
data class WidgetCategoriesUiState(
    val pinnedSlots: List<PinnedCategorySlot> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val pinnedCount: Int = 0,
    val isAtMaxPins: Boolean = false,
    val overLimitMessage: UiText? = null,
    val error: UiText? = null,
    val isLoading: Boolean = true,
)
