package dev.tuandoan.expensetracker.domain.repository

import java.io.InputStream
import java.io.OutputStream

interface BackupRepository {
    suspend fun exportBackupJson(): String

    suspend fun importBackupJson(json: String): BackupRestoreResult

    suspend fun exportBackup(
        outputStream: OutputStream,
        onProgress: suspend (BackupProgress) -> Unit = {},
    )

    suspend fun importBackup(
        inputStream: InputStream,
        onProgress: suspend (BackupProgress) -> Unit = {},
    ): BackupRestoreResult
}

data class BackupRestoreResult(
    val categoryCount: Int,
    val transactionCount: Int,
)

data class BackupProgress(
    val current: Int,
    val total: Int,
)
