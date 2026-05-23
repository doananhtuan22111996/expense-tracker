package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.lifecycle.SavedStateHandle
import dev.tuandoan.expensetracker.data.database.entity.DailyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TripCategorySumRow
import dev.tuandoan.expensetracker.domain.model.DeleteTripBehavior
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.model.TripFilter
import dev.tuandoan.expensetracker.domain.repository.TripRepository
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
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
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class CreateEditTripViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: FakeCreateEditTripRepository
    private lateinit var currencyRepo: FakeCurrencyPreferenceRepository
    private lateinit var clock: Clock

    private val today: Long = LocalDate.of(2026, 5, 19).toEpochDay()

    @Before
    fun setup() {
        repo = FakeCreateEditTripRepository()
        currencyRepo = FakeCurrencyPreferenceRepository(initialCurrency = "VND")
        clock =
            Clock.fixed(
                LocalDate.of(2026, 5, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC,
            )
    }

    private fun newVm(tripId: Long = 0L): CreateEditTripViewModel =
        CreateEditTripViewModel(
            savedStateHandle = SavedStateHandle(mapOf("tripId" to tripId)),
            tripRepository = repo,
            currencyPreferenceRepository = currencyRepo,
            clock = clock,
        )

    @Test
    fun addMode_loadsDefaults() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(vm.isEditMode)
            assertFalse(state.isLoading)
            assertEquals("VND", state.homeCurrencyCode)
            assertEquals(today, state.startEpochDay)
            assertNull(state.endEpochDay)
            assertFalse(state.isForeignCurrency)
            assertFalse(state.isValid) // missing name + end date
        }

    @Test
    fun addMode_fullValidInput_isValidAndSaveCreatesTrip() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onNameChange("Tokyo")
            vm.onDestinationChange("Tokyo, Japan")
            vm.onDatesSelected(today, today + 5)

            assertTrue(vm.uiState.value.isValid)

            var success = false
            vm.save { success = true }
            advanceUntilIdle()

            assertTrue(success)
            val created = repo.createdTrips.single()
            assertEquals("Tokyo", created.name)
            assertEquals("Tokyo, Japan", created.destination)
            assertEquals(today, created.startEpochDay)
            assertEquals(today + 5, created.endEpochDay)
            assertNull(created.foreignCurrencyCode)
            assertNull(created.foreignToHomeRate)
        }

    @Test
    fun addMode_blankDestination_persistedAsNull() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onNameChange("Tokyo")
            vm.onDatesSelected(today, today + 5)

            vm.save { }
            advanceUntilIdle()

            assertNull(repo.createdTrips.single().destination)
        }

    @Test
    fun nameTooLong_setsErrorAndDisablesSave() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onNameChange("x".repeat(61))
            vm.onDatesSelected(today, today + 1)

            assertNotNull(vm.uiState.value.nameError)
            assertFalse(vm.uiState.value.isValid)
        }

    @Test
    fun endBeforeStart_setsDateErrorAndDisablesSave() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onNameChange("Tokyo")
            vm.onDatesSelected(today + 5, today)

            assertNotNull(vm.uiState.value.dateError)
            assertFalse(vm.uiState.value.isValid)
        }

    @Test
    fun foreignToggleOn_emptyRate_disablesSaveButHasNoRateError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onNameChange("Tokyo")
            vm.onDatesSelected(today, today + 1)
            vm.onToggleForeignCurrency(true)
            vm.onForeignCurrencyChange("JPY")

            // Empty rate is "not yet typed", not an error.
            assertNull(vm.uiState.value.rateError)
            assertFalse(vm.uiState.value.isValid)
        }

    @Test
    fun foreignToggleOn_codeEqualsHome_setsRateError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onToggleForeignCurrency(true)
            vm.onForeignCurrencyChange("VND") // same as home

            assertNotNull(vm.uiState.value.rateError)
        }

    @Test
    fun foreignToggleOn_validRate_isValidAndSavesFxFields() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onNameChange("Tokyo")
            vm.onDatesSelected(today, today + 1)
            vm.onToggleForeignCurrency(true)
            vm.onForeignCurrencyChange("JPY")
            vm.onRateTextChange("165.5")

            assertTrue(vm.uiState.value.isValid)
            vm.save { }
            advanceUntilIdle()

            val created = repo.createdTrips.single()
            assertEquals("JPY", created.foreignCurrencyCode)
            assertEquals(165.5, created.foreignToHomeRate!!, 0.0001)
        }

    @Test
    fun foreignToggleOff_clearsCodeAndRateOnSave() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onNameChange("Tokyo")
            vm.onDatesSelected(today, today + 1)
            vm.onToggleForeignCurrency(true)
            vm.onForeignCurrencyChange("JPY")
            vm.onRateTextChange("165")
            vm.onToggleForeignCurrency(false)

            vm.save { }
            advanceUntilIdle()

            val created = repo.createdTrips.single()
            assertNull(created.foreignCurrencyCode)
            assertNull(created.foreignToHomeRate)
        }

    @Test
    fun rateText_acceptsCommaDecimal() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onToggleForeignCurrency(true)
            vm.onForeignCurrencyChange("JPY")
            vm.onRateTextChange("165,5")

            assertEquals("165.5", vm.uiState.value.rateText)
        }

    @Test
    fun editMode_loadsExistingTripAndSeedsAllFields() =
        runTest(mainDispatcherRule.testDispatcher) {
            val existing =
                Trip(
                    id = 7L,
                    name = "Tokyo",
                    destination = "Japan",
                    startDateEpochDay = today,
                    endDateEpochDay = today + 4,
                    foreignCurrencyCode = "JPY",
                    foreignToHomeRate = 165.0,
                    originalCategoryId = null,
                    originalCategoryNameSnapshot = null,
                    originalCategoryIconSnapshot = null,
                    originalCategoryColorSnapshot = null,
                    createdAt = 0L,
                )
            repo.tripsById[7L] = existing

            val vm = newVm(tripId = 7L)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(vm.isEditMode)
            assertFalse(state.isLoading)
            assertEquals("Tokyo", state.name)
            assertEquals("Japan", state.destination)
            assertEquals(today, state.startEpochDay)
            assertEquals(today + 4, state.endEpochDay)
            assertTrue(state.isForeignCurrency)
            assertEquals("JPY", state.foreignCurrencyCode)
            // 165.0 should round-trip as "165" — no trailing zero or scientific notation.
            assertEquals("165", state.rateText)
            assertFalse(state.isConversionOrigin)
        }

    @Test
    fun editMode_tripNotFound_setsErrorAndStopsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm(tripId = 999L)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.errorMessage)
        }

    @Test
    fun editMode_savePreservesSnapshotFields() =
        runTest(mainDispatcherRule.testDispatcher) {
            val existing =
                Trip(
                    id = 7L,
                    name = "Old name",
                    destination = "Old dest",
                    startDateEpochDay = today,
                    endDateEpochDay = today + 4,
                    foreignCurrencyCode = null,
                    foreignToHomeRate = null,
                    originalCategoryId = 42L,
                    originalCategoryNameSnapshot = "Da Nang",
                    originalCategoryIconSnapshot = "icon-key",
                    originalCategoryColorSnapshot = "#FF00FF",
                    createdAt = 1234L,
                )
            repo.tripsById[7L] = existing

            val vm = newVm(tripId = 7L)
            advanceUntilIdle()
            vm.onNameChange("New name")
            vm.save { }
            advanceUntilIdle()

            val updated = repo.updatedTrips.single()
            assertEquals("New name", updated.name)
            // Snapshot fields are pass-through-only.
            assertEquals(42L, updated.originalCategoryId)
            assertEquals("Da Nang", updated.originalCategoryNameSnapshot)
            assertEquals("icon-key", updated.originalCategoryIconSnapshot)
            assertEquals("#FF00FF", updated.originalCategoryColorSnapshot)
            assertEquals(1234L, updated.createdAt)
        }

    @Test
    fun save_repoThrows_surfacesErrorAndKeepsFormState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onNameChange("Tokyo")
            vm.onDatesSelected(today, today + 1)
            repo.failOnCreate = true

            var success = false
            vm.save { success = true }
            advanceUntilIdle()

            assertFalse(success)
            val state = vm.uiState.value
            assertFalse(state.isSaving)
            assertNotNull(state.errorMessage)
            assertEquals("Tokyo", state.name) // form intact for retry
        }

    @Test
    fun hasUnsavedChanges_addMode_anyInputCounts() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            assertFalse(vm.uiState.value.hasUnsavedChanges)

            vm.onNameChange("x")
            assertTrue(vm.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun hasUnsavedChanges_editMode_falseUntilDirty() =
        runTest(mainDispatcherRule.testDispatcher) {
            val existing =
                Trip(
                    id = 7L,
                    name = "Tokyo",
                    destination = null,
                    startDateEpochDay = today,
                    endDateEpochDay = today + 1,
                    foreignCurrencyCode = null,
                    foreignToHomeRate = null,
                    originalCategoryId = null,
                    originalCategoryNameSnapshot = null,
                    originalCategoryIconSnapshot = null,
                    originalCategoryColorSnapshot = null,
                    createdAt = 0L,
                )
            repo.tripsById[7L] = existing

            val vm = newVm(tripId = 7L)
            advanceUntilIdle()
            assertFalse(vm.uiState.value.hasUnsavedChanges)

            vm.onNameChange("Kyoto")
            assertTrue(vm.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun clearError_resetsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm(tripId = 999L)
            advanceUntilIdle()
            assertNotNull(vm.uiState.value.errorMessage)

            vm.clearError()
            assertNull(vm.uiState.value.errorMessage)
        }

    // --- Boundaries + edit-mode safety ---

    @Test
    fun nameLength_at60_isValidAndAt61_isInvalid() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onDatesSelected(today, today + 1)

            vm.onNameChange("x".repeat(60))
            assertNull(vm.uiState.value.nameError)
            assertTrue(vm.uiState.value.isValid)

            vm.onNameChange("x".repeat(61))
            assertNotNull(vm.uiState.value.nameError)
            assertFalse(vm.uiState.value.isValid)
        }

    @Test
    fun rate_atMaxBoundary_isInvalid() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onNameChange("Tokyo")
            vm.onDatesSelected(today, today + 1)
            vm.onToggleForeignCurrency(true)
            vm.onForeignCurrencyChange("JPY")
            vm.onRateTextChange("1000000") // exactly MAX_RATE — must be invalid (> bound)

            assertNotNull(vm.uiState.value.rateError)
            assertFalse(vm.uiState.value.isValid)
        }

    @Test
    fun rate_negativeViaDirectInput_isInvalid() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
            advanceUntilIdle()
            vm.onToggleForeignCurrency(true)
            vm.onForeignCurrencyChange("JPY")
            // Keyboard regex would suppress '-' at the screen layer; this
            // pins the VM-level guard against direct programmatic input.
            vm.onRateTextChange("-5")

            assertNotNull(vm.uiState.value.rateError)
        }

    @Test
    fun editMode_loadFailed_saveIsNoOpAndDoesNotThrow() =
        runTest(mainDispatcherRule.testDispatcher) {
            repo.failOnGetTripById = true

            val vm = newVm(tripId = 7L)
            advanceUntilIdle()
            // Form rendered with errorMessage set; originalTrip is null.
            assertNotNull(vm.uiState.value.errorMessage)
            assertNull(vm.uiState.value.originalTrip)

            // Even with full valid input, isValid must be false in edit mode
            // when originalTrip is null — the guard prevents a write attempt.
            vm.onNameChange("Tokyo")
            vm.onDatesSelected(today, today + 1)
            assertFalse(vm.uiState.value.isValid)

            var success = false
            vm.save { success = true }
            advanceUntilIdle()

            assertFalse(success)
            assertTrue(repo.updatedTrips.isEmpty())
            assertTrue(repo.createdTrips.isEmpty())
        }
}

private class FakeCreateEditTripRepository : TripRepository {
    val tripsById = mutableMapOf<Long, Trip>()
    val createdTrips = mutableListOf<CreatedTrip>()
    val updatedTrips = mutableListOf<Trip>()
    var failOnCreate = false
    var failOnGetTripById = false

    data class CreatedTrip(
        val name: String,
        val destination: String?,
        val startEpochDay: Long,
        val endEpochDay: Long,
        val foreignCurrencyCode: String?,
        val foreignToHomeRate: Double?,
    )

    override fun observeTrips(filter: TripFilter): Flow<List<Trip>> = MutableStateFlow(emptyList())

    override fun observeTripById(id: Long): Flow<Trip?> = MutableStateFlow(tripsById[id])

    override suspend fun getTripById(id: Long): Trip? {
        if (failOnGetTripById) throw IllegalStateException("forced getTripById failure")
        return tripsById[id]
    }

    override suspend fun createTrip(
        name: String,
        destination: String?,
        startDateEpochDay: Long,
        endDateEpochDay: Long,
        foreignCurrencyCode: String?,
        foreignToHomeRate: Double?,
    ): Long {
        if (failOnCreate) throw IllegalStateException("forced create failure")
        createdTrips.add(
            CreatedTrip(
                name = name,
                destination = destination,
                startEpochDay = startDateEpochDay,
                endEpochDay = endDateEpochDay,
                foreignCurrencyCode = foreignCurrencyCode,
                foreignToHomeRate = foreignToHomeRate,
            ),
        )
        return createdTrips.size.toLong()
    }

    override suspend fun updateTrip(trip: Trip) {
        updatedTrips.add(trip)
        tripsById[trip.id] = trip
    }

    override suspend fun deleteTrip(
        id: Long,
        behavior: DeleteTripBehavior,
    ) = error("not used")

    override fun observeTripTotal(tripId: Long): Flow<Long?> = MutableStateFlow(null)

    override fun observeTripTransactionCount(tripId: Long): Flow<Int> = MutableStateFlow(0)

    override fun observeTripDailyTotals(tripId: Long): Flow<List<DailyTotalRow>> = MutableStateFlow(emptyList())

    override fun observeTripCategoryBreakdown(tripId: Long): Flow<List<TripCategorySumRow>> =
        MutableStateFlow(emptyList())
}
