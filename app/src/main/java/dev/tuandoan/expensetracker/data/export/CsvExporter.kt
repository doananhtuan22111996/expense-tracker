package dev.tuandoan.expensetracker.data.export

import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.CategoryTotal
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.Trip
import java.io.BufferedWriter
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class TransactionWithCategory(
    val transaction: TransactionEntity,
    val categoryName: String,
    val tripName: String? = null,
    val tripForeignCurrencyCode: String? = null,
)

class CsvExporter
    @Inject
    constructor(
        private val zoneId: ZoneId,
    ) {
        fun export(
            transactions: List<TransactionWithCategory>,
            outputStream: OutputStream,
        ): BufferedWriter {
            // UTF-8 BOM for Excel compatibility
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            writer.write("Date,Type,Amount,Currency,Category,Note,Trip,Foreign Amount,Foreign Currency")
            writer.newLine()

            for (twc in transactions) {
                val t = twc.transaction
                val date =
                    Instant
                        .ofEpochMilli(t.timestamp)
                        .atZone(zoneId)
                        .format(dateFormatter)
                val type = if (t.type == TransactionEntity.TYPE_INCOME) "Income" else "Expense"
                val plainAmount = formatPlainAmount(t.amount, t.currencyCode)
                val category = escapeCsvField(twc.categoryName)
                val note = escapeCsvField(t.note ?: "")
                val trip = twc.tripName?.let { escapeCsvField(it) } ?: ""
                val foreignAmount =
                    t.amountForeignMinor?.let {
                        formatPlainAmount(it, twc.tripForeignCurrencyCode ?: t.currencyCode)
                    } ?: ""
                val foreignCurrency = if (t.amountForeignMinor != null) (twc.tripForeignCurrencyCode ?: "") else ""

                writer.write(
                    "$date,$type,$plainAmount,${t.currencyCode},$category,$note,$trip,$foreignAmount,$foreignCurrency",
                )
                writer.newLine()
            }
            writer.flush()
            return writer
        }

        fun exportTrip(
            trip: Trip,
            transactions: List<Transaction>,
            outputStream: OutputStream,
        ): BufferedWriter {
            // UTF-8 BOM for Excel compatibility
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            writer.write("Date,Category,Amount,Currency,Foreign Amount,Foreign Currency,FX Rate,Note")
            writer.newLine()

            val sortedTransactions = transactions.sortedBy { it.timestamp }
            for (t in sortedTransactions) {
                val date =
                    Instant
                        .ofEpochMilli(t.timestamp)
                        .atZone(zoneId)
                        .format(dateFormatter)
                val category = escapeCsvField(t.category.name)
                val plainAmount = formatPlainAmount(t.amount, t.currencyCode)
                val foreignAmount =
                    t.amountForeignMinor?.let {
                        formatPlainAmount(it, trip.foreignCurrencyCode ?: t.currencyCode)
                    } ?: ""
                val foreignCurrency = if (t.amountForeignMinor != null) (trip.foreignCurrencyCode ?: "") else ""
                val fxRate = if (t.amountForeignMinor != null) (trip.foreignToHomeRate?.toString() ?: "") else ""
                val note = escapeCsvField(t.note ?: "")

                writer.write(
                    "$date,$category,$plainAmount,${t.currencyCode},$foreignAmount,$foreignCurrency,$fxRate,$note",
                )
                writer.newLine()
            }
            writer.flush()
            return writer
        }

        fun formatTripSummaryText(
            trip: Trip,
            totalLabel: String?,
            dailyAvgLabel: String?,
            transactionCount: Int,
            categoryTotals: List<CategoryTotal>,
            currencyFormatter: CurrencyFormatter? = null,
            currencyCode: String? = null,
            dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
        ): String {
            val startDateStr = LocalDate.ofEpochDay(trip.startDateEpochDay).format(dateFormatter)
            val endDateStr = LocalDate.ofEpochDay(trip.endDateEpochDay).format(dateFormatter)
            val sb = StringBuilder()
            if (trip.destination.isNullOrBlank()) {
                sb.appendLine("Trip: ${trip.name}")
            } else {
                sb.appendLine("Trip: ${trip.name} (${trip.destination})")
            }
            sb.appendLine("Dates: $startDateStr – $endDateStr")
            sb.appendLine("Total Spent: ${totalLabel ?: "0"}")
            if (dailyAvgLabel != null) {
                sb.appendLine("Daily Average: $dailyAvgLabel")
            }
            sb.appendLine("Transactions: $transactionCount")

            if (categoryTotals.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("Top Categories:")
                val totalAmount = categoryTotals.sumOf { it.total }
                categoryTotals.take(5).forEach { item ->
                    val percentage =
                        if (totalAmount > 0) {
                            Math.round((item.total.toDouble() / totalAmount) * 100).toInt()
                        } else {
                            0
                        }
                    val formattedAmount =
                        if (currencyFormatter != null && currencyCode != null) {
                            currencyFormatter.format(item.total, currencyCode)
                        } else {
                            formatPlainAmount(item.total, SupportedCurrencies.default().code)
                        }
                    sb.appendLine("• ${item.category.name}: $formattedAmount ($percentage%)")
                }
                if (categoryTotals.size > 5) {
                    val remainingSum = categoryTotals.drop(5).sumOf { it.total }
                    val remainingPercentage =
                        if (totalAmount > 0) {
                            Math.round((remainingSum.toDouble() / totalAmount) * 100).toInt()
                        } else {
                            0
                        }
                    val formattedRemaining =
                        if (currencyFormatter != null && currencyCode != null) {
                            currencyFormatter.format(remainingSum, currencyCode)
                        } else {
                            formatPlainAmount(remainingSum, SupportedCurrencies.default().code)
                        }
                    sb.appendLine("• Other: $formattedRemaining ($remainingPercentage%)")
                }
            }
            return sb.toString().trimEnd()
        }

        fun exportGoldHoldings(
            holdings: List<GoldHoldingEntity>,
            writer: BufferedWriter,
        ) {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            writer.newLine()
            writer.write("Date,Type,Weight,Unit,Buy Price,Currency,Note")
            writer.newLine()

            for (h in holdings) {
                val date =
                    Instant
                        .ofEpochMilli(h.buyDateMillis)
                        .atZone(zoneId)
                        .format(dateFormatter)
                val plainPrice = formatPlainAmount(h.buyPricePerUnit, h.currencyCode)
                val note = escapeCsvField(h.note ?: "")

                writer.write("$date,${h.type},${h.weightValue},${h.weightUnit},$plainPrice,${h.currencyCode},$note")
                writer.newLine()
            }
            writer.flush()
        }

        fun exportGoldSummary(
            holdings: List<GoldHoldingEntity>,
            prices: List<GoldPriceEntity>,
            writer: BufferedWriter,
        ) {
            val priceMap = prices.associateBy { it.type to it.unit }
            val rows =
                holdings.mapNotNull { h ->
                    val price = priceMap[h.type to h.weightUnit] ?: return@mapNotNull null
                    val marketValue = (price.pricePerUnit * h.weightValue).toLong()
                    val liquidationValue = price.buyBackPricePerUnit?.let { (it * h.weightValue).toLong() }
                    GoldSummaryRow(
                        type = h.type,
                        unit = h.weightUnit,
                        weight = h.weightValue,
                        buyPrice = h.buyPricePerUnit,
                        currentPrice = price.pricePerUnit,
                        buyBackPrice = price.buyBackPricePerUnit,
                        currencyCode = h.currencyCode,
                        cost = (h.buyPricePerUnit * h.weightValue).toLong(),
                        marketValue = marketValue,
                        liquidationValue = liquidationValue,
                    )
                }
            if (rows.isEmpty()) return

            writer.newLine()
            writer.write(
                "Type,Unit,Weight,Buy Price,Current Price,Buy-Back Price,Currency,Cost,Market Value,Liquidation Value,P&L",
            )
            writer.newLine()

            for (r in rows) {
                val buyPrice = formatPlainAmount(r.buyPrice, r.currencyCode)
                val currentPrice = formatPlainAmount(r.currentPrice, r.currencyCode)
                val buyBackPrice = r.buyBackPrice?.let { formatPlainAmount(it, r.currencyCode) } ?: ""
                val cost = formatPlainAmount(r.cost, r.currencyCode)
                val marketValue = formatPlainAmount(r.marketValue, r.currencyCode)
                val liquidationValue = r.liquidationValue?.let { formatPlainAmount(it, r.currencyCode) } ?: ""
                val pnl = formatPlainAmount((r.liquidationValue ?: r.marketValue) - r.cost, r.currencyCode)
                writer.write(
                    "${r.type},${r.unit},${r.weight},$buyPrice,$currentPrice,$buyBackPrice,${r.currencyCode},$cost,$marketValue,$liquidationValue,$pnl",
                )
                writer.newLine()
            }
            writer.flush()
        }

        internal fun formatPlainAmount(
            amount: Long,
            currencyCode: String,
        ): String {
            val currency = SupportedCurrencies.byCode(currencyCode)
            val minorDigits = currency?.minorUnitDigits ?: 0
            return if (minorDigits > 0) {
                val divisor = pow10(minorDigits)
                val major = amount / divisor
                val minor = amount % divisor
                "$major.${minor.toString().padStart(minorDigits, '0')}"
            } else {
                amount.toString()
            }
        }

        internal fun escapeCsvField(value: String): String =
            if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }

        private fun pow10(n: Int): Long {
            require(n >= 0) { "Minor unit digits must not be negative" }
            return Math.pow(10.0, n.toDouble()).toLong()
        }
    }

internal data class GoldSummaryRow(
    val type: String,
    val unit: String,
    val weight: Double,
    val buyPrice: Long,
    val currentPrice: Long,
    val buyBackPrice: Long?,
    val currencyCode: String,
    val cost: Long,
    val marketValue: Long,
    val liquidationValue: Long?,
)
