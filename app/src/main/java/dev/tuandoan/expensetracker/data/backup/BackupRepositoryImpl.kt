package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.core.util.AppInfo
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.backup.mapper.toBackupDto
import dev.tuandoan.expensetracker.data.backup.mapper.toEntity
import dev.tuandoan.expensetracker.data.backup.model.BackupDocumentV1
import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.GoldHoldingDao
import dev.tuandoan.expensetracker.data.database.dao.GoldPriceDao
import dev.tuandoan.expensetracker.data.database.dao.RecurringTransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.export.CsvExporter
import dev.tuandoan.expensetracker.data.export.TransactionWithCategory
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.BackupProgress
import dev.tuandoan.expensetracker.domain.repository.BackupRepository
import dev.tuandoan.expensetracker.domain.repository.BackupRestoreResult
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.GZIPInputStream
import javax.inject.Inject

class BackupRepositoryImpl
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
        private val transactionDao: TransactionDao,
        private val recurringTransactionDao: RecurringTransactionDao,
        private val goldHoldingDao: GoldHoldingDao,
        private val goldPriceDao: GoldPriceDao,
        private val backupValidator: BackupValidator,
        private val backupSerializer: BackupSerializer,
        private val backupAssembler: BackupAssembler,
        private val timeProvider: TimeProvider,
        private val transactionRunner: TransactionRunner,
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
        private val csvExporter: CsvExporter,
        private val crashReporter: CrashReporter,
    ) : BackupRepository {
        override suspend fun exportBackupJson(): String {
            val document = buildExportDocument()
            return backupSerializer.encode(document)
        }

        override suspend fun importBackupJson(json: String): BackupRestoreResult {
            val document =
                backupSerializer.decode(json)
                    ?: throw IllegalArgumentException("Invalid backup file format")
            return performImport(document)
        }

        override suspend fun exportBackup(
            outputStream: OutputStream,
            onProgress: suspend (BackupProgress) -> Unit,
        ) {
            val document = buildExportDocument()
            val total = document.categories.size + document.transactions.size
            onProgress(BackupProgress(current = 0, total = total))
            backupSerializer.encodeToStream(document, outputStream)
            onProgress(BackupProgress(current = total, total = total))
        }

        override suspend fun importBackup(
            inputStream: InputStream,
            onProgress: suspend (BackupProgress) -> Unit,
        ): BackupRestoreResult =
            @Suppress("TooGenericExceptionCaught")
            try {
                val decompressed = decompressIfGzip(inputStream)
                val document =
                    backupSerializer.decodeFromStream(decompressed)
                        ?: throw IllegalArgumentException("Invalid backup file format")
                performImport(document, onProgress)
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(e)
                throw e
            }

        override suspend fun exportCsv(outputStream: OutputStream) {
            @Suppress("TooGenericExceptionCaught")
            try {
                val allTransactions = transactionDao.getAllOrdered()
                val transactionsWithCategory =
                    allTransactions.map { entity ->
                        TransactionWithCategory(
                            transaction = entity,
                            categoryName = categoryDao.getById(entity.categoryId)?.name ?: "Unknown",
                        )
                    }
                csvExporter.export(transactionsWithCategory, outputStream)

                val goldHoldings = goldHoldingDao.getAll()
                if (goldHoldings.isNotEmpty()) {
                    val writer = outputStream.bufferedWriter(Charsets.UTF_8)
                    csvExporter.exportGoldHoldings(goldHoldings, writer)

                    val goldPrices = goldPriceDao.getAll()
                    if (goldPrices.isNotEmpty()) {
                        csvExporter.exportGoldSummary(goldHoldings, goldPrices, writer)
                    }
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(e)
                throw e
            }
        }

        private suspend fun buildExportDocument(): BackupDocumentV1 {
            data class ExportData(
                val categories: List<dev.tuandoan.expensetracker.data.backup.model.BackupCategoryDto>,
                val transactions: List<dev.tuandoan.expensetracker.data.backup.model.BackupTransactionDto>,
                val recurring: List<dev.tuandoan.expensetracker.data.backup.model.BackupRecurringTransactionDto>,
                val goldHoldings: List<dev.tuandoan.expensetracker.data.backup.model.BackupGoldHoldingDto>,
                val goldPrices: List<dev.tuandoan.expensetracker.data.backup.model.BackupGoldPriceDto>,
            )

            val result =
                transactionRunner.runInTransaction {
                    ExportData(
                        categories = categoryDao.getAll().map { it.toBackupDto() },
                        transactions = transactionDao.getAll().map { it.toBackupDto() },
                        recurring = recurringTransactionDao.getAllList().map { it.toBackupDto() },
                        goldHoldings = goldHoldingDao.getAll().map { it.toBackupDto() },
                        goldPrices = goldPriceDao.getAll().map { it.toBackupDto() },
                    )
                }

            val defaultCurrencyCode = currencyPreferenceRepository.getDefaultCurrency()

            return backupAssembler.assemble(
                categories = result.categories,
                transactions = result.transactions,
                recurringTransactions = result.recurring,
                goldHoldings = result.goldHoldings,
                goldPrices = result.goldPrices,
                defaultCurrencyCode = defaultCurrencyCode,
                appVersionName = AppInfo.getVersionName(),
                createdAtEpochMs = timeProvider.currentTimeMillis(),
                deviceLocale = Locale.getDefault().toLanguageTag(),
            )
        }

        private suspend fun performImport(
            document: BackupDocumentV1,
            onProgress: suspend (BackupProgress) -> Unit = {},
        ): BackupRestoreResult {
            val validationResult = backupValidator.validate(document)
            if (validationResult is BackupValidationResult.Invalid) {
                throw BackupValidationException(validationResult.errors)
            }

            val categoryEntities = document.categories.map { it.toEntity() }
            val transactionEntities = document.transactions.map { it.toEntity() }
            val recurringEntities = document.recurringTransactions.map { it.toEntity() }
            val goldHoldingEntities = document.goldHoldings.map { it.toEntity() }
            val goldPriceEntities = document.goldPrices.map { it.toEntity() }
            val total =
                categoryEntities.size + transactionEntities.size +
                    recurringEntities.size + goldHoldingEntities.size + goldPriceEntities.size
            onProgress(BackupProgress(current = 0, total = total))

            // Once the destructive deleteAll() begins, the transaction must run to
            // completion so we never leave the DB in a half-wiped state.
            withContext(NonCancellable) {
                transactionRunner.runInTransaction {
                    recurringTransactionDao.deleteAll()
                    transactionDao.deleteAll()
                    categoryDao.deleteAll()
                    if (goldHoldingEntities.isNotEmpty()) {
                        goldHoldingDao.deleteAll()
                    }
                    categoryDao.insertAll(categoryEntities)

                    var inserted = categoryEntities.size
                    onProgress(BackupProgress(current = inserted, total = total))

                    for (batch in transactionEntities.chunked(BATCH_SIZE)) {
                        transactionDao.insertAll(batch)
                        inserted += batch.size
                        onProgress(BackupProgress(current = inserted, total = total))
                    }

                    if (recurringEntities.isNotEmpty()) {
                        recurringTransactionDao.insertAll(recurringEntities)
                        inserted += recurringEntities.size
                        onProgress(BackupProgress(current = inserted, total = total))
                    }

                    if (goldHoldingEntities.isNotEmpty()) {
                        goldHoldingDao.insertAll(goldHoldingEntities)
                        inserted += goldHoldingEntities.size
                        onProgress(BackupProgress(current = inserted, total = total))
                    }

                    if (goldPriceEntities.isNotEmpty()) {
                        goldPriceDao.deleteAll()
                        goldPriceDao.upsertAll(goldPriceEntities)
                        inserted += goldPriceEntities.size
                        onProgress(BackupProgress(current = inserted, total = total))
                    }
                }
            }

            restoreCurrencyPreference(document.defaultCurrencyCode)

            return BackupRestoreResult(
                categoryCount = document.categories.size,
                transactionCount = document.transactions.size,
                goldHoldingCount = document.goldHoldings.size,
                goldPriceCount = document.goldPrices.size,
            )
        }

        private suspend fun restoreCurrencyPreference(currencyCode: String) {
            if (currencyCode.isNotBlank() && SupportedCurrencies.byCode(currencyCode) != null) {
                @Suppress("TooGenericExceptionCaught")
                try {
                    currencyPreferenceRepository.setDefaultCurrency(currencyCode)
                } catch (_: Exception) {
                    // Currency preference will keep its previous value
                }
            }
        }

        private fun decompressIfGzip(inputStream: InputStream): InputStream {
            val buffered =
                if (inputStream.markSupported()) inputStream else BufferedInputStream(inputStream)
            buffered.mark(2)
            val byte1 = buffered.read()
            val byte2 = buffered.read()
            buffered.reset()
            return if (byte1 == GZIP_MAGIC_BYTE1 && byte2 == GZIP_MAGIC_BYTE2) {
                try {
                    GZIPInputStream(buffered)
                } catch (e: IOException) {
                    buffered.close()
                    throw e
                }
            } else {
                buffered
            }
        }

        companion object {
            const val BATCH_SIZE = 500
            private const val GZIP_MAGIC_BYTE1 = 0x1f
            private const val GZIP_MAGIC_BYTE2 = 0x8b
        }
    }

class BackupValidationException(
    val errors: List<BackupValidationError>,
) : Exception("Backup validation failed with ${errors.size} error(s)")
