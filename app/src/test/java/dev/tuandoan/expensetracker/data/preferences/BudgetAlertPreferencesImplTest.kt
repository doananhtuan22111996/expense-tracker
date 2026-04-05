package dev.tuandoan.expensetracker.data.preferences

import dev.tuandoan.expensetracker.domain.repository.BudgetAlertPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BudgetAlertPreferencesImplTest {
    private lateinit var preferences: FakeBudgetAlertPreferences

    @Before
    fun setup() {
        preferences = FakeBudgetAlertPreferences()
    }

    // --- alertsEnabled ---

    @Test
    fun defaultAlertsEnabled_isFalse() =
        runTest {
            assertFalse(preferences.alertsEnabled.first())
        }

    @Test
    fun setAlertsEnabled_true_updatesValue() =
        runTest {
            preferences.setAlertsEnabled(true)

            assertEquals(true, preferences.alertsEnabled.first())
        }

    @Test
    fun setAlertsEnabled_trueThenFalse_returnsFalse() =
        runTest {
            preferences.setAlertsEnabled(true)
            preferences.setAlertsEnabled(false)

            assertFalse(preferences.alertsEnabled.first())
        }

    // --- lastAlertMonth ---

    @Test
    fun defaultLastAlertMonth_isNull() =
        runTest {
            assertNull(preferences.lastAlertMonth.first())
        }

    @Test
    fun setLastAlertMonth_updatesValue() =
        runTest {
            preferences.setLastAlertMonth("2026-04")

            assertEquals("2026-04", preferences.lastAlertMonth.first())
        }

    @Test
    fun setLastAlertMonth_overwritesPreviousValue() =
        runTest {
            preferences.setLastAlertMonth("2026-03")
            preferences.setLastAlertMonth("2026-04")

            assertEquals("2026-04", preferences.lastAlertMonth.first())
        }

    // --- lastAlertLevel ---

    @Test
    fun defaultLastAlertLevel_isNull() =
        runTest {
            assertNull(preferences.lastAlertLevel.first())
        }

    @Test
    fun setLastAlertLevel_updatesValue() =
        runTest {
            preferences.setLastAlertLevel("WARNING")

            assertEquals("WARNING", preferences.lastAlertLevel.first())
        }

    @Test
    fun setLastAlertLevel_overwritesPreviousValue() =
        runTest {
            preferences.setLastAlertLevel("WARNING")
            preferences.setLastAlertLevel("OVER_BUDGET")

            assertEquals("OVER_BUDGET", preferences.lastAlertLevel.first())
        }
}

/**
 * In-memory fake of [BudgetAlertPreferences] for unit testing.
 */
class FakeBudgetAlertPreferences : BudgetAlertPreferences {
    private val enabledState = MutableStateFlow(false)
    private val lastAlertMonthState = MutableStateFlow<String?>(null)
    private val lastAlertLevelState = MutableStateFlow<String?>(null)

    override val alertsEnabled: Flow<Boolean> = enabledState
    override val lastAlertMonth: Flow<String?> = lastAlertMonthState
    override val lastAlertLevel: Flow<String?> = lastAlertLevelState

    val lastAlertMonthValue: String? get() = lastAlertMonthState.value
    val lastAlertLevelValue: String? get() = lastAlertLevelState.value

    override suspend fun setAlertsEnabled(enabled: Boolean) {
        enabledState.value = enabled
    }

    override suspend fun setLastAlertMonth(yearMonth: String) {
        lastAlertMonthState.value = yearMonth
    }

    override suspend fun setLastAlertLevel(level: String) {
        lastAlertLevelState.value = level
    }
}
