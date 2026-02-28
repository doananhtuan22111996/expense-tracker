package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.data.backup.model.BackupDocumentV1
import dev.tuandoan.expensetracker.testutil.TestData
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BackupAssemblerTest {
    private lateinit var assembler: BackupAssembler

    @Before
    fun setUp() {
        assembler = BackupAssembler()
    }

    @Test
    fun assemble_buildsStableDocumentGivenFixtures() {
        val document =
            assembler.assemble(
                categories = listOf(TestData.sampleBackupCategoryDto),
                transactions = listOf(TestData.sampleBackupTransactionDto),
                defaultCurrencyCode = "VND",
                appVersionName = "1.5.0",
                createdAtEpochMs = TestData.FIXED_TIME,
                deviceLocale = "en-US",
            )

        assertEquals(BackupDocumentV1.CURRENT_SCHEMA_VERSION, document.schemaVersion)
        assertEquals("1.5.0", document.appVersionName)
        assertEquals(TestData.FIXED_TIME, document.createdAtEpochMs)
        assertEquals("VND", document.defaultCurrencyCode)
        assertEquals("en-US", document.deviceLocale)
        assertEquals(1, document.categories.size)
        assertEquals(1, document.transactions.size)
        assertEquals(TestData.sampleBackupCategoryDto, document.categories[0])
        assertEquals(TestData.sampleBackupTransactionDto, document.transactions[0])
    }

    @Test
    fun assemble_sortsCategoriesById() {
        val cat3 = TestData.sampleBackupCategoryDto.copy(id = 3L, name = "C")
        val cat1 = TestData.sampleBackupCategoryDto.copy(id = 1L, name = "A")
        val cat2 = TestData.sampleBackupCategoryDto.copy(id = 2L, name = "B")

        val document =
            assembler.assemble(
                categories = listOf(cat3, cat1, cat2),
                transactions = emptyList(),
                defaultCurrencyCode = "VND",
                appVersionName = "1.5.0",
                createdAtEpochMs = TestData.FIXED_TIME,
                deviceLocale = "en-US",
            )

        assertEquals(listOf(1L, 2L, 3L), document.categories.map { it.id })
    }

    @Test
    fun assemble_sortsTransactionsById() {
        val txn3 = TestData.sampleBackupTransactionDto.copy(id = 3L)
        val txn1 = TestData.sampleBackupTransactionDto.copy(id = 1L)
        val txn2 = TestData.sampleBackupTransactionDto.copy(id = 2L)

        val document =
            assembler.assemble(
                categories = listOf(TestData.sampleBackupCategoryDto),
                transactions = listOf(txn3, txn1, txn2),
                defaultCurrencyCode = "VND",
                appVersionName = "1.5.0",
                createdAtEpochMs = TestData.FIXED_TIME,
                deviceLocale = "en-US",
            )

        assertEquals(listOf(1L, 2L, 3L), document.transactions.map { it.id })
    }

    @Test
    fun assemble_emptyLists_producesValidDocument() {
        val document =
            assembler.assemble(
                categories = emptyList(),
                transactions = emptyList(),
                defaultCurrencyCode = "VND",
                appVersionName = "1.5.0",
                createdAtEpochMs = TestData.FIXED_TIME,
                deviceLocale = "en-US",
            )

        assertEquals(0, document.categories.size)
        assertEquals(0, document.transactions.size)
        assertEquals(BackupDocumentV1.CURRENT_SCHEMA_VERSION, document.schemaVersion)
    }

    @Test
    fun assemble_setsMetadataFields() {
        val document =
            assembler.assemble(
                categories = emptyList(),
                transactions = emptyList(),
                defaultCurrencyCode = "USD",
                appVersionName = "2.0.0",
                createdAtEpochMs = 1700500000000L,
                deviceLocale = "vi-VN",
            )

        assertEquals("USD", document.defaultCurrencyCode)
        assertEquals("2.0.0", document.appVersionName)
        assertEquals(1700500000000L, document.createdAtEpochMs)
        assertEquals("vi-VN", document.deviceLocale)
    }

    @Test
    fun assemble_isDeterministic_sameInputSameOutput() {
        val categories =
            listOf(
                TestData.sampleBackupCategoryDto.copy(id = 3L),
                TestData.sampleBackupCategoryDto.copy(id = 1L),
            )
        val transactions =
            listOf(
                TestData.sampleBackupTransactionDto.copy(id = 2L),
                TestData.sampleBackupTransactionDto.copy(id = 1L),
            )

        val doc1 =
            assembler.assemble(
                categories = categories,
                transactions = transactions,
                defaultCurrencyCode = "VND",
                appVersionName = "1.5.0",
                createdAtEpochMs = TestData.FIXED_TIME,
                deviceLocale = "en-US",
            )
        val doc2 =
            assembler.assemble(
                categories = categories,
                transactions = transactions,
                defaultCurrencyCode = "VND",
                appVersionName = "1.5.0",
                createdAtEpochMs = TestData.FIXED_TIME,
                deviceLocale = "en-US",
            )

        assertEquals(doc1, doc2)
    }

    @Test
    fun assemble_alreadySortedInput_preservesOrder() {
        val cat1 = TestData.sampleBackupCategoryDto.copy(id = 1L)
        val cat2 = TestData.sampleBackupCategoryDto.copy(id = 2L)

        val document =
            assembler.assemble(
                categories = listOf(cat1, cat2),
                transactions = emptyList(),
                defaultCurrencyCode = "VND",
                appVersionName = "1.5.0",
                createdAtEpochMs = TestData.FIXED_TIME,
                deviceLocale = "en-US",
            )

        assertEquals(listOf(1L, 2L), document.categories.map { it.id })
    }
}
