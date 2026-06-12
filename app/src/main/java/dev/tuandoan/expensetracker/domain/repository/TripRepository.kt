package dev.tuandoan.expensetracker.domain.repository

import dev.tuandoan.expensetracker.data.database.entity.DailyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TripCategorySumRow
import dev.tuandoan.expensetracker.domain.model.DeleteTripBehavior
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.model.TripFilter
import kotlinx.coroutines.flow.Flow

/**
 * Persistence + domain operations for Trips (v3.13.0). All write paths run inside a
 * single `TransactionRunner.runInTransaction` block so revert / delete is atomic per
 * ADR-001.
 *
 * `commitConversion(draft: ConversionDraft)` is deliberately not on this interface yet —
 * it lands with T4.6 (Epic 4 wizard commit) where the per-row migration semantics + the
 * `attachToTrip` DAO helper land together.
 */
interface TripRepository {
    /** Cold flow of trips matching [filter]. Time-relative filters close over `now`. */
    fun observeTrips(filter: TripFilter): Flow<List<Trip>>

    /** Cold flow that emits the trip with [id] (or `null` if it doesn't exist or was deleted). */
    fun observeTripById(id: Long): Flow<Trip?>

    /** One-shot lookup. Returns `null` when no trip with [id] exists. */
    suspend fun getTripById(id: Long): Trip?

    /**
     * Create a new home-currency or foreign-currency trip. Snapshot fields stay null —
     * only the conversion wizard creates conversion-origin trips, and that path lives in
     * `commitConversion` (T4.6).
     *
     * Returns the new trip's id.
     */
    suspend fun createTrip(
        name: String,
        destination: String?,
        startDateEpochDay: Long,
        endDateEpochDay: Long,
        foreignCurrencyCode: String?,
        foreignToHomeRate: Double?,
    ): Long

    /**
     * Persist edits to an existing trip. Caller is responsible for keeping the snapshot
     * fields untouched on conversion-origin trips (UI surfaces them as read-only).
     *
     * No FK validation here — the per-tx `amountForeignMinor` invariant
     * ("can't change `foreignCurrencyCode` if any tx has a foreign amount") lives in
     * `CreateEditTripViewModel.validate()` per ADR-002.
     */
    suspend fun updateTrip(trip: Trip)

    /**
     * Delete a trip with the requested [behavior] (ADR-001). Atomic.
     *
     * - [DeleteTripBehavior.UNTAG] clears `trip_id` on every trip transaction (preserving
     *   `original_category_id` + `amount_foreign_minor` so the user can re-tag), then
     *   deletes the trip row.
     * - [DeleteTripBehavior.REVERT_TO_ORIGINAL_CATEGORY] is only valid for
     *   conversion-origin trips. Recreates the source category from the snapshot if
     *   it was deleted on commit, restores each transaction's `category_id`, clears all
     *   trip-related columns, then deletes the trip row.
     *
     * @throws IllegalArgumentException when REVERT is requested on a non-conversion-origin trip.
     */
    suspend fun deleteTrip(
        id: Long,
        behavior: DeleteTripBehavior,
    )

    /**
     * Total spend for the trip in home minor units. Emits `null` when the trip has no
     * transactions yet (so VM can distinguish "no data" from "0 spent").
     */
    fun observeTripTotal(tripId: Long): Flow<Long?>

    /** Number of transactions attached to the trip. Emits `0` when empty. */
    fun observeTripTransactionCount(tripId: Long): Flow<Int>

    /** Day-by-day totals (`yyyy-MM-dd` localtime) for the bar chart. */
    fun observeTripDailyTotals(tripId: Long): Flow<List<DailyTotalRow>>

    /** Per-category totals for the donut, sorted descending by total. */
    fun observeTripCategoryBreakdown(tripId: Long): Flow<List<TripCategorySumRow>>

    /** All transactions belonging to the trip, newest first. */
    fun observeTripTransactions(tripId: Long): Flow<List<Transaction>>

    /**
     * Emits `true` when the trip has at least one transaction with a recorded foreign
     * amount. Used by the edit form to lock the [foreignCurrencyCode] picker (T3.7).
     */
    fun observeHasForeignTransactions(tripId: Long): Flow<Boolean>
}
