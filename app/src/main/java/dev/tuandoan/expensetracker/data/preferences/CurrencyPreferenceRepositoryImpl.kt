package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "currency_preferences")

@Singleton
class CurrencyPreferenceRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : CurrencyPreferenceRepository {
        private val defaultCurrencyKey = stringPreferencesKey("default_currency_code")

        override fun observeDefaultCurrency(): Flow<String> =
            context.dataStore.data.map { preferences ->
                resolveCode(preferences)
            }

        override suspend fun setDefaultCurrency(currencyCode: String) {
            require(SupportedCurrencies.byCode(currencyCode) != null) {
                "Unsupported currency code: $currencyCode"
            }
            withContext(ioDispatcher) {
                context.dataStore.edit { preferences ->
                    preferences[defaultCurrencyKey] = currencyCode
                }
            }
        }

        override suspend fun getDefaultCurrency(): String {
            val preferences = context.dataStore.data.first()
            return resolveCode(preferences)
        }

        private fun resolveCode(preferences: Preferences): String {
            val code = preferences[defaultCurrencyKey]
            return if (code != null && SupportedCurrencies.byCode(code) != null) {
                code
            } else {
                SupportedCurrencies.default().code
            }
        }
    }
