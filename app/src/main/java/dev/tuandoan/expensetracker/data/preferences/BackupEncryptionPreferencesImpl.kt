package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backupEncryptionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "backup_encryption_preferences",
)

@Singleton
class BackupEncryptionPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : BackupEncryptionPreferences {
        private val encryptBackupsKey = booleanPreferencesKey("encrypt_backups")
        private val acknowledgedWarningKey =
            booleanPreferencesKey("has_acknowledged_password_warning")

        override val encryptBackups: Flow<Boolean> =
            context.backupEncryptionDataStore.data.map { preferences ->
                preferences[encryptBackupsKey] ?: false
            }

        override suspend fun setEncryptBackups(enabled: Boolean) {
            withContext(ioDispatcher) {
                context.backupEncryptionDataStore.edit { preferences ->
                    preferences[encryptBackupsKey] = enabled
                }
            }
        }

        override val hasAcknowledgedPasswordWarning: Flow<Boolean> =
            context.backupEncryptionDataStore.data.map { preferences ->
                preferences[acknowledgedWarningKey] ?: false
            }

        override suspend fun setHasAcknowledgedPasswordWarning(acknowledged: Boolean) {
            withContext(ioDispatcher) {
                context.backupEncryptionDataStore.edit { preferences ->
                    preferences[acknowledgedWarningKey] = acknowledged
                }
            }
        }
    }
