package dev.tuandoan.expensetracker.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupCryptoTest {
    private lateinit var crypto: BackupCrypto

    @Before
    fun setUp() {
        crypto = BackupCrypto()
    }

    @Test
    fun encrypt_decrypt_roundTrip() {
        val plaintext = "hello encrypted backup".encodeToByteArray()
        val password = "correct horse battery".toCharArray()

        val ciphertext = crypto.encrypt(plaintext, password.copyOf())
        val decrypted = crypto.decrypt(ciphertext, password.copyOf())

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun encrypt_producesHeaderWithMagic() {
        val ciphertext = crypto.encrypt(byteArrayOf(1, 2, 3), "pw12345a".toCharArray())

        assertEquals(0x45.toByte(), ciphertext[0]) // E
        assertEquals(0x54.toByte(), ciphertext[1]) // T
        assertEquals(0x42.toByte(), ciphertext[2]) // B
        assertEquals(0x4B.toByte(), ciphertext[3]) // K
    }

    @Test
    fun encrypt_producesVersionByte() {
        val ciphertext = crypto.encrypt(byteArrayOf(1, 2, 3), "pw12345a".toCharArray())

        assertEquals(BackupCrypto.VERSION.toByte(), ciphertext[4])
    }

    @Test
    fun encrypt_outputLongerThanHeaderPlusPlaintextPlusGcmTag() {
        val plaintext = ByteArray(100)
        val ciphertext = crypto.encrypt(plaintext, "pw12345a".toCharArray())

        // header + plaintext + 16-byte GCM tag
        assertEquals(BackupCrypto.HEADER_LENGTH + plaintext.size + 16, ciphertext.size)
    }

    @Test
    fun encrypt_uniqueOutputPerCall() {
        val plaintext = "same input".encodeToByteArray()
        val password = "pw12345a".toCharArray()

        val first = crypto.encrypt(plaintext, password.copyOf())
        val second = crypto.encrypt(plaintext, password.copyOf())

        // Salt + IV are random, so two encrypts of the same input must differ.
        assertFalse_arraysEqual(first, second)
    }

    @Test
    fun decrypt_wrongPassword_throwsWrongPassword() {
        val ciphertext = crypto.encrypt("secret".encodeToByteArray(), "correctPW1".toCharArray())

        assertThrows(BackupCryptoException.WrongPassword::class.java) {
            crypto.decrypt(ciphertext, "wrongPW123".toCharArray())
        }
    }

    @Test
    fun decrypt_tamperedCiphertext_throwsWrongPassword() {
        val password = "pw12345a".toCharArray()
        val ciphertext = crypto.encrypt("secret".encodeToByteArray(), password.copyOf())

        // Flip a bit in the ciphertext body (after the header).
        ciphertext[BackupCrypto.HEADER_LENGTH] = (ciphertext[BackupCrypto.HEADER_LENGTH].toInt() xor 0x01).toByte()

        assertThrows(BackupCryptoException.WrongPassword::class.java) {
            crypto.decrypt(ciphertext, password.copyOf())
        }
    }

    @Test
    fun decrypt_shorterThanHeader_throwsMalformedHeader() {
        val shortInput = ByteArray(10) // less than HEADER_LENGTH (33)

        assertThrows(BackupCryptoException.MalformedHeader::class.java) {
            crypto.decrypt(shortInput, "pw12345a".toCharArray())
        }
    }

    @Test
    fun decrypt_badMagicBytes_throwsMalformedHeader() {
        val ciphertext = crypto.encrypt("secret".encodeToByteArray(), "pw12345a".toCharArray())
        ciphertext[0] = 'X'.code.toByte()

        assertThrows(BackupCryptoException.MalformedHeader::class.java) {
            crypto.decrypt(ciphertext, "pw12345a".toCharArray())
        }
    }

    @Test
    fun decrypt_unsupportedVersion_throwsUnsupportedVersion() {
        val ciphertext = crypto.encrypt("secret".encodeToByteArray(), "pw12345a".toCharArray())
        ciphertext[4] = 0x99.toByte()

        val exception =
            assertThrows(BackupCryptoException.UnsupportedVersion::class.java) {
                crypto.decrypt(ciphertext, "pw12345a".toCharArray())
            }
        assertEquals(0x99, exception.version)
    }

    @Test
    fun encrypt_emptyPlaintext_roundTrips() {
        val plaintext = ByteArray(0)
        val password = "pw12345a".toCharArray()

        val ciphertext = crypto.encrypt(plaintext, password.copyOf())
        val decrypted = crypto.decrypt(ciphertext, password.copyOf())

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun encrypt_largePlaintext_roundTrips() {
        val plaintext = ByteArray(1 shl 20) { (it and 0xFF).toByte() } // 1 MB
        val password = "pw12345a".toCharArray()

        val ciphertext = crypto.encrypt(plaintext, password.copyOf())
        val decrypted = crypto.decrypt(ciphertext, password.copyOf())

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun encryptInternal_deterministicWithFixedSaltAndIv() {
        val plaintext = "deterministic".encodeToByteArray()
        val password = "pw12345a".toCharArray()
        val salt = ByteArray(BackupCrypto.SALT_LENGTH) { it.toByte() }
        val iv = ByteArray(BackupCrypto.IV_LENGTH) { (it + 100).toByte() }

        val first = crypto.encryptInternal(plaintext, password.copyOf(), salt.copyOf(), iv.copyOf())
        val second = crypto.encryptInternal(plaintext, password.copyOf(), salt.copyOf(), iv.copyOf())
        val third = crypto.encryptInternal(plaintext, password.copyOf(), salt.copyOf(), iv.copyOf())

        assertArrayEquals(first, second)
        assertArrayEquals(second, third)
    }

    @Test
    fun encryptInternal_rejectsInvalidSaltLength() {
        assertThrows(IllegalArgumentException::class.java) {
            crypto.encryptInternal(
                plaintext = byteArrayOf(1),
                password = "pw12345a".toCharArray(),
                salt = ByteArray(8),
                iv = ByteArray(BackupCrypto.IV_LENGTH),
            )
        }
    }

    @Test
    fun encryptInternal_rejectsInvalidIvLength() {
        assertThrows(IllegalArgumentException::class.java) {
            crypto.encryptInternal(
                plaintext = byteArrayOf(1),
                password = "pw12345a".toCharArray(),
                salt = ByteArray(BackupCrypto.SALT_LENGTH),
                iv = ByteArray(8),
            )
        }
    }

    @Test
    fun encrypt_decrypt_differentPasswords_neverCollide() {
        val plaintext = "same".encodeToByteArray()
        val ciphertext = crypto.encrypt(plaintext, "passwordA1".toCharArray())

        // Any other password must fail authentication.
        assertThrows(BackupCryptoException.WrongPassword::class.java) {
            crypto.decrypt(ciphertext, "passwordA2".toCharArray())
        }
    }

    @Test
    fun encrypt_doesNotModifyCallerPassword() {
        val password = "stayIntact1".toCharArray()
        val snapshot = password.copyOf()

        crypto.encrypt(byteArrayOf(1, 2, 3), password)

        assertArrayEquals(snapshot, password)
    }

    private fun assertFalse_arraysEqual(
        a: ByteArray,
        b: ByteArray,
    ) {
        val equal = a.contentEquals(b)
        assertTrue("expected arrays to differ", !equal)
    }
}
