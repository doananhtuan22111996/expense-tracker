package dev.tuandoan.expensetracker.repository

import app.cash.turbine.test
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.CurrencyCategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencySumRow
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import dev.tuandoan.expensetracker.testutil.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionRepositoryImplTest {
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setup() {
        fakeTransactionDao = FakeTransactionDao()
        fakeCategoryDao = FakeCategoryDao()
        fakeTimeProvider = FakeTimeProvider()
        repository =
            TransactionRepositoryImpl(
                transactionDao = fakeTransactionDao,
                categoryDao = fakeCategoryDao,
                timeProvider = fakeTimeProvider,
                ioDispatcher = testDispatcher,
            )
    }

    // observeTransactions tests

    @Test
    fun observeTransactions_matchesWithCategories() =
        runTest(testDispatcher) {
            fakeCategoryDao.expenseCategories.value = listOf(TestData.expenseCategoryEntity)
            fakeCategoryDao.incomeCategories.value = listOf(TestData.incomeCategoryEntity)
            fakeTransactionDao.transactionsFlow.value = listOf(TestData.sampleExpenseEntity)

            repository.observeTransactions(0L, Long.MAX_VALUE).test {
                val result = awaitItem()
                assertEquals(1, result.size)
                assertEquals("Food", result[0].category.name)
                assertEquals(50000L, result[0].amount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeTransactions_skipsOrphanedTransactions() =
        runTest(testDispatcher) {
            // Transaction references categoryId=99 which doesn't exist
            val orphanedEntity = TestData.sampleExpenseEntity.copy(categoryId = 99L)
            fakeCategoryDao.expenseCategories.value = listOf(TestData.expenseCategoryEntity)
            fakeCategoryDao.incomeCategories.value = emptyList()
            fakeTransactionDao.transactionsFlow.value = listOf(orphanedEntity)

            repository.observeTransactions(0L, Long.MAX_VALUE).test {
                val result = awaitItem()
                assertEquals(0, result.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeTransactions_emptyList() =
        runTest(testDispatcher) {
            fakeCategoryDao.expenseCategories.value = listOf(TestData.expenseCategoryEntity)
            fakeCategoryDao.incomeCategories.value = emptyList()
            fakeTransactionDao.transactionsFlow.value = emptyList()

            repository.observeTransactions(0L, Long.MAX_VALUE).test {
                val result = awaitItem()
                assertEquals(0, result.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // addTransaction tests

    @Test
    fun addTransaction_setsTimestampsFromTimeProvider() =
        runTest(testDispatcher) {
            fakeTimeProvider.setCurrentMillis(1700000000000L)

            repository.addTransaction(
                type = TransactionType.EXPENSE,
                amount = 50000L,
                categoryId = 1L,
                note = "Test",
                timestamp = 1700000000000L,
            )

            val inserted = fakeTransactionDao.lastInserted
            assertNotNull(inserted)
            assertEquals(1700000000000L, inserted!!.createdAt)
            assertEquals(1700000000000L, inserted.updatedAt)
        }

    @Test
    fun addTransaction_passesAllFieldsCorrectly() =
        runTest(testDispatcher) {
            repository.addTransaction(
                type = TransactionType.INCOME,
                amount = 10000000L,
                categoryId = 2L,
                note = "Salary",
                timestamp = 1700000000000L,
                currencyCode = "USD",
            )

            val inserted = fakeTransactionDao.lastInserted!!
            assertEquals(1, inserted.type)
            assertEquals(10000000L, inserted.amount)
            assertEquals(2L, inserted.categoryId)
            assertEquals("Salary", inserted.note)
            assertEquals("USD", inserted.currencyCode)
        }

    @Test
    fun addTransaction_defaultCurrencyIsVND() =
        runTest(testDispatcher) {
            repository.addTransaction(
                type = TransactionType.EXPENSE,
                amount = 50000L,
                categoryId = 1L,
                note = null,
                timestamp = 1700000000000L,
            )

            assertEquals("VND", fakeTransactionDao.lastInserted!!.currencyCode)
        }

    // updateTransaction tests

    @Test
    fun updateTransaction_setsUpdatedAtFromTimeProvider() =
        runTest(testDispatcher) {
            fakeTimeProvider.setCurrentMillis(1700099999000L)

            repository.updateTransaction(TestData.sampleExpenseTransaction)

            val updated = fakeTransactionDao.lastUpdated
            assertNotNull(updated)
            assertEquals(1700099999000L, updated!!.updatedAt)
        }

    @Test
    fun updateTransaction_preservesOtherFields() =
        runTest(testDispatcher) {
            fakeTimeProvider.setCurrentMillis(1700099999000L)
            val transaction = TestData.sampleExpenseTransaction

            repository.updateTransaction(transaction)

            val updated = fakeTransactionDao.lastUpdated!!
            assertEquals(transaction.id, updated.id)
            assertEquals(transaction.amount, updated.amount)
            assertEquals(transaction.category.id, updated.categoryId)
        }

    // deleteTransaction tests

    @Test
    fun deleteTransaction_callsDao() =
        runTest(testDispatcher) {
            repository.deleteTransaction(42L)

            assertEquals(42L, fakeTransactionDao.lastDeletedId)
        }

    // getTransaction tests

    @Test
    fun getTransaction_existingWithCategory_returnsDomain() =
        runTest(testDispatcher) {
            fakeTransactionDao.transactionsById[1L] = TestData.sampleExpenseEntity
            fakeCategoryDao.categoriesById[1L] = TestData.expenseCategoryEntity

            val result = repository.getTransaction(1L)

            assertNotNull(result)
            assertEquals(1L, result!!.id)
            assertEquals("Food", result.category.name)
        }

    @Test
    fun getTransaction_nonExisting_returnsNull() =
        runTest(testDispatcher) {
            val result = repository.getTransaction(999L)

            assertNull(result)
        }

    @Test
    fun getTransaction_orphanedCategory_returnsNull() =
        runTest(testDispatcher) {
            fakeTransactionDao.transactionsById[1L] = TestData.sampleExpenseEntity
            // Don't add category - simulates orphaned transaction

            val result = repository.getTransaction(1L)

            assertNull(result)
        }

    // observeMonthlySummary tests

    @Test
    fun observeMonthlySummary_singleCurrency_computesCorrectly() =
        runTest(testDispatcher) {
            fakeTransactionDao.expenseByCurrencyFlow.value =
                listOf(CurrencySumRow(currencyCode = "VND", total = 200000L))
            fakeTransactionDao.incomeByCurrencyFlow.value =
                listOf(CurrencySumRow(currencyCode = "VND", total = 10000000L))
            fakeTransactionDao.categorySumByCurrencyFlow.value =
                listOf(
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 1L, total = 150000L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 3L, total = 50000L),
                )
            fakeCategoryDao.expenseCategories.value =
                listOf(TestData.expenseCategoryEntity, TestData.transportCategoryEntity)

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                assertEquals(1, summary.currencySummaries.size)

                val vnd = summary.currencySummaries[0]
                assertEquals("VND", vnd.currencyCode)
                assertEquals(200000L, vnd.totalExpense)
                assertEquals(10000000L, vnd.totalIncome)
                assertEquals(9800000L, vnd.balance)
                assertEquals(2, vnd.topExpenseCategories.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeMonthlySummary_multipleCurrencies_produceSeparateSummaries() =
        runTest(testDispatcher) {
            fakeTransactionDao.expenseByCurrencyFlow.value =
                listOf(
                    CurrencySumRow(currencyCode = "VND", total = 200000L),
                    CurrencySumRow(currencyCode = "USD", total = 5000L),
                )
            fakeTransactionDao.incomeByCurrencyFlow.value =
                listOf(
                    CurrencySumRow(currencyCode = "VND", total = 10000000L),
                    CurrencySumRow(currencyCode = "USD", total = 300000L),
                )
            fakeTransactionDao.categorySumByCurrencyFlow.value =
                listOf(
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 1L, total = 200000L),
                    CurrencyCategorySumRow(currencyCode = "USD", categoryId = 1L, total = 5000L),
                )
            fakeCategoryDao.expenseCategories.value = listOf(TestData.expenseCategoryEntity)

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                assertEquals(2, summary.currencySummaries.size)

                val vnd = summary.currencySummaries[0]
                assertEquals("VND", vnd.currencyCode)
                assertEquals(200000L, vnd.totalExpense)
                assertEquals(10000000L, vnd.totalIncome)
                assertEquals(9800000L, vnd.balance)

                val usd = summary.currencySummaries[1]
                assertEquals("USD", usd.currencyCode)
                assertEquals(5000L, usd.totalExpense)
                assertEquals(300000L, usd.totalIncome)
                assertEquals(295000L, usd.balance)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeMonthlySummary_currencyOrdering_matchesSupportedCurrenciesRegistry() =
        runTest(testDispatcher) {
            // Add currencies in reverse registry order
            fakeTransactionDao.expenseByCurrencyFlow.value =
                listOf(
                    CurrencySumRow(currencyCode = "SGD", total = 100L),
                    CurrencySumRow(currencyCode = "KRW", total = 200L),
                    CurrencySumRow(currencyCode = "JPY", total = 300L),
                    CurrencySumRow(currencyCode = "EUR", total = 400L),
                    CurrencySumRow(currencyCode = "USD", total = 500L),
                    CurrencySumRow(currencyCode = "VND", total = 600L),
                )
            fakeTransactionDao.incomeByCurrencyFlow.value = emptyList()
            fakeTransactionDao.categorySumByCurrencyFlow.value = emptyList()
            fakeCategoryDao.expenseCategories.value = emptyList()

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                assertEquals(6, summary.currencySummaries.size)
                assertEquals("VND", summary.currencySummaries[0].currencyCode)
                assertEquals("USD", summary.currencySummaries[1].currencyCode)
                assertEquals("EUR", summary.currencySummaries[2].currencyCode)
                assertEquals("JPY", summary.currencySummaries[3].currencyCode)
                assertEquals("KRW", summary.currencySummaries[4].currencyCode)
                assertEquals("SGD", summary.currencySummaries[5].currencyCode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeMonthlySummary_unknownCurrencies_sortedAlphabeticallyAfterKnown() =
        runTest(testDispatcher) {
            fakeTransactionDao.expenseByCurrencyFlow.value =
                listOf(
                    CurrencySumRow(currencyCode = "GBP", total = 100L),
                    CurrencySumRow(currencyCode = "VND", total = 200L),
                    CurrencySumRow(currencyCode = "AUD", total = 300L),
                )
            fakeTransactionDao.incomeByCurrencyFlow.value = emptyList()
            fakeTransactionDao.categorySumByCurrencyFlow.value = emptyList()
            fakeCategoryDao.expenseCategories.value = emptyList()

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                assertEquals(3, summary.currencySummaries.size)
                assertEquals("VND", summary.currencySummaries[0].currencyCode)
                assertEquals("AUD", summary.currencySummaries[1].currencyCode)
                assertEquals("GBP", summary.currencySummaries[2].currencyCode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeMonthlySummary_top5PlusOtherAggregation() =
        runTest(testDispatcher) {
            fakeTransactionDao.expenseByCurrencyFlow.value =
                listOf(CurrencySumRow(currencyCode = "VND", total = 700L))
            fakeTransactionDao.incomeByCurrencyFlow.value = emptyList()

            // 7 categories - top 5 should be shown, rest aggregated to "Other"
            fakeTransactionDao.categorySumByCurrencyFlow.value =
                listOf(
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 1L, total = 200L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 3L, total = 150L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 4L, total = 120L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 5L, total = 100L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 6L, total = 80L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 7L, total = 30L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 8L, total = 20L),
                )
            fakeCategoryDao.expenseCategories.value =
                listOf(
                    TestData.expenseCategoryEntity,
                    TestData.transportCategoryEntity,
                    CategoryEntity(
                        id = 4L,
                        name = "Shopping",
                        type = 0,
                        iconKey = "shopping_cart",
                        colorKey = "purple",
                        isDefault = true,
                    ),
                    CategoryEntity(
                        id = 5L,
                        name = "Bills",
                        type = 0,
                        iconKey = "receipt",
                        colorKey = "orange",
                        isDefault = true,
                    ),
                    CategoryEntity(
                        id = 6L,
                        name = "Health",
                        type = 0,
                        iconKey = "medical_services",
                        colorKey = "teal",
                        isDefault = true,
                    ),
                    CategoryEntity(
                        id = 7L,
                        name = "Entertainment",
                        type = 0,
                        iconKey = "sports_esports",
                        colorKey = "pink",
                        isDefault = false,
                    ),
                    CategoryEntity(
                        id = 8L,
                        name = "Education",
                        type = 0,
                        iconKey = "school",
                        colorKey = "indigo",
                        isDefault = false,
                    ),
                )

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                val vnd = summary.currencySummaries[0]
                // 5 top + 1 "Other" = 6
                assertEquals(6, vnd.topExpenseCategories.size)

                // Verify top 5 are in correct order (desc by total)
                assertEquals("Food", vnd.topExpenseCategories[0].category.name)
                assertEquals(200L, vnd.topExpenseCategories[0].total)
                assertEquals("Transport", vnd.topExpenseCategories[1].category.name)
                assertEquals(150L, vnd.topExpenseCategories[1].total)
                assertEquals("Shopping", vnd.topExpenseCategories[2].category.name)
                assertEquals(120L, vnd.topExpenseCategories[2].total)
                assertEquals("Bills", vnd.topExpenseCategories[3].category.name)
                assertEquals(100L, vnd.topExpenseCategories[3].total)
                assertEquals("Health", vnd.topExpenseCategories[4].category.name)
                assertEquals(80L, vnd.topExpenseCategories[4].total)

                // Last entry is "Other" with aggregated total
                val other = vnd.topExpenseCategories[5]
                assertEquals("Other", other.category.name)
                assertEquals(-1L, other.category.id)
                assertEquals(50L, other.total) // 30 + 20
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeMonthlySummary_incomeOnlyCurrency_hasZeroExpense() =
        runTest(testDispatcher) {
            fakeTransactionDao.expenseByCurrencyFlow.value = emptyList()
            fakeTransactionDao.incomeByCurrencyFlow.value =
                listOf(CurrencySumRow(currencyCode = "USD", total = 500000L))
            fakeTransactionDao.categorySumByCurrencyFlow.value = emptyList()
            fakeCategoryDao.expenseCategories.value = emptyList()

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                assertEquals(1, summary.currencySummaries.size)

                val usd = summary.currencySummaries[0]
                assertEquals("USD", usd.currencyCode)
                assertEquals(0L, usd.totalExpense)
                assertEquals(500000L, usd.totalIncome)
                assertEquals(500000L, usd.balance)
                assertTrue(usd.topExpenseCategories.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeMonthlySummary_expenseOnlyCurrency_hasZeroIncomeAndNegativeBalance() =
        runTest(testDispatcher) {
            fakeTransactionDao.expenseByCurrencyFlow.value =
                listOf(CurrencySumRow(currencyCode = "VND", total = 500000L))
            fakeTransactionDao.incomeByCurrencyFlow.value = emptyList()
            fakeTransactionDao.categorySumByCurrencyFlow.value =
                listOf(CurrencyCategorySumRow(currencyCode = "VND", categoryId = 1L, total = 500000L))
            fakeCategoryDao.expenseCategories.value = listOf(TestData.expenseCategoryEntity)

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                assertEquals(1, summary.currencySummaries.size)

                val vnd = summary.currencySummaries[0]
                assertEquals("VND", vnd.currencyCode)
                assertEquals(500000L, vnd.totalExpense)
                assertEquals(0L, vnd.totalIncome)
                assertEquals(-500000L, vnd.balance)
                assertEquals(1, vnd.topExpenseCategories.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeMonthlySummary_exactly5Categories_noOtherRow() =
        runTest(testDispatcher) {
            fakeTransactionDao.expenseByCurrencyFlow.value =
                listOf(CurrencySumRow(currencyCode = "VND", total = 550L))
            fakeTransactionDao.incomeByCurrencyFlow.value = emptyList()
            fakeTransactionDao.categorySumByCurrencyFlow.value =
                listOf(
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 1L, total = 200L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 3L, total = 150L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 4L, total = 100L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 5L, total = 60L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 6L, total = 40L),
                )
            fakeCategoryDao.expenseCategories.value =
                listOf(
                    TestData.expenseCategoryEntity,
                    TestData.transportCategoryEntity,
                    CategoryEntity(
                        id = 4L,
                        name = "Shopping",
                        type = 0,
                        iconKey = "shopping_cart",
                        colorKey = "purple",
                        isDefault = true,
                    ),
                    CategoryEntity(
                        id = 5L,
                        name = "Bills",
                        type = 0,
                        iconKey = "receipt",
                        colorKey = "orange",
                        isDefault = true,
                    ),
                    CategoryEntity(
                        id = 6L,
                        name = "Health",
                        type = 0,
                        iconKey = "medical_services",
                        colorKey = "teal",
                        isDefault = true,
                    ),
                )

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                val vnd = summary.currencySummaries[0]
                assertEquals(5, vnd.topExpenseCategories.size)
                // No "Other" row when exactly 5 categories
                assertTrue(vnd.topExpenseCategories.none { it.category.id == -1L })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeMonthlySummary_categoryTiebreaker_ordersByCategoryIdAscending() =
        runTest(testDispatcher) {
            fakeTransactionDao.expenseByCurrencyFlow.value =
                listOf(CurrencySumRow(currencyCode = "VND", total = 200L))
            fakeTransactionDao.incomeByCurrencyFlow.value = emptyList()
            // Two categories with identical totals: id=3 and id=1, both total=100
            fakeTransactionDao.categorySumByCurrencyFlow.value =
                listOf(
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 3L, total = 100L),
                    CurrencyCategorySumRow(currencyCode = "VND", categoryId = 1L, total = 100L),
                )
            fakeCategoryDao.expenseCategories.value =
                listOf(TestData.expenseCategoryEntity, TestData.transportCategoryEntity)

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                val vnd = summary.currencySummaries[0]
                assertEquals(2, vnd.topExpenseCategories.size)
                // Tiebreaker: lower categoryId comes first
                assertEquals(1L, vnd.topExpenseCategories[0].category.id)
                assertEquals(3L, vnd.topExpenseCategories[1].category.id)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeMonthlySummary_emptyMonth_returnsEmptyList() =
        runTest(testDispatcher) {
            fakeTransactionDao.expenseByCurrencyFlow.value = emptyList()
            fakeTransactionDao.incomeByCurrencyFlow.value = emptyList()
            fakeTransactionDao.categorySumByCurrencyFlow.value = emptyList()
            fakeCategoryDao.expenseCategories.value = emptyList()

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                assertTrue(summary.isEmpty)
                assertTrue(summary.currencySummaries.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    // Fake DAOs

    private class FakeTransactionDao : TransactionDao {
        val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        val transactionsById = mutableMapOf<Long, TransactionEntity>()
        val expenseByCurrencyFlow = MutableStateFlow<List<CurrencySumRow>>(emptyList())
        val incomeByCurrencyFlow = MutableStateFlow<List<CurrencySumRow>>(emptyList())
        val categorySumByCurrencyFlow = MutableStateFlow<List<CurrencyCategorySumRow>>(emptyList())
        var lastInserted: TransactionEntity? = null
        var lastUpdated: TransactionEntity? = null
        var lastDeletedId: Long? = null
        var allTransactions = mutableListOf<TransactionEntity>()

        override fun getTransactions(
            from: Long,
            to: Long,
            type: Int?,
        ): Flow<List<TransactionEntity>> = transactionsFlow

        override suspend fun insert(entity: TransactionEntity): Long {
            lastInserted = entity
            return 1L
        }

        override suspend fun update(entity: TransactionEntity) {
            lastUpdated = entity
        }

        override suspend fun deleteById(id: Long) {
            lastDeletedId = id
        }

        override suspend fun getById(id: Long): TransactionEntity? = transactionsById[id]

        override suspend fun getAll(): List<TransactionEntity> = allTransactions

        override suspend fun insertAll(list: List<TransactionEntity>) {
            allTransactions.addAll(list)
        }

        override suspend fun deleteAll() {
            allTransactions.clear()
        }

        override fun sumExpenseByCurrency(
            from: Long,
            to: Long,
        ): Flow<List<CurrencySumRow>> = expenseByCurrencyFlow

        override fun sumIncomeByCurrency(
            from: Long,
            to: Long,
        ): Flow<List<CurrencySumRow>> = incomeByCurrencyFlow

        override fun sumByCurrencyAndCategory(
            from: Long,
            to: Long,
            type: Int,
        ): Flow<List<CurrencyCategorySumRow>> = categorySumByCurrencyFlow
    }

    private class FakeCategoryDao : CategoryDao {
        val expenseCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
        val incomeCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
        val categoriesById = mutableMapOf<Long, CategoryEntity>()

        override fun getCategories(type: Int): Flow<List<CategoryEntity>> =
            when (type) {
                0 -> expenseCategories
                1 -> incomeCategories
                else -> MutableStateFlow(emptyList())
            }

        override suspend fun getById(id: Long): CategoryEntity? = categoriesById[id]

        override suspend fun getAll(): List<CategoryEntity> = categoriesById.values.toList()

        override suspend fun insertAll(list: List<CategoryEntity>) {
            list.forEach { categoriesById[it.id] = it }
        }

        override suspend fun deleteAll() {
            categoriesById.clear()
        }
    }
}
