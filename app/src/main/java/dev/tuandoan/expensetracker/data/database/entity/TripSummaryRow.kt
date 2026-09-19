package dev.tuandoan.expensetracker.data.database.entity

/**
 * Summary row for a trip: total spend (expenses only, null if no expenses) and transaction count.
 */
data class TripSummaryRow(
    val tripId: Long,
    val total: Long?,
    val count: Int,
)
