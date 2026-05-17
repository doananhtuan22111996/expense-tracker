package dev.tuandoan.expensetracker.domain.model

/**
 * Filter shape for `TripRepository.observeTrips`. Time-relative variants close over
 * `nowEpochDay` so the repository stays pure (no `TimeProvider` injection on read paths)
 * and observation results are reproducible in tests.
 */
sealed interface TripFilter {
    /** Every trip, ordered most-recent-first by `start_date_epoch_day`. */
    data object All : TripFilter

    /** Trips where `start <= now <= end`. */
    data class Active(
        val nowEpochDay: Long,
    ) : TripFilter

    /** Trips where `start > now`. */
    data class Upcoming(
        val nowEpochDay: Long,
    ) : TripFilter

    /** Trips where `end < now`. */
    data class Past(
        val nowEpochDay: Long,
    ) : TripFilter
}
