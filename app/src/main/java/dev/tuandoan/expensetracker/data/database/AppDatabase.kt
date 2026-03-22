package dev.tuandoan.expensetracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.GoldHoldingDao
import dev.tuandoan.expensetracker.data.database.dao.GoldPriceDao
import dev.tuandoan.expensetracker.data.database.dao.RecurringTransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity
import dev.tuandoan.expensetracker.data.database.entity.RecurringTransactionEntity
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        RecurringTransactionEntity::class,
        GoldHoldingEntity::class,
        GoldPriceEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun recurringTransactionDao(): RecurringTransactionDao

    abstract fun goldHoldingDao(): GoldHoldingDao

    abstract fun goldPriceDao(): GoldPriceDao
}
