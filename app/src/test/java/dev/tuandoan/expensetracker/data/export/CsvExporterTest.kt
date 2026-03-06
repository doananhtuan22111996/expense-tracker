package dev.tuandoan.expensetracker.data.export

import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.ZoneId

class CsvExporterTest {
    private lateinit var exporter: CsvExporter
    private val fixedZone = ZoneId.of("UTC")

    @Before
    fun setup() {
        exporter = CsvExporter(fixedZone)
    }

    private fun createTransaction(
        type: Int = TransactionEntity.TYPE_EXPENSE,
        amount: Long = 50000L,
        currencyCode: String = "VND",
        categoryId: Long = 1L,
        note: String? = "Lunch",
        timestamp: Long = 1700000000000L, // 2023-11-14 UTC
    ): TransactionEntity =
        TransactionEntity(
            id = 1L,
            type = type,
            amount = amount,
            currencyCode = currencyCode,
            categoryId = categoryId,
            note = note,
            timestamp = timestamp,
            createdAt = timestamp,
            updatedAt = timestamp,
        )

    private fun exportToString(transactions: List<TransactionWithCategory>): String {
        val outputStream = ByteArrayOutputStream()
        exporter.export(transactions, outputStream)
        return outputStream.toString(Charsets.UTF_8.name())
    }

    private fun exportToBytes(transactions: List<TransactionWithCategory>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        exporter.export(transactions, outputStream)
        return outputStream.toByteArray()
    }

    @Test
    fun exportProducesCorrectHeader() {
        val result = exportToString(emptyList())
        val lines = result.lines().filter { it.isNotBlank() }
        // BOM + header
        assertTrue(lines.isNotEmpty())
        // Strip BOM if present at start
        val header = lines[0].removePrefix("\uFEFF")
        assertEquals("Date,Type,Amount,Currency,Category,Note", header)
    }

    @Test
    fun emptyTransactionListProducesHeaderOnly() {
        val result = exportToString(emptyList())
        val lines = result.lines().filter { it.isNotBlank() }
        assertEquals(1, lines.size)
    }

    @Test
    fun utfBomIsPresentAsFirstBytes() {
        val bytes = exportToBytes(emptyList())
        assertTrue(bytes.size >= 3)
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])
    }

    @Test
    fun noteWithCommaIsEscaped() {
        val twc =
            TransactionWithCategory(
                transaction = createTransaction(note = "Food, drinks"),
                categoryName = "Food",
            )
        val result = exportToString(listOf(twc))
        val dataLine = result.lines().filter { it.isNotBlank() }[1]
        assertTrue(dataLine.endsWith("\"Food, drinks\""))
    }

    @Test
    fun noteWithQuoteIsDoubleEscaped() {
        val twc =
            TransactionWithCategory(
                transaction = createTransaction(note = "He said \"hello\""),
                categoryName = "Food",
            )
        val result = exportToString(listOf(twc))
        val dataLine = result.lines().filter { it.isNotBlank() }[1]
        assertTrue(dataLine.endsWith("\"He said \"\"hello\"\"\""))
    }

    @Test
    fun vndAmountHasNoDecimals() {
        val amount = exporter.formatPlainAmount(120000L, "VND")
        assertEquals("120000", amount)
    }

    @Test
    fun usdAmountHasTwoDecimals() {
        val amount = exporter.formatPlainAmount(12050L, "USD")
        assertEquals("120.50", amount)
    }

    @Test
    fun expenseTransactionHasCorrectType() {
        val twc =
            TransactionWithCategory(
                transaction = createTransaction(type = TransactionEntity.TYPE_EXPENSE),
                categoryName = "Food",
            )
        val result = exportToString(listOf(twc))
        val dataLine = result.lines().filter { it.isNotBlank() }[1]
        assertTrue(dataLine.contains(",Expense,"))
    }

    @Test
    fun incomeTransactionHasCorrectType() {
        val twc =
            TransactionWithCategory(
                transaction = createTransaction(type = TransactionEntity.TYPE_INCOME),
                categoryName = "Salary",
            )
        val result = exportToString(listOf(twc))
        val dataLine = result.lines().filter { it.isNotBlank() }[1]
        assertTrue(dataLine.contains(",Income,"))
    }

    @Test
    fun dateIsFormattedCorrectly() {
        // 1700000000000L = 2023-11-14 in UTC
        val twc =
            TransactionWithCategory(
                transaction = createTransaction(timestamp = 1700000000000L),
                categoryName = "Food",
            )
        val result = exportToString(listOf(twc))
        val dataLine = result.lines().filter { it.isNotBlank() }[1]
        assertTrue(dataLine.startsWith("2023-11-14,"))
    }

    @Test
    fun categoryNameWithCommaIsEscaped() {
        val twc =
            TransactionWithCategory(
                transaction = createTransaction(note = "test"),
                categoryName = "Food, Beverage",
            )
        val result = exportToString(listOf(twc))
        val dataLine = result.lines().filter { it.isNotBlank() }[1]
        assertTrue(dataLine.contains("\"Food, Beverage\""))
    }

    @Test
    fun nullNoteProducesEmptyField() {
        val twc =
            TransactionWithCategory(
                transaction = createTransaction(note = null),
                categoryName = "Food",
            )
        val result = exportToString(listOf(twc))
        val dataLine = result.lines().filter { it.isNotBlank() }[1]
        assertTrue(dataLine.endsWith(",Food,"))
    }

    @Test
    fun multipleTransactionsProduceMultipleRows() {
        val transactions =
            listOf(
                TransactionWithCategory(
                    transaction = createTransaction(amount = 50000L),
                    categoryName = "Food",
                ),
                TransactionWithCategory(
                    transaction = createTransaction(amount = 100000L, type = TransactionEntity.TYPE_INCOME),
                    categoryName = "Salary",
                ),
            )
        val result = exportToString(transactions)
        val lines = result.lines().filter { it.isNotBlank() }
        assertEquals(3, lines.size) // header + 2 data rows
    }
}
