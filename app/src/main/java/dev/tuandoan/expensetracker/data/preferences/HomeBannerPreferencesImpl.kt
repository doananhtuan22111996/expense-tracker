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
 * "One store per feature" convention (see [InsightsCollapsePreferencesImpl]).
 * Each banner gets its own boolean key — versioned so a future banner does
 * not inherit a previous dismissal.
 */
private val Context.homeBannerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "home_banner_preferences",
)

@Singleton
class HomeBannerPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : HomeBannerPreferences {
        private val widgetQuickAddKey = booleanPreferencesKey("widget_quick_add_v3_12_0_dismissed")

        override val widgetQuickAddBannerDismissed: Flow<Boolean> =
            context.homeBannerDataStore.data.map { preferences ->
                preferences[widgetQuickAddKey] ?: false
            }

        override suspend fun setWidgetQuickAddBannerDismissed() {
            withContext(ioDispatcher) {
                context.homeBannerDataStore.edit { preferences ->
                    preferences[widgetQuickAddKey] = true
                }
            }
        }
    }
