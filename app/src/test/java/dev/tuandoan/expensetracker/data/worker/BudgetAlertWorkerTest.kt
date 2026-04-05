package dev.tuandoan.expensetracker.data.worker

import android.content.Context
import androidx.work.WorkerParameters
import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.core.notification.NotificationHelper
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.CurrencySumRow
import dev.tuandoan.expensetracker.domain.crash.NoOpCrashReporter
import dev.tuandoan.expensetracker.domain.repository.BudgetAlertPreferences
import dev.tuandoan.expensetracker.domain.repository.BudgetPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetAlertWorkerTest {
    private lateinit var fakeBudgetPrefs: FakeBudgetPreferences
    private lateinit var fakeBudgetAlertPrefs: FakeBudgetAlertPreferences
    private lateinit var mockTransactionDao: TransactionDao
    private lateinit var mockNotificationHelper: NotificationHelper
    private lateinit var mockCurrencyFormatter: CurrencyFormatter
    private lateinit var clock: Clock
    private lateinit var zoneId: ZoneId

    // Fixed time: 2026-04-15T10:00:00Z
    private val fixedInstant = Instant.parse("2026-04-15T10:00:00Z")

    @Before
    fun setup() {
        fakeBudgetPrefs = FakeBudgetPreferences()
        fakeBudgetAlertPrefs = FakeBudgetAlertPreferences()
        mockTransactionDao = mock()
        mockNotificationHelper = mock()
        mockCurrencyFormatter = mock()
        zoneId = ZoneId.of("UTC")
        clock = Clock.fixed(fixedInstant, zoneId)

        whenever(mockCurrencyFormatter.format(any(), any())).thenReturn("100")
    }

    private fun createWorker(): BudgetAlertWorker {
        val context = mock<Context>()
        val workerParams = mock<WorkerParameters>()
        whenever(context.getString(any())).thenReturn("title")
        whenever(context.getString(any(), any(), any(), any())).thenReturn("message")
        whenever(context.applicationContext).thenReturn(context)

        return BudgetAlertWorker(
            context,
            workerParams,
            fakeBudgetPrefs,
            fakeBudgetAlertPrefs,
            mockTransactionDao,
            mockNotificationHelper,
            mockCurrencyFormatter,
            NoOpCrashReporter(),
            clock,
            zoneId,
        )
    }

    @Test
    fun checkBudgets_alertsDisabled_doesNothing() =
        runTest {
            fakeBudgetAlertPrefs.setAlertsEnabled(false)

            val worker = createWorker()
            worker.checkBudgets()

            verify(mockNotificationHelper, never()).showBudgetAlert(any(), any(), any())
            assertNull(fakeBudgetAlertPrefs.lastAlertMonthValue)
        }

    @Test
    fun checkBudgets_noBudgets_doesNothing() =
        runTest {
            fakeBudgetAlertPrefs.setAlertsEnabled(true)
            // No budgets set

            val worker = createWorker()
            worker.checkBudgets()

            verify(mockNotificationHelper, never()).showBudgetAlert(any(), any(), any())
        }

    @Test
    fun checkBudgets_spendingBelowThreshold_doesNothing() =
        runTest {
            fakeBudgetAlertPrefs.setAlertsEnabled(true)
            fakeBudgetPrefs.setBudget("VND", 1_000_000L)

            // 50% spent — below 80% threshold
            whenever(mockTransactionDao.getExpenseTotalsByCurrency(any(), any()))
                .thenReturn(listOf(CurrencySumRow("VND", 500_000L)))

            val worker = createWorker()
            worker.checkBudgets()

            verify(mockNotificationHelper, never()).showBudgetAlert(any(), any(), any())
        }

    @Test
    fun checkBudgets_warningThreshold_sendsWarningNotification() =
        runTest {
            fakeBudgetAlertPrefs.setAlertsEnabled(true)
            fakeBudgetPrefs.setBudget("VND", 1_000_000L)

            // 85% spent — above 80% threshold
            whenever(mockTransactionDao.getExpenseTotalsByCurrency(any(), any()))
                .thenReturn(listOf(CurrencySumRow("VND", 850_000L)))

            val worker = createWorker()
            worker.checkBudgets()

            verify(mockNotificationHelper).showBudgetAlert(
                any(),
                any(),
                eq(NotificationHelper.NOTIFICATION_ID_BUDGET_WARNING),
            )
            assertEquals("2026-04", fakeBudgetAlertPrefs.lastAlertMonthValue)
            assertEquals("WARNING", fakeBudgetAlertPrefs.lastAlertLevelValue)
        }

    @Test
    fun checkBudgets_exceededThreshold_sendsExceededNotification() =
        runTest {
            fakeBudgetAlertPrefs.setAlertsEnabled(true)
            fakeBudgetPrefs.setBudget("VND", 1_000_000L)

            // 110% spent — over budget
            whenever(mockTransactionDao.getExpenseTotalsByCurrency(any(), any()))
                .thenReturn(listOf(CurrencySumRow("VND", 1_100_000L)))

            val worker = createWorker()
            worker.checkBudgets()

            verify(mockNotificationHelper).showBudgetAlert(
                any(),
                any(),
                eq(NotificationHelper.NOTIFICATION_ID_BUDGET_EXCEEDED),
            )
            assertEquals("2026-04", fakeBudgetAlertPrefs.lastAlertMonthValue)
            assertEquals("OVER_BUDGET", fakeBudgetAlertPrefs.lastAlertLevelValue)
        }

    @Test
    fun checkBudgets_sameMonthSameLevel_skips() =
        runTest {
            fakeBudgetAlertPrefs.setAlertsEnabled(true)
            fakeBudgetAlertPrefs.setLastAlertMonth("2026-04")
            fakeBudgetAlertPrefs.setLastAlertLevel("WARNING")
            fakeBudgetPrefs.setBudget("VND", 1_000_000L)

            // Still at 85% — WARNING again, same level
            whenever(mockTransactionDao.getExpenseTotalsByCurrency(any(), any()))
                .thenReturn(listOf(CurrencySumRow("VND", 850_000L)))

            val worker = createWorker()
            worker.checkBudgets()

            verify(mockNotificationHelper, never()).showBudgetAlert(any(), any(), any())
        }

    @Test
    fun checkBudgets_sameMonthEscalation_sendsNotification() =
        runTest {
            fakeBudgetAlertPrefs.setAlertsEnabled(true)
            fakeBudgetAlertPrefs.setLastAlertMonth("2026-04")
            fakeBudgetAlertPrefs.setLastAlertLevel("WARNING")
            fakeBudgetPrefs.setBudget("VND", 1_000_000L)

            // Now 110% — escalation from WARNING to OVER_BUDGET
            whenever(mockTransactionDao.getExpenseTotalsByCurrency(any(), any()))
                .thenReturn(listOf(CurrencySumRow("VND", 1_100_000L)))

            val worker = createWorker()
            worker.checkBudgets()

            verify(mockNotificationHelper).showBudgetAlert(
                any(),
                any(),
                eq(NotificationHelper.NOTIFICATION_ID_BUDGET_EXCEEDED),
            )
            assertEquals("OVER_BUDGET", fakeBudgetAlertPrefs.lastAlertLevelValue)
        }

    @Test
    fun checkBudgets_newMonth_resetsAndEvaluates() =
        runTest {
            fakeBudgetAlertPrefs.setAlertsEnabled(true)
            fakeBudgetAlertPrefs.setLastAlertMonth("2026-03") // Last month
            fakeBudgetAlertPrefs.setLastAlertLevel("OVER_BUDGET")
            fakeBudgetPrefs.setBudget("VND", 1_000_000L)

            // New month, 85% — should alert even though last month was OVER_BUDGET
            whenever(mockTransactionDao.getExpenseTotalsByCurrency(any(), any()))
                .thenReturn(listOf(CurrencySumRow("VND", 850_000L)))

            val worker = createWorker()
            worker.checkBudgets()

            verify(mockNotificationHelper).showBudgetAlert(
                any(),
                any(),
                eq(NotificationHelper.NOTIFICATION_ID_BUDGET_WARNING),
            )
            assertEquals("2026-04", fakeBudgetAlertPrefs.lastAlertMonthValue)
            assertEquals("WARNING", fakeBudgetAlertPrefs.lastAlertLevelValue)
        }

    @Test
    fun checkBudgets_multipleCurrencies_alertsWorstStatus() =
        runTest {
            fakeBudgetAlertPrefs.setAlertsEnabled(true)
            fakeBudgetPrefs.setBudget("VND", 1_000_000L)
            fakeBudgetPrefs.setBudget("USD", 10_000L)

            // VND at 85% (WARNING), USD at 110% (OVER_BUDGET)
            whenever(mockTransactionDao.getExpenseTotalsByCurrency(any(), any()))
                .thenReturn(
                    listOf(
                        CurrencySumRow("VND", 850_000L),
                        CurrencySumRow("USD", 11_000L),
                    ),
                )

            val worker = createWorker()
            worker.checkBudgets()

            // Should send OVER_BUDGET (worst status)
            verify(mockNotificationHelper).showBudgetAlert(
                any(),
                any(),
                eq(NotificationHelper.NOTIFICATION_ID_BUDGET_EXCEEDED),
            )
            assertEquals("OVER_BUDGET", fakeBudgetAlertPrefs.lastAlertLevelValue)
        }

    @Test
    fun parseBudgetStatusLevel_validName_returnsLevel() {
        assertEquals(
            dev.tuandoan.expensetracker.domain.model.BudgetStatusLevel.WARNING,
            BudgetAlertWorker.parseBudgetStatusLevel("WARNING"),
        )
        assertEquals(
            dev.tuandoan.expensetracker.domain.model.BudgetStatusLevel.OVER_BUDGET,
            BudgetAlertWorker.parseBudgetStatusLevel("OVER_BUDGET"),
        )
    }

    @Test
    fun parseBudgetStatusLevel_invalidName_returnsNull() {
        assertNull(BudgetAlertWorker.parseBudgetStatusLevel("INVALID"))
    }

    // --- Fakes ---

    private class FakeBudgetPreferences : BudgetPreferences {
        private val budgets = mutableMapOf<String, Long>()
        private val budgetsFlow = MutableStateFlow<Map<String, Long>>(emptyMap())

        override fun getBudget(currencyCode: String): Flow<Long?> = MutableStateFlow(budgets[currencyCode])

        override suspend fun setBudget(
            currencyCode: String,
            amount: Long,
        ) {
            budgets[currencyCode] = amount
            budgetsFlow.value = budgets.toMap()
        }

        override suspend fun clearBudget(currencyCode: String) {
            budgets.remove(currencyCode)
            budgetsFlow.value = budgets.toMap()
        }

        override fun getAllBudgets(): Flow<Map<String, Long>> = budgetsFlow
    }

    private class FakeBudgetAlertPreferences : BudgetAlertPreferences {
        private val enabledState = MutableStateFlow(false)
        private val lastAlertMonthState = MutableStateFlow<String?>(null)
        private val lastAlertLevelState = MutableStateFlow<String?>(null)

        val lastAlertMonthValue: String? get() = lastAlertMonthState.value
        val lastAlertLevelValue: String? get() = lastAlertLevelState.value

        override val alertsEnabled: Flow<Boolean> = enabledState
        override val lastAlertMonth: Flow<String?> = lastAlertMonthState
        override val lastAlertLevel: Flow<String?> = lastAlertLevelState

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
}
