package dev.tuandoan.expensetracker.ui.screen.settings

import android.content.ContentResolver
import android.net.Uri
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.backup.BackupCryptoException
import dev.tuandoan.expensetracker.data.backup.BackupValidationError
import dev.tuandoan.expensetracker.data.backup.BackupValidationException
import dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferences
import dev.tuandoan.expensetracker.data.preferences.BackupEncryptionPreferences
import dev.tuandoan.expensetracker.data.preferences.FakeBudgetAlertPreferences
import dev.tuandoan.expensetracker.data.preferences.FakeThemePreferencesRepository
import dev.tuandoan.expensetracker.data.preferences.ThemePreference
import dev.tuandoan.expensetracker.domain.crash.NoOpCrashReporter
import dev.tuandoan.expensetracker.domain.model.RecurringTransaction
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.BackupRepository
import dev.tuandoan.expensetracker.domain.repository.BackupRestoreResult
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeCurrencyPreferenceRepo: FakeCurrencyPreferenceRepository
    private lateinit var fakeBackupRepository: FakeBackupRepository
    private lateinit var fakeRecurringRepo: FakeRecurringTransactionRepository
    private lateinit var fakeThemeRepo: FakeThemePreferencesRepository
    private lateinit var fakeAnalyticsPrefs: FakeAnalyticsPreferences
    private lateinit var fakeBudgetAlertPrefs: FakeBudgetAlertPreferences
    private lateinit var fakeBackupEncryptionPrefs: FakeBackupEncryptionPreferences
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var mockUri: Uri

    @Before
    fun setup() {
        fakeCurrencyPreferenceRepo = FakeCurrencyPreferenceRepository()
        fakeBackupRepository = FakeBackupRepository()
        fakeRecurringRepo = FakeRecurringTransactionRepository()
        fakeThemeRepo = FakeThemePreferencesRepository()
        fakeAnalyticsPrefs = FakeAnalyticsPreferences()
        fakeBudgetAlertPrefs = FakeBudgetAlertPreferences()
        fakeBackupEncryptionPrefs = FakeBackupEncryptionPreferences()
        mockContentResolver = Mockito.mock(ContentResolver::class.java)
        mockUri = Mockito.mock(Uri::class.java)
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            fakeCurrencyPreferenceRepo,
            fakeBackupRepository,
            mockContentResolver,
            fakeRecurringRepo,
            fakeThemeRepo,
            fakeAnalyticsPrefs,
            fakeBudgetAlertPrefs,
            fakeBackupEncryptionPrefs,
            NoOpCrashReporter(),
            mainDispatcherRule.testDispatcher,
        )

    // --- Theme tests ---

    @Test
    fun themePreference_defaultsToSystem() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            val collectJob =
                backgroundScope.launch {
                    viewModel.themePreference.collect {}
                }
            advanceUntilIdle()

            assertEquals(ThemePreference.SYSTEM, viewModel.themePreference.value)
            collectJob.cancel()
        }

    @Test
    fun setTheme_light_delegatesToRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            val collectJob =
                backgroundScope.launch {
                    viewModel.themePreference.collect {}
                }
            advanceUntilIdle()

            viewModel.setTheme(ThemePreference.LIGHT)
            advanceUntilIdle()

            assertEquals(ThemePreference.LIGHT, viewModel.themePreference.value)
            collectJob.cancel()
        }

    @Test
    fun setTheme_dark_reflectsInStateFlow() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            val collectJob =
                backgroundScope.launch {
                    viewModel.themePreference.collect {}
                }
            advanceUntilIdle()

            viewModel.setTheme(ThemePreference.DARK)
            advanceUntilIdle()

            assertEquals(ThemePreference.DARK, viewModel.themePreference.value)
            collectJob.cancel()
        }

    // --- Currency tests ---

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
    fun onCurrencySelected_unsupportedCode_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCurrencySelected("INVALID")
            advanceUntilIdle()

            val error = viewModel.uiState.value.errorMessage
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.error_unsupported_currency, (error as UiText.StringResource).resId)
        }

    // --- Backup state tests ---

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

            assertNull(viewModel.uiState.value.backupMessage)
        }

    @Test
    fun clearError_clearsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            // Trigger an error first
            viewModel.onCurrencySelected("INVALID")
            advanceUntilIdle()
            val error = viewModel.uiState.value.errorMessage
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.error_unsupported_currency, (error as UiText.StringResource).resId)

            viewModel.clearError()

            assertNull(viewModel.uiState.value.errorMessage)
        }

    // --- Export tests ---

    @Test
    fun exportBackup_success_setsBackupMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val outputStream = ByteArrayOutputStream()
            Mockito
                .`when`(mockContentResolver.openOutputStream(mockUri))
                .thenReturn(outputStream)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onExportUriReady(mockUri)
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            val msg = viewModel.uiState.value.backupMessage
            assertTrue(msg is UiText.StringResource)
            assertEquals(R.string.backup_exported, (msg as UiText.StringResource).resId)
            assertTrue(outputStream.toByteArray().isNotEmpty())
        }

    @Test
    fun exportBackup_repositoryThrows_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val outputStream = ByteArrayOutputStream()
            Mockito
                .`when`(mockContentResolver.openOutputStream(mockUri))
                .thenReturn(outputStream)
            fakeBackupRepository.exportException = RuntimeException("DB error")

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onExportUriReady(mockUri)
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            val error = viewModel.uiState.value.errorMessage
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.error_export_failed, (error as UiText.StringResource).resId)
        }

    @Test
    fun exportBackup_nullOutputStream_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            Mockito
                .`when`(mockContentResolver.openOutputStream(mockUri))
                .thenReturn(null)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onExportUriReady(mockUri)
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            val error = viewModel.uiState.value.errorMessage
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.error_export_failed, (error as UiText.StringResource).resId)
        }

    // --- Import tests ---

    @Test
    fun onRestoreFileSelected_setsPendingRestoreUri() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)

            assertEquals(mockUri, viewModel.uiState.value.pendingRestoreUri)
        }

    @Test
    fun dismissRestoreConfirmation_clearsPendingRestoreUri() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.dismissRestoreConfirmation()

            assertNull(viewModel.uiState.value.pendingRestoreUri)
        }

    @Test
    fun confirmRestore_success_setsBackupMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val inputStream = ByteArrayInputStream("{}".toByteArray())
            Mockito
                .`when`(mockContentResolver.openInputStream(mockUri))
                .thenReturn(inputStream)
            fakeBackupRepository.importResult = BackupRestoreResult(5, 42)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.confirmRestore()
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            val msg = viewModel.uiState.value.backupMessage
            assertTrue(msg is UiText.StringResource)
            assertEquals(R.string.import_result, (msg as UiText.StringResource).resId)
            assertNull(viewModel.uiState.value.pendingRestoreUri)
        }

    @Test
    fun confirmRestore_repositoryThrows_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val inputStream = ByteArrayInputStream("{}".toByteArray())
            Mockito
                .`when`(mockContentResolver.openInputStream(mockUri))
                .thenReturn(inputStream)
            fakeBackupRepository.importException =
                IllegalArgumentException("Invalid backup file format")

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.confirmRestore()
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            val error = viewModel.uiState.value.errorMessage
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.error_import_failed, (error as UiText.StringResource).resId)
        }

    @Test
    fun confirmRestore_validationException_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val inputStream = ByteArrayInputStream("{}".toByteArray())
            Mockito
                .`when`(mockContentResolver.openInputStream(mockUri))
                .thenReturn(inputStream)
            fakeBackupRepository.importException =
                BackupValidationException(
                    listOf(BackupValidationError.UnsupportedSchemaVersion(99)),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.confirmRestore()
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            val error = viewModel.uiState.value.errorMessage
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.error_import_failed, (error as UiText.StringResource).resId)
        }

    @Test
    fun confirmRestore_nullInputStream_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            Mockito
                .`when`(mockContentResolver.openInputStream(mockUri))
                .thenReturn(null)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.confirmRestore()
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            val error = viewModel.uiState.value.errorMessage
            assertTrue(error is UiText.StringResource)
            assertEquals(R.string.error_import_failed, (error as UiText.StringResource).resId)
        }

    // --- Import: encrypted (.etbackup) password prompt ---

    /** Stubs the ContentResolver to return two independent input streams on
     *  successive openInputStream(uri) calls — one for the 4-byte magic probe,
     *  one for the actual restore read. Real SAF providers allow reopens. */
    private fun stubReopenableStream(bytes: ByteArray) {
        Mockito
            .`when`(mockContentResolver.openInputStream(mockUri))
            .thenAnswer { ByteArrayInputStream(bytes) }
    }

    private fun etbkBytes(): ByteArray =
        // Magic "ETBK" + a version byte; enough for the probe, content beyond
        // is irrelevant since FakeBackupRepository doesn't parse.
        byteArrayOf(0x45, 0x54, 0x42, 0x4B, 0x01) // 'E','T','B','K',1

    @Test
    fun confirmRestore_plainFile_importsWithoutDecrypt() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubReopenableStream("{\"schema_version\":1}".toByteArray())
            fakeBackupRepository.importResult = BackupRestoreResult(1, 3)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.confirmRestore()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingImportDecryptUri)
            assertNull(viewModel.uiState.value.importPasswordError)
            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertNull(fakeBackupRepository.lastDecryptOptions)
            val msg = viewModel.uiState.value.backupMessage
            assertTrue(msg is UiText.StringResource)
        }

    @Test
    fun confirmRestore_encryptedFile_surfacesImportPasswordDialog() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubReopenableStream(etbkBytes())

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.confirmRestore()
            advanceUntilIdle()

            assertEquals(mockUri, viewModel.uiState.value.pendingImportDecryptUri)
            assertNull(viewModel.uiState.value.importPasswordError)
            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            // Repository should NOT have been called yet.
            assertNull(fakeBackupRepository.lastDecryptOptions)
        }

    @Test
    fun onImportPasswordConfirmed_correctPassword_importsAndZeroesPassword() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubReopenableStream(etbkBytes())
            fakeBackupRepository.importResult = BackupRestoreResult(2, 7)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.confirmRestore()
            advanceUntilIdle()

            val password = "correcthorse".toCharArray()
            viewModel.onImportPasswordConfirmed(password)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingImportDecryptUri)
            assertNull(viewModel.uiState.value.importPasswordError)
            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            val decrypt = fakeBackupRepository.lastDecryptOptions
            assertNotNull(decrypt)
            // Repository-owned copy must be zeroed via close() in the finally block.
            assertTrue(decrypt!!.password.all { it == ' ' })
            val msg = viewModel.uiState.value.backupMessage
            assertTrue(msg is UiText.StringResource)
        }

    @Test
    fun onImportPasswordConfirmed_wrongPassword_resurfacesDialogWithErrorAndPreservesUri() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubReopenableStream(etbkBytes())
            // First attempt throws WrongPassword; second call (retry) will succeed.
            fakeBackupRepository.importExceptionSequence.addLast(
                BackupCryptoException.WrongPassword(),
            )
            fakeBackupRepository.importResult = BackupRestoreResult(0, 1)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.confirmRestore()
            advanceUntilIdle()

            viewModel.onImportPasswordConfirmed("wrong1234".toCharArray())
            advanceUntilIdle()

            assertEquals(mockUri, viewModel.uiState.value.pendingImportDecryptUri)
            val err = viewModel.uiState.value.importPasswordError
            assertTrue(err is UiText.StringResource)
            assertEquals(
                R.string.settings_backup_password_wrong,
                (err as UiText.StringResource).resId,
            )
            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)

            // Retry with correct password succeeds.
            viewModel.onImportPasswordConfirmed("correcthorse".toCharArray())
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingImportDecryptUri)
            assertNull(viewModel.uiState.value.importPasswordError)
            val msg = viewModel.uiState.value.backupMessage
            assertTrue(msg is UiText.StringResource)
        }

    @Test
    fun dismissImportPasswordDialog_clearsStateAndDoesNotImport() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubReopenableStream(etbkBytes())

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.confirmRestore()
            advanceUntilIdle()

            viewModel.dismissImportPasswordDialog()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingImportDecryptUri)
            assertNull(viewModel.uiState.value.importPasswordError)
            assertNull(fakeBackupRepository.lastDecryptOptions)
        }

    @Test
    fun onImportPasswordConfirmed_noPendingUri_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onImportPasswordConfirmed("anything1".toCharArray())
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertNull(fakeBackupRepository.lastDecryptOptions)
        }

    @Test
    fun onImportPasswordConfirmed_malformedHeader_showsGenericErrorAndClearsDialog() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubReopenableStream(etbkBytes())
            fakeBackupRepository.importExceptionSequence.addLast(
                BackupCryptoException.MalformedHeader("truncated"),
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.confirmRestore()
            advanceUntilIdle()

            viewModel.onImportPasswordConfirmed("doesntmatter".toCharArray())
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingImportDecryptUri)
            val err = viewModel.uiState.value.errorMessage
            assertTrue(err is UiText.StringResource)
            assertEquals(
                R.string.error_import_file_corrupted,
                (err as UiText.StringResource).resId,
            )
        }

    @Test
    fun confirmRestore_noPendingUri_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.confirmRestore()
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertNull(viewModel.uiState.value.backupMessage)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun exportBackup_success_progressResetsToNull() =
        runTest(mainDispatcherRule.testDispatcher) {
            val outputStream = ByteArrayOutputStream()
            Mockito
                .`when`(mockContentResolver.openOutputStream(mockUri))
                .thenReturn(outputStream)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onExportUriReady(mockUri)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.backupProgress)
        }

    @Test
    fun confirmRestore_success_progressResetsToNull() =
        runTest(mainDispatcherRule.testDispatcher) {
            val inputStream = ByteArrayInputStream("{}".toByteArray())
            Mockito
                .`when`(mockContentResolver.openInputStream(mockUri))
                .thenReturn(inputStream)
            fakeBackupRepository.importResult = BackupRestoreResult(1, 10)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onRestoreFileSelected(mockUri)
            viewModel.confirmRestore()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.backupProgress)
            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
        }

    @Test
    fun cancelOperation_resetsStateToIdle() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.cancelOperation()
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertNull(viewModel.uiState.value.backupProgress)
        }

    // --- Active recurring count tests ---

    @Test
    fun activeRecurringCount_emitsZeroWhenNoRecurring() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(0, viewModel.activeRecurringCount.value)
        }

    @Test
    fun activeRecurringCount_emitsCorrectCountFromRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val active1 =
                RecurringTransaction(
                    id = 1,
                    type = dev.tuandoan.expensetracker.domain.model.TransactionType.EXPENSE,
                    amount = 100,
                    currencyCode = "USD",
                    categoryId = 1,
                    note = null,
                    frequency = dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency.MONTHLY,
                    dayOfMonth = 1,
                    dayOfWeek = null,
                    nextDueMillis = System.currentTimeMillis(),
                    isActive = true,
                )
            val active2 = active1.copy(id = 2)
            val inactive = active1.copy(id = 3, isActive = false)

            fakeRecurringRepo.recurringFlow.value = listOf(active1, active2, inactive)

            val viewModel = createViewModel()

            // Start collecting to activate WhileSubscribed
            val collectJob =
                backgroundScope.launch {
                    viewModel.activeRecurringCount.collect {}
                }
            advanceUntilIdle()

            assertEquals(2, viewModel.activeRecurringCount.value)
            collectJob.cancel()
        }

    // --- Analytics consent tests ---

    @Test
    fun analyticsConsent_defaultsToFalse() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            val collectJob =
                backgroundScope.launch {
                    viewModel.analyticsConsent.collect {}
                }
            advanceUntilIdle()

            assertEquals(false, viewModel.analyticsConsent.value)
            collectJob.cancel()
        }

    @Test
    fun setAnalyticsConsent_true_reflectsInStateFlow() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            val collectJob =
                backgroundScope.launch {
                    viewModel.analyticsConsent.collect {}
                }
            advanceUntilIdle()

            viewModel.setAnalyticsConsent(true)
            advanceUntilIdle()

            assertEquals(true, viewModel.analyticsConsent.value)
            collectJob.cancel()
        }

    @Test
    fun setAnalyticsConsent_false_reflectsInStateFlow() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            val collectJob =
                backgroundScope.launch {
                    viewModel.analyticsConsent.collect {}
                }
            advanceUntilIdle()

            viewModel.setAnalyticsConsent(true)
            advanceUntilIdle()
            viewModel.setAnalyticsConsent(false)
            advanceUntilIdle()

            assertEquals(false, viewModel.analyticsConsent.value)
            collectJob.cancel()
        }

    // --- Encrypt backup tests ---

    @Test
    fun encryptBackupsEnabled_defaultsToFalse() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            val collectJob =
                backgroundScope.launch {
                    viewModel.encryptBackupsEnabled.collect {}
                }
            advanceUntilIdle()

            assertEquals(false, viewModel.encryptBackupsEnabled.value)
            collectJob.cancel()
        }

    @Test
    fun setEncryptBackupsEnabled_true_reflectsInStateFlow() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            val collectJob =
                backgroundScope.launch {
                    viewModel.encryptBackupsEnabled.collect {}
                }
            advanceUntilIdle()

            viewModel.setEncryptBackupsEnabled(true)
            advanceUntilIdle()

            assertEquals(true, viewModel.encryptBackupsEnabled.value)
            collectJob.cancel()
        }

    @Test
    fun onExportClicked_whenEncryptDisabled_emitsPlainEvent() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onExportClicked()
            val event = viewModel.exportLaunchEvents.first()

            assertEquals(ExportLaunchEvent.Plain, event)
        }

    @Test
    fun onExportClicked_whenEncryptEnabled_emitsEncryptedEvent() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEncryptBackupsEnabled(true)
            advanceUntilIdle()

            viewModel.onExportClicked()
            val event = viewModel.exportLaunchEvents.first()

            assertEquals(ExportLaunchEvent.Encrypted, event)
        }

    @Test
    fun onExportUriReady_afterEncryptedClick_setsPendingExportUri() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEncryptBackupsEnabled(true)
            advanceUntilIdle()
            viewModel.onExportClicked()
            viewModel.exportLaunchEvents.first()

            viewModel.onExportUriReady(mockUri)

            assertEquals(mockUri, viewModel.uiState.value.pendingExportUri)
            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
        }

    @Test
    fun onExportUriReady_afterPlainClick_exportsImmediately() =
        runTest(mainDispatcherRule.testDispatcher) {
            val outputStream = ByteArrayOutputStream()
            Mockito
                .`when`(mockContentResolver.openOutputStream(mockUri))
                .thenReturn(outputStream)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onExportClicked()
            viewModel.exportLaunchEvents.first()

            viewModel.onExportUriReady(mockUri)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingExportUri)
            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            val msg = viewModel.uiState.value.backupMessage
            assertTrue(msg is UiText.StringResource)
            assertEquals(R.string.backup_exported, (msg as UiText.StringResource).resId)
        }

    @Test
    fun dismissExportPasswordDialog_clearsPendingExportUri() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEncryptBackupsEnabled(true)
            advanceUntilIdle()
            viewModel.onExportClicked()
            viewModel.exportLaunchEvents.first()
            viewModel.onExportUriReady(mockUri)
            viewModel.dismissExportPasswordDialog()

            assertNull(viewModel.uiState.value.pendingExportUri)
        }

    @Test
    fun onExportPasswordConfirmed_passesEncryptOptionsAndZeroesAfterExport() =
        runTest(mainDispatcherRule.testDispatcher) {
            val outputStream = ByteArrayOutputStream()
            Mockito
                .`when`(mockContentResolver.openOutputStream(mockUri))
                .thenReturn(outputStream)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setEncryptBackupsEnabled(true)
            advanceUntilIdle()
            viewModel.onExportClicked()
            viewModel.exportLaunchEvents.first()
            viewModel.onExportUriReady(mockUri)

            val password = "hunter22!".toCharArray()
            viewModel.onExportPasswordConfirmed(password)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingExportUri)
            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            val capturedEncrypt = fakeBackupRepository.lastEncryptOptions
            assertNotNull(capturedEncrypt)
            // Repository-owned copy must be zeroed via close() in the finally block.
            assertTrue(capturedEncrypt!!.password.all { it == '\u0000' })
        }

    @Test
    fun onExportClicked_whenPrefReadFails_surfacesErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeBackupEncryptionPrefs.readError = RuntimeException("datastore down")
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onExportClicked()
            advanceUntilIdle()

            val err = viewModel.uiState.value.errorMessage
            assertTrue(err is UiText.StringResource)
            assertEquals(R.string.error_export_failed, (err as UiText.StringResource).resId)
        }

    @Test
    fun onExportClicked_twiceInFlight_emitsOnlyOneEvent() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            // Two rapid clicks. The first launches a coroutine that only runs
            // when we advance — the second should see clickInFlight=true and
            // be a no-op, so only one event is ever sent to the channel.
            viewModel.onExportClicked()
            viewModel.onExportClicked()
            advanceUntilIdle()

            // Drain: first event should be immediately available, second should not.
            val first = viewModel.exportLaunchEvents.first()
            assertEquals(ExportLaunchEvent.Plain, first)

            var secondReceived = false
            val job =
                launch {
                    viewModel.exportLaunchEvents.first()
                    secondReceived = true
                }
            advanceUntilIdle()
            assertEquals(false, secondReceived)
            job.cancel()
        }

    @Test
    fun onExportPasswordConfirmed_noPendingUri_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onExportPasswordConfirmed("anything".toCharArray())
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertNull(viewModel.uiState.value.backupMessage)
        }

    // --- Budget alerts tests ---

    @Test
    fun budgetAlertsEnabled_defaultsToFalse() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            val collectJob =
                backgroundScope.launch {
                    viewModel.budgetAlertsEnabled.collect {}
                }
            advanceUntilIdle()

            assertEquals(false, viewModel.budgetAlertsEnabled.value)
            collectJob.cancel()
        }

    @Test
    fun setBudgetAlertsEnabled_true_reflectsInStateFlow() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            val collectJob =
                backgroundScope.launch {
                    viewModel.budgetAlertsEnabled.collect {}
                }
            advanceUntilIdle()

            viewModel.setBudgetAlertsEnabled(true)
            advanceUntilIdle()

            assertEquals(true, viewModel.budgetAlertsEnabled.value)
            collectJob.cancel()
        }

    @Test
    fun setBudgetAlertsEnabled_false_reflectsInStateFlow() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            val collectJob =
                backgroundScope.launch {
                    viewModel.budgetAlertsEnabled.collect {}
                }
            advanceUntilIdle()

            viewModel.setBudgetAlertsEnabled(true)
            advanceUntilIdle()
            viewModel.setBudgetAlertsEnabled(false)
            advanceUntilIdle()

            assertEquals(false, viewModel.budgetAlertsEnabled.value)
            collectJob.cancel()
        }

    private class FakeBackupEncryptionPreferences : BackupEncryptionPreferences {
        private val state = MutableStateFlow(false)
        var readError: Throwable? = null
        override val encryptBackups: Flow<Boolean> =
            kotlinx.coroutines.flow.flow {
                readError?.let { throw it }
                state.collect { emit(it) }
            }

        override suspend fun setEncryptBackups(enabled: Boolean) {
            state.value = enabled
        }
    }

    private class FakeAnalyticsPreferences : AnalyticsPreferences {
        private val consentState = MutableStateFlow(false)
        override val analyticsConsent: Flow<Boolean> = consentState

        override suspend fun setAnalyticsConsent(enabled: Boolean) {
            consentState.value = enabled
        }
    }

    private class FakeRecurringTransactionRepository : RecurringTransactionRepository {
        val recurringFlow = MutableStateFlow<List<RecurringTransaction>>(emptyList())

        override fun observeAll(): Flow<List<RecurringTransaction>> = recurringFlow

        override suspend fun getById(id: Long): RecurringTransaction? = null

        override suspend fun create(recurring: RecurringTransaction): Long = 1L

        override suspend fun update(recurring: RecurringTransaction) {}

        override suspend fun delete(id: Long) {}

        override suspend fun setActive(
            id: Long,
            active: Boolean,
        ) {}

        override suspend fun processDueRecurring() {}
    }

    private class FakeBackupRepository : BackupRepository {
        var exportException: Exception? = null
        var importException: Exception? = null
        var importResult: BackupRestoreResult = BackupRestoreResult(categoryCount = 0, transactionCount = 0)
        var lastEncryptOptions: dev.tuandoan.expensetracker.domain.repository.EncryptOptions? = null
        var lastDecryptOptions: dev.tuandoan.expensetracker.domain.repository.EncryptOptions? = null
        var importExceptionSequence: ArrayDeque<Exception> = ArrayDeque()

        override suspend fun exportBackupJson(): String {
            exportException?.let { throw it }
            return "{\"schema_version\":1,\"app_version_name\":\"1.5.0\"," +
                "\"created_at_epoch_ms\":0,\"default_currency_code\":\"VND\"," +
                "\"device_locale\":\"en-US\",\"categories\":[],\"transactions\":[]}"
        }

        override suspend fun importBackupJson(json: String): BackupRestoreResult {
            importException?.let { throw it }
            return importResult
        }

        override suspend fun exportBackup(
            outputStream: java.io.OutputStream,
            encrypt: dev.tuandoan.expensetracker.domain.repository.EncryptOptions?,
            onProgress: suspend (dev.tuandoan.expensetracker.domain.repository.BackupProgress) -> Unit,
        ) {
            lastEncryptOptions = encrypt
            exportException?.let { throw it }
            val json = exportBackupJson()
            onProgress(
                dev.tuandoan.expensetracker.domain.repository
                    .BackupProgress(0, 0),
            )
            outputStream.write(json.toByteArray(Charsets.UTF_8))
            onProgress(
                dev.tuandoan.expensetracker.domain.repository
                    .BackupProgress(0, 0),
            )
        }

        override suspend fun importBackup(
            inputStream: java.io.InputStream,
            decrypt: dev.tuandoan.expensetracker.domain.repository.EncryptOptions?,
            onProgress: suspend (dev.tuandoan.expensetracker.domain.repository.BackupProgress) -> Unit,
        ): BackupRestoreResult {
            lastDecryptOptions = decrypt
            if (importExceptionSequence.isNotEmpty()) {
                throw importExceptionSequence.removeFirst()
            }
            importException?.let { throw it }
            onProgress(
                dev.tuandoan.expensetracker.domain.repository
                    .BackupProgress(0, importResult.transactionCount),
            )
            onProgress(
                dev.tuandoan.expensetracker.domain.repository.BackupProgress(
                    importResult.transactionCount,
                    importResult.transactionCount,
                ),
            )
            return importResult
        }

        override suspend fun exportCsv(outputStream: java.io.OutputStream) {
            exportException?.let { throw it }
            outputStream.write("Date,Type,Amount,Currency,Category,Note\n".toByteArray(Charsets.UTF_8))
        }
    }
}
