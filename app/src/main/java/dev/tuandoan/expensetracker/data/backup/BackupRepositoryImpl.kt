package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.core.util.AppInfo
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.backup.mapper.toBackupDto
import dev.tuandoan.expensetracker.data.backup.mapper.toEntity
import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.BackupRepository
import dev.tuandoan.expensetracker.domain.repository.BackupRestoreResult
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import java.util.Locale
import javax.inject.Inject

class BackupRepositoryImpl
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
        private val transactionDao: TransactionDao,
        private val backupValidator: BackupValidator,
        private val backupSerializer: BackupSerializer,
        private val backupAssembler: BackupAssembler,
        private val timeProvider: TimeProvider,
        private val transactionRunner: TransactionRunner,
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
    ) : BackupRepository {
        override suspend fun exportBackupJson(): String {
            val (categories, transactions) =
                transactionRunner.runInTransaction {
                    val cats = categoryDao.getAll().map { it.toBackupDto() }
                    val txns = transactionDao.getAll().map { it.toBackupDto() }
                    cats to txns
                }

            val defaultCurrencyCode = currencyPreferenceRepository.getDefaultCurrency()

            val document =
                backupAssembler.assemble(
                    categories = categories,
                    transactions = transactions,
                    defaultCurrencyCode = defaultCurrencyCode,
                    appVersionName = AppInfo.getVersionName(),
                    createdAtEpochMs = timeProvider.currentTimeMillis(),
                    deviceLocale = Locale.getDefault().toLanguageTag(),
                )

            return backupSerializer.encode(document)
        }

        override suspend fun importBackupJson(json: String): BackupRestoreResult {
            val document =
                backupSerializer.decode(json)
                    ?: throw IllegalArgumentException("Invalid backup file format")

            val validationResult = backupValidator.validate(document)
            if (validationResult is BackupValidationResult.Invalid) {
                throw BackupValidationException(validationResult.errors)
            }

            val categoryEntities = document.categories.map { it.toEntity() }
            val transactionEntities = document.transactions.map { it.toEntity() }

            transactionRunner.runInTransaction {
                transactionDao.deleteAll()
                categoryDao.deleteAll()
                categoryDao.insertAll(categoryEntities)
                transactionDao.insertAll(transactionEntities)
            }

            // Best-effort currency preference restore: DataStore is independent of Room,
            // so a failure here should not mask a successful database restore.
            val currencyCode = document.defaultCurrencyCode
            if (currencyCode.isNotBlank() && SupportedCurrencies.byCode(currencyCode) != null) {
                @Suppress("TooGenericExceptionCaught")
                try {
                    currencyPreferenceRepository.setDefaultCurrency(currencyCode)
                } catch (_: Exception) {
                    // Currency preference will keep its previous value
                }
            }

            return BackupRestoreResult(
                categoryCount = document.categories.size,
                transactionCount = document.transactions.size,
            )
        }
    }

class BackupValidationException(
    val errors: List<BackupValidationError>,
) : Exception("Backup validation failed with ${errors.size} error(s)")
