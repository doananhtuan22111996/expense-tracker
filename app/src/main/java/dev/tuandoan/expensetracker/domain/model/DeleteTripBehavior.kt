package dev.tuandoan.expensetracker.domain.model

/**
 * Behavior selector for `TripRepository.deleteTrip(id, behavior)`.
 *
 * `UNTAG` clears `trip_id` on every transaction belonging to the trip and deletes the
 * trip row. Categories are preserved as-is.
 *
 * `REVERT_TO_ORIGINAL_CATEGORY` is only valid when [Trip.isConversionOrigin]. It restores
 * the snapshot category, resets each transaction's `category_id` from
 * `original_category_id`, clears trip-related columns, and deletes the trip row. Atomic
 * via `TransactionRunner.runInTransaction` per ADR-001.
 */
enum class DeleteTripBehavior {
    UNTAG,
    REVERT_TO_ORIGINAL_CATEGORY,
}
