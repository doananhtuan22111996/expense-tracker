package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.model.CategoryTotal
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import dev.tuandoan.expensetracker.repository.mapper.toDomain
import dev.tuandoan.expensetracker.repository.mapper.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransactionRepositoryImpl
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val categoryDao: CategoryDao,
        private val timeProvider: TimeProvider,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : TransactionRepository {
        override fun observeTransactions(
            from: Long,
            to: Long,
            filterType: TransactionType?,
        ): Flow<List<Transaction>> =
            transactionDao
                .getTransactions(from, to, filterType?.toInt())
                .combine(
                    // Get all categories to map to transactions
                    categoryDao
                        .getCategories(TransactionType.EXPENSE.toInt())
                        .combine(categoryDao.getCategories(TransactionType.INCOME.toInt())) { expense, income ->
                            (expense + income).associateBy { it.id }
                        },
                ) { transactions, categoriesMap ->
                    transactions.mapNotNull { transaction ->
                        categoriesMap[transaction.categoryId]?.let { category ->
                            transaction.toDomain(category.toDomain())
                        }
                    }
                }.flowOn(ioDispatcher)

        override suspend fun addTransaction(
            type: TransactionType,
            amount: Long,
            categoryId: Long,
            note: String?,
            timestamp: Long,
            currencyCode: String,
        ): Long =
            withContext(ioDispatcher) {
                val now = timeProvider.currentTimeMillis()
                val entity =
                    TransactionEntity(
                        type = type.toInt(),
                        amount = amount,
                        currencyCode = currencyCode,
                        categoryId = categoryId,
                        note = note,
                        timestamp = timestamp,
                        createdAt = now,
                        updatedAt = now,
                    )
                transactionDao.insert(entity)
            }

        override suspend fun updateTransaction(transaction: Transaction): Unit =
            withContext(ioDispatcher) {
                val updatedEntity = transaction.toEntity().copy(updatedAt = timeProvider.currentTimeMillis())
                transactionDao.update(updatedEntity)
            }

        override suspend fun deleteTransaction(id: Long): Unit =
            withContext(ioDispatcher) {
                transactionDao.deleteById(id)
            }

        override suspend fun getTransaction(id: Long): Transaction? =
            withContext(ioDispatcher) {
                val entity = transactionDao.getById(id) ?: return@withContext null
                val category = categoryDao.getById(entity.categoryId)?.toDomain() ?: return@withContext null
                entity.toDomain(category)
            }

        override fun observeMonthlySummary(
            from: Long,
            to: Long,
        ): Flow<MonthlySummary> =
            combine(
                transactionDao.sumExpense(from, to),
                transactionDao.sumIncome(from, to),
                transactionDao.sumByCategory(from, to, TransactionType.EXPENSE.toInt()),
                // Add categories as a Flow to avoid blocking calls
                categoryDao.getCategories(TransactionType.EXPENSE.toInt()),
            ) { expenseSum, incomeSum, expenseCategories, categories ->
                val totalExpense = expenseSum ?: 0L
                val totalIncome = incomeSum ?: 0L
                val balance = totalIncome - totalExpense

                // Create category lookup map for efficient access
                val categoryMap = categories.associateBy { it.id }

                // Get category details for top expenses using non-blocking lookup
                val topExpenseCategories =
                    expenseCategories.take(5).mapNotNull { categorySum ->
                        categoryMap[categorySum.categoryId]?.let { categoryEntity ->
                            CategoryTotal(
                                category = categoryEntity.toDomain(),
                                total = categorySum.total,
                            )
                        }
                    }

                MonthlySummary(
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    balance = balance,
                    topExpenseCategories = topExpenseCategories,
                )
            }.flowOn(ioDispatcher)
    }
