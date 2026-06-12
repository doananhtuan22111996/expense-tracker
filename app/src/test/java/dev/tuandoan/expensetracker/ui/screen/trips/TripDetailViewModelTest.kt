package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.lifecycle.SavedStateHandle
import dev.tuandoan.expensetracker.data.database.entity.DailyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TripCategorySumRow
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryWithCount
import dev.tuandoan.expensetracker.domain.model.DeleteTripBehavior
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.model.TripFilter
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.TripRepository
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TripDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fixedDate = LocalDate.of(2026, 5, 19)
    private val clock = Clock.fixed(fixedDate.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private val baseTrip =
        Trip(
            id = 1L,
            name = "Tokyo",
            destination = "Japan",
            startDateEpochDay = fixedDate.minusDays(3).toEpochDay(),
            endDateEpochDay = fixedDate.plusDays(4).toEpochDay(),
            foreignCurrencyCode = null,
            foreignToHomeRate = null,
            originalCategoryId = null,
            originalCategoryNameSnapshot = null,
            originalCategoryIconSnapshot = null,
            originalCategoryColorSnapshot = null,
            createdAt = 1_000_000L,
        )

    private val conversionTrip =
        baseTrip.copy(
            id = 2L,
            originalCategoryId = 99L,
            originalCategoryNameSnapshot = "Shopping",
            originalCategoryIconSnapshot = null,
            originalCategoryColorSnapshot = null,
        )

    private fun newVm(
        trip: Trip,
        repo: FakeDetailTripRepository = FakeDetailTripRepository(trip),
    ): TripDetailViewModel =
        TripDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("tripId" to trip.id)),
            tripRepository = repo,
            categoryRepository = FakeDetailCategoryRepository(),
            currencyPreferenceRepository = FakeDetailCurrencyRepository(),
            currencyFormatter = FakeDetailCurrencyFormatter(),
            clock = clock,
        )

    // --- requestDelete ---

    @Test
    fun requestDelete_setsPendingActionToUntagDelete() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm(baseTrip)
            advanceUntilIdle()

            vm.requestDelete()

            assertEquals(PendingTripAction.UNTAG_DELETE, vm.uiState.value.pendingAction)
        }

    // --- requestRevert ---

    @Test
    fun requestRevert_setsPendingActionToRevert() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm(conversionTrip)
            advanceUntilIdle()

            vm.requestRevert()

            assertEquals(PendingTripAction.REVERT, vm.uiState.value.pendingAction)
        }

    // --- dismissAction ---

    @Test
    fun dismissAction_clearsPendingAction() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm(baseTrip)
            advanceUntilIdle()
            vm.requestDelete()

            vm.dismissAction()

            assertNull(vm.uiState.value.pendingAction)
        }

    // --- confirmAction: UNTAG_DELETE ---

    @Test
    fun confirmAction_untagDelete_callsDeleteWithUntagBehavior() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = FakeDetailTripRepository(baseTrip)
            val vm = newVm(baseTrip, repo)
            advanceUntilIdle()
            vm.requestDelete()

            vm.confirmAction()
            advanceUntilIdle()

            assertEquals(baseTrip.id, repo.lastDeletedId)
            assertEquals(DeleteTripBehavior.UNTAG, repo.lastDeletedBehavior)
        }

    @Test
    fun confirmAction_untagDelete_clearsPendingActionImmediately() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm(baseTrip)
            advanceUntilIdle()
            vm.requestDelete()

            vm.confirmAction()

            assertNull(vm.uiState.value.pendingAction)
        }

    // --- confirmAction: REVERT ---

    @Test
    fun confirmAction_revert_callsDeleteWithRevertBehavior() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = FakeDetailTripRepository(conversionTrip)
            val vm = newVm(conversionTrip, repo)
            advanceUntilIdle()
            vm.requestRevert()

            vm.confirmAction()
            advanceUntilIdle()

            assertEquals(conversionTrip.id, repo.lastDeletedId)
            assertEquals(DeleteTripBehavior.REVERT_TO_ORIGINAL_CATEGORY, repo.lastDeletedBehavior)
        }

    // --- error handling ---

    @Test
    fun confirmAction_repositoryThrows_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = FakeDetailTripRepository(baseTrip, deleteThrows = true)
            val vm = newVm(baseTrip, repo)
            advanceUntilIdle()
            vm.requestDelete()

            vm.confirmAction()
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.errorMessage)
        }

    @Test
    fun confirmAction_noOpWhenNoPendingAction() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = FakeDetailTripRepository(baseTrip)
            val vm = newVm(baseTrip, repo)
            advanceUntilIdle()

            vm.confirmAction()
            advanceUntilIdle()

            assertNull(repo.lastDeletedId)
        }
}

// --- Local fakes (narrow scope — only what TripDetailViewModel needs) ---

private class FakeDetailTripRepository(
    private val trip: Trip,
    private val deleteThrows: Boolean = false,
) : TripRepository {
    private val tripFlow = MutableStateFlow<Trip?>(trip)

    var lastDeletedId: Long? = null
    var lastDeletedBehavior: DeleteTripBehavior? = null

    override fun observeTripById(id: Long): Flow<Trip?> = tripFlow

    override suspend fun deleteTrip(
        id: Long,
        behavior: DeleteTripBehavior,
    ) {
        if (deleteThrows) throw RuntimeException("forced delete failure")
        lastDeletedId = id
        lastDeletedBehavior = behavior
        tripFlow.value = null
    }

    override fun observeTrips(filter: TripFilter): Flow<List<Trip>> = error("unused")

    override suspend fun getTripById(id: Long): Trip? = trip

    override suspend fun createTrip(
        name: String,
        destination: String?,
        startDateEpochDay: Long,
        endDateEpochDay: Long,
        foreignCurrencyCode: String?,
        foreignToHomeRate: Double?,
    ): Long = error("unused")

    override suspend fun updateTrip(trip: Trip) = error("unused")

    override fun observeTripTotal(tripId: Long): Flow<Long?> = MutableStateFlow(null)

    override fun observeTripTransactionCount(tripId: Long): Flow<Int> = MutableStateFlow(0)

    override fun observeTripDailyTotals(tripId: Long): Flow<List<DailyTotalRow>> = MutableStateFlow(emptyList())

    override fun observeTripCategoryBreakdown(tripId: Long): Flow<List<TripCategorySumRow>> =
        MutableStateFlow(emptyList())

    override fun observeTripTransactions(tripId: Long): Flow<List<Transaction>> = MutableStateFlow(emptyList())

    override fun observeHasForeignTransactions(tripId: Long): Flow<Boolean> = MutableStateFlow(false)
}

private class FakeDetailCategoryRepository : CategoryRepository {
    override fun observeCategories(type: TransactionType): Flow<List<Category>> = MutableStateFlow(emptyList())

    override fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>> = error("unused")

    override suspend fun getCategory(id: Long): Category? = null

    override suspend fun createCategory(
        name: String,
        type: TransactionType,
        iconKey: String?,
        colorKey: String?,
    ) = error("unused")

    override suspend fun updateCategory(
        id: Long,
        name: String,
        iconKey: String?,
        colorKey: String?,
    ) = error("unused")

    override suspend fun deleteCategory(id: Long) = error("unused")
}

private class FakeDetailCurrencyRepository : CurrencyPreferenceRepository {
    override fun observeDefaultCurrency(): Flow<String> = MutableStateFlow("VND")

    override suspend fun getDefaultCurrency(): String = "VND"

    override suspend fun setDefaultCurrency(code: String) = Unit
}

private class FakeDetailCurrencyFormatter : dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter {
    override fun format(
        amountMinor: Long,
        currencyCode: String,
    ): String = "$amountMinor $currencyCode"

    override fun formatWithSign(
        amountMinor: Long,
        currencyCode: String,
        isIncome: Boolean,
    ): String = "$amountMinor $currencyCode"

    override fun formatBareAmount(
        amountMinor: Long,
        currencyCode: String,
    ): String = "$amountMinor"
}
