package dev.tuandoan.expensetracker.domain.analytics

import dev.tuandoan.expensetracker.domain.insights.InsightRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins the [InsightRow] → [InsightRowType] mapping for the
 * `insight_shown` event (v3.11.0, PRD FR-A6). The sealed-class `when` in
 * [toAnalyticsRowType] is compiler-checked exhaustive — these tests add
 * a runtime assertion on top that (a) every concrete row maps to the
 * correct wire value per the PRD table, and (b) the two sentinel rows
 * (Empty, Error) correctly opt out of event tracking.
 */
class InsightRowAnalyticsMappingTest {
    @Test
    fun biggestMover_mapsToBiggestMoverRowType() {
        val row =
            InsightRow.BiggestMover(
                categoryName = "Food",
                previousFormatted = "$100",
                currentFormatted = "$150",
                percentChange = 50,
                direction = InsightRow.Direction.UP,
            )

        assertEquals(InsightRowType.BIGGEST_MOVER, row.toAnalyticsRowType())
    }

    @Test
    fun dailyPace_mapsToDailyPaceRowType() {
        val row =
            InsightRow.DailyPace(
                status = InsightRow.PaceStatus.ON_PACE,
                projectedFormatted = "$1000",
                budgetFormatted = "$1000",
                differenceFormatted = null,
            )

        assertEquals(InsightRowType.DAILY_PACE, row.toAnalyticsRowType())
    }

    @Test
    fun noBudgetFallback_mapsToNoBudgetFallbackRowType() {
        val row =
            InsightRow.NoBudgetFallback(
                monthSpendFormatted = "$500",
                dailyAverageFormatted = "$25",
            )

        assertEquals(InsightRowType.NO_BUDGET_FALLBACK, row.toAnalyticsRowType())
    }

    @Test
    fun dayOfMonth_mapsToDayOfMonthRowType() {
        val row =
            InsightRow.DayOfMonth(
                currentFormatted = "$100",
                dayOfMonth = 15,
                percentChange = 10,
                direction = InsightRow.Direction.UP,
            )

        assertEquals(InsightRowType.DAY_OF_MONTH, row.toAnalyticsRowType())
    }

    @Test
    fun empty_mapsToNull_opts_out_of_event() {
        // Sentinel: Empty row means "no insights to show" — we must NOT
        // log an `insight_shown` event for this. The null return is the
        // opt-out mechanism.
        assertNull(InsightRow.Empty.toAnalyticsRowType())
    }

    @Test
    fun error_mapsToNull_opts_out_of_event() {
        // Sentinel: Error row means data-layer failure — logging an
        // event would miscount "shown" as "displayed successfully".
        assertNull(InsightRow.Error.toAnalyticsRowType())
    }
}
