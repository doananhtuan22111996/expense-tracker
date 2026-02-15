package dev.tuandoan.expensetracker.ui.screen.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditTransactionViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val timeProvider: TimeProvider,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: 0L
        private val isEditMode = transactionId > 0L

        private val _uiState = MutableStateFlow(AddEditTransactionUiState())
        val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

        // Job to track and cancel ongoing category loading operations
        private var categoryLoadingJob: Job? = null

        init {
            loadInitialData()
        }

        fun onTypeChanged(type: TransactionType) {
            _uiState.value =
                _uiState.value.copy(
                    type = type,
                    selectedCategory = null, // Reset category when type changes
                )
            loadCategories(type)
        }

        fun onAmountChanged(amountText: String) {
            _uiState.value = _uiState.value.copy(amountText = amountText)
        }

        fun onCategorySelected(category: Category) {
            _uiState.value = _uiState.value.copy(selectedCategory = category)
        }

        fun onDateSelected(timestamp: Long) {
            _uiState.value = _uiState.value.copy(timestamp = timestamp)
        }

        fun onNoteChanged(note: String) {
            _uiState.value = _uiState.value.copy(note = note)
        }

        fun onBackPressed() {
            val state = _uiState.value
            if (isEditMode && state.hasUnsavedChanges) {
                _uiState.value = state.copy(showDiscardDialog = true)
            } else {
                // No changes or not in edit mode - navigate back immediately
                // This will be handled by the UI
            }
        }

        fun onDiscardChanges() {
            _uiState.value = _uiState.value.copy(showDiscardDialog = false)
            // Navigation will be handled by UI
        }

        fun onCancelDiscard() {
            _uiState.value = _uiState.value.copy(showDiscardDialog = false)
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }

        fun saveTransaction(onSuccess: () -> Unit) {
            val state = _uiState.value

            // Validate input
            val amount = AmountFormatter.parseAmount(state.amountText)
            if (amount == null || amount <= 0) {
                _uiState.value = state.copy(errorMessage = "Please enter a valid amount")
                return
            }

            val category = state.selectedCategory
            if (category == null) {
                _uiState.value = state.copy(errorMessage = "Please select a category")
                return
            }

            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            viewModelScope.launch {
                try {
                    if (isEditMode) {
                        // Update existing transaction
                        val updatedTransaction =
                            state.originalTransaction!!.copy(
                                type = state.type,
                                amount = amount,
                                category = category,
                                note = state.note.ifBlank { null },
                                timestamp = state.timestamp,
                                updatedAt = timeProvider.currentTimeMillis(),
                            )
                        transactionRepository.updateTransaction(updatedTransaction)
                    } else {
                        // Create new transaction
                        transactionRepository.addTransaction(
                            type = state.type,
                            amount = amount,
                            categoryId = category.id,
                            note = state.note.ifBlank { null },
                            timestamp = state.timestamp,
                        )
                    }
                    onSuccess()
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        private fun loadInitialData() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)

                try {
                    if (isEditMode) {
                        // Load existing transaction
                        val transaction = transactionRepository.getTransaction(transactionId)
                        if (transaction != null) {
                            _uiState.value =
                                _uiState.value.copy(
                                    originalTransaction = transaction,
                                    type = transaction.type,
                                    amountText = AmountFormatter.formatAmount(transaction.amount),
                                    selectedCategory = transaction.category,
                                    timestamp = transaction.timestamp,
                                    note = transaction.note ?: "",
                                )
                            loadCategories(transaction.type)
                        } else {
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Transaction not found",
                                )
                            return@launch
                        }
                    } else {
                        // New transaction - set defaults
                        _uiState.value =
                            _uiState.value.copy(
                                type = TransactionType.EXPENSE,
                                timestamp = timeProvider.currentTimeMillis(),
                            )
                        loadCategories(TransactionType.EXPENSE)
                    }
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        private fun loadCategories(type: TransactionType) {
            // Cancel any ongoing category loading operation to prevent resource leaks
            categoryLoadingJob?.cancel()

            categoryLoadingJob =
                viewModelScope.launch {
                    try {
                        categoryRepository.observeCategories(type).collect { categories ->
                            _uiState.value =
                                _uiState.value.copy(
                                    categories = categories,
                                    isLoading = false,
                                    errorMessage = null, // Clear any previous errors
                                )
                        }
                    } catch (e: Exception) {
                        // Only update error state if this job hasn't been cancelled
                        if (isActive) {
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = ErrorUtils.getErrorMessage(e),
                                )
                        }
                    }
                }
        }

        override fun onCleared() {
            super.onCleared()
            // Ensure category loading job is cancelled when ViewModel is destroyed
            categoryLoadingJob?.cancel()
        }
    }

data class AddEditTransactionUiState(
    val originalTransaction: Transaction? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showDiscardDialog: Boolean = false,
) {
    val isFormValid: Boolean
        get() =
            amountText.isNotBlank() &&
                selectedCategory != null &&
                AmountFormatter.parseAmount(amountText)?.let { it > 0 } == true

    val hasUnsavedChanges: Boolean
        get() {
            val original = originalTransaction ?: return false

            // Compare current state with original transaction
            val currentAmount = AmountFormatter.parseAmount(amountText) ?: 0L
            val currentNote = note.ifBlank { null }

            return type != original.type ||
                currentAmount != original.amount ||
                selectedCategory?.id != original.category.id ||
                timestamp != original.timestamp ||
                currentNote != original.note
        }

    val isSaveEnabled: Boolean
        get() {
            if (!isFormValid) return false

            // For edit mode, also check if there are changes
            return originalTransaction == null || hasUnsavedChanges
        }
}
