package dev.tuandoan.expensetracker.ui.screen.trips

import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.database.entity.DailyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TripCategorySumRow
import dev.tuandoan.expensetracker.domain.model.DeleteTripBehavior
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.model.TripFilter
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
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
    private lateinit var currencyRepo: FakeCurrencyPreferenceRepository
    private lateinit var formatter: FakeCurrencyFormatter
    private lateinit var clock: Clock

    private val today: Long = LocalDate.of(2026, 5, 19).toEpochDay()

    @Before
    fun setup() {
        repo = FakeTripRepository()
        currencyRepo = FakeCurrencyPreferenceRepository(initial = "VND")
        formatter = FakeCurrencyFormatter()
        clock =
            Clock.fixed(
                LocalDate.of(2026, 5, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC,
            )
    }

    @Test
    fun init_emitsLoadingThenSectionedTripsWithAggregates() =
        runTest(mainDispatcherRule.testDispatcher) {
            val active = trip(id = 1L, name = "Da Nang")
            val upcomingA = trip(id = 2L, name = "Tokyo")
            val upcomingB = trip(id = 3L, name = "Bali")
            repo.activeFlow.value = listOf(active)
            repo.upcomingFlow.value = listOf(upcomingA, upcomingB)
            repo.pastFlow.value = emptyList()
            repo.totalsByTrip[1L] = 250_000L
            repo.countsByTrip[1L] = 4
            repo.totalsByTrip[2L] = null
            repo.countsByTrip[2L] = 0
            repo.totalsByTrip[3L] = 1_200_000L
            repo.countsByTrip[3L] = 17

            val vm = newVm()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(1, state.active.size)
            assertEquals("250000 VND", state.active.first().totalLabel)
            assertEquals(4, state.active.first().transactionCount)
            assertNull(state.upcoming.first().totalLabel)
            assertEquals(0, state.upcoming.first().transactionCount)
            assertEquals("1200000 VND", state.upcoming[1].totalLabel)
            assertEquals(17, state.upcoming[1].transactionCount)
            assertTrue(state.past.isEmpty())
            assertFalse(state.isEmpty)
            assertNull(state.errorMessage)
        }

    @Test
    fun init_emptyRepo_resultsInIsEmptyTrueWithoutHangingOnInnerCombine() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm()
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

            val vm = newVm()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.errorMessage)
            assertTrue(state.errorMessage is UiText)
        }

    @Test
    fun init_passesSameNowEpochDayToAllThreeFilters() =
        runTest(mainDispatcherRule.testDispatcher) {
            newVm()
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
            val vm = newVm()
            advanceUntilIdle()
            assertNotNull(vm.uiState.value.errorMessage)

            vm.clearError()

            assertNull(vm.uiState.value.errorMessage)
        }

    @Test
    fun currencyChange_reformatsAllTotals() =
        runTest(mainDispatcherRule.testDispatcher) {
            repo.activeFlow.value = listOf(trip(id = 1L, name = "Da Nang"))
            repo.totalsByTrip[1L] = 100_000L
            repo.countsByTrip[1L] = 2

            val vm = newVm()
            advanceUntilIdle()
            assertEquals(
                "100000 VND",
                vm.uiState.value.active
                    .first()
                    .totalLabel,
            )

            currencyRepo.currencyFlow.value = "USD"
            advanceUntilIdle()

            assertEquals(
                "100000 USD",
                vm.uiState.value.active
                    .first()
                    .totalLabel,
            )
        }

    @Test
    fun nullTotal_yieldsNullLabelAndZeroCountFallback() =
        runTest(mainDispatcherRule.testDispatcher) {
            repo.activeFlow.value = listOf(trip(id = 9L, name = "Empty trip"))
            // Intentionally leave totals/counts unseeded → flows emit null/0.

            val vm = newVm()
            advanceUntilIdle()

            val card =
                vm.uiState.value.active
                    .single()
            assertNull(card.totalLabel)
            assertEquals(0, card.transactionCount)
        }

    @Test
    fun sectionChange_midStream_addsCardWithAggregates() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Start empty so the inner combine takes the short-circuit branch.
            val vm = newVm()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.isEmpty)

            // Seed aggregates BEFORE the trip appears — the new card must
            // pick them up when sections re-emit and the enrichment branch
            // re-attaches.
            repo.totalsByTrip[5L] = 750_000L
            repo.countsByTrip[5L] = 3
            repo.activeFlow.value = listOf(trip(id = 5L, name = "Hue"))
            advanceUntilIdle()

            val card =
                vm.uiState.value.active
                    .single()
            assertEquals("Hue", card.trip.name)
            assertEquals("750000 VND", card.totalLabel)
            assertEquals(3, card.transactionCount)
        }

    @Test
    fun tripTotalUpdate_withoutTripChange_reformatsRowLabel() =
        runTest(mainDispatcherRule.testDispatcher) {
            repo.activeFlow.value = listOf(trip(id = 1L, name = "Da Nang"))
            repo.setTripTotal(1L, 100_000L)
            repo.setTripTransactionCount(1L, 2)

            val vm = newVm()
            advanceUntilIdle()
            assertEquals(
                "100000 VND",
                vm.uiState.value.active
                    .first()
                    .totalLabel,
            )

            // Same Trip object — only the per-trip total changes (e.g. user
            // adds a transaction). Pins that the per-trip combine bubbles
            // updates without depending on a Trip re-emission.
            repo.setTripTotal(1L, 250_000L)
            advanceUntilIdle()

            val card =
                vm.uiState.value.active
                    .first()
            assertEquals("250000 VND", card.totalLabel)
            assertEquals(1L, card.trip.id)
        }

    private fun newVm(): TripsViewModel = TripsViewModel(repo, currencyRepo, formatter, clock)

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

    val totalsByTrip = mutableMapOf<Long, Long?>()
    val countsByTrip = mutableMapOf<Long, Int>()

    // Per-id MutableStateFlow caches so post-init mutations bubble to active
    // collectors (used by tests that change a value AFTER newVm()).
    private val totalFlows = mutableMapOf<Long, MutableStateFlow<Long?>>()
    private val countFlows = mutableMapOf<Long, MutableStateFlow<Int>>()

    fun setTripTotal(
        tripId: Long,
        value: Long?,
    ) {
        totalsByTrip[tripId] = value
        totalFlows.getOrPut(tripId) { MutableStateFlow(value) }.value = value
    }

    fun setTripTransactionCount(
        tripId: Long,
        value: Int,
    ) {
        countsByTrip[tripId] = value
        countFlows.getOrPut(tripId) { MutableStateFlow(value) }.value = value
    }

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

    override fun observeTripTotal(tripId: Long): Flow<Long?> =
        totalFlows.getOrPut(tripId) { MutableStateFlow(totalsByTrip[tripId]) }

    override fun observeTripTransactionCount(tripId: Long): Flow<Int> =
        countFlows.getOrPut(tripId) { MutableStateFlow(countsByTrip[tripId] ?: 0) }

    override fun observeTripDailyTotals(tripId: Long): Flow<List<DailyTotalRow>> = error("not used")

    override fun observeTripCategoryBreakdown(tripId: Long): Flow<List<TripCategorySumRow>> = error("not used")

    override fun observeTripTransactions(tripId: Long): Flow<List<Transaction>> = error("not used")

    override fun observeHasForeignTransactions(tripId: Long): Flow<Boolean> = error("not used")
}

private class FakeCurrencyPreferenceRepository(
    initial: String,
) : CurrencyPreferenceRepository {
    val currencyFlow = MutableStateFlow(initial)

    override fun observeDefaultCurrency(): Flow<String> = currencyFlow

    override suspend fun setDefaultCurrency(currencyCode: String) {
        currencyFlow.value = currencyCode
    }

    override suspend fun getDefaultCurrency(): String = currencyFlow.value
}

private class FakeCurrencyFormatter : CurrencyFormatter {
    override fun format(
        amountMinor: Long,
        currencyCode: String,
    ): String = "$amountMinor $currencyCode"

    override fun formatWithSign(
        amountMinor: Long,
        currencyCode: String,
        isIncome: Boolean,
    ): String = "${if (isIncome) "+" else "-"}$amountMinor $currencyCode"

    override fun formatBareAmount(
        amountMinor: Long,
        currencyCode: String,
    ): String = amountMinor.toString()
}
