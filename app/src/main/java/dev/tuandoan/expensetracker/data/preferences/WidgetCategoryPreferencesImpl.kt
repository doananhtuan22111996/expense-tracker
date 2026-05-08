package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore name follows the "one store per feature" convention already
 * used by `budget_preferences`, `insights_preferences`, etc. Three fixed
 * long keys (slot_1..3) keep the storage trivially enumerable; reordering
 * rewrites the set atomically via a single `edit { }` block.
 */
private val Context.widgetCategoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_category_preferences",
)

private const val MAX_PINNED = 3

@Singleton
class WidgetCategoryPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val widgetUpdater: WidgetUpdater,
    ) : WidgetCategoryPreferences {
        private val slotKeys = List(MAX_PINNED) { index -> longPreferencesKey("pinned_category_id_${index + 1}") }

        override val pinnedCategoryIds: Flow<List<Long>> =
            context.widgetCategoryDataStore.data.map { preferences ->
                slotKeys.mapNotNull { preferences[it] }
            }

        override suspend fun setPinnedCategoryIds(ids: List<Long>) {
            val trimmed = ids.take(MAX_PINNED)
            withContext(ioDispatcher) {
                context.widgetCategoryDataStore.edit { preferences ->
                    slotKeys.forEachIndexed { index, key ->
                        val value = trimmed.getOrNull(index)
                        if (value != null) {
                            preferences[key] = value
                        } else {
                            preferences.remove(key)
                        }
                    }
                }
            }
            widgetUpdater.requestUpdate()
        }
    }
