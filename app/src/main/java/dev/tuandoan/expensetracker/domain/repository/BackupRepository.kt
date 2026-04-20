package dev.tuandoan.expensetracker.domain.repository

import java.io.InputStream
import java.io.OutputStream

interface BackupRepository {
    suspend fun exportBackupJson(): String

    suspend fun importBackupJson(json: String): BackupRestoreResult

    suspend fun exportBackup(
        outputStream: OutputStream,
        encrypt: EncryptOptions? = null,
        onProgress: suspend (BackupProgress) -> Unit = {},
    )

    suspend fun importBackup(
        inputStream: InputStream,
        decrypt: EncryptOptions? = null,
        onProgress: suspend (BackupProgress) -> Unit = {},
    ): BackupRestoreResult

    suspend fun exportCsv(outputStream: OutputStream)
}

data class BackupRestoreResult(
    val categoryCount: Int,
    val transactionCount: Int,
    val goldHoldingCount: Int = 0,
    val goldPriceCount: Int = 0,
)

data class BackupProgress(
    val current: Int,
    val total: Int,
)

class EncryptOptions(
    val password: CharArray,
) : AutoCloseable {
    /**
     * Zeroes the password array in place. Best-effort only — the JVM may have
     * already copied the CharArray (e.g. into a PBEKeySpec) before close() runs.
     * Callers should still prefer short-lived instances.
     */
    override fun close() {
        password.fill('\u0000')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptOptions) return false
        return password.contentEquals(other.password)
    }

    override fun hashCode(): Int = password.contentHashCode()
}
