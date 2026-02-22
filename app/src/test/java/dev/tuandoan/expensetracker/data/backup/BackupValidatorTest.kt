package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.data.backup.model.BackupCategoryDto
import dev.tuandoan.expensetracker.data.backup.model.BackupTransactionDto
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.testutil.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupValidatorTest {
    private lateinit var validator: BackupValidator

    @Before
    fun setUp() {
        validator = BackupValidator()
    }

    @Test
    fun validate_validDocument_returnsValid() {
        val result = validator.validate(TestData.sampleBackupDocument)

        assertTrue(result is BackupValidationResult.Valid)
    }

    @Test
    fun validate_emptyLists_returnsValid() {
        val document =
            TestData.sampleBackupDocument.copy(
                categories = emptyList(),
                transactions = emptyList(),
            )

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Valid)
    }

    @Test
    fun validate_wrongSchemaVersion_returnsError() {
        val document = TestData.sampleBackupDocument.copy(schemaVersion = 99)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.UnsupportedSchemaVersion })
        val error = errors.filterIsInstance<BackupValidationError.UnsupportedSchemaVersion>().first()
        assertEquals(99, error.version)
    }

    @Test
    fun validate_duplicateCategoryIds_returnsError() {
        val categories =
            listOf(
                TestData.sampleBackupCategoryDto.copy(id = 1L),
                TestData.sampleBackupCategoryDto.copy(id = 1L, name = "Duplicate"),
            )
        val document = TestData.sampleBackupDocument.copy(categories = categories)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.DuplicateCategoryId })
        val error = errors.filterIsInstance<BackupValidationError.DuplicateCategoryId>().first()
        assertEquals(1L, error.id)
    }

    @Test
    fun validate_duplicateTransactionIds_returnsError() {
        val transactions =
            listOf(
                TestData.sampleBackupTransactionDto.copy(id = 5L),
                TestData.sampleBackupTransactionDto.copy(id = 5L, amount = 999L),
            )
        val document = TestData.sampleBackupDocument.copy(transactions = transactions)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.DuplicateTransactionId })
        val error = errors.filterIsInstance<BackupValidationError.DuplicateTransactionId>().first()
        assertEquals(5L, error.id)
    }

    @Test
    fun validate_blankCategoryName_returnsError() {
        val categories = listOf(TestData.sampleBackupCategoryDto.copy(id = 3L, name = "   "))
        val document =
            TestData.sampleBackupDocument.copy(
                categories = categories,
                transactions = emptyList(),
            )

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.BlankCategoryName })
        val error = errors.filterIsInstance<BackupValidationError.BlankCategoryName>().first()
        assertEquals(3L, error.categoryId)
    }

    @Test
    fun validate_emptyCategoryName_returnsError() {
        val categories = listOf(TestData.sampleBackupCategoryDto.copy(id = 3L, name = ""))
        val document =
            TestData.sampleBackupDocument.copy(
                categories = categories,
                transactions = emptyList(),
            )

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.BlankCategoryName })
    }

    @Test
    fun validate_invalidCategoryType_returnsError() {
        val categories = listOf(TestData.sampleBackupCategoryDto.copy(id = 4L, type = 5))
        val document =
            TestData.sampleBackupDocument.copy(
                categories = categories,
                transactions = emptyList(),
            )

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.InvalidCategoryType })
        val error = errors.filterIsInstance<BackupValidationError.InvalidCategoryType>().first()
        assertEquals(4L, error.categoryId)
        assertEquals(5, error.type)
    }

    @Test
    fun validate_negativeCategoryType_returnsError() {
        val categories = listOf(TestData.sampleBackupCategoryDto.copy(id = 4L, type = -1))
        val document =
            TestData.sampleBackupDocument.copy(
                categories = categories,
                transactions = emptyList(),
            )

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.InvalidCategoryType })
    }

    @Test
    fun validate_invalidTransactionType_returnsError() {
        val transactions = listOf(TestData.sampleBackupTransactionDto.copy(id = 7L, type = 3))
        val document = TestData.sampleBackupDocument.copy(transactions = transactions)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.InvalidTransactionType })
        val error = errors.filterIsInstance<BackupValidationError.InvalidTransactionType>().first()
        assertEquals(7L, error.transactionId)
        assertEquals(3, error.type)
    }

    @Test
    fun validate_negativeAmount_returnsError() {
        val transactions = listOf(TestData.sampleBackupTransactionDto.copy(id = 8L, amount = -100L))
        val document = TestData.sampleBackupDocument.copy(transactions = transactions)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.NegativeAmount })
        val error = errors.filterIsInstance<BackupValidationError.NegativeAmount>().first()
        assertEquals(8L, error.transactionId)
        assertEquals(-100L, error.amount)
    }

    @Test
    fun validate_zeroAmount_returnsValid() {
        val transactions = listOf(TestData.sampleBackupTransactionDto.copy(amount = 0L))
        val document = TestData.sampleBackupDocument.copy(transactions = transactions)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Valid)
    }

    @Test
    fun validate_unsupportedCurrencyCode_returnsError() {
        val transactions =
            listOf(TestData.sampleBackupTransactionDto.copy(id = 9L, currencyCode = "GBP"))
        val document = TestData.sampleBackupDocument.copy(transactions = transactions)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.UnsupportedCurrencyCode })
        val error = errors.filterIsInstance<BackupValidationError.UnsupportedCurrencyCode>().first()
        assertEquals(9L, error.transactionId)
        assertEquals("GBP", error.currencyCode)
    }

    @Test
    fun validate_orphanedTransaction_returnsError() {
        val categories = listOf(TestData.sampleBackupCategoryDto.copy(id = 1L))
        val transactions =
            listOf(TestData.sampleBackupTransactionDto.copy(id = 10L, categoryId = 999L))
        val document =
            TestData.sampleBackupDocument.copy(
                categories = categories,
                transactions = transactions,
            )

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.OrphanedTransaction })
        val error = errors.filterIsInstance<BackupValidationError.OrphanedTransaction>().first()
        assertEquals(10L, error.transactionId)
        assertEquals(999L, error.categoryId)
    }

    @Test
    fun validate_multipleErrors_collectsAll() {
        val categories =
            listOf(
                BackupCategoryDto(id = 1L, name = "", type = 5),
            )
        val transactions =
            listOf(
                BackupTransactionDto(
                    id = 1L,
                    type = 9,
                    amount = -50L,
                    currencyCode = "XYZ",
                    categoryId = 999L,
                    note = null,
                    timestamp = 1700000000000L,
                    createdAt = 1700000000000L,
                    updatedAt = 1700000000000L,
                ),
            )
        val document =
            TestData.sampleBackupDocument.copy(
                schemaVersion = 99,
                categories = categories,
                transactions = transactions,
            )

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        // Should contain: UnsupportedSchemaVersion, BlankCategoryName, InvalidCategoryType,
        // InvalidTransactionType, NegativeAmount, UnsupportedCurrencyCode, OrphanedTransaction
        assertTrue(errors.any { it is BackupValidationError.UnsupportedSchemaVersion })
        assertTrue(errors.any { it is BackupValidationError.BlankCategoryName })
        assertTrue(errors.any { it is BackupValidationError.InvalidCategoryType })
        assertTrue(errors.any { it is BackupValidationError.InvalidTransactionType })
        assertTrue(errors.any { it is BackupValidationError.NegativeAmount })
        assertTrue(errors.any { it is BackupValidationError.UnsupportedCurrencyCode })
        assertTrue(errors.any { it is BackupValidationError.OrphanedTransaction })
        assertEquals(7, errors.size)
    }

    @Test
    fun validate_validCategoryTypeExpense_returnsValid() {
        val categories = listOf(TestData.sampleBackupCategoryDto.copy(type = 0))
        val document =
            TestData.sampleBackupDocument.copy(
                categories = categories,
                transactions = emptyList(),
            )

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Valid)
    }

    @Test
    fun validate_validCategoryTypeIncome_returnsValid() {
        val categories = listOf(TestData.sampleBackupCategoryDto.copy(type = 1))
        val document =
            TestData.sampleBackupDocument.copy(
                categories = categories,
                transactions = emptyList(),
            )

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Valid)
    }

    @Test
    fun validate_allSupportedCurrencies_returnsValid() {
        val supportedCodes = SupportedCurrencies.all().map { it.code }
        for (code in supportedCodes) {
            val transactions =
                listOf(TestData.sampleBackupTransactionDto.copy(currencyCode = code))
            val document = TestData.sampleBackupDocument.copy(transactions = transactions)

            val result = validator.validate(document)

            assertTrue("Expected Valid for currency $code", result is BackupValidationResult.Valid)
        }
    }

    @Test
    fun validate_schemaVersionZero_returnsError() {
        val document = TestData.sampleBackupDocument.copy(schemaVersion = 0)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.UnsupportedSchemaVersion })
        assertEquals(
            0,
            (
                errors.first { it is BackupValidationError.UnsupportedSchemaVersion }
                    as BackupValidationError.UnsupportedSchemaVersion
            ).version,
        )
    }

    @Test
    fun validate_negativeSchemaVersion_returnsError() {
        val document = TestData.sampleBackupDocument.copy(schemaVersion = -1)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
    }

    @Test
    fun validate_lowercaseCurrencyCode_returnsError() {
        val transactions =
            listOf(TestData.sampleBackupTransactionDto.copy(id = 11L, currencyCode = "vnd"))
        val document = TestData.sampleBackupDocument.copy(transactions = transactions)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.UnsupportedCurrencyCode })
    }

    @Test
    fun validate_emptyCurrencyCode_returnsError() {
        val transactions =
            listOf(TestData.sampleBackupTransactionDto.copy(id = 12L, currencyCode = ""))
        val document = TestData.sampleBackupDocument.copy(transactions = transactions)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.UnsupportedCurrencyCode })
    }

    @Test
    fun validate_longMinValueAmount_returnsError() {
        val transactions =
            listOf(TestData.sampleBackupTransactionDto.copy(id = 13L, amount = Long.MIN_VALUE))
        val document = TestData.sampleBackupDocument.copy(transactions = transactions)

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.NegativeAmount })
    }

    @Test
    fun validate_transactionWithCategoryIdZero_noMatchingCategory_returnsOrphanedError() {
        val categories = listOf(TestData.sampleBackupCategoryDto.copy(id = 1L))
        val transactions =
            listOf(TestData.sampleBackupTransactionDto.copy(id = 14L, categoryId = 0L))
        val document =
            TestData.sampleBackupDocument.copy(
                categories = categories,
                transactions = transactions,
            )

        val result = validator.validate(document)

        assertTrue(result is BackupValidationResult.Invalid)
        val errors = (result as BackupValidationResult.Invalid).errors
        assertTrue(errors.any { it is BackupValidationError.OrphanedTransaction })
    }
}
