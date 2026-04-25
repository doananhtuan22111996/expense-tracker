package dev.tuandoan.expensetracker.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.domain.repository.BudgetPreferences
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository

/**
 * Hilt `@EntryPoint` for the home-screen widget.
 *
 * [ExpenseWidget] doesn't participate in the Activity/Fragment/ViewModel
 * Hilt graph — it's instantiated by Glance from a stateless broadcast
 * receiver. This entry point pulls Singleton-scoped dependencies off the
 * application graph so `provideGlance` can read transactions, budget, and
 * currency preference without requiring a full Hilt-injected widget.
 *
 * Pattern mirrors Task Tracker v1.5.0's `WidgetEntryPoint`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun transactionRepository(): TransactionRepository

    fun budgetPreferences(): BudgetPreferences

    fun currencyPreferenceRepository(): CurrencyPreferenceRepository

    fun currencyFormatter(): CurrencyFormatter

    fun timeProvider(): TimeProvider

    companion object {
        fun get(context: Context): WidgetEntryPoint =
            EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
    }
}
