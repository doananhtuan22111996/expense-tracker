package dev.tuandoan.expensetracker.ui.screen.settings

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.data.backup.BackupValidationException
import dev.tuandoan.expensetracker.domain.model.CurrencyDefinition
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.BackupRepository
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
        private val backupRepository: BackupRepository,
        private val contentResolver: ContentResolver,
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
                    val json = backupRepository.exportBackupJson()
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Cannot open output stream")
                    _uiState.value =
                        _uiState.value.copy(
                            backupOperation = BackupOperation.Idle,
                            backupMessage = "Backup exported successfully",
                        )
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            backupOperation = BackupOperation.Idle,
                            errorMessage = "Export failed: ${e.message ?: "Unknown error"}",
                        )
                }
            }
        }

        fun importBackup(uri: Uri) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(backupOperation = BackupOperation.Importing)
                try {
                    val fileSize = getFileSize(uri)
                    if (fileSize > MAX_IMPORT_FILE_SIZE) {
                        throw IllegalArgumentException(
                            "File too large (${fileSize / 1024 / 1024} MB). Maximum is ${MAX_IMPORT_FILE_SIZE / 1024 / 1024} MB.",
                        )
                    }

                    val json =
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.bufferedReader(Charsets.UTF_8).readText()
                        } ?: throw IllegalStateException("Cannot open input stream")

                    val result = backupRepository.importBackupJson(json)

                    _uiState.value =
                        _uiState.value.copy(
                            backupOperation = BackupOperation.Idle,
                            backupMessage =
                                "Backup restored: ${result.categoryCount} categories, " +
                                    "${result.transactionCount} transactions",
                        )
                } catch (e: BackupValidationException) {
                    _uiState.value =
                        _uiState.value.copy(
                            backupOperation = BackupOperation.Idle,
                            errorMessage = "Backup validation failed with ${e.errors.size} error(s)",
                        )
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            backupOperation = BackupOperation.Idle,
                            errorMessage = "Import failed: ${e.message ?: "Unknown error"}",
                        )
                }
            }
        }

        private fun getFileSize(uri: Uri): Long {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst()) {
                    return cursor.getLong(sizeIndex)
                }
            }
            return 0L
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }

        fun clearBackupMessage() {
            _uiState.value = _uiState.value.copy(backupMessage = null)
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

        companion object {
            const val MAX_IMPORT_FILE_SIZE = 50L * 1024L * 1024L // 50 MB
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
)
