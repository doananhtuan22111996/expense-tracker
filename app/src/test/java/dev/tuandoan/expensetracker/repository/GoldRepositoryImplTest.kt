package dev.tuandoan.expensetracker.repository

import app.cash.turbine.test
import dev.tuandoan.expensetracker.data.database.dao.GoldHoldingDao
import dev.tuandoan.expensetracker.data.database.dao.GoldPriceDao
import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity
import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldPrice
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GoldRepositoryImplTest {
    private lateinit var fakeHoldingDao: FakeGoldHoldingDao
    private lateinit var fakePriceDao: FakeGoldPriceDao
    private val fakeTimeProvider = FakeTimeProvider()
    private lateinit var repository: GoldRepositoryImpl

    @Before
    fun setup() {
        fakeHoldingDao = FakeGoldHoldingDao()
        fakePriceDao = FakeGoldPriceDao()
        repository = GoldRepositoryImpl(fakeHoldingDao, fakePriceDao, fakeTimeProvider)
    }

    // --- Holdings: observe ---

    @Test
    fun observeAllHoldings_mapsEntitiesToDomain() =
        runTest {
            fakeHoldingDao.holdings.value = listOf(testHoldingEntity())

            repository.observeAllHoldings().test {
                val holdings = awaitItem()
                assertEquals(1, holdings.size)
                assertEquals(GoldType.SJC, holdings[0].type)
                assertEquals(2.0, holdings[0].weightValue, 0.001)
                assertEquals(GoldWeightUnit.TAEL, holdings[0].weightUnit)
                assertEquals(87_000_000L, holdings[0].buyPricePerUnit)
                assertEquals("VND", holdings[0].currencyCode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeAllHoldings_emptyList() =
        runTest {
            repository.observeAllHoldings().test {
                val holdings = awaitItem()
                assertEquals(0, holdings.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // --- Holdings: get ---

    @Test
    fun getHolding_existing_returnsMappedDomain() =
        runTest {
            fakeHoldingDao.holdingsById[1L] = testHoldingEntity()

            val holding = repository.getHolding(1L)

            assertNotNull(holding)
            assertEquals(GoldType.SJC, holding!!.type)
            assertEquals(2.0, holding.weightValue, 0.001)
        }

    @Test
    fun getHolding_nonExisting_returnsNull() =
        runTest {
            val holding = repository.getHolding(99L)
            assertNull(holding)
        }

    // --- Holdings: add ---

    @Test
    fun addHolding_insertsEntityWithTimestamps() =
        runTest {
            fakeTimeProvider.setCurrentMillis(5000L)

            val id = repository.addHolding(testHolding())

            assertEquals(100L, id)
            val inserted = fakeHoldingDao.lastInserted!!
            assertEquals("SJC", inserted.type)
            assertEquals("TAEL", inserted.weightUnit)
            assertEquals(87_000_000L, inserted.buyPricePerUnit)
            assertEquals(5000L, inserted.createdAt)
            assertEquals(5000L, inserted.updatedAt)
        }

    // --- Holdings: update ---

    @Test
    fun updateHolding_preservesCreatedAtAndSetsNewUpdatedAt() =
        runTest {
            fakeTimeProvider.setCurrentMillis(9000L)
            val holding = testHolding().copy(id = 1L, createdAt = 500L)

            repository.updateHolding(holding)

            val updated = fakeHoldingDao.lastUpdated!!
            assertEquals(500L, updated.createdAt)
            assertEquals(9000L, updated.updatedAt)
        }

    // --- Holdings: delete ---

    @Test
    fun deleteHolding_delegatesToDao() =
        runTest {
            repository.deleteHolding(42L)
            assertEquals(42L, fakeHoldingDao.lastDeletedId)
        }

    // --- Prices: observe ---

    @Test
    fun observeAllPrices_mapsEntitiesToDomain() =
        runTest {
            fakePriceDao.prices.value = listOf(testPriceEntity())

            repository.observeAllPrices().test {
                val prices = awaitItem()
                assertEquals(1, prices.size)
                assertEquals(GoldType.SJC, prices[0].type)
                assertEquals(GoldWeightUnit.TAEL, prices[0].unit)
                assertEquals(93_000_000L, prices[0].sellPricePerUnit)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // --- Prices: get ---

    @Test
    fun getPrice_existing_returnsMappedDomain() =
        runTest {
            fakePriceDao.pricesByKey["SJC|TAEL"] = testPriceEntity()

            val price = repository.getPrice(GoldType.SJC, GoldWeightUnit.TAEL)

            assertNotNull(price)
            assertEquals(93_000_000L, price!!.sellPricePerUnit)
        }

    @Test
    fun getPrice_nonExisting_returnsNull() =
        runTest {
            val price = repository.getPrice(GoldType.GOLD_24K, GoldWeightUnit.GRAM)
            assertNull(price)
        }

    // --- Prices: upsert ---

    @Test
    fun upsertPrice_delegatesWithTimestamp() =
        runTest {
            fakeTimeProvider.setCurrentMillis(7000L)

            repository.upsertPrice(testPrice())

            val upserted = fakePriceDao.lastUpserted!!
            assertEquals("SJC", upserted.type)
            assertEquals("TAEL", upserted.unit)
            assertEquals(93_000_000L, upserted.pricePerUnit)
            assertEquals(7000L, upserted.updatedAt)
        }

    @Test
    fun upsertPrices_delegatesBatchWithTimestamp() =
        runTest {
            fakeTimeProvider.setCurrentMillis(8000L)
            val prices =
                listOf(
                    testPrice(),
                    GoldPrice(
                        type = GoldType.GOLD_24K,
                        unit = GoldWeightUnit.GRAM,
                        sellPricePerUnit = 2_480_000L,
                    ),
                )

            repository.upsertPrices(prices)

            assertEquals(2, fakePriceDao.lastUpsertedBatch!!.size)
            assertEquals(8000L, fakePriceDao.lastUpsertedBatch!![0].updatedAt)
            assertEquals(8000L, fakePriceDao.lastUpsertedBatch!![1].updatedAt)
        }

    @Test
    fun observeAllPrices_mapsBuyBackPricePerUnit() =
        runTest {
            fakePriceDao.prices.value =
                listOf(testPriceEntity().copy(buyBackPricePerUnit = 91_000_000L))

            repository.observeAllPrices().test {
                val prices = awaitItem()
                assertEquals(91_000_000L, prices[0].buyBackPricePerUnit)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun upsertPrice_mapsBuyBackPricePerUnit() =
        runTest {
            fakeTimeProvider.setCurrentMillis(7000L)

            repository.upsertPrice(testPrice().copy(buyBackPricePerUnit = 91_000_000L))

            val upserted = fakePriceDao.lastUpserted!!
            assertEquals(91_000_000L, upserted.buyBackPricePerUnit)
        }

    // --- Helpers ---

    private fun testHoldingEntity() =
        GoldHoldingEntity(
            id = 1L,
            type = "SJC",
            weightValue = 2.0,
            weightUnit = "TAEL",
            buyPricePerUnit = 87_000_000L,
            currencyCode = "VND",
            buyDateMillis = 1000L,
            note = null,
            createdAt = 100L,
            updatedAt = 100L,
        )

    private fun testHolding() =
        GoldHolding(
            type = GoldType.SJC,
            weightValue = 2.0,
            weightUnit = GoldWeightUnit.TAEL,
            buyPricePerUnit = 87_000_000L,
            buyDateMillis = 1000L,
        )

    private fun testPriceEntity() =
        GoldPriceEntity(
            type = "SJC",
            unit = "TAEL",
            pricePerUnit = 93_000_000L,
            currencyCode = "VND",
            updatedAt = 100L,
        )

    private fun testPrice() =
        GoldPrice(
            type = GoldType.SJC,
            unit = GoldWeightUnit.TAEL,
            sellPricePerUnit = 93_000_000L,
        )

    // --- Fake DAOs ---

    private class FakeGoldHoldingDao : GoldHoldingDao {
        val holdings = MutableStateFlow<List<GoldHoldingEntity>>(emptyList())
        val holdingsById = mutableMapOf<Long, GoldHoldingEntity>()
        var lastInserted: GoldHoldingEntity? = null
        var lastUpdated: GoldHoldingEntity? = null
        var lastDeletedId: Long? = null

        override fun observeAll(): Flow<List<GoldHoldingEntity>> = holdings

        override suspend fun getAll(): List<GoldHoldingEntity> = holdings.value

        override suspend fun getById(id: Long): GoldHoldingEntity? = holdingsById[id]

        override suspend fun insert(entity: GoldHoldingEntity): Long {
            lastInserted = entity
            return 100L
        }

        override suspend fun update(entity: GoldHoldingEntity) {
            lastUpdated = entity
        }

        override suspend fun deleteById(id: Long) {
            lastDeletedId = id
        }

        override suspend fun insertAll(list: List<GoldHoldingEntity>) {}

        override suspend fun deleteAll() {}
    }

    private class FakeGoldPriceDao : GoldPriceDao {
        val prices = MutableStateFlow<List<GoldPriceEntity>>(emptyList())
        val pricesByKey = mutableMapOf<String, GoldPriceEntity>()
        var lastUpserted: GoldPriceEntity? = null
        var lastUpsertedBatch: List<GoldPriceEntity>? = null

        override fun observeAll(): Flow<List<GoldPriceEntity>> = prices

        override suspend fun getAll(): List<GoldPriceEntity> = prices.value

        override suspend fun getByTypeAndUnit(
            type: String,
            unit: String,
        ): GoldPriceEntity? = pricesByKey["$type|$unit"]

        override suspend fun upsert(entity: GoldPriceEntity) {
            lastUpserted = entity
        }

        override suspend fun upsertAll(list: List<GoldPriceEntity>) {
            lastUpsertedBatch = list
        }

        override suspend fun deleteAll() {}
    }
}
