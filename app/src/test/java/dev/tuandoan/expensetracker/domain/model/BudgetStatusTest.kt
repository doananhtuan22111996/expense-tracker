package dev.tuandoan.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetStatusTest {
    private val vnd = SupportedCurrencies.requireByCode("VND")

    private fun budgetStatus(
        budget: Long,
        spent: Long,
    ): BudgetStatus = BudgetStatus(currency = vnd, budgetAmount = budget, spentAmount = spent)

    @Test
    fun zeroPercentSpent_statusIsOk() {
        val status = budgetStatus(budget = 1000000L, spent = 0L)
        assertEquals(BudgetStatusLevel.OK, status.status)
        assertEquals(0f, status.progressFraction, 0.001f)
        assertEquals(1000000L, status.remainingAmount)
    }

    @Test
    fun seventyNinePercentSpent_statusIsOk() {
        val status = budgetStatus(budget = 100L, spent = 79L)
        assertEquals(BudgetStatusLevel.OK, status.status)
        assertEquals(0.79f, status.progressFraction, 0.001f)
        assertEquals(21L, status.remainingAmount)
    }

    @Test
    fun eightyPercentSpent_statusIsWarning() {
        val status = budgetStatus(budget = 100L, spent = 80L)
        assertEquals(BudgetStatusLevel.WARNING, status.status)
        assertEquals(0.80f, status.progressFraction, 0.001f)
        assertEquals(20L, status.remainingAmount)
    }

    @Test
    fun hundredPercentSpent_statusIsOverBudget() {
        val status = budgetStatus(budget = 100L, spent = 100L)
        assertEquals(BudgetStatusLevel.OVER_BUDGET, status.status)
        assertEquals(1.0f, status.progressFraction, 0.001f)
        assertEquals(0L, status.remainingAmount)
    }

    @Test
    fun hundredTwentyPercentSpent_statusIsOverBudget() {
        val status = budgetStatus(budget = 100L, spent = 120L)
        assertEquals(BudgetStatusLevel.OVER_BUDGET, status.status)
        assertEquals(1.2f, status.progressFraction, 0.001f)
        assertEquals(-20L, status.remainingAmount)
    }

    @Test
    fun zeroBudget_progressFractionIsZero() {
        val status = budgetStatus(budget = 0L, spent = 50L)
        assertEquals(0f, status.progressFraction, 0.001f)
    }
}
