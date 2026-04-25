package dev.tuandoan.expensetracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import dev.tuandoan.expensetracker.core.notification.NotificationHelper
import dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferences
import dev.tuandoan.expensetracker.data.seed.SeedRepository
import dev.tuandoan.expensetracker.data.worker.BudgetAlertWorker
import dev.tuandoan.expensetracker.data.worker.RecurringTransactionWorker
import dev.tuandoan.expensetracker.data.worker.WidgetRefreshWorker
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ExpenseTrackerApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var seedRepository: SeedRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var analyticsPreferences: AnalyticsPreferences

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val applicationScope = CoroutineScope(SupervisorJob())

    override val workManagerConfiguration: Configuration by lazy {
        Configuration
            .Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        notificationHelper.createChannels()

        // Seed database with default categories on first run,
        // then schedule recurring transaction processing via WorkManager
        applicationScope.launch {
            seedRepository.seedDatabaseIfNeeded()
            scheduleRecurringWork()
            scheduleBudgetAlertWork()
            scheduleWidgetRefreshWork()
        }

        // Observe analytics consent and enable/disable crash reporting accordingly.
        // Crash reporting is disabled by default until the user explicitly opts in.
        applicationScope.launch {
            analyticsPreferences.analyticsConsent.collect { consent ->
                crashReporter.setCollectionEnabled(consent)
            }
        }
    }

    private fun scheduleRecurringWork() {
        val workManager = WorkManager.getInstance(this)

        // One-time work on startup to process any due recurring transactions
        val oneTimeRequest = OneTimeWorkRequestBuilder<RecurringTransactionWorker>().build()
        workManager.enqueueUniqueWork(
            RecurringTransactionWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest,
        )

        // Daily periodic work to process recurring transactions in the background
        val periodicRequest =
            PeriodicWorkRequestBuilder<RecurringTransactionWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            RecurringTransactionWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
    }

    private fun scheduleBudgetAlertWork() {
        val workManager = WorkManager.getInstance(this)

        // One-time check on startup
        val oneTimeRequest = OneTimeWorkRequestBuilder<BudgetAlertWorker>().build()
        workManager.enqueueUniqueWork(
            BudgetAlertWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest,
        )

        // Daily periodic check for budget thresholds
        val periodicRequest =
            PeriodicWorkRequestBuilder<BudgetAlertWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            BudgetAlertWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
    }

    private fun scheduleWidgetRefreshWork() {
        val workManager = WorkManager.getInstance(this)

        // Periodic refresh every 30 minutes — backstops repository-write
        // hooks so the widget reflects day rollovers (new "Today" total) and
        // any external state changes when the app isn't foregrounded. No
        // one-time kick needed on startup because the widget's provideGlance
        // fetches fresh data on every render.
        val periodicRequest =
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(WIDGET_REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            WidgetRefreshWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
    }

    companion object {
        private const val WIDGET_REFRESH_INTERVAL_MINUTES = 30L
    }
}
