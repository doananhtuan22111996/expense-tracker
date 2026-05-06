package dev.tuandoan.expensetracker.domain.analytics

/**
 * Abstraction for anonymous usage-analytics event collection (v3.11.0,
 * ADR-011 + PRD FR-A9).
 *
 * Implementations MUST only forward events that are already typed via the
 * [AnalyticsEvent] sealed hierarchy — there is no `logEvent(name, params)`
 * catch-all by design, so FR-A7 compliance (no amounts / category names /
 * note text / region-identifying currency codes) is enforced at compile
 * time rather than at the SDK boundary.
 *
 * Gating lives outside this interface. Consent observation runs in
 * `ExpenseTrackerApplication.onCreate` (the same place the Crashlytics
 * observer lives) and toggles [setCollectionEnabled] as
 * `AnalyticsPreferences.analyticsEventsConsent` changes; feature code
 * can call [logEvent] unconditionally without knowing the consent state.
 *
 * Debug builds bind [NoOpAnalytics] via the debug source-set DI module
 * (ADR-010), so release-only Firebase classes never reach the debug
 * classpath.
 */
interface Analytics {
    /**
     * Forwards the event to the configured analytics backend. A no-op if
     * [setCollectionEnabled] was last called with `false`, or if the
     * binding is [NoOpAnalytics].
     */
    fun logEvent(event: AnalyticsEvent)

    /**
     * Enables or disables event collection based on the user's current
     * `analyticsEventsConsent` preference. MUST be process-wide: the
     * underlying SDK setting survives for the lifetime of the process
     * regardless of Activity re-creation.
     */
    fun setCollectionEnabled(enabled: Boolean)
}
