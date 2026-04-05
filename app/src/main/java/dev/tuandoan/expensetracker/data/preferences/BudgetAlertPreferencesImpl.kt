package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.domain.repository.BudgetAlertPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.budgetAlertDataStore by preferencesDataStore(name = "budget_alert_preferences")

@Singleton
class BudgetAlertPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BudgetAlertPreferences {
        override val alertsEnabled: Flow<Boolean> =
            context.budgetAlertDataStore.data.map { it[KEY_ALERTS_ENABLED] ?: false }

        override suspend fun setAlertsEnabled(enabled: Boolean) {
            context.budgetAlertDataStore.edit { it[KEY_ALERTS_ENABLED] = enabled }
        }

        override val lastAlertMonth: Flow<String?> =
            context.budgetAlertDataStore.data.map { it[KEY_LAST_ALERT_MONTH] }

        override suspend fun setLastAlertMonth(yearMonth: String) {
            context.budgetAlertDataStore.edit { it[KEY_LAST_ALERT_MONTH] = yearMonth }
        }

        override val lastAlertLevel: Flow<String?> =
            context.budgetAlertDataStore.data.map { it[KEY_LAST_ALERT_LEVEL] }

        override suspend fun setLastAlertLevel(level: String) {
            context.budgetAlertDataStore.edit { it[KEY_LAST_ALERT_LEVEL] = level }
        }

        private companion object {
            val KEY_ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
            val KEY_LAST_ALERT_MONTH = stringPreferencesKey("last_alert_month")
            val KEY_LAST_ALERT_LEVEL = stringPreferencesKey("last_alert_level")
        }
    }
