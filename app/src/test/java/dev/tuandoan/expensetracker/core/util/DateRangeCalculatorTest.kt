package dev.tuandoan.expensetracker.core.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class DateRangeCalculatorTest {
    private val utcZone: ZoneId = ZoneId.of("UTC")

    private lateinit var calculator: DateRangeCalculator

    @Before
    fun setup() {
        // Fixed at 2026-03-15T12:00:00Z
        val fixed = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), utcZone)
        calculator = DateRangeCalculator(fixed, utcZone)
    }

    // --- currentMonth ---

    @Test
    fun currentMonth_returnsCorrectYearMonth() {
        assertEquals(YearMonth.of(2026, 3), calculator.currentMonth())
    }

    // --- rangeOf ---

    @Test
    fun rangeOf_march2026_startsAtMarch1() {
        val range = calculator.rangeOf(YearMonth.of(2026, 3))
        // 2026-03-01T00:00:00Z
        assertEquals(1772323200000L, range.startMillis)
    }

    @Test
    fun rangeOf_march2026_endsAtApril1() {
        val range = calculator.rangeOf(YearMonth.of(2026, 3))
        // 2026-04-01T00:00:00Z
        assertEquals(1775001600000L, range.endMillisExclusive)
    }

    @Test
    fun rangeOf_january_startsAtJan1() {
        val range = calculator.rangeOf(YearMonth.of(2026, 1))
        // 2026-01-01T00:00:00Z
        assertEquals(1767225600000L, range.startMillis)
    }

    @Test
    fun rangeOf_january_endsAtFeb1() {
        val range = calculator.rangeOf(YearMonth.of(2026, 1))
        // 2026-02-01T00:00:00Z
        assertEquals(1769904000000L, range.endMillisExclusive)
    }

    @Test
    fun rangeOf_february_nonLeapYear_has28Days() {
        // 2026 is not a leap year
        val range = calculator.rangeOf(YearMonth.of(2026, 2))
        val daysInMillis = range.endMillisExclusive - range.startMillis
        assertEquals(28L * 24 * 60 * 60 * 1000, daysInMillis)
    }

    @Test
    fun rangeOf_february_leapYear_has29Days() {
        // 2024 is a leap year
        val range = calculator.rangeOf(YearMonth.of(2024, 2))
        val daysInMillis = range.endMillisExclusive - range.startMillis
        assertEquals(29L * 24 * 60 * 60 * 1000, daysInMillis)
    }

    @Test
    fun rangeOf_december_endsAtJan1NextYear() {
        val range = calculator.rangeOf(YearMonth.of(2025, 12))
        // 2026-01-01T00:00:00Z
        assertEquals(1767225600000L, range.endMillisExclusive)
    }

    // --- previousMonth / nextMonth ---

    @Test
    fun previousMonth_march_returnsFebruary() {
        val prev = calculator.previousMonth(YearMonth.of(2026, 3))
        assertEquals(YearMonth.of(2026, 2), prev)
    }

    @Test
    fun previousMonth_january_returnsDecemberPreviousYear() {
        val prev = calculator.previousMonth(YearMonth.of(2026, 1))
        assertEquals(YearMonth.of(2025, 12), prev)
    }

    @Test
    fun nextMonth_march_returnsApril() {
        val next = calculator.nextMonth(YearMonth.of(2026, 3))
        assertEquals(YearMonth.of(2026, 4), next)
    }

    @Test
    fun nextMonth_december_returnsJanuaryNextYear() {
        val next = calculator.nextMonth(YearMonth.of(2025, 12))
        assertEquals(YearMonth.of(2026, 1), next)
    }

    // --- displayLabel ---

    @Test
    fun displayLabel_march2026() {
        assertEquals("Mar 2026", calculator.displayLabel(YearMonth.of(2026, 3)))
    }

    @Test
    fun displayLabel_january2025() {
        assertEquals("Jan 2025", calculator.displayLabel(YearMonth.of(2025, 1)))
    }

    @Test
    fun displayLabel_december2024() {
        assertEquals("Dec 2024", calculator.displayLabel(YearMonth.of(2024, 12)))
    }

    // --- currentYear ---

    @Test
    fun currentYear_returns2026() {
        assertEquals(2026, calculator.currentYear())
    }

    // --- rangeOfYear ---

    @Test
    fun rangeOfYear_2026_startsAtJan1() {
        val range = calculator.rangeOfYear(2026)
        // 2026-01-01T00:00:00Z
        assertEquals(1767225600000L, range.startMillis)
    }

    @Test
    fun rangeOfYear_2026_endsAtJan1_2027() {
        val range = calculator.rangeOfYear(2026)
        // 2027-01-01T00:00:00Z
        assertEquals(1798761600000L, range.endMillisExclusive)
    }

    @Test
    fun rangeOfYear_2026_spans365Days() {
        val range = calculator.rangeOfYear(2026)
        val daysInMillis = range.endMillisExclusive - range.startMillis
        assertEquals(365L * 24 * 60 * 60 * 1000, daysInMillis)
    }

    @Test
    fun rangeOfYear_leapYear2024_spans366Days() {
        val range = calculator.rangeOfYear(2024)
        val daysInMillis = range.endMillisExclusive - range.startMillis
        assertEquals(366L * 24 * 60 * 60 * 1000, daysInMillis)
    }

    @Test
    fun rangeOfYear_respectsTimezone() {
        val hcmZone = ZoneId.of("Asia/Ho_Chi_Minh")
        val hcmCalc = DateRangeCalculator(Clock.systemDefaultZone(), hcmZone)

        val range = hcmCalc.rangeOfYear(2026)
        // 2026-01-01T00:00:00+07:00 = 2025-12-31T17:00:00Z
        assertEquals(1767200400000L, range.startMillis)
    }

    // --- timezone-aware ---

    @Test
    fun rangeOf_respectsTimezone() {
        val hcmZone = ZoneId.of("Asia/Ho_Chi_Minh")
        val hcmCalc = DateRangeCalculator(Clock.systemDefaultZone(), hcmZone)

        val range = hcmCalc.rangeOf(YearMonth.of(2026, 3))
        // 2026-03-01T00:00:00+07:00 = 2026-02-28T17:00:00Z
        assertEquals(1772298000000L, range.startMillis)
    }

    // --- round-trip ---

    @Test
    fun previousThenNext_returnsOriginal() {
        val original = YearMonth.of(2026, 6)
        val roundTrip = calculator.nextMonth(calculator.previousMonth(original))
        assertEquals(original, roundTrip)
    }

    @Test
    fun nextThenPrevious_returnsOriginal() {
        val original = YearMonth.of(2026, 6)
        val roundTrip = calculator.previousMonth(calculator.nextMonth(original))
        assertEquals(original, roundTrip)
    }
}
