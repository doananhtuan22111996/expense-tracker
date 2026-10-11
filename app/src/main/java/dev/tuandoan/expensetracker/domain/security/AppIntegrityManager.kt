package dev.tuandoan.expensetracker.domain.security

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain interface for verifying application integrity, anti-tampering,
 * and presenting Google Play remediation dialogs.
 */
interface AppIntegrityManager {
    /**
     * Observable state of integrity checks throughout the application lifecycle.
     */
    val integrityState: StateFlow<IntegrityState>

    /**
     * Warms up the integrity provider in the background at app launch to minimize
     * subsequent attestation latency.
     */
    suspend fun warmUp()

    /**
     * Evaluates application and device integrity.
     *
     * In an offline environment, this operation must never block the user from
     * accessing local financial records, returning an offline-tolerant result instead.
     */
    suspend fun evaluateIntegrity(): IntegrityResult

    /**
     * Requests Google Play to display an in-app remediation dialog (e.g. GET_LICENSED)
     * allowing the user to acquire or fix the app directly from the Google Play Store.
     *
     * @param activity Foreground activity to host the Play dialog.
     * @param typeCode The remediation type requested.
     */
    suspend fun requestRemediation(
        activity: Activity,
        typeCode: RemediationType,
    ): RemediationResult
}
