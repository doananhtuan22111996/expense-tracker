package dev.tuandoan.expensetracker.domain.analytics

import dev.tuandoan.expensetracker.domain.insights.InsightRow

/**
 * Maps an [InsightRow] subtype to its analytics [InsightRowType], or
 * `null` if the row is a sentinel (Empty / Error) that should NOT
 * trigger an `insight_shown` event.
 *
 * Kept in the analytics package rather than inside `InsightRow.kt` so the
 * Insights domain layer stays free of analytics concerns (`InsightRow` is
 * a pure-Kotlin UI model). The `when` over the sealed hierarchy is
 * compiler-checked exhaustive — adding a new `InsightRow` subtype forces
 * a mapping decision here, which in turn forces a privacy-policy +
 * PRD-FR-A6 review if the new row should be tracked.
 */
internal fun InsightRow.toAnalyticsRowType(): InsightRowType? =
    when (this) {
        is InsightRow.BiggestMover -> InsightRowType.BIGGEST_MOVER
        is InsightRow.DailyPace -> InsightRowType.DAILY_PACE
        is InsightRow.NoBudgetFallback -> InsightRowType.NO_BUDGET_FALLBACK
        is InsightRow.DayOfMonth -> InsightRowType.DAY_OF_MONTH
        InsightRow.Empty -> null
        InsightRow.Error -> null
    }
