package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.data.backup.mapper.toBackupDto
import dev.tuandoan.expensetracker.data.backup.mapper.toEntity
import dev.tuandoan.expensetracker.data.backup.model.BackupDocumentV1
import dev.tuandoan.expensetracker.testutil.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupSerializerTest {
    private lateinit var serializer: BackupSerializer

    @Before
    fun setUp() {
        serializer = BackupSerializer()
    }

    @Test
    fun encodeAndDecode_roundTrip() {
        val original = TestData.sampleBackupDocument

        val json = serializer.encode(original)
        val decoded = serializer.decode(json)

        assertNotNull(decoded)
        assertEquals(original, decoded)
    }

    @Test
    fun encode_includesSchemaVersion() {
        val json = serializer.encode(TestData.sampleBackupDocument)

        assertTrue(json.contains("\"schema_version\""))
        assertTrue(json.contains(": ${BackupDocumentV1.CURRENT_SCHEMA_VERSION}"))
    }

    @Test
    fun encode_includesAppVersionName() {
        val json = serializer.encode(TestData.sampleBackupDocument)

        assertTrue(json.contains("\"app_version_name\""))
        assertTrue(json.contains("\"1.5.0\""))
    }

    @Test
    fun encode_nullFieldsExplicit() {
        val json = serializer.encode(TestData.sampleBackupDocument)

        // icon_key and color_key should be present even for default values
        // note should be present for the transaction
        assertTrue(json.contains("\"icon_key\""))
        assertTrue(json.contains("\"note\""))
    }

    @Test
    fun encode_defaultFieldsExplicit() {
        val document =
            TestData.sampleBackupDocument.copy(
                categories =
                    listOf(
                        TestData.sampleBackupCategoryDto.copy(
                            iconKey = null,
                            colorKey = null,
                            isDefault = false,
                        ),
                    ),
                transactions =
                    listOf(
                        TestData.sampleBackupTransactionDto.copy(note = null),
                    ),
            )

        val json = serializer.encode(document)

        // encodeDefaults = true means null fields and false booleans are written
        assertTrue(json.contains("\"icon_key\""))
        assertTrue(json.contains("\"is_default\""))
        assertTrue(json.contains("\"note\""))
    }

    @Test
    fun decode_malformedJson_returnsNull() {
        val result = serializer.decode("{not valid json")

        assertNull(result)
    }

    @Test
    fun decode_emptyString_returnsNull() {
        val result = serializer.decode("")

        assertNull(result)
    }

    @Test
    fun decode_wrongStructure_returnsNull() {
        val result = serializer.decode("""{"some_field": "value"}""")

        assertNull(result)
    }

    @Test
    fun decode_ignoresUnknownKeys() {
        val original = TestData.sampleBackupDocument
        val json = serializer.encode(original)

        // Insert an unknown key into the JSON
        val modifiedJson = json.replaceFirst("{", """{"unknown_future_field": "hello",""")
        val decoded = serializer.decode(modifiedJson)

        assertNotNull(decoded)
        assertEquals(original, decoded)
    }

    @Test
    fun emptyLists_roundTrip() {
        val document =
            TestData.sampleBackupDocument.copy(
                categories = emptyList(),
                transactions = emptyList(),
            )

        val json = serializer.encode(document)
        val decoded = serializer.decode(json)

        assertNotNull(decoded)
        assertEquals(document, decoded)
    }

    @Test
    fun encode_usesSnakeCaseKeys() {
        val json = serializer.encode(TestData.sampleBackupDocument)

        assertTrue(json.contains("\"schema_version\""))
        assertTrue(json.contains("\"app_version_name\""))
        assertTrue(json.contains("\"created_at_epoch_ms\""))
        assertTrue(json.contains("\"icon_key\""))
        assertTrue(json.contains("\"color_key\""))
        assertTrue(json.contains("\"is_default\""))
        assertTrue(json.contains("\"currency_code\""))
        assertTrue(json.contains("\"category_id\""))
        assertTrue(json.contains("\"created_at\""))
        assertTrue(json.contains("\"updated_at\""))
    }

    @Test
    fun decode_arrayInsteadOfObject_returnsNull() {
        val result = serializer.decode("[]")

        assertNull(result)
    }

    @Test
    fun encode_prettyPrints() {
        val json = serializer.encode(TestData.sampleBackupDocument)

        // Pretty-printed JSON should contain newlines and indentation
        assertTrue(json.contains("\n"))
        assertTrue(json.contains("    "))
    }

    @Test
    fun fullRoundTrip_entityToJsonToEntity() {
        val originalCategory = TestData.expenseCategoryEntity
        val originalTransaction = TestData.sampleExpenseEntity

        val document =
            BackupDocumentV1(
                appVersionName = "1.5.0",
                createdAtEpochMs = TestData.FIXED_TIME,
                categories = listOf(originalCategory.toBackupDto()),
                transactions = listOf(originalTransaction.toBackupDto()),
            )

        val json = serializer.encode(document)
        val decoded = serializer.decode(json)!!

        val restoredCategory = decoded.categories[0].toEntity()
        val restoredTransaction = decoded.transactions[0].toEntity()

        assertEquals(originalCategory, restoredCategory)
        assertEquals(originalTransaction, restoredTransaction)
    }

    @Test
    fun roundTrip_longMaxValueAmount_preserved() {
        val transaction = TestData.sampleBackupTransactionDto.copy(amount = Long.MAX_VALUE)
        val document = TestData.sampleBackupDocument.copy(transactions = listOf(transaction))

        val json = serializer.encode(document)
        val decoded = serializer.decode(json)!!

        assertEquals(Long.MAX_VALUE, decoded.transactions[0].amount)
    }

    @Test
    fun roundTrip_emojiInCategoryName_preserved() {
        val emojiCategory = TestData.sampleBackupCategoryDto.copy(name = "Food \uD83C\uDF54\uD83C\uDF5C")
        val document =
            TestData.sampleBackupDocument.copy(
                categories = listOf(emojiCategory),
                transactions = listOf(TestData.sampleBackupTransactionDto),
            )

        val json = serializer.encode(document)
        val decoded = serializer.decode(json)!!

        assertEquals("Food \uD83C\uDF54\uD83C\uDF5C", decoded.categories[0].name)
    }

    @Test
    fun decode_missingRequiredField_returnsNull() {
        val json =
            """{"schema_version": 1, "app_version_name": "1.0.0", "created_at_epoch_ms": 1700000000000}"""
        val result = serializer.decode(json)

        assertNull(result)
    }

    @Test
    fun decode_typeMismatchInField_returnsNull() {
        val json =
            """
            {
                "schema_version": 1,
                "app_version_name": "1.0.0",
                "created_at_epoch_ms": 1700000000000,
                "categories": [],
                "transactions": [{
                    "id": 1,
                    "type": 0,
                    "amount": "not_a_number",
                    "currency_code": "VND",
                    "category_id": 1,
                    "timestamp": 1700000000000,
                    "created_at": 1700000000000,
                    "updated_at": 1700000000000
                }]
            }
            """.trimIndent()

        val result = serializer.decode(json)
        assertNull(result)
    }
}
