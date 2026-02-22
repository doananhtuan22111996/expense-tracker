package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.CurrencyCategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencySumRow
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import dev.tuandoan.expensetracker.testutil.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRepositoryImplTest {
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var validator: BackupValidator
    private lateinit var repository: BackupRepositoryImpl

    @Before
    fun setup() {
        fakeCategoryDao = FakeCategoryDao()
        fakeTransactionDao = FakeTransactionDao()
        fakeTimeProvider = FakeTimeProvider()
        validator = BackupValidator()
        repository =
            BackupRepositoryImpl(
                categoryDao = fakeCategoryDao,
                transactionDao = fakeTransactionDao,
                backupValidator = validator,
                timeProvider = fakeTimeProvider,
                transactionRunner = FakeTransactionRunner(),
            )
    }

    @Test
    fun createBackupDocument_emptyDatabase_returnsEmptyLists() =
        runTest {
            val document = repository.createBackupDocument()

            assertEquals(0, document.categories.size)
            assertEquals(0, document.transactions.size)
        }

    @Test
    fun createBackupDocument_setsTimestampFromTimeProvider() =
        runTest {
            fakeTimeProvider.setCurrentMillis(1700500000000L)

            val document = repository.createBackupDocument()

            assertEquals(1700500000000L, document.createdAtEpochMs)
        }

    @Test
    fun createBackupDocument_setsSchemaVersion() =
        runTest {
            val document = repository.createBackupDocument()

            assertEquals(1, document.schemaVersion)
        }

    @Test
    fun createBackupDocument_mapsCategoriesToDtos() =
        runTest {
            fakeCategoryDao.allCategories.addAll(
                listOf(TestData.expenseCategoryEntity, TestData.incomeCategoryEntity),
            )

            val document = repository.createBackupDocument()

            assertEquals(2, document.categories.size)
            assertEquals("Food", document.categories[0].name)
            assertEquals(0, document.categories[0].type)
            assertEquals("Salary", document.categories[1].name)
            assertEquals(1, document.categories[1].type)
        }

    @Test
    fun createBackupDocument_mapsTransactionsToDtos() =
        runTest {
            fakeTransactionDao.allTransactions.add(TestData.sampleExpenseEntity)

            val document = repository.createBackupDocument()

            assertEquals(1, document.transactions.size)
            assertEquals(50000L, document.transactions[0].amount)
            assertEquals("VND", document.transactions[0].currencyCode)
            assertEquals("Lunch", document.transactions[0].note)
        }

    @Test
    fun restoreFromBackup_validDocument_clearsThenInsertsData() =
        runTest {
            // Pre-populate with existing data
            fakeCategoryDao.allCategories.add(
                CategoryEntity(id = 99L, name = "Old", type = 0),
            )
            fakeTransactionDao.allTransactions.add(
                TransactionEntity(
                    id = 99L,
                    type = 0,
                    amount = 1L,
                    currencyCode = "VND",
                    categoryId = 99L,
                    note = null,
                    timestamp = TestData.FIXED_TIME,
                    createdAt = TestData.FIXED_TIME,
                    updatedAt = TestData.FIXED_TIME,
                ),
            )

            repository.restoreFromBackup(TestData.sampleBackupDocument)

            // Old data should be gone, replaced with backup data
            assertEquals(1, fakeCategoryDao.allCategories.size)
            assertEquals("Food", fakeCategoryDao.allCategories[0].name)
            assertEquals(1, fakeTransactionDao.allTransactions.size)
            assertEquals(50000L, fakeTransactionDao.allTransactions[0].amount)
        }

    @Test
    fun restoreFromBackup_deletesTransactionsBeforeCategories() =
        runTest {
            globalCallOrder = 0

            repository.restoreFromBackup(TestData.sampleBackupDocument)

            // Transactions deleted first (FK constraint), then categories
            assertTrue(
                fakeTransactionDao.deleteAllOrder < fakeCategoryDao.deleteAllOrder,
            )
        }

    @Test(expected = BackupValidationException::class)
    fun restoreFromBackup_invalidDocument_throwsValidationException() =
        runTest {
            val invalidDocument = TestData.sampleBackupDocument.copy(schemaVersion = 99)

            repository.restoreFromBackup(invalidDocument)
        }

    @Test
    fun restoreFromBackup_invalidDocument_doesNotModifyData() =
        runTest {
            fakeCategoryDao.allCategories.add(TestData.expenseCategoryEntity)
            fakeTransactionDao.allTransactions.add(TestData.sampleExpenseEntity)

            try {
                repository.restoreFromBackup(
                    TestData.sampleBackupDocument.copy(schemaVersion = 99),
                )
            } catch (_: BackupValidationException) {
                // Expected
            }

            // Data should remain untouched
            assertEquals(1, fakeCategoryDao.allCategories.size)
            assertEquals("Food", fakeCategoryDao.allCategories[0].name)
            assertEquals(1, fakeTransactionDao.allTransactions.size)
        }

    @Test
    fun restoreFromBackup_emptyLists_clearsAllData() =
        runTest {
            fakeCategoryDao.allCategories.add(TestData.expenseCategoryEntity)
            fakeTransactionDao.allTransactions.add(TestData.sampleExpenseEntity)

            val emptyDocument =
                TestData.sampleBackupDocument.copy(
                    categories = emptyList(),
                    transactions = emptyList(),
                )

            repository.restoreFromBackup(emptyDocument)

            assertEquals(0, fakeCategoryDao.allCategories.size)
            assertEquals(0, fakeTransactionDao.allTransactions.size)
        }

    // Fakes

    private class FakeTransactionRunner : TransactionRunner {
        override suspend fun <R> runInTransaction(block: suspend () -> R): R = block()
    }

    private class FakeTransactionDao : TransactionDao {
        val allTransactions = mutableListOf<TransactionEntity>()
        var deleteAllOrder = 0

        override fun getTransactions(
            from: Long,
            to: Long,
            type: Int?,
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override suspend fun insert(entity: TransactionEntity): Long = 1L

        override suspend fun update(entity: TransactionEntity) {}

        override suspend fun deleteById(id: Long) {}

        override suspend fun getById(id: Long): TransactionEntity? = null

        override suspend fun getAll(): List<TransactionEntity> = allTransactions.toList()

        override suspend fun insertAll(list: List<TransactionEntity>) {
            allTransactions.addAll(list)
        }

        override suspend fun deleteAll() {
            deleteAllOrder = ++globalCallOrder
            allTransactions.clear()
        }

        override fun sumExpenseByCurrency(
            from: Long,
            to: Long,
        ): Flow<List<CurrencySumRow>> = MutableStateFlow(emptyList())

        override fun sumIncomeByCurrency(
            from: Long,
            to: Long,
        ): Flow<List<CurrencySumRow>> = MutableStateFlow(emptyList())

        override fun sumByCurrencyAndCategory(
            from: Long,
            to: Long,
            type: Int,
        ): Flow<List<CurrencyCategorySumRow>> = MutableStateFlow(emptyList())
    }

    private class FakeCategoryDao : CategoryDao {
        val allCategories = mutableListOf<CategoryEntity>()
        var deleteAllOrder = 0

        override fun getCategories(type: Int): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())

        override suspend fun getById(id: Long): CategoryEntity? = null

        override suspend fun getAll(): List<CategoryEntity> = allCategories.toList()

        override suspend fun insertAll(list: List<CategoryEntity>) {
            allCategories.addAll(list)
        }

        override suspend fun deleteAll() {
            deleteAllOrder = ++globalCallOrder
            allCategories.clear()
        }
    }

    companion object {
        private var globalCallOrder = 0
    }
}
