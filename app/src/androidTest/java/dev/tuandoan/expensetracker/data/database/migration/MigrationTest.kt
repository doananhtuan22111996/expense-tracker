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
 * Instrumented test that verifies Room migration from version 1 to version 2.
 *
 * Since exportSchema is false, we create a v1 database manually using raw SQL,
 * then open it with Room (which triggers the migration) and validate the result.
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

    @Test
    fun migration1To2_addsColumnWithDefaultVND() {
        // Create a v1 database manually with the original schema
        createV1Database()

        // Open with Room at version 2 -- this triggers MIGRATION_1_2
        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_1_2)
                .build()

        // Accessing the DAO forces Room to open the database and run migration
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
        // Create a v1 database with multiple transactions
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

        // Verify first row
        cursor.moveToFirst()
        assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("type")))
        assertEquals(50000L, cursor.getLong(cursor.getColumnIndexOrThrow("amount")))
        assertEquals("VND", cursor.getString(cursor.getColumnIndexOrThrow("currency_code")))
        assertEquals("Lunch", cursor.getString(cursor.getColumnIndexOrThrow("note")))

        // Verify second row
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

        // Verify the column exists via PRAGMA
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

    /**
     * Creates a raw SQLite database matching the v1 schema (before currency_code).
     * Inserts sample data for migration validation.
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

        // Room master table for schema identity tracking
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS room_master_table (
                id INTEGER PRIMARY KEY,
                identity_hash TEXT
            )
            """.trimIndent(),
        )

        // Insert seed category
        db.execSQL(
            "INSERT INTO categories (id, name, type, is_default) VALUES (1, 'Food', 0, 1)",
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
}
