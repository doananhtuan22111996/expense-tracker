package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Preferences for anonymous diagnostic-data consent. Four independent
 * booleans, split across two concerns (crash reports + usage analytics)
 * per ADR-011's two-toggle decision.
 *
 * ### Crash reporting (existing, from v3.2.4 / PR #33)
 *
 * - [analyticsConsent] — the user's Yes/No answer for **Firebase Crashlytics**
 *   only. Controls Crashlytics runtime collection via the observer in
 *   `ExpenseTrackerApplication.onCreate`.
 * - [consentPromptShown] — whether the Crashlytics-portion of the post-
 *   onboarding consent dialog has already been shown and resolved.
 *
 *   **Historical naming note:** the key name `analyticsConsent` predates the
 *   Firebase Analytics integration (v3.11.0). Despite the generic-sounding
 *   name, this flag covers Crashlytics only. Renaming would reset beta-tester
 *   consent; keeping the name preserves existing user choices. See ADR-011.
 *
 * ### Usage analytics (new in v3.11.0 per ADR-011 two-toggle decision)
 *
 * - [analyticsEventsConsent] — the user's Yes/No answer for **Firebase
 *   Analytics** event collection. Controls Analytics runtime collection
 *   independently from [analyticsConsent].
 * - [analyticsEventsPromptShown] — whether the Analytics-portion of the
 *   consent dialog has been shown. Independent of [consentPromptShown] so
 *   upgrade users who already resolved the Crashlytics prompt see only the
 *   new Analytics question (per ADR-011's beta-tester upgrade path).
 *
 * ### Independence invariant
 *
 * All four keys are independent. Flipping one MUST NOT flip any other. This
 * is the privacy-critical contract: a user who accepted crash reports has
 * NOT implicitly accepted analytics events, and vice versa. Enforced by
 * tests in `AnalyticsPreferencesImplTest`.
 *
 * All four keys default to `false` — no data leaves the device until the
 * user explicitly opts in to each concern.
 */
interface AnalyticsPreferences {
    /**
     * Whether the user has consented to anonymous crash reporting (Firebase
     * Crashlytics). Defaults to [false].
     *
     * Historical name — covers Crashlytics only despite the generic
     * "analytics" word. See interface KDoc for the naming rationale.
     */
    val analyticsConsent: Flow<Boolean>

    /**
     * Whether the Crashlytics-portion of the post-onboarding consent dialog
     * has already been shown and resolved (either Yes, No, or back-gesture
     * dismissed). When `true`, that portion of the dialog will not re-appear
     * on subsequent app launches. Defaults to [false].
     */
    val consentPromptShown: Flow<Boolean>

    /**
     * Whether the user has consented to anonymous usage analytics (Firebase
     * Analytics event collection). Defaults to [false]. Independent of
     * [analyticsConsent] per ADR-011.
     */
    val analyticsEventsConsent: Flow<Boolean>

    /**
     * Whether the Analytics-portion of the consent dialog has already been
     * shown and resolved. When `true`, that portion will not re-appear.
     * Defaults to [false]. Independent of [consentPromptShown] so upgrade
     * users only see the new Analytics question.
     */
    val analyticsEventsPromptShown: Flow<Boolean>

    /** Sets the Crashlytics consent preference. */
    suspend fun setAnalyticsConsent(enabled: Boolean)

    /** Marks the Crashlytics portion of the consent prompt as shown. */
    suspend fun setConsentPromptShown(shown: Boolean)

    /** Sets the Analytics event-collection consent preference. */
    suspend fun setAnalyticsEventsConsent(enabled: Boolean)

    /** Marks the Analytics portion of the consent prompt as shown. */
    suspend fun setAnalyticsEventsPromptShown(shown: Boolean)
}
