package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Test double for [AnalyticsPreferences].
 *
 * Single source of truth for this fixture across all test modules — use this
 * instead of inlining a private `FakeAnalyticsPreferences` inside a test
 * class. Any future interface change (e.g. a third consent-adjacent boolean
 * in v3.12+) gets updated here and everywhere it's referenced picks up the
 * change via the compiler.
 *
 * Both flows default to `false` to match the real impl's privacy-safe default.
 */
class FakeAnalyticsPreferences : AnalyticsPreferences {
    private val _analyticsConsent = MutableStateFlow(false)
    private val _consentPromptShown = MutableStateFlow(false)
    private val _analyticsEventsConsent = MutableStateFlow(false)
    private val _analyticsEventsPromptShown = MutableStateFlow(false)

    override val analyticsConsent: Flow<Boolean> = _analyticsConsent
    override val consentPromptShown: Flow<Boolean> = _consentPromptShown
    override val analyticsEventsConsent: Flow<Boolean> = _analyticsEventsConsent
    override val analyticsEventsPromptShown: Flow<Boolean> = _analyticsEventsPromptShown

    override suspend fun setAnalyticsConsent(enabled: Boolean) {
        _analyticsConsent.value = enabled
    }

    override suspend fun setConsentPromptShown(shown: Boolean) {
        _consentPromptShown.value = shown
    }

    override suspend fun setAnalyticsEventsConsent(enabled: Boolean) {
        _analyticsEventsConsent.value = enabled
    }

    override suspend fun setAnalyticsEventsPromptShown(shown: Boolean) {
        _analyticsEventsPromptShown.value = shown
    }
}
