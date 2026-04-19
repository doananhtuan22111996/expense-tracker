package dev.tuandoan.expensetracker.data.backup

import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupCrypto
    @Inject
    constructor() {
        private val secureRandom = SecureRandom()

        fun encrypt(
            plaintext: ByteArray,
            password: CharArray,
        ): ByteArray {
            val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
            val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }
            return encryptInternal(plaintext, password, salt, iv)
        }

        fun decrypt(
            ciphertext: ByteArray,
            password: CharArray,
        ): ByteArray {
            if (ciphertext.size < HEADER_LENGTH) {
                throw BackupCryptoException.MalformedHeader("file too short")
            }
            for (i in MAGIC_BYTES.indices) {
                if (ciphertext[i] != MAGIC_BYTES[i]) {
                    throw BackupCryptoException.MalformedHeader("invalid magic bytes")
                }
            }
            val version = ciphertext[MAGIC_LENGTH].toInt() and 0xFF
            if (version != VERSION) {
                throw BackupCryptoException.UnsupportedVersion(version)
            }
            val salt = ciphertext.copyOfRange(SALT_OFFSET, SALT_OFFSET + SALT_LENGTH)
            val iv = ciphertext.copyOfRange(IV_OFFSET, IV_OFFSET + IV_LENGTH)
            val body = ciphertext.copyOfRange(HEADER_LENGTH, ciphertext.size)

            val key = deriveKey(password, salt)
            try {
                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                return try {
                    cipher.doFinal(body)
                } catch (e: javax.crypto.AEADBadTagException) {
                    throw BackupCryptoException.WrongPassword(e)
                } catch (e: javax.crypto.IllegalBlockSizeException) {
                    throw BackupCryptoException.DecryptionFailed(e)
                }
            } finally {
                zeroKey(key)
            }
        }

        internal fun encryptInternal(
            plaintext: ByteArray,
            password: CharArray,
            salt: ByteArray,
            iv: ByteArray,
        ): ByteArray {
            require(salt.size == SALT_LENGTH) { "salt must be $SALT_LENGTH bytes" }
            require(iv.size == IV_LENGTH) { "iv must be $IV_LENGTH bytes" }

            val key = deriveKey(password, salt)
            val body: ByteArray =
                try {
                    val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                    cipher.doFinal(plaintext)
                } finally {
                    zeroKey(key)
                }

            val output = ByteArray(HEADER_LENGTH + body.size)
            System.arraycopy(MAGIC_BYTES, 0, output, 0, MAGIC_LENGTH)
            output[MAGIC_LENGTH] = VERSION.toByte()
            System.arraycopy(salt, 0, output, SALT_OFFSET, SALT_LENGTH)
            System.arraycopy(iv, 0, output, IV_OFFSET, IV_LENGTH)
            System.arraycopy(body, 0, output, HEADER_LENGTH, body.size)
            return output
        }

        private fun deriveKey(
            password: CharArray,
            salt: ByteArray,
        ): SecretKey {
            val spec: KeySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            try {
                val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                val keyBytes = factory.generateSecret(spec).encoded
                return SecretKeySpec(keyBytes, KEY_ALGORITHM)
            } finally {
                (spec as PBEKeySpec).clearPassword()
            }
        }

        private fun zeroKey(key: SecretKey) {
            val encoded = key.encoded ?: return
            encoded.fill(0)
        }

        companion object {
            const val VERSION = 1
            const val PBKDF2_ITERATIONS = 200_000
            const val SALT_LENGTH = 16
            const val IV_LENGTH = 12
            const val KEY_LENGTH_BITS = 256
            const val GCM_TAG_BITS = 128

            private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
            private const val KEY_ALGORITHM = "AES"
            private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

            private val MAGIC_BYTES: ByteArray =
                byteArrayOf(0x45, 0x54, 0x42, 0x4B) // "ETBK"
            private const val MAGIC_LENGTH = 4
            private const val VERSION_LENGTH = 1
            private const val SALT_OFFSET = MAGIC_LENGTH + VERSION_LENGTH
            private const val IV_OFFSET = SALT_OFFSET + SALT_LENGTH
            const val HEADER_LENGTH = IV_OFFSET + IV_LENGTH
        }
    }
