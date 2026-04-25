package dev.tuandoan.expensetracker.domain.insights

import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Orchestration-level tests for [computeInsights]. This PR (Task 2.1) ships
 * the engine skeleton: the data model, the currency/income filter, and the
 * slot-fill rules. The three insight-specific algorithms are stubbed and
 * covered by follow-up tasks (2.2 biggest mover, 2.3 daily pace, 2.4 day of
 * month). These tests pin the orchestration invariants so those later tasks
 * can't silently regress them.
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

    private val salaryCategory =
        Category(
            id = 2L,
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
    ): Transaction =
        Transaction(
            id = id,
            type = TransactionType.EXPENSE,
            amount = amount,
            currencyCode = currency,
            category = foodCategory,
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
    fun compute_stubbedAlgorithmsReturnNull_resultIsEmptyEvenWithRelevantData() {
        // Task 2.1 ships algorithms as stubs that return null. With relevant
        // expense data present, the data-presence short-circuit does NOT
        // trigger, but every slot returns null — so the orchestrator must
        // still produce an Empty result rather than an empty-list result.
        // When algorithms land in 2.2–2.4, THIS TEST SHOULD START FAILING —
        // that's the signal to replace it with algorithm-specific assertions.
        val result =
            computeInsights(
                currentMonthExpenses = listOf(expense(id = 1, amount = 100_000L)),
                previousMonthExpenses = listOf(expense(id = 2, amount = 80_000L)),
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
}
