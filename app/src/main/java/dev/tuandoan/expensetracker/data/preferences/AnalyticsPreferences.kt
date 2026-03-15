package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Preferences for anonymous crash report consent.
 * Collection is disabled by default and must be explicitly enabled by the user.
 */
interface AnalyticsPreferences {
    /** Whether the user has consented to anonymous crash reporting. Defaults to [false]. */
    val analyticsConsent: Flow<Boolean>

    /** Sets the analytics consent preference. */
    suspend fun setAnalyticsConsent(enabled: Boolean)
}
