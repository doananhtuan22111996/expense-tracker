package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    val themePreference: Flow<ThemePreference>

    suspend fun setTheme(pref: ThemePreference)
}
