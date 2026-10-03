package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.data.preferences.SecurityPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSecurityPreferences(
    initialBiometric: Boolean = false,
    initialTimeout: Long = SecurityPreferences.TIMEOUT_IMMEDIATELY,
    initialHideInRecents: Boolean = false,
) : SecurityPreferences {
    private val _isBiometricEnabled = MutableStateFlow(initialBiometric)
    override val isBiometricEnabled: Flow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _autoLockTimeoutMs = MutableStateFlow(initialTimeout)
    override val autoLockTimeoutMs: Flow<Long> = _autoLockTimeoutMs.asStateFlow()

    private val _isHideInRecentsEnabled = MutableStateFlow(initialHideInRecents)
    override val isHideInRecentsEnabled: Flow<Boolean> = _isHideInRecentsEnabled.asStateFlow()

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
    }

    override suspend fun setAutoLockTimeoutMs(timeoutMs: Long) {
        _autoLockTimeoutMs.value = timeoutMs
    }

    override suspend fun setHideInRecentsEnabled(enabled: Boolean) {
        _isHideInRecentsEnabled.value = enabled
    }
}
