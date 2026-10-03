package dev.tuandoan.expensetracker.ui.screen.trips

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.database.entity.DailyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TripCategorySumRow
import dev.tuandoan.expensetracker.data.export.CsvExporter
import dev.tuandoan.expensetracker.domain.model.BudgetStatusLevel
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TripDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fixedDate = LocalDate.of(2026, 5, 19)
    private val clock = Clock.fixed(fixedDate.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val fixedZone = ZoneId.of("UTC")
    private val csvExporter = CsvExporter(fixedZone)
    private val mockContentResolver = Mockito.mock(ContentResolver::class.java)
    private val mockUri = Mockito.mock(Uri::class.java)

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
        contentResolver: ContentResolver = mockContentResolver,
        ioDispatcher: CoroutineDispatcher = mainDispatcherRule.testDispatcher,
    ): TripDetailViewModel =
        TripDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("tripId" to trip.id)),
            tripRepository = repo,
            categoryRepository = FakeDetailCategoryRepository(),
            currencyPreferenceRepository = FakeDetailCurrencyRepository(),
            currencyFormatter = FakeDetailCurrencyFormatter(),
            csvExporter = csvExporter,
            contentResolver = contentResolver,
            ioDispatcher = ioDispatcher,
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

    @Test
    fun observeDetail_activeTrip_computesDaysElapsedAndRemainingCorrectly() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm(baseTrip)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(TripStatus.ACTIVE, state.tripStatus)
            assertEquals(8, state.totalDays)
            assertEquals(4, state.daysElapsed)
            assertEquals(4, state.daysRemaining)
            assertEquals(0, state.daysUntilStart)
        }

    @Test
    fun observeDetail_upcomingTrip_computesDaysCorrectly() =
        runTest(mainDispatcherRule.testDispatcher) {
            val upcomingTrip =
                baseTrip.copy(
                    id = 3L,
                    startDateEpochDay = fixedDate.plusDays(2).toEpochDay(),
                    endDateEpochDay = fixedDate.plusDays(5).toEpochDay(),
                )
            val vm = newVm(upcomingTrip)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(TripStatus.UPCOMING, state.tripStatus)
            assertEquals(4, state.totalDays)
            assertEquals(0, state.daysElapsed)
            assertEquals(4, state.daysRemaining)
            assertEquals(2, state.daysUntilStart)
        }

    @Test
    fun observeDetail_pastTrip_computesDaysCorrectly() =
        runTest(mainDispatcherRule.testDispatcher) {
            val pastTrip =
                baseTrip.copy(
                    id = 4L,
                    startDateEpochDay = fixedDate.minusDays(10).toEpochDay(),
                    endDateEpochDay = fixedDate.minusDays(2).toEpochDay(),
                )
            val vm = newVm(pastTrip)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(TripStatus.PAST, state.tripStatus)
            assertEquals(9, state.totalDays)
            assertEquals(9, state.daysElapsed)
            assertEquals(0, state.daysRemaining)
            assertEquals(0, state.daysUntilStart)
        }

    @Test
    fun onCategoryClick_togglesSelectionAndClears() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm(baseTrip)
            advanceUntilIdle()

            val testCategory =
                Category(
                    id = 42L,
                    name = "Food",
                    type = TransactionType.EXPENSE,
                    colorKey = "blue",
                )

            assertNull(vm.uiState.value.selectedCategoryId)

            vm.onCategoryClick(testCategory)
            assertEquals(42L, vm.uiState.value.selectedCategoryId)

            // Clicking again toggles off
            vm.onCategoryClick(testCategory)
            assertNull(vm.uiState.value.selectedCategoryId)

            // Clicking and explicit clear
            vm.onCategoryClick(testCategory)
            assertEquals(42L, vm.uiState.value.selectedCategoryId)
            vm.clearCategoryFilter()
            assertNull(vm.uiState.value.selectedCategoryId)
        }

    // --- Trip budget and daily allowance (T1.4) ---

    @Test
    fun budgetStatus_nullWhenTripHasNoBudget() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm(baseTrip)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertNull(state.budgetStatus)
            assertNull(state.budgetLabel)
            assertNull(state.budgetRemainingLabel)
            assertNull(state.dailyAllowanceLabel)
        }

    @Test
    fun budgetStatus_calculatedForActiveTrip_withZeroSpend() =
        runTest(mainDispatcherRule.testDispatcher) {
            val budgetedTrip = baseTrip.copy(budgetAmount = 800_000L)
            val vm = newVm(budgetedTrip)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertNotNull(state.budgetStatus)
            assertEquals(800_000L, state.budgetStatus?.budgetAmount)
            assertEquals(0L, state.budgetStatus?.spentAmount)
            assertEquals(800_000L, state.budgetStatus?.remainingAmount)
            assertEquals(160_000L, state.budgetStatus?.dailyAllowance)
            assertEquals(5, state.budgetStatus?.remainingDays)
            assertEquals(BudgetStatusLevel.OK, state.budgetStatus?.status)
            assertEquals("800000 VND", state.budgetLabel)
            assertEquals("800000 VND", state.budgetRemainingLabel)
            assertEquals("160000 VND", state.dailyAllowanceLabel)
        }

    @Test
    fun budgetStatus_reactivelyUpdates_whenTripTotalChanges() =
        runTest(mainDispatcherRule.testDispatcher) {
            val budgetedTrip = baseTrip.copy(budgetAmount = 1_000_000L)
            val repo = FakeDetailTripRepository(budgetedTrip)
            repo.tripTotalFlow.value = 200_000L
            val vm = newVm(budgetedTrip, repo)
            advanceUntilIdle()

            assertEquals(
                200_000L,
                vm.uiState.value.budgetStatus
                    ?.spentAmount,
            )
            assertEquals(
                800_000L,
                vm.uiState.value.budgetStatus
                    ?.remainingAmount,
            )
            assertEquals(
                BudgetStatusLevel.OK,
                vm.uiState.value.budgetStatus
                    ?.status,
            )

            // Emit updated spend
            repo.tripTotalFlow.value = 850_000L
            advanceUntilIdle()

            assertEquals(
                850_000L,
                vm.uiState.value.budgetStatus
                    ?.spentAmount,
            )
            assertEquals(
                150_000L,
                vm.uiState.value.budgetStatus
                    ?.remainingAmount,
            )
            assertEquals(
                BudgetStatusLevel.WARNING,
                vm.uiState.value.budgetStatus
                    ?.status,
            )
        }

    @Test
    fun budgetStatus_warningLevel_whenEightyPercentSpent() =
        runTest(mainDispatcherRule.testDispatcher) {
            val budgetedTrip = baseTrip.copy(budgetAmount = 1_000_000L)
            val repo = FakeDetailTripRepository(budgetedTrip)
            repo.tripTotalFlow.value = 800_000L
            val vm = newVm(budgetedTrip, repo)
            advanceUntilIdle()

            assertEquals(
                BudgetStatusLevel.WARNING,
                vm.uiState.value.budgetStatus
                    ?.status,
            )
        }

    @Test
    fun budgetStatus_overBudget_whenSpentExceedsBudget() =
        runTest(mainDispatcherRule.testDispatcher) {
            val budgetedTrip = baseTrip.copy(budgetAmount = 1_000_000L)
            val repo = FakeDetailTripRepository(budgetedTrip)
            repo.tripTotalFlow.value = 1_200_000L
            val vm = newVm(budgetedTrip, repo)
            advanceUntilIdle()

            val status = vm.uiState.value.budgetStatus
            assertNotNull(status)
            assertEquals(BudgetStatusLevel.OVER_BUDGET, status?.status)
            assertEquals(-200_000L, status?.remainingAmount)
            assertEquals(0L, status?.dailyAllowance)
            assertEquals("200000 VND", vm.uiState.value.budgetRemainingLabel)
        }

    @Test
    fun budgetStatus_upcomingTrip_calculatesPlannedAllowanceAcrossTotalDays() =
        runTest(mainDispatcherRule.testDispatcher) {
            val upcomingTrip =
                baseTrip.copy(
                    id = 3L,
                    startDateEpochDay = fixedDate.plusDays(2).toEpochDay(),
                    endDateEpochDay = fixedDate.plusDays(5).toEpochDay(),
                    budgetAmount = 400_000L,
                )
            val vm = newVm(upcomingTrip)
            advanceUntilIdle()

            val status = vm.uiState.value.budgetStatus
            assertNotNull(status)
            assertEquals(4, status?.remainingDays)
            assertEquals(100_000L, status?.dailyAllowance)
            assertEquals("100000 VND", vm.uiState.value.dailyAllowanceLabel)
        }

    @Test
    fun budgetStatus_pastTrip_nullDailyAllowance() =
        runTest(mainDispatcherRule.testDispatcher) {
            val pastTrip =
                baseTrip.copy(
                    id = 4L,
                    startDateEpochDay = fixedDate.minusDays(10).toEpochDay(),
                    endDateEpochDay = fixedDate.minusDays(2).toEpochDay(),
                    budgetAmount = 500_000L,
                )
            val repo = FakeDetailTripRepository(pastTrip)
            repo.tripTotalFlow.value = 400_000L
            val vm = newVm(pastTrip, repo)
            advanceUntilIdle()

            val status = vm.uiState.value.budgetStatus
            assertNotNull(status)
            assertEquals(0, status?.remainingDays)
            assertNull(status?.dailyAllowance)
            assertNull(vm.uiState.value.dailyAllowanceLabel)
        }

    @Test
    fun exportTripCsv_successfulExport_updatesUserMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val outputStream = ByteArrayOutputStream()
            Mockito
                .`when`(mockContentResolver.openOutputStream(mockUri))
                .thenReturn(outputStream)

            val vm = newVm(baseTrip)
            advanceUntilIdle()

            vm.exportTripCsv(mockUri)
            advanceUntilIdle()

            val msg = vm.uiState.value.userMessage
            assertNotNull(msg)
            assertTrue(msg is UiText.StringResource)
            assertEquals(R.string.trip_csv_exported_successfully, (msg as UiText.StringResource).resId)
            assertTrue(outputStream.size() > 0)
        }

    @Test
    fun exportTripCsv_openStreamFails_updatesErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            Mockito
                .`when`(mockContentResolver.openOutputStream(mockUri))
                .thenReturn(null)

            val vm = newVm(baseTrip)
            advanceUntilIdle()

            vm.exportTripCsv(mockUri)
            advanceUntilIdle()

            val err = vm.uiState.value.errorMessage
            assertNotNull(err)
            assertTrue(err is UiText.StringResource)
            assertEquals(R.string.trip_csv_export_failed, (err as UiText.StringResource).resId)
        }

    @Test
    fun getShareableTripSummary_returnsFormattedSummary() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newVm(baseTrip)
            advanceUntilIdle()

            val summary = vm.getShareableTripSummary()
            assertNotNull(summary)
            assertTrue(summary!!.contains("Trip: Tokyo (Japan)"))
            assertTrue(summary.contains("Total Spent:"))
            assertTrue(summary.contains("Transactions:"))
        }

    @Test
    fun clearUserMessage_resetsUserMessageToNull() =
        runTest(mainDispatcherRule.testDispatcher) {
            val outputStream = ByteArrayOutputStream()
            Mockito
                .`when`(mockContentResolver.openOutputStream(mockUri))
                .thenReturn(outputStream)

            val vm = newVm(baseTrip)
            advanceUntilIdle()

            vm.exportTripCsv(mockUri)
            advanceUntilIdle()
            assertNotNull(vm.uiState.value.userMessage)

            vm.clearUserMessage()
            assertNull(vm.uiState.value.userMessage)
        }
}

// --- Local fakes (narrow scope — only what TripDetailViewModel needs) ---

private class FakeDetailTripRepository(
    private val trip: Trip,
    private val deleteThrows: Boolean = false,
) : TripRepository {
    private val tripFlow = MutableStateFlow<Trip?>(trip)
    val tripTotalFlow = MutableStateFlow<Long?>(null)

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
        budgetAmount: Long?,
    ): Long = error("unused")

    override suspend fun updateTrip(trip: Trip) = error("unused")

    override fun observeTripTotal(tripId: Long): Flow<Long?> = tripTotalFlow

    override fun observeTripTransactionCount(tripId: Long): Flow<Int> = MutableStateFlow(0)

    override fun observeTripDailyTotals(tripId: Long): Flow<List<DailyTotalRow>> = MutableStateFlow(emptyList())

    override fun observeTripCategoryBreakdown(tripId: Long): Flow<List<TripCategorySumRow>> =
        MutableStateFlow(emptyList())

    override fun observeTripTransactions(tripId: Long): Flow<List<Transaction>> = MutableStateFlow(emptyList())

    override fun observeHasForeignTransactions(tripId: Long): Flow<Boolean> = MutableStateFlow(false)

    override fun observeAllTripSummaries(): Flow<Map<Long, dev.tuandoan.expensetracker.domain.repository.TripSummary>> =
        MutableStateFlow(emptyMap())

    override suspend fun commitConversion(draft: dev.tuandoan.expensetracker.domain.model.ConversionDraft): Long =
        error("not used")
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
