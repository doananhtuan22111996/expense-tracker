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

private val Context.analyticsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "analytics_preferences",
)

/**
 * DataStore-backed implementation of [AnalyticsPreferences].
 * Consent defaults to `false` -- crash reporting is disabled until the user opts in.
 */
@Singleton
class AnalyticsPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : AnalyticsPreferences {
        private val consentKey = booleanPreferencesKey("analytics_consent")

        override val analyticsConsent: Flow<Boolean> =
            context.analyticsDataStore.data.map { preferences ->
                preferences[consentKey] ?: false
            }

        override suspend fun setAnalyticsConsent(enabled: Boolean) {
            withContext(ioDispatcher) {
                context.analyticsDataStore.edit { preferences ->
                    preferences[consentKey] = enabled
                }
            }
        }
    }
