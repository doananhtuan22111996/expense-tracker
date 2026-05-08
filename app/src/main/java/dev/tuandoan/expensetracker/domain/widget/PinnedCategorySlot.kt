package dev.tuandoan.expensetracker.domain.widget

import dev.tuandoan.expensetracker.domain.model.Category

/**
 * Represents one of the three fixed widget tile positions. Either resolved
 * to a [Category] ([Filled]) or intentionally empty ([Empty]) — either
 * because the user hasn't pinned that slot yet or because the previously
 * pinned category was deleted (FR-07 silent fallback).
 *
 * [index] is 1-based (1, 2, or 3) to match the DataStore key naming
 * (`pinned_category_id_1/2/3`) and the user-facing slot labels in the
 * Settings → Widget Categories screen.
 *
 * The use case always emits exactly three slots in ascending [index] order
 * so the widget layout stays stable across config changes — empty slots
 * surface as placeholder tiles, never disappearing rows.
 */
sealed interface PinnedCategorySlot {
    val index: Int

    data class Filled(
        override val index: Int,
        val category: Category,
    ) : PinnedCategorySlot

    data class Empty(
        override val index: Int,
    ) : PinnedCategorySlot
}
