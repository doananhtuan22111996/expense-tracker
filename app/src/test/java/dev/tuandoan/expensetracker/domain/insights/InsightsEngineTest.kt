package dev.tuandoan.expensetracker.domain.insights

import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.domain.model.BudgetStatus
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Tests for [computeInsights].
 *
 * Organized by concern:
 *  - **Orchestration** — filter ordering, empty-state short-circuit, result
 *    data-class contract. Stable across algorithm tasks.
 *  - **Biggest-mover algorithm** (Task 2.2) — ≥2-txn threshold, delta floor,
 *    ranking, tie-break, direction.
 *  - **Daily-pace algorithm** (Task 2.3) — projection math, ±5% slack,
 *    ON_PACE / OVER / UNDER branching.
 *  - **Future algorithms** (Tasks 2.4 / 2.7) — day-of-month and no-budget
 *    fallback. Tests land with their PRs.
 */
class InsightsEngineTest {
    private val zone = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 4, 15)
    private val nowMillis: Long =
        today
            .atTime(10, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private val fakeFormatter =
        object : CurrencyFormatter {
            override fun format(
                amountMinor: Long,
                currencyCode: String,
            ): String = "$amountMinor $currencyCode"

            override fun formatWithSign(
                amountMinor: Long,
                currencyCode: String,
                isIncome: Boolean,
            ): String = (if (isIncome) "+" else "-") + format(amountMinor, currencyCode)

            override fun formatBareAmount(
                amountMinor: Long,
                currencyCode: String,
            ): String = amountMinor.toString()
        }

    private val foodCategory =
        Category(
            id = 1L,
            name = "Food",
            type = TransactionType.EXPENSE,
            iconKey = "restaurant",
            colorKey = "red",
            isDefault = true,
        )

    private val transportCategory =
        Category(
            id = 2L,
            name = "Transport",
            type = TransactionType.EXPENSE,
            iconKey = "directions_bus",
            colorKey = "blue",
            isDefault = true,
        )

    private val shoppingCategory =
        Category(
            id = 3L,
            name = "Shopping",
            type = TransactionType.EXPENSE,
            iconKey = "shopping_cart",
            colorKey = "purple",
            isDefault = true,
        )

    private val salaryCategory =
        Category(
            id = 99L,
            name = "Salary",
            type = TransactionType.INCOME,
            iconKey = "payments",
            colorKey = "green",
            isDefault = true,
        )

    private fun expense(
        id: Long,
        amount: Long,
        timestamp: Long = nowMillis,
        currency: String = "VND",
        category: Category = foodCategory,
    ): Transaction =
        Transaction(
            id = id,
            type = TransactionType.EXPENSE,
            amount = amount,
            currencyCode = currency,
            category = category,
            note = null,
            timestamp = timestamp,
            createdAt = timestamp,
            updatedAt = timestamp,
        )

    @Test
    fun compute_bothMonthsEmpty_returnsEmptyResultWithSingleEmptyRow() {
        val result =
            computeInsights(
                currentMonthExpenses = emptyList(),
                previousMonthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetStatus = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertTrue(result.isEmpty)
        assertEquals(listOf(InsightRow.Empty), result.rows)
    }

    @Test
    fun compute_onlyOtherCurrencyAndIncome_isTreatedAsEmpty() {
        // Currency + income filter runs BEFORE the empty-state check, so a
        // payload full of irrelevant transactions must collapse to the empty
        // state (not fall through to the algorithm slots).
        val income =
            Transaction(
                id = 1,
                type = TransactionType.INCOME,
                amount = 5_000_000L,
                currencyCode = "VND",
                category = salaryCategory,
                note = null,
                timestamp = nowMillis,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            )
        val usdExpense = expense(id = 2, amount = 500_00L, currency = "USD")

        val result =
            computeInsights(
                currentMonthExpenses = listOf(income, usdExpense),
                previousMonthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetStatus = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertTrue(result.isEmpty)
        assertEquals(listOf(InsightRow.Empty), result.rows)
    }

    @Test
    fun insightsResult_emptyRowsWithIsEmptyTrue_isTheContract() {
        // Pins the InsightsResult data class contract so neither field drifts
        // away from its PRD meaning.
        val empty = InsightsResult(rows = listOf(InsightRow.Empty), isEmpty = true)
        val populated = InsightsResult(rows = emptyList(), isEmpty = false)

        assertTrue(empty.isEmpty)
        assertFalse(populated.isEmpty)
    }

    // --- Biggest-mover algorithm (Task 2.2) ---

    @Test
    fun biggestMover_happyPathUp_returnsBiggestMoverWithCorrectFormatting() {
        // Food: 200_000 → 400_000 (+100%). Above 5% of current-month total
        // (400k is 100% of total, so any delta passes %-floor). Above 100k
        // absolute floor.
        val current =
            listOf(
                expense(id = 1, amount = 150_000L, category = foodCategory),
                expense(id = 2, amount = 250_000L, category = foodCategory),
            )
        val previous =
            listOf(
                expense(id = 3, amount = 100_000L, category = foodCategory),
                expense(id = 4, amount = 100_000L, category = foodCategory),
            )

        val result =
            computeInsights(
                currentMonthExpenses = current,
                previousMonthExpenses = previous,
                defaultCurrencyCode = "VND",
                budgetStatus = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val mover = result.rows.filterIsInstance<InsightRow.BiggestMover>().single()
        assertEquals("Food", mover.categoryName)
        assertEquals("200000 VND", mover.previousFormatted)
        assertEquals("400000 VND", mover.currentFormatted)
        assertEquals(100, mover.percentChange)
        assertEquals(InsightRow.Direction.UP, mover.direction)
    }

    @Test
    fun biggestMover_happyPathDown_returnsDownDirectionAndNegativePercent() {
        // Food: 300_000 → 200_000 (-33%).
        val current =
            listOf(
                expense(id = 1, amount = 100_000L, category = foodCategory),
                expense(id = 2, amount = 100_000L, category = foodCategory),
            )
        val previous =
            listOf(
                expense(id = 3, amount = 150_000L, category = foodCategory),
                expense(id = 4, amount = 150_000L, category = foodCategory),
            )

        val result =
            computeInsights(
                currentMonthExpenses = current,
                previousMonthExpenses = previous,
                defaultCurrencyCode = "VND",
                budgetStatus = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val mover = result.rows.filterIsInstance<InsightRow.BiggestMover>().single()
        assertEquals(-33, mover.percentChange)
        assertEquals(InsightRow.Direction.DOWN, mover.direction)
    }

    @Test
    fun biggestMover_singleTxnCategoryIsFiltered_evenIfBiggestPercentChange() {
        // Food has only 1 txn in previous month → filtered.
        // Transport has 2+ in both months and a smaller % change → wins.
        val current =
            listOf(
                expense(id = 1, amount = 500_000L, category = foodCategory),
                expense(id = 2, amount = 500_000L, category = foodCategory),
                expense(id = 3, amount = 150_000L, category = transportCategory),
                expense(id = 4, amount = 150_000L, category = transportCategory),
            )
        val previous =
            listOf(
                // Food: only 1 txn — doesn't meet threshold even though %
                // change (1_000 → 1_000_000, +99900%) would otherwise dominate.
                expense(id = 5, amount = 1_000L, category = foodCategory),
                expense(id = 6, amount = 100_000L, category = transportCategory),
                expense(id = 7, amount = 100_000L, category = transportCategory),
            )

        val result =
            computeInsights(
                currentMonthExpenses = current,
                previousMonthExpenses = previous,
                defaultCurrencyCode = "VND",
                budgetStatus = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val mover = result.rows.filterIsInstance<InsightRow.BiggestMover>().single()
        assertEquals("Transport", mover.categoryName)
        assertEquals(50, mover.percentChange) // 200k → 300k = +50%
    }

    @Test
    fun biggestMover_rankingByAbsolutePercentChange_picksWinner() {
        // Food: 200k → 300k = +50%
        // Transport: 200k → 400k = +100%  ← winner
        // Shopping: 300k → 150k = -50% (absolute 50)
        val current =
            listOf(
                expense(id = 1, amount = 150_000L, category = foodCategory),
                expense(id = 2, amount = 150_000L, category = foodCategory),
                expense(id = 3, amount = 200_000L, category = transportCategory),
                expense(id = 4, amount = 200_000L, category = transportCategory),
                expense(id = 5, amount = 75_000L, category = shoppingCategory),
                expense(id = 6, amount = 75_000L, category = shoppingCategory),
            )
        val previous =
            listOf(
                expense(id = 7, amount = 100_000L, category = foodCategory),
                expense(id = 8, amount = 100_000L, category = foodCategory),
                expense(id = 9, amount = 100_000L, category = transportCategory),
                expense(id = 10, amount = 100_000L, category = transportCategory),
                expense(id = 11, amount = 150_000L, category = shoppingCategory),
                expense(id = 12, amount = 150_000L, category = shoppingCategory),
            )

        val result =
            computeInsights(
                currentMonthExpenses = current,
                previousMonthExpenses = previous,
                defaultCurrencyCode = "VND",
                budgetStatus = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val mover = result.rows.filterIsInstance<InsightRow.BiggestMover>().single()
        assertEquals("Transport", mover.categoryName)
        assertEquals(100, mover.percentChange)
    }

    @Test
    fun biggestMover_deltaBelowBothFloors_returnsNull() {
        // Tiny 1k VND delta on a huge month — fails the %-of-total check AND
        // is below the 100k absolute floor. No mover → orchestrator returns
        // Empty (no other insights in this test setup either).
        val current =
            listOf(
                expense(id = 1, amount = 1_000_000L, category = foodCategory),
                expense(id = 2, amount = 1_000_000L, category = foodCategory),
            )
        val previous =
            listOf(
                expense(id = 3, amount = 999_500L, category = foodCategory),
                expense(id = 4, amount = 999_500L, category = foodCategory),
            )

        val result =
            computeInsights(
                currentMonthExpenses = current,
                previousMonthExpenses = previous,
                defaultCurrencyCode = "VND",
                budgetStatus = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertTrue(result.rows.filterIsInstance<InsightRow.BiggestMover>().isEmpty())
    }

    @Test
    fun biggestMover_deltaAboveAbsoluteFloorButBelowPercentFloor_passes() {
        // OR semantics of the delta floor: a 3% change that clears the 100k
        // absolute minor-unit threshold should still surface. Guards against
        // an accidental AND instead of OR in the floor check.
        // Prev 5M → Curr 5.15M = +150k absolute, +3% (below 5% floor).
        val current =
            listOf(
                expense(id = 1, amount = 2_575_000L, category = foodCategory),
                expense(id = 2, amount = 2_575_000L, category = foodCategory),
            )
        val previous =
            listOf(
                expense(id = 3, amount = 2_500_000L, category = foodCategory),
                expense(id = 4, amount = 2_500_000L, category = foodCategory),
            )

        val result =
            computeInsights(
                currentMonthExpenses = current,
                previousMonthExpenses = previous,
                defaultCurrencyCode = "VND",
                budgetStatus = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val mover = result.rows.filterIsInstance<InsightRow.BiggestMover>().single()
        assertEquals(3, mover.percentChange)
    }

    @Test
    fun biggestMover_tieOnAbsolutePercent_lowerCategoryIdWins() {
        // Food (id=1) and Transport (id=2) both at +50%. Deterministic
        // tie-break: lower categoryId first.
        val current =
            listOf(
                expense(id = 1, amount = 150_000L, category = foodCategory),
                expense(id = 2, amount = 150_000L, category = foodCategory),
                expense(id = 3, amount = 150_000L, category = transportCategory),
                expense(id = 4, amount = 150_000L, category = transportCategory),
            )
        val previous =
            listOf(
                expense(id = 5, amount = 100_000L, category = foodCategory),
                expense(id = 6, amount = 100_000L, category = foodCategory),
                expense(id = 7, amount = 100_000L, category = transportCategory),
                expense(id = 8, amount = 100_000L, category = transportCategory),
            )

        val result =
            computeInsights(
                currentMonthExpenses = current,
                previousMonthExpenses = previous,
                defaultCurrencyCode = "VND",
                budgetStatus = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val mover = result.rows.filterIsInstance<InsightRow.BiggestMover>().single()
        assertEquals("Food", mover.categoryName)
    }

    @Test
    fun biggestMover_usdFloor_10000MinorUnitsInstead_of100000() {
        // USD has minorUnitDigits=2, so the absolute floor is 10_000 minor
        // units (≈ $100), not 100_000. A 15_000 minor-unit delta (≈ $150)
        // with <5% percent change must still surface via the OR branch.
        // Prev 1_000_000 → Curr 1_015_000 = +1.5% (below 5%), +15_000 (above $100).
        val current =
            listOf(
                expense(id = 1, amount = 507_500L, category = foodCategory, currency = "USD"),
                expense(id = 2, amount = 507_500L, category = foodCategory, currency = "USD"),
            )
        val previous =
            listOf(
                expense(id = 3, amount = 500_000L, category = foodCategory, currency = "USD"),
                expense(id = 4, amount = 500_000L, category = foodCategory, currency = "USD"),
            )

        val result =
            computeInsights(
                currentMonthExpenses = current,
                previousMonthExpenses = previous,
                defaultCurrencyCode = "USD",
                budgetStatus = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val mover = result.rows.filterIsInstance<InsightRow.BiggestMover>().single()
        assertEquals(2, mover.percentChange)
    }

    // --- Daily-pace algorithm (Task 2.3) ---
    //
    // "today" is fixed to April 15, 2026 (UTC) → daysElapsed = 15, daysInMonth = 30.
    // So projected = spend * 30 / 15 = 2 * spend. Easy to reason about.

    private val vndBudget1M =
        BudgetStatus(
            currency = SupportedCurrencies.byCode("VND")!!,
            budgetAmount = 1_000_000L,
            spentAmount = 0L, // unused by the engine; only budgetAmount matters for pace
        )

    @Test
    fun dailyPace_projectedExactlyMatchesBudget_isOnPace() {
        // spend = 500k → projected = 1M = budget. ON_PACE.
        val result =
            computeInsights(
                currentMonthExpenses = listOf(expense(id = 1, amount = 500_000L)),
                previousMonthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetStatus = vndBudget1M,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val pace = result.rows.filterIsInstance<InsightRow.DailyPace>().single()
        assertEquals(InsightRow.PaceStatus.ON_PACE, pace.status)
        assertEquals("1000000 VND", pace.projectedFormatted)
        assertEquals("1000000 VND", pace.budgetFormatted)
        assertNull(pace.differenceFormatted)
    }

    @Test
    fun dailyPace_projectedAboveBudgetBeyondSlack_isOver() {
        // spend = 750k → projected = 1.5M = budget × 1.5 (well above 5% slack).
        // difference = projected - budget = 500k.
        val result =
            computeInsights(
                currentMonthExpenses = listOf(expense(id = 1, amount = 750_000L)),
                previousMonthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetStatus = vndBudget1M,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val pace = result.rows.filterIsInstance<InsightRow.DailyPace>().single()
        assertEquals(InsightRow.PaceStatus.OVER, pace.status)
        assertEquals("1500000 VND", pace.projectedFormatted)
        assertEquals("500000 VND", pace.differenceFormatted)
    }

    @Test
    fun dailyPace_projectedBelowBudgetBeyondSlack_isUnder() {
        // spend = 250k → projected = 500k = budget × 0.5.
        // difference = 500k - 1M = -500k ; |diff| = 500k.
        val result =
            computeInsights(
                currentMonthExpenses = listOf(expense(id = 1, amount = 250_000L)),
                previousMonthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetStatus = vndBudget1M,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val pace = result.rows.filterIsInstance<InsightRow.DailyPace>().single()
        assertEquals(InsightRow.PaceStatus.UNDER, pace.status)
        assertEquals("500000 VND", pace.projectedFormatted)
        assertEquals("500000 VND", pace.differenceFormatted) // |diff|, not signed
    }

    @Test
    fun dailyPace_projectedWithin5PercentAboveBudget_isOnPace() {
        // Slack is 5% of budget = 50k. Projected budget + 30k = 1.03M is inside
        // the slack → ON_PACE. Guards against accidentally treating any delta as OVER.
        // spend * 2 = 1.03M → spend = 515k.
        val result =
            computeInsights(
                currentMonthExpenses = listOf(expense(id = 1, amount = 515_000L)),
                previousMonthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetStatus = vndBudget1M,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val pace = result.rows.filterIsInstance<InsightRow.DailyPace>().single()
        assertEquals(InsightRow.PaceStatus.ON_PACE, pace.status)
        assertNull(pace.differenceFormatted)
    }

    @Test
    fun dailyPace_projectedWithin5PercentBelowBudget_isOnPace() {
        // Projected = budget - 30k = 970k. Within 50k slack → ON_PACE.
        // spend * 2 = 970k → spend = 485k.
        val result =
            computeInsights(
                currentMonthExpenses = listOf(expense(id = 1, amount = 485_000L)),
                previousMonthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetStatus = vndBudget1M,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val pace = result.rows.filterIsInstance<InsightRow.DailyPace>().single()
        assertEquals(InsightRow.PaceStatus.ON_PACE, pace.status)
    }

    @Test
    fun dailyPace_zeroSpend_isUnderWithFullBudgetAsDifference() {
        // User hasn't logged anything this month. Projected = 0; under by full budget.
        // Edge case: the orchestrator's empty-data short-circuit triggers BEFORE
        // the algorithm runs if BOTH months are empty — so we include a prev-month
        // transaction to keep the current-month-empty path reachable.
        val result =
            computeInsights(
                currentMonthExpenses = emptyList(),
                previousMonthExpenses = listOf(expense(id = 1, amount = 100_000L)),
                defaultCurrencyCode = "VND",
                budgetStatus = vndBudget1M,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val pace = result.rows.filterIsInstance<InsightRow.DailyPace>().single()
        assertEquals(InsightRow.PaceStatus.UNDER, pace.status)
        assertEquals("0 VND", pace.projectedFormatted)
        assertEquals("1000000 VND", pace.differenceFormatted) // full budget unspent
    }
}
