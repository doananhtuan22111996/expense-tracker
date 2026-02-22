package dev.tuandoan.expensetracker.domain.repository

interface BackupRepository {
    suspend fun exportBackupJson(): String

    suspend fun importBackupJson(json: String): BackupRestoreResult
}

data class BackupRestoreResult(
    val categoryCount: Int,
    val transactionCount: Int,
)
