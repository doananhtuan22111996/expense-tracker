package dev.tuandoan.expensetracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.core.formatter.DefaultCurrencyFormatter
import dev.tuandoan.expensetracker.core.util.SystemTimeProvider
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.preferences.CurrencyPreferenceRepositoryImpl
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import dev.tuandoan.expensetracker.repository.CategoryRepositoryImpl
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
}
