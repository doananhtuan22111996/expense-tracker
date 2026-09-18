package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.tuandoan.expensetracker.data.database.entity.CurrencyCategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencySumRow
import dev.tuandoan.expensetracker.data.database.entity.MonthlyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT * FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND (:type IS NULL OR type = :type)
        AND (:excludeTrips = 0 OR trip_id IS NULL)
        ORDER BY timestamp DESC
    """,
    )
    fun getTransactions(
        from: Long,
        to: Long,
        type: Int? = null,
        excludeTrips: Int = 0,
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND (:type IS NULL OR type = :type)
        AND (:excludeTrips = 0 OR trip_id IS NULL)
        AND note LIKE '%' || :query || '%' ESCAPE '\' COLLATE NOCASE
        ORDER BY timestamp DESC
    """,
    )
    fun searchTransactions(
        from: Long,
        to: Long,
        query: String,
        type: Int? = null,
        excludeTrips: Int = 0,
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:from IS NULL OR timestamp >= :from)
        AND (:to IS NULL OR timestamp < :to)
        AND (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR category_id = :categoryId)
        AND (:excludeTrips = 0 OR trip_id IS NULL)
        AND (:query = '' OR note LIKE '%' || :query || '%' ESCAPE '\' COLLATE NOCASE)
        ORDER BY timestamp DESC
    """,
    )
    fun searchTransactionsAdvanced(
        from: Long?,
        to: Long?,
        query: String,
        type: Int? = null,
        categoryId: Long? = null,
        excludeTrips: Int = 0,
    ): Flow<List<TransactionEntity>>

    /** All transactions belonging to a trip, newest first. Empty list when trip has no transactions. */
    @Query("SELECT * FROM transactions WHERE trip_id = :tripId ORDER BY timestamp DESC")
    fun observeByTripId(tripId: Long): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY timestamp ASC")
    suspend fun getAllOrdered(): List<TransactionEntity>

    @Insert
    suspend fun insertAll(list: List<TransactionEntity>)

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId")
    suspend fun countByCategoryId(categoryId: Long): Int

    @Query("UPDATE transactions SET category_id = :toId WHERE category_id = :fromId")
    suspend fun reassignCategory(
        fromId: Long,
        toId: Long,
    )

    /**
     * UNTAG path of `TripRepository.deleteTrip` (ADR-001). Clears `trip_id` on every
     * transaction belonging to the trip. Category, original_category_id, and
     * amount_foreign_minor are intentionally preserved — UNTAG does not undo a prior
     * conversion. Bumps `updated_at` so sync/backup reflect the change.
     */
    @Query(
        """
        UPDATE transactions
        SET trip_id = NULL, updated_at = :now
        WHERE trip_id = :tripId
        """,
    )
    suspend fun clearTripId(
        tripId: Long,
        now: Long,
    )

    /**
     * REVERT_TO_ORIGINAL_CATEGORY path of `TripRepository.deleteTrip` (ADR-001).
     * For transactions that were migrated during category-to-trip conversion
     * (`original_category_id IS NOT NULL`): restore `category_id` to the caller-supplied
     * snapshot id, and clear `original_category_id` and `amount_foreign_minor`.
     *
     * For any transactions added to the trip after conversion (`original_category_id IS NULL`),
     * their user-selected category and foreign amounts are preserved intact; they are simply
     * untagged from the trip (`trip_id = NULL`).
     *
     * Atomic per row; the surrounding `runInTransaction` block makes the cross-row revert
     * all-or-nothing.
     */
    @Query(
        """
        UPDATE transactions
        SET category_id = CASE
                WHEN original_category_id IS NOT NULL THEN :restoredCategoryId
                ELSE category_id
            END,
            amount_foreign_minor = CASE
                WHEN original_category_id IS NOT NULL THEN NULL
                ELSE amount_foreign_minor
            END,
            trip_id = NULL,
            original_category_id = NULL,
            updated_at = :now
        WHERE trip_id = :tripId
        """,
    )
    suspend fun revertTripAssignments(
        tripId: Long,
        restoredCategoryId: Long,
        now: Long,
    )

    /**
     * Conversion-wizard commit path (T4.6). For a single Migrate row decision: assigns
     * `trip_id`, updates `category_id` to the new category chosen by the user,
     * and stores `original_category_id` so the REVERT_TO_ORIGINAL_CATEGORY delete
     * path (ADR-001) can restore it later. `amount_foreign_minor` is untouched —
     * it was written by the add/edit screen at save time (T3.6). Bumps `updated_at`.
     */
    @Query(
        """
        UPDATE transactions
        SET trip_id = :tripId,
            category_id = :newCategoryId,
            original_category_id = :originalCategoryId,
            updated_at = :now
        WHERE id = :transactionId
        """,
    )
    suspend fun migrateToTrip(
        transactionId: Long,
        tripId: Long,
        newCategoryId: Long,
        originalCategoryId: Long,
        now: Long,
    )

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query(
        """
        SELECT currency_code AS currencyCode, SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND type = ${TransactionEntity.TYPE_EXPENSE}
        AND (:excludeTrips = 0 OR trip_id IS NULL)
        GROUP BY currency_code
    """,
    )
    fun sumExpenseByCurrency(
        from: Long,
        to: Long,
        excludeTrips: Int = 0,
    ): Flow<List<CurrencySumRow>>

    @Query(
        """
        SELECT currency_code AS currencyCode, SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND type = ${TransactionEntity.TYPE_EXPENSE}
        AND (:excludeTrips = 0 OR trip_id IS NULL)
        GROUP BY currency_code
    """,
    )
    suspend fun getExpenseTotalsByCurrency(
        from: Long,
        to: Long,
        excludeTrips: Int = 0,
    ): List<CurrencySumRow>

    @Query(
        """
        SELECT currency_code AS currencyCode, SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND type = ${TransactionEntity.TYPE_INCOME}
        AND (:excludeTrips = 0 OR trip_id IS NULL)
        GROUP BY currency_code
    """,
    )
    fun sumIncomeByCurrency(
        from: Long,
        to: Long,
        excludeTrips: Int = 0,
    ): Flow<List<CurrencySumRow>>

    @Query(
        """
        SELECT
            strftime('%m', datetime(timestamp / 1000, 'unixepoch', 'localtime')) AS month,
            SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND type = ${TransactionEntity.TYPE_EXPENSE}
        AND currency_code = :currencyCode
        AND (:excludeTrips = 0 OR trip_id IS NULL)
        GROUP BY month
        ORDER BY month ASC
    """,
    )
    suspend fun getMonthlyExpenseTotals(
        from: Long,
        to: Long,
        currencyCode: String,
        excludeTrips: Int = 0,
    ): List<MonthlyTotalRow>

    @Query(
        """
        SELECT currency_code AS currencyCode, category_id AS categoryId, SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND type = :type
        AND (:excludeTrips = 0 OR trip_id IS NULL)
        GROUP BY currency_code, category_id
        ORDER BY currency_code ASC, total DESC
    """,
    )
    fun sumByCurrencyAndCategory(
        from: Long,
        to: Long,
        type: Int,
        excludeTrips: Int = 0,
    ): Flow<List<CurrencyCategorySumRow>>
}
