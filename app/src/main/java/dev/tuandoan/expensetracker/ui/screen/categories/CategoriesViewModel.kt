package dev.tuandoan.expensetracker.ui.screen.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.domain.model.CategoryWithCount
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CategoriesUiState())
        val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

        init {
            loadCategories()
        }

        fun onTabSelected(type: TransactionType) {
            _uiState.update { it.copy(selectedTab = type) }
        }

        fun requestDelete(id: Long) {
            _uiState.update { it.copy(pendingDeleteId = id) }
        }

        fun confirmDelete() {
            val id = _uiState.value.pendingDeleteId ?: return
            _uiState.update { it.copy(pendingDeleteId = null) }
            viewModelScope.launch {
                try {
                    categoryRepository.deleteCategory(id)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = e.message ?: "Failed to delete category")
                    }
                }
            }
        }

        fun undoDelete() {
            _uiState.update { it.copy(pendingDeleteId = null) }
        }

        fun createCategory(
            name: String,
            type: TransactionType,
            iconKey: String?,
            colorKey: String?,
        ) {
            viewModelScope.launch {
                try {
                    categoryRepository.createCategory(name, type, iconKey, colorKey)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = e.message ?: "Failed to create category")
                    }
                }
            }
        }

        fun updateCategory(
            id: Long,
            name: String,
            iconKey: String?,
            colorKey: String?,
        ) {
            viewModelScope.launch {
                try {
                    categoryRepository.updateCategory(id, name, iconKey, colorKey)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = e.message ?: "Failed to update category")
                    }
                }
            }
        }

        fun onErrorDismissed() {
            _uiState.update { it.copy(error = null) }
        }

        private fun loadCategories() {
            viewModelScope.launch {
                categoryRepository
                    .getCategoriesWithTransactionCount()
                    .catch { e ->
                        _uiState.update { it.copy(error = e.message, isLoading = false) }
                    }.collect { categoriesWithCount ->
                        val expense =
                            categoriesWithCount.filter {
                                it.category.type == TransactionType.EXPENSE
                            }
                        val income =
                            categoriesWithCount.filter {
                                it.category.type == TransactionType.INCOME
                            }
                        _uiState.update {
                            it.copy(
                                expenseCategories = expense,
                                incomeCategories = income,
                                isLoading = false,
                            )
                        }
                    }
            }
        }
    }

data class CategoriesUiState(
    val expenseCategories: List<CategoryWithCount> = emptyList(),
    val incomeCategories: List<CategoryWithCount> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedTab: TransactionType = TransactionType.EXPENSE,
    val pendingDeleteId: Long? = null,
) {
    val visibleExpenseCategories: List<CategoryWithCount>
        get() = expenseCategories.filter { it.category.id != pendingDeleteId }

    val visibleIncomeCategories: List<CategoryWithCount>
        get() = incomeCategories.filter { it.category.id != pendingDeleteId }
}
