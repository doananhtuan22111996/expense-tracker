package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.repository.mapper.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
    ) : CategoryRepository {
        override fun observeCategories(type: TransactionType): Flow<List<Category>> =
            categoryDao
                .getCategories(type.toInt())
                .map { entities ->
                    entities.map { it.toDomain() }
                }

        override suspend fun getCategory(id: Long): Category? = categoryDao.getById(id)?.toDomain()
    }
