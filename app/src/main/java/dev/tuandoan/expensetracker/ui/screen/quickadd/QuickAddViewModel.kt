package dev.tuandoan.expensetracker.ui.screen.quickadd

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.domain.analytics.Analytics
import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent
import dev.tuandoan.expensetracker.domain.analytics.toAnalyticsKind
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.BudgetAlertScheduler
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel backing the quick-add bottom sheet (T3.2).
 *
 * Responsibilities are intentionally narrow compared with
 * [dev.tuandoan.expensetracker.ui.screen.addedit.AddEditTransactionViewModel]:
 * quick-add only ever adds an EXPENSE transaction in the user's default
 * currency for the pinned category identified by `categoryId`. No edit
 * mode, no category picker, no currency picker, no date picker, no note.
 *
 * ### Init flow
 *
 * 1. Read `categoryId` off [SavedStateHandle] (set by the hosting Activity
 *    when it passes the intent extra through). A missing/zero value
 *    finishes the sheet via [QuickAddUiState.categoryMissing] = true.
 * 2. Resolve the default currency from [CurrencyPreferenceRepository] once.
 *    The user cannot change currency on quick-add (PRD FR-11).
 * 3. Look up the [Category] by id; if it no longer exists (the user
 *    deleted it between widget snapshot and tap — FR-07 race), surface
 *    [QuickAddUiState.categoryMissing] and stop.
 *
 * ### Save flow
 *
 * Amount validation uses [AmountFormatter.parseAmount]:
 * - Empty / only non-digits → `null` → error.
 * - `0L` → rejected (PRD FR-17: Save disabled on zero).
 * - Over `Long.MAX_VALUE` / 19+ digits → `null` → error.
 *
 * On success: write via [TransactionRepository.addTransaction], fire the
 * FR-A6 `TransactionAdded` analytics event (same emitter as the full
 * add-edit path — matches the v3.11.0 PR #121 pattern), kick the budget
 * alert scheduler, and flip [QuickAddUiState.saved] so the hosting
 * Activity can dismiss.
 *
 * ### Failure handling
 *
 * Repository exceptions are caught at the ViewModel boundary and mapped
 * to a user-facing [UiText] via [ErrorUtils.getErrorMessage] — matching
 * the add-edit pattern. The coroutine re-throws `CancellationException`
 * implicitly because `catch (Exception)` excludes it.
 */
@HiltViewModel
class QuickAddViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
        private val budgetAlertScheduler: BudgetAlertScheduler,
        private val timeProvider: TimeProvider,
        private val analytics: Analytics,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val categoryId: Long = savedStateHandle.get<Long>(KEY_CATEGORY_ID) ?: 0L

        private val _uiState = MutableStateFlow(QuickAddUiState())
        val uiState: StateFlow<QuickAddUiState> = _uiState.asStateFlow()

        init {
            loadInitialData()
        }

        fun onAmountChanged(amountText: String) {
            _uiState.value = _uiState.value.copy(amountText = amountText, errorMessage = null)
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }

        fun saveTransaction() {
            val state = _uiState.value
            if (state.isSaving) return

            val category = state.category
            if (category == null) {
                // Should only hit this if Save fires during the race window
                // between init and categoryMissing surfacing; belt-and-braces
                // so the ViewModel never tries to write without a category.
                _uiState.value =
                    state.copy(errorMessage = UiText.StringResource(R.string.error_select_category))
                return
            }

            val amount = AmountFormatter.parseAmount(state.amountText)
            if (amount == null || amount <= 0L) {
                _uiState.value =
                    state.copy(errorMessage = UiText.StringResource(R.string.error_invalid_amount))
                return
            }

            _uiState.value = state.copy(isSaving = true, errorMessage = null)

            viewModelScope.launch {
                try {
                    transactionRepository.addTransaction(
                        type = TransactionType.EXPENSE,
                        amount = amount,
                        categoryId = category.id,
                        note = null,
                        timestamp = timeProvider.currentTimeMillis(),
                        currencyCode = state.currencyCode,
                    )
                    analytics.logEvent(
                        AnalyticsEvent.TransactionAdded(type = TransactionType.EXPENSE.toAnalyticsKind()),
                    )
                    budgetAlertScheduler.scheduleImmediateCheck()
                    _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        private fun loadInitialData() {
            if (categoryId <= 0L) {
                _uiState.value = _uiState.value.copy(categoryMissing = true)
                return
            }
            viewModelScope.launch {
                val currencyCode = currencyPreferenceRepository.getDefaultCurrency()
                val category = categoryRepository.getCategory(categoryId)
                _uiState.value =
                    if (category == null) {
                        _uiState.value.copy(currencyCode = currencyCode, categoryMissing = true)
                    } else {
                        _uiState.value.copy(currencyCode = currencyCode, category = category)
                    }
            }
        }

        companion object {
            /**
             * Key used by [SavedStateHandle] to retrieve the pinned category
             * ID. The hosting Activity seeds this off the intent extra so the
             * ViewModel stays Android-Intent-free.
             */
            const val KEY_CATEGORY_ID: String = "category_id"
        }
    }
