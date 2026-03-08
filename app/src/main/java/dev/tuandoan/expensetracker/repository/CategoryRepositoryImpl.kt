package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryWithCount
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
        private val transactionDao: TransactionDao,
        private val transactionRunner: TransactionRunner,
    ) : CategoryRepository {
        override fun observeCategories(type: TransactionType): Flow<List<Category>> =
            categoryDao
                .getCategories(type.toInt())
                .map { entities ->
                    entities.map { it.toDomain() }
                }

        override suspend fun getCategory(id: Long): Category? = categoryDao.getById(id)?.toDomain()

        override suspend fun createCategory(
            name: String,
            type: TransactionType,
            iconKey: String?,
            colorKey: String?,
        ): Long {
            val existing = categoryDao.getByNameAndType(name, type.toInt())
            if (existing != null) {
                throw IllegalArgumentException("A category with this name already exists")
            }
            val entity =
                CategoryEntity(
                    name = name,
                    type = type.toInt(),
                    iconKey = iconKey,
                    colorKey = colorKey,
                    isDefault = false,
                )
            return categoryDao.insert(entity)
        }

        override suspend fun updateCategory(
            id: Long,
            name: String,
            iconKey: String?,
            colorKey: String?,
        ) {
            val existing =
                categoryDao.getById(id)
                    ?: throw IllegalStateException("Category not found")
            val duplicate = categoryDao.getByNameAndType(name, existing.type)
            if (duplicate != null && duplicate.id != id) {
                throw IllegalArgumentException("A category with this name already exists")
            }
            val updated =
                existing.copy(
                    name = name,
                    iconKey = iconKey,
                    colorKey = colorKey,
                )
            categoryDao.update(updated)
        }

        override suspend fun deleteCategory(id: Long) {
            val category =
                categoryDao.getById(id)
                    ?: throw IllegalStateException("Category not found")
            if (category.isDefault) {
                throw IllegalStateException("Cannot delete default category")
            }
            val uncategorized =
                categoryDao.getAll().firstOrNull {
                    it.name == UNCATEGORIZED_NAME && it.type == category.type
                } ?: throw IllegalStateException("Uncategorized fallback category not found")

            transactionRunner.runInTransaction {
                transactionDao.reassignCategory(fromId = id, toId = uncategorized.id)
                categoryDao.deleteNonDefault(id)
            }
        }

        override fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>> =
            categoryDao.getCategoriesWithCount().map { rows ->
                rows.map { row ->
                    CategoryWithCount(
                        category =
                            Category(
                                id = row.id,
                                name = row.name,
                                type = TransactionType.fromInt(row.type),
                                iconKey = row.iconKey,
                                colorKey = row.colorKey,
                                isDefault = row.isDefault,
                            ),
                        transactionCount = row.transactionCount,
                    )
                }
            }

        companion object {
            const val UNCATEGORIZED_NAME = "Uncategorized"
        }
    }
