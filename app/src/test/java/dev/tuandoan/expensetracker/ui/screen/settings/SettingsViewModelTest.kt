package dev.tuandoan.expensetracker.ui.screen.settings

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import dev.tuandoan.expensetracker.data.backup.BackupValidationError
import dev.tuandoan.expensetracker.data.backup.BackupValidationException
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.BackupRepository
import dev.tuandoan.expensetracker.domain.repository.BackupRestoreResult
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeCurrencyPreferenceRepo: FakeCurrencyPreferenceRepository
    private lateinit var fakeBackupRepository: FakeBackupRepository
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var mockUri: Uri

    @Before
    fun setup() {
        fakeCurrencyPreferenceRepo = FakeCurrencyPreferenceRepository()
        fakeBackupRepository = FakeBackupRepository()
        mockContentResolver = Mockito.mock(ContentResolver::class.java)
        mockUri = Mockito.mock(Uri::class.java)
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            fakeCurrencyPreferenceRepo,
            fakeBackupRepository,
            mockContentResolver,
        )

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

            assertEquals("Unsupported currency code", viewModel.uiState.value.errorMessage)
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
            assertEquals("Unsupported currency code", viewModel.uiState.value.errorMessage)

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

            viewModel.exportBackup(mockUri)
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertEquals("Backup exported successfully", viewModel.uiState.value.backupMessage)
            assertTrue(outputStream.toByteArray().isNotEmpty())
        }

    @Test
    fun exportBackup_repositoryThrows_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeBackupRepository.exportException = RuntimeException("DB error")

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.exportBackup(mockUri)
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertEquals("Export failed: DB error", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun exportBackup_nullOutputStream_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            Mockito
                .`when`(mockContentResolver.openOutputStream(mockUri))
                .thenReturn(null)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.exportBackup(mockUri)
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertEquals(
                "Export failed: Cannot open output stream",
                viewModel.uiState.value.errorMessage,
            )
        }

    // --- Import tests ---

    @Test
    fun importBackup_success_setsBackupMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val json = """{"valid": "json"}"""
            val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
            Mockito
                .`when`(mockContentResolver.openInputStream(mockUri))
                .thenReturn(inputStream)
            setupFileSizeCursor(json.length.toLong())

            fakeBackupRepository.importResult =
                BackupRestoreResult(
                    categoryCount = 3,
                    transactionCount = 10,
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.importBackup(mockUri)
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertEquals(
                "Backup restored: 3 categories, 10 transactions",
                viewModel.uiState.value.backupMessage,
            )
        }

    @Test
    fun importBackup_repositoryThrows_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val json = """{"some": "json"}"""
            val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
            Mockito
                .`when`(mockContentResolver.openInputStream(mockUri))
                .thenReturn(inputStream)
            setupFileSizeCursor(json.length.toLong())

            fakeBackupRepository.importException = RuntimeException("Parse error")

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.importBackup(mockUri)
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertEquals("Import failed: Parse error", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun importBackup_validationException_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val json = """{"some": "json"}"""
            val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
            Mockito
                .`when`(mockContentResolver.openInputStream(mockUri))
                .thenReturn(inputStream)
            setupFileSizeCursor(json.length.toLong())

            fakeBackupRepository.importException =
                BackupValidationException(
                    listOf(
                        BackupValidationError.UnsupportedSchemaVersion(99),
                        BackupValidationError.DuplicateCategoryId(1L),
                    ),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.importBackup(mockUri)
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertEquals(
                "Backup validation failed with 2 error(s)",
                viewModel.uiState.value.errorMessage,
            )
        }

    @Test
    fun importBackup_fileTooLarge_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val tooLargeSize = SettingsViewModel.MAX_IMPORT_FILE_SIZE + 1
            setupFileSizeCursor(tooLargeSize)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.importBackup(mockUri)
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertTrue(
                viewModel.uiState.value.errorMessage!!
                    .contains("File too large"),
            )
        }

    @Test
    fun importBackup_nullInputStream_setsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            setupFileSizeCursor(100L)
            Mockito
                .`when`(mockContentResolver.openInputStream(mockUri))
                .thenReturn(null)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.importBackup(mockUri)
            advanceUntilIdle()

            assertEquals(BackupOperation.Idle, viewModel.uiState.value.backupOperation)
            assertEquals(
                "Import failed: Cannot open input stream",
                viewModel.uiState.value.errorMessage,
            )
        }

    // --- Helpers ---

    private fun setupFileSizeCursor(size: Long) {
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito
            .`when`(
                mockContentResolver.query(
                    Mockito.eq(mockUri),
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.isNull(),
                ),
            ).thenReturn(cursor)
        Mockito.`when`(cursor.getColumnIndex(anyString())).thenReturn(0)
        Mockito.`when`(cursor.moveToFirst()).thenReturn(true)
        Mockito.`when`(cursor.getLong(anyInt())).thenReturn(size)
    }

    private class FakeBackupRepository : BackupRepository {
        var exportException: Exception? = null
        var importException: Exception? = null
        var importResult: BackupRestoreResult =
            BackupRestoreResult(
                categoryCount = 0,
                transactionCount = 0,
            )

        override suspend fun exportBackupJson(): String {
            exportException?.let { throw it }
            return """{"schema_version":1,"categories":[],"transactions":[]}"""
        }

        override suspend fun importBackupJson(json: String): BackupRestoreResult {
            importException?.let { throw it }
            return importResult
        }
    }
}
