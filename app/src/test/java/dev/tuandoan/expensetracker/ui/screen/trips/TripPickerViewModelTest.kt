package dev.tuandoan.expensetracker.ui.screen.trips

import dev.tuandoan.expensetracker.data.database.entity.DailyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TripCategorySumRow
import dev.tuandoan.expensetracker.domain.model.DeleteTripBehavior
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.model.TripFilter
import dev.tuandoan.expensetracker.domain.repository.TripRepository
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TripPickerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: PickerFakeTripRepository
    private lateinit var clock: Clock

    private val today: Long = LocalDate.of(2026, 6, 5).toEpochDay()

    @Before
    fun setup() {
        repo = PickerFakeTripRepository()
        clock =
            Clock.fixed(
                Instant.ofEpochSecond(today * 86400),
                ZoneOffset.UTC,
            )
    }

    private fun newVm() = TripPickerViewModel(repo, clock)

    // --- initial state ---

    @Test
    fun initialState_isLoading() {
        val vm = newVm()
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun initialState_isPastCollapsed() {
        val vm = newVm()
        assertFalse(vm.uiState.value.isPastExpanded)
    }

    // --- section bucketing ---

    @Test
    fun activeTrips_populateActiveSection() =
        runTest {
            val trip = trip(id = 1L, name = "Tokyo")
            repo.activeFlow.value = listOf(trip)

            val vm = newVm()
            advanceUntilIdle()

            assertEquals(listOf(trip), vm.uiState.value.active)
            assertTrue(
                vm.uiState.value.upcoming
                    .isEmpty(),
            )
            assertTrue(
                vm.uiState.value.past
                    .isEmpty(),
            )
        }

    @Test
    fun upcomingTrips_populateUpcomingSection() =
        runTest {
            val trip = trip(id = 2L, name = "Paris")
            repo.upcomingFlow.value = listOf(trip)

            val vm = newVm()
            advanceUntilIdle()

            assertEquals(listOf(trip), vm.uiState.value.upcoming)
            assertTrue(
                vm.uiState.value.active
                    .isEmpty(),
            )
            assertTrue(
                vm.uiState.value.past
                    .isEmpty(),
            )
        }

    @Test
    fun pastTrips_populatePastSection() =
        runTest {
            val trip = trip(id = 3L, name = "Seoul")
            repo.pastFlow.value = listOf(trip)

            val vm = newVm()
            advanceUntilIdle()

            assertEquals(listOf(trip), vm.uiState.value.past)
            assertTrue(
                vm.uiState.value.active
                    .isEmpty(),
            )
            assertTrue(
                vm.uiState.value.upcoming
                    .isEmpty(),
            )
        }

    @Test
    fun allSectionsPopulated_eachBucketedCorrectly() =
        runTest {
            val active = trip(id = 1L, name = "Active")
            val upcoming = trip(id = 2L, name = "Upcoming")
            val past = trip(id = 3L, name = "Past")
            repo.activeFlow.value = listOf(active)
            repo.upcomingFlow.value = listOf(upcoming)
            repo.pastFlow.value = listOf(past)

            val vm = newVm()
            advanceUntilIdle()

            assertEquals(listOf(active), vm.uiState.value.active)
            assertEquals(listOf(upcoming), vm.uiState.value.upcoming)
            assertEquals(listOf(past), vm.uiState.value.past)
        }

    @Test
    fun isLoading_falseAfterFirstEmission() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoading)
        }

    // --- reactive updates ---

    @Test
    fun activeFlow_update_reflectedInState() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()

            val trip = trip(id = 10L, name = "Singapore")
            repo.activeFlow.value = listOf(trip)
            advanceUntilIdle()

            assertEquals(listOf(trip), vm.uiState.value.active)
        }

    // --- past expand/collapse toggle ---

    @Test
    fun togglePastExpanded_trueAfterFirstToggle() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()

            vm.togglePastExpanded()

            assertTrue(vm.uiState.value.isPastExpanded)
        }

    @Test
    fun togglePastExpanded_falseAfterSecondToggle() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()

            vm.togglePastExpanded()
            vm.togglePastExpanded()

            assertFalse(vm.uiState.value.isPastExpanded)
        }

    // --- error resilience ---

    @Test
    fun repositoryError_isLoadingFalseAndSectionsEmpty() =
        runTest {
            repo.shouldFail = true
            val vm = newVm()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoading)
            assertTrue(
                vm.uiState.value.active
                    .isEmpty(),
            )
            assertTrue(
                vm.uiState.value.upcoming
                    .isEmpty(),
            )
            assertTrue(
                vm.uiState.value.past
                    .isEmpty(),
            )
        }

    private fun trip(
        id: Long,
        name: String,
    ) = Trip(
        id = id,
        name = name,
        destination = null,
        startDateEpochDay = today,
        endDateEpochDay = today + 5,
        foreignCurrencyCode = null,
        foreignToHomeRate = null,
        originalCategoryId = null,
        originalCategoryNameSnapshot = null,
        originalCategoryIconSnapshot = null,
        originalCategoryColorSnapshot = null,
        createdAt = 0L,
    )
}

private class PickerFakeTripRepository : TripRepository {
    val activeFlow = MutableStateFlow<List<Trip>>(emptyList())
    val upcomingFlow = MutableStateFlow<List<Trip>>(emptyList())
    val pastFlow = MutableStateFlow<List<Trip>>(emptyList())

    var shouldFail = false

    override fun observeTrips(filter: TripFilter): Flow<List<Trip>> {
        if (shouldFail) return flow { throw IllegalStateException("forced failure") }
        return when (filter) {
            is TripFilter.Active -> activeFlow
            is TripFilter.Upcoming -> upcomingFlow
            is TripFilter.Past -> pastFlow
            TripFilter.All -> error("All not used by TripPickerViewModel")
        }
    }

    override fun observeTripById(id: Long): Flow<Trip?> = error("not used")

    override suspend fun getTripById(id: Long): Trip? = error("not used")

    override suspend fun createTrip(
        name: String,
        destination: String?,
        startDateEpochDay: Long,
        endDateEpochDay: Long,
        foreignCurrencyCode: String?,
        foreignToHomeRate: Double?,
    ): Long = error("not used")

    override suspend fun updateTrip(trip: Trip) = error("not used")

    override suspend fun deleteTrip(
        id: Long,
        behavior: DeleteTripBehavior,
    ) = error("not used")

    override fun observeTripTotal(tripId: Long): Flow<Long?> = error("not used")

    override fun observeTripTransactionCount(tripId: Long): Flow<Int> = error("not used")

    override fun observeTripDailyTotals(tripId: Long): Flow<List<DailyTotalRow>> = error("not used")

    override fun observeTripCategoryBreakdown(tripId: Long): Flow<List<TripCategorySumRow>> = error("not used")

    override fun observeTripTransactions(tripId: Long): Flow<List<Transaction>> = error("not used")
}
