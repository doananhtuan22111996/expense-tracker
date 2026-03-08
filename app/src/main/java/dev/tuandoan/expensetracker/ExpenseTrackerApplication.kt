package dev.tuandoan.expensetracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.tuandoan.expensetracker.data.seed.SeedRepository
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ExpenseTrackerApplication : Application() {
    @Inject
    lateinit var seedRepository: SeedRepository

    @Inject
    lateinit var recurringTransactionRepository: RecurringTransactionRepository

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Seed database with default categories on first run,
        // then process any due recurring transactions
        applicationScope.launch {
            seedRepository.seedDatabaseIfNeeded()
            @Suppress("TooGenericExceptionCaught")
            try {
                recurringTransactionRepository.processDueRecurring()
            } catch (_: Exception) {
                // Silent failure for background processing
            }
        }
    }
}
