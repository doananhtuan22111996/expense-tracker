package dev.tuandoan.expensetracker.data.preferences

import dev.tuandoan.expensetracker.testutil.FakeSecurityPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecurityPreferencesTest {
    private lateinit var preferences: FakeSecurityPreferences

    @Before
    fun setup() {
        preferences = FakeSecurityPreferences()
    }

    @Test
    fun defaultValues_areCorrect() =
        runTest {
            assertFalse(preferences.isBiometricEnabled.first())
            assertEquals(SecurityPreferences.TIMEOUT_IMMEDIATELY, preferences.autoLockTimeoutMs.first())
            assertFalse(preferences.isHideInRecentsEnabled.first())
        }

    @Test
    fun setBiometricEnabled_updatesFlow() =
        runTest {
            preferences.setBiometricEnabled(true)
            assertTrue(preferences.isBiometricEnabled.first())

            preferences.setBiometricEnabled(false)
            assertFalse(preferences.isBiometricEnabled.first())
        }

    @Test
    fun setAutoLockTimeoutMs_updatesFlow() =
        runTest {
            preferences.setAutoLockTimeoutMs(SecurityPreferences.TIMEOUT_ONE_MINUTE)
            assertEquals(SecurityPreferences.TIMEOUT_ONE_MINUTE, preferences.autoLockTimeoutMs.first())

            preferences.setAutoLockTimeoutMs(SecurityPreferences.TIMEOUT_FIVE_MINUTES)
            assertEquals(SecurityPreferences.TIMEOUT_FIVE_MINUTES, preferences.autoLockTimeoutMs.first())
        }

    @Test
    fun setHideInRecentsEnabled_updatesFlow() =
        runTest {
            preferences.setHideInRecentsEnabled(true)
            assertTrue(preferences.isHideInRecentsEnabled.first())

            preferences.setHideInRecentsEnabled(false)
            assertFalse(preferences.isHideInRecentsEnabled.first())
        }
}
