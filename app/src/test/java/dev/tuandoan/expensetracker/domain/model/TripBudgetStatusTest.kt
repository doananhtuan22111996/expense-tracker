package dev.tuandoan.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripBudgetStatusTest {
    private fun createTrip(
        budgetAmount: Long? = 500_000L,
        startDateEpochDay: Long = 100L,
        endDateEpochDay: Long = 104L,
    ): Trip =
        Trip(
            id = 1L,
            name = "Japan Trip",
            destination = "Tokyo",
            startDateEpochDay = startDateEpochDay,
            endDateEpochDay = endDateEpochDay,
            foreignCurrencyCode = "JPY",
            foreignToHomeRate = 0.0067,
            originalCategoryId = null,
            originalCategoryNameSnapshot = null,
            originalCategoryIconSnapshot = null,
            originalCategoryColorSnapshot = null,
            createdAt = 1_000_000L,
            budgetAmount = budgetAmount,
        )

    @Test
    fun calculate_returnsNull_whenBudgetAmountIsNull() {
        val trip = createTrip(budgetAmount = null)
        val status = TripBudgetStatus.calculate(trip = trip, spentAmount = 100L, nowEpochDay = 100L)
        assertNull(status)
    }

    @Test
    fun calculate_returnsNull_whenBudgetAmountIsZeroOrNegative() {
        val tripZero = createTrip(budgetAmount = 0L)
        assertNull(TripBudgetStatus.calculate(trip = tripZero, spentAmount = 0L, nowEpochDay = 100L))

        val tripNegative = createTrip(budgetAmount = -500L)
        assertNull(TripBudgetStatus.calculate(trip = tripNegative, spentAmount = 0L, nowEpochDay = 100L))
    }

    @Test
    fun calculate_upcomingTrip_computesAllowanceOverTotalTripDays() {
        // Trip is days 100..104 (5 days), now = 95
        val trip = createTrip(budgetAmount = 500_000L, startDateEpochDay = 100L, endDateEpochDay = 104L)
        val status = TripBudgetStatus.calculate(trip = trip, spentAmount = 0L, nowEpochDay = 95L)

        requireNotNull(status)
        assertEquals(500_000L, status.budgetAmount)
        assertEquals(0L, status.spentAmount)
        assertEquals(500_000L, status.remainingAmount)
        assertEquals(5, status.remainingDays)
        // 500_000 / 5 = 100_000
        assertEquals(100_000L, status.dailyAllowance)
        assertEquals(0f, status.progressFraction, 0.001f)
        assertEquals(BudgetStatusLevel.OK, status.status)
    }

    @Test
    fun calculate_activeTrip_dayOne_noSpend() {
        // Trip days 100..104 (5 days), now = 100
        val trip = createTrip(budgetAmount = 500_000L, startDateEpochDay = 100L, endDateEpochDay = 104L)
        val status = TripBudgetStatus.calculate(trip = trip, spentAmount = 0L, nowEpochDay = 100L)

        requireNotNull(status)
        assertEquals(5, status.remainingDays)
        assertEquals(100_000L, status.dailyAllowance)
        assertEquals(BudgetStatusLevel.OK, status.status)
    }

    @Test
    fun calculate_activeTrip_midTrip_withSpending() {
        // Trip days 100..104 (5 days), now = 102
        // Days left: 104 - 102 + 1 = 3 days (102, 103, 104)
        // Spent: 200_000, Remaining: 300_000
        // Daily allowance: 300_000 / 3 = 100_000
        val trip = createTrip(budgetAmount = 500_000L, startDateEpochDay = 100L, endDateEpochDay = 104L)
        val status = TripBudgetStatus.calculate(trip = trip, spentAmount = 200_000L, nowEpochDay = 102L)

        requireNotNull(status)
        assertEquals(3, status.remainingDays)
        assertEquals(300_000L, status.remainingAmount)
        assertEquals(100_000L, status.dailyAllowance)
        assertEquals(0.4f, status.progressFraction, 0.001f)
        assertEquals(BudgetStatusLevel.OK, status.status)
    }

    @Test
    fun calculate_activeTrip_lastDay_remainingBudgetAllocatedToSingleDay() {
        // Trip days 100..104, now = 104
        // Days left: 104 - 104 + 1 = 1 day
        // Spent: 420_000, Remaining: 80_000
        val trip = createTrip(budgetAmount = 500_000L, startDateEpochDay = 100L, endDateEpochDay = 104L)
        val status = TripBudgetStatus.calculate(trip = trip, spentAmount = 420_000L, nowEpochDay = 104L)

        requireNotNull(status)
        assertEquals(1, status.remainingDays)
        assertEquals(80_000L, status.remainingAmount)
        assertEquals(80_000L, status.dailyAllowance)
        assertEquals(0.84f, status.progressFraction, 0.001f)
        assertEquals(BudgetStatusLevel.WARNING, status.status)
    }

    @Test
    fun calculate_activeTrip_overBudget_allowanceIsZeroAndStatusOverBudget() {
        // Trip days 100..104, now = 102
        // Spent: 550_000 (exceeds 500_000 budget)
        val trip = createTrip(budgetAmount = 500_000L, startDateEpochDay = 100L, endDateEpochDay = 104L)
        val status = TripBudgetStatus.calculate(trip = trip, spentAmount = 550_000L, nowEpochDay = 102L)

        requireNotNull(status)
        assertEquals(3, status.remainingDays)
        assertEquals(-50_000L, status.remainingAmount)
        assertEquals(0L, status.dailyAllowance)
        assertEquals(1.1f, status.progressFraction, 0.001f)
        assertEquals(BudgetStatusLevel.OVER_BUDGET, status.status)
    }

    @Test
    fun calculate_pastTrip_allowanceIsNullAndRemainingDaysZero() {
        // Trip days 100..104, now = 105 (past)
        val trip = createTrip(budgetAmount = 500_000L, startDateEpochDay = 100L, endDateEpochDay = 104L)
        val status = TripBudgetStatus.calculate(trip = trip, spentAmount = 450_000L, nowEpochDay = 105L)

        requireNotNull(status)
        assertEquals(0, status.remainingDays)
        assertNull(status.dailyAllowance)
        assertEquals(50_000L, status.remainingAmount)
        assertEquals(0.9f, status.progressFraction, 0.001f)
        assertEquals(BudgetStatusLevel.WARNING, status.status)
    }

    @Test
    fun calculate_singleDayTrip_active() {
        // Start = 100, End = 100, Now = 100
        val trip = createTrip(budgetAmount = 100_000L, startDateEpochDay = 100L, endDateEpochDay = 100L)
        val status = TripBudgetStatus.calculate(trip = trip, spentAmount = 25_000L, nowEpochDay = 100L)

        requireNotNull(status)
        assertEquals(1, status.remainingDays)
        assertEquals(75_000L, status.remainingAmount)
        assertEquals(75_000L, status.dailyAllowance)
    }

    @Test
    fun statusLevels_progressFractionThresholds() {
        // 0% -> OK
        val s0 =
            TripBudgetStatus(budgetAmount = 100_000L, spentAmount = 0L, dailyAllowance = null, remainingDays = null)
        assertEquals(BudgetStatusLevel.OK, s0.status)
        assertEquals(0f, s0.progressFraction, 0.001f)

        // 79% -> OK
        val s79 =
            TripBudgetStatus(
                budgetAmount = 100_000L,
                spentAmount = 79_000L,
                dailyAllowance = null,
                remainingDays = null,
            )
        assertEquals(BudgetStatusLevel.OK, s79.status)
        assertEquals(0.79f, s79.progressFraction, 0.001f)

        // 80% -> WARNING
        val s80 =
            TripBudgetStatus(
                budgetAmount = 100_000L,
                spentAmount = 80_000L,
                dailyAllowance = null,
                remainingDays = null,
            )
        assertEquals(BudgetStatusLevel.WARNING, s80.status)
        assertEquals(0.80f, s80.progressFraction, 0.001f)

        // 99.9% -> WARNING
        val s99 =
            TripBudgetStatus(
                budgetAmount = 100_000L,
                spentAmount = 99_900L,
                dailyAllowance = null,
                remainingDays = null,
            )
        assertEquals(BudgetStatusLevel.WARNING, s99.status)

        // 100% -> OVER_BUDGET
        val s100 =
            TripBudgetStatus(
                budgetAmount = 100_000L,
                spentAmount = 100_000L,
                dailyAllowance = null,
                remainingDays = null,
            )
        assertEquals(BudgetStatusLevel.OVER_BUDGET, s100.status)
        assertEquals(1.0f, s100.progressFraction, 0.001f)

        // 150% -> OVER_BUDGET
        val s150 =
            TripBudgetStatus(
                budgetAmount = 100_000L,
                spentAmount = 150_000L,
                dailyAllowance = null,
                remainingDays = null,
            )
        assertEquals(BudgetStatusLevel.OVER_BUDGET, s150.status)
        assertEquals(1.5f, s150.progressFraction, 0.001f)
    }
}
