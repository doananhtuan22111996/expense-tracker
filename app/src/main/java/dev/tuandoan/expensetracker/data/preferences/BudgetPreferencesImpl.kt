package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.domain.repository.BudgetPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.budgetDataStore by preferencesDataStore(name = "budget_preferences")

@Singleton
class BudgetPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BudgetPreferences {
        private fun budgetKey(currencyCode: String) = longPreferencesKey("budget_$currencyCode")

        override fun getBudget(currencyCode: String): Flow<Long?> =
            context.budgetDataStore.data.map { it[budgetKey(currencyCode)] }

        override suspend fun setBudget(
            currencyCode: String,
            amount: Long,
        ) {
            require(amount > 0) { "Budget amount must be positive" }
            context.budgetDataStore.edit { it[budgetKey(currencyCode)] = amount }
        }

        override suspend fun clearBudget(currencyCode: String) {
            context.budgetDataStore.edit { it.remove(budgetKey(currencyCode)) }
        }

        override fun getAllBudgets(): Flow<Map<String, Long>> =
            context.budgetDataStore.data.map { preferences ->
                preferences
                    .asMap()
                    .filterKeys { it.name.startsWith("budget_") }
                    .mapKeys { it.key.name.removePrefix("budget_") }
                    .mapValues { it.value as Long }
            }
    }
