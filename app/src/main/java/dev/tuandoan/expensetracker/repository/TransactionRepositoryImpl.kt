package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.CurrencyCategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencySumRow
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryTotal
import dev.tuandoan.expensetracker.domain.model.CurrencyMonthlySummary
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
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
                transactionDao.sumExpenseByCurrency(from, to),
                transactionDao.sumIncomeByCurrency(from, to),
                transactionDao.sumByCurrencyAndCategory(from, to, TransactionType.EXPENSE.toInt()),
                categoryDao.getCategories(TransactionType.EXPENSE.toInt()),
            ) { expenseByCurrency, incomeByCurrency, categorySums, categories ->
                buildMonthlySummary(expenseByCurrency, incomeByCurrency, categorySums, categories)
            }.flowOn(ioDispatcher)

        private fun buildMonthlySummary(
            expenseByCurrency: List<CurrencySumRow>,
            incomeByCurrency: List<CurrencySumRow>,
            categorySums: List<CurrencyCategorySumRow>,
            categories: List<CategoryEntity>,
        ): MonthlySummary {
            val categoryMap = categories.associateBy { it.id }

            // Collect all distinct currency codes from all data sources
            val allCurrencyCodes =
                (
                    expenseByCurrency.map { it.currencyCode } +
                        incomeByCurrency.map { it.currencyCode } +
                        categorySums.map { it.currencyCode }
                ).toSet()

            if (allCurrencyCodes.isEmpty()) {
                return MonthlySummary(currencySummaries = emptyList())
            }

            val expenseMap = expenseByCurrency.associateBy { it.currencyCode }
            val incomeMap = incomeByCurrency.associateBy { it.currencyCode }
            val categorySumsByCurrency = categorySums.groupBy { it.currencyCode }

            // Build currency ordering: SupportedCurrencies registry order first, then unknown alphabetically
            val registryOrder = SupportedCurrencies.all().map { it.code }
            val registryOrderMap = registryOrder.withIndex().associate { (index, code) -> code to index }

            val sortedCurrencyCodes =
                allCurrencyCodes.sortedWith(
                    compareBy<String> { code ->
                        registryOrderMap[code] ?: (registryOrder.size + 1)
                    }.thenBy { it },
                )

            val currencySummaries =
                sortedCurrencyCodes.map { currencyCode ->
                    val totalExpense = expenseMap[currencyCode]?.total ?: 0L
                    val totalIncome = incomeMap[currencyCode]?.total ?: 0L
                    val balance = totalIncome - totalExpense

                    val currencyCategorySums = categorySumsByCurrency[currencyCode] ?: emptyList()
                    val topExpenseCategories = buildTopCategories(currencyCategorySums, categoryMap)

                    CurrencyMonthlySummary(
                        currencyCode = currencyCode,
                        totalExpense = totalExpense,
                        totalIncome = totalIncome,
                        balance = balance,
                        topExpenseCategories = topExpenseCategories,
                    )
                }

            return MonthlySummary(currencySummaries = currencySummaries)
        }

        private fun buildTopCategories(
            categorySums: List<CurrencyCategorySumRow>,
            categoryMap: Map<Long, CategoryEntity>,
        ): List<CategoryTotal> {
            if (categorySums.isEmpty()) return emptyList()

            // Sort deterministically: amount DESC, then category id ASC
            val sorted =
                categorySums.sortedWith(
                    compareByDescending<CurrencyCategorySumRow> { it.total }
                        .thenBy { it.categoryId },
                )

            if (sorted.size <= TOP_CATEGORIES_LIMIT) {
                return sorted.map { row ->
                    CategoryTotal(
                        category = categoryMap[row.categoryId]?.toDomain() ?: unknownCategory(row.categoryId),
                        total = row.total,
                    )
                }
            }

            // Take top 5, aggregate the rest into "Other"
            val topRows = sorted.take(TOP_CATEGORIES_LIMIT)
            val otherRows = sorted.drop(TOP_CATEGORIES_LIMIT)

            val topCategories =
                topRows.map { row ->
                    CategoryTotal(
                        category = categoryMap[row.categoryId]?.toDomain() ?: unknownCategory(row.categoryId),
                        total = row.total,
                    )
                }

            val otherTotal = otherRows.sumOf { it.total }
            if (otherTotal > 0L) {
                return topCategories +
                    CategoryTotal(
                        category = OTHER_CATEGORY,
                        total = otherTotal,
                    )
            }

            return topCategories
        }

        companion object {
            private const val TOP_CATEGORIES_LIMIT = 5
            const val OTHER_CATEGORY_ID = -1L

            internal val OTHER_CATEGORY =
                Category(
                    id = OTHER_CATEGORY_ID,
                    name = "Other",
                    type = TransactionType.EXPENSE,
                    iconKey = "more_horiz",
                    colorKey = "gray",
                    isDefault = false,
                )

            private fun unknownCategory(id: Long): Category =
                Category(
                    id = id,
                    name = "Unknown",
                    type = TransactionType.EXPENSE,
                    iconKey = "help_outline",
                    colorKey = "gray",
                    isDefault = false,
                )
        }
    }
