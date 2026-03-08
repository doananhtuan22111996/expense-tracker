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

/**
 * Migration from version 4 to version 5.
 *
 * Makes category_id nullable in recurring_transactions and adds
 * ON DELETE SET NULL foreign key behavior. SQLite does not support
 * ALTER COLUMN, so the table is rebuilt.
 */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Rename existing table
            db.execSQL("ALTER TABLE `recurring_transactions` RENAME TO `recurring_transactions_old`")

            // Create new table with nullable category_id and ON DELETE SET NULL
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `type` INTEGER NOT NULL,
                    `amount` INTEGER NOT NULL,
                    `currency_code` TEXT NOT NULL DEFAULT 'VND',
                    `category_id` INTEGER,
                    `note` TEXT,
                    `frequency` INTEGER NOT NULL,
                    `day_of_month` INTEGER,
                    `day_of_week` INTEGER,
                    `next_due_millis` INTEGER NOT NULL,
                    `is_active` INTEGER NOT NULL DEFAULT 1,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    FOREIGN KEY (`category_id`) REFERENCES `categories`(`id`) ON DELETE SET NULL
                )
                """.trimIndent(),
            )

            // Copy data from old table
            db.execSQL(
                """
                INSERT INTO `recurring_transactions`
                    (`id`, `type`, `amount`, `currency_code`, `category_id`, `note`,
                     `frequency`, `day_of_month`, `day_of_week`, `next_due_millis`,
                     `is_active`, `created_at`, `updated_at`)
                SELECT `id`, `type`, `amount`, `currency_code`, `category_id`, `note`,
                       `frequency`, `day_of_month`, `day_of_week`, `next_due_millis`,
                       `is_active`, `created_at`, `updated_at`
                FROM `recurring_transactions_old`
                """.trimIndent(),
            )

            // Drop old table
            db.execSQL("DROP TABLE `recurring_transactions_old`")

            // Recreate index
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_recurring_transactions_next_due_millis` " +
                    "ON `recurring_transactions` (`next_due_millis`)",
            )
        }
    }
