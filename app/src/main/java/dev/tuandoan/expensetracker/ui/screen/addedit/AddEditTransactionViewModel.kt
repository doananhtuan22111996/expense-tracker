package dev.tuandoan.expensetracker.ui.screen.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.core.util.DateTimeUtil
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditTransactionViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val transactionId: Long = savedStateHandle.get<String>("transactionId")?.toLongOrNull() ?: 0L
        private val isEditMode = transactionId > 0L

        private val _uiState = MutableStateFlow(AddEditTransactionUiState())
        val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

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
                                updatedAt = DateTimeUtil.getCurrentTimeMillis(),
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
                            errorMessage = "Failed to save transaction: ${e.message}",
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
                                timestamp = DateTimeUtil.getCurrentTimeMillis(),
                            )
                        loadCategories(TransactionType.EXPENSE)
                    }
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load data: ${e.message}",
                        )
                }
            }
        }

        private fun loadCategories(type: TransactionType) {
            viewModelScope.launch {
                try {
                    categoryRepository.observeCategories(type).collect { categories ->
                        _uiState.value =
                            _uiState.value.copy(
                                categories = categories,
                                isLoading = false,
                            )
                    }
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load categories: ${e.message}",
                        )
                }
            }
        }
    }

data class AddEditTransactionUiState(
    val originalTransaction: Transaction? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val timestamp: Long = DateTimeUtil.getCurrentTimeMillis(),
    val note: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isFormValid: Boolean
        get() =
            amountText.isNotBlank() &&
                selectedCategory != null &&
                AmountFormatter.parseAmount(amountText)?.let { it > 0 } == true
}
