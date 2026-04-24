package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Preferences for backup encryption. When enabled, backup export writes an
 * encrypted `.etbackup` container; when disabled, backup export writes plain
 * JSON as before. Defaults to `false`.
 */
interface BackupEncryptionPreferences {
    val encryptBackups: Flow<Boolean>

    suspend fun setEncryptBackups(enabled: Boolean)

    /**
     * Whether the user has dismissed the one-time "if you forget the password
     * your data can't be recovered" warning. `false` until they explicitly
     * acknowledge it on the first attempt to enable encryption; then `true`
     * forever so subsequent toggles don't re-prompt.
     */
    val hasAcknowledgedPasswordWarning: Flow<Boolean>

    suspend fun setHasAcknowledgedPasswordWarning(acknowledged: Boolean)
}
