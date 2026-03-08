package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.CategoryWithCountRow
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategories(type: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(list: List<CategoryEntity>)

    @Insert
    suspend fun insert(entity: CategoryEntity): Long

    @Update
    suspend fun update(entity: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id AND is_default = 0")
    suspend fun deleteNonDefault(id: Long): Int

    @Query(
        """
        SELECT c.*, COUNT(t.id) AS transaction_count
        FROM categories c
        LEFT JOIN transactions t ON t.category_id = c.id
        GROUP BY c.id
        ORDER BY c.is_default DESC, c.name ASC
    """,
    )
    fun getCategoriesWithCount(): Flow<List<CategoryWithCountRow>>

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getByNameAndType(
        name: String,
        type: Int,
    ): CategoryEntity?

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
