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
import javax.inject.Inject

class BackupRepositoryImpl
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
        private val transactionDao: TransactionDao,
        private val backupValidator: BackupValidator,
        private val timeProvider: TimeProvider,
        private val transactionRunner: TransactionRunner,
    ) : BackupRepository {
        override suspend fun createBackupDocument(): BackupDocumentV1 {
            val categories = categoryDao.getAll().map { it.toBackupDto() }
            val transactions = transactionDao.getAll().map { it.toBackupDto() }

            return BackupDocumentV1(
                appVersionName = AppInfo.getVersionName(),
                createdAtEpochMs = timeProvider.currentTimeMillis(),
                categories = categories,
                transactions = transactions,
            )
        }

        override suspend fun restoreFromBackup(document: BackupDocumentV1) {
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
        }
    }

class BackupValidationException(
    val errors: List<BackupValidationError>,
) : Exception("Backup validation failed with ${errors.size} error(s)")
