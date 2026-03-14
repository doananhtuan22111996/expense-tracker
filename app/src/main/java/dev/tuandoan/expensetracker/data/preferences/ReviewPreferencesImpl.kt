package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reviewDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "review_preferences",
)

/**
 * DataStore-backed implementation of [ReviewPreferences].
 */
@Singleton
class ReviewPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ReviewPreferences {
        private val lastPromptKey = longPreferencesKey("last_review_prompt_millis")
        private val shownCountKey = intPreferencesKey("review_shown_count")

        override val lastReviewPromptMillis: Flow<Long> =
            context.reviewDataStore.data.map { preferences ->
                preferences[lastPromptKey] ?: 0L
            }

        override val reviewShownCount: Flow<Int> =
            context.reviewDataStore.data.map { preferences ->
                preferences[shownCountKey] ?: 0
            }

        override suspend fun markReviewShown() {
            withContext(ioDispatcher) {
                context.reviewDataStore.edit { preferences ->
                    val currentCount = preferences[shownCountKey] ?: 0
                    preferences[shownCountKey] = currentCount + 1
                    preferences[lastPromptKey] = System.currentTimeMillis()
                }
            }
        }
    }
