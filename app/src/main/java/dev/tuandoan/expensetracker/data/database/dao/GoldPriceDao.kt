package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoldPriceDao {
    @Query("SELECT * FROM gold_prices")
    fun observeAll(): Flow<List<GoldPriceEntity>>

    @Query("SELECT * FROM gold_prices")
    suspend fun getAll(): List<GoldPriceEntity>

    @Query("SELECT * FROM gold_prices WHERE type = :type AND unit = :unit")
    suspend fun getByTypeAndUnit(
        type: String,
        unit: String,
    ): GoldPriceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GoldPriceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<GoldPriceEntity>)

    @Query("DELETE FROM gold_prices")
    suspend fun deleteAll()
}
