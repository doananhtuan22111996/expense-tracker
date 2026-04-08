package dev.tuandoan.expensetracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.core.formatter.DefaultCurrencyFormatter
import dev.tuandoan.expensetracker.core.util.SystemTimeProvider
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.backup.BackupRepositoryImpl
import dev.tuandoan.expensetracker.data.database.RoomTransactionRunner
import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.preferences.BudgetAlertPreferencesImpl
import dev.tuandoan.expensetracker.data.preferences.BudgetPreferencesImpl
import dev.tuandoan.expensetracker.data.preferences.CurrencyPreferenceRepositoryImpl
import dev.tuandoan.expensetracker.data.preferences.SearchScopePreferencesRepository
import dev.tuandoan.expensetracker.data.preferences.SearchScopePreferencesRepositoryImpl
import dev.tuandoan.expensetracker.data.preferences.SelectedMonthRepositoryImpl
import dev.tuandoan.expensetracker.data.worker.BudgetAlertSchedulerImpl
import dev.tuandoan.expensetracker.domain.repository.BackupRepository
import dev.tuandoan.expensetracker.domain.repository.BudgetAlertPreferences
import dev.tuandoan.expensetracker.domain.repository.BudgetAlertScheduler
import dev.tuandoan.expensetracker.domain.repository.BudgetPreferences
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.GoldRepository
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import dev.tuandoan.expensetracker.domain.repository.SelectedMonthRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import dev.tuandoan.expensetracker.repository.CategoryRepositoryImpl
import dev.tuandoan.expensetracker.repository.GoldRepositoryImpl
import dev.tuandoan.expensetracker.repository.RecurringTransactionRepositoryImpl
import dev.tuandoan.expensetracker.repository.TransactionRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTransactionRepository(transactionRepositoryImpl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    abstract fun bindCategoryRepository(categoryRepositoryImpl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun bindTimeProvider(systemTimeProvider: SystemTimeProvider): TimeProvider

    @Binds
    abstract fun bindCurrencyFormatter(defaultCurrencyFormatter: DefaultCurrencyFormatter): CurrencyFormatter

    @Binds
    abstract fun bindCurrencyPreferenceRepository(
        currencyPreferenceRepositoryImpl: CurrencyPreferenceRepositoryImpl,
    ): CurrencyPreferenceRepository

    @Binds
    abstract fun bindBackupRepository(backupRepositoryImpl: BackupRepositoryImpl): BackupRepository

    @Binds
    abstract fun bindTransactionRunner(roomTransactionRunner: RoomTransactionRunner): TransactionRunner

    @Binds
    abstract fun bindSelectedMonthRepository(
        selectedMonthRepositoryImpl: SelectedMonthRepositoryImpl,
    ): SelectedMonthRepository

    @Binds
    abstract fun bindBudgetPreferences(budgetPreferencesImpl: BudgetPreferencesImpl): BudgetPreferences

    @Binds
    abstract fun bindRecurringTransactionRepository(
        recurringTransactionRepositoryImpl: RecurringTransactionRepositoryImpl,
    ): RecurringTransactionRepository

    @Binds
    abstract fun bindGoldRepository(goldRepositoryImpl: GoldRepositoryImpl): GoldRepository

    @Binds
    abstract fun bindBudgetAlertPreferences(
        budgetAlertPreferencesImpl: BudgetAlertPreferencesImpl,
    ): BudgetAlertPreferences

    @Binds
    abstract fun bindBudgetAlertScheduler(budgetAlertSchedulerImpl: BudgetAlertSchedulerImpl): BudgetAlertScheduler

    @Binds
    abstract fun bindSearchScopePreferencesRepository(
        searchScopePreferencesRepositoryImpl: SearchScopePreferencesRepositoryImpl,
    ): SearchScopePreferencesRepository
}
