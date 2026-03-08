package dev.tuandoan.expensetracker.ui.screen.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.domain.model.RecurringTransaction
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringTransactionsViewModel
    @Inject
    constructor(
        private val recurringTransactionRepository: RecurringTransactionRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RecurringTransactionsUiState())
        val uiState: StateFlow<RecurringTransactionsUiState> = _uiState.asStateFlow()

        init {
            observeRecurringTransactions()
            processDue()
        }

        fun requestDelete(id: Long) {
            val previousPendingId = _uiState.value.pendingDeleteId
            _uiState.update { it.copy(pendingDeleteId = id) }
            if (previousPendingId != null) {
                performDelete(previousPendingId)
            }
        }

        fun confirmDelete() {
            val id = _uiState.value.pendingDeleteId ?: return
            _uiState.update { it.copy(pendingDeleteId = null) }
            performDelete(id)
        }

        fun undoDelete() {
            _uiState.update { it.copy(pendingDeleteId = null) }
        }

        private fun performDelete(id: Long) {
            viewModelScope.launch {
                try {
                    recurringTransactionRepository.delete(id)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = ErrorUtils.getErrorMessage(e))
                    }
                }
            }
        }

        fun toggleActive(
            id: Long,
            active: Boolean,
        ) {
            viewModelScope.launch {
                try {
                    recurringTransactionRepository.setActive(id, active)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = ErrorUtils.getErrorMessage(e))
                    }
                }
            }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun clearMessage() {
            _uiState.update { it.copy(message = null) }
        }

        private fun observeRecurringTransactions() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                recurringTransactionRepository
                    .observeAll()
                    .catch { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = ErrorUtils.getErrorMessage(e),
                            )
                        }
                    }.collect { items ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                recurringTransactions = items,
                            )
                        }
                    }
            }
        }

        private fun processDue() {
            viewModelScope.launch {
                @Suppress("TooGenericExceptionCaught")
                try {
                    recurringTransactionRepository.processDueRecurring()
                } catch (_: Exception) {
                    // Silent failure for background processing
                }
            }
        }
    }

data class RecurringTransactionsUiState(
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val pendingDeleteId: Long? = null,
) {
    val visibleRecurringTransactions: List<RecurringTransaction>
        get() = recurringTransactions.filter { it.id != pendingDeleteId }
}
