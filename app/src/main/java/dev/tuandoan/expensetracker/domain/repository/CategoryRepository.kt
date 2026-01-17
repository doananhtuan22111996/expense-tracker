package dev.tuandoan.expensetracker.domain.repository

import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(type: TransactionType): Flow<List<Category>>

    suspend fun getCategory(id: Long): Category?
}
