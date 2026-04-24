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
}
