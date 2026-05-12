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
 * ### Max-3 rule
 *
 * FR-05: pin at most 3 categories. When the user taps a not-yet-pinned
 * row and [currentPinnedIds] already has 3 entries, we emit a one-shot
 * [UiText] to [WidgetCategoriesUiState.overLimitMessage]; the UI shows a
 * snackbar and clears via [onOverLimitMessageShown]. No DataStore write
 * happens in that path — the rule is enforced in-memory at the VM
 * boundary, not at the persistence boundary (which also enforces it via
 * `ids.take(3)`, but the helper snackbar only fires on user tap, not on
 * e.g. a programmatic setAll-4 call — nothing in-app actually does that
 * today).
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

        init {
            viewModelScope.launch {
                combine(
                    pinnedCategoriesUseCase(),
                    categoryRepository.observeCategories(TransactionType.EXPENSE),
                    widgetCategoryPreferences.pinnedCategoryIds,
                ) { slots, categories, rawPinnedIds ->
                    currentPinnedIds = rawPinnedIds
                    WidgetCategoriesUiState(
                        pinnedSlots = slots,
                        availableCategories = categories,
                        pinnedCount = rawPinnedIds.size,
                        isAtMaxPins = rawPinnedIds.size >= MAX_PINS,
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
         * isn't pinned and we're under the 3-pin cap, append it. Otherwise
         * emit the over-limit hint and make no write.
         */
        fun onTogglePin(categoryId: Long) {
            val snapshot = currentPinnedIds
            val next =
                when {
                    categoryId in snapshot -> snapshot.filterNot { it == categoryId }
                    snapshot.size >= MAX_PINS -> {
                        _uiState.update {
                            it.copy(overLimitMessage = UiText.StringResource(R.string.widget_categories_over_limit))
                        }
                        return
                    }
                    else -> snapshot + categoryId
                }
            persist(next)
        }

        /**
         * Reorder the pinned list: take the entry at [fromIndex] and drop
         * it at [toIndex]. Both are zero-based positions within the raw
         * pinned-IDs list (not the 1-based slot index). Out-of-range
         * inputs are dropped silently — defensive guard for future drag
         * gestures that could report phantom positions during teardown.
         */
        fun onMove(
            fromIndex: Int,
            toIndex: Int,
        ) {
            val snapshot = currentPinnedIds
            if (fromIndex == toIndex) return
            if (fromIndex !in snapshot.indices) return
            if (toIndex !in snapshot.indices) return
            val reordered =
                snapshot.toMutableList().apply {
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
