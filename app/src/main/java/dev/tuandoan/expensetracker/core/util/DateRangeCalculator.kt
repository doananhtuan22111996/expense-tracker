package dev.tuandoan.expensetracker.core.util

import dev.tuandoan.expensetracker.domain.model.DateRange
import java.time.Clock
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Computes calendar-month and calendar-year [DateRange]s using an injectable
 * [Clock] and [ZoneId] so that boundaries are deterministic in tests.
 */
@Singleton
class DateRangeCalculator
    @Inject
    constructor(
        private val clock: Clock,
        private val zoneId: ZoneId,
    ) {
        /** The calendar month that contains "now". */
        fun currentMonth(): YearMonth = YearMonth.now(clock.withZone(zoneId))

        /** The calendar year that contains "now". */
        fun currentYear(): Int = Year.now(clock.withZone(zoneId)).value

        /** Half-open millis range for the given [yearMonth]. */
        fun rangeOf(yearMonth: YearMonth): DateRange {
            val start =
                yearMonth
                    .atDay(1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
            val end =
                yearMonth
                    .plusMonths(1)
                    .atDay(1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
            return DateRange(startMillis = start, endMillisExclusive = end)
        }

        /** Half-open millis range for the given [year] (Jan 1 to Jan 1 next year). */
        fun rangeOfYear(year: Int): DateRange {
            val start =
                LocalDate
                    .of(year, 1, 1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
            val end =
                LocalDate
                    .of(year + 1, 1, 1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
            return DateRange(startMillis = start, endMillisExclusive = end)
        }

        /** Month immediately before [yearMonth]. */
        fun previousMonth(yearMonth: YearMonth): YearMonth = yearMonth.minusMonths(1)

        /** Month immediately after [yearMonth]. */
        fun nextMonth(yearMonth: YearMonth): YearMonth = yearMonth.plusMonths(1)

        /** Format a [YearMonth] for display, e.g. "Feb 2026". */
        fun displayLabel(yearMonth: YearMonth): String {
            val month =
                yearMonth.month.name
                    .take(3)
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
            return "$month ${yearMonth.year}"
        }
    }
