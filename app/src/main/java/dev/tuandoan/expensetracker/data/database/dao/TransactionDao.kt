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
        ORDER BY timestamp DESC
    """,
    )
    fun getTransactions(
        from: Long,
        to: Long,
        type: Int? = null,
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND (:type IS NULL OR type = :type)
        AND note LIKE '%' || :query || '%' ESCAPE '\' COLLATE NOCASE
        ORDER BY timestamp DESC
    """,
    )
    fun searchTransactions(
        from: Long,
        to: Long,
        query: String,
        type: Int? = null,
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:from IS NULL OR timestamp >= :from)
        AND (:to IS NULL OR timestamp < :to)
        AND (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR category_id = :categoryId)
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
    ): Flow<List<TransactionEntity>>

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
     * REVERT_TO_ORIGINAL_CATEGORY path of `TripRepository.deleteTrip` (ADR-001). For every
     * transaction in the trip: restore `category_id` to the caller-supplied snapshot id,
     * then clear `trip_id`, `original_category_id`, and `amount_foreign_minor`. Atomic
     * per row; the surrounding `runInTransaction` block makes the cross-row revert
     * all-or-nothing.
     *
     * `restoredCategoryId` is the id of the recreated (or still-present) category;
     * normally equal to the trip's `originalCategoryId` snapshot, but the caller passes
     * it explicitly to handle the rare reuse-of-id edge case.
     */
    @Query(
        """
        UPDATE transactions
        SET category_id = :restoredCategoryId,
            trip_id = NULL,
            original_category_id = NULL,
            amount_foreign_minor = NULL,
            updated_at = :now
        WHERE trip_id = :tripId
        """,
    )
    suspend fun revertTripAssignments(
        tripId: Long,
        restoredCategoryId: Long,
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
        GROUP BY currency_code
    """,
    )
    fun sumExpenseByCurrency(
        from: Long,
        to: Long,
    ): Flow<List<CurrencySumRow>>

    @Query(
        """
        SELECT currency_code AS currencyCode, SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND type = ${TransactionEntity.TYPE_EXPENSE}
        GROUP BY currency_code
    """,
    )
    suspend fun getExpenseTotalsByCurrency(
        from: Long,
        to: Long,
    ): List<CurrencySumRow>

    @Query(
        """
        SELECT currency_code AS currencyCode, SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND type = ${TransactionEntity.TYPE_INCOME}
        GROUP BY currency_code
    """,
    )
    fun sumIncomeByCurrency(
        from: Long,
        to: Long,
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
        GROUP BY month
        ORDER BY month ASC
    """,
    )
    suspend fun getMonthlyExpenseTotals(
        from: Long,
        to: Long,
        currencyCode: String,
    ): List<MonthlyTotalRow>

    @Query(
        """
        SELECT currency_code AS currencyCode, category_id AS categoryId, SUM(amount) AS total
        FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND type = :type
        GROUP BY currency_code, category_id
        ORDER BY currency_code ASC, total DESC
    """,
    )
    fun sumByCurrencyAndCategory(
        from: Long,
        to: Long,
        type: Int,
    ): Flow<List<CurrencyCategorySumRow>>
}
