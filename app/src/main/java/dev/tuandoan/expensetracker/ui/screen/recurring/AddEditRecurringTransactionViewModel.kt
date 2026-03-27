package dev.tuandoan.expensetracker.ui.screen.recurring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency
import dev.tuandoan.expensetracker.domain.model.RecurringTransaction
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AddEditRecurringTransactionViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val recurringTransactionRepository: RecurringTransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
        private val timeProvider: TimeProvider,
        private val zoneId: ZoneId,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AddEditRecurringUiState())
        val uiState: StateFlow<AddEditRecurringUiState> = _uiState.asStateFlow()

        private var categoryLoadingJob: Job? = null

        private val recurringId: Long = savedStateHandle.get<Long>("recurringId") ?: 0L
        val isEditMode: Boolean = recurringId != 0L

        init {
            if (isEditMode) {
                loadExisting(recurringId)
            } else {
                loadDefaults()
            }
        }

        fun onTypeChanged(type: TransactionType) {
            _uiState.update {
                it.copy(type = type, selectedCategory = null)
            }
            loadCategories(type)
        }

        fun onAmountChanged(amountText: String) {
            _uiState.update { it.copy(amountText = amountText) }
        }

        fun onCurrencyChanged(currencyCode: String) {
            val state = _uiState.value
            if (currencyCode == state.currencyCode) return
            if (SupportedCurrencies.byCode(currencyCode) == null) return

            // amountText is raw digits — no reformatting needed on currency change
            _uiState.update { state.copy(currencyCode = currencyCode) }
        }

        fun onCategorySelected(category: Category) {
            _uiState.update { it.copy(selectedCategory = category) }
        }

        fun onFrequencyChanged(frequency: RecurrenceFrequency) {
            _uiState.update { it.copy(frequency = frequency) }
        }

        fun onStartDateSelected(timestamp: Long) {
            _uiState.update { it.copy(startDate = timestamp) }
        }

        fun onNoteChanged(note: String) {
            _uiState.update { it.copy(note = note) }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun save(onSuccess: () -> Unit) {
            val state = _uiState.value
            val amount = AmountFormatter.parseAmount(state.amountText)
            if (amount == null || amount <= 0) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
                return
            }
            val category = state.selectedCategory
            if (category == null) {
                _uiState.update { it.copy(errorMessage = "Please select a category") }
                return
            }

            _uiState.update { it.copy(isSaving = true) }

            val zdt = Instant.ofEpochMilli(state.startDate).atZone(zoneId)

            viewModelScope.launch {
                try {
                    val recurring =
                        RecurringTransaction(
                            id = if (isEditMode) recurringId else 0L,
                            type = state.type,
                            amount = amount,
                            currencyCode = state.currencyCode,
                            categoryId = category.id,
                            note = state.note.ifBlank { null },
                            frequency = state.frequency,
                            dayOfMonth = zdt.dayOfMonth,
                            dayOfWeek = zdt.dayOfWeek.value,
                            nextDueMillis = state.startDate,
                            isActive = state.isActive,
                        )
                    if (isEditMode) {
                        recurringTransactionRepository.update(recurring)
                    } else {
                        recurringTransactionRepository.create(recurring)
                    }
                    onSuccess()
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                    }
                }
            }
        }

        private fun loadExisting(id: Long) {
            viewModelScope.launch {
                try {
                    val existing = recurringTransactionRepository.getById(id)
                    if (existing == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Recurring transaction not found",
                            )
                        }
                        return@launch
                    }

                    val amountText = existing.amount.toString()

                    // Fetch categories synchronously with first() instead of
                    // starting an infinite collect that would block on join()
                    val categories = categoryRepository.observeCategories(existing.type).first()
                    val selectedCategory = categories.find { it.id == existing.categoryId }

                    _uiState.update {
                        it.copy(
                            type = existing.type,
                            amountText = amountText,
                            currencyCode = existing.currencyCode,
                            frequency = existing.frequency,
                            startDate = existing.nextDueMillis,
                            note = existing.note ?: "",
                            isActive = existing.isActive,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            isLoading = false,
                        )
                    }

                    // Snapshot original state for hasUnsavedChanges comparison
                    _uiState.update {
                        it.copy(
                            originalAmountText = amountText,
                            originalNote = existing.note ?: "",
                            originalCategoryId = existing.categoryId,
                            originalFrequency = existing.frequency,
                            originalStartDate = existing.nextDueMillis,
                            originalCurrencyCode = existing.currencyCode,
                            originalType = existing.type,
                        )
                    }

                    // Start observing categories for live updates
                    loadCategories(existing.type)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                    }
                }
            }
        }

        private fun loadDefaults() {
            viewModelScope.launch {
                val defaultCurrency = currencyPreferenceRepository.getDefaultCurrency()
                _uiState.update {
                    it.copy(
                        currencyCode = defaultCurrency,
                        startDate = timeProvider.currentTimeMillis(),
                    )
                }
                loadCategories(TransactionType.EXPENSE)
            }
        }

        private fun loadCategories(type: TransactionType) {
            categoryLoadingJob?.cancel()
            categoryLoadingJob =
                viewModelScope.launch {
                    try {
                        categoryRepository.observeCategories(type).collect { categories ->
                            _uiState.update {
                                it.copy(
                                    categories = categories,
                                    isLoading = false,
                                )
                            }
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = ErrorUtils.getErrorMessage(e),
                                )
                            }
                        }
                    }
                }
        }

        override fun onCleared() {
            super.onCleared()
            categoryLoadingJob?.cancel()
        }
    }

data class AddEditRecurringUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val currencyCode: String = SupportedCurrencies.default().code,
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val startDate: Long = System.currentTimeMillis(),
    val note: String = "",
    val isActive: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    // Original state for edit mode hasUnsavedChanges comparison
    val originalAmountText: String? = null,
    val originalNote: String? = null,
    val originalCategoryId: Long? = null,
    val originalFrequency: RecurrenceFrequency? = null,
    val originalStartDate: Long? = null,
    val originalCurrencyCode: String? = null,
    val originalType: TransactionType? = null,
) {
    val isFormValid: Boolean
        get() =
            amountText.isNotBlank() &&
                selectedCategory != null &&
                AmountFormatter.parseAmount(amountText)?.let { it > 0 } == true

    val hasUnsavedChanges: Boolean
        get() {
            // In edit mode: compare against original loaded state
            if (originalAmountText != null) {
                return amountText != originalAmountText ||
                    (note.ifBlank { "" }) != (originalNote ?: "") ||
                    selectedCategory?.id != originalCategoryId ||
                    frequency != originalFrequency ||
                    startDate != originalStartDate ||
                    currencyCode != originalCurrencyCode ||
                    type != originalType
            }
            // In add mode: any user input counts as changes
            return amountText.isNotBlank() || note.isNotBlank() || selectedCategory != null
        }
}
