package dev.tuandoan.expensetracker.core.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtil {
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())

    fun getCurrentMonthRange(): Pair<Long, Long> {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1)
        val endOfMonth = now.plusMonths(1).withDayOfMonth(1)

        val startMillis = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return startMillis to endMillis
    }

    fun formatTimestamp(timestamp: Long): String {
        val localDate = LocalDate.ofEpochDay(timestamp / (24 * 60 * 60 * 1000))
        return localDate.format(dateFormatter)
    }

    fun formatShortDate(timestamp: Long): String {
        val localDate = LocalDate.ofEpochDay(timestamp / (24 * 60 * 60 * 1000))
        return localDate.format(shortDateFormatter)
    }

    fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

    fun getTodayStartMillis(): Long {
        val today = LocalDate.now()
        return today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
