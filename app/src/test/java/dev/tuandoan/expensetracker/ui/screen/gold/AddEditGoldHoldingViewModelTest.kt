package dev.tuandoan.expensetracker.ui.screen.gold

import androidx.lifecycle.SavedStateHandle
import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldPrice
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
import dev.tuandoan.expensetracker.domain.repository.GoldRepository
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class AddEditGoldHoldingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeGoldRepo: FakeGoldRepository
    private lateinit var fakeCurrencyRepo: FakeCurrencyPreferenceRepository
    private lateinit var fakeTimeProvider: FakeTimeProvider

    @Before
    fun setup() {
        fakeGoldRepo = FakeGoldRepository()
        fakeCurrencyRepo = FakeCurrencyPreferenceRepository("VND")
        fakeTimeProvider = FakeTimeProvider()
    }

    private fun createViewModel(holdingId: Long = 0L): AddEditGoldHoldingViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("holdingId" to holdingId))
        return AddEditGoldHoldingViewModel(
            fakeGoldRepo,
            fakeCurrencyRepo,
            fakeTimeProvider,
            savedStateHandle,
        )
    }

    // --- Add mode ---

    @Test
    fun addMode_loadsDefaults() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(viewModel.isEditMode)
            assertFalse(state.isLoading)
            assertNull(state.originalHolding)
            assertEquals("VND", state.currencyCode)
            assertEquals(GoldType.SJC, state.type)
            assertEquals(GoldWeightUnit.TAEL, state.weightUnit)
            assertEquals(fakeTimeProvider.currentTimeMillis(), state.buyDateMillis)
        }

    @Test
    fun addMode_formValidation_emptyWeight() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onBuyPriceChanged("90000000")
            assertFalse(viewModel.uiState.value.isFormValid)
        }

    @Test
    fun addMode_formValidation_emptyPrice() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("1.0")
            assertFalse(viewModel.uiState.value.isFormValid)
        }

    @Test
    fun addMode_formValidation_validForm() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("2.0")
            viewModel.onBuyPriceChanged("87000000")
            assertTrue(viewModel.uiState.value.isFormValid)
            assertTrue(viewModel.uiState.value.isSaveEnabled)
        }

    @Test
    fun addMode_saveHolding_callsRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onTypeChanged(GoldType.GOLD_24K)
            viewModel.onWeightChanged("1.5")
            viewModel.onWeightUnitChanged(GoldWeightUnit.GRAM)
            viewModel.onBuyPriceChanged("2000000")
            viewModel.onNoteChanged("test note")

            var successCalled = false
            viewModel.saveHolding { successCalled = true }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertEquals(1, fakeGoldRepo.addedHoldings.size)
            val added = fakeGoldRepo.addedHoldings[0]
            assertEquals(GoldType.GOLD_24K, added.type)
            assertEquals(1.5, added.weightValue, 0.001)
            assertEquals(GoldWeightUnit.GRAM, added.weightUnit)
            assertEquals(2000000L, added.buyPricePerUnit)
            assertEquals("VND", added.currencyCode)
            assertEquals("test note", added.note)
        }

    @Test
    fun addMode_saveHolding_invalidWeight_showsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onBuyPriceChanged("90000000")
            // No weight set

            viewModel.saveHolding {}
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun addMode_saveHolding_invalidPrice_showsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("1.0")
            // No price set

            viewModel.saveHolding {}
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.errorMessage)
        }

    // --- Edit mode ---

    @Test
    fun editMode_loadsExistingHolding() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepo.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel(holdingId = 1L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(viewModel.isEditMode)
            assertFalse(state.isLoading)
            assertNotNull(state.originalHolding)
            assertEquals(GoldType.SJC, state.type)
            assertEquals("2.0", state.weightText)
            assertEquals(GoldWeightUnit.TAEL, state.weightUnit)
            assertEquals("87000000", state.buyPriceText)
            assertEquals("VND", state.currencyCode)
        }

    @Test
    fun editMode_holdingNotFound_showsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            // holdingsFlow is empty — getHolding returns null
            val viewModel = createViewModel(holdingId = 999L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.errorMessage)
            assertFalse(state.isLoading)
        }

    @Test
    fun editMode_hasUnsavedChanges_detectsChange() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepo.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel(holdingId = 1L)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.hasUnsavedChanges)

            viewModel.onWeightChanged("3.0")
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun editMode_isSaveEnabled_requiresChanges() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepo.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel(holdingId = 1L)
            advanceUntilIdle()

            // Form valid but no changes — save disabled
            assertTrue(viewModel.uiState.value.isFormValid)
            assertFalse(viewModel.uiState.value.isSaveEnabled)

            viewModel.onTypeChanged(GoldType.GOLD_24K)
            assertTrue(viewModel.uiState.value.isSaveEnabled)
        }

    @Test
    fun editMode_saveHolding_updatesRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepo.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel(holdingId = 1L)
            advanceUntilIdle()

            viewModel.onWeightChanged("3.0")

            var successCalled = false
            viewModel.saveHolding { successCalled = true }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertEquals(1, fakeGoldRepo.updatedHoldings.size)
            assertEquals(3.0, fakeGoldRepo.updatedHoldings[0].weightValue, 0.001)
        }

    // --- Error / clear ---

    @Test
    fun clearError_clearsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.saveHolding {} // will fail with empty form
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.errorMessage)

            viewModel.clearError()
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun onFieldChanges_updateState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onTypeChanged(GoldType.GOLD_18K)
            assertEquals(GoldType.GOLD_18K, viewModel.uiState.value.type)

            viewModel.onWeightChanged("5.0")
            assertEquals("5.0", viewModel.uiState.value.weightText)

            viewModel.onWeightUnitChanged(GoldWeightUnit.OUNCE)
            assertEquals(GoldWeightUnit.OUNCE, viewModel.uiState.value.weightUnit)

            viewModel.onBuyPriceChanged("100000")
            assertEquals("100000", viewModel.uiState.value.buyPriceText)

            viewModel.onDateSelected(1234567890L)
            assertEquals(1234567890L, viewModel.uiState.value.buyDateMillis)

            viewModel.onNoteChanged("my note")
            assertEquals("my note", viewModel.uiState.value.note)
        }

    // --- Boundary / Edge cases ---

    @Test
    fun addMode_formValidation_zeroWeight_invalid() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("0")
            viewModel.onBuyPriceChanged("90000000")
            assertFalse(viewModel.uiState.value.isFormValid)
        }

    @Test
    fun addMode_formValidation_negativeWeight_invalid() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("-1")
            viewModel.onBuyPriceChanged("90000000")
            assertFalse(viewModel.uiState.value.isFormValid)
        }

    @Test
    fun addMode_formValidation_zeroBuyPrice_invalid() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("1.0")
            viewModel.onBuyPriceChanged("0")
            assertFalse(viewModel.uiState.value.isFormValid)
        }

    @Test
    fun addMode_saveHolding_blankNote_becomesNull() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("1.0")
            viewModel.onBuyPriceChanged("90000000")
            viewModel.onNoteChanged("   ")

            viewModel.saveHolding {}
            advanceUntilIdle()

            assertEquals(1, fakeGoldRepo.addedHoldings.size)
            assertNull(fakeGoldRepo.addedHoldings[0].note)
        }

    @Test
    fun addMode_saveHolding_repositoryThrows_showsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepo.addShouldThrow = true

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onWeightChanged("1.0")
            viewModel.onBuyPriceChanged("90000000")

            var successCalled = false
            viewModel.saveHolding { successCalled = true }
            advanceUntilIdle()

            assertFalse(successCalled)
            assertNotNull(viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun editMode_saveHolding_setsUpdatedAt() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepo.holdingsFlow.value = listOf(testHolding())
            fakeTimeProvider.setCurrentMillis(9999999L)

            val viewModel = createViewModel(holdingId = 1L)
            advanceUntilIdle()

            viewModel.onWeightChanged("3.0")
            viewModel.saveHolding {}
            advanceUntilIdle()

            assertEquals(9999999L, fakeGoldRepo.updatedHoldings[0].updatedAt)
        }

    @Test
    fun editMode_hasUnsavedChanges_detectsEachField() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepo.holdingsFlow.value = listOf(testHolding())

            val viewModel = createViewModel(holdingId = 1L)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.hasUnsavedChanges)

            // Type change
            viewModel.onTypeChanged(GoldType.GOLD_24K)
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)
            viewModel.onTypeChanged(GoldType.SJC) // revert
            assertFalse(viewModel.uiState.value.hasUnsavedChanges)

            // Unit change
            viewModel.onWeightUnitChanged(GoldWeightUnit.GRAM)
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)
            viewModel.onWeightUnitChanged(GoldWeightUnit.TAEL) // revert
            assertFalse(viewModel.uiState.value.hasUnsavedChanges)

            // Price change
            viewModel.onBuyPriceChanged("99000000")
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)
            viewModel.onBuyPriceChanged("87000000") // revert
            assertFalse(viewModel.uiState.value.hasUnsavedChanges)

            // Date change
            viewModel.onDateSelected(1234567890L)
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)
            viewModel.onDateSelected(1710000000000L) // revert
            assertFalse(viewModel.uiState.value.hasUnsavedChanges)

            // Note change (original note is null → empty string in state)
            viewModel.onNoteChanged("new note")
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)
            viewModel.onNoteChanged("") // revert (blank → null matches original null)
            assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun addMode_loadInitialData_exception_showsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeGoldRepo.getHoldingShouldThrow = true

            // In add mode, loadInitialData calls currencyPreferenceRepository
            // Make currency repo throw instead
            fakeCurrencyRepo = FakeCurrencyPreferenceRepository("VND", shouldThrow = true)

            val viewModel =
                AddEditGoldHoldingViewModel(
                    fakeGoldRepo,
                    fakeCurrencyRepo,
                    fakeTimeProvider,
                    SavedStateHandle(mapOf("holdingId" to 0L)),
                )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.errorMessage)
        }

    // --- Helpers ---

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

    private class FakeGoldRepository : GoldRepository {
        val holdingsFlow = MutableStateFlow<List<GoldHolding>>(emptyList())
        val pricesFlow = MutableStateFlow<List<GoldPrice>>(emptyList())

        val addedHoldings = mutableListOf<GoldHolding>()
        val updatedHoldings = mutableListOf<GoldHolding>()
        val deletedIds = mutableListOf<Long>()
        val upsertedPrices = mutableListOf<GoldPrice>()

        var addShouldThrow = false
        var getHoldingShouldThrow = false

        override fun observeAllHoldings(): Flow<List<GoldHolding>> = holdingsFlow

        override suspend fun getHolding(id: Long): GoldHolding? {
            if (getHoldingShouldThrow) throw RuntimeException("Fake getHolding error")
            return holdingsFlow.value.firstOrNull { it.id == id }
        }

        override suspend fun addHolding(holding: GoldHolding): Long {
            if (addShouldThrow) throw RuntimeException("Fake addHolding error")
            addedHoldings.add(holding)
            return holding.id
        }

        override suspend fun updateHolding(holding: GoldHolding) {
            updatedHoldings.add(holding)
        }

        override suspend fun deleteHolding(id: Long) {
            deletedIds.add(id)
        }

        override fun observeAllPrices(): Flow<List<GoldPrice>> = pricesFlow

        override suspend fun getPrice(
            type: GoldType,
            unit: GoldWeightUnit,
        ): GoldPrice? = pricesFlow.value.firstOrNull { it.type == type && it.unit == unit }

        override suspend fun upsertPrice(price: GoldPrice) {
            upsertedPrices.add(price)
        }

        override suspend fun upsertPrices(prices: List<GoldPrice>) {
            upsertedPrices.addAll(prices)
        }
    }
}
