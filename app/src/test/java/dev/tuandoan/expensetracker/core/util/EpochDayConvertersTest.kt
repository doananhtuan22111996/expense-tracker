package dev.tuandoan.expensetracker.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class EpochDayConvertersTest {
    @Test
    fun millisToEpochDay_isUtcAnchored_2026_05_19() {
        // 2026-05-19T00:00:00Z in millis since epoch.
        val millis =
            LocalDate
                .of(2026, 5, 19)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()

        val expected = LocalDate.of(2026, 5, 19).toEpochDay()
        assertEquals(expected, EpochDayConverters.utcMillisToEpochDay(millis))
    }

    @Test
    fun epochDayToMillis_isUtcAnchored_epochZero() {
        // LocalDate(1970-01-01).toEpochDay() == 0 by definition.
        assertEquals(0L, EpochDayConverters.epochDayToUtcMillis(0L))
    }

    @Test
    fun roundTrip_millisToEpochDayAndBack_isIdentity() {
        val millis =
            LocalDate
                .of(2026, 12, 31)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()

        val back = EpochDayConverters.epochDayToUtcMillis(EpochDayConverters.utcMillisToEpochDay(millis))
        assertEquals(millis, back)
    }

    @Test
    fun roundTrip_epochDayToMillisAndBack_isIdentity() {
        val epochDay = LocalDate.of(2026, 5, 19).toEpochDay()

        val back = EpochDayConverters.utcMillisToEpochDay(EpochDayConverters.epochDayToUtcMillis(epochDay))
        assertEquals(epochDay, back)
    }
}
