package dev.tuandoan.expensetracker.domain.security

import androidx.lifecycle.LifecycleOwner
import dev.tuandoan.expensetracker.data.preferences.SecurityPreferences
import dev.tuandoan.expensetracker.testutil.FakeSecurityPreferences
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockManagerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var currentClockMs = 1_000_000L
    private val fakeClock = ElapsedRealtimeClock { currentClockMs }
    private val mockLifecycleOwner = Mockito.mock(LifecycleOwner::class.java)

    @Test
    fun coldStart_biometricDisabled_startsUnlocked() =
        runTest(mainDispatcherRule.testDispatcher) {
            val prefs = FakeSecurityPreferences(initialBiometric = false)
            val manager = AppLockManager(prefs, mainDispatcherRule.testDispatcher, fakeClock)
            advanceUntilIdle()

            assertTrue(manager.isInitialized.value)
            assertFalse(manager.isLocked.value)
            assertFalse(manager.isBiometricEnabled)
        }

    @Test
    fun coldStart_biometricEnabled_startsLocked() =
        runTest(mainDispatcherRule.testDispatcher) {
            val prefs = FakeSecurityPreferences(initialBiometric = true)
            val manager = AppLockManager(prefs, mainDispatcherRule.testDispatcher, fakeClock)
            advanceUntilIdle()

            assertTrue(manager.isInitialized.value)
            assertTrue(manager.isLocked.value)
            assertTrue(manager.isBiometricEnabled)
        }

    @Test
    fun unlock_setsIsLockedFalse() =
        runTest(mainDispatcherRule.testDispatcher) {
            val prefs = FakeSecurityPreferences(initialBiometric = true)
            val manager = AppLockManager(prefs, mainDispatcherRule.testDispatcher, fakeClock)
            advanceUntilIdle()
            assertTrue(manager.isLocked.value)

            manager.unlock()
            assertFalse(manager.isLocked.value)
        }

    @Test
    fun lock_whenBiometricEnabled_setsIsLockedTrue() =
        runTest(mainDispatcherRule.testDispatcher) {
            val prefs = FakeSecurityPreferences(initialBiometric = true)
            val manager = AppLockManager(prefs, mainDispatcherRule.testDispatcher, fakeClock)
            advanceUntilIdle()

            manager.unlock()
            assertFalse(manager.isLocked.value)

            manager.lock()
            assertTrue(manager.isLocked.value)
        }

    @Test
    fun lock_whenBiometricDisabled_remainsUnlocked() =
        runTest(mainDispatcherRule.testDispatcher) {
            val prefs = FakeSecurityPreferences(initialBiometric = false)
            val manager = AppLockManager(prefs, mainDispatcherRule.testDispatcher, fakeClock)
            advanceUntilIdle()

            manager.lock()
            assertFalse(manager.isLocked.value)
        }

    @Test
    fun backgrounding_exceedingTimeout_locksOnForeground() =
        runTest(mainDispatcherRule.testDispatcher) {
            val prefs =
                FakeSecurityPreferences(
                    initialBiometric = true,
                    initialTimeout = SecurityPreferences.TIMEOUT_ONE_MINUTE,
                )
            val manager = AppLockManager(prefs, mainDispatcherRule.testDispatcher, fakeClock)
            advanceUntilIdle()

            manager.unlock()
            assertFalse(manager.isLocked.value)

            // App goes to background at t = 1,000,000
            currentClockMs = 1_000_000L
            manager.onStop(mockLifecycleOwner)

            // App returns to foreground at t = 1,070,000 (70s elapsed > 60s)
            currentClockMs = 1_070_000L
            manager.onStart(mockLifecycleOwner)

            assertTrue(manager.isLocked.value)
        }

    @Test
    fun backgrounding_withinTimeout_remainsUnlockedOnForeground() =
        runTest(mainDispatcherRule.testDispatcher) {
            val prefs =
                FakeSecurityPreferences(
                    initialBiometric = true,
                    initialTimeout = SecurityPreferences.TIMEOUT_ONE_MINUTE,
                )
            val manager = AppLockManager(prefs, mainDispatcherRule.testDispatcher, fakeClock)
            advanceUntilIdle()

            manager.unlock()
            assertFalse(manager.isLocked.value)

            // App goes to background at t = 1,000,000
            currentClockMs = 1_000_000L
            manager.onStop(mockLifecycleOwner)

            // App returns to foreground at t = 1,030,000 (30s elapsed < 60s)
            currentClockMs = 1_030_000L
            manager.onStart(mockLifecycleOwner)

            assertFalse(manager.isLocked.value)
        }

    @Test
    fun backgrounding_immediateTimeout_locksOnForeground() =
        runTest(mainDispatcherRule.testDispatcher) {
            val prefs =
                FakeSecurityPreferences(
                    initialBiometric = true,
                    initialTimeout = SecurityPreferences.TIMEOUT_IMMEDIATELY,
                )
            val manager = AppLockManager(prefs, mainDispatcherRule.testDispatcher, fakeClock)
            advanceUntilIdle()

            manager.unlock()
            assertFalse(manager.isLocked.value)

            // App goes to background at t = 1,000,000
            currentClockMs = 1_000_000L
            manager.onStop(mockLifecycleOwner)

            // App returns to foreground at t = 1,000,001 (1ms elapsed >= 0ms)
            currentClockMs = 1_000_001L
            manager.onStart(mockLifecycleOwner)

            assertTrue(manager.isLocked.value)
        }

    @Test
    fun disablingBiometric_unlocksImmediately() =
        runTest(mainDispatcherRule.testDispatcher) {
            val prefs = FakeSecurityPreferences(initialBiometric = true)
            val manager = AppLockManager(prefs, mainDispatcherRule.testDispatcher, fakeClock)
            advanceUntilIdle()
            assertTrue(manager.isLocked.value)

            prefs.setBiometricEnabled(false)
            advanceUntilIdle()

            assertFalse(manager.isLocked.value)
            assertFalse(manager.isBiometricEnabled)
        }
}
