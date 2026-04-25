package dev.tuandoan.expensetracker.widget

/**
 * Immutable snapshot of what the home-screen widget should render.
 *
 * Amounts are pre-formatted via `CurrencyFormatter` by the mapper so the
 * Glance layer doesn't need a formatter reference and can stay focused on
 * composition. Keeping this as pure data (no `Context`, no resources) makes
 * it trivially unit-testable and safe to hand to Glance's own serializer.
 *
 * @property currencyCode the user's default currency at mapping time; carried
 * through for accessibility announcements that want to spell out the currency.
 * @property todayFormatted today's expense total, already symbol-formatted
 * (e.g. `"120.000 ₫"` / `"$42.00"`). `"0 ₫"` / `"$0.00"` when no transactions.
 * @property monthFormatted this calendar month's expense total, same formatting.
 * @property budget the budget block — `null` when the user hasn't set a monthly
 * budget for [currencyCode], in which case the medium widget hides the row.
 */
data class ExpenseWidgetState(
    val currencyCode: String,
    val todayFormatted: String,
    val monthFormatted: String,
    val budget: BudgetDisplay?,
) {
    companion object {
        /**
         * Neutral placeholder used before the repository emits and in the
         * loading path. Currency symbol is unknown at this point so we
         * leave the amount strings blank rather than guessing.
         */
        val LOADING: ExpenseWidgetState =
            ExpenseWidgetState(
                currencyCode = "",
                todayFormatted = "",
                monthFormatted = "",
                budget = null,
            )
    }
}

/**
 * Budget-row payload. Present only when the user has a budget set for the
 * state's currency.
 *
 * @property spentFormatted month-to-date spend, already symbol-formatted.
 * @property budgetFormatted configured monthly budget, already symbol-formatted.
 * @property progressFraction `spent / budget`, clamped to `[0.0, 1.0]` for the
 * progress bar. Use [isOverBudget] for the over-budget color/glyph decision —
 * `progressFraction == 1.0f` can mean either "exactly at budget" or "over",
 * so callers must not read the over-state from it.
 * @property isOverBudget `true` when the raw ratio exceeds 1.0 — drives the
 * error color + ↑ glyph per colorblind accessibility guidance.
 * @property overByFormatted pre-formatted `spent - budget` when [isOverBudget]
 * is `true`, else `null`. TalkBack's single-sentence announcement spells out
 * "over by X" instead of leaving the screen reader to parse a raw ↑ glyph;
 * populating it in the mapper keeps the "Glance layer needs no formatter"
 * contract intact.
 */
data class BudgetDisplay(
    val spentFormatted: String,
    val budgetFormatted: String,
    val progressFraction: Float,
    val isOverBudget: Boolean,
    val overByFormatted: String?,
)
