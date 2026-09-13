package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.domain.repository.TripPreferences
import dev.tuandoan.expensetracker.domain.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tripDataStore by preferencesDataStore(name = "trip_preferences")

@Singleton
class TripPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val widgetUpdater: WidgetUpdater,
    ) : TripPreferences {
        private val excludeTripsKey = booleanPreferencesKey("exclude_trips")

        override val excludeTrips: Flow<Boolean> =
            context.tripDataStore.data.map { preferences ->
                preferences[excludeTripsKey] ?: false
            }

        override suspend fun setExcludeTrips(exclude: Boolean) {
            context.tripDataStore.edit { preferences ->
                preferences[excludeTripsKey] = exclude
            }
            widgetUpdater.requestUpdate()
        }
    }
