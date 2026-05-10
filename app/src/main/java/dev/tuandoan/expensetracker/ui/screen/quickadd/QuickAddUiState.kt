package dev.tuandoan.expensetracker.ui.screen.quickadd

import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.domain.model.Category

/**
 * Immutable snapshot of the quick-add bottom sheet's UI state.
 *
 * @property category the pinned category resolved by `categoryId`; `null`
 * while loading and when the id no longer resolves (see [categoryMissing]).
 * @property currencyCode the user's default currency at sheet-open time.
 * Read once at init and never editable in the sheet — multi-currency on
 * quick-add is out of scope per PRD FR-11.
 * @property amountText user-entered amount, raw digits (locale formatting
 * is deliberately skipped — `AmountFormatter.parseAmount` only reads 0-9).
 * @property isSaving true while the save coroutine is in-flight; gates the
 * Save button and ignores duplicate taps.
 * @property saved one-shot flag flipped `true` by a successful save. The
 * hosting Activity observes this via `LaunchedEffect` and calls `finish()`.
 * @property categoryMissing true when the pinned category no longer
 * resolves (FR-07 race: widget snapshot showed the category; between tap
 * and sheet-open it was deleted). UI shows an inline "Category no longer
 * exists" message with a "Pick another" fallback in T3.4.
 * @property errorMessage transient validation / save-failure message.
 * Cleared on the next user input.
 */
data class QuickAddUiState(
    val category: Category? = null,
    val currencyCode: String = "",
    val amountText: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val categoryMissing: Boolean = false,
    val errorMessage: UiText? = null,
)
