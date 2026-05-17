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

/**
 * Migration from version 5 to version 6.
 *
 * Adds gold_holdings and gold_prices tables for the Gold Portfolio
 * feature. No existing data is modified.
 */
val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `gold_holdings` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `type` TEXT NOT NULL,
                    `weight_value` REAL NOT NULL,
                    `weight_unit` TEXT NOT NULL,
                    `buy_price_per_unit` INTEGER NOT NULL,
                    `currency_code` TEXT NOT NULL DEFAULT 'VND',
                    `buy_date_millis` INTEGER NOT NULL,
                    `note` TEXT,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_gold_holdings_buy_date_millis` " +
                    "ON `gold_holdings` (`buy_date_millis`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `gold_prices` (
                    `type` TEXT NOT NULL,
                    `unit` TEXT NOT NULL,
                    `price_per_unit` INTEGER NOT NULL,
                    `currency_code` TEXT NOT NULL DEFAULT 'VND',
                    `updated_at` INTEGER NOT NULL,
                    PRIMARY KEY (`type`, `unit`)
                )
                """.trimIndent(),
            )
        }
    }

/**
 * Migration from version 6 to version 7.
 *
 * Adds buy_back_price_per_unit column to gold_prices table for
 * dealer buy/sell price spread tracking. Nullable with no default,
 * so existing rows get NULL (single-price fallback behavior preserved).
 */
val MIGRATION_6_7 =
    object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `gold_prices` ADD COLUMN `buy_back_price_per_unit` INTEGER DEFAULT NULL",
            )
        }
    }

/**
 * Migration from version 7 to version 8.
 *
 * Introduces the Trip entity (v3.13.0) and trip-related columns on transactions.
 *
 * - Creates `trips` table per ADR-001 (in-row snapshots for reversible legacy-category
 *   conversion).
 * - Adds nullable `trip_id`, `original_category_id`, `amount_foreign_minor` columns to
 *   `transactions`. Existing rows get NULL for all three, preserving pre-v3.13.0 shape.
 * - Adds `index_transactions_trip_id` to keep trip-scoped queries (totals, daily,
 *   category breakdown, transaction list) cheap even on large histories.
 *
 * No SQLite-level FK from `transactions.trip_id` to `trips.id` — ON DELETE SET NULL
 * semantics are enforced by `TripRepository.deleteTrip` in code (ADR-001).
 */
val MIGRATION_7_8 =
    object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `trips` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `destination` TEXT,
                    `start_date_epoch_day` INTEGER NOT NULL,
                    `end_date_epoch_day` INTEGER NOT NULL,
                    `foreign_currency_code` TEXT,
                    `foreign_to_home_rate` REAL,
                    `original_category_id` INTEGER,
                    `original_category_name_snapshot` TEXT,
                    `original_category_icon_snapshot` TEXT,
                    `original_category_color_snapshot` TEXT,
                    `created_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `trip_id` INTEGER DEFAULT NULL")
            db.execSQL(
                "ALTER TABLE `transactions` ADD COLUMN `original_category_id` INTEGER DEFAULT NULL",
            )
            db.execSQL(
                "ALTER TABLE `transactions` ADD COLUMN `amount_foreign_minor` INTEGER DEFAULT NULL",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_trip_id` ON `transactions` (`trip_id`)",
            )
        }
    }
