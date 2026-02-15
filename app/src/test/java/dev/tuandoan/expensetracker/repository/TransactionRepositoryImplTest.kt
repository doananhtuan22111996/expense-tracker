package dev.tuandoan.expensetracker.repository

import app.cash.turbine.test
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.CategorySumRow
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
    fun observeMonthlySummary_computesSummaryCorrectly() =
        runTest(testDispatcher) {
            fakeTransactionDao.expenseSumFlow.value = 200000L
            fakeTransactionDao.incomeSumFlow.value = 10000000L
            fakeTransactionDao.categorySumFlow.value = TestData.sampleCategorySumRows
            fakeCategoryDao.expenseCategories.value =
                listOf(TestData.expenseCategoryEntity, TestData.transportCategoryEntity)

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                assertEquals(200000L, summary.totalExpense)
                assertEquals(10000000L, summary.totalIncome)
                assertEquals(9800000L, summary.balance)
                assertEquals(2, summary.topExpenseCategories.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeMonthlySummary_nullSums_defaultToZero() =
        runTest(testDispatcher) {
            fakeTransactionDao.expenseSumFlow.value = null
            fakeTransactionDao.incomeSumFlow.value = null
            fakeTransactionDao.categorySumFlow.value = emptyList()
            fakeCategoryDao.expenseCategories.value = emptyList()

            repository.observeMonthlySummary(0L, Long.MAX_VALUE).test {
                val summary = awaitItem()
                assertEquals(0L, summary.totalExpense)
                assertEquals(0L, summary.totalIncome)
                assertEquals(0L, summary.balance)
                assertEquals(0, summary.topExpenseCategories.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // Fake DAOs

    private class FakeTransactionDao : TransactionDao {
        val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
        val transactionsById = mutableMapOf<Long, TransactionEntity>()
        val expenseSumFlow = MutableStateFlow<Long?>(null)
        val incomeSumFlow = MutableStateFlow<Long?>(null)
        val categorySumFlow = MutableStateFlow<List<CategorySumRow>>(emptyList())
        var lastInserted: TransactionEntity? = null
        var lastUpdated: TransactionEntity? = null
        var lastDeletedId: Long? = null

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

        override fun sumExpense(
            from: Long,
            to: Long,
        ): Flow<Long?> = expenseSumFlow

        override fun sumIncome(
            from: Long,
            to: Long,
        ): Flow<Long?> = incomeSumFlow

        override fun sumByCategory(
            from: Long,
            to: Long,
            type: Int,
        ): Flow<List<CategorySumRow>> = categorySumFlow
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

        override suspend fun insertAll(list: List<CategoryEntity>) {
            list.forEach { categoriesById[it.id] = it }
        }
    }
}
