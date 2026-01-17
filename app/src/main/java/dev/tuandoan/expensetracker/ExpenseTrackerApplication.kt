package dev.tuandoan.expensetracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.tuandoan.expensetracker.data.seed.SeedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ExpenseTrackerApplication : Application() {
    @Inject
    lateinit var seedRepository: SeedRepository

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Seed database with default categories on first run
        applicationScope.launch {
            seedRepository.seedDatabaseIfNeeded()
        }
    }
}
