package dev.tuandoan.expensetracker.data.security

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.tuandoan.expensetracker.domain.security.IntegrityState
import dev.tuandoan.expensetracker.domain.security.RemediationResult
import dev.tuandoan.expensetracker.domain.security.RemediationType
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlayIntegrityManagerImplTest {
    private val context: Context = mock()
    private val testDispatcher = StandardTestDispatcher()
    private val connectivityManager: ConnectivityManager = mock()
    private val standardIntegrityManager: com.google.android.play.core.integrity.StandardIntegrityManager = mock()
    private lateinit var uriMockedStatic: MockedStatic<android.net.Uri>

    private lateinit var manager: PlayIntegrityManagerImpl

    @Before
    fun setUp() {
        uriMockedStatic = mockStatic(android.net.Uri::class.java)
        val mockUri: android.net.Uri = mock()
        uriMockedStatic.`when`<android.net.Uri> { android.net.Uri.parse(any()) }.thenReturn(mockUri)

        whenever(context.packageName).thenReturn("dev.tuandoan.expensetracker")
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)

        manager = PlayIntegrityManagerImpl(context, testDispatcher) { standardIntegrityManager }
    }

    @After
    fun tearDown() {
        uriMockedStatic.close()
    }

    @Test
    fun initialState_isUnchecked() {
        assertEquals(IntegrityState.Unchecked, manager.integrityState.value)
    }

    @Test
    fun evaluateIntegrity_whenOffline_returnsOfflineGenuineResult() =
        runTest(testDispatcher) {
            // Mock offline: activeNetwork is null
            whenever(connectivityManager.activeNetwork).thenReturn(null)

            val result = manager.evaluateIntegrity()

            assertTrue(result.isGenuine)
            assertTrue(result.isOfflineMode)
            assertTrue(result.isDeviceIntegrityMet)

            val state = manager.integrityState.value
            assertTrue(state is IntegrityState.Evaluated)
            assertEquals(result, (state as IntegrityState.Evaluated).result)
        }

    @Test
    fun evaluateIntegrity_whenNoInternetCapability_returnsOfflineGenuineResult() =
        runTest(testDispatcher) {
            val network: android.net.Network = mock()
            val capabilities: NetworkCapabilities = mock()

            whenever(connectivityManager.activeNetwork).thenReturn(network)
            whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(capabilities)
            whenever(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(false)

            val result = manager.evaluateIntegrity()

            assertTrue(result.isGenuine)
            assertTrue(result.isOfflineMode)
        }

    @Test
    fun requestRemediation_withGetLicensed_startsPlayStoreActivity() =
        runTest(testDispatcher) {
            val activity: Activity = mock()

            val outcome = manager.requestRemediation(activity, RemediationType.GET_LICENSED)

            assertEquals(RemediationResult.SUCCESSFUL, outcome)
            verify(activity).startActivity(any<Intent>())
        }

    @Test
    fun requestRemediation_whenMarketActivityNotFound_fallsBackToBrowser() =
        runTest(testDispatcher) {
            val activity: Activity = mock()
            var callCount = 0

            doAnswer {
                callCount++
                if (callCount == 1) {
                    throw android.content.ActivityNotFoundException("No market app")
                }
                null
            }.whenever(activity).startActivity(any<Intent>())

            val outcome = manager.requestRemediation(activity, RemediationType.GET_LICENSED)

            assertEquals(RemediationResult.SUCCESSFUL, outcome)
            assertEquals(2, callCount)
        }

    @Test
    fun requestRemediation_whenAllIntentsFail_returnsFailed() =
        runTest(testDispatcher) {
            val activity: Activity = mock()
            doThrow(RuntimeException("Cannot launch any activity")).whenever(activity).startActivity(any<Intent>())

            val outcome = manager.requestRemediation(activity, RemediationType.GET_LICENSED)

            assertEquals(RemediationResult.FAILED, outcome)
        }

    @Test
    fun requestRemediation_withUnknownType_returnsUnavailable() =
        runTest(testDispatcher) {
            val activity: Activity = mock()

            val outcome = manager.requestRemediation(activity, RemediationType.CLOSE_RISKY_APPS)

            assertEquals(RemediationResult.UNAVAILABLE, outcome)
        }

    @Test
    fun warmUp_whenPlayServicesFails_doesNotCrash() =
        runTest(testDispatcher) {
            val failedTask =
                com.google.android.gms.tasks.Tasks.forException<
                    com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider,
                >(
                    RuntimeException("Play Services unavailable"),
                )
            whenever(standardIntegrityManager.prepareIntegrityToken(any())).thenReturn(failedTask)

            manager.warmUp()
            assertEquals(IntegrityState.Unchecked, manager.integrityState.value)
        }

    @Test
    fun warmUp_whenPlayServicesSucceeds_cachesProvider() =
        runTest(testDispatcher) {
            val mockProvider:
                com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider =
                mock()
            val successTask =
                com.google.android.gms.tasks.Tasks
                    .forResult(mockProvider)
            whenever(standardIntegrityManager.prepareIntegrityToken(any())).thenReturn(successTask)

            manager.warmUp()
            // Subsequent warmUp returns immediately without re-calling prepareIntegrityToken
            manager.warmUp()
            verify(standardIntegrityManager).prepareIntegrityToken(any())
        }
}
