package dev.tuandoan.expensetracker.data.database.entity

/**
 * One row per category that has at least one transaction in the trip, summed in home
 * currency. UI joins to `Category` for color/name when rendering the donut + legend.
 */
data class TripCategorySumRow(
    val categoryId: Long,
    val total: Long,
)
