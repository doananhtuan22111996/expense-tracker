package dev.tuandoan.expensetracker.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Conversions between `LocalDate.toEpochDay()` and start-of-day UTC millis.
 *
 * M3 `DateRangePicker` emits start-of-day **UTC** millis. Converting via a
 * UTC-anchored zone (not the system zone) is required so a non-UTC system
 * zone can't shift the date by one when the picker's millis is interpreted
 * as a local instant.
 *
 * Pure-Kotlin and side-effect-free; safe to call from any thread.
 */
object EpochDayConverters {
    fun utcMillisToEpochDay(millis: Long): Long =
        Instant
            .ofEpochMilli(millis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .toEpochDay()

    fun epochDayToUtcMillis(epochDay: Long): Long =
        LocalDate
            .ofEpochDay(epochDay)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
}
