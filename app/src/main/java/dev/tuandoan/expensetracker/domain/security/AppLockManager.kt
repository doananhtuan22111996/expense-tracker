package dev.tuandoan.expensetracker.domain.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import dev.tuandoan.expensetracker.data.preferences.SecurityPreferences
import dev.tuandoan.expensetracker.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

fun interface ElapsedRealtimeClock {
    fun elapsedRealtime(): Long
}

/**
 * Manages the in-memory app lock lifecycle and background duration timer (v3.16.0, ADR-018).
 *
 * Listens to ProcessLifecycleOwner to determine when the entire application has been backgrounded.
 * If the duration spent in the background exceeds [SecurityPreferences.autoLockTimeoutMs],
 * the app is locked upon returning to the foreground.
 */
@Singleton
class AppLockManager
    @Inject
    constructor(
        private val securityPreferences: SecurityPreferences,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
        private val clock: ElapsedRealtimeClock = ElapsedRealtimeClock { android.os.SystemClock.elapsedRealtime() },
    ) : DefaultLifecycleObserver {
        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

        private val _isLocked = MutableStateFlow(false)
        val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

        private val _isInitialized = MutableStateFlow(false)
        val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

        var isBiometricEnabled: Boolean = false
            private set

        var autoLockTimeoutMs: Long = SecurityPreferences.TIMEOUT_IMMEDIATELY
            private set

        private var lastBackgroundTimeMs: Long = 0L
        private var isAppInBackground: Boolean = false
        private var isObserving: Boolean = false

        init {
            scope.launch {
                val initialBiometric = securityPreferences.isBiometricEnabled.first()
                val initialTimeout = securityPreferences.autoLockTimeoutMs.first()
                isBiometricEnabled = initialBiometric
                autoLockTimeoutMs = initialTimeout
                if (initialBiometric) {
                    _isLocked.value = true
                }
                _isInitialized.value = true

                launch {
                    securityPreferences.isBiometricEnabled.collect { enabled ->
                        isBiometricEnabled = enabled
                        if (!enabled) {
                            _isLocked.value = false
                        }
                    }
                }
                launch {
                    securityPreferences.autoLockTimeoutMs.collect { timeout ->
                        autoLockTimeoutMs = timeout
                    }
                }
            }
        }

        fun startObserving(lifecycle: Lifecycle) {
            if (!isObserving) {
                isObserving = true
                lifecycle.addObserver(this)
            }
        }

        fun unlock() {
            _isLocked.value = false
            lastBackgroundTimeMs = 0L
        }

        fun lock() {
            if (isBiometricEnabled) {
                _isLocked.value = true
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            isAppInBackground = true
            lastBackgroundTimeMs = clock.elapsedRealtime()
        }

        override fun onStart(owner: LifecycleOwner) {
            val wasInBackground = isAppInBackground
            isAppInBackground = false
            if (wasInBackground && lastBackgroundTimeMs > 0L) {
                val elapsed = clock.elapsedRealtime() - lastBackgroundTimeMs
                lastBackgroundTimeMs = 0L
                if (isBiometricEnabled && elapsed >= autoLockTimeoutMs) {
                    _isLocked.value = true
                }
            }
        }
    }
