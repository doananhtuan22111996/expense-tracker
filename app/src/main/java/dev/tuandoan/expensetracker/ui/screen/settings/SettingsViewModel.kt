package dev.tuandoan.expensetracker.ui.screen.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferences
import dev.tuandoan.expensetracker.data.preferences.BackupEncryptionPreferences
import dev.tuandoan.expensetracker.data.preferences.ThemePreference
import dev.tuandoan.expensetracker.data.preferences.ThemePreferencesRepository
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import dev.tuandoan.expensetracker.domain.model.CurrencyDefinition
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.BackupRepository
import dev.tuandoan.expensetracker.domain.repository.BackupRestoreResult
import dev.tuandoan.expensetracker.domain.repository.BudgetAlertPreferences
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.EncryptOptions
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
        private val backupRepository: BackupRepository,
        private val contentResolver: ContentResolver,
        private val recurringTransactionRepository: RecurringTransactionRepository,
        private val themePreferencesRepository: ThemePreferencesRepository,
        private val analyticsPreferences: AnalyticsPreferences,
        private val budgetAlertPreferences: BudgetAlertPreferences,
        private val backupEncryptionPreferences: BackupEncryptionPreferences,
        private val crashReporter: CrashReporter,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
        private var backupJob: Job? = null

        // Captured at click time from DataStore so the later URI-ready callback
        // isn't subject to the StateFlow's initial-value race window.
        // @Volatile because it's written on viewModelScope and read on the main
        // thread when the picker returns a URI.
        @Volatile
        private var pendingEncrypt: Boolean = false

        // Guards against a double-tap launching two file pickers while the first
        // DataStore read is still in flight.
        @Volatile
        private var clickInFlight: Boolean = false

        private val _exportLaunchEvents = Channel<ExportLaunchEvent>(Channel.BUFFERED)
        val exportLaunchEvents: Flow<ExportLaunchEvent> = _exportLaunchEvents.receiveAsFlow()

        val themePreference: StateFlow<ThemePreference> =
            themePreferencesRepository.themePreference
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ThemePreference.SYSTEM)

        /** Whether the user has opted in to anonymous crash reporting. */
        val analyticsConsent: StateFlow<Boolean> =
            analyticsPreferences.analyticsConsent
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

        /** Whether budget alerts are enabled. */
        val budgetAlertsEnabled: StateFlow<Boolean> =
            budgetAlertPreferences.alertsEnabled
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

        /** Whether backup export should write an encrypted `.etbackup` container. */
        val encryptBackupsEnabled: StateFlow<Boolean> =
            backupEncryptionPreferences.encryptBackups
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

        /** Number of currently active recurring transactions. */
        val activeRecurringCount: StateFlow<Int> =
            recurringTransactionRepository
                .observeAll()
                .map { items -> items.count { it.isActive } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

        init {
            observeDefaultCurrency()
        }

        fun onCurrencySelected(code: String) {
            viewModelScope.launch {
                try {
                    currencyPreferenceRepository.setDefaultCurrency(code)
                } catch (e: IllegalArgumentException) {
                    _uiState.update {
                        it.copy(
                            errorMessage = UiText.StringResource(R.string.error_unsupported_currency),
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = UiText.StringResource(R.string.error_save_currency)) }
                }
            }
        }

        /**
         * Called when the user taps the export row. Reads the encryption preference
         * one-shot from DataStore (so we never rely on the initial `false` seed of
         * `encryptBackupsEnabled` before the first emission lands) and emits an
         * [ExportLaunchEvent] telling the UI which `CreateDocument` contract to fire.
         */
        fun onExportClicked() {
            if (clickInFlight) return
            clickInFlight = true
            viewModelScope.launch {
                try {
                    val encrypt = backupEncryptionPreferences.encryptBackups.first()
                    pendingEncrypt = encrypt
                    _exportLaunchEvents.send(
                        if (encrypt) ExportLaunchEvent.Encrypted else ExportLaunchEvent.Plain,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            errorMessage =
                                UiText.StringResource(
                                    R.string.error_export_failed,
                                    listOf(e.message ?: ""),
                                ),
                        )
                    }
                } finally {
                    clickInFlight = false
                }
            }
        }

        /**
         * Called by the UI once a target file URI has been chosen for export.
         * If encryption was enabled at click time, stashes the URI and shows the
         * password dialog; otherwise kicks off a plain export immediately.
         */
        fun onExportUriReady(uri: Uri) {
            if (pendingEncrypt) {
                _uiState.update { it.copy(pendingExportUri = uri) }
            } else {
                runExport(uri, encrypt = null)
            }
        }

        /**
         * Dismisses the export password dialog without starting an export.
         * The caller is expected to also zero any password input it kept locally.
         */
        fun dismissExportPasswordDialog() {
            _uiState.update { it.copy(pendingExportUri = null) }
        }

        /**
         * Confirms the password typed into the export dialog and starts the encrypted
         * export. The repository takes ownership of the [password] array long enough to
         * derive the key; this function wraps it in an [EncryptOptions] that is closed
         * (zeroed) once the job completes. The caller should still discard its own
         * copy of the array after calling.
         */
        fun onExportPasswordConfirmed(password: CharArray) {
            val uri = _uiState.value.pendingExportUri ?: return
            _uiState.update { it.copy(pendingExportUri = null) }
            runExport(uri, encrypt = EncryptOptions(password.copyOf()))
        }

        private fun runExport(
            uri: Uri,
            encrypt: EncryptOptions?,
        ) {
            backupJob =
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            backupOperation = BackupOperation.Exporting,
                            backupProgress = 0f,
                        )
                    }
                    try {
                        withContext(ioDispatcher) {
                            contentResolver.openOutputStream(uri)?.use { outputStream ->
                                backupRepository.exportBackup(outputStream, encrypt) { progress ->
                                    val fraction =
                                        if (progress.total > 0) {
                                            progress.current.toFloat() / progress.total
                                        } else {
                                            0f
                                        }
                                    _uiState.update { it.copy(backupProgress = fraction) }
                                }
                            } ?: throw IllegalStateException("Cannot open output stream")
                        }
                        _uiState.update {
                            it.copy(
                                backupOperation = BackupOperation.Idle,
                                backupProgress = null,
                                backupMessage = UiText.StringResource(R.string.backup_exported),
                            )
                        }
                    } catch (e: CancellationException) {
                        _uiState.update {
                            it.copy(
                                backupOperation = BackupOperation.Idle,
                                backupProgress = null,
                            )
                        }
                        throw e
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                backupOperation = BackupOperation.Idle,
                                backupProgress = null,
                                errorMessage =
                                    UiText.StringResource(
                                        R.string.error_export_failed,
                                        listOf(
                                            e.message ?: "",
                                        ),
                                    ),
                            )
                        }
                    } finally {
                        // Zero the password array whether the export succeeded, failed, or was cancelled.
                        encrypt?.close()
                    }
                }
        }

        fun exportCsv(uri: Uri) {
            backupJob =
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            backupOperation = BackupOperation.Exporting,
                            backupProgress = null,
                        )
                    }
                    try {
                        withContext(ioDispatcher) {
                            contentResolver.openOutputStream(uri)?.use { outputStream ->
                                backupRepository.exportCsv(outputStream)
                            } ?: throw IllegalStateException("Cannot open output stream")
                        }
                        _uiState.update {
                            it.copy(
                                backupOperation = BackupOperation.Idle,
                                backupProgress = null,
                                backupMessage = UiText.StringResource(R.string.csv_exported_successfully),
                            )
                        }
                    } catch (e: CancellationException) {
                        _uiState.update {
                            it.copy(
                                backupOperation = BackupOperation.Idle,
                                backupProgress = null,
                            )
                        }
                        throw e
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                backupOperation = BackupOperation.Idle,
                                backupProgress = null,
                                errorMessage =
                                    UiText.StringResource(
                                        R.string.csv_export_failed,
                                        listOf(e.message ?: ""),
                                    ),
                            )
                        }
                    }
                }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun clearBackupMessage() {
            _uiState.update { it.copy(backupMessage = null) }
        }

        fun onRestoreFileSelected(uri: Uri) {
            _uiState.update { it.copy(pendingRestoreUri = uri) }
        }

        fun dismissRestoreConfirmation() {
            _uiState.update { it.copy(pendingRestoreUri = null) }
        }

        fun confirmRestore() {
            val uri = _uiState.value.pendingRestoreUri ?: return
            _uiState.update {
                it.copy(
                    pendingRestoreUri = null,
                    backupOperation = BackupOperation.Importing,
                    backupProgress = 0f,
                )
            }
            backupJob =
                viewModelScope.launch {
                    try {
                        val result =
                            withContext(ioDispatcher) {
                                contentResolver.openInputStream(uri)?.use { inputStream ->
                                    backupRepository.importBackup(inputStream) { progress ->
                                        val fraction =
                                            if (progress.total > 0) {
                                                progress.current.toFloat() / progress.total
                                            } else {
                                                0f
                                            }
                                        _uiState.update { it.copy(backupProgress = fraction) }
                                    }
                                } ?: throw IllegalStateException("Cannot open file")
                            }
                        _uiState.update {
                            it.copy(
                                backupOperation = BackupOperation.Idle,
                                backupProgress = null,
                                backupMessage = buildImportMessage(result),
                            )
                        }
                    } catch (e: CancellationException) {
                        _uiState.update {
                            it.copy(
                                backupOperation = BackupOperation.Idle,
                                backupProgress = null,
                            )
                        }
                        throw e
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                backupOperation = BackupOperation.Idle,
                                backupProgress = null,
                                errorMessage =
                                    UiText.StringResource(
                                        R.string.error_import_failed,
                                        listOf(
                                            e.message ?: "",
                                        ),
                                    ),
                            )
                        }
                    }
                }
        }

        fun setAnalyticsConsent(enabled: Boolean) {
            viewModelScope.launch {
                analyticsPreferences.setAnalyticsConsent(enabled)
            }
        }

        fun setEncryptBackupsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                backupEncryptionPreferences.setEncryptBackups(enabled)
            }
        }

        fun setBudgetAlertsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                budgetAlertPreferences.setAlertsEnabled(enabled)
            }
        }

        fun setTheme(pref: ThemePreference) {
            viewModelScope.launch {
                themePreferencesRepository.setTheme(pref)
            }
        }

        fun cancelOperation() {
            backupJob?.cancel()
            backupJob = null
        }

        private fun buildImportMessage(result: BackupRestoreResult): UiText {
            val parts = mutableListOf<String>()
            parts.add("${result.transactionCount} transactions")
            if (result.goldHoldingCount > 0) {
                parts.add("${result.goldHoldingCount} gold holdings")
            }
            if (result.goldPriceCount > 0) {
                parts.add("${result.goldPriceCount} gold prices")
            }
            return UiText.StringResource(R.string.import_result, listOf(parts.joinToString(", ")))
        }

        private fun observeDefaultCurrency() {
            viewModelScope.launch {
                currencyPreferenceRepository
                    .observeDefaultCurrency()
                    .catch { e ->
                        if (e is Exception) crashReporter.recordException(e)
                    }.collect { currencyCode ->
                        _uiState.update { it.copy(selectedCurrencyCode = currencyCode) }
                    }
            }
        }
    }

enum class BackupOperation {
    Idle,
    Exporting,
    Importing,
}

/** One-shot event telling the Settings screen which export file picker to fire. */
sealed class ExportLaunchEvent {
    data object Plain : ExportLaunchEvent()

    data object Encrypted : ExportLaunchEvent()
}

data class SettingsUiState(
    val selectedCurrencyCode: String = SupportedCurrencies.default().code,
    val availableCurrencies: List<CurrencyDefinition> = SupportedCurrencies.all(),
    val errorMessage: UiText? = null,
    val backupOperation: BackupOperation = BackupOperation.Idle,
    val backupProgress: Float? = null,
    val backupMessage: UiText? = null,
    val pendingRestoreUri: Uri? = null,
    val pendingExportUri: Uri? = null,
)
