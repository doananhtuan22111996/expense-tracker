package dev.tuandoan.expensetracker

import dev.tuandoan.expensetracker.core.util.DateTimeUtil
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unit tests for DateTimeUtil
 * Critical business logic for date handling and month range calculations
 */
class DateTimeUtilTest {
    @Test
    fun getCurrentMonthRange_returnsValidRange() {
        val (start, end) = DateTimeUtil.getCurrentMonthRange()

        // Start should be before end
        assertTrue("Start should be before end", start < end)

        // Range should span about a month (28-31 days)
        val rangeDays = (end - start) / (1000 * 60 * 60 * 24)
        assertTrue("Range should be 28-31 days", rangeDays in 28..31)
    }

    @Test
    fun getCurrentMonthRange_startIsFirstDayOfMonth() {
        val (start, _) = DateTimeUtil.getCurrentMonthRange()

        // Convert back to LocalDate using Instant for accuracy
        val startDate =
            java.time.Instant
                .ofEpochMilli(start)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        assertEquals("Start should be first day of month", 1, startDate.dayOfMonth)
    }

    @Test
    fun getCurrentMonthRange_endIsFirstDayOfNextMonth() {
        val (_, end) = DateTimeUtil.getCurrentMonthRange()

        // Convert back to LocalDate using Instant for accuracy
        val endDate =
            java.time.Instant
                .ofEpochMilli(end)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        assertEquals("End should be first day of next month", 1, endDate.dayOfMonth)
    }

    @Test
    fun formatTimestamp_validTimestamp_returnsFormattedDate() {
        // January 15, 2024 - use epoch days calculation matching implementation
        val januaryDate = LocalDate.of(2024, 1, 15)
        val timestamp = januaryDate.toEpochDay() * (24 * 60 * 60 * 1000)

        val formatted = DateTimeUtil.formatTimestamp(timestamp)

        // Should contain the month, day, and year
        assertTrue("Should contain Jan", formatted.contains("Jan"))
        assertTrue("Should contain 15", formatted.contains("15"))
        assertTrue("Should contain 2024", formatted.contains("2024"))
    }

    @Test
    fun formatShortDate_validTimestamp_returnsShortFormat() {
        // December 25, 2024 - use epoch days calculation matching implementation
        val christmasDate = LocalDate.of(2024, 12, 25)
        val timestamp = christmasDate.toEpochDay() * (24 * 60 * 60 * 1000)

        val formatted = DateTimeUtil.formatShortDate(timestamp)

        // Should contain month and day but not year
        assertTrue("Should contain Dec", formatted.contains("Dec"))
        assertTrue("Should contain 25", formatted.contains("25"))
        assertFalse("Should not contain 2024", formatted.contains("2024"))
    }

    @Test
    fun getCurrentTimeMillis_returnsReasonableValue() {
        val currentTime = DateTimeUtil.getCurrentTimeMillis()
        val systemTime = System.currentTimeMillis()

        // Should be within 1 second of system time
        val difference = Math.abs(currentTime - systemTime)
        assertTrue("Should be close to system time", difference < 1000)
    }

    @Test
    fun getTodayStartMillis_returnsMidnight() {
        val todayStart = DateTimeUtil.getTodayStartMillis()
        val currentTime = DateTimeUtil.getCurrentTimeMillis()

        // Today start should be before or equal to current time
        assertTrue("Today start should be before current time", todayStart <= currentTime)

        // Should be within 24 hours
        val difference = currentTime - todayStart
        assertTrue("Should be within 24 hours", difference < 24 * 60 * 60 * 1000)

        // Convert to LocalDateTime and check it's today at 00:00:00
        val todayStartDate =
            java.time.Instant
                .ofEpochMilli(todayStart)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

        assertEquals("Should be at midnight (hour)", 0, todayStartDate.hour)
        assertEquals("Should be at midnight (minute)", 0, todayStartDate.minute)
        assertEquals("Should be at midnight (second)", 0, todayStartDate.second)
    }

    @Test
    fun dateFormatting_differentMonths_handlesCorrectly() {
        val testCases =
            mapOf(
                1 to "Jan",
                2 to "Feb",
                3 to "Mar",
                6 to "Jun",
                12 to "Dec",
            )

        testCases.forEach { (month, expectedAbbrev) ->
            val date = LocalDate.of(2024, month, 15)
            val timestamp = date.toEpochDay() * (24 * 60 * 60 * 1000)

            val formatted = DateTimeUtil.formatTimestamp(timestamp)
            assertTrue(
                "Month $month should format as $expectedAbbrev",
                formatted.contains(expectedAbbrev),
            )
        }
    }

    @Test
    fun monthRangeCalculation_isConsistent() {
        // Verify the range is consistent when called multiple times
        val (start1, end1) = DateTimeUtil.getCurrentMonthRange()

        // Wait a brief moment and get range again
        Thread.sleep(10)
        val (start2, end2) = DateTimeUtil.getCurrentMonthRange()

        // Should be the same since we're in the same month
        assertEquals("Start times should be equal", start1, start2)
        assertEquals("End times should be equal", end1, end2)
    }

    @Test
    fun timestampFormatting_edgeCases_handlesGracefully() {
        // Test epoch start (Jan 1, 1970)
        val epochStart = 0L
        val formatted = DateTimeUtil.formatTimestamp(epochStart)
        assertNotNull("Should handle epoch start", formatted)
        assertTrue("Should not be empty", formatted.isNotEmpty())

        // Test a recent date using correct epoch day calculation
        val testDate = LocalDate.of(2024, 6, 15)
        val timestamp = testDate.toEpochDay() * (24 * 60 * 60 * 1000)

        val formattedTest = DateTimeUtil.formatTimestamp(timestamp)
        assertTrue("Should contain 2024", formattedTest.contains("2024"))
        assertTrue("Should contain Jun", formattedTest.contains("Jun"))
        assertTrue("Should contain 15", formattedTest.contains("15"))
    }

    @Test
    fun formatAndParse_roundTrip_maintainsDateConsistency() {
        // Test that formatting and extracting date components works consistently
        val testDate = LocalDate.of(2024, 3, 20)
        val timestamp = testDate.toEpochDay() * (24 * 60 * 60 * 1000)

        val formatted = DateTimeUtil.formatTimestamp(timestamp)
        val shortFormatted = DateTimeUtil.formatShortDate(timestamp)

        assertTrue("Full format should contain year", formatted.contains("2024"))
        assertTrue("Full format should contain Mar", formatted.contains("Mar"))
        assertTrue("Full format should contain 20", formatted.contains("20"))

        assertTrue("Short format should contain Mar", shortFormatted.contains("Mar"))
        assertTrue("Short format should contain 20", shortFormatted.contains("20"))
        assertFalse("Short format should not contain year", shortFormatted.contains("2024"))
    }
}
