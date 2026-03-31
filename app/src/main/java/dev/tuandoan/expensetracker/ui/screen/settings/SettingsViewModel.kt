package dev.tuandoan.expensetracker.ui.screen.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferences
import dev.tuandoan.expensetracker.data.preferences.ThemePreference
import dev.tuandoan.expensetracker.data.preferences.ThemePreferencesRepository
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import dev.tuandoan.expensetracker.domain.model.CurrencyDefinition
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.BackupRepository
import dev.tuandoan.expensetracker.domain.repository.BackupRestoreResult
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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
        private val crashReporter: CrashReporter,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
        private var backupJob: Job? = null

        val themePreference: StateFlow<ThemePreference> =
            themePreferencesRepository.themePreference
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ThemePreference.SYSTEM)

        /** Whether the user has opted in to anonymous crash reporting. */
        val analyticsConsent: StateFlow<Boolean> =
            analyticsPreferences.analyticsConsent
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

        fun exportBackup(uri: Uri) {
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
                                backupRepository.exportBackup(outputStream) { progress ->
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

data class SettingsUiState(
    val selectedCurrencyCode: String = SupportedCurrencies.default().code,
    val availableCurrencies: List<CurrencyDefinition> = SupportedCurrencies.all(),
    val errorMessage: UiText? = null,
    val backupOperation: BackupOperation = BackupOperation.Idle,
    val backupProgress: Float? = null,
    val backupMessage: UiText? = null,
    val pendingRestoreUri: Uri? = null,
)
