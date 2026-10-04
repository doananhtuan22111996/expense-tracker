package dev.tuandoan.expensetracker.ui.screen.gold

import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldPrice
import dev.tuandoan.expensetracker.domain.model.GoldSale
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
    fun savePrices_multipleTypes_persistsAll() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(id = 1, type = GoldType.SJC, unit = GoldWeightUnit.TAEL),
                    testHolding(id = 2, type = GoldType.GOLD_24K, unit = GoldWeightUnit.GRAM, buyPrice = 2_400_000L),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.savePrices(
                mapOf(
                    (GoldType.SJC to GoldWeightUnit.TAEL) to
                        PriceInput(sellPrice = 93_000_000L, buyBackPrice = 91_000_000L),
                    (GoldType.GOLD_24K to GoldWeightUnit.GRAM) to
                        PriceInput(sellPrice = 2_500_000L),
                ),
            )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showPricesUpdated)
            assertEquals(2, fakeGoldRepository.upsertedPrices.size)
            val sjc = fakeGoldRepository.upsertedPrices.first { it.type == GoldType.SJC }
            assertEquals(93_000_000L, sjc.sellPricePerUnit)
            assertEquals(91_000_000L, sjc.buyBackPricePerUnit)
            val gold24k = fakeGoldRepository.upsertedPrices.first { it.type == GoldType.GOLD_24K }
            assertEquals(2_500_000L, gold24k.sellPricePerUnit)
            assertNull(gold24k.buyBackPricePerUnit)
        }

    @Test
    fun savePrices_emptyMap_setsUpdatedFlagAndPersistsNothing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.savePrices(emptyMap())
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showPricesUpdated)
            assertTrue(fakeGoldRepository.upsertedPrices.isEmpty())
        }

    @Test
    fun savePrices_nullBuyBack_persistsAsNull() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.savePrices(
                mapOf(
                    (GoldType.SJC to GoldWeightUnit.TAEL) to
                        PriceInput(sellPrice = 93_000_000L, buyBackPrice = null),
                ),
            )
            advanceUntilIdle()

            assertEquals(1, fakeGoldRepository.upsertedPrices.size)
            assertEquals(93_000_000L, fakeGoldRepository.upsertedPrices[0].sellPricePerUnit)
            assertNull(fakeGoldRepository.upsertedPrices[0].buyBackPricePerUnit)
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

    // --- Clear Error / Retry ---

    @Test
    fun clearError_resetsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.shouldThrow = true
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isError)
            assertNotNull(viewModel.uiState.value.errorMessage)

            viewModel.clearError()

            assertFalse(viewModel.uiState.value.isError)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun retry_clearsErrorAndReloadsData() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.shouldThrow = true
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isError)

            fakeGoldRepository.shouldThrow = false
            fakeGoldRepository.holdingsFlow.value = listOf(testHolding())
            fakeGoldRepository.pricesFlow.value = listOf(testPrice(sellPrice = 93_000_000L))
            viewModel.retry()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isError)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(1, viewModel.uiState.value.holdings.size)
            assertNotNull(viewModel.uiState.value.summary)
        }

    // --- Allocations & Weight ---

    @Test
    fun init_computesTotalWeight_inGramsAndTaels() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(id = 1, type = GoldType.SJC, weightValue = 2.0, unit = GoldWeightUnit.TAEL),
                    testHolding(id = 2, type = GoldType.GOLD_24K, weightValue = 37.5, unit = GoldWeightUnit.GRAM),
                )
            fakeGoldRepository.pricesFlow.value = listOf(testPrice())

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            // 2 taels (75g) + 37.5g (1 tael) = 112.5g = 3.0 taels
            assertEquals(112.5, state.totalWeightGrams, 0.001)
            assertEquals(3.0, state.totalWeightTaels, 0.001)
            assertEquals(112.5, state.summary!!.totalWeightGrams, 0.001)
            assertEquals(3.0, state.summary!!.totalWeightTaels, 0.001)
        }

    @Test
    fun init_computesAllocations_byGoldType() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(id = 1, type = GoldType.SJC, weightValue = 1.0, unit = GoldWeightUnit.TAEL),
                    testHolding(id = 2, type = GoldType.GOLD_24K, weightValue = 1.0, unit = GoldWeightUnit.TAEL),
                )
            fakeGoldRepository.pricesFlow.value =
                listOf(
                    testPrice(type = GoldType.SJC, unit = GoldWeightUnit.TAEL, sellPrice = 90_000_000L),
                    testPrice(type = GoldType.GOLD_24K, unit = GoldWeightUnit.TAEL, sellPrice = 30_000_000L),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.allocations.size)
            // SJC value = 90M (75%), 24K value = 30M (25%)
            val sjcAlloc = state.allocations.first { it.type == GoldType.SJC }
            val gold24kAlloc = state.allocations.first { it.type == GoldType.GOLD_24K }
            assertEquals(75.0, sjcAlloc.percentageOfPortfolio, 0.01)
            assertEquals(25.0, gold24kAlloc.percentageOfPortfolio, 0.01)
            // SJC should be first because it has higher percentage
            assertEquals(GoldType.SJC, state.allocations[0].type)
            assertEquals(GoldType.GOLD_24K, state.allocations[1].type)
        }

    @Test
    fun init_allocationsWithoutPrices_usesCostBasis() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(id = 1, type = GoldType.SJC, weightValue = 1.0, buyPrice = 80_000_000L),
                    testHolding(id = 2, type = GoldType.GOLD_24K, weightValue = 1.0, buyPrice = 20_000_000L),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(2, state.allocations.size)
            val sjcAlloc = state.allocations.first { it.type == GoldType.SJC }
            val gold24kAlloc = state.allocations.first { it.type == GoldType.GOLD_24K }
            assertEquals(80.0, sjcAlloc.percentageOfPortfolio, 0.01)
            assertEquals(20.0, gold24kAlloc.percentageOfPortfolio, 0.01)
        }

    // --- Filter State ---

    @Test
    fun setTypeFilter_filtersHoldings() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(id = 1, type = GoldType.SJC),
                    testHolding(id = 2, type = GoldType.GOLD_24K),
                    testHolding(id = 3, type = GoldType.GOLD_18K),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.holdings.size)
            assertEquals(3, viewModel.uiState.value.totalHoldingsCount)
            assertFalse(viewModel.uiState.value.hasActiveFilter)

            // Filter to SJC
            viewModel.setTypeFilter(GoldType.SJC)
            advanceUntilIdle()

            val filteredState = viewModel.uiState.value
            assertEquals(1, filteredState.holdings.size)
            assertEquals(GoldType.SJC, filteredState.holdings[0].holding.type)
            assertEquals(3, filteredState.totalHoldingsCount)
            assertTrue(filteredState.hasActiveFilter)
            assertEquals(GoldType.SJC, filteredState.selectedTypeFilter)

            // Reset filter to All
            viewModel.setTypeFilter(null)
            advanceUntilIdle()

            assertEquals(3, viewModel.uiState.value.holdings.size)
            assertFalse(viewModel.uiState.value.hasActiveFilter)
            assertNull(viewModel.uiState.value.selectedTypeFilter)
        }

    @Test
    fun setTypeFilter_noMatchingHoldings_yieldsEmptyHoldingsList() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(id = 1, type = GoldType.SJC),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setTypeFilter(GoldType.OTHER)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.holdings.isEmpty())
            assertEquals(1, state.totalHoldingsCount)
            assertTrue(state.hasActiveFilter)
            assertEquals(GoldType.OTHER, state.selectedTypeFilter)
        }

    // --- Sort State ---

    @Test
    fun setSortOption_sortsByBuyDateAscAndDesc() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(id = 1, buyDateMillis = 1000L),
                    testHolding(id = 2, buyDateMillis = 3000L),
                    testHolding(id = 3, buyDateMillis = 2000L),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            // Default: BUY_DATE_DESC
            assertEquals(
                listOf(2L, 3L, 1L),
                viewModel.uiState.value.holdings
                    .map { it.holding.id },
            )

            // Sort BUY_DATE_ASC
            viewModel.setSortOption(GoldHoldingSortOption.BUY_DATE_ASC)
            advanceUntilIdle()

            assertEquals(
                listOf(1L, 3L, 2L),
                viewModel.uiState.value.holdings
                    .map { it.holding.id },
            )
            assertEquals(GoldHoldingSortOption.BUY_DATE_ASC, viewModel.uiState.value.sortOption)
        }

    @Test
    fun setSortOption_sortsByWeightDesc() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(id = 1, weightValue = 1.0, unit = GoldWeightUnit.TAEL), // 37.5g
                    testHolding(id = 2, weightValue = 50.0, unit = GoldWeightUnit.GRAM), // 50.0g
                    testHolding(id = 3, weightValue = 0.5, unit = GoldWeightUnit.TAEL), // 18.75g
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setSortOption(GoldHoldingSortOption.WEIGHT_DESC)
            advanceUntilIdle()

            // Heaviest first: id 2 (50g), id 1 (37.5g), id 3 (18.75g)
            assertEquals(
                listOf(2L, 1L, 3L),
                viewModel.uiState.value.holdings
                    .map { it.holding.id },
            )
        }

    @Test
    fun setSortOption_sortsByValueDesc() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    testHolding(id = 1, type = GoldType.SJC, weightValue = 1.0),
                    testHolding(id = 2, type = GoldType.GOLD_24K, weightValue = 1.0),
                    testHolding(id = 3, type = GoldType.GOLD_18K, weightValue = 1.0),
                )
            fakeGoldRepository.pricesFlow.value =
                listOf(
                    testPrice(type = GoldType.SJC, sellPrice = 50_000_000L),
                    testPrice(type = GoldType.GOLD_24K, sellPrice = 90_000_000L),
                    testPrice(type = GoldType.GOLD_18K, sellPrice = 70_000_000L),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setSortOption(GoldHoldingSortOption.VALUE_DESC)
            advanceUntilIdle()

            // Highest value first: id 2 (90M), id 3 (70M), id 1 (50M)
            assertEquals(
                listOf(2L, 3L, 1L),
                viewModel.uiState.value.holdings
                    .map { it.holding.id },
            )
        }

    @Test
    fun setSortOption_sortsByPnLDesc() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepository.holdingsFlow.value =
                listOf(
                    // buy 80M, sell 90M -> +12.5%
                    testHolding(id = 1, type = GoldType.SJC, weightValue = 1.0, buyPrice = 80_000_000L),
                    // buy 50M, sell 90M -> +80.0%
                    testHolding(id = 2, type = GoldType.GOLD_24K, weightValue = 1.0, buyPrice = 50_000_000L),
                    // buy 100M, sell 90M -> -10.0%
                    testHolding(id = 3, type = GoldType.GOLD_18K, weightValue = 1.0, buyPrice = 100_000_000L),
                )
            fakeGoldRepository.pricesFlow.value =
                listOf(
                    testPrice(type = GoldType.SJC, sellPrice = 90_000_000L),
                    testPrice(type = GoldType.GOLD_24K, sellPrice = 90_000_000L),
                    testPrice(type = GoldType.GOLD_18K, sellPrice = 90_000_000L),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setSortOption(GoldHoldingSortOption.PNL_DESC)
            advanceUntilIdle()

            // Highest P&L % first: id 2 (+80%), id 1 (+12.5%), id 3 (-10%)
            assertEquals(
                listOf(2L, 1L, 3L),
                viewModel.uiState.value.holdings
                    .map { it.holding.id },
            )
        }

    // --- Test Helpers ---

    private fun testHolding(
        id: Long = 1,
        type: GoldType = GoldType.SJC,
        weightValue: Double = 2.0,
        unit: GoldWeightUnit = GoldWeightUnit.TAEL,
        buyPrice: Long = 87_000_000L,
        buyDateMillis: Long = 1710000000000L,
    ) = GoldHolding(
        id = id,
        type = type,
        weightValue = weightValue,
        weightUnit = unit,
        buyPricePerUnit = buyPrice,
        currencyCode = "VND",
        buyDateMillis = buyDateMillis,
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

        val salesFlow = MutableStateFlow<List<GoldSale>>(emptyList())

        override fun observeAllSales(): Flow<List<GoldSale>> =
            if (shouldThrow) flow { throw RuntimeException("Test error") } else salesFlow

        override suspend fun getAllSales(): List<GoldSale> = salesFlow.value

        override suspend fun getSale(id: Long): GoldSale? = salesFlow.value.firstOrNull { it.id == id }

        override suspend fun recordSale(
            holdingId: Long,
            soldWeight: Double,
            sellPricePerUnit: Long,
            saleDateMillis: Long,
            note: String?,
        ): Long = 1L

        override suspend fun deleteSale(id: Long) {}
    }
}
