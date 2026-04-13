package dev.tuandoan.expensetracker.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.data.database.AppDatabase
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.GoldHoldingDao
import dev.tuandoan.expensetracker.data.database.dao.GoldPriceDao
import dev.tuandoan.expensetracker.data.database.dao.RecurringTransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.migration.MIGRATION_1_2
import dev.tuandoan.expensetracker.data.database.migration.MIGRATION_2_3
import dev.tuandoan.expensetracker.data.database.migration.MIGRATION_3_4
import dev.tuandoan.expensetracker.data.database.migration.MIGRATION_4_5
import dev.tuandoan.expensetracker.data.database.migration.MIGRATION_5_6
import dev.tuandoan.expensetracker.data.database.migration.MIGRATION_6_7
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
                name = "expense_tracker_database",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideRecurringTransactionDao(database: AppDatabase): RecurringTransactionDao =
        database.recurringTransactionDao()

    @Provides
    fun provideGoldHoldingDao(database: AppDatabase): GoldHoldingDao = database.goldHoldingDao()

    @Provides
    fun provideGoldPriceDao(database: AppDatabase): GoldPriceDao = database.goldPriceDao()

    @Provides
    fun provideContentResolver(
        @ApplicationContext context: Context,
    ): ContentResolver = context.contentResolver
}
