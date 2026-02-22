package dev.tuandoan.expensetracker.domain.repository

import kotlinx.coroutines.flow.Flow

interface CurrencyPreferenceRepository {
    fun observeDefaultCurrency(): Flow<String>

    suspend fun setDefaultCurrency(currencyCode: String)

    suspend fun getDefaultCurrency(): String
}
