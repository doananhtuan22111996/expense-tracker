package dev.tuandoan.expensetracker.ui.screen.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.backup.BackupCrypto
import dev.tuandoan.expensetracker.data.backup.BackupCryptoException
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

        /**
         * Called after the user confirms "Replace All" on the restore dialog.
         *
         * Reads the first 4 bytes of the picked URI to decide whether the file is an
         * encrypted `.etbackup` container (ETBK magic) or a plain JSON/gzip backup.
         * For encrypted files we stash the URI and show a decrypt password dialog;
         * for plain files we kick off the import immediately.
         *
         * The probe stream is a short-lived `openInputStream` that's closed before
         * the actual import opens a fresh stream — SAF permissions persist for the
         * URI lifetime so reopening is safe.
         */
        fun confirmRestore() {
            val uri = _uiState.value.pendingRestoreUri ?: return
            _uiState.update { it.copy(pendingRestoreUri = null) }
            backupJob =
                viewModelScope.launch {
                    val encrypted =
                        try {
                            withContext(ioDispatcher) { probeIsEncrypted(uri) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(
                                    errorMessage =
                                        UiText.StringResource(
                                            R.string.error_import_failed,
                                            listOf(e.message ?: ""),
                                        ),
                                )
                            }
                            return@launch
                        }
                    if (encrypted) {
                        _uiState.update { it.copy(pendingImportDecryptUri = uri) }
                    } else {
                        runImport(uri, decrypt = null)
                    }
                }
        }

        /**
         * Dismisses the import password dialog without starting a restore.
         * The caller is expected to also zero any password input it kept locally.
         */
        fun dismissImportPasswordDialog() {
            _uiState.update {
                it.copy(pendingImportDecryptUri = null, importPasswordError = null)
            }
        }

        /**
         * Confirms the password typed into the import dialog and starts decryption.
         * If the password is wrong, the dialog is re-surfaced with an inline error
         * so the user can retry without re-picking the file.
         */
        fun onImportPasswordConfirmed(password: CharArray) {
            val uri = _uiState.value.pendingImportDecryptUri ?: return
            _uiState.update {
                it.copy(pendingImportDecryptUri = null, importPasswordError = null)
            }
            runImport(uri, decrypt = EncryptOptions(password.copyOf()))
        }

        private fun runImport(
            uri: Uri,
            decrypt: EncryptOptions?,
        ) {
            backupJob =
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            backupOperation = BackupOperation.Importing,
                            backupProgress = 0f,
                        )
                    }
                    try {
                        val result =
                            withContext(ioDispatcher) {
                                contentResolver.openInputStream(uri)?.use { inputStream ->
                                    backupRepository.importBackup(inputStream, decrypt) { progress ->
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
                    } catch (e: BackupCryptoException.WrongPassword) {
                        // Re-surface the dialog so the user can retry without re-picking.
                        _uiState.update {
                            it.copy(
                                backupOperation = BackupOperation.Idle,
                                backupProgress = null,
                                pendingImportDecryptUri = uri,
                                importPasswordError =
                                    UiText.StringResource(R.string.settings_backup_password_wrong),
                            )
                        }
                    } catch (e: BackupCryptoException) {
                        // MalformedHeader / UnsupportedVersion / DecryptionFailed — don't
                        // leak exception details; the file is either corrupt or from a
                        // newer app version. Dedicated string avoids the trailing
                        // ": " left by error_import_failed's %1$s placeholder.
                        _uiState.update {
                            it.copy(
                                backupOperation = BackupOperation.Idle,
                                backupProgress = null,
                                errorMessage =
                                    UiText.StringResource(R.string.error_import_file_corrupted),
                            )
                        }
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
                    } finally {
                        decrypt?.close()
                    }
                }
        }

        /**
         * Reads the first 4 bytes of the picked URI to decide whether it's an encrypted
         * `.etbackup` (ETBK magic) container. Runs on [ioDispatcher]; returns `false` if
         * the stream is unopenable or truncated, letting the existing plain-import
         * path surface the real error when it tries again.
         */
        private fun probeIsEncrypted(uri: Uri): Boolean {
            val stream = contentResolver.openInputStream(uri) ?: return false
            return stream.use { input ->
                val header = ByteArray(BackupCrypto.MAGIC_LENGTH)
                val read = input.read(header)
                BackupCrypto.isEtbkHeader(header, read)
            }
        }

        fun setAnalyticsConsent(enabled: Boolean) {
            viewModelScope.launch {
                analyticsPreferences.setAnalyticsConsent(enabled)
            }
        }

        /**
         * Toggles encrypted backups. Turning encryption **on** for the first time
         * surfaces a one-time warning dialog ("If you forget this password, your
         * data cannot be recovered") which the user must explicitly acknowledge
         * before the toggle is persisted. Turning it off (or re-enabling after
         * acknowledgement) writes through immediately.
         */
        fun setEncryptBackupsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                if (!enabled) {
                    backupEncryptionPreferences.setEncryptBackups(false)
                    return@launch
                }
                val acknowledged =
                    backupEncryptionPreferences.hasAcknowledgedPasswordWarning.first()
                if (acknowledged) {
                    backupEncryptionPreferences.setEncryptBackups(true)
                } else {
                    _uiState.update { it.copy(pendingEncryptToggleAck = true) }
                }
            }
        }

        /**
         * Called when the user taps "I understand" on the forgotten-password warning.
         * Persists both the ack flag and the encrypt toggle, then clears the pending
         * dialog state. Both writes run sequentially on [ioDispatcher] inside the
         * preferences impl — the order guarantees a caller restarting mid-flow sees
         * the ack before the toggle flips.
         */
        fun confirmPasswordWarning() {
            viewModelScope.launch {
                backupEncryptionPreferences.setHasAcknowledgedPasswordWarning(true)
                backupEncryptionPreferences.setEncryptBackups(true)
                _uiState.update { it.copy(pendingEncryptToggleAck = false) }
            }
        }

        /**
         * Dismisses the warning dialog without persisting anything. The toggle stays
         * off; the ack flag remains unset so the dialog reappears on the next attempt.
         */
        fun dismissPasswordWarning() {
            _uiState.update { it.copy(pendingEncryptToggleAck = false) }
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
    val pendingImportDecryptUri: Uri? = null,
    val importPasswordError: UiText? = null,
    val pendingEncryptToggleAck: Boolean = false,
)
