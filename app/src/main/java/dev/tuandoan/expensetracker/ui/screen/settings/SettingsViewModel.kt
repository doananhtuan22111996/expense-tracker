package dev.tuandoan.expensetracker.ui.screen.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.model.CurrencyDefinition
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.BackupRepository
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        init {
            observeDefaultCurrency()
        }

        fun onCurrencySelected(code: String) {
            viewModelScope.launch {
                try {
                    currencyPreferenceRepository.setDefaultCurrency(code)
                } catch (e: IllegalArgumentException) {
                    _uiState.value =
                        _uiState.value.copy(
                            errorMessage = "Unsupported currency code",
                        )
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            errorMessage = "Failed to save currency preference",
                        )
                }
            }
        }

        fun exportBackup(uri: Uri) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(backupOperation = BackupOperation.Exporting)
                try {
                    withContext(ioDispatcher) {
                        val json = backupRepository.exportBackupJson()
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(json.toByteArray(Charsets.UTF_8))
                        } ?: throw IllegalStateException("Cannot open output stream")
                    }
                    _uiState.value =
                        _uiState.value.copy(
                            backupOperation = BackupOperation.Idle,
                            backupMessage = "Backup exported successfully",
                        )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            backupOperation = BackupOperation.Idle,
                            errorMessage = "Export failed: ${e.message ?: "Unknown error"}",
                        )
                }
            }
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }

        fun clearBackupMessage() {
            _uiState.value = _uiState.value.copy(backupMessage = null)
        }

        fun onRestoreFileSelected(uri: Uri) {
            _uiState.value = _uiState.value.copy(pendingRestoreUri = uri)
        }

        fun dismissRestoreConfirmation() {
            _uiState.value = _uiState.value.copy(pendingRestoreUri = null)
        }

        fun confirmRestore() {
            val uri = _uiState.value.pendingRestoreUri ?: return
            _uiState.value =
                _uiState.value.copy(
                    pendingRestoreUri = null,
                    backupOperation = BackupOperation.Importing,
                )
            viewModelScope.launch {
                try {
                    val result =
                        withContext(ioDispatcher) {
                            val json =
                                contentResolver.openInputStream(uri)?.use { inputStream ->
                                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                                } ?: throw IllegalStateException("Cannot open file")
                            backupRepository.importBackupJson(json)
                        }
                    _uiState.value =
                        _uiState.value.copy(
                            backupOperation = BackupOperation.Idle,
                            backupMessage = "Imported ${result.transactionCount} transactions",
                        )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            backupOperation = BackupOperation.Idle,
                            errorMessage = "Import failed: ${e.message ?: "Unknown error"}",
                        )
                }
            }
        }

        private fun observeDefaultCurrency() {
            viewModelScope.launch {
                currencyPreferenceRepository
                    .observeDefaultCurrency()
                    .catch {
                        // On error, keep default state
                    }.collect { currencyCode ->
                        _uiState.value =
                            _uiState.value.copy(
                                selectedCurrencyCode = currencyCode,
                            )
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
    val errorMessage: String? = null,
    val backupOperation: BackupOperation = BackupOperation.Idle,
    val backupMessage: String? = null,
    val pendingRestoreUri: Uri? = null,
)
