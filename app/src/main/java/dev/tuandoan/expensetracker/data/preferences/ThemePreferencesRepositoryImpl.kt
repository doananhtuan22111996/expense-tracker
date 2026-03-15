package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

@Singleton
class ThemePreferencesRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ThemePreferencesRepository {
        private val themeKey = intPreferencesKey("theme_preference")

        override val themePreference: Flow<ThemePreference> =
            context.themeDataStore.data.map { preferences ->
                val ordinal = preferences[themeKey] ?: ThemePreference.SYSTEM.ordinal
                ThemePreference.entries.getOrElse(ordinal) { ThemePreference.SYSTEM }
            }

        override suspend fun setTheme(pref: ThemePreference) {
            withContext(ioDispatcher) {
                context.themeDataStore.edit { preferences ->
                    preferences[themeKey] = pref.ordinal
                }
            }
        }
    }
