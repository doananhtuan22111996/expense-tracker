package dev.tuandoan.expensetracker.domain.repository

import dev.tuandoan.expensetracker.data.backup.model.BackupDocumentV1

interface BackupRepository {
    suspend fun createBackupDocument(): BackupDocumentV1

    suspend fun restoreFromBackup(document: BackupDocumentV1)
}
