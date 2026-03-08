package dev.tuandoan.expensetracker.domain.repository

import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryWithCount
import dev.tuandoan.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(type: TransactionType): Flow<List<Category>>

    suspend fun getCategory(id: Long): Category?

    suspend fun createCategory(
        name: String,
        type: TransactionType,
        iconKey: String?,
        colorKey: String?,
    ): Long

    suspend fun updateCategory(
        id: Long,
        name: String,
        iconKey: String?,
        colorKey: String?,
    )

    suspend fun deleteCategory(id: Long)

    fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>>
}
