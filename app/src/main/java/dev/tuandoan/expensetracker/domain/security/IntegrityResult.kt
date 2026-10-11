package dev.tuandoan.expensetracker.domain.security

/**
 * State of app and device integrity verification.
 */
sealed interface IntegrityState {
    data object Unchecked : IntegrityState

    data object Checking : IntegrityState

    data class Evaluated(
        val result: IntegrityResult,
    ) : IntegrityState
}

/**
 * Result data model of an integrity evaluation.
 *
 * @property isGenuine True if app is verified as genuine (or offline-bypassed safely).
 * @property isPlayStoreInstalled True if installer package indicates Google Play Store (`com.android.vending`).
 * @property isSignatureValid True if release signing certificate matches expected production digest.
 * @property isDeviceIntegrityMet True if device attestation meets basic or strong device integrity.
 * @property isOfflineMode True if check ran while device had no network (non-blocking offline pass).
 * @property remediationRequired The recommended Google Play remediation flow if issues are detected.
 * @property details Additional diagnostic information or error context (never logs PII).
 */
data class IntegrityResult(
    val isGenuine: Boolean,
    val isPlayStoreInstalled: Boolean = true,
    val isSignatureValid: Boolean = true,
    val isDeviceIntegrityMet: Boolean = true,
    val isOfflineMode: Boolean = false,
    val remediationRequired: RemediationType? = null,
    val details: String? = null,
)

/**
 * Remediation actions mapped to Google Play Integrity Dialog types.
 */
enum class RemediationType {
    GET_LICENSED,
    GET_INTEGRITY,
    CLOSE_RISKY_APPS,
}

/**
 * Outcome of presenting a Google Play remediation dialog.
 */
enum class RemediationResult {
    SUCCESSFUL,
    CANCELLED,
    FAILED,
    UNAVAILABLE,
}
