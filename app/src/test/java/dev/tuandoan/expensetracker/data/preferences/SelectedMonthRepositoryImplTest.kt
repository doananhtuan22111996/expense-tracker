package dev.tuandoan.expensetracker.data.preferences

import dev.tuandoan.expensetracker.core.util.DateRangeCalculator
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class SelectedMonthRepositoryImplTest {
    private val fixedZone: ZoneId = ZoneId.of("UTC")
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), fixedZone)
    private val dateRangeCalculator = DateRangeCalculator(fixedClock, fixedZone)
    private lateinit var repository: SelectedMonthRepositoryImpl

    @Before
    fun setup() {
        repository = SelectedMonthRepositoryImpl(dateRangeCalculator)
    }

    @Test
    fun default_isCurrentMonth() {
        assertEquals(YearMonth.of(2026, 3), repository.selectedMonth.value)
    }

    @Test
    fun setMonth_updatesValue() {
        repository.setMonth(YearMonth.of(2025, 12))
        assertEquals(YearMonth.of(2025, 12), repository.selectedMonth.value)
    }

    @Test
    fun goToPreviousMonth_decrementsMonth() {
        repository.goToPreviousMonth()
        assertEquals(YearMonth.of(2026, 2), repository.selectedMonth.value)
    }

    @Test
    fun goToNextMonth_incrementsMonth() {
        repository.goToNextMonth()
        assertEquals(YearMonth.of(2026, 4), repository.selectedMonth.value)
    }

    @Test
    fun goToPreviousMonth_crossesYearBoundary() {
        repository.setMonth(YearMonth.of(2026, 1))
        repository.goToPreviousMonth()
        assertEquals(YearMonth.of(2025, 12), repository.selectedMonth.value)
    }

    @Test
    fun goToNextMonth_crossesYearBoundary() {
        repository.setMonth(YearMonth.of(2025, 12))
        repository.goToNextMonth()
        assertEquals(YearMonth.of(2026, 1), repository.selectedMonth.value)
    }

    @Test
    fun multipleNavigations_accumulateCorrectly() {
        // Start at March 2026, go back 3 months
        repository.goToPreviousMonth()
        repository.goToPreviousMonth()
        repository.goToPreviousMonth()
        assertEquals(YearMonth.of(2025, 12), repository.selectedMonth.value)

        // Go forward 5 months
        repository.goToNextMonth()
        repository.goToNextMonth()
        repository.goToNextMonth()
        repository.goToNextMonth()
        repository.goToNextMonth()
        assertEquals(YearMonth.of(2026, 5), repository.selectedMonth.value)
    }

    @Test
    fun setMonth_afterNavigation_overridesState() {
        repository.goToPreviousMonth()
        repository.goToPreviousMonth()
        assertEquals(YearMonth.of(2026, 1), repository.selectedMonth.value)

        repository.setMonth(YearMonth.of(2024, 6))
        assertEquals(YearMonth.of(2024, 6), repository.selectedMonth.value)
    }
}
