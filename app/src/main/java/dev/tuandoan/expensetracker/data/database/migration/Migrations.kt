package dev.tuandoan.expensetracker.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 1 to version 2.
 *
 * Adds currency_code column to the transactions table.
 * All existing rows receive 'VND' as the default value, preserving current
 * single-currency (Vietnamese Dong) behavior.
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN currency_code TEXT NOT NULL DEFAULT 'VND'",
            )
        }
    }
