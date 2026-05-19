package dev.tuandoan.expensetracker.ui.screen.trips

import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.database.entity.DailyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TripCategorySumRow
import dev.tuandoan.expensetracker.domain.model.DeleteTripBehavior
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
class TripsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: FakeTripRepository
    private lateinit var clock: Clock

    // 2026-05-19 chosen to match the absolute date this test was first authored —
    // any LocalDate.now(clock).toEpochDay() must equal `today`.
    private val today: Long = LocalDate.of(2026, 5, 19).toEpochDay()

    @Before
    fun setup() {
        repo = FakeTripRepository()
        clock =
            Clock.fixed(
                LocalDate.of(2026, 5, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC,
            )
    }

    @Test
    fun init_emitsLoadingThenSectionedTrips() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activeTrip = trip(id = 1L, name = "Da Nang")
            val upcomingA = trip(id = 2L, name = "Tokyo")
            val upcomingB = trip(id = 3L, name = "Bali")
            repo.activeFlow.value = listOf(activeTrip)
            repo.upcomingFlow.value = listOf(upcomingA, upcomingB)
            repo.pastFlow.value = emptyList()

            val vm = TripsViewModel(repo, clock)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(listOf(activeTrip), state.active)
            assertEquals(listOf(upcomingA, upcomingB), state.upcoming)
            assertTrue(state.past.isEmpty())
            assertFalse(state.isEmpty)
            assertNull(state.errorMessage)
        }

    @Test
    fun init_emptyRepo_resultsInIsEmptyTrue() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = TripsViewModel(repo, clock)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isEmpty)
            assertNull(state.errorMessage)
        }

    @Test
    fun init_repoThrows_setsErrorMessageAndStopsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {
            repo.failingFilter = TripFilter::Active

            val vm = TripsViewModel(repo, clock)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage is UiText)
        }

    @Test
    fun init_passesSameNowEpochDayToAllThreeFilters() =
        runTest(mainDispatcherRule.testDispatcher) {
            TripsViewModel(repo, clock)
            advanceUntilIdle()

            val active = repo.observedFilters.filterIsInstance<TripFilter.Active>().single()
            val upcoming = repo.observedFilters.filterIsInstance<TripFilter.Upcoming>().single()
            val past = repo.observedFilters.filterIsInstance<TripFilter.Past>().single()

            assertEquals(today, active.nowEpochDay)
            assertEquals(today, upcoming.nowEpochDay)
            assertEquals(today, past.nowEpochDay)
        }

    @Test
    fun clearError_resetsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            repo.failingFilter = TripFilter::Active
            val vm = TripsViewModel(repo, clock)
            advanceUntilIdle()
            assertNotNull(vm.uiState.value.errorMessage)

            vm.clearError()

            assertNull(vm.uiState.value.errorMessage)
        }

    private fun trip(
        id: Long,
        name: String,
    ): Trip =
        Trip(
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

private class FakeTripRepository : TripRepository {
    val activeFlow = MutableStateFlow<List<Trip>>(emptyList())
    val upcomingFlow = MutableStateFlow<List<Trip>>(emptyList())
    val pastFlow = MutableStateFlow<List<Trip>>(emptyList())
    val observedFilters = mutableListOf<TripFilter>()

    /**
     * When set, `observeTrips` returns a flow that throws as soon as a filter of the
     * matching variant is requested. Used to drive the error path in the VM.
     */
    var failingFilter: ((Long) -> TripFilter)? = null

    override fun observeTrips(filter: TripFilter): Flow<List<Trip>> {
        observedFilters.add(filter)
        val expected = failingFilter?.invoke(0L)
        if (expected != null && expected::class == filter::class) {
            return flow { throw IllegalStateException("forced failure for $filter") }
        }
        return when (filter) {
            is TripFilter.Active -> activeFlow
            is TripFilter.Upcoming -> upcomingFlow
            is TripFilter.Past -> pastFlow
            TripFilter.All -> error("All filter not used by TripsViewModel")
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
}
