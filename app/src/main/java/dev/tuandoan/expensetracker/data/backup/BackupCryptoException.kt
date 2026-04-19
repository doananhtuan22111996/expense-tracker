package dev.tuandoan.expensetracker.data.backup

sealed class BackupCryptoException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class WrongPassword(
        cause: Throwable? = null,
    ) : BackupCryptoException("Incorrect password or tampered backup", cause)

    class MalformedHeader(
        reason: String,
    ) : BackupCryptoException("Malformed encrypted backup: $reason")

    class UnsupportedVersion(
        val version: Int,
    ) : BackupCryptoException("Unsupported encrypted backup version: $version")

    class DecryptionFailed(
        cause: Throwable,
    ) : BackupCryptoException("Decryption failed", cause)
}
