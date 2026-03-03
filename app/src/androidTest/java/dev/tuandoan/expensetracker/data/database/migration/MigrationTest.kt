package dev.tuandoan.expensetracker.data.database.migration

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tuandoan.expensetracker.data.database.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests that verify Room migrations preserve data across all
 * upgrade paths. Uses manual SQL to create old-version databases since
 * schema JSON files were not exported for v1 and v2.
 *
 * Upgrade paths tested:
 *   v1 → v2  (MIGRATION_1_2: adds currency_code column)
 *   v2 → v3  (MIGRATION_2_3: adds timestamp + category_id indices)
 *   v1 → v3  (chained: MIGRATION_1_2 then MIGRATION_2_3)
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDbName = "migration-test"

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Before
    fun setUp() {
        context.deleteDatabase(testDbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(testDbName)
    }

    // ───────────────────────────────────────────────────────────
    //  v1 → v2: adds currency_code column with DEFAULT 'VND'
    // ───────────────────────────────────────────────────────────

    @Test
    fun migration1To2_addsColumnWithDefaultVND() {
        createV1Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_1_2)
                .build()

        val cursor =
            db.openHelper.readableDatabase.query(
                "SELECT currency_code FROM transactions WHERE id = 1",
            )

        assertTrue("Expected at least one row", cursor.moveToFirst())
        assertEquals("VND", cursor.getString(0))
        cursor.close()
        db.close()
    }

    @Test
    fun migration1To2_preservesExistingData() {
        createV1Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_1_2)
                .build()

        val cursor =
            db.openHelper.readableDatabase.query(
                "SELECT id, type, amount, currency_code, note FROM transactions ORDER BY id",
            )

        assertEquals(2, cursor.count)

        cursor.moveToFirst()
        assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("type")))
        assertEquals(50000L, cursor.getLong(cursor.getColumnIndexOrThrow("amount")))
        assertEquals("VND", cursor.getString(cursor.getColumnIndexOrThrow("currency_code")))
        assertEquals("Lunch", cursor.getString(cursor.getColumnIndexOrThrow("note")))

        cursor.moveToNext()
        assertEquals(2L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("type")))
        assertEquals(5000000L, cursor.getLong(cursor.getColumnIndexOrThrow("amount")))
        assertEquals("VND", cursor.getString(cursor.getColumnIndexOrThrow("currency_code")))

        cursor.close()
        db.close()
    }

    @Test
    fun migration1To2_currencyCodeColumnExists() {
        createV1Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_1_2)
                .build()

        val cursor =
            db.openHelper.readableDatabase.query("PRAGMA table_info(transactions)")

        val columnNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columnNames.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()

        assertTrue(
            "Expected currency_code column. Found: $columnNames",
            columnNames.contains("currency_code"),
        )

        db.close()
    }

    @Test
    fun migration1To2_preservesCategoryData() {
        createV1Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_1_2)
                .build()

        val cursor =
            db.openHelper.readableDatabase.query(
                "SELECT id, name, type, is_default FROM categories ORDER BY id",
            )

        assertEquals("Category row count should be preserved", 2, cursor.count)

        cursor.moveToFirst()
        assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Food", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("type")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))

        cursor.moveToNext()
        assertEquals(2L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Salary", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("type")))

        cursor.close()
        db.close()
    }

    // ───────────────────────────────────────────────────────────
    //  v2 → v3: adds indices on timestamp and category_id
    // ───────────────────────────────────────────────────────────

    @Test
    fun migration2To3_addsIndices() {
        createV2Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_2_3)
                .build()

        val cursor =
            db.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'transactions'",
            )

        val indexNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            indexNames.add(cursor.getString(0))
        }
        cursor.close()

        assertTrue(
            "Expected index_transactions_timestamp. Found: $indexNames",
            indexNames.contains("index_transactions_timestamp"),
        )
        assertTrue(
            "Expected index_transactions_category_id. Found: $indexNames",
            indexNames.contains("index_transactions_category_id"),
        )

        db.close()
    }

    @Test
    fun migration2To3_preservesTransactionData() {
        createV2Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_2_3)
                .build()

        val cursor =
            db.openHelper.readableDatabase.query(
                "SELECT id, type, amount, currency_code, note FROM transactions ORDER BY id",
            )

        assertEquals("Transaction row count should be preserved", 2, cursor.count)

        cursor.moveToFirst()
        assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals(50000L, cursor.getLong(cursor.getColumnIndexOrThrow("amount")))
        assertEquals("VND", cursor.getString(cursor.getColumnIndexOrThrow("currency_code")))
        assertEquals("Lunch", cursor.getString(cursor.getColumnIndexOrThrow("note")))

        cursor.moveToNext()
        assertEquals(2L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals(5000000L, cursor.getLong(cursor.getColumnIndexOrThrow("amount")))
        assertEquals("USD", cursor.getString(cursor.getColumnIndexOrThrow("currency_code")))

        cursor.close()
        db.close()
    }

    @Test
    fun migration2To3_preservesCategoryData() {
        createV2Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_2_3)
                .build()

        val cursor =
            db.openHelper.readableDatabase.query(
                "SELECT id, name, type, is_default FROM categories ORDER BY id",
            )

        assertEquals("Category row count should be preserved", 2, cursor.count)

        cursor.moveToFirst()
        assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Food", cursor.getString(cursor.getColumnIndexOrThrow("name")))

        cursor.moveToNext()
        assertEquals(2L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Salary", cursor.getString(cursor.getColumnIndexOrThrow("name")))

        cursor.close()
        db.close()
    }

    @Test
    fun migration2To3_canQueryWithRoomAfterMigration() {
        createV2Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_2_3)
                .build()

        // Verify the migrated DB is queryable via raw SQL on Room's open helper
        val cursor =
            db.openHelper.readableDatabase.query(
                "SELECT COUNT(*) FROM categories",
            )
        cursor.moveToFirst()
        assertEquals("Should have 2 categories", 2, cursor.getInt(0))
        cursor.close()

        db.close()
    }

    // ───────────────────────────────────────────────────────────
    //  v1 → v3: chained migration (v1 → v2 → v3)
    // ───────────────────────────────────────────────────────────

    @Test
    fun migration1To3_chained_preservesAllTransactionData() {
        createV1Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        val cursor =
            db.openHelper.readableDatabase.query(
                "SELECT id, type, amount, currency_code, note, timestamp FROM transactions ORDER BY id",
            )

        assertEquals("Transaction row count should be preserved", 2, cursor.count)

        cursor.moveToFirst()
        assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("type")))
        assertEquals(50000L, cursor.getLong(cursor.getColumnIndexOrThrow("amount")))
        assertEquals("VND", cursor.getString(cursor.getColumnIndexOrThrow("currency_code")))
        assertEquals("Lunch", cursor.getString(cursor.getColumnIndexOrThrow("note")))
        assertEquals(1700000000000L, cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")))

        cursor.moveToNext()
        assertEquals(2L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("type")))
        assertEquals(5000000L, cursor.getLong(cursor.getColumnIndexOrThrow("amount")))
        assertEquals("VND", cursor.getString(cursor.getColumnIndexOrThrow("currency_code")))

        cursor.close()
        db.close()
    }

    @Test
    fun migration1To3_chained_preservesAllCategoryData() {
        createV1Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        val cursor =
            db.openHelper.readableDatabase.query(
                "SELECT id, name, type, is_default FROM categories ORDER BY id",
            )

        assertEquals("Category row count should be preserved", 2, cursor.count)

        cursor.moveToFirst()
        assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Food", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("type")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))

        cursor.moveToNext()
        assertEquals(2L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Salary", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("type")))

        cursor.close()
        db.close()
    }

    @Test
    fun migration1To3_chained_hasIndices() {
        createV1Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        val cursor =
            db.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'transactions'",
            )

        val indexNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            indexNames.add(cursor.getString(0))
        }
        cursor.close()

        assertTrue(
            "Expected index_transactions_timestamp after chained migration",
            indexNames.contains("index_transactions_timestamp"),
        )
        assertTrue(
            "Expected index_transactions_category_id after chained migration",
            indexNames.contains("index_transactions_category_id"),
        )

        db.close()
    }

    @Test
    fun migration1To3_chained_hasCurrencyCodeColumn() {
        createV1Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        val cursor =
            db.openHelper.readableDatabase.query("PRAGMA table_info(transactions)")

        val columnNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columnNames.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()

        assertTrue(
            "Expected currency_code column after chained migration",
            columnNames.contains("currency_code"),
        )

        db.close()
    }

    @Test
    fun migration1To3_chained_canQueryWithRoomDao() {
        createV1Database()

        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        // Verify the migrated DB is fully queryable after chained migration
        val cursor =
            db.openHelper.readableDatabase.query(
                "SELECT id, name FROM categories ORDER BY id",
            )

        assertEquals("Should have 2 categories after chained migration", 2, cursor.count)
        cursor.moveToFirst()
        assertEquals("Food", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        cursor.moveToNext()
        assertEquals("Salary", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        cursor.close()

        db.close()
    }

    // ───────────────────────────────────────────────────────────
    //  Helper: create databases at specific versions
    // ───────────────────────────────────────────────────────────

    /**
     * Creates a raw SQLite database matching the v1 schema (before currency_code).
     * Inserts sample categories and transactions for migration validation.
     */
    private fun createV1Database() {
        val db = context.openOrCreateDatabase(testDbName, 0, null)

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                type INTEGER NOT NULL,
                icon_key TEXT,
                color_key TEXT,
                is_default INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                category_id INTEGER NOT NULL,
                note TEXT,
                timestamp INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS room_master_table (
                id INTEGER PRIMARY KEY,
                identity_hash TEXT
            )
            """.trimIndent(),
        )

        // Seed categories
        db.execSQL(
            "INSERT INTO categories (id, name, type, is_default) VALUES (1, 'Food', 0, 1)",
        )
        db.execSQL(
            "INSERT INTO categories (id, name, type, is_default) VALUES (2, 'Salary', 1, 1)",
        )

        // Insert test transactions
        db.execSQL(
            """
            INSERT INTO transactions (type, amount, category_id, note, timestamp, created_at, updated_at)
            VALUES (0, 50000, 1, 'Lunch', 1700000000000, 1700000000000, 1700000000000)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO transactions (type, amount, category_id, note, timestamp, created_at, updated_at)
            VALUES (1, 5000000, 1, NULL, 1700100000000, 1700100000000, 1700100000000)
            """.trimIndent(),
        )

        db.version = 1
        db.close()
    }

    /**
     * Creates a raw SQLite database matching the v2 schema (with currency_code, no indices).
     * Inserts sample data with mixed currencies for migration validation.
     */
    private fun createV2Database() {
        val db = context.openOrCreateDatabase(testDbName, 0, null)

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                type INTEGER NOT NULL,
                icon_key TEXT,
                color_key TEXT,
                is_default INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                currency_code TEXT NOT NULL DEFAULT 'VND',
                category_id INTEGER NOT NULL,
                note TEXT,
                timestamp INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS room_master_table (
                id INTEGER PRIMARY KEY,
                identity_hash TEXT
            )
            """.trimIndent(),
        )

        // Seed categories
        db.execSQL(
            "INSERT INTO categories (id, name, type, is_default) VALUES (1, 'Food', 0, 1)",
        )
        db.execSQL(
            "INSERT INTO categories (id, name, type, is_default) VALUES (2, 'Salary', 1, 1)",
        )

        // Insert test transactions with mixed currencies
        db.execSQL(
            """
            INSERT INTO transactions (type, amount, currency_code, category_id, note, timestamp, created_at, updated_at)
            VALUES (0, 50000, 'VND', 1, 'Lunch', 1700000000000, 1700000000000, 1700000000000)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO transactions (type, amount, currency_code, category_id, note, timestamp, created_at, updated_at)
            VALUES (1, 5000000, 'USD', 2, NULL, 1700100000000, 1700100000000, 1700100000000)
            """.trimIndent(),
        )

        db.version = 2
        db.close()
    }
}
