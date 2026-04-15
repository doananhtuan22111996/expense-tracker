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
            assertEquals(90_000_000L, state.holdings[0].currentSellPricePerUnit)
            assertNotNull(state.summary)
            assertEquals(174_000_000L, state.summary!!.totalCost)
            assertEquals(180_000_000L, state.summary!!.totalMarketValue)
            assertEquals(6_000_000L, state.summary!!.marketPnL)
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
            assertNull(state.holdings[0].currentSellPricePerUnit)
            assertNull(state.summary)
            assertEquals(1, state.currentPrices.size)
            assertEquals(0L, state.currentPrices[0].sellPricePerUnit)
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
            fakeGoldRepository.pricesFlow.value = listOf(testPrice(sellPrice = 90_000_000L))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.holdings.size)
            assertNotNull(state.summary)
            // cost = (87M * 2) + (85M * 1) = 174M + 85M = 259M
            assertEquals(259_000_000L, state.summary!!.totalCost)
            // value = (90M * 2) + (90M * 1) = 180M + 90M = 270M
            assertEquals(270_000_000L, state.summary!!.totalMarketValue)
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
                    testPrice(type = GoldType.SJC, unit = GoldWeightUnit.TAEL, sellPrice = 90_000_000L),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // 2 distinct combos: SJC/TAEL and 24K/GRAM
            assertEquals(2, state.currentPrices.size)
        }

    // --- Buy-back price flow ---

    @Test
    fun init_loadsPortfolio_withBuyBackPrices() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())
            fakeGoldRepository.pricesFlow.value =
                listOf(testPrice(sellPrice = 93_000_000L, buyBackPrice = 91_000_000L))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(93_000_000L, state.holdings[0].currentSellPricePerUnit)
            assertEquals(91_000_000L, state.holdings[0].currentBuyBackPricePerUnit)
            assertNotNull(state.summary!!.totalLiquidationValue)
            // market = 93M * 2 = 186M, liquidation = 91M * 2 = 182M
            assertEquals(186_000_000L, state.summary!!.totalMarketValue)
            assertEquals(182_000_000L, state.summary!!.totalLiquidationValue)
        }

    @Test
    fun init_nullBuyBack_liquidationIsNull() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())
            fakeGoldRepository.pricesFlow.value = listOf(testPrice(sellPrice = 93_000_000L))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.holdings[0].currentBuyBackPricePerUnit)
            assertNull(state.summary!!.totalLiquidationValue)
        }

    @Test
    fun init_currentPrices_includesBuyBackPricePerUnit() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())
            fakeGoldRepository.pricesFlow.value =
                listOf(testPrice(sellPrice = 93_000_000L, buyBackPrice = 91_000_000L))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val price = viewModel.uiState.value.currentPrices[0]
            assertEquals(93_000_000L, price.sellPricePerUnit)
            assertEquals(91_000_000L, price.buyBackPricePerUnit)
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
                    (GoldType.SJC to GoldWeightUnit.TAEL) to PriceInput(sellPrice = 92_000_000L),
                )
            viewModel.savePrices(priceInputs)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showPricesUpdated)
            assertEquals(1, fakeGoldRepository.upsertedPrices.size)
            assertEquals(92_000_000L, fakeGoldRepository.upsertedPrices[0].sellPricePerUnit)
            assertNull(fakeGoldRepository.upsertedPrices[0].buyBackPricePerUnit)
            assertEquals("VND", fakeGoldRepository.upsertedPrices[0].currencyCode)
        }

    @Test
    fun savePrices_withBuyBack_persistsBothPrices() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel()
            advanceUntilIdle()

            val priceInputs =
                mapOf(
                    (GoldType.SJC to GoldWeightUnit.TAEL) to
                        PriceInput(sellPrice = 93_000_000L, buyBackPrice = 91_000_000L),
                )
            viewModel.savePrices(priceInputs)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showPricesUpdated)
            assertEquals(93_000_000L, fakeGoldRepository.upsertedPrices[0].sellPricePerUnit)
            assertEquals(91_000_000L, fakeGoldRepository.upsertedPrices[0].buyBackPricePerUnit)
        }

    @Test
    fun savePrices_error_setsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeGoldRepository.shouldThrowOnMutation = true
            viewModel.savePrices(
                mapOf(
                    (GoldType.SJC to GoldWeightUnit.TAEL) to PriceInput(sellPrice = 90_000_000L),
                ),
            )
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

            viewModel.savePrices(
                mapOf(
                    (GoldType.SJC to GoldWeightUnit.TAEL) to PriceInput(sellPrice = 90_000_000L),
                ),
            )
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.showPricesUpdated)

            viewModel.clearPricesUpdatedFlag()

            assertFalse(viewModel.uiState.value.showPricesUpdated)
        }

    // --- Mixed buy-back ---

    @Test
    fun init_mixedBuyBack_liquidationFallsBackToMarketValue() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Holding 1: SJC/TAEL with buy-back price
            // Holding 2: 24K/GRAM without buy-back price
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(
                        id = 1,
                        type = GoldType.SJC,
                        unit = GoldWeightUnit.TAEL,
                        weightValue = 2.0,
                        buyPrice = 87_000_000L,
                    ),
                    testHolding(
                        id = 2,
                        type = GoldType.GOLD_24K,
                        unit = GoldWeightUnit.GRAM,
                        weightValue = 10.0,
                        buyPrice = 2_000_000L,
                    ),
                )
            fakeGoldRepository.pricesFlow.value =
                listOf(
                    testPrice(
                        type = GoldType.SJC,
                        unit = GoldWeightUnit.TAEL,
                        sellPrice = 93_000_000L,
                        buyBackPrice = 91_000_000L,
                    ),
                    testPrice(
                        type = GoldType.GOLD_24K,
                        unit = GoldWeightUnit.GRAM,
                        sellPrice = 2_500_000L,
                        buyBackPrice = null,
                    ),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.summary)
            // At least one holding has buy-back → totalLiquidationValue is computed
            assertNotNull(state.summary!!.totalLiquidationValue)
            // Holding 1: liquidationValue = 91M * 2 = 182M
            // Holding 2: liquidationValue = null, falls back to marketValue = 2.5M * 10 = 25M
            // totalLiquidation = 182M + 25M = 207M
            assertEquals(207_000_000L, state.summary!!.totalLiquidationValue)
            // totalMarketValue = (93M * 2) + (2.5M * 10) = 186M + 25M = 211M
            assertEquals(211_000_000L, state.summary!!.totalMarketValue)
            // totalCost = (87M * 2) + (2M * 10) = 174M + 20M = 194M
            assertEquals(194_000_000L, state.summary!!.totalCost)
        }

    @Test
    fun init_allHoldingsHaveBuyBack_liquidationUsesOnlyBuyBack() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(
                        id = 1,
                        type = GoldType.SJC,
                        unit = GoldWeightUnit.TAEL,
                        weightValue = 2.0,
                        buyPrice = 87_000_000L,
                    ),
                    testHolding(
                        id = 2,
                        type = GoldType.GOLD_24K,
                        unit = GoldWeightUnit.GRAM,
                        weightValue = 10.0,
                        buyPrice = 2_000_000L,
                    ),
                )
            fakeGoldRepository.pricesFlow.value =
                listOf(
                    testPrice(
                        type = GoldType.SJC,
                        unit = GoldWeightUnit.TAEL,
                        sellPrice = 93_000_000L,
                        buyBackPrice = 91_000_000L,
                    ),
                    testPrice(
                        type = GoldType.GOLD_24K,
                        unit = GoldWeightUnit.GRAM,
                        sellPrice = 2_500_000L,
                        buyBackPrice = 2_400_000L,
                    ),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.summary!!.totalLiquidationValue)
            // All buy-back: no market fallback — pure liquidation
            // Holding 1: 91M * 2 = 182M, Holding 2: 2.4M * 10 = 24M → total = 206M
            assertEquals(206_000_000L, state.summary!!.totalLiquidationValue)
            // Market: 93M * 2 + 2.5M * 10 = 186M + 25M = 211M
            assertEquals(211_000_000L, state.summary!!.totalMarketValue)
        }

    @Test
    fun savePrices_emptyMap_doesNotCrash() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.savePrices(emptyMap())
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showPricesUpdated)
            assertTrue(fakeGoldRepository.upsertedPrices.isEmpty())
        }

    @Test
    fun savePrices_withBuyBackZero_persistsZeroNotNull() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel()
            advanceUntilIdle()

            val priceInputs =
                mapOf(
                    (GoldType.SJC to GoldWeightUnit.TAEL) to
                        PriceInput(sellPrice = 93_000_000L, buyBackPrice = 0L),
                )
            viewModel.savePrices(priceInputs)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showPricesUpdated)
            assertEquals(0L, fakeGoldRepository.upsertedPrices[0].buyBackPricePerUnit)
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
        sellPrice: Long = 90_000_000L,
        buyBackPrice: Long? = null,
    ) = GoldPrice(
        type = type,
        unit = unit,
        sellPricePerUnit = sellPrice,
        buyBackPricePerUnit = buyBackPrice,
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
