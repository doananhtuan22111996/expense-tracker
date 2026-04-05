package dev.tuandoan.expensetracker.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Preferences for budget alert notifications.
 * Alerts are disabled by default and must be explicitly enabled by the user.
 */
interface BudgetAlertPreferences {
    /** Whether budget alerts are enabled. Defaults to [false]. */
    val alertsEnabled: Flow<Boolean>

    /** Sets the budget alerts enabled preference. */
    suspend fun setAlertsEnabled(enabled: Boolean)

    /** The year-month string (e.g., "2026-04") of the last alert sent, used for dedup. */
    val lastAlertMonth: Flow<String?>

    /** Updates the last alert month after sending a notification. */
    suspend fun setLastAlertMonth(yearMonth: String)
}
