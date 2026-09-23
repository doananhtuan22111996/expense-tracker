package dev.tuandoan.expensetracker.domain.model

/**
 * Summary metrics for the travel portfolio, mirroring [GoldPortfolioSummary].
 * Aggregates all trip expenses and counts across active, upcoming, and past trips.
 */
data class TripPortfolioSummary(
    val totalSpend: Long,
    val currencyCode: String,
    val activeTripsCount: Int,
    val upcomingTripsCount: Int,
    val pastTripsCount: Int,
    val totalTransactionsCount: Int,
    val activeTripHighlight: ActiveTripHighlight? = null,
)

/**
 * Quick-status highlight when a trip is currently active (today falls within trip date range).
 */
data class ActiveTripHighlight(
    val trip: Trip,
    val totalSpentLabel: String?,
    val totalDays: Int,
    val currentDay: Int,
)
