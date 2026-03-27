package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.tuandoan.expensetracker.data.database.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY next_due_millis ASC")
    fun getAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE next_due_millis <= :nowMillis AND is_active = 1")
    suspend fun getDue(nowMillis: Long): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecurringTransactionEntity): Long

    @Update
    suspend fun update(entity: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE recurring_transactions SET next_due_millis = :nextDue, updated_at = :now WHERE id = :id")
    suspend fun updateNextDue(
        id: Long,
        nextDue: Long,
        now: Long,
    )

    @Query("UPDATE recurring_transactions SET is_active = :active, updated_at = :now WHERE id = :id")
    suspend fun setActive(
        id: Long,
        active: Boolean,
        now: Long,
    )

    @Query("SELECT * FROM recurring_transactions ORDER BY next_due_millis ASC")
    suspend fun getAllList(): List<RecurringTransactionEntity>

    @Insert
    suspend fun insertAll(list: List<RecurringTransactionEntity>)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()
}
