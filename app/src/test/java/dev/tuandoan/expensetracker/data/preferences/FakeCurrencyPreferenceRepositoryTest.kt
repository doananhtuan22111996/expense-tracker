package dev.tuandoan.expensetracker.data.preferences

import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeCurrencyPreferenceRepositoryTest {
    private lateinit var repository: FakeCurrencyPreferenceRepository

    @Before
    fun setup() {
        repository = FakeCurrencyPreferenceRepository()
    }

    @Test
    fun getDefaultCurrency_returnsVndByDefault() =
        runTest {
            val currency = repository.getDefaultCurrency()
            assertEquals(SupportedCurrencies.default().code, currency)
        }

    @Test
    fun setDefaultCurrency_validCode_updatesCurrency() =
        runTest {
            repository.setDefaultCurrency("USD")

            assertEquals("USD", repository.getDefaultCurrency())
            assertTrue(repository.setDefaultCurrencyCalled)
            assertEquals("USD", repository.lastSetCurrencyCode)
        }

    @Test(expected = IllegalArgumentException::class)
    fun setDefaultCurrency_invalidCode_throws() =
        runTest {
            repository.setDefaultCurrency("INVALID")
        }

    @Test
    fun observeDefaultCurrency_emitsDefaultOnSubscription() =
        runTest {
            val currency = repository.observeDefaultCurrency().first()
            assertEquals(SupportedCurrencies.default().code, currency)
        }

    @Test
    fun observeDefaultCurrency_emitsUpdatedValue() =
        runTest {
            repository.setDefaultCurrency("EUR")

            val currency = repository.observeDefaultCurrency().first()
            assertEquals("EUR", currency)
        }

    @Test
    fun setDefaultCurrency_allSupportedCurrencies_succeed() =
        runTest {
            for (currencyDef in SupportedCurrencies.all()) {
                repository.setDefaultCurrency(currencyDef.code)
                assertEquals(currencyDef.code, repository.getDefaultCurrency())
            }
        }

    @Test
    fun initialCurrency_customDefault_returnsCustom() =
        runTest {
            val customRepo = FakeCurrencyPreferenceRepository(initialCurrency = "JPY")
            assertEquals("JPY", customRepo.getDefaultCurrency())
        }
}
