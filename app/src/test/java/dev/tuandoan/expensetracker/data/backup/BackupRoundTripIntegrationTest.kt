package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.GoldHoldingDao
import dev.tuandoan.expensetracker.data.database.dao.GoldPriceDao
import dev.tuandoan.expensetracker.data.database.dao.RecurringTransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TripDao
import dev.tuandoan.expensetracker.data.database.dao.TripQueriesDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.CategoryWithCountRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencyCategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencySumRow
import dev.tuandoan.expensetracker.data.database.entity.DailyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity
import dev.tuandoan.expensetracker.data.database.entity.MonthlyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.RecurringTransactionEntity
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.data.database.entity.TripCategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.TripEntity
import dev.tuandoan.expensetracker.data.export.CsvExporter
import dev.tuandoan.expensetracker.domain.analytics.NoOpAnalytics
import dev.tuandoan.expensetracker.domain.crash.NoOpCrashReporter
import dev.tuandoan.expensetracker.domain.model.ConversionDraft
import dev.tuandoan.expensetracker.domain.model.DeleteTripBehavior
import dev.tuandoan.expensetracker.repository.TripRepositoryImpl
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

/**
 * Mandatory ADR-001 Integration Test:
 * Backup round-trip: convert → backup → restore → revert → equals pre-conversion.
 */
class BackupRoundTripIntegrationTest {
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var tripDao: FakeTripDao
    private lateinit var recurringDao: FakeRecurringDao
    private lateinit var goldHoldingDao: FakeGoldHoldingDao
    private lateinit var goldPriceDao: FakeGoldPriceDao
    private lateinit var tripQueriesDao: FakeTripQueriesDao
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var tripRepository: TripRepositoryImpl
    private lateinit var backupRepository: BackupRepositoryImpl

    private val fixedTime = 1_700_000_000_000L

    @Before
    fun setup() {
        categoryDao = FakeCategoryDao()
        transactionDao = FakeTransactionDao()
        tripDao = FakeTripDao()
        recurringDao = FakeRecurringDao()
        goldHoldingDao = FakeGoldHoldingDao()
        goldPriceDao = FakeGoldPriceDao()
        tripQueriesDao = FakeTripQueriesDao()
        timeProvider = FakeTimeProvider(currentMillis = fixedTime)

        val runner =
            object : TransactionRunner {
                override suspend fun <R> runInTransaction(block: suspend () -> R): R = block()
            }

        tripRepository =
            TripRepositoryImpl(
                tripDao = tripDao,
                tripQueriesDao = tripQueriesDao,
                transactionDao = transactionDao,
                categoryDao = categoryDao,
                transactionRunner = runner,
                timeProvider = timeProvider,
            )

        backupRepository =
            BackupRepositoryImpl(
                categoryDao = categoryDao,
                transactionDao = transactionDao,
                recurringTransactionDao = recurringDao,
                goldHoldingDao = goldHoldingDao,
                goldPriceDao = goldPriceDao,
                tripDao = tripDao,
                backupValidator = BackupValidator(),
                backupSerializer = BackupSerializer(),
                backupAssembler = BackupAssembler(),
                timeProvider = timeProvider,
                transactionRunner = runner,
                currencyPreferenceRepository = FakeCurrencyPreferenceRepository(),
                csvExporter = CsvExporter(ZoneId.of("UTC")),
                crashReporter = NoOpCrashReporter(),
                backupCrypto = BackupCrypto(),
                analytics = NoOpAnalytics(),
            )
    }

    @Test
    fun roundTrip_convert_backup_restore_revert_equalsPreConversionState_withSourceDeleted() =
        runTest {
            // ── 1. PRE-CONVERSION STATE ─────────────────────────────
            val sourceCategory =
                CategoryEntity(
                    id = 10L,
                    name = "Da Nang Vacation",
                    type = CategoryEntity.TYPE_EXPENSE,
                    iconKey = "beach_access",
                    colorKey = "#2196F3",
                    isDefault = false,
                )
            val foodCategory =
                CategoryEntity(
                    id = 20L,
                    name = "Food & Dining",
                    type = CategoryEntity.TYPE_EXPENSE,
                    iconKey = "restaurant",
                    colorKey = "#F44336",
                    isDefault = false,
                )
            categoryDao.insertAll(listOf(sourceCategory, foodCategory))

            val tx1 =
                TransactionEntity(
                    id = 101L,
                    type = TransactionEntity.TYPE_EXPENSE,
                    amount = 150000L,
                    currencyCode = "VND",
                    categoryId = sourceCategory.id,
                    note = "Seafood dinner",
                    timestamp = fixedTime,
                    createdAt = fixedTime,
                    updatedAt = fixedTime,
                )
            val tx2 =
                TransactionEntity(
                    id = 102L,
                    type = TransactionEntity.TYPE_EXPENSE,
                    amount = 45000L,
                    currencyCode = "VND",
                    categoryId = sourceCategory.id,
                    note = "Coffee by the beach",
                    timestamp = fixedTime + 1000L,
                    createdAt = fixedTime + 1000L,
                    updatedAt = fixedTime + 1000L,
                )
            transactionDao.insertAll(listOf(tx1, tx2))

            // Snapshot pre-conversion state
            val preConversionCategories = categoryDao.getAll().sortedBy { it.id }
            val preConversionTransactions = transactionDao.getAll().sortedBy { it.id }

            assertEquals(2, preConversionCategories.size)
            assertEquals(2, preConversionTransactions.size)

            // ── 2. CONVERT CATEGORY TO TRIP (wizard commit with DELETE) ──
            val draft =
                ConversionDraft(
                    sourceCategoryId = sourceCategory.id,
                    sourceCategorySnapshot =
                        ConversionDraft.CategorySnapshot(
                            name = sourceCategory.name,
                            iconKey = sourceCategory.iconKey ?: "beach_access",
                            colorKey = sourceCategory.colorKey ?: "#2196F3",
                        ),
                    tripMetadata =
                        ConversionDraft.TripMetadata(
                            name = "Da Nang 2026",
                            destination = "Da Nang",
                            startDateEpochDay = 20000L,
                            endDateEpochDay = 20005L,
                            foreignCurrencyCode = null,
                            foreignToHomeRate = null,
                        ),
                    rowDecisions =
                        listOf(
                            ConversionDraft.RowDecision.Migrate(
                                transactionId = tx1.id,
                                newCategoryId = foodCategory.id,
                            ),
                            ConversionDraft.RowDecision.Migrate(
                                transactionId = tx2.id,
                                newCategoryId = foodCategory.id,
                            ),
                        ),
                    sourceDisposition = ConversionDraft.SourceDisposition.DELETE,
                )

            val createdTripId = tripRepository.commitConversion(draft)

            // Verify conversion intermediate state
            assertNull("Source category should be deleted", categoryDao.getById(sourceCategory.id))
            val createdTrip = tripDao.getById(createdTripId)
            assertNotNull(createdTrip)
            assertEquals("Da Nang 2026", createdTrip!!.name)
            assertEquals(sourceCategory.id, createdTrip.originalCategoryId)
            assertEquals(sourceCategory.name, createdTrip.originalCategoryNameSnapshot)

            val postConversionTx1 = transactionDao.getById(tx1.id)!!
            assertEquals(foodCategory.id, postConversionTx1.categoryId)
            assertEquals(createdTripId, postConversionTx1.tripId)
            assertEquals(sourceCategory.id, postConversionTx1.originalCategoryId)

            // ── 3. EXPORT BACKUP ────────────────────────────────────
            val backupJson = backupRepository.exportBackupJson()
            assertTrue(backupJson.contains("\"schema_version\": 2"))
            assertTrue(backupJson.contains("\"Da Nang 2026\""))
            assertTrue(backupJson.contains("\"original_category_id\": 10"))

            // ── 4. RESTORE BACKUP ON CLEAN DATABASE ────────────────
            categoryDao.deleteAll()
            transactionDao.deleteAll()
            tripDao.deleteAll()

            val restoreResult = backupRepository.importBackupJson(backupJson)
            assertEquals(1, restoreResult.categoryCount) // only Food category restored
            assertEquals(2, restoreResult.transactionCount)
            assertEquals(1, restoreResult.tripCount)

            // ── 5. REVERT TRIP (ADR-001) ────────────────────────────
            tripRepository.deleteTrip(createdTripId, DeleteTripBehavior.REVERT_TO_ORIGINAL_CATEGORY)

            // ── 6. VERIFY FINAL EQUALS PRE-CONVERSION STATE ──────────
            val postRevertCategories = categoryDao.getAll().sortedBy { it.id }
            val postRevertTransactions = transactionDao.getAll().sortedBy { it.id }

            // Category assertions: original category recreated with exact fields
            assertEquals(preConversionCategories.size, postRevertCategories.size)
            val restoredSourceCategory = postRevertCategories.first { it.name == sourceCategory.name }
            assertEquals(sourceCategory.name, restoredSourceCategory.name)
            assertEquals(sourceCategory.iconKey, restoredSourceCategory.iconKey)
            assertEquals(sourceCategory.colorKey, restoredSourceCategory.colorKey)
            assertEquals(sourceCategory.type, restoredSourceCategory.type)

            // Transaction assertions: transactions reverted to source category with null trip fields
            assertEquals(preConversionTransactions.size, postRevertTransactions.size)
            for (i in preConversionTransactions.indices) {
                val preTx = preConversionTransactions[i]
                val postTx = postRevertTransactions[i]

                assertEquals(preTx.id, postTx.id)
                assertEquals(preTx.type, postTx.type)
                assertEquals(preTx.amount, postTx.amount)
                assertEquals(preTx.currencyCode, postTx.currencyCode)
                assertEquals(
                    "Transaction categoryId should equal source category id",
                    restoredSourceCategory.id,
                    postTx.categoryId,
                )
                assertEquals(preTx.note, postTx.note)
                assertEquals(preTx.timestamp, postTx.timestamp)
                assertNull("tripId must be null after revert", postTx.tripId)
                assertNull("originalCategoryId must be null after revert", postTx.originalCategoryId)
                assertNull("amountForeignMinor must be null after revert", postTx.amountForeignMinor)
            }

            // Trip deleted
            assertNull("Trip entity must be deleted", tripDao.getById(createdTripId))
            assertTrue("Trip table must be empty", tripDao.getAllList().isEmpty())
        }

    @Test
    fun roundTrip_convert_backup_restore_revert_equalsPreConversionState_withSourceKept() =
        runTest {
            // ── 1. PRE-CONVERSION STATE ─────────────────────────────
            val sourceCategory =
                CategoryEntity(
                    id = 30L,
                    name = "Hue Trip",
                    type = CategoryEntity.TYPE_EXPENSE,
                    iconKey = "location_city",
                    colorKey = "#9C27B0",
                    isDefault = false,
                )
            val foodCategory =
                CategoryEntity(
                    id = 40L,
                    name = "Food & Dining",
                    type = CategoryEntity.TYPE_EXPENSE,
                    iconKey = "restaurant",
                    colorKey = "#F44336",
                    isDefault = false,
                )
            categoryDao.insertAll(listOf(sourceCategory, foodCategory))

            val tx1 =
                TransactionEntity(
                    id = 201L,
                    type = TransactionEntity.TYPE_EXPENSE,
                    amount = 80000L,
                    currencyCode = "VND",
                    categoryId = sourceCategory.id,
                    note = "Bun bo Hue",
                    timestamp = fixedTime,
                    createdAt = fixedTime,
                    updatedAt = fixedTime,
                )
            transactionDao.insertAll(listOf(tx1))

            val preCategories = categoryDao.getAll().sortedBy { it.id }
            val preTransactions = transactionDao.getAll().sortedBy { it.id }

            // ── 2. CONVERT WITH KEEP DISPOSITION ────────────────────
            val draft =
                ConversionDraft(
                    sourceCategoryId = sourceCategory.id,
                    sourceCategorySnapshot =
                        ConversionDraft.CategorySnapshot(
                            name = sourceCategory.name,
                            iconKey = sourceCategory.iconKey ?: "location_city",
                            colorKey = sourceCategory.colorKey ?: "#9C27B0",
                        ),
                    tripMetadata =
                        ConversionDraft.TripMetadata(
                            name = "Hue Citadel 2026",
                            destination = "Hue",
                            startDateEpochDay = 20100L,
                            endDateEpochDay = 20103L,
                            foreignCurrencyCode = null,
                            foreignToHomeRate = null,
                        ),
                    rowDecisions =
                        listOf(
                            ConversionDraft.RowDecision.Migrate(
                                transactionId = tx1.id,
                                newCategoryId = foodCategory.id,
                            ),
                        ),
                    sourceDisposition = ConversionDraft.SourceDisposition.KEEP,
                )

            val tripId = tripRepository.commitConversion(draft)

            // Source category kept
            assertNotNull(categoryDao.getById(sourceCategory.id))

            // ── 3. EXPORT & RESTORE ─────────────────────────────────
            val backupJson = backupRepository.exportBackupJson()

            categoryDao.deleteAll()
            transactionDao.deleteAll()
            tripDao.deleteAll()

            backupRepository.importBackupJson(backupJson)

            // ── 4. REVERT ───────────────────────────────────────────
            tripRepository.deleteTrip(tripId, DeleteTripBehavior.REVERT_TO_ORIGINAL_CATEGORY)

            // ── 5. VERIFY EQUALITY ──────────────────────────────────
            val postCategories = categoryDao.getAll().sortedBy { it.id }
            val postTransactions = transactionDao.getAll().sortedBy { it.id }

            assertEquals(preCategories.size, postCategories.size)
            assertEquals(sourceCategory.id, postCategories.first { it.name == sourceCategory.name }.id)

            val revertedTx = postTransactions.first { it.id == tx1.id }
            assertEquals(sourceCategory.id, revertedTx.categoryId)
            assertNull(revertedTx.tripId)
            assertNull(revertedTx.originalCategoryId)

            assertNull(tripDao.getById(tripId))
        }

    // ───────────────────────────────────────────────────────────
    // Fakes
    // ───────────────────────────────────────────────────────────

    private class FakeCategoryDao : CategoryDao {
        private var entities = listOf<CategoryEntity>()
        private var nextId = 1L

        override fun getCategories(type: Int): Flow<List<CategoryEntity>> =
            MutableStateFlow(entities.filter { it.type == type })

        override suspend fun getById(id: Long): CategoryEntity? = entities.firstOrNull { it.id == id }

        override suspend fun getAll(): List<CategoryEntity> = entities

        override suspend fun count(): Int = entities.size

        override suspend fun insertAll(list: List<CategoryEntity>) {
            list.forEach { insert(it) }
        }

        override suspend fun insert(entity: CategoryEntity): Long {
            val id = if (entity.id == 0L) nextId++ else entity.id.also { nextId = maxOf(nextId, it + 1) }
            entities = entities + entity.copy(id = id)
            return id
        }

        override suspend fun update(entity: CategoryEntity) {
            entities = entities.map { if (it.id == entity.id) entity else it }
        }

        override suspend fun deleteNonDefault(id: Long): Int {
            val before = entities.size
            entities = entities.filterNot { it.id == id && !it.isDefault }
            return before - entities.size
        }

        override fun getCategoriesWithCount(): Flow<List<CategoryWithCountRow>> = MutableStateFlow(emptyList())

        override suspend fun getByNameAndType(
            name: String,
            type: Int,
        ): CategoryEntity? = entities.firstOrNull { it.name.equals(name, ignoreCase = true) && it.type == type }

        override suspend fun deleteAll() {
            entities = emptyList()
        }
    }

    private class FakeTransactionDao : TransactionDao {
        private var entities = listOf<TransactionEntity>()

        override fun getTransactions(
            from: Long,
            to: Long,
            type: Int?,
            excludeTrips: Int,
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override fun searchTransactions(
            from: Long,
            to: Long,
            query: String,
            type: Int?,
            excludeTrips: Int,
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override fun searchTransactionsAdvanced(
            from: Long?,
            to: Long?,
            query: String,
            type: Int?,
            categoryId: Long?,
            excludeTrips: Int,
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override suspend fun insert(entity: TransactionEntity): Long {
            entities = entities + entity
            return entity.id
        }

        override suspend fun update(entity: TransactionEntity) {
            entities = entities.map { if (it.id == entity.id) entity else it }
        }

        override suspend fun deleteById(id: Long) {
            entities = entities.filter { it.id != id }
        }

        override suspend fun getById(id: Long): TransactionEntity? = entities.firstOrNull { it.id == id }

        override suspend fun getAll(): List<TransactionEntity> = entities

        override suspend fun getAllOrdered(): List<TransactionEntity> = entities

        override suspend fun insertAll(list: List<TransactionEntity>) {
            entities = entities + list
        }

        override suspend fun reassignCategory(
            fromId: Long,
            toId: Long,
        ) {
            entities = entities.map { if (it.categoryId == fromId) it.copy(categoryId = toId) else it }
        }

        override suspend fun clearTripId(
            tripId: Long,
            now: Long,
        ) {
            entities =
                entities.map {
                    if (it.tripId == tripId) it.copy(tripId = null, updatedAt = now) else it
                }
        }

        override suspend fun revertTripAssignments(
            tripId: Long,
            restoredCategoryId: Long,
            now: Long,
        ) {
            entities =
                entities.map {
                    if (it.tripId == tripId) {
                        it.copy(
                            categoryId = restoredCategoryId,
                            tripId = null,
                            originalCategoryId = null,
                            amountForeignMinor = null,
                            updatedAt = now,
                        )
                    } else {
                        it
                    }
                }
        }

        override suspend fun migrateToTrip(
            transactionId: Long,
            tripId: Long,
            newCategoryId: Long,
            originalCategoryId: Long,
            now: Long,
        ) {
            entities =
                entities.map {
                    if (it.id == transactionId) {
                        it.copy(
                            tripId = tripId,
                            categoryId = newCategoryId,
                            originalCategoryId = originalCategoryId,
                            updatedAt = now,
                        )
                    } else {
                        it
                    }
                }
        }

        override fun observeByTripId(tripId: Long): Flow<List<TransactionEntity>> =
            MutableStateFlow(entities.filter { it.tripId == tripId })

        override suspend fun deleteAll() {
            entities = emptyList()
        }

        override fun sumExpenseByCurrency(
            from: Long,
            to: Long,
            excludeTrips: Int,
        ): Flow<List<CurrencySumRow>> = MutableStateFlow(emptyList())

        override suspend fun getExpenseTotalsByCurrency(
            from: Long,
            to: Long,
            excludeTrips: Int,
        ): List<CurrencySumRow> = emptyList()

        override fun sumIncomeByCurrency(
            from: Long,
            to: Long,
            excludeTrips: Int,
        ): Flow<List<CurrencySumRow>> = MutableStateFlow(emptyList())

        override suspend fun getMonthlyExpenseTotals(
            from: Long,
            to: Long,
            currencyCode: String,
            excludeTrips: Int,
        ): List<MonthlyTotalRow> = emptyList()

        override fun sumByCurrencyAndCategory(
            from: Long,
            to: Long,
            type: Int,
            excludeTrips: Int,
        ): Flow<List<CurrencyCategorySumRow>> = MutableStateFlow(emptyList())
    }

    private class FakeTripDao : TripDao {
        private val flow = MutableStateFlow<List<TripEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<TripEntity>> = flow

        override fun observeActive(nowEpochDay: Long): Flow<List<TripEntity>> =
            MutableStateFlow(
                flow.value.filter {
                    it.startDateEpochDay <= nowEpochDay &&
                        it.endDateEpochDay >= nowEpochDay
                },
            )

        override fun observeUpcoming(nowEpochDay: Long): Flow<List<TripEntity>> =
            MutableStateFlow(flow.value.filter { it.startDateEpochDay > nowEpochDay })

        override fun observePast(nowEpochDay: Long): Flow<List<TripEntity>> =
            MutableStateFlow(flow.value.filter { it.endDateEpochDay < nowEpochDay })

        override fun observeById(id: Long): Flow<TripEntity?> = MutableStateFlow(flow.value.firstOrNull { it.id == id })

        override suspend fun getById(id: Long): TripEntity? = flow.value.firstOrNull { it.id == id }

        override suspend fun insert(entity: TripEntity): Long {
            val id = if (entity.id == 0L) nextId++ else entity.id.also { nextId = maxOf(nextId, it + 1) }
            flow.value = flow.value + entity.copy(id = id)
            return id
        }

        override suspend fun update(entity: TripEntity) {
            flow.value = flow.value.map { if (it.id == entity.id) entity else it }
        }

        override suspend fun deleteById(id: Long) {
            flow.value = flow.value.filter { it.id != id }
        }

        override suspend fun getAllList(): List<TripEntity> = flow.value

        override suspend fun insertAll(list: List<TripEntity>) {
            list.forEach { insert(it) }
        }

        override suspend fun deleteAll() {
            flow.value = emptyList()
        }
    }

    private class FakeTripQueriesDao : TripQueriesDao {
        override fun observeTotal(tripId: Long): Flow<Long?> = MutableStateFlow(null)

        override fun observeTransactionCount(tripId: Long): Flow<Int> = MutableStateFlow(0)

        override fun observeDailyTotals(tripId: Long): Flow<List<DailyTotalRow>> = MutableStateFlow(emptyList())

        override fun observeCategoryBreakdown(tripId: Long): Flow<List<TripCategorySumRow>> =
            MutableStateFlow(emptyList())

        override fun observeHasForeignTransactions(tripId: Long): Flow<Boolean> = MutableStateFlow(false)
    }

    private class FakeRecurringDao : RecurringTransactionDao {
        override fun getAll(): Flow<List<RecurringTransactionEntity>> = MutableStateFlow(emptyList())

        override suspend fun getAllList(): List<RecurringTransactionEntity> = emptyList()

        override suspend fun insertAll(list: List<RecurringTransactionEntity>) {}

        override suspend fun deleteAll() {}

        override suspend fun getById(id: Long): RecurringTransactionEntity? = null

        override suspend fun insert(entity: RecurringTransactionEntity): Long = 0L

        override suspend fun update(entity: RecurringTransactionEntity) {}

        override suspend fun deleteById(id: Long) {}

        override suspend fun getDue(nowMillis: Long): List<RecurringTransactionEntity> = emptyList()

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
    }

    private class FakeGoldHoldingDao : GoldHoldingDao {
        override suspend fun getAll(): List<GoldHoldingEntity> = emptyList()

        override suspend fun insertAll(list: List<GoldHoldingEntity>) {}

        override suspend fun deleteAll() {}

        override fun observeAll(): Flow<List<GoldHoldingEntity>> = MutableStateFlow(emptyList())

        override suspend fun getById(id: Long): GoldHoldingEntity? = null

        override suspend fun insert(entity: GoldHoldingEntity): Long = 0L

        override suspend fun update(entity: GoldHoldingEntity) {}

        override suspend fun deleteById(id: Long) {}
    }

    private class FakeGoldPriceDao : GoldPriceDao {
        override suspend fun getAll(): List<GoldPriceEntity> = emptyList()

        override suspend fun upsertAll(list: List<GoldPriceEntity>) {}

        override suspend fun deleteAll() {}

        override fun observeAll(): Flow<List<GoldPriceEntity>> = MutableStateFlow(emptyList())

        override suspend fun getByTypeAndUnit(
            type: String,
            unit: String,
        ): GoldPriceEntity? = null

        override suspend fun upsert(entity: GoldPriceEntity) {}
    }
}
