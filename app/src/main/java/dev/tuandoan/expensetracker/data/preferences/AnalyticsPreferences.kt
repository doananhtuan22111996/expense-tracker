package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Preferences for anonymous crash report consent.
 *
 * Two independent booleans:
 *
 * - [analyticsConsent] — the user's Yes/No answer. Controls Firebase Crashlytics
 *   runtime collection via `ExpenseTrackerApplication.onCreate`'s observer.
 * - [consentPromptShown] — whether the one-time onboarding consent dialog has
 *   already been shown. Independent of [analyticsConsent]: a `false` answer
 *   suppresses re-prompts (set `consentPromptShown = true`, keep
 *   `analyticsConsent = false`). Per ADR-008.
 *
 * Both default to `false` — no data leaves the device until the user explicitly
 * flips the consent toggle.
 */
interface AnalyticsPreferences {
    /** Whether the user has consented to anonymous crash reporting. Defaults to [false]. */
    val analyticsConsent: Flow<Boolean>

    /**
     * Whether the post-onboarding consent dialog has already been shown and
     * resolved (either Yes, No, or back-gesture dismissed). When `true`, the
     * dialog will not re-appear on subsequent app launches. Defaults to [false].
     */
    val consentPromptShown: Flow<Boolean>

    /** Sets the analytics consent preference. */
    suspend fun setAnalyticsConsent(enabled: Boolean)

    /** Marks the consent prompt as shown so it is not re-displayed. */
    suspend fun setConsentPromptShown(shown: Boolean)
}
