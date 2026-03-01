package dev.tuandoan.expensetracker.domain.model

/**
 * Represents a half-open time range [startMillis, endMillisExclusive).
 * All DAO queries use `timestamp >= start AND timestamp < end`.
 */
data class DateRange(
    val startMillis: Long,
    val endMillisExclusive: Long,
)
