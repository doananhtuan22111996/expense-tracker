package dev.tuandoan.expensetracker.domain.widget

import dev.tuandoan.expensetracker.data.preferences.WidgetCategoryPreferences
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

private const val MAX_PINNED = 3

/**
 * Resolves the raw pinned-category IDs from [WidgetCategoryPreferences]
 * against the live EXPENSE-category list so the widget (and the pin
 * Settings screen) can render slots without each caller hand-rolling the
 * filter-deleted + 3-slot-padding logic.
 *
 * Emits exactly [MAX_PINNED] slots in ascending index order (1..3):
 * - Index ≤ pinnedCategoryIds.size AND the ID still resolves to an
 *   EXPENSE [Category] → [PinnedCategorySlot.Filled]
 * - Otherwise → [PinnedCategorySlot.Empty]
 *
 * Per FR-07, a deleted pinned category collapses to an empty slot
 * silently — the widget shows the placeholder, the user can re-pin
 * another category from Settings. No error surface, no toast.
 *
 * Re-emits on any upstream change (user reorders pins, deletes a
 * category, creates a new one, etc.).
 */
class PinnedCategoriesUseCase
    @Inject
    constructor(
        private val widgetCategoryPreferences: WidgetCategoryPreferences,
        private val categoryRepository: CategoryRepository,
    ) {
        operator fun invoke(): Flow<List<PinnedCategorySlot>> =
            combine(
                widgetCategoryPreferences.pinnedCategoryIds,
                categoryRepository.observeCategories(TransactionType.EXPENSE),
            ) { pinnedIds, expenseCategories ->
                val categoriesById = expenseCategories.associateBy { it.id }
                List(MAX_PINNED) { position ->
                    val slotIndex = position + 1
                    val pinnedId = pinnedIds.getOrNull(position)
                    val resolved = pinnedId?.let { categoriesById[it] }
                    if (resolved != null) {
                        PinnedCategorySlot.Filled(index = slotIndex, category = resolved)
                    } else {
                        PinnedCategorySlot.Empty(index = slotIndex)
                    }
                }
            }
    }
