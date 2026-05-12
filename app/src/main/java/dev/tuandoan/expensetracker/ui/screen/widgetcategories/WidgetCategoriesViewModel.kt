package dev.tuandoan.expensetracker.ui.screen.widgetcategories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.preferences.WidgetCategoryPreferences
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.widget.PinnedCategoriesUseCase
import dev.tuandoan.expensetracker.domain.widget.PinnedCategorySlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_PINS = 3

/**
 * ViewModel backing the Settings → Widget Categories screen (T6.1).
 *
 * Responsibilities:
 * - Observe resolved pin slots via [PinnedCategoriesUseCase] (drives the
 *   3-slot preview strip at the top of the screen).
 * - Observe all EXPENSE [dev.tuandoan.expensetracker.domain.model.Category]
 *   rows (drives the pickable list below the preview).
 * - Mutate the underlying [WidgetCategoryPreferences] atomically on every
 *   user action (toggle-pin, reorder). Persistence goes through a single
 *   `setPinnedCategoryIds` call so the DataStore `edit {}` block stays
 *   the sole write boundary — matches T1.1's atomic-rewrite invariant.
 *
 * ### Source of truth for pin mutations
 *
 * [PinnedCategoriesUseCase] returns resolved [PinnedCategorySlot]s but the
 * mutation surface ([WidgetCategoryPreferences.setPinnedCategoryIds])
 * takes raw `List<Long>`. Rather than round-tripping through the slot
 * sealed type on every write, we subscribe to the preferences flow
 * separately and cache the latest raw list in [currentPinnedIds]. Reads
 * from that cache are lock-free — writes run on [viewModelScope] (single-
 * threaded from the main dispatcher) and the preferences flow is cold, so
 * toggle/reorder callers see a consistent snapshot.
 *
 * ### Max-3 rule & orphan pins
 *
 * FR-05: pin at most 3 categories. The cap is computed against *live*
 * pins only — raw IDs that still resolve to an existing [Category].
 * Orphan pins (IDs whose category was deleted elsewhere in the app) are
 * excluded from the count so the user isn't locked out of pinning new
 * categories by dead slots they can't see. `PinnedCategoriesUseCase`'s
 * FR-07 positional-fallback means orphans stay in the raw list as Empty
 * slots for widget layout stability; in the Settings screen we prefer
 * the leaner semantics.
 *
 * When the user triggers any mutation (toggle-pin, reorder), the VM
 * writes a *compacted* list with orphans stripped — so a single user
 * interaction heals the raw list persistently. A tap on a 4th category
 * while at the live cap emits a one-shot [UiText] to
 * [WidgetCategoriesUiState.overLimitMessage]; no DataStore write
 * happens in that path.
 */
@HiltViewModel
class WidgetCategoriesViewModel
    @Inject
    constructor(
        private val widgetCategoryPreferences: WidgetCategoryPreferences,
        private val categoryRepository: CategoryRepository,
        pinnedCategoriesUseCase: PinnedCategoriesUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(WidgetCategoriesUiState())
        val uiState: StateFlow<WidgetCategoriesUiState> = _uiState.asStateFlow()

        /**
         * Latest raw pin list — the mutation source of truth. Read by
         * [onTogglePin] / [onMove] to compute the next list without having
         * to suspend on `preferences.pinnedCategoryIds.first()`, which
         * would race the viewModelScope job ordering in tests.
         */
        @Volatile
        private var currentPinnedIds: List<Long> = emptyList()

        /**
         * Latest known set of live EXPENSE category IDs. Used to strip
         * orphaned entries from [currentPinnedIds] on mutation and to
         * compute the live-pin count for the cap.
         */
        @Volatile
        private var liveCategoryIds: Set<Long> = emptySet()

        init {
            viewModelScope.launch {
                combine(
                    pinnedCategoriesUseCase(),
                    categoryRepository.observeCategories(TransactionType.EXPENSE),
                    widgetCategoryPreferences.pinnedCategoryIds,
                ) { slots, categories, rawPinnedIds ->
                    currentPinnedIds = rawPinnedIds
                    liveCategoryIds = categories.mapTo(mutableSetOf()) { it.id }
                    val livePinnedCount = rawPinnedIds.count { it in liveCategoryIds }
                    WidgetCategoriesUiState(
                        pinnedSlots = slots,
                        availableCategories = categories,
                        pinnedCount = livePinnedCount,
                        isAtMaxPins = livePinnedCount >= MAX_PINS,
                        overLimitMessage = _uiState.value.overLimitMessage,
                        error = _uiState.value.error,
                        isLoading = false,
                    )
                }.collect { next ->
                    _uiState.value = next
                }
            }
        }

        /**
         * Toggle pin state for [categoryId]. If the ID is already pinned,
         * remove it (collapses following slots up by one index). If it
         * isn't pinned and we're under the 3-pin cap (measured against
         * *live* pins only), append it. Otherwise emit the over-limit
         * hint and make no write.
         *
         * Any toggle write strips orphaned pins (IDs whose category was
         * deleted elsewhere) from the persisted list — one user
         * interaction heals the raw list so future reads match what the
         * Settings UI shows.
         */
        fun onTogglePin(categoryId: Long) {
            val snapshot = currentPinnedIds
            val live = liveCategoryIds
            val liveCount = snapshot.count { it in live }
            val next =
                when {
                    categoryId in snapshot ->
                        snapshot.filterNot { it == categoryId || it !in live }
                    liveCount >= MAX_PINS -> {
                        _uiState.update {
                            it.copy(overLimitMessage = UiText.StringResource(R.string.widget_categories_over_limit))
                        }
                        return
                    }
                    else -> snapshot.filter { it in live } + categoryId
                }
            persist(next)
        }

        /**
         * Reorder the pinned list: take the entry at [fromIndex] and drop
         * it at [toIndex]. Both are zero-based positions within the
         * *live* pinned list (orphans excluded), matching what the UI
         * renders. Out-of-range inputs are dropped silently — defensive
         * guard for future drag gestures that could report phantom
         * positions during teardown. Like [onTogglePin], a reorder write
         * compacts orphans out of the persisted list.
         */
        fun onMove(
            fromIndex: Int,
            toIndex: Int,
        ) {
            if (fromIndex == toIndex) return
            val live = liveCategoryIds
            val compacted = currentPinnedIds.filter { it in live }
            if (fromIndex !in compacted.indices) return
            if (toIndex !in compacted.indices) return
            val reordered =
                compacted.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
            persist(reordered)
        }

        fun onOverLimitMessageShown() {
            _uiState.update { it.copy(overLimitMessage = null) }
        }

        fun onErrorShown() {
            _uiState.update { it.copy(error = null) }
        }

        private fun persist(ids: List<Long>) {
            viewModelScope.launch {
                try {
                    widgetCategoryPreferences.setPinnedCategoryIds(ids)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            error =
                                e.message?.let { msg -> UiText.DynamicString(msg) }
                                    ?: UiText.StringResource(R.string.error_save_widget_categories),
                        )
                    }
                }
            }
        }
    }
