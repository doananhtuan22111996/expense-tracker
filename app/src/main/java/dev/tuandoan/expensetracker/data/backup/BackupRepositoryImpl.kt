package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.core.util.AppInfo
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.backup.mapper.toBackupDto
import dev.tuandoan.expensetracker.data.backup.mapper.toEntity
import dev.tuandoan.expensetracker.data.backup.model.BackupDocumentV1
import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.domain.repository.BackupRepository
import dev.tuandoan.expensetracker.domain.repository.BackupRestoreResult
import javax.inject.Inject

class BackupRepositoryImpl
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
        private val transactionDao: TransactionDao,
        private val backupValidator: BackupValidator,
        private val backupSerializer: BackupSerializer,
        private val timeProvider: TimeProvider,
        private val transactionRunner: TransactionRunner,
    ) : BackupRepository {
        override suspend fun exportBackupJson(): String {
            val (categories, transactions) =
                transactionRunner.runInTransaction {
                    val cats = categoryDao.getAll().map { it.toBackupDto() }
                    val txns = transactionDao.getAll().map { it.toBackupDto() }
                    cats to txns
                }

            val document =
                BackupDocumentV1(
                    appVersionName = AppInfo.getVersionName(),
                    createdAtEpochMs = timeProvider.currentTimeMillis(),
                    categories = categories,
                    transactions = transactions,
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

            return BackupRestoreResult(
                categoryCount = document.categories.size,
                transactionCount = document.transactions.size,
            )
        }
    }

class BackupValidationException(
    val errors: List<BackupValidationError>,
) : Exception("Backup validation failed with ${errors.size} error(s)")
