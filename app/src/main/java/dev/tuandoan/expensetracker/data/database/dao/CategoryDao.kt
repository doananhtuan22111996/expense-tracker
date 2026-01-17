package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategories(type: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert
    suspend fun insertAll(list: List<CategoryEntity>)
}
