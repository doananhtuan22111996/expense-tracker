package dev.tuandoan.expensetracker.data.database.entity

/**
 * One row per day-grain bucket from the trip's transactions, summed in home currency.
 *
 * `date` is `yyyy-MM-dd` derived from `timestamp` via SQLite `strftime` in the device's
 * local time zone — same convention as `MonthlyTotalRow` but day-grain.
 */
data class DailyTotalRow(
    val date: String,
    val total: Long,
)
