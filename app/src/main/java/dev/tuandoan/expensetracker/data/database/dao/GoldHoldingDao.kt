package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoldHoldingDao {
    @Query("SELECT * FROM gold_holdings ORDER BY buy_date_millis DESC")
    fun observeAll(): Flow<List<GoldHoldingEntity>>

    @Query("SELECT * FROM gold_holdings ORDER BY buy_date_millis DESC")
    suspend fun getAll(): List<GoldHoldingEntity>

    @Query("SELECT * FROM gold_holdings WHERE id = :id")
    suspend fun getById(id: Long): GoldHoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GoldHoldingEntity): Long

    @Update
    suspend fun update(entity: GoldHoldingEntity)

    @Query("DELETE FROM gold_holdings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insertAll(list: List<GoldHoldingEntity>)

    @Query("DELETE FROM gold_holdings")
    suspend fun deleteAll()
}
