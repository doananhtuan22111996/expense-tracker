package dev.tuandoan.expensetracker.domain.insights

/**
 * Sealed hierarchy describing a single row rendered by the Summary-tab
 * Insights section. Each variant carries fully pre-formatted strings so the
 * UI layer does no arithmetic and no currency formatting — same contract used
 * by the widget state layer.
 *
 * Ordering inside [InsightsResult.rows] matches the PRD slot priority:
 * 1. [BiggestMover] (or [DailyPace] / [NoBudgetFallback] promoted into slot 1
 *    when no category meets the ≥2-txn threshold — see `computeInsights`)
 * 2. [DailyPace] or [NoBudgetFallback]
 * 3. [DayOfMonth]
 *
 * [Empty] replaces the entire row list when the user has insufficient data;
 * [Error] is reserved for data-layer failures (the engine itself is pure and
 * total — errors are populated by the caller, typically `SummaryViewModel`).
 */
sealed interface InsightRow {
    /** Biggest month-over-month category mover in either direction. */
    data class BiggestMover(
        val categoryName: String,
        val previousFormatted: String,
        val currentFormatted: String,
        val percentChange: Int,
        val direction: Direction,
    ) : InsightRow

    /** Daily spend pace vs. the user's active monthly budget. */
    data class DailyPace(
        val status: PaceStatus,
        val projectedFormatted: String,
        val budgetFormatted: String,
        val differenceFormatted: String?,
    ) : InsightRow

    /**
     * Informational fallback shown when no budget is set — PRD FR-12.
     * Mirrors [DailyPace]'s visual row but with no status/budget comparison.
     */
    data class NoBudgetFallback(
        val monthSpendFormatted: String,
        val dailyAverageFormatted: String,
    ) : InsightRow

    /** Day-of-month comparison vs. same day in the prior month. */
    data class DayOfMonth(
        val currentFormatted: String,
        val dayOfMonth: Int,
        val percentChange: Int?,
        val direction: Direction?,
    ) : InsightRow

    /** New user / no data yet — drives the "keep logging" empty-state copy. */
    data object Empty : InsightRow

    /** Data layer threw during the read path — drives the "insights unavailable" copy. */
    data object Error : InsightRow

    enum class Direction { UP, DOWN }

    enum class PaceStatus { ON_PACE, OVER, UNDER }
}
