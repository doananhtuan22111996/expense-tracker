package dev.tuandoan.expensetracker.ui.screen.summary

import dev.tuandoan.expensetracker.domain.insights.InsightsResult

/**
 * UI state for the Summary-tab Insights section.
 *
 * Kept separate from [SummaryUiState] so the Insights flow can live on its
 * own `StateFlow` pipeline (combine + debounce + flowOn) without tangling
 * the existing summary/budget/bar-chart flows. [SummaryScreen] observes both
 * states independently.
 */
sealed interface InsightsUiState {
    /** Initial emission before the first combine resolves. */
    data object Loading : InsightsUiState

    /**
     * Year-view selected (PRD FR-20). The section is not rendered at all —
     * distinct from [Loading] so the UI can short-circuit without waiting
     * for an Insights re-compute that will never matter in year mode.
     */
    data object Hidden : InsightsUiState

    /**
     * Engine produced a result. [isCollapsed] drives the "show/hide rows"
     * toggle while keeping the section header visible (PRD FR-21); the row
     * list in [result] is populated regardless so flipping collapse is a
     * pure render concern.
     */
    data class Populated(
        val result: InsightsResult,
        val isCollapsed: Boolean,
    ) : InsightsUiState

    /**
     * Data-layer read failed (PRD FR-19). Surfaces the "Insights unavailable"
     * copy; the underlying exception is already logged via `CrashReporter` at
     * the repository boundary.
     */
    data object Error : InsightsUiState
}
