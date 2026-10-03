package dev.tuandoan.expensetracker.data.export

import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryTotal
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        amountForeignMinor: Long? = null,
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
            amountForeignMinor = amountForeignMinor,
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
        assertEquals("Date,Type,Amount,Currency,Category,Note,Trip,Foreign Amount,Foreign Currency", header)
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
        assertTrue(dataLine.contains("\"Food, drinks\""))
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
        assertTrue(dataLine.contains("\"He said \"\"hello\"\"\""))
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
        assertTrue(dataLine.contains(",Food,,,"))
    }

    @Test
    fun export_withTripAndForeignAmount_producesCorrectFields() {
        val twc =
            TransactionWithCategory(
                transaction =
                    createTransaction(
                        amount = 165000L,
                        currencyCode = "VND",
                    ).copy(amountForeignMinor = 1000L),
                categoryName = "Food",
                tripName = "Japan, 2026",
                tripForeignCurrencyCode = "JPY",
            )
        val result = exportToString(listOf(twc))
        val dataLine = result.lines().filter { it.isNotBlank() }[1]
        assertTrue(dataLine.endsWith(",\"Japan, 2026\",1000,JPY"))
    }

    // --- Gold holding export tests ---

    private fun createGoldHolding(
        id: Long = 1L,
        type: String = "SJC",
        weightValue: Double = 2.5,
        weightUnit: String = "TAEL",
        buyPricePerUnit: Long = 87_000_000L,
        currencyCode: String = "VND",
        buyDateMillis: Long = 1700000000000L,
        note: String? = "test gold",
    ): GoldHoldingEntity =
        GoldHoldingEntity(
            id = id,
            type = type,
            weightValue = weightValue,
            weightUnit = weightUnit,
            buyPricePerUnit = buyPricePerUnit,
            currencyCode = currencyCode,
            buyDateMillis = buyDateMillis,
            note = note,
            createdAt = buyDateMillis,
            updatedAt = buyDateMillis,
        )

    @Test
    fun exportGoldHoldings_producesCorrectHeader() {
        val writer = StringWriter()
        exporter.exportGoldHoldings(listOf(createGoldHolding()), writer.buffered())
        val lines = writer.toString().lines().filter { it.isNotBlank() }
        assertEquals("Date,Type,Weight,Unit,Buy Price,Currency,Note", lines[0])
    }

    @Test
    fun exportGoldHoldings_producesCorrectDataRow() {
        val writer = StringWriter()
        exporter.exportGoldHoldings(listOf(createGoldHolding()), writer.buffered())
        val lines = writer.toString().lines().filter { it.isNotBlank() }
        // 1700000000000L = 2023-11-14 in UTC
        assertEquals("2023-11-14,SJC,2.5,TAEL,87000000,VND,test gold", lines[1])
    }

    @Test
    fun exportGoldHoldings_nullNoteProducesEmptyField() {
        val writer = StringWriter()
        exporter.exportGoldHoldings(listOf(createGoldHolding(note = null)), writer.buffered())
        val lines = writer.toString().lines().filter { it.isNotBlank() }
        assertTrue(lines[1].endsWith(",VND,"))
    }

    @Test
    fun exportGoldHoldings_noteWithCommaIsEscaped() {
        val writer = StringWriter()
        exporter.exportGoldHoldings(listOf(createGoldHolding(note = "bought, sold")), writer.buffered())
        val lines = writer.toString().lines().filter { it.isNotBlank() }
        assertTrue(lines[1].endsWith("\"bought, sold\""))
    }

    // --- Gold P&L summary tests ---

    private fun createGoldPrice(
        type: String = "SJC",
        unit: String = "TAEL",
        pricePerUnit: Long = 92_000_000L,
        buyBackPricePerUnit: Long? = null,
        currencyCode: String = "VND",
    ): GoldPriceEntity =
        GoldPriceEntity(
            type = type,
            unit = unit,
            pricePerUnit = pricePerUnit,
            buyBackPricePerUnit = buyBackPricePerUnit,
            currencyCode = currencyCode,
            updatedAt = 1700000000000L,
        )

    @Test
    fun exportGoldSummary_producesCorrectHeader() {
        val writer = StringWriter()
        val holdings = listOf(createGoldHolding())
        val prices = listOf(createGoldPrice())
        exporter.exportGoldSummary(holdings, prices, writer.buffered())
        val lines = writer.toString().lines().filter { it.isNotBlank() }
        assertEquals(
            "Type,Unit,Weight,Buy Price,Current Price,Buy-Back Price,Currency,Cost,Market Value,Liquidation Value,P&L",
            lines[0],
        )
    }

    @Test
    fun exportGoldSummary_noBuyBack_marketPnL() {
        val writer = StringWriter()
        // buy at 87M, current at 92M, weight 2.5 tael, no buy-back
        // cost = 87M * 2.5 = 217,500,000
        // market value = 92M * 2.5 = 230,000,000
        // P&L = 230M - 217.5M = 12,500,000 (market-based)
        val holdings = listOf(createGoldHolding(buyPricePerUnit = 87_000_000L, weightValue = 2.5))
        val prices = listOf(createGoldPrice(pricePerUnit = 92_000_000L))
        exporter.exportGoldSummary(holdings, prices, writer.buffered())
        val lines = writer.toString().lines().filter { it.isNotBlank() }
        // Buy-Back Price empty, Liquidation Value empty, P&L = market-based
        assertEquals(
            "SJC,TAEL,2.5,87000000,92000000,,VND,217500000,230000000,,12500000",
            lines[1],
        )
    }

    @Test
    fun exportGoldSummary_withBuyBack_liquidationPnL() {
        val writer = StringWriter()
        // buy at 87M, sell at 93M, buy-back at 91M, weight 2.0 tael
        // cost = 87M * 2 = 174,000,000
        // market value = 93M * 2 = 186,000,000
        // liquidation value = 91M * 2 = 182,000,000
        // P&L = 182M - 174M = 8,000,000 (liquidation-based)
        val holdings = listOf(createGoldHolding(buyPricePerUnit = 87_000_000L, weightValue = 2.0))
        val prices = listOf(createGoldPrice(pricePerUnit = 93_000_000L, buyBackPricePerUnit = 91_000_000L))
        exporter.exportGoldSummary(holdings, prices, writer.buffered())
        val lines = writer.toString().lines().filter { it.isNotBlank() }
        assertEquals(
            "SJC,TAEL,2.0,87000000,93000000,91000000,VND,174000000,186000000,182000000,8000000",
            lines[1],
        )
    }

    @Test
    fun exportGoldSummary_multipleTypes_producesCorrectRows() {
        val writer = StringWriter()
        val holdings =
            listOf(
                createGoldHolding(
                    id = 1,
                    type = "SJC",
                    weightUnit = "TAEL",
                    weightValue = 2.0,
                    buyPricePerUnit = 87_000_000L,
                ),
                createGoldHolding(
                    id = 2,
                    type = "GOLD_24K",
                    weightUnit = "GRAM",
                    weightValue = 10.0,
                    buyPricePerUnit = 2_000_000L,
                ),
            )
        val prices =
            listOf(
                createGoldPrice(
                    type = "SJC",
                    unit = "TAEL",
                    pricePerUnit = 93_000_000L,
                    buyBackPricePerUnit = 91_000_000L,
                ),
                createGoldPrice(
                    type = "GOLD_24K",
                    unit = "GRAM",
                    pricePerUnit = 2_500_000L,
                ),
            )
        exporter.exportGoldSummary(holdings, prices, writer.buffered())
        val lines = writer.toString().lines().filter { it.isNotBlank() }
        assertEquals(3, lines.size) // header + 2 data rows
        // SJC: cost=174M, market=186M, liquidation=182M, P&L=182M-174M=8M
        assertEquals(
            "SJC,TAEL,2.0,87000000,93000000,91000000,VND,174000000,186000000,182000000,8000000",
            lines[1],
        )
        // GOLD_24K: cost=20M, market=25M, no buyBack, P&L=25M-20M=5M
        assertEquals(
            "GOLD_24K,GRAM,10.0,2000000,2500000,,VND,20000000,25000000,,5000000",
            lines[2],
        )
    }

    @Test
    fun exportGoldSummary_noPricesForHolding_skipsRow() {
        val writer = StringWriter()
        val holdings = listOf(createGoldHolding(type = "GOLD_24K", weightUnit = "GRAM"))
        val prices = listOf(createGoldPrice(type = "SJC", unit = "TAEL"))
        exporter.exportGoldSummary(holdings, prices, writer.buffered())
        // No matching price, so no output at all
        assertEquals("", writer.toString())
    }

    @Test
    fun exportGoldSummary_emptyPrices_producesNoOutput() {
        val writer = StringWriter()
        exporter.exportGoldSummary(listOf(createGoldHolding()), emptyList(), writer.buffered())
        assertEquals("", writer.toString())
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

    @Test
    fun exportIncludesTripColumnsWhenPresent() {
        val twcWithForeign =
            TransactionWithCategory(
                transaction = createTransaction(amount = 50000L, amountForeignMinor = 300L),
                categoryName = "Food",
                tripName = "Tokyo 2024",
                tripForeignCurrencyCode = "JPY",
            )
        val twcWithoutForeign =
            TransactionWithCategory(
                transaction = createTransaction(amount = 60000L, amountForeignMinor = null),
                categoryName = "Transport",
                tripName = "Tokyo 2024",
                tripForeignCurrencyCode = "JPY",
            )
        val result = exportToString(listOf(twcWithForeign, twcWithoutForeign))
        val lines = result.lines().filter { it.isNotBlank() }
        assertTrue(lines[1].contains(",Tokyo 2024,300,JPY"))
        assertTrue(lines[2].contains(",Tokyo 2024,,"))
    }

    @Test
    fun exportTrip_producesCorrectHeaderAndBom() {
        val trip =
            Trip(
                id = 1L,
                name = "Tokyo 2024",
                destination = "Japan",
                startDateEpochDay = 19723L,
                endDateEpochDay = 19730L,
                foreignCurrencyCode = "JPY",
                foreignToHomeRate = 160.0,
                originalCategoryId = null,
                originalCategoryNameSnapshot = null,
                originalCategoryIconSnapshot = null,
                originalCategoryColorSnapshot = null,
                createdAt = 1700000000000L,
            )
        val out = ByteArrayOutputStream()
        exporter.exportTrip(trip, emptyList(), out)
        val bytes = out.toByteArray()
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])
        val text = out.toString(Charsets.UTF_8.name())
        val lines = text.lines().filter { it.isNotBlank() }
        assertEquals(1, lines.size)
        val header = lines[0].removePrefix("\uFEFF")
        assertEquals("Date,Category,Amount,Currency,Foreign Amount,Foreign Currency,FX Rate,Note", header)
    }

    @Test
    fun exportTrip_formatsRowsCorrectlyAndSortsChronologically() {
        val trip =
            Trip(
                id = 1L,
                name = "Europe 2024",
                destination = "Paris",
                startDateEpochDay = 19723L,
                endDateEpochDay = 19730L,
                foreignCurrencyCode = "EUR",
                foreignToHomeRate = 27000.0,
                originalCategoryId = null,
                originalCategoryNameSnapshot = null,
                originalCategoryIconSnapshot = null,
                originalCategoryColorSnapshot = null,
                createdAt = 1700000000000L,
            )
        val catFood = Category(id = 1, name = "Food & Dining", type = TransactionType.EXPENSE)
        val catMuseum = Category(id = 2, name = "Museums, etc.", type = TransactionType.EXPENSE)

        val t2 =
            Transaction(
                id = 2L,
                type = TransactionType.EXPENSE,
                amount = 2700000L,
                currencyCode = "VND",
                category = catMuseum,
                note = "Louvre \"tickets\"",
                timestamp = 1700000100000L,
                createdAt = 1700000100000L,
                updatedAt = 1700000100000L,
                tripId = 1L,
                amountForeignMinor = 10000L,
            )
        val t1 =
            Transaction(
                id = 1L,
                type = TransactionType.EXPENSE,
                amount = 540000L,
                currencyCode = "VND",
                category = catFood,
                note = "Croissant",
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
                tripId = 1L,
                amountForeignMinor = 2000L,
            )

        val out = ByteArrayOutputStream()
        exporter.exportTrip(trip, listOf(t2, t1), out)
        val lines = out.toString(Charsets.UTF_8.name()).lines().filter { it.isNotBlank() }
        assertEquals(3, lines.size)
        assertTrue(lines[1].contains("Croissant"))
        assertTrue(lines[1].contains("Food & Dining,540000,VND,20.00,EUR,27000.0,Croissant"))
        assertTrue(lines[2].contains("\"Museums, etc.\""))
        assertTrue(lines[2].contains("\"Louvre \"\"tickets\"\"\""))
        assertTrue(lines[2].contains("2700000,VND,100.00,EUR,27000.0"))
    }

    @Test
    fun exportTrip_handlesNoForeignCurrency() {
        val trip =
            Trip(
                id = 1L,
                name = "Local Trip",
                destination = null,
                startDateEpochDay = 19723L,
                endDateEpochDay = 19730L,
                foreignCurrencyCode = null,
                foreignToHomeRate = null,
                originalCategoryId = null,
                originalCategoryNameSnapshot = null,
                originalCategoryIconSnapshot = null,
                originalCategoryColorSnapshot = null,
                createdAt = 1700000000000L,
            )
        val catFood = Category(id = 1, name = "Food", type = TransactionType.EXPENSE)
        val t =
            Transaction(
                id = 1L,
                type = TransactionType.EXPENSE,
                amount = 100000L,
                currencyCode = "VND",
                category = catFood,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
                tripId = 1L,
                amountForeignMinor = null,
            )
        val out = ByteArrayOutputStream()
        exporter.exportTrip(trip, listOf(t), out)
        val lines = out.toString(Charsets.UTF_8.name()).lines().filter { it.isNotBlank() }
        assertEquals(2, lines.size)
        assertEquals("2023-11-14,Food,100000,VND,,,,", lines[1])
    }

    @Test
    fun formatTripSummaryText_formatsCompleteSummary() {
        val trip =
            Trip(
                id = 1L,
                name = "Japan Trip",
                destination = "Tokyo",
                startDateEpochDay = 19723L,
                endDateEpochDay = 19727L,
                foreignCurrencyCode = "JPY",
                foreignToHomeRate = 160.0,
                originalCategoryId = null,
                originalCategoryNameSnapshot = null,
                originalCategoryIconSnapshot = null,
                originalCategoryColorSnapshot = null,
                createdAt = 1700000000000L,
            )
        val cat1 = Category(id = 1, name = "Food", type = TransactionType.EXPENSE)
        val cat2 = Category(id = 2, name = "Transport", type = TransactionType.EXPENSE)
        val categoryTotals =
            listOf(
                CategoryTotal(category = cat1, total = 800000L),
                CategoryTotal(category = cat2, total = 200000L),
            )
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val summary =
            exporter.formatTripSummaryText(
                trip = trip,
                totalLabel = "1,000,000 ₫",
                dailyAvgLabel = "200,000 ₫/day",
                transactionCount = 10,
                categoryTotals = categoryTotals,
                dateFormatter = dateFormatter,
            )
        assertTrue(summary.contains("Trip: Japan Trip (Tokyo)"))
        assertTrue(summary.contains("Dates: 2024-01-01 – 2024-01-05"))
        assertTrue(summary.contains("Total Spent: 1,000,000 ₫"))
        assertTrue(summary.contains("Daily Average: 200,000 ₫/day"))
        assertTrue(summary.contains("Transactions: 10"))
        assertTrue(summary.contains("Top Categories:"))
        assertTrue(summary.contains("• Food: 800000 (80%)"))
        assertTrue(summary.contains("• Transport: 200000 (20%)"))
    }

    @Test
    fun formatTripSummaryText_handlesNoDestinationAndEmptyCategories() {
        val trip =
            Trip(
                id = 1L,
                name = "Staycation",
                destination = null,
                startDateEpochDay = 19723L,
                endDateEpochDay = 19725L,
                foreignCurrencyCode = null,
                foreignToHomeRate = null,
                originalCategoryId = null,
                originalCategoryNameSnapshot = null,
                originalCategoryIconSnapshot = null,
                originalCategoryColorSnapshot = null,
                createdAt = 1700000000000L,
            )
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val summary =
            exporter.formatTripSummaryText(
                trip = trip,
                totalLabel = null,
                dailyAvgLabel = null,
                transactionCount = 0,
                categoryTotals = emptyList(),
                dateFormatter = dateFormatter,
            )
        assertTrue(summary.contains("Trip: Staycation"))
        assertTrue(!summary.contains("("))
        assertTrue(summary.contains("Total Spent: 0"))
        assertTrue(!summary.contains("Daily Average:"))
        assertTrue(summary.contains("Transactions: 0"))
        assertTrue(!summary.contains("Top Categories:"))
    }

    @Test
    fun formatTripSummaryText_handlesMoreThanFiveCategories() {
        val trip =
            Trip(
                id = 1L,
                name = "World Tour",
                destination = "Global",
                startDateEpochDay = 19723L,
                endDateEpochDay = 19725L,
                foreignCurrencyCode = null,
                foreignToHomeRate = null,
                originalCategoryId = null,
                originalCategoryNameSnapshot = null,
                originalCategoryIconSnapshot = null,
                originalCategoryColorSnapshot = null,
                createdAt = 1700000000000L,
            )
        val totals =
            (1..7).map { i ->
                CategoryTotal(
                    category = Category(id = i.toLong(), name = "Cat$i", type = TransactionType.EXPENSE),
                    total = 100000L,
                )
            }
        val summary =
            exporter.formatTripSummaryText(
                trip = trip,
                totalLabel = "700,000",
                dailyAvgLabel = null,
                transactionCount = 7,
                categoryTotals = totals,
                dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            )
        assertTrue(summary.contains("• Cat1: 100000 (14%)"))
        assertTrue(summary.contains("• Other: 200000 (29%)"))
    }
}
