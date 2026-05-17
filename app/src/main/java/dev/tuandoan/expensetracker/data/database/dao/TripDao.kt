package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.tuandoan.expensetracker.data.database.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY start_date_epoch_day DESC, id DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query(
        """
        SELECT * FROM trips
        WHERE start_date_epoch_day <= :nowEpochDay AND end_date_epoch_day >= :nowEpochDay
        ORDER BY created_at DESC, id DESC
        """,
    )
    fun observeActive(nowEpochDay: Long): Flow<List<TripEntity>>

    @Query(
        """
        SELECT * FROM trips
        WHERE start_date_epoch_day > :nowEpochDay
        ORDER BY start_date_epoch_day ASC, id ASC
        """,
    )
    fun observeUpcoming(nowEpochDay: Long): Flow<List<TripEntity>>

    @Query(
        """
        SELECT * FROM trips
        WHERE end_date_epoch_day < :nowEpochDay
        ORDER BY end_date_epoch_day DESC, id DESC
        """,
    )
    fun observePast(nowEpochDay: Long): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeById(id: Long): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): TripEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TripEntity): Long

    @Update
    suspend fun update(entity: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM trips ORDER BY start_date_epoch_day DESC, id DESC")
    suspend fun getAllList(): List<TripEntity>

    @Insert
    suspend fun insertAll(list: List<TripEntity>)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}
