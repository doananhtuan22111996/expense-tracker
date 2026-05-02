package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Contract tests for [AnalyticsPreferences] exercised through a fake.
 *
 * Follows the convention established by [InsightsCollapsePreferencesImplTest] —
 * the real DataStore-backed impl depends on Android `Context` +
 * [androidx.datastore] which would pull in Robolectric just to exercise
 * boolean preferences. The fake proves the interface semantics (defaults,
 * round-trip, independence of the two keys); the impl itself is straight-line
 * DataStore boilerplate that's covered by device usage.
 *
 * The two keys [AnalyticsPreferences.analyticsConsent] and
 * [AnalyticsPreferences.consentPromptShown] are intentionally independent
 * (ADR-008): a user who declines the prompt has `consentPromptShown = true`
 * (don't re-ask) but `analyticsConsent = false` (no data collected). These
 * tests pin that independence.
 */
class AnalyticsPreferencesImplTest {
    private lateinit var preferences: FakeAnalyticsPreferences

    @Before
    fun setup() {
        preferences = FakeAnalyticsPreferences()
    }

    @Test
    fun defaultAnalyticsConsent_isFalse() =
        runTest {
            assertFalse(preferences.analyticsConsent.first())
        }

    @Test
    fun defaultConsentPromptShown_isFalse() =
        runTest {
            assertFalse(preferences.consentPromptShown.first())
        }

    @Test
    fun setAnalyticsConsentTrue_thenReadEmitsTrue() =
        runTest {
            preferences.setAnalyticsConsent(true)

            assertTrue(preferences.analyticsConsent.first())
        }

    @Test
    fun setAnalyticsConsentFalse_afterTrue_roundTripsBack() =
        runTest {
            preferences.setAnalyticsConsent(true)
            preferences.setAnalyticsConsent(false)

            assertFalse(preferences.analyticsConsent.first())
        }

    @Test
    fun setConsentPromptShownTrue_thenReadEmitsTrue() =
        runTest {
            preferences.setConsentPromptShown(true)

            assertTrue(preferences.consentPromptShown.first())
        }

    @Test
    fun setAnalyticsConsentTrue_doesNotFlipConsentPromptShown() =
        runTest {
            // Independence: flipping consent doesn't implicitly mark the prompt shown.
            preferences.setAnalyticsConsent(true)

            assertFalse(preferences.consentPromptShown.first())
        }

    @Test
    fun setConsentPromptShownTrue_doesNotFlipAnalyticsConsent() =
        runTest {
            // Independence: marking the prompt shown doesn't grant consent.
            // This is the "user declined" path — we remember we asked, consent stays off.
            preferences.setConsentPromptShown(true)

            assertFalse(preferences.analyticsConsent.first())
        }

    @Test
    fun declinedScenario_consentOffPromptShown_bothPersistAcrossReads() =
        runTest {
            // Simulate: user sees dialog, taps "No thanks".
            preferences.setConsentPromptShown(true)
            // analyticsConsent stays at default false.

            assertFalse(preferences.analyticsConsent.first())
            assertTrue(preferences.consentPromptShown.first())
            // Subsequent reads emit the same values (no surprise flips).
            assertFalse(preferences.analyticsConsent.first())
            assertTrue(preferences.consentPromptShown.first())
        }

    @Test
    fun acceptedScenario_bothTrue() =
        runTest {
            // Simulate: user sees dialog, taps "Yes, share crash reports".
            preferences.setAnalyticsConsent(true)
            preferences.setConsentPromptShown(true)

            assertTrue(preferences.analyticsConsent.first())
            assertTrue(preferences.consentPromptShown.first())
        }
}

class FakeAnalyticsPreferences : AnalyticsPreferences {
    private val _analyticsConsent = MutableStateFlow(false)
    private val _consentPromptShown = MutableStateFlow(false)

    override val analyticsConsent: Flow<Boolean> = _analyticsConsent
    override val consentPromptShown: Flow<Boolean> = _consentPromptShown

    override suspend fun setAnalyticsConsent(enabled: Boolean) {
        _analyticsConsent.value = enabled
    }

    override suspend fun setConsentPromptShown(shown: Boolean) {
        _consentPromptShown.value = shown
    }
}
