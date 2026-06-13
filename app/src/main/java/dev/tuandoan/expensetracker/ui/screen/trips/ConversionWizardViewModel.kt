package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.ConversionDraft
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

enum class WizardStep { Loading, Metadata, RowDecisions, Preview }

data class ConversionWizardUiState(
    val step: WizardStep = WizardStep.Loading,
    val done: Boolean = false,
    // Source category
    val sourceCategoryId: Long = 0L,
    val sourceCategoryName: String = "",
    val sourceCategoryIconKey: String? = null,
    val sourceCategoryColorKey: String? = null,
    // Transactions belonging to the source category
    val transactions: List<Transaction> = emptyList(),
    // Available EXPENSE categories for re-categorisation in Step 2
    val availableCategories: List<Category> = emptyList(),
    // Step 1 — Trip metadata
    val tripName: String = "",
    val tripDestination: String = "",
    val startEpochDay: Long? = null,
    val endEpochDay: Long? = null,
    val isForeignCurrency: Boolean = false,
    val foreignCurrencyCode: String = "",
    val rateText: String = "",
    val homeCurrencyCode: String = SupportedCurrencies.default().code,
    // Step 2 — Row decisions keyed by transactionId
    val rowDecisions: Map<Long, ConversionDraft.RowDecision> = emptyMap(),
    // Validation errors (Step 1)
    val nameError: UiText? = null,
    val dateError: UiText? = null,
    val rateError: UiText? = null,
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
) {
    /** Auto-derives KEEP when any row is skipped; DELETE otherwise. */
    val sourceDisposition: ConversionDraft.SourceDisposition
        get() =
            if (rowDecisions.values.any { it is ConversionDraft.RowDecision.Skip }) {
                ConversionDraft.SourceDisposition.KEEP
            } else {
                ConversionDraft.SourceDisposition.DELETE
            }

    val migratedCount: Int get() = rowDecisions.values.count { it is ConversionDraft.RowDecision.Migrate }
    val skippedCount: Int get() = rowDecisions.values.count { it is ConversionDraft.RowDecision.Skip }
}

/**
 * Backs [ConversionWizardScreen] (v3.13.0, T4.2). Three-step wizard:
 * Step 1 — Trip metadata, Step 2 — Per-row decisions, Step 3 — Preview + Commit.
 *
 * Zero DB writes until [commit] is called (T4.6 wires the actual commit path).
 * All state is held in-memory; the wizard is abortable at any point.
 */
@HiltViewModel
class ConversionWizardViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val categoryRepository: CategoryRepository,
        private val transactionRepository: TransactionRepository,
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
        clock: Clock,
    ) : ViewModel() {
        private val categoryId: Long = savedStateHandle["categoryId"] ?: 0L

        private val today: Long = LocalDate.now(clock).toEpochDay()

        private val _uiState = MutableStateFlow(ConversionWizardUiState())
        val uiState: StateFlow<ConversionWizardUiState> = _uiState.asStateFlow()

        init {
            loadSource()
        }

        // ── Step navigation ──────────────────────────────────────────────────────

        fun onNext() {
            when (_uiState.value.step) {
                WizardStep.Metadata -> advanceFromMetadata()
                WizardStep.RowDecisions -> _uiState.update { it.copy(step = WizardStep.Preview) }
                WizardStep.Preview -> commit()
                WizardStep.Loading -> Unit
            }
        }

        fun onBack() {
            when (_uiState.value.step) {
                WizardStep.RowDecisions -> _uiState.update { it.copy(step = WizardStep.Metadata) }
                WizardStep.Preview -> _uiState.update { it.copy(step = WizardStep.RowDecisions) }
                else -> _uiState.update { it.copy(done = true) }
            }
        }

        // ── Step 1 — metadata inputs ─────────────────────────────────────────────

        fun onTripNameChange(value: String) {
            _uiState.update { it.copy(tripName = value, nameError = null) }
        }

        fun onTripDestinationChange(value: String) {
            _uiState.update { it.copy(tripDestination = value) }
        }

        fun onDatesSelected(
            startEpochDay: Long,
            endEpochDay: Long,
        ) {
            _uiState.update {
                it.copy(
                    startEpochDay = startEpochDay,
                    endEpochDay = endEpochDay,
                    dateError = null,
                )
            }
        }

        fun onForeignCurrencyToggle(enabled: Boolean) {
            _uiState.update {
                if (enabled) {
                    it.copy(isForeignCurrency = true)
                } else {
                    it.copy(isForeignCurrency = false, foreignCurrencyCode = "", rateText = "", rateError = null)
                }
            }
        }

        fun onForeignCurrencyChange(code: String) {
            if (SupportedCurrencies.byCode(code) == null) return
            _uiState.update { it.copy(foreignCurrencyCode = code) }
        }

        fun onRateTextChange(value: String) {
            val normalised = value.replace(',', '.')
            _uiState.update { it.copy(rateText = normalised, rateError = null) }
        }

        // ── Step 2 — row decisions ───────────────────────────────────────────────

        fun onDecisionChanged(
            transactionId: Long,
            decision: ConversionDraft.RowDecision,
        ) {
            _uiState.update {
                it.copy(rowDecisions = it.rowDecisions + (transactionId to decision))
            }
        }

        // ── Commit (stub — T4.6 wires the actual DB write) ───────────────────────

        fun commit() {
            // T4.6 will replace this with the real commitConversion call.
            // For now, mark done so the screen pops back.
            _uiState.update { it.copy(done = true) }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        /** Builds the commit payload from current state. Used by T4.6 and tests. */
        fun buildDraft(): ConversionDraft? {
            val state = _uiState.value
            val start = state.startEpochDay ?: return null
            val end = state.endEpochDay ?: return null
            return ConversionDraft(
                sourceCategoryId = state.sourceCategoryId,
                sourceCategorySnapshot =
                    ConversionDraft.CategorySnapshot(
                        name = state.sourceCategoryName,
                        iconKey = state.sourceCategoryIconKey,
                        colorKey = state.sourceCategoryColorKey,
                    ),
                tripMetadata =
                    ConversionDraft.TripMetadata(
                        name = state.tripName.trim(),
                        destination = state.tripDestination.trim().ifBlank { null },
                        startDateEpochDay = start,
                        endDateEpochDay = end,
                        foreignCurrencyCode =
                            if (state.isForeignCurrency) {
                                state.foreignCurrencyCode.ifBlank {
                                    null
                                }
                            } else {
                                null
                            },
                        foreignToHomeRate = if (state.isForeignCurrency) state.rateText.toDoubleOrNull() else null,
                    ),
                rowDecisions = state.rowDecisions.values.toList(),
                sourceDisposition = state.sourceDisposition,
            )
        }

        // ── Private helpers ──────────────────────────────────────────────────────

        private fun loadSource() {
            if (categoryId <= 0L) {
                _uiState.update { it.copy(isLoading = false, done = true) }
                return
            }
            viewModelScope.launch {
                try {
                    val category = categoryRepository.getCategory(categoryId)
                    if (category == null || category.type != TransactionType.EXPENSE) {
                        _uiState.update { it.copy(isLoading = false, done = true) }
                        return@launch
                    }

                    // Load all EXPENSE categories for Step 2 re-categorise picker
                    val allExpenseCategories =
                        categoryRepository
                            .observeCategories(TransactionType.EXPENSE)
                            .first()

                    // Load transactions via searchTransactionsAdvanced (categoryId filter)
                    val transactions =
                        transactionRepository
                            .searchTransactionsAdvanced(
                                from = null,
                                to = null,
                                query = "",
                                filterType = TransactionType.EXPENSE,
                                categoryId = categoryId,
                            ).first()

                    val home =
                        runCatching { currencyPreferenceRepository.getDefaultCurrency() }
                            .getOrDefault(SupportedCurrencies.default().code)

                    // Seed all rows as Migrate to the first available non-source category,
                    // falling back to source itself if no other category exists.
                    val defaultNewCategoryId =
                        allExpenseCategories
                            .firstOrNull { it.id != categoryId }
                            ?.id
                            ?: categoryId
                    val initialDecisions =
                        transactions.associate { tx ->
                            tx.id to
                                ConversionDraft.RowDecision.Migrate(
                                    transactionId = tx.id,
                                    newCategoryId = defaultNewCategoryId,
                                )
                        }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = WizardStep.Metadata,
                            sourceCategoryId = category.id,
                            sourceCategoryName = category.name,
                            sourceCategoryIconKey = category.iconKey,
                            sourceCategoryColorKey = category.colorKey,
                            transactions = transactions,
                            availableCategories = allExpenseCategories.filter { c -> c.id != categoryId },
                            rowDecisions = initialDecisions,
                            homeCurrencyCode = home,
                            tripName = category.name,
                            startEpochDay = today,
                            endEpochDay = today,
                        )
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResource(R.string.error_load_category),
                        )
                    }
                }
            }
        }

        private fun advanceFromMetadata() {
            val state = _uiState.value
            var nameError: UiText? = null
            var dateError: UiText? = null
            var rateError: UiText? = null

            if (state.tripName.isBlank()) {
                nameError = UiText.StringResource(R.string.error_trip_name_required)
            }

            val start = state.startEpochDay
            val end = state.endEpochDay
            if (start == null || end == null) {
                dateError = UiText.StringResource(R.string.error_trip_dates_required)
            } else if (end < start) {
                dateError = UiText.StringResource(R.string.error_trip_end_before_start)
            }

            if (state.isForeignCurrency) {
                val rate = state.rateText.toDoubleOrNull()
                if (rate == null || rate <= 0.0) {
                    rateError = UiText.StringResource(R.string.error_trip_rate_invalid)
                }
            }

            if (nameError != null || dateError != null || rateError != null) {
                _uiState.update {
                    it.copy(nameError = nameError, dateError = dateError, rateError = rateError)
                }
                return
            }

            _uiState.update {
                it.copy(
                    nameError = null,
                    dateError = null,
                    rateError = null,
                    step = WizardStep.RowDecisions,
                )
            }
        }
    }
