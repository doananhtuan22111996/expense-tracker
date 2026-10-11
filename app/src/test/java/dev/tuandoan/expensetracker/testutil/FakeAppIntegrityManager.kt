package dev.tuandoan.expensetracker.testutil

import android.app.Activity
import dev.tuandoan.expensetracker.domain.security.AppIntegrityManager
import dev.tuandoan.expensetracker.domain.security.IntegrityResult
import dev.tuandoan.expensetracker.domain.security.IntegrityState
import dev.tuandoan.expensetracker.domain.security.RemediationResult
import dev.tuandoan.expensetracker.domain.security.RemediationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAppIntegrityManager(
    initialState: IntegrityState = IntegrityState.Unchecked,
) : AppIntegrityManager {
    val integrityStateFlow = MutableStateFlow(initialState)
    override val integrityState: StateFlow<IntegrityState> = integrityStateFlow.asStateFlow()

    var warmUpCallCount: Int = 0
    var evaluateIntegrityCallCount: Int = 0
    val requestedRemediationTypes: MutableList<RemediationType> = mutableListOf()
    var remediationResultToReturn: RemediationResult = RemediationResult.SUCCESSFUL
    var resultToReturn: IntegrityResult =
        IntegrityResult(
            isGenuine = true,
            isPlayStoreInstalled = true,
            isSignatureValid = true,
            isDeviceIntegrityMet = true,
            isOfflineMode = false,
        )

    override suspend fun warmUp() {
        warmUpCallCount++
    }

    override suspend fun evaluateIntegrity(): IntegrityResult {
        evaluateIntegrityCallCount++
        integrityStateFlow.value = IntegrityState.Evaluated(resultToReturn)
        return resultToReturn
    }

    override suspend fun requestRemediation(
        activity: Activity,
        typeCode: RemediationType,
    ): RemediationResult {
        requestedRemediationTypes.add(typeCode)
        return remediationResultToReturn
    }
}
