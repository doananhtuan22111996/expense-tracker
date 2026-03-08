package dev.tuandoan.expensetracker.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        val localDate =
            Instant
                .ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        return localDate.format(dateFormatter)
    }

    fun formatShortDate(timestamp: Long): String {
        val localDate =
            Instant
                .ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        return localDate.format(shortDateFormatter)
    }

    fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

    fun getTodayStartMillis(): Long {
        val today = LocalDate.now()
        return today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Formats a next-due timestamp as a human-readable relative label.
     *
     * @param nextDueMillis the next-due epoch milliseconds
     * @param nowMillis the current epoch milliseconds
     * @return a label such as "Due today", "Due tomorrow", "Overdue by 3 day(s)",
     *         "Due in 5 days", or a short date like "Mar 15"
     */
    fun formatNextDueLabel(
        nextDueMillis: Long,
        nowMillis: Long,
    ): String {
        val daysDiff = TimeUnit.MILLISECONDS.toDays(nextDueMillis - nowMillis).toInt()
        return when {
            daysDiff < 0 -> {
                val absDays = -daysDiff
                "Overdue by $absDays day${if (absDays != 1) "s" else ""}"
            }
            daysDiff == 0 -> "Due today"
            daysDiff == 1 -> "Due tomorrow"
            daysDiff <= 7 -> "Due in $daysDiff days"
            else -> formatShortDate(nextDueMillis)
        }
    }
}
