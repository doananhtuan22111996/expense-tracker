package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.GoldHoldingDao
import dev.tuandoan.expensetracker.data.database.dao.GoldPriceDao
import dev.tuandoan.expensetracker.data.database.dao.RecurringTransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.CategoryWithCountRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencyCategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencySumRow
import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity
import dev.tuandoan.expensetracker.data.database.entity.RecurringTransactionEntity
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.data.export.CsvExporter
import dev.tuandoan.expensetracker.domain.crash.NoOpCrashReporter
import dev.tuandoan.expensetracker.domain.repository.BackupProgress
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import dev.tuandoan.expensetracker.testutil.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.util.zip.GZIPOutputStream
import java.io.ByteArrayInputStream as BAIS

class BackupRepositoryImplTest {
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var fakeRecurringDao: FakeRecurringTransactionDao
    private lateinit var fakeGoldHoldingDao: FakeGoldHoldingDao
    private lateinit var fakeGoldPriceDao: FakeGoldPriceDao
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeCurrencyPreferenceRepo: FakeCurrencyPreferenceRepository
    private lateinit var validator: BackupValidator
    private lateinit var serializer: BackupSerializer
    private lateinit var assembler: BackupAssembler
    private lateinit var repository: BackupRepositoryImpl
    private var globalCallOrder = 0

    @Before
    fun setup() {
        fakeCategoryDao = FakeCategoryDao()
        fakeTransactionDao = FakeTransactionDao()
        fakeRecurringDao = FakeRecurringTransactionDao()
        fakeGoldHoldingDao = FakeGoldHoldingDao()
        fakeGoldPriceDao = FakeGoldPriceDao()
        fakeTimeProvider = FakeTimeProvider()
        fakeCurrencyPreferenceRepo = FakeCurrencyPreferenceRepository()
        validator = BackupValidator()
        serializer = BackupSerializer()
        assembler = BackupAssembler()
        globalCallOrder = 0
        repository =
            BackupRepositoryImpl(
                categoryDao = fakeCategoryDao,
                transactionDao = fakeTransactionDao,
                recurringTransactionDao = fakeRecurringDao,
                goldHoldingDao = fakeGoldHoldingDao,
                goldPriceDao = fakeGoldPriceDao,
                backupValidator = validator,
                backupSerializer = serializer,
                backupAssembler = assembler,
                timeProvider = fakeTimeProvider,
                transactionRunner = FakeTransactionRunner(),
                currencyPreferenceRepository = fakeCurrencyPreferenceRepo,
                csvExporter = CsvExporter(ZoneId.of("UTC")),
                crashReporter = NoOpCrashReporter(),
            )
    }

    @Test
    fun exportBackupJson_emptyDatabase_returnsJsonWithEmptyLists() =
        runTest {
            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertEquals(0, document.categories.size)
            assertEquals(0, document.transactions.size)
        }

    @Test
    fun exportBackupJson_setsTimestampFromTimeProvider() =
        runTest {
            fakeTimeProvider.setCurrentMillis(1700500000000L)

            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertEquals(1700500000000L, document.createdAtEpochMs)
        }

    @Test
    fun exportBackupJson_setsSchemaVersion() =
        runTest {
            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertEquals(1, document.schemaVersion)
        }

    @Test
    fun exportBackupJson_mapsCategoriesToDtos() =
        runTest {
            fakeCategoryDao.allCategories.addAll(
                listOf(TestData.expenseCategoryEntity, TestData.incomeCategoryEntity),
            )

            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertEquals(2, document.categories.size)
            assertEquals("Food", document.categories[0].name)
            assertEquals(0, document.categories[0].type)
            assertEquals("Salary", document.categories[1].name)
            assertEquals(1, document.categories[1].type)
        }

    @Test
    fun exportBackupJson_mapsTransactionsToDtos() =
        runTest {
            fakeTransactionDao.allTransactions.add(TestData.sampleExpenseEntity)

            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertEquals(1, document.transactions.size)
            assertEquals(50000L, document.transactions[0].amount)
            assertEquals("VND", document.transactions[0].currencyCode)
            assertEquals("Lunch", document.transactions[0].note)
        }

    @Test
    fun exportBackupJson_includesDefaultCurrencyCode() =
        runTest {
            fakeCurrencyPreferenceRepo.setDefaultCurrency("USD")

            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertEquals("USD", document.defaultCurrencyCode)
        }

    @Test
    fun exportBackupJson_includesDeviceLocale() =
        runTest {
            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertTrue(document.deviceLocale.isNotEmpty())
        }

    @Test
    fun exportBackupJson_categoriesSortedById() =
        runTest {
            fakeCategoryDao.allCategories.addAll(
                listOf(
                    TestData.expenseCategoryEntity.copy(id = 3L, name = "Z"),
                    TestData.expenseCategoryEntity.copy(id = 1L, name = "A"),
                    TestData.expenseCategoryEntity.copy(id = 2L, name = "M"),
                ),
            )

            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertEquals(listOf(1L, 2L, 3L), document.categories.map { it.id })
        }

    @Test
    fun exportBackupJson_transactionsSortedById() =
        runTest {
            fakeTransactionDao.allTransactions.addAll(
                listOf(
                    TestData.sampleExpenseEntity.copy(id = 3L),
                    TestData.sampleExpenseEntity.copy(id = 1L),
                    TestData.sampleExpenseEntity.copy(id = 2L),
                ),
            )

            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertEquals(listOf(1L, 2L, 3L), document.transactions.map { it.id })
        }

    @Test
    fun importBackupJson_validJson_clearsThenInsertsData() =
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

            val json = serializer.encode(TestData.sampleBackupDocument)
            val result = repository.importBackupJson(json)

            // Old data should be gone, replaced with backup data
            assertEquals(1, fakeCategoryDao.allCategories.size)
            assertEquals("Food", fakeCategoryDao.allCategories[0].name)
            assertEquals(1, fakeTransactionDao.allTransactions.size)
            assertEquals(50000L, fakeTransactionDao.allTransactions[0].amount)
            assertEquals(1, result.categoryCount)
            assertEquals(1, result.transactionCount)
        }

    @Test
    fun importBackupJson_deletesTransactionsBeforeCategories() =
        runTest {
            val json = serializer.encode(TestData.sampleBackupDocument)
            repository.importBackupJson(json)

            // Transactions deleted first (FK constraint), then categories
            assertTrue(
                fakeTransactionDao.deleteAllOrder < fakeCategoryDao.deleteAllOrder,
            )
        }

    @Test(expected = BackupValidationException::class)
    fun importBackupJson_invalidSchemaVersion_throwsValidationException() =
        runTest {
            val invalidDocument = TestData.sampleBackupDocument.copy(schemaVersion = 99)
            val json = serializer.encode(invalidDocument)

            repository.importBackupJson(json)
        }

    @Test
    fun importBackupJson_invalidDocument_doesNotModifyData() =
        runTest {
            fakeCategoryDao.allCategories.add(TestData.expenseCategoryEntity)
            fakeTransactionDao.allTransactions.add(TestData.sampleExpenseEntity)

            val invalidDocument = TestData.sampleBackupDocument.copy(schemaVersion = 99)
            val json = serializer.encode(invalidDocument)

            try {
                repository.importBackupJson(json)
            } catch (_: BackupValidationException) {
                // Expected
            }

            // Data should remain untouched
            assertEquals(1, fakeCategoryDao.allCategories.size)
            assertEquals("Food", fakeCategoryDao.allCategories[0].name)
            assertEquals(1, fakeTransactionDao.allTransactions.size)
        }

    @Test
    fun importBackupJson_emptyLists_clearsAllData() =
        runTest {
            fakeCategoryDao.allCategories.add(TestData.expenseCategoryEntity)
            fakeTransactionDao.allTransactions.add(TestData.sampleExpenseEntity)

            val emptyDocument =
                TestData.sampleBackupDocument.copy(
                    categories = emptyList(),
                    transactions = emptyList(),
                )
            val json = serializer.encode(emptyDocument)

            val result = repository.importBackupJson(json)

            assertEquals(0, fakeCategoryDao.allCategories.size)
            assertEquals(0, fakeTransactionDao.allTransactions.size)
            assertEquals(0, result.categoryCount)
            assertEquals(0, result.transactionCount)
        }

    @Test(expected = IllegalArgumentException::class)
    fun importBackupJson_invalidJsonFormat_throwsIllegalArgument() =
        runTest {
            repository.importBackupJson("not valid json")
        }

    @Test
    fun importBackupJson_returnsCorrectCounts() =
        runTest {
            val json = serializer.encode(TestData.sampleBackupDocument)
            val result = repository.importBackupJson(json)

            assertEquals(1, result.categoryCount)
            assertEquals(1, result.transactionCount)
        }

    @Test
    fun importBackupJson_restoresSupportedCurrencyPreference() =
        runTest {
            val document = TestData.sampleBackupDocument.copy(defaultCurrencyCode = "USD")
            val json = serializer.encode(document)

            repository.importBackupJson(json)

            assertTrue(fakeCurrencyPreferenceRepo.setDefaultCurrencyCalled)
            assertEquals("USD", fakeCurrencyPreferenceRepo.lastSetCurrencyCode)
        }

    @Test
    fun importBackupJson_skipsUnsupportedCurrencyPreference() =
        runTest {
            val document = TestData.sampleBackupDocument.copy(defaultCurrencyCode = "INVALID")
            val json = serializer.encode(document)

            repository.importBackupJson(json)

            assertEquals(false, fakeCurrencyPreferenceRepo.setDefaultCurrencyCalled)
        }

    @Test
    fun importBackupJson_skipsBlankCurrencyPreference() =
        runTest {
            val document = TestData.sampleBackupDocument.copy(defaultCurrencyCode = "")
            val json = serializer.encode(document)

            repository.importBackupJson(json)

            assertEquals(false, fakeCurrencyPreferenceRepo.setDefaultCurrencyCalled)
        }

    // --- Stream-based export tests ---

    @Test
    fun exportBackup_writesValidJsonToOutputStream() =
        runTest {
            fakeCategoryDao.allCategories.add(TestData.expenseCategoryEntity)
            fakeTransactionDao.allTransactions.add(TestData.sampleExpenseEntity)

            val outputStream = ByteArrayOutputStream()
            repository.exportBackup(outputStream)

            val json = outputStream.toString(Charsets.UTF_8.name())
            val document = serializer.decode(json)!!

            assertEquals(1, document.categories.size)
            assertEquals(1, document.transactions.size)
            assertEquals("Food", document.categories[0].name)
        }

    @Test
    fun exportBackup_reportsProgress() =
        runTest {
            fakeCategoryDao.allCategories.addAll(
                listOf(TestData.expenseCategoryEntity, TestData.incomeCategoryEntity),
            )
            fakeTransactionDao.allTransactions.add(TestData.sampleExpenseEntity)

            val progressUpdates = mutableListOf<BackupProgress>()
            val outputStream = ByteArrayOutputStream()
            repository.exportBackup(outputStream) { progressUpdates.add(it) }

            assertTrue(progressUpdates.isNotEmpty())
            assertEquals(0, progressUpdates.first().current)
            assertEquals(3, progressUpdates.first().total)
            assertEquals(3, progressUpdates.last().current)
            assertEquals(3, progressUpdates.last().total)
        }

    // --- Stream-based import tests ---

    @Test
    fun importBackup_validStream_clearsAndInsertsData() =
        runTest {
            fakeCategoryDao.allCategories.add(
                CategoryEntity(id = 99L, name = "Old", type = 0),
            )

            val json = serializer.encode(TestData.sampleBackupDocument)
            val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))

            val result = repository.importBackup(inputStream)

            assertEquals(1, fakeCategoryDao.allCategories.size)
            assertEquals("Food", fakeCategoryDao.allCategories[0].name)
            assertEquals(1, fakeTransactionDao.allTransactions.size)
            assertEquals(1, result.categoryCount)
            assertEquals(1, result.transactionCount)
        }

    @Test
    fun importBackup_reportsProgress() =
        runTest {
            val json = serializer.encode(TestData.sampleBackupDocument)
            val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))

            val progressUpdates = mutableListOf<BackupProgress>()
            repository.importBackup(inputStream) { progressUpdates.add(it) }

            // Should have: initial(0), after categories(1), after transactions batch(2)
            assertTrue(progressUpdates.size >= 2)
            assertEquals(0, progressUpdates.first().current)
            assertEquals(2, progressUpdates.first().total)
            assertEquals(2, progressUpdates.last().current)
        }

    @Test
    fun importBackup_batchInserts_largeDataset() =
        runTest {
            val categories = listOf(TestData.sampleBackupCategoryDto)
            val transactions =
                (1..1200L).map { i ->
                    TestData.sampleBackupTransactionDto.copy(id = i)
                }
            val document =
                TestData.sampleBackupDocument.copy(
                    categories = categories,
                    transactions = transactions,
                )
            val json = serializer.encode(document)
            val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))

            val progressUpdates = mutableListOf<BackupProgress>()
            val result = repository.importBackup(inputStream) { progressUpdates.add(it) }

            assertEquals(1, result.categoryCount)
            assertEquals(1200, result.transactionCount)
            assertEquals(1200, fakeTransactionDao.allTransactions.size)
            // Multiple progress updates: initial + categories + transaction batches
            // 1200 / 500 = 3 batches (500, 500, 200)
            assertTrue(progressUpdates.size >= 4) // 0, cats, batch1, batch2, batch3
        }

    @Test
    fun importBackup_gzipCompressed_decompressesAndImports() =
        runTest {
            val json = serializer.encode(TestData.sampleBackupDocument)
            val compressedBytes =
                ByteArrayOutputStream()
                    .also { baos ->
                        GZIPOutputStream(baos).use { gzip ->
                            gzip.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }.toByteArray()

            val result = repository.importBackup(BAIS(compressedBytes))

            assertEquals(1, result.categoryCount)
            assertEquals(1, result.transactionCount)
            assertEquals("Food", fakeCategoryDao.allCategories[0].name)
        }

    @Test(expected = IllegalArgumentException::class)
    fun importBackup_invalidStream_throwsIllegalArgument() =
        runTest {
            val inputStream = ByteArrayInputStream("not valid json".toByteArray())
            repository.importBackup(inputStream)
        }

    // --- Gold holding export/import tests ---

    @Test
    fun exportBackupJson_includesGoldHoldings() =
        runTest {
            fakeGoldHoldingDao.allHoldings.add(
                GoldHoldingEntity(
                    id = 1L,
                    type = "SJC",
                    weightValue = 2.5,
                    weightUnit = "TAEL",
                    buyPricePerUnit = 87_000_000L,
                    currencyCode = "VND",
                    buyDateMillis = TestData.FIXED_TIME,
                    note = "test",
                    createdAt = TestData.FIXED_TIME,
                    updatedAt = TestData.FIXED_TIME,
                ),
            )

            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertEquals(1, document.goldHoldings.size)
            assertEquals("SJC", document.goldHoldings[0].type)
            assertEquals(2.5, document.goldHoldings[0].weightValue, 0.001)
            assertEquals(87_000_000L, document.goldHoldings[0].buyPricePerUnit)
        }

    @Test
    fun exportBackupJson_goldHoldingsSortedById() =
        runTest {
            fakeGoldHoldingDao.allHoldings.addAll(
                listOf(
                    GoldHoldingEntity(
                        id = 3L,
                        type = "SJC",
                        weightValue = 1.0,
                        weightUnit = "TAEL",
                        buyPricePerUnit = 1L,
                        currencyCode = "VND",
                        buyDateMillis = TestData.FIXED_TIME,
                        note = null,
                        createdAt = TestData.FIXED_TIME,
                        updatedAt = TestData.FIXED_TIME,
                    ),
                    GoldHoldingEntity(
                        id = 1L,
                        type = "SJC",
                        weightValue = 1.0,
                        weightUnit = "TAEL",
                        buyPricePerUnit = 1L,
                        currencyCode = "VND",
                        buyDateMillis = TestData.FIXED_TIME,
                        note = null,
                        createdAt = TestData.FIXED_TIME,
                        updatedAt = TestData.FIXED_TIME,
                    ),
                ),
            )

            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertEquals(listOf(1L, 3L), document.goldHoldings.map { it.id })
        }

    @Test
    fun importBackupJson_withGoldHoldings_insertsGoldData() =
        runTest {
            val document =
                TestData.sampleBackupDocument.copy(
                    goldHoldings = listOf(TestData.sampleBackupGoldHoldingDto),
                )
            val json = serializer.encode(document)

            val result = repository.importBackupJson(json)

            assertEquals(1, result.goldHoldingCount)
            assertEquals(1, fakeGoldHoldingDao.allHoldings.size)
            assertEquals("SJC", fakeGoldHoldingDao.allHoldings[0].type)
        }

    @Test
    fun importBackupJson_withGoldHoldings_deletesExistingGold() =
        runTest {
            fakeGoldHoldingDao.allHoldings.add(
                GoldHoldingEntity(
                    id = 99L,
                    type = "GOLD_24K",
                    weightValue = 1.0,
                    weightUnit = "GRAM",
                    buyPricePerUnit = 1L,
                    currencyCode = "VND",
                    buyDateMillis = TestData.FIXED_TIME,
                    note = null,
                    createdAt = TestData.FIXED_TIME,
                    updatedAt = TestData.FIXED_TIME,
                ),
            )
            val document =
                TestData.sampleBackupDocument.copy(
                    goldHoldings = listOf(TestData.sampleBackupGoldHoldingDto),
                )
            val json = serializer.encode(document)

            repository.importBackupJson(json)

            assertEquals(1, fakeGoldHoldingDao.allHoldings.size)
            assertEquals("SJC", fakeGoldHoldingDao.allHoldings[0].type)
        }

    @Test
    fun importBackupJson_emptyGoldHoldings_preservesExistingGold() =
        runTest {
            fakeGoldHoldingDao.allHoldings.add(
                GoldHoldingEntity(
                    id = 99L,
                    type = "GOLD_24K",
                    weightValue = 1.0,
                    weightUnit = "GRAM",
                    buyPricePerUnit = 1L,
                    currencyCode = "VND",
                    buyDateMillis = TestData.FIXED_TIME,
                    note = null,
                    createdAt = TestData.FIXED_TIME,
                    updatedAt = TestData.FIXED_TIME,
                ),
            )
            val document =
                TestData.sampleBackupDocument.copy(goldHoldings = emptyList())
            val json = serializer.encode(document)

            repository.importBackupJson(json)

            // Existing gold preserved when backup has no gold holdings
            assertEquals(1, fakeGoldHoldingDao.allHoldings.size)
            assertEquals(99L, fakeGoldHoldingDao.allHoldings[0].id)
        }

    // --- Gold price export/import tests ---

    @Test
    fun exportBackupJson_includesGoldPrices() =
        runTest {
            fakeGoldPriceDao.allPrices.add(
                GoldPriceEntity(
                    type = "SJC",
                    unit = "TAEL",
                    pricePerUnit = 92_000_000L,
                    currencyCode = "VND",
                    updatedAt = TestData.FIXED_TIME,
                ),
            )

            val json = repository.exportBackupJson()
            val document = serializer.decode(json)!!

            assertEquals(1, document.goldPrices.size)
            assertEquals("SJC", document.goldPrices[0].type)
            assertEquals(92_000_000L, document.goldPrices[0].pricePerUnit)
        }

    @Test
    fun importBackupJson_withGoldPrices_insertsPriceData() =
        runTest {
            val document =
                TestData.sampleBackupDocument.copy(
                    goldPrices = listOf(TestData.sampleBackupGoldPriceDto),
                )
            val json = serializer.encode(document)

            val result = repository.importBackupJson(json)

            assertEquals(1, result.goldPriceCount)
            assertEquals(1, fakeGoldPriceDao.allPrices.size)
            assertEquals("SJC", fakeGoldPriceDao.allPrices[0].type)
        }

    @Test
    fun importBackupJson_emptyGoldPrices_preservesExistingPrices() =
        runTest {
            fakeGoldPriceDao.allPrices.add(
                GoldPriceEntity(
                    type = "GOLD_24K",
                    unit = "GRAM",
                    pricePerUnit = 2_000_000L,
                    currencyCode = "VND",
                    updatedAt = TestData.FIXED_TIME,
                ),
            )
            val document = TestData.sampleBackupDocument.copy(goldPrices = emptyList())
            val json = serializer.encode(document)

            repository.importBackupJson(json)

            assertEquals(1, fakeGoldPriceDao.allPrices.size)
            assertEquals("GOLD_24K", fakeGoldPriceDao.allPrices[0].type)
        }

    @Test
    fun importBackupJson_withGoldPrices_deletesExistingPrices() =
        runTest {
            fakeGoldPriceDao.allPrices.add(
                GoldPriceEntity(
                    type = "GOLD_24K",
                    unit = "GRAM",
                    pricePerUnit = 2_000_000L,
                    currencyCode = "VND",
                    updatedAt = TestData.FIXED_TIME,
                ),
            )
            val document =
                TestData.sampleBackupDocument.copy(
                    goldPrices = listOf(TestData.sampleBackupGoldPriceDto),
                )
            val json = serializer.encode(document)

            repository.importBackupJson(json)

            assertEquals(1, fakeGoldPriceDao.allPrices.size)
            assertEquals("SJC", fakeGoldPriceDao.allPrices[0].type)
        }

    @Test
    fun importBackupJson_goldHoldingCount_returnsZeroForNoGold() =
        runTest {
            val json = serializer.encode(TestData.sampleBackupDocument)
            val result = repository.importBackupJson(json)

            assertEquals(0, result.goldHoldingCount)
        }

    @Test(expected = BackupValidationException::class)
    fun importBackup_invalidSchemaVersion_throwsValidationException() =
        runTest {
            val invalidDocument = TestData.sampleBackupDocument.copy(schemaVersion = 99)
            val json = serializer.encode(invalidDocument)
            val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))

            repository.importBackup(inputStream)
        }

    // Fakes

    private inner class FakeTransactionRunner : TransactionRunner {
        override suspend fun <R> runInTransaction(block: suspend () -> R): R = block()
    }

    private inner class FakeTransactionDao : TransactionDao {
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

        override suspend fun getAllOrdered(): List<TransactionEntity> = allTransactions.sortedBy { it.timestamp }

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

        override fun searchTransactions(
            from: Long,
            to: Long,
            query: String,
            type: Int?,
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override suspend fun getMonthlyExpenseTotals(
            from: Long,
            to: Long,
            currencyCode: String,
        ): List<dev.tuandoan.expensetracker.data.database.entity.MonthlyTotalRow> = emptyList()

        override suspend fun reassignCategory(
            fromId: Long,
            toId: Long,
        ) {}
    }

    private inner class FakeCategoryDao : CategoryDao {
        val allCategories = mutableListOf<CategoryEntity>()
        var deleteAllOrder = 0

        override fun getCategories(type: Int): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())

        override suspend fun getById(id: Long): CategoryEntity? = null

        override suspend fun getAll(): List<CategoryEntity> = allCategories.toList()

        override suspend fun count(): Int = allCategories.size

        override suspend fun insertAll(list: List<CategoryEntity>) {
            allCategories.addAll(list)
        }

        override suspend fun insert(entity: CategoryEntity): Long = entity.id

        override suspend fun update(entity: CategoryEntity) {}

        override suspend fun deleteNonDefault(id: Long): Int = 0

        override fun getCategoriesWithCount(): Flow<List<CategoryWithCountRow>> = MutableStateFlow(emptyList())

        override suspend fun getByNameAndType(
            name: String,
            type: Int,
        ): CategoryEntity? = allCategories.firstOrNull { it.name == name && it.type == type }

        override suspend fun deleteAll() {
            deleteAllOrder = ++globalCallOrder
            allCategories.clear()
        }
    }

    private inner class FakeGoldPriceDao : GoldPriceDao {
        val allPrices = mutableListOf<GoldPriceEntity>()

        override fun observeAll(): Flow<List<GoldPriceEntity>> = MutableStateFlow(allPrices.toList())

        override suspend fun getAll(): List<GoldPriceEntity> = allPrices.toList()

        override suspend fun getByTypeAndUnit(
            type: String,
            unit: String,
        ): GoldPriceEntity? = allPrices.firstOrNull { it.type == type && it.unit == unit }

        override suspend fun upsert(entity: GoldPriceEntity) {
            allPrices.removeAll { it.type == entity.type && it.unit == entity.unit }
            allPrices.add(entity)
        }

        override suspend fun upsertAll(list: List<GoldPriceEntity>) {
            for (entity in list) {
                upsert(entity)
            }
        }

        override suspend fun deleteAll() {
            allPrices.clear()
        }
    }

    private inner class FakeGoldHoldingDao : GoldHoldingDao {
        val allHoldings = mutableListOf<GoldHoldingEntity>()

        override fun observeAll(): Flow<List<GoldHoldingEntity>> = MutableStateFlow(allHoldings.toList())

        override suspend fun getAll(): List<GoldHoldingEntity> = allHoldings.toList()

        override suspend fun getById(id: Long): GoldHoldingEntity? = allHoldings.firstOrNull { it.id == id }

        override suspend fun insert(entity: GoldHoldingEntity): Long {
            allHoldings.add(entity)
            return entity.id
        }

        override suspend fun update(entity: GoldHoldingEntity) {}

        override suspend fun deleteById(id: Long) {
            allHoldings.removeAll { it.id == id }
        }

        override suspend fun insertAll(list: List<GoldHoldingEntity>) {
            allHoldings.addAll(list)
        }

        override suspend fun deleteAll() {
            allHoldings.clear()
        }
    }

    private inner class FakeRecurringTransactionDao : RecurringTransactionDao {
        val allRecurring = mutableListOf<RecurringTransactionEntity>()

        override fun getAll(): Flow<List<RecurringTransactionEntity>> = MutableStateFlow(allRecurring.toList())

        override suspend fun getById(id: Long): RecurringTransactionEntity? = allRecurring.find { it.id == id }

        override suspend fun getDue(nowMillis: Long): List<RecurringTransactionEntity> = emptyList()

        override suspend fun insert(entity: RecurringTransactionEntity): Long = entity.id

        override suspend fun update(entity: RecurringTransactionEntity) {}

        override suspend fun deleteById(id: Long) {}

        override suspend fun updateNextDue(
            id: Long,
            nextDue: Long,
            now: Long,
        ) {}

        override suspend fun setActive(
            id: Long,
            active: Boolean,
            now: Long,
        ) {}

        override suspend fun getAllList(): List<RecurringTransactionEntity> = allRecurring.toList()

        override suspend fun insertAll(list: List<RecurringTransactionEntity>) {
            allRecurring.addAll(list)
        }

        override suspend fun deleteAll() {
            allRecurring.clear()
        }
    }
}
