package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.tuandoan.expensetracker.data.database.entity.CategorySumRow
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
        SELECT SUM(amount) FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND type = ${TransactionEntity.TYPE_EXPENSE}
    """,
    )
    fun sumExpense(
        from: Long,
        to: Long,
    ): Flow<Long?>

    @Query(
        """
        SELECT SUM(amount) FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND type = ${TransactionEntity.TYPE_INCOME}
    """,
    )
    fun sumIncome(
        from: Long,
        to: Long,
    ): Flow<Long?>

    @Query(
        """
        SELECT category_id as categoryId, SUM(amount) as total
        FROM transactions
        WHERE timestamp >= :from AND timestamp < :to
        AND type = :type
        GROUP BY category_id
        ORDER BY total DESC
    """,
    )
    fun sumByCategory(
        from: Long,
        to: Long,
        type: Int,
    ): Flow<List<CategorySumRow>>
}
