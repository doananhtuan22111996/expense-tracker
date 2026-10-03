package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "security_preferences",
)

@Singleton
class SecurityPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : SecurityPreferences {
        private val biometricEnabledKey = booleanPreferencesKey("is_biometric_enabled")
        private val autoLockTimeoutKey = longPreferencesKey("auto_lock_timeout_ms")
        private val hideInRecentsKey = booleanPreferencesKey("is_hide_in_recents_enabled")

        override val isBiometricEnabled: Flow<Boolean> =
            context.securityDataStore.data.map { preferences ->
                preferences[biometricEnabledKey] ?: false
            }

        override suspend fun setBiometricEnabled(enabled: Boolean) {
            withContext(ioDispatcher) {
                context.securityDataStore.edit { preferences ->
                    preferences[biometricEnabledKey] = enabled
                }
            }
        }

        override val autoLockTimeoutMs: Flow<Long> =
            context.securityDataStore.data.map { preferences ->
                preferences[autoLockTimeoutKey] ?: SecurityPreferences.TIMEOUT_IMMEDIATELY
            }

        override suspend fun setAutoLockTimeoutMs(timeoutMs: Long) {
            withContext(ioDispatcher) {
                context.securityDataStore.edit { preferences ->
                    preferences[autoLockTimeoutKey] = timeoutMs
                }
            }
        }

        override val isHideInRecentsEnabled: Flow<Boolean> =
            context.securityDataStore.data.map { preferences ->
                preferences[hideInRecentsKey] ?: false
            }

        override suspend fun setHideInRecentsEnabled(enabled: Boolean) {
            withContext(ioDispatcher) {
                context.securityDataStore.edit { preferences ->
                    preferences[hideInRecentsKey] = enabled
                }
            }
        }
    }
