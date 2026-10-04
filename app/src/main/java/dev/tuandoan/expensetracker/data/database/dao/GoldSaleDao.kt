package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.tuandoan.expensetracker.data.database.entity.GoldSaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoldSaleDao {
    @Query("SELECT * FROM gold_sales ORDER BY sale_date_millis DESC")
    fun observeAll(): Flow<List<GoldSaleEntity>>

    @Query("SELECT * FROM gold_sales ORDER BY sale_date_millis DESC")
    suspend fun getAll(): List<GoldSaleEntity>

    @Query("SELECT * FROM gold_sales WHERE id = :id")
    suspend fun getById(id: Long): GoldSaleEntity?

    @Query("SELECT * FROM gold_sales WHERE holding_id = :holdingId")
    suspend fun getByHoldingId(holdingId: Long): List<GoldSaleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GoldSaleEntity): Long

    @Query("DELETE FROM gold_sales WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insertAll(list: List<GoldSaleEntity>)

    @Query("DELETE FROM gold_sales")
    suspend fun deleteAll()
}
