package dev.tuandoan.expensetracker.data.export

import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import java.io.BufferedWriter
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TransactionWithCategory(
    val transaction: TransactionEntity,
    val categoryName: String,
)

class CsvExporter
    @Inject
    constructor(
        private val zoneId: ZoneId,
    ) {
        fun export(
            transactions: List<TransactionWithCategory>,
            outputStream: OutputStream,
        ) {
            // UTF-8 BOM for Excel compatibility
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            writer.write("Date,Type,Amount,Currency,Category,Note")
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

                writer.write("$date,$type,$plainAmount,${t.currencyCode},$category,$note")
                writer.newLine()
            }
            writer.flush()
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
                    GoldSummaryRow(
                        type = h.type,
                        unit = h.weightUnit,
                        weight = h.weightValue,
                        buyPrice = h.buyPricePerUnit,
                        currentPrice = price.pricePerUnit,
                        currencyCode = h.currencyCode,
                        cost = (h.buyPricePerUnit * h.weightValue).toLong(),
                        value = (price.pricePerUnit * h.weightValue).toLong(),
                    )
                }
            if (rows.isEmpty()) return

            writer.newLine()
            writer.write("Type,Unit,Weight,Buy Price,Current Price,Currency,Cost,Value,P&L")
            writer.newLine()

            for (r in rows) {
                val buyPrice = formatPlainAmount(r.buyPrice, r.currencyCode)
                val currentPrice = formatPlainAmount(r.currentPrice, r.currencyCode)
                val cost = formatPlainAmount(r.cost, r.currencyCode)
                val value = formatPlainAmount(r.value, r.currencyCode)
                val pnl = formatPlainAmount(r.value - r.cost, r.currencyCode)
                writer.write(
                    "${r.type},${r.unit},${r.weight},$buyPrice,$currentPrice,${r.currencyCode},$cost,$value,$pnl",
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
    val currencyCode: String,
    val cost: Long,
    val value: Long,
)
