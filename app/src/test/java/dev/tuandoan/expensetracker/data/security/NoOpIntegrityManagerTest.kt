package dev.tuandoan.expensetracker.data.security

import dev.tuandoan.expensetracker.domain.security.IntegrityResult
import dev.tuandoan.expensetracker.domain.security.IntegrityState
import dev.tuandoan.expensetracker.domain.security.RemediationResult
import dev.tuandoan.expensetracker.domain.security.RemediationType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoOpIntegrityManagerTest {
    private val manager = NoOpIntegrityManager()

    @Test
    fun initialState_isEvaluatedAndGenuine() {
        val state = manager.integrityState.value
        assertTrue(state is IntegrityState.Evaluated)
        val result = (state as IntegrityState.Evaluated).result
        assertTrue(result.isGenuine)
        assertTrue(result.isPlayStoreInstalled)
        assertTrue(result.isSignatureValid)
        assertTrue(result.isDeviceIntegrityMet)
    }

    @Test
    fun evaluateIntegrity_returnsGenuineResult() =
        runTest {
            val result = manager.evaluateIntegrity()
            assertEquals(
                IntegrityResult(
                    isGenuine = true,
                    isPlayStoreInstalled = true,
                    isSignatureValid = true,
                    isDeviceIntegrityMet = true,
                    isOfflineMode = false,
                    details = "No-op integrity provider",
                ),
                result,
            )
            val currentState = manager.integrityState.value
            assertEquals(IntegrityState.Evaluated(result), currentState)
        }

    @Test
    fun requestRemediation_returnsSuccessful() =
        runTest {
            val result = manager.requestRemediation(mockActivity(), RemediationType.GET_LICENSED)
            assertEquals(RemediationResult.SUCCESSFUL, result)
        }

    @Test
    fun warmUp_completesSilently() =
        runTest {
            manager.warmUp()
            assertTrue(manager.integrityState.value is IntegrityState.Evaluated)
        }

    private fun mockActivity(): android.app.Activity = org.mockito.kotlin.mock()
}
