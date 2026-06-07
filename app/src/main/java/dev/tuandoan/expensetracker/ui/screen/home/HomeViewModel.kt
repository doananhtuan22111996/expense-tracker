package dev.tuandoan.expensetracker.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.util.DateRangeCalculator
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferences
import dev.tuandoan.expensetracker.data.preferences.HomeBannerPreferences
import dev.tuandoan.expensetracker.data.preferences.OnboardingRepository
import dev.tuandoan.expensetracker.data.preferences.WidgetCategoryPreferences
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.SearchScope
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.SearchFilterPreferences
import dev.tuandoan.expensetracker.domain.repository.SelectedMonthRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val selectedMonthRepository: SelectedMonthRepository,
        private val categoryRepository: CategoryRepository,
        private val dateRangeCalculator: DateRangeCalculator,
        private val searchFilterPreferences: SearchFilterPreferences,
        private val analyticsPreferences: AnalyticsPreferences,
        private val onboardingRepository: OnboardingRepository,
        private val homeBannerPreferences: HomeBannerPreferences,
        private val widgetCategoryPreferences: WidgetCategoryPreferences,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
        private val searchQueryFlow = MutableStateFlow("")
        private val filterFlow = MutableStateFlow<TransactionType?>(null)
        private val searchScopeFlow = MutableStateFlow(SearchScope.CURRENT_MONTH)
        private val selectedCategoryIdFlow = MutableStateFlow<Long?>(null)
        private val retryTrigger = MutableStateFlow(0)

        val expenseCategories: StateFlow<List<Category>> =
            categoryRepository
                .observeCategories(TransactionType.EXPENSE)
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        val incomeCategories: StateFlow<List<Category>> =
            categoryRepository
                .observeCategories(TransactionType.INCOME)
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        /**
         * Drives the one-shot post-onboarding consent dialog (v3.11.0,
         * ADR-008 + ADR-011). The variant depends on which prompt(s) the
         * user has already resolved:
         *
         * - [ConsentPromptVariant.None] — onboarding not finished yet, OR
         *   both `*PromptShown` flags already `true`. No dialog.
         * - [ConsentPromptVariant.Main] — onboarding done and the Crashlytics
         *   prompt has never been shown. Covers fresh installs (both
         *   `*PromptShown` false); the main dual-checkbox dialog asks both
         *   questions at once.
         * - [ConsentPromptVariant.AnalyticsOnly] — onboarding done, the
         *   Crashlytics prompt was already resolved in a pre-bundle v3.11.0
         *   preview (`consentPromptShown = true`), but the Analytics
         *   question is new (`analyticsEventsPromptShown = false`). ADR-011
         *   says preserve prior consent, ask only the new question.
         *
         * On cold start, `isOnboardingComplete` emits `false` first (default
         * before DataStore resolves), then its persisted value — so this
         * flow naturally reads `None` during the loading race and flips
         * only once all prefs are settled. No extra `LaunchedEffect` gate.
         */
        val consentPromptVariant: StateFlow<ConsentPromptVariant> =
            combine(
                analyticsPreferences.consentPromptShown,
                analyticsPreferences.analyticsEventsPromptShown,
                onboardingRepository.isOnboardingComplete,
            ) { crashShown, analyticsShown, onboardingDone ->
                when {
                    !onboardingDone -> ConsentPromptVariant.None
                    !crashShown -> ConsentPromptVariant.Main
                    !analyticsShown -> ConsentPromptVariant.AnalyticsOnly
                    else -> ConsentPromptVariant.None
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                ConsentPromptVariant.None,
            )

        /**
         * v3.12.0 one-shot announcement: shown on the Home screen when
         * (a) the user has not yet dismissed it AND (b) no widget categories
         * are pinned yet. Auto-hiding once the user pins anything from
         * Settings → Widget Categories means a freshly-configured user does
         * not see a stale "Set it up" CTA — even if they never explicitly
         * tapped Dismiss. Tapping the CTA also persists the dismissal so
         * returning to Home after configuring does not flash the banner.
         */
        val showWidgetQuickAddBanner: StateFlow<Boolean> =
            combine(
                homeBannerPreferences.widgetQuickAddBannerDismissed,
                widgetCategoryPreferences.pinnedCategoryIds,
            ) { dismissed, pinnedIds ->
                !dismissed && pinnedIds.isEmpty()
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                false,
            )

        /**
         * Persist dismissal of the v3.12.0 widget-quick-add banner. Called
         * from both the explicit Dismiss action and the CTA tap so that
         * either path retires the banner permanently.
         */
        fun dismissWidgetQuickAddBanner() {
            viewModelScope.launch {
                homeBannerPreferences.setWidgetQuickAddBannerDismissed()
            }
        }

        /**
         * Called when the main dual-checkbox dialog dismisses — either via
         * the "Done" button or a back-gesture / outside-tap (ADR-008:
         * ambiguous dismissal resolves to the privacy-safe default, which
         * is whatever the user currently has checked).
         *
         * Writes all four keys: the two `*Consent` booleans from the caller
         * PLUS marks both `*PromptShown` flags `true` so the dialog does
         * not re-appear.
         */
        fun onConsentsResolved(
            crashAccepted: Boolean,
            analyticsAccepted: Boolean,
        ) {
            viewModelScope.launch {
                analyticsPreferences.setAnalyticsConsent(crashAccepted)
                analyticsPreferences.setAnalyticsEventsConsent(analyticsAccepted)
                analyticsPreferences.setConsentPromptShown(true)
                analyticsPreferences.setAnalyticsEventsPromptShown(true)
            }
        }

        /**
         * Called when the Analytics-only upgrade dialog dismisses. Touches
         * only the Analytics pair — the prior Crashlytics choice
         * (`analyticsConsent`) is intentionally not re-written, preserving
         * whatever the user answered pre-bundle. This is the privacy
         * invariant ADR-011 pins: upgrading a beta tester must not silently
         * re-ask or reset their prior consent.
         */
        fun onAnalyticsOnlyResolved(analyticsAccepted: Boolean) {
            viewModelScope.launch {
                analyticsPreferences.setAnalyticsEventsConsent(analyticsAccepted)
                analyticsPreferences.setAnalyticsEventsPromptShown(true)
            }
        }

        init {
            viewModelScope.launch {
                val scope = searchFilterPreferences.searchScope.first()
                val type = searchFilterPreferences.filterType.first()
                val catId = searchFilterPreferences.categoryId.first()
                val startDay = searchFilterPreferences.dateRangeStartEpochDay.first()
                val endDay = searchFilterPreferences.dateRangeEndEpochDay.first()

                _uiState.value =
                    _uiState.value.copy(
                        searchScope = scope,
                        filter = type,
                        selectedCategoryId = catId,
                        dateRangeStart = startDay?.let { LocalDate.ofEpochDay(it) },
                        dateRangeEnd = endDay?.let { LocalDate.ofEpochDay(it) },
                    )
                searchScopeFlow.value = scope
                filterFlow.value = type
                selectedCategoryIdFlow.value = catId

                if (catId != null) {
                    val category = categoryRepository.getCategory(catId)
                    if (category != null) {
                        _uiState.value = _uiState.value.copy(selectedCategoryName = category.name)
                    } else {
                        selectedCategoryIdFlow.value = null
                        _uiState.value = _uiState.value.copy(selectedCategoryId = null)
                        searchFilterPreferences.setCategoryId(null)
                    }
                }
            }

            @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
            viewModelScope.launch {
                combine(
                    selectedMonthRepository.selectedMonth,
                    searchQueryFlow.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(),
                    filterFlow,
                    searchScopeFlow,
                    selectedCategoryIdFlow,
                ) { month, query, filter, scope, categoryId ->
                    FilterParams(month, query.trim(), filter, scope, categoryId)
                }.combine(retryTrigger) { params, _ -> params }
                    .flatMapLatest { params ->
                        val currentState = _uiState.value
                        _uiState.value =
                            currentState.copy(
                                isLoading = true,
                                isError = false,
                                monthLabel = dateRangeCalculator.displayLabel(params.month),
                            )

                        val range = dateRangeCalculator.rangeOf(params.month)
                        val dateRangeFrom = currentState.dateRangeStart?.let { dateToMillis(it) }
                        val dateRangeTo = currentState.dateRangeEnd?.let { dateToEndOfDayMillis(it) }
                        val hasDateRange = dateRangeFrom != null
                        val hasAdvancedFilters =
                            params.scope == SearchScope.ALL_MONTHS || params.categoryId != null || hasDateRange

                        val sourceFlow =
                            if (hasAdvancedFilters) {
                                val from =
                                    when {
                                        hasDateRange -> dateRangeFrom
                                        params.scope == SearchScope.ALL_MONTHS -> null
                                        else -> range.startMillis
                                    }
                                val to =
                                    when {
                                        hasDateRange -> dateRangeTo
                                        params.scope == SearchScope.ALL_MONTHS -> null
                                        else -> range.endMillisExclusive
                                    }
                                transactionRepository.searchTransactionsAdvanced(
                                    from = from,
                                    to = to,
                                    query = params.query,
                                    filterType = params.filter,
                                    categoryId = params.categoryId,
                                )
                            } else if (params.query.isEmpty()) {
                                transactionRepository.observeTransactions(
                                    from = range.startMillis,
                                    to = range.endMillisExclusive,
                                    filterType = params.filter,
                                )
                            } else {
                                transactionRepository.searchTransactions(
                                    from = range.startMillis,
                                    to = range.endMillisExclusive,
                                    query = params.query,
                                    filterType = params.filter,
                                )
                            }
                        sourceFlow.catch { e ->
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    isError = true,
                                    errorMessage = ErrorUtils.getErrorMessage(e),
                                )
                        }
                    }.collectLatest { transactions ->
                        _uiState.value =
                            _uiState.value.copy(
                                transactions = transactions,
                                isLoading = false,
                                isError = false,
                            )
                    }
            }
        }

        fun onSearchQueryChanged(query: String) {
            _uiState.value = _uiState.value.copy(searchQuery = query)
            searchQueryFlow.value = query
        }

        fun clearSearch() {
            onSearchQueryChanged("")
        }

        fun onFilterChanged(filter: TransactionType?) {
            _uiState.value = _uiState.value.copy(filter = filter)
            filterFlow.value = filter
            viewModelScope.launch { searchFilterPreferences.setFilterType(filter) }
        }

        fun onSearchScopeChanged(scope: SearchScope) {
            _uiState.value = _uiState.value.copy(searchScope = scope)
            searchScopeFlow.value = scope
            viewModelScope.launch { searchFilterPreferences.setSearchScope(scope) }
        }

        fun onCategorySelected(categoryId: Long?) {
            selectedCategoryIdFlow.value = categoryId
            viewModelScope.launch { searchFilterPreferences.setCategoryId(categoryId) }
            if (categoryId == null) {
                _uiState.value =
                    _uiState.value.copy(
                        selectedCategoryId = null,
                        selectedCategoryName = null,
                    )
            } else {
                _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
                viewModelScope.launch {
                    val category = categoryRepository.getCategory(categoryId)
                    _uiState.value = _uiState.value.copy(selectedCategoryName = category?.name)
                }
            }
        }

        fun onDateRangeSelected(
            startMillis: Long,
            endMillis: Long,
        ) {
            val zone = ZoneId.systemDefault()
            val startDate = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
            val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
            _uiState.value =
                _uiState.value.copy(
                    dateRangeStart = startDate,
                    dateRangeEnd = endDate,
                )
            viewModelScope.launch {
                searchFilterPreferences.setDateRange(startDate.toEpochDay(), endDate.toEpochDay())
            }
            // Date range implies ALL_MONTHS scope
            if (searchScopeFlow.value == SearchScope.ALL_MONTHS) {
                // Scope already ALL_MONTHS — MutableStateFlow deduplicates, so bump retryTrigger
                retryTrigger.value++
            } else {
                onSearchScopeChanged(SearchScope.ALL_MONTHS)
            }
        }

        fun clearDateRange() {
            _uiState.value =
                _uiState.value.copy(
                    dateRangeStart = null,
                    dateRangeEnd = null,
                )
            viewModelScope.launch { searchFilterPreferences.setDateRange(null, null) }
            // Trigger re-query
            retryTrigger.value++
        }

        fun clearAllFilters() {
            onFilterChanged(null)
            onSearchScopeChanged(SearchScope.CURRENT_MONTH)
            onCategorySelected(null)
            clearDateRange()
            clearSearch()
        }

        fun goToPreviousMonth() {
            selectedMonthRepository.goToPreviousMonth()
        }

        fun goToNextMonth() {
            selectedMonthRepository.goToNextMonth()
        }

        fun setMonth(yearMonth: YearMonth) {
            selectedMonthRepository.setMonth(yearMonth)
        }

        fun currentSelectedMonth(): YearMonth = selectedMonthRepository.selectedMonth.value

        fun deleteTransaction(transaction: Transaction) {
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(lastDeletedTransaction = transaction)
                    transactionRepository.deleteTransaction(transaction.id)
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            lastDeletedTransaction = null,
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        fun undoDelete() {
            val transaction = _uiState.value.lastDeletedTransaction ?: return
            viewModelScope.launch {
                try {
                    transactionRepository.addTransaction(
                        type = transaction.type,
                        amount = transaction.amount,
                        categoryId = transaction.category.id,
                        note = transaction.note,
                        timestamp = transaction.timestamp,
                        currencyCode = transaction.currencyCode,
                        tripId = transaction.tripId,
                        amountForeignMinor = transaction.amountForeignMinor,
                    )
                    _uiState.value = _uiState.value.copy(lastDeletedTransaction = null)
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        fun clearLastDeleted() {
            _uiState.value = _uiState.value.copy(lastDeletedTransaction = null)
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(isError = false, errorMessage = null)
        }

        fun retry() {
            clearError()
            retryTrigger.value++
        }

        private fun dateToMillis(date: LocalDate): Long =
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        private fun dateToEndOfDayMillis(date: LocalDate): Long =
            date
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

        private data class FilterParams(
            val month: YearMonth,
            val query: String,
            val filter: TransactionType?,
            val scope: SearchScope,
            val categoryId: Long?,
        )

        companion object {
            const val SEARCH_DEBOUNCE_MS = 300L
            const val STOP_TIMEOUT_MS = 5000L
        }
    }

/**
 * Which consent dialog (if any) the home screen should render right now.
 * Computed by [HomeViewModel.consentPromptVariant] from the four-key
 * `AnalyticsPreferences` state + onboarding completion.
 */
sealed interface ConsentPromptVariant {
    data object None : ConsentPromptVariant

    data object Main : ConsentPromptVariant

    data object AnalyticsOnly : ConsentPromptVariant
}

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val filter: TransactionType? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: UiText? = null,
    val monthLabel: String = "",
    val lastDeletedTransaction: Transaction? = null,
    val searchScope: SearchScope = SearchScope.CURRENT_MONTH,
    val selectedCategoryId: Long? = null,
    val selectedCategoryName: String? = null,
    val dateRangeStart: LocalDate? = null,
    val dateRangeEnd: LocalDate? = null,
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (filter != null) count++
            if (searchScope == SearchScope.ALL_MONTHS) count++
            if (selectedCategoryId != null) count++
            if (dateRangeStart != null) count++
            return count
        }

    val hasActiveFilters: Boolean
        get() = activeFilterCount > 0
}
