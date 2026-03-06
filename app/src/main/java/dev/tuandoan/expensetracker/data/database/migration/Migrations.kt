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

/**
 * Migration from version 2 to version 3.
 *
 * Adds indices on transactions.timestamp (for date-range queries)
 * and transactions.category_id (for FK cascade performance).
 * No data changes; existing rows are fully preserved.
 */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_category_id` ON `transactions` (`category_id`)",
            )
        }
    }

/**
 * Migration from version 3 to version 4.
 *
 * Adds the recurring_transactions table for template-based recurring
 * income and expense entries. No existing data is modified.
 */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `type` INTEGER NOT NULL,
                    `amount` INTEGER NOT NULL,
                    `currency_code` TEXT NOT NULL DEFAULT 'VND',
                    `category_id` INTEGER NOT NULL,
                    `note` TEXT,
                    `frequency` INTEGER NOT NULL,
                    `day_of_month` INTEGER,
                    `day_of_week` INTEGER,
                    `next_due_millis` INTEGER NOT NULL,
                    `is_active` INTEGER NOT NULL DEFAULT 1,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_recurring_transactions_next_due_millis` " +
                    "ON `recurring_transactions` (`next_due_millis`)",
            )
        }
    }
