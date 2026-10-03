package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Preferences for app security and biometric lock (v3.16.0, ADR-018).
 */
interface SecurityPreferences {
    /**
     * Whether biometric/screen lock authentication is required to access the app.
     * Defaults to `false`.
     */
    val isBiometricEnabled: Flow<Boolean>

    suspend fun setBiometricEnabled(enabled: Boolean)

    /**
     * Auto-lock duration threshold in milliseconds after the app is backgrounded.
     * Defaults to `0L` (immediately).
     */
    val autoLockTimeoutMs: Flow<Long>

    suspend fun setAutoLockTimeoutMs(timeoutMs: Long)

    /**
     * Whether the window should set `FLAG_SECURE` to prevent preview screenshots
     * in the Android system recent apps overview. Defaults to `false`.
     */
    val isHideInRecentsEnabled: Flow<Boolean>

    suspend fun setHideInRecentsEnabled(enabled: Boolean)

    companion object {
        const val TIMEOUT_IMMEDIATELY = 0L
        const val TIMEOUT_ONE_MINUTE = 60_000L
        const val TIMEOUT_FIVE_MINUTES = 300_000L
    }
}
