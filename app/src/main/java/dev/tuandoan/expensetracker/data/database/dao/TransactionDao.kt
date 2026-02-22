package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.tuandoan.expensetracker.data.database.entity.CurrencyCategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencySumRow
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

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

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
