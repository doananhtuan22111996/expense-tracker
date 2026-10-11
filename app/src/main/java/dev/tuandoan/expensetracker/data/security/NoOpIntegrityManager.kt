package dev.tuandoan.expensetracker.data.security

import android.app.Activity
import dev.tuandoan.expensetracker.domain.security.AppIntegrityManager
import dev.tuandoan.expensetracker.domain.security.IntegrityResult
import dev.tuandoan.expensetracker.domain.security.IntegrityState
import dev.tuandoan.expensetracker.domain.security.RemediationResult
import dev.tuandoan.expensetracker.domain.security.RemediationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * No-op implementation of [AppIntegrityManager] used for tests and debug environments.
 * Always reports genuine status so development and emulator workflows remain smooth.
 */
@Singleton
class NoOpIntegrityManager
    @Inject
    constructor() : AppIntegrityManager {
        private val _integrityState =
            MutableStateFlow<IntegrityState>(
                IntegrityState.Evaluated(
                    IntegrityResult(
                        isGenuine = true,
                        isPlayStoreInstalled = true,
                        isSignatureValid = true,
                        isDeviceIntegrityMet = true,
                        isOfflineMode = false,
                        details = "No-op integrity provider",
                    ),
                ),
            )

        override val integrityState: StateFlow<IntegrityState> = _integrityState.asStateFlow()

        override suspend fun warmUp() {
            // No-op
        }

        override suspend fun evaluateIntegrity(): IntegrityResult {
            val result =
                IntegrityResult(
                    isGenuine = true,
                    isPlayStoreInstalled = true,
                    isSignatureValid = true,
                    isDeviceIntegrityMet = true,
                    isOfflineMode = false,
                    details = "No-op integrity provider",
                )
            _integrityState.value = IntegrityState.Evaluated(result)
            return result
        }

        override suspend fun requestRemediation(
            activity: Activity,
            typeCode: RemediationType,
        ): RemediationResult = RemediationResult.SUCCESSFUL
    }
