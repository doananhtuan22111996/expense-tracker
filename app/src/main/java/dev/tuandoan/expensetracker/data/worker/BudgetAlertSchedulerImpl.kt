package dev.tuandoan.expensetracker.data.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.domain.repository.BudgetAlertScheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetAlertSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BudgetAlertScheduler {
        override fun scheduleImmediateCheck() {
            val request = OneTimeWorkRequestBuilder<BudgetAlertWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                BudgetAlertWorker.IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
