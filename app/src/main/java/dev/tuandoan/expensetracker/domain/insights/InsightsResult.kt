package dev.tuandoan.expensetracker.domain.insights

/**
 * Output of [computeInsights]. Carries the ordered list of rows the Summary
 * tab should render and a convenience [isEmpty] flag that mirrors whether
 * [rows] contains only an [InsightRow.Empty] placeholder.
 *
 * The engine is pure and total: errors are never returned here — the caller
 * is expected to wrap its invocation in try/catch and produce a result with
 * [InsightRow.Error] in [rows] on data-layer failures (PRD FR-19).
 */
data class InsightsResult(
    val rows: List<InsightRow>,
    val isEmpty: Boolean,
)
