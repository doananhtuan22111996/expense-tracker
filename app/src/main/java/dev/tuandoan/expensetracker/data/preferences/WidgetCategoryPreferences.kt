package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Preferences for the home-screen widget's pinned quick-add categories.
 * Users pick up to 3 EXPENSE categories in Settings → Widget Categories;
 * taps on a pinned tile trigger the quick-add sheet for that category.
 *
 * The list is always at most 3 entries long, ordered as the user arranged
 * them. Missing slots surface as empty placeholders on the widget. See
 * `PinnedCategoriesUseCase` for the category-resolution + filter-deleted
 * layer on top of these raw IDs.
 *
 * Placed in `data/preferences/` alongside [InsightsCollapsePreferences] and
 * friends rather than under `domain/repository/` because DataStore-backed
 * preferences are an inherently data-layer concern — matches the pattern
 * already established for analytics / review / backup-encryption prefs.
 */
interface WidgetCategoryPreferences {
    /**
     * Emits the current ordered list of pinned category IDs; defaults to an
     * empty list when no slots are set.
     */
    val pinnedCategoryIds: Flow<List<Long>>

    /**
     * Persist [ids] as the new ordered pin list. Only the first 3 entries
     * are retained; additional entries are silently dropped. Triggers a
     * widget refresh so the tile strip reflects the change within ~1s.
     */
    suspend fun setPinnedCategoryIds(ids: List<Long>)
}
