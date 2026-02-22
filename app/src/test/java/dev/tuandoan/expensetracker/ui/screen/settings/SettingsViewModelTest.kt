package dev.tuandoan.expensetracker.ui.screen.settings

import dev.tuandoan.expensetracker.data.backup.BackupSerializer
import dev.tuandoan.expensetracker.data.backup.model.BackupDocumentV1
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.BackupRepository
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeCurrencyPreferenceRepo: FakeCurrencyPreferenceRepository
    private lateinit var fakeBackupRepository: FakeBackupRepository

    @Before
    fun setup() {
        fakeCurrencyPreferenceRepo = FakeCurrencyPreferenceRepository()
        fakeBackupRepository = FakeBackupRepository()
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            fakeCurrencyPreferenceRepo,
            fakeBackupRepository,
            BackupSerializer(),
        )

    @Test
    fun init_loadsDefaultCurrency() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(SupportedCurrencies.default().code, state.selectedCurrencyCode)
        }

    @Test
    fun init_availableCurrenciesMatchesSupportedCurrencies() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(SupportedCurrencies.all(), viewModel.uiState.value.availableCurrencies)
        }

    @Test
    fun onCurrencySelected_updatesSelectedCurrency() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCurrencySelected("USD")
            advanceUntilIdle()

            assertEquals("USD", viewModel.uiState.value.selectedCurrencyCode)
            assertTrue(fakeCurrencyPreferenceRepo.setDefaultCurrencyCalled)
            assertEquals("USD", fakeCurrencyPreferenceRepo.lastSetCurrencyCode)
        }

    @Test
    fun onCurrencySelected_withDifferentCurrency_updatesState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCurrencySelected("EUR")
            advanceUntilIdle()

            assertEquals("EUR", viewModel.uiState.value.selectedCurrencyCode)
        }

    @Test
    fun init_withNonDefaultPreference_loadsCorrectCurrency() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCurrencyPreferenceRepo = FakeCurrencyPreferenceRepository(initialCurrency = "JPY")

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals("JPY", viewModel.uiState.value.selectedCurrencyCode)
        }

    @Test
    fun init_backupOperationIsIdle() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
        }

    @Test
    fun clearBackupMessage_clearsMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.clearBackupMessage()

            assertEquals(null, viewModel.uiState.value.backupMessage)
        }

    private class FakeBackupRepository : BackupRepository {
        var shouldThrow: Exception? = null

        override suspend fun createBackupDocument(): BackupDocumentV1 {
            shouldThrow?.let { throw it }
            return BackupDocumentV1(
                appVersionName = "1.5.0",
                createdAtEpochMs = 1700000000000L,
                categories = emptyList(),
                transactions = emptyList(),
            )
        }

        override suspend fun restoreFromBackup(document: BackupDocumentV1) {
            shouldThrow?.let { throw it }
        }
    }
}
