package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TripDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.CategoryWithCountRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencyCategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencySumRow
import dev.tuandoan.expensetracker.data.database.entity.MonthlyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.data.database.entity.TripEntity
import dev.tuandoan.expensetracker.domain.model.DeleteTripBehavior
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.model.TripFilter
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class TripRepositoryImplTest {
    private lateinit var tripDao: FakeTripDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var transactionRunner: SnapshotTransactionRunner
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var repository: TripRepositoryImpl

    private val now = 1_700_000_000_000L

    @Before
    fun setup() {
        tripDao = FakeTripDao()
        transactionDao = FakeTransactionDao()
        categoryDao = FakeCategoryDao()
        transactionRunner = SnapshotTransactionRunner(tripDao, transactionDao, categoryDao)
        timeProvider = FakeTimeProvider(currentMillis = now)
        repository =
            TripRepositoryImpl(
                tripDao = tripDao,
                transactionDao = transactionDao,
                categoryDao = categoryDao,
                transactionRunner = transactionRunner,
                timeProvider = timeProvider,
            )
    }

    // ───────────────────────────────────────────────────────────
    //  observe + create + update
    // ───────────────────────────────────────────────────────────

    @Test
    fun createTrip_persistsAndReturnsId() =
        runTest {
            val id =
                repository.createTrip(
                    name = "Da Nang",
                    destination = "Da Nang",
                    startDateEpochDay = 100L,
                    endDateEpochDay = 105L,
                    foreignCurrencyCode = null,
                    foreignToHomeRate = null,
                )
            val list = repository.observeTrips(TripFilter.All).first()
            assertEquals(1, list.size)
            assertEquals(id, list[0].id)
            assertEquals("Da Nang", list[0].name)
            assertNull("createTrip never sets snapshot fields", list[0].originalCategoryId)
            assertEquals(now, list[0].createdAt)
        }

    @Test
    fun updateTrip_persistsChanges() =
        runTest {
            val id =
                repository.createTrip(
                    name = "Hue",
                    destination = null,
                    startDateEpochDay = 100L,
                    endDateEpochDay = 105L,
                    foreignCurrencyCode = null,
                    foreignToHomeRate = null,
                )
            val original = repository.getTripById(id)!!
            repository.updateTrip(original.copy(name = "Hue 2026", destination = "Hue, Vietnam"))
            val updated = repository.getTripById(id)!!
            assertEquals("Hue 2026", updated.name)
            assertEquals("Hue, Vietnam", updated.destination)
        }

    @Test
    fun observeTripById_emitsNullForMissingThenValueAfterInsert() =
        runTest {
            assertNull(repository.observeTripById(42L).first())
            val id =
                repository.createTrip(
                    name = "Tokyo",
                    destination = "Tokyo",
                    startDateEpochDay = 200L,
                    endDateEpochDay = 207L,
                    foreignCurrencyCode = "JPY",
                    foreignToHomeRate = 165.0,
                )
            val emitted = repository.observeTripById(id).first()
            assertNotNull(emitted)
            assertEquals("Tokyo", emitted!!.name)
        }

    @Test
    fun observeTrips_filtersByActive() =
        runTest {
            // Active overlaps day 100
            tripDao.seed(tripEntity(id = 1, name = "Active", start = 95, end = 105))
            tripDao.seed(tripEntity(id = 2, name = "Upcoming", start = 110, end = 115))
            tripDao.seed(tripEntity(id = 3, name = "Past", start = 80, end = 90))

            val active = repository.observeTrips(TripFilter.Active(nowEpochDay = 100)).first()
            assertEquals(listOf("Active"), active.map { it.name })
        }

    @Test
    fun observeTrips_filtersByUpcomingAndPast() =
        runTest {
            tripDao.seed(tripEntity(id = 1, name = "Active", start = 95, end = 105))
            tripDao.seed(tripEntity(id = 2, name = "Upcoming", start = 110, end = 115))
            tripDao.seed(tripEntity(id = 3, name = "Past", start = 80, end = 90))

            val upcoming = repository.observeTrips(TripFilter.Upcoming(nowEpochDay = 100)).first()
            val past = repository.observeTrips(TripFilter.Past(nowEpochDay = 100)).first()

            assertEquals(listOf("Upcoming"), upcoming.map { it.name })
            assertEquals(listOf("Past"), past.map { it.name })
        }

    // ───────────────────────────────────────────────────────────
    //  deleteTrip(UNTAG)
    // ───────────────────────────────────────────────────────────

    @Test
    fun deleteTripUntag_clearsTripIdButPreservesCategoryAndConversionState() =
        runTest {
            val tripId = tripDao.seed(tripEntity(id = 7, name = "T")).id
            transactionDao.seed(
                txEntity(
                    id = 1,
                    categoryId = 5,
                    tripId = tripId,
                    originalCategoryId = 99, // pretend this came from a past conversion
                    amountForeignMinor = 1200,
                ),
            )

            repository.deleteTrip(tripId, DeleteTripBehavior.UNTAG)

            val tx = transactionDao.getById(1)!!
            assertNull("trip_id cleared", tx.tripId)
            assertEquals("category_id preserved", 5L, tx.categoryId)
            assertEquals("original_category_id preserved (UNTAG breadcrumb)", 99L, tx.originalCategoryId)
            assertEquals("amount_foreign_minor preserved", 1200L, tx.amountForeignMinor)
            assertEquals("updated_at bumped", now, tx.updatedAt)
            assertNull(tripDao.getById(tripId))
        }

    // ───────────────────────────────────────────────────────────
    //  deleteTrip(REVERT_TO_ORIGINAL_CATEGORY)
    // ───────────────────────────────────────────────────────────

    @Test
    fun deleteTripRevert_happyPath_categoryStillPresent() =
        runTest {
            // Source category still exists post-conversion (sourceDisposition was KEEP)
            categoryDao.seed(categoryEntity(id = 42, name = "Da Nang"))
            val tripId =
                tripDao
                    .seed(
                        tripEntity(
                            id = 8,
                            name = "Da Nang May",
                            originalCategoryId = 42,
                            originalCategoryNameSnapshot = "Da Nang",
                            originalCategoryIconSnapshot = "place",
                            originalCategoryColorSnapshot = "amber",
                        ),
                    ).id
            transactionDao.seed(
                txEntity(
                    id = 1,
                    categoryId = 5, // some real category ("Food") assigned during conversion
                    tripId = tripId,
                    originalCategoryId = 42,
                    amountForeignMinor = null,
                ),
            )

            repository.deleteTrip(tripId, DeleteTripBehavior.REVERT_TO_ORIGINAL_CATEGORY)

            // Source category still exists at id 42 — no duplicate created
            assertNotNull(categoryDao.getById(42))
            assertEquals(1, categoryDao.allEntities().count { it.id == 42L })

            val tx = transactionDao.getById(1)!!
            assertEquals("category_id reverted to source", 42L, tx.categoryId)
            assertNull(tx.tripId)
            assertNull(tx.originalCategoryId)
            assertNull(tx.amountForeignMinor)
            assertEquals(now, tx.updatedAt)
            assertNull(tripDao.getById(tripId))
        }

    @Test
    fun deleteTripRevert_recreatesSourceCategoryWhenDeleted() =
        runTest {
            // sourceDisposition was DELETE on commit → source category gone
            val tripId =
                tripDao
                    .seed(
                        tripEntity(
                            id = 8,
                            name = "Hue Apr",
                            originalCategoryId = 99, // points at a category that no longer exists
                            originalCategoryNameSnapshot = "Hue",
                            originalCategoryIconSnapshot = "place",
                            originalCategoryColorSnapshot = "blue",
                        ),
                    ).id
            transactionDao.seed(
                txEntity(id = 1, categoryId = 5, tripId = tripId, originalCategoryId = 99),
            )

            repository.deleteTrip(tripId, DeleteTripBehavior.REVERT_TO_ORIGINAL_CATEGORY)

            // A new category was created from the snapshot
            val recreated = categoryDao.allEntities().firstOrNull { it.name == "Hue" }
            assertNotNull("category recreated from snapshot", recreated)
            assertEquals("place", recreated!!.iconKey)
            assertEquals("blue", recreated.colorKey)

            val tx = transactionDao.getById(1)!!
            assertEquals("tx points at the freshly-created category", recreated.id, tx.categoryId)
            assertNull(tx.tripId)
            assertNull(tx.originalCategoryId)
            assertNull(tripDao.getById(tripId))
        }

    @Test
    fun deleteTripRevert_throwsForNonConversionOriginTrip() =
        runTest {
            val tripId =
                tripDao
                    .seed(
                        tripEntity(id = 9, name = "Plain", originalCategoryId = null),
                    ).id
            try {
                repository.deleteTrip(tripId, DeleteTripBehavior.REVERT_TO_ORIGINAL_CATEGORY)
                fail("expected IllegalArgumentException")
            } catch (expected: IllegalArgumentException) {
                assertTrue(
                    expected.message.orEmpty().contains("conversion-origin"),
                )
            }
            // Trip stays put — failed atomic op rolls back
            assertNotNull(tripDao.getById(tripId))
        }

    // ───────────────────────────────────────────────────────────
    //  Atomicity (ADR-001 mandatory test)
    // ───────────────────────────────────────────────────────────

    @Test
    fun deleteTripRevert_isAtomic_rollsBackWhenTripDeleteFails() =
        runTest {
            categoryDao.seed(categoryEntity(id = 42, name = "Da Nang"))
            val tripId =
                tripDao
                    .seed(
                        tripEntity(
                            id = 8,
                            name = "Da Nang May",
                            originalCategoryId = 42,
                            originalCategoryNameSnapshot = "Da Nang",
                            originalCategoryIconSnapshot = "place",
                            originalCategoryColorSnapshot = "amber",
                        ),
                    ).id
            transactionDao.seed(
                txEntity(id = 1, categoryId = 5, tripId = tripId, originalCategoryId = 42),
            )
            // Force the final `deleteById` step to throw — the transaction MUST roll back.
            tripDao.failNextDeleteById = true

            try {
                repository.deleteTrip(tripId, DeleteTripBehavior.REVERT_TO_ORIGINAL_CATEGORY)
                fail("expected IllegalStateException from forced failure")
            } catch (expected: IllegalStateException) {
                // expected
            }

            // Pre-revert state restored
            val tx = transactionDao.getById(1)!!
            assertEquals("category_id NOT reverted (rolled back)", 5L, tx.categoryId)
            assertEquals("trip_id NOT cleared (rolled back)", tripId, tx.tripId)
            assertEquals("original_category_id NOT cleared (rolled back)", 42L, tx.originalCategoryId)
            assertNotNull("trip NOT deleted (rolled back)", tripDao.getById(tripId))
        }

    // ───────────────────────────────────────────────────────────
    //  Helpers
    // ───────────────────────────────────────────────────────────

    private fun tripEntity(
        id: Long,
        name: String,
        start: Long = 100,
        end: Long = 105,
        originalCategoryId: Long? = null,
        originalCategoryNameSnapshot: String? = null,
        originalCategoryIconSnapshot: String? = null,
        originalCategoryColorSnapshot: String? = null,
    ) = TripEntity(
        id = id,
        name = name,
        destination = null,
        startDateEpochDay = start,
        endDateEpochDay = end,
        foreignCurrencyCode = null,
        foreignToHomeRate = null,
        originalCategoryId = originalCategoryId,
        originalCategoryNameSnapshot = originalCategoryNameSnapshot,
        originalCategoryIconSnapshot = originalCategoryIconSnapshot,
        originalCategoryColorSnapshot = originalCategoryColorSnapshot,
        createdAt = now,
    )

    private fun categoryEntity(
        id: Long,
        name: String,
    ) = CategoryEntity(
        id = id,
        name = name,
        type = CategoryEntity.TYPE_EXPENSE,
        iconKey = "place",
        colorKey = "amber",
        isDefault = false,
    )

    private fun txEntity(
        id: Long,
        categoryId: Long,
        tripId: Long?,
        originalCategoryId: Long? = null,
        amountForeignMinor: Long? = null,
    ) = TransactionEntity(
        id = id,
        type = TransactionEntity.TYPE_EXPENSE,
        amount = 50_000L,
        currencyCode = "VND",
        categoryId = categoryId,
        note = null,
        timestamp = now,
        createdAt = now,
        updatedAt = now,
        tripId = tripId,
        originalCategoryId = originalCategoryId,
        amountForeignMinor = amountForeignMinor,
    )
}

// ───────────────────────────────────────────────────────────
//  Fakes
// ───────────────────────────────────────────────────────────

/**
 * Snapshots all three fakes' state at runInTransaction entry; on throw, restores. This is
 * the contract `RoomTransactionRunner` provides for free via SQLite — the in-memory fakes
 * have to do it manually so the atomicity test bites.
 */
private class SnapshotTransactionRunner(
    private val tripDao: FakeTripDao,
    private val transactionDao: FakeTransactionDao,
    private val categoryDao: FakeCategoryDao,
) : TransactionRunner {
    var transactionCount = 0

    override suspend fun <R> runInTransaction(block: suspend () -> R): R {
        transactionCount++
        val tripsSnapshot = tripDao.snapshot()
        val txSnapshot = transactionDao.snapshot()
        val catSnapshot = categoryDao.snapshot()
        return try {
            block()
        } catch (t: Throwable) {
            tripDao.restore(tripsSnapshot)
            transactionDao.restore(txSnapshot)
            categoryDao.restore(catSnapshot)
            throw t
        }
    }
}

private class FakeTripDao : TripDao {
    private val flow = MutableStateFlow<List<TripEntity>>(emptyList())
    private var nextId = 1L
    var failNextDeleteById = false

    fun seed(entity: TripEntity): TripEntity {
        val resolvedId = if (entity.id == 0L) nextId++ else entity.id.also { nextId = maxOf(nextId, it + 1) }
        val stored = entity.copy(id = resolvedId)
        flow.value = flow.value + stored
        return stored
    }

    fun snapshot(): List<TripEntity> = flow.value

    fun restore(snapshot: List<TripEntity>) {
        flow.value = snapshot
    }

    override fun observeAll(): Flow<List<TripEntity>> = flow

    override fun observeActive(nowEpochDay: Long): Flow<List<TripEntity>> =
        MutableStateFlow(flow.value.filter { it.startDateEpochDay <= nowEpochDay && it.endDateEpochDay >= nowEpochDay })

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
        if (failNextDeleteById) {
            failNextDeleteById = false
            error("forced failure for atomicity test")
        }
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

private class FakeCategoryDao : CategoryDao {
    private var entities = listOf<CategoryEntity>()
    private var nextId = 1L

    fun seed(entity: CategoryEntity) {
        val id = if (entity.id == 0L) nextId++ else entity.id.also { nextId = maxOf(nextId, it + 1) }
        entities = entities + entity.copy(id = id)
    }

    fun allEntities(): List<CategoryEntity> = entities

    fun snapshot(): List<CategoryEntity> = entities

    fun restore(snapshot: List<CategoryEntity>) {
        entities = snapshot
    }

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

    fun seed(entity: TransactionEntity) {
        entities = entities + entity
    }

    fun snapshot(): List<TransactionEntity> = entities

    fun restore(snapshot: List<TransactionEntity>) {
        entities = snapshot
    }

    override fun getTransactions(
        from: Long,
        to: Long,
        type: Int?,
    ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

    override fun searchTransactions(
        from: Long,
        to: Long,
        query: String,
        type: Int?,
    ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

    override fun searchTransactionsAdvanced(
        from: Long?,
        to: Long?,
        query: String,
        type: Int?,
        categoryId: Long?,
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

    override suspend fun deleteAll() {
        entities = emptyList()
    }

    override fun sumExpenseByCurrency(
        from: Long,
        to: Long,
    ): Flow<List<CurrencySumRow>> = MutableStateFlow(emptyList())

    override suspend fun getExpenseTotalsByCurrency(
        from: Long,
        to: Long,
    ): List<CurrencySumRow> = emptyList()

    override fun sumIncomeByCurrency(
        from: Long,
        to: Long,
    ): Flow<List<CurrencySumRow>> = MutableStateFlow(emptyList())

    override suspend fun getMonthlyExpenseTotals(
        from: Long,
        to: Long,
        currencyCode: String,
    ): List<MonthlyTotalRow> = emptyList()

    override fun sumByCurrencyAndCategory(
        from: Long,
        to: Long,
        type: Int,
    ): Flow<List<CurrencyCategorySumRow>> = MutableStateFlow(emptyList())
}

@Suppress("unused")
private fun List<Trip>.assertContainsName(name: String) {
    assertTrue("expected name=$name in $this", any { it.name == name })
}

@Suppress("unused")
private fun List<Trip>.assertDoesNotContainName(name: String) {
    assertFalse("did not expect name=$name in $this", any { it.name == name })
}
