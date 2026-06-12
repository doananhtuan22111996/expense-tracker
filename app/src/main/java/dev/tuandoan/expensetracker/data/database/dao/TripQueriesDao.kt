package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import dev.tuandoan.expensetracker.data.database.entity.DailyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TripCategorySumRow
import kotlinx.coroutines.flow.Flow

/**
 * Read-only aggregations scoped to a single Trip (T1.4). Home currency only, per FR-05 +
 * ADR-002 — `transactions.amount` already carries the home equivalent for foreign-currency
 * transactions, so plain `SUM(amount)` aggregates correctly without FX math.
 *
 * All queries return `Flow` so the Trip detail screen reflects new transactions in real
 * time.
 */
@Dao
interface TripQueriesDao {
    /** Total spend in home minor units. `null` when the trip has no transactions yet. */
    @Query("SELECT SUM(amount) FROM transactions WHERE trip_id = :tripId")
    fun observeTotal(tripId: Long): Flow<Long?>

    /** Number of transactions attached to the trip. `0` when empty. */
    @Query("SELECT COUNT(*) FROM transactions WHERE trip_id = :tripId")
    fun observeTransactionCount(tripId: Long): Flow<Int>

    /**
     * Day-by-day totals for the bar chart. `date` is `yyyy-MM-dd` in device-local time.
     * Empty list when the trip has no transactions.
     */
    @Query(
        """
        SELECT
            strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch', 'localtime')) AS date,
            SUM(amount) AS total
        FROM transactions
        WHERE trip_id = :tripId
        GROUP BY date
        ORDER BY date ASC
        """,
    )
    fun observeDailyTotals(tripId: Long): Flow<List<DailyTotalRow>>

    /**
     * Per-category totals for the donut. UI joins to `Category` for name + color when
     * rendering. Sorted descending so the largest slice draws first.
     */
    @Query(
        """
        SELECT category_id AS categoryId, SUM(amount) AS total
        FROM transactions
        WHERE trip_id = :tripId
        GROUP BY category_id
        ORDER BY total DESC
        """,
    )
    fun observeCategoryBreakdown(tripId: Long): Flow<List<TripCategorySumRow>>

    /**
     * Emits `true` when at least one transaction linked to this trip has a non-null
     * `amount_foreign_minor`. Used by [CreateEditTripViewModel] to lock the foreign-
     * currency code picker — changing the code after FX amounts are stored would make
     * the recorded amounts uninterpretable (T3.7).
     */
    @Query(
        "SELECT COUNT(*) > 0 FROM transactions WHERE trip_id = :tripId AND amount_foreign_minor IS NOT NULL",
    )
    fun observeHasForeignTransactions(tripId: Long): Flow<Boolean>
}
