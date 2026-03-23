package dev.tuandoan.expensetracker.ui.screen.gold

import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldPrice
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
import dev.tuandoan.expensetracker.domain.repository.GoldRepository
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoldPortfolioViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeGoldRepository: FakeGoldRepository
    private lateinit var fakeCurrencyRepo: FakeCurrencyPreferenceRepository

    @Before
    fun setup() {
        fakeGoldRepository = FakeGoldRepository()
        fakeCurrencyRepo = FakeCurrencyPreferenceRepository("VND")
    }

    private fun createViewModel(): GoldPortfolioViewModel = GoldPortfolioViewModel(fakeGoldRepository, fakeCurrencyRepo)

    // --- Init / Load ---

    @Test
    fun initialState_isDefault() {
        val state = GoldPortfolioUiState()
        assertTrue(state.holdings.isEmpty())
        assertNull(state.summary)
        assertTrue(state.currentPrices.isEmpty())
        assertEquals("VND", state.currencyCode)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertNull(state.errorMessage)
        assertNull(state.lastDeletedHolding)
        assertFalse(state.showPricesUpdated)
    }

    @Test
    fun init_loadsPortfolio_withHoldingsAndPrices() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())
            fakeGoldRepository.pricesFlow.value = listOf(testPrice())

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isError)
            assertEquals(1, state.holdings.size)
            assertEquals(GoldType.SJC, state.holdings[0].holding.type)
            assertEquals(90_000_000L, state.holdings[0].currentPricePerUnit)
            assertNotNull(state.summary)
            assertEquals(174_000_000L, state.summary!!.totalCost)
            assertEquals(180_000_000L, state.summary!!.totalCurrentValue)
            assertEquals(6_000_000L, state.summary!!.totalPnL)
            assertEquals("VND", state.currencyCode)
        }

    @Test
    fun init_emptyHoldings_showsEmptyState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.holdings.isEmpty())
            assertNull(state.summary)
            assertTrue(state.currentPrices.isEmpty())
            assertFalse(state.isLoading)
        }

    @Test
    fun init_holdingsWithoutPrices_summaryIsNull() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.holdings.size)
            assertNull(state.holdings[0].currentPricePerUnit)
            assertNull(state.summary)
            assertEquals(1, state.currentPrices.size)
            assertEquals(0L, state.currentPrices[0].pricePerUnit)
        }

    @Test
    fun init_error_setsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.shouldThrow = true

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isError)
            assertFalse(state.isLoading)
            assertNotNull(state.errorMessage)
        }

    @Test
    fun init_multipleHoldingsSameType_summaryAggregates() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(id = 1, weightValue = 2.0, buyPrice = 87_000_000L),
                    testHolding(id = 2, weightValue = 1.0, buyPrice = 85_000_000L),
                )
            fakeGoldRepository.pricesFlow.value = listOf(testPrice(pricePerUnit = 90_000_000L))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.holdings.size)
            assertNotNull(state.summary)
            // cost = (87M * 2) + (85M * 1) = 174M + 85M = 259M
            assertEquals(259_000_000L, state.summary!!.totalCost)
            // value = (90M * 2) + (90M * 1) = 180M + 90M = 270M
            assertEquals(270_000_000L, state.summary!!.totalCurrentValue)
        }

    @Test
    fun init_currentPrices_containsDistinctCombos() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(id = 1, type = GoldType.SJC, unit = GoldWeightUnit.TAEL),
                    testHolding(id = 2, type = GoldType.SJC, unit = GoldWeightUnit.TAEL),
                    testHolding(id = 3, type = GoldType.GOLD_24K, unit = GoldWeightUnit.GRAM),
                )
            fakeGoldRepository.pricesFlow.value =
                listOf(
                    testPrice(type = GoldType.SJC, unit = GoldWeightUnit.TAEL, pricePerUnit = 90_000_000L),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // 2 distinct combos: SJC/TAEL and 24K/GRAM
            assertEquals(2, state.currentPrices.size)
        }

    // --- Delete / Undo ---

    @Test
    fun deleteHolding_setsLastDeletedHolding() =
        runTest(mainDispatcherRule.testDispatcher) {
            val holding = testHolding()
            fakeGoldRepository.holdingsFlow.value = listOf(holding)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.deleteHolding(holding)
            advanceUntilIdle()

            assertEquals(holding, viewModel.uiState.value.lastDeletedHolding)
            assertTrue(fakeGoldRepository.deletedIds.contains(holding.id))
        }

    @Test
    fun undoDelete_restoresHolding() =
        runTest(mainDispatcherRule.testDispatcher) {
            val holding = testHolding()
            fakeGoldRepository.holdingsFlow.value = listOf(holding)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.deleteHolding(holding)
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.lastDeletedHolding)

            viewModel.undoDelete()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.lastDeletedHolding)
            assertTrue(fakeGoldRepository.addedHoldings.contains(holding))
        }

    @Test
    fun undoDelete_noLastDeleted_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.undoDelete()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.lastDeletedHolding)
            assertTrue(fakeGoldRepository.addedHoldings.isEmpty())
        }

    @Test
    fun clearLastDeleted_clearsState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val holding = testHolding()
            fakeGoldRepository.holdingsFlow.value = listOf(holding)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.deleteHolding(holding)
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.lastDeletedHolding)

            viewModel.clearLastDeleted()

            assertNull(viewModel.uiState.value.lastDeletedHolding)
        }

    @Test
    fun deleteHolding_error_setsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val holding = testHolding()
            fakeGoldRepository.holdingsFlow.value = listOf(holding)

            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeGoldRepository.shouldThrowOnMutation = true
            viewModel.deleteHolding(holding)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isError)
            assertNull(viewModel.uiState.value.lastDeletedHolding)
        }

    @Test
    fun undoDelete_error_setsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val holding = testHolding()
            fakeGoldRepository.holdingsFlow.value = listOf(holding)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.deleteHolding(holding)
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.lastDeletedHolding)

            fakeGoldRepository.shouldThrowOnMutation = true
            viewModel.undoDelete()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isError)
        }

    // --- Save Prices ---

    @Test
    fun savePrices_callsRepositoryAndSetsFlag() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel()
            advanceUntilIdle()

            val priceInputs =
                mapOf(
                    (GoldType.SJC to GoldWeightUnit.TAEL) to 92_000_000L,
                )
            viewModel.savePrices(priceInputs)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showPricesUpdated)
            assertEquals(1, fakeGoldRepository.upsertedPrices.size)
            assertEquals(92_000_000L, fakeGoldRepository.upsertedPrices[0].pricePerUnit)
            assertEquals("VND", fakeGoldRepository.upsertedPrices[0].currencyCode)
        }

    @Test
    fun savePrices_error_setsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeGoldRepository.shouldThrowOnMutation = true
            viewModel.savePrices(mapOf((GoldType.SJC to GoldWeightUnit.TAEL) to 90_000_000L))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isError)
            assertFalse(viewModel.uiState.value.showPricesUpdated)
        }

    @Test
    fun clearPricesUpdatedFlag_clearsState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.savePrices(mapOf((GoldType.SJC to GoldWeightUnit.TAEL) to 90_000_000L))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showPricesUpdated)

            viewModel.clearPricesUpdatedFlag()

            assertFalse(viewModel.uiState.value.showPricesUpdated)
        }

    // --- Test Helpers ---

    private fun testHolding(
        id: Long = 1,
        type: GoldType = GoldType.SJC,
        weightValue: Double = 2.0,
        unit: GoldWeightUnit = GoldWeightUnit.TAEL,
        buyPrice: Long = 87_000_000L,
    ) = GoldHolding(
        id = id,
        type = type,
        weightValue = weightValue,
        weightUnit = unit,
        buyPricePerUnit = buyPrice,
        currencyCode = "VND",
        buyDateMillis = 1710000000000L,
    )

    private fun testPrice(
        type: GoldType = GoldType.SJC,
        unit: GoldWeightUnit = GoldWeightUnit.TAEL,
        pricePerUnit: Long = 90_000_000L,
    ) = GoldPrice(
        type = type,
        unit = unit,
        pricePerUnit = pricePerUnit,
        currencyCode = "VND",
    )

    private class FakeGoldRepository : GoldRepository {
        val holdingsFlow = MutableStateFlow<List<GoldHolding>>(emptyList())
        val pricesFlow = MutableStateFlow<List<GoldPrice>>(emptyList())
        var shouldThrow = false
        var shouldThrowOnMutation = false

        val deletedIds = mutableListOf<Long>()
        val addedHoldings = mutableListOf<GoldHolding>()
        val upsertedPrices = mutableListOf<GoldPrice>()

        override fun observeAllHoldings(): Flow<List<GoldHolding>> =
            if (shouldThrow) flow { throw RuntimeException("Test error") } else holdingsFlow

        override suspend fun getHolding(id: Long): GoldHolding? = holdingsFlow.value.firstOrNull { it.id == id }

        override suspend fun addHolding(holding: GoldHolding): Long {
            if (shouldThrowOnMutation) throw RuntimeException("Mutation error")
            addedHoldings.add(holding)
            return holding.id
        }

        override suspend fun updateHolding(holding: GoldHolding) {
            if (shouldThrowOnMutation) throw RuntimeException("Mutation error")
        }

        override suspend fun deleteHolding(id: Long) {
            if (shouldThrowOnMutation) throw RuntimeException("Mutation error")
            deletedIds.add(id)
        }

        override fun observeAllPrices(): Flow<List<GoldPrice>> =
            if (shouldThrow) flow { throw RuntimeException("Test error") } else pricesFlow

        override suspend fun getPrice(
            type: GoldType,
            unit: GoldWeightUnit,
        ): GoldPrice? = pricesFlow.value.firstOrNull { it.type == type && it.unit == unit }

        override suspend fun upsertPrice(price: GoldPrice) {
            if (shouldThrowOnMutation) throw RuntimeException("Mutation error")
            upsertedPrices.add(price)
        }

        override suspend fun upsertPrices(prices: List<GoldPrice>) {
            if (shouldThrowOnMutation) throw RuntimeException("Mutation error")
            upsertedPrices.addAll(prices)
        }
    }
}
