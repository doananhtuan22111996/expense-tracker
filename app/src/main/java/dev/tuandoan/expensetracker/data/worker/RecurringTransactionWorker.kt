package dev.tuandoan.expensetracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * WorkManager worker that processes due recurring transactions in the background.
 * Delegates to [RecurringTransactionRepository] to maintain MVVM layering.
 */
@HiltWorker
class RecurringTransactionWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val recurringTransactionRepository: RecurringTransactionRepository,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result =
            @Suppress("TooGenericExceptionCaught")
            try {
                recurringTransactionRepository.processDueRecurring()
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Result.retry()
            }

        companion object {
            const val WORK_NAME = "recurring_transaction_worker"
            const val PERIODIC_WORK_NAME = "recurring_transaction_periodic"
        }
    }
