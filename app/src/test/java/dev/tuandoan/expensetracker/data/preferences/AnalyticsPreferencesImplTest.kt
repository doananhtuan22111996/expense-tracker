package dev.tuandoan.expensetracker.data.preferences

import dev.tuandoan.expensetracker.testutil.FakeAnalyticsPreferences
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
 * round-trip, independence of the four keys); the impl itself is straight-line
 * DataStore boilerplate that's covered by device usage.
 *
 * ### Key independence (privacy-critical)
 *
 * All four keys — [AnalyticsPreferences.analyticsConsent],
 * [AnalyticsPreferences.consentPromptShown],
 * [AnalyticsPreferences.analyticsEventsConsent], and
 * [AnalyticsPreferences.analyticsEventsPromptShown] — are intentionally
 * independent (ADR-008 + ADR-011). A user who accepts Crashlytics has NOT
 * implicitly accepted Analytics events, and vice versa. A user who declines
 * a prompt (either Crashlytics or Analytics) has `*PromptShown = true` but
 * `*Consent = false`. These tests pin that independence in every pairwise
 * direction: flipping any one key must not flip any of the other three.
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

    // ── Analytics events consent (v3.11.0 / ADR-011) ───────────────────────

    @Test
    fun defaultAnalyticsEventsConsent_isFalse() =
        runTest {
            assertFalse(preferences.analyticsEventsConsent.first())
        }

    @Test
    fun defaultAnalyticsEventsPromptShown_isFalse() =
        runTest {
            assertFalse(preferences.analyticsEventsPromptShown.first())
        }

    @Test
    fun setAnalyticsEventsConsentTrue_thenReadEmitsTrue() =
        runTest {
            preferences.setAnalyticsEventsConsent(true)

            assertTrue(preferences.analyticsEventsConsent.first())
        }

    @Test
    fun setAnalyticsEventsConsentFalse_afterTrue_roundTripsBack() =
        runTest {
            preferences.setAnalyticsEventsConsent(true)
            preferences.setAnalyticsEventsConsent(false)

            assertFalse(preferences.analyticsEventsConsent.first())
        }

    @Test
    fun setAnalyticsEventsPromptShownTrue_thenReadEmitsTrue() =
        runTest {
            preferences.setAnalyticsEventsPromptShown(true)

            assertTrue(preferences.analyticsEventsPromptShown.first())
        }

    // ── Independence invariants (ADR-011 — privacy-critical contract) ─────

    @Test
    fun setAnalyticsEventsConsentTrue_doesNotFlipAnyOtherKey() =
        runTest {
            // Privacy invariant: opting into Analytics must not imply opting into
            // Crashlytics, and must not mark any prompt as shown.
            preferences.setAnalyticsEventsConsent(true)

            assertFalse(preferences.analyticsConsent.first())
            assertFalse(preferences.consentPromptShown.first())
            assertFalse(preferences.analyticsEventsPromptShown.first())
        }

    @Test
    fun setAnalyticsEventsPromptShownTrue_doesNotFlipAnyOtherKey() =
        runTest {
            // Marking the Analytics dialog as shown must not grant either consent,
            // nor mark the Crashlytics dialog as shown.
            preferences.setAnalyticsEventsPromptShown(true)

            assertFalse(preferences.analyticsConsent.first())
            assertFalse(preferences.consentPromptShown.first())
            assertFalse(preferences.analyticsEventsConsent.first())
        }

    @Test
    fun setAnalyticsConsentTrue_doesNotFlipAnalyticsEventsKeys() =
        runTest {
            // Reverse direction: opting into Crashlytics must not imply opting
            // into Analytics, and must not mark the Analytics prompt as shown.
            // This is the direction that matters for beta testers upgrading from
            // pre-bundle v3.11.0 (Crashlytics-only) to the bundled release.
            preferences.setAnalyticsConsent(true)

            assertFalse(preferences.analyticsEventsConsent.first())
            assertFalse(preferences.analyticsEventsPromptShown.first())
        }

    @Test
    fun setConsentPromptShownTrue_doesNotFlipAnalyticsEventsKeys() =
        runTest {
            // Marking the Crashlytics dialog as shown must not auto-mark the
            // Analytics dialog as shown — upgrade users with prior Crashlytics
            // consent still need to see the new Analytics question.
            preferences.setConsentPromptShown(true)

            assertFalse(preferences.analyticsEventsConsent.first())
            assertFalse(preferences.analyticsEventsPromptShown.first())
        }
}
