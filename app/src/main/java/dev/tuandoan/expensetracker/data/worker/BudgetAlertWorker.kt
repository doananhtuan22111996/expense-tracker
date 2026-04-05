package dev.tuandoan.expensetracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.core.notification.NotificationHelper
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import dev.tuandoan.expensetracker.domain.model.BudgetStatus
import dev.tuandoan.expensetracker.domain.model.BudgetStatusLevel
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.BudgetAlertPreferences
import dev.tuandoan.expensetracker.domain.repository.BudgetPreferences
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * WorkManager worker that checks monthly budgets against spending and sends
 * notifications when thresholds are reached (80% warning, 100% exceeded).
 *
 * Dedup logic:
 * - Tracks [lastAlertMonth] and [lastAlertLevel] in [BudgetAlertPreferences].
 * - Same month + same level → skip (already alerted).
 * - Same month + higher level (WARNING → OVER_BUDGET) → escalate and send.
 * - New month → reset and evaluate fresh.
 */
@HiltWorker
class BudgetAlertWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val budgetPreferences: BudgetPreferences,
        private val budgetAlertPreferences: BudgetAlertPreferences,
        private val transactionDao: TransactionDao,
        private val notificationHelper: NotificationHelper,
        private val currencyFormatter: CurrencyFormatter,
        private val crashReporter: CrashReporter,
        private val clock: Clock,
        private val zoneId: ZoneId,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result =
            @Suppress("TooGenericExceptionCaught")
            try {
                checkBudgets()
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(e)
                Result.retry()
            }

        internal suspend fun checkBudgets() {
            val alertsEnabled = budgetAlertPreferences.alertsEnabled.first()
            if (!alertsEnabled) return

            val allBudgets = budgetPreferences.getAllBudgets().first()
            if (allBudgets.isEmpty()) return

            val now = ZonedDateTime.now(clock.withZone(zoneId))
            val currentYearMonth = YearMonth.from(now)
            val currentMonthStr = currentYearMonth.toString() // "YYYY-MM"

            val monthStart =
                currentYearMonth
                    .atDay(1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
            val monthEnd =
                currentYearMonth
                    .plusMonths(1)
                    .atDay(1)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()

            val expenseTotals = transactionDao.getExpenseTotalsByCurrency(monthStart, monthEnd)
            val expenseMap = expenseTotals.associate { it.currencyCode to it.total }

            val lastMonth = budgetAlertPreferences.lastAlertMonth.first()
            val lastLevel = budgetAlertPreferences.lastAlertLevel.first()
            val previousLevel =
                if (lastMonth == currentMonthStr) {
                    lastLevel?.let { parseBudgetStatusLevel(it) }
                } else {
                    null
                }

            // Find the worst budget status across all currencies
            var worstStatus: BudgetStatus? = null
            for ((currencyCode, budgetAmount) in allBudgets) {
                val spent = expenseMap[currencyCode] ?: 0L
                val currency = SupportedCurrencies.byCode(currencyCode) ?: continue
                val status = BudgetStatus(currency = currency, budgetAmount = budgetAmount, spentAmount = spent)

                if (status.status == BudgetStatusLevel.OK) continue

                if (worstStatus == null || status.status.ordinal > worstStatus.status.ordinal) {
                    worstStatus = status
                }
            }

            if (worstStatus == null) return

            // Dedup: skip if already alerted at this level or higher this month
            if (previousLevel != null && worstStatus.status.ordinal <= previousLevel.ordinal) return

            sendNotification(worstStatus)

            budgetAlertPreferences.setLastAlertMonth(currentMonthStr)
            budgetAlertPreferences.setLastAlertLevel(worstStatus.status.name)
        }

        private fun sendNotification(budgetStatus: BudgetStatus) {
            val currencyCode = budgetStatus.currency.code
            val spentFormatted = currencyFormatter.format(budgetStatus.spentAmount, currencyCode)
            val budgetFormatted = currencyFormatter.format(budgetStatus.budgetAmount, currencyCode)

            when (budgetStatus.status) {
                BudgetStatusLevel.WARNING -> {
                    val percentage = (budgetStatus.progressFraction * 100).toInt()
                    notificationHelper.showBudgetAlert(
                        title = applicationContext.getString(R.string.notification_budget_warning_title),
                        message =
                            applicationContext.getString(
                                R.string.notification_budget_warning_body,
                                spentFormatted,
                                budgetFormatted,
                                percentage,
                            ),
                        notificationId = NotificationHelper.NOTIFICATION_ID_BUDGET_WARNING,
                    )
                }
                BudgetStatusLevel.OVER_BUDGET -> {
                    val overAmount = budgetStatus.spentAmount - budgetStatus.budgetAmount
                    val overFormatted = currencyFormatter.format(overAmount, currencyCode)
                    notificationHelper.showBudgetAlert(
                        title = applicationContext.getString(R.string.notification_budget_exceeded_title),
                        message =
                            applicationContext.getString(
                                R.string.notification_budget_exceeded_body,
                                spentFormatted,
                                budgetFormatted,
                                overFormatted,
                            ),
                        notificationId = NotificationHelper.NOTIFICATION_ID_BUDGET_EXCEEDED,
                    )
                }
                BudgetStatusLevel.OK -> { /* No notification needed */ }
            }
        }

        companion object {
            const val WORK_NAME = "budget_alert_worker"
            const val PERIODIC_WORK_NAME = "budget_alert_periodic"
            const val IMMEDIATE_WORK_NAME = "budget_alert_immediate"

            internal fun parseBudgetStatusLevel(name: String): BudgetStatusLevel? =
                try {
                    BudgetStatusLevel.valueOf(name)
                } catch (_: IllegalArgumentException) {
                    null
                }
        }
    }
