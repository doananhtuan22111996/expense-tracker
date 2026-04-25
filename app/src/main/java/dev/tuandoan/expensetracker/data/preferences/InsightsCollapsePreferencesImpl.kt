package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore name chosen per the "one store per feature" convention already
 * used by `backup_encryption_preferences`, `budget_preferences`, etc. Keeps
 * writes from one UI surface from blocking reads of an unrelated surface.
 */
private val Context.insightsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "insights_preferences",
)

@Singleton
class InsightsCollapsePreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : InsightsCollapsePreferences {
        private val collapsedKey = booleanPreferencesKey("insights_collapsed")

        override val collapsed: Flow<Boolean> =
            context.insightsDataStore.data.map { preferences ->
                preferences[collapsedKey] ?: false
            }

        override suspend fun setCollapsed(value: Boolean) {
            withContext(ioDispatcher) {
                context.insightsDataStore.edit { preferences ->
                    preferences[collapsedKey] = value
                }
            }
        }
    }
