package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCurrencyPreferenceRepository(
    initialCurrency: String = SupportedCurrencies.default().code,
) : CurrencyPreferenceRepository {
    private val currencyState = MutableStateFlow(initialCurrency)

    var setDefaultCurrencyCalled = false
        private set

    var lastSetCurrencyCode: String? = null
        private set

    override fun observeDefaultCurrency(): Flow<String> = currencyState

    override suspend fun setDefaultCurrency(currencyCode: String) {
        require(SupportedCurrencies.byCode(currencyCode) != null) {
            "Unsupported currency code: $currencyCode"
        }
        setDefaultCurrencyCalled = true
        lastSetCurrencyCode = currencyCode
        currencyState.value = currencyCode
    }

    override suspend fun getDefaultCurrency(): String = currencyState.value
}
