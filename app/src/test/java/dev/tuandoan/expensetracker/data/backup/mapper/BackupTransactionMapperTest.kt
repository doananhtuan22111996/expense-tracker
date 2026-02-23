package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupTransactionDto
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupTransactionMapperTest {
    @Test
    fun toBackupDto_mapsAllFields() {
        val entity =
            TransactionEntity(
                id = 1L,
                type = 0,
                amount = 50000L,
                currencyCode = "VND",
                categoryId = 1L,
                note = "Lunch",
                timestamp = 1700000000000L,
                createdAt = 1700000001000L,
                updatedAt = 1700000002000L,
            )

        val dto = entity.toBackupDto()

        assertEquals(1L, dto.id)
        assertEquals(0, dto.type)
        assertEquals(50000L, dto.amount)
        assertEquals("VND", dto.currencyCode)
        assertEquals(1L, dto.categoryId)
        assertEquals("Lunch", dto.note)
        assertEquals(1700000000000L, dto.timestamp)
        assertEquals(1700000001000L, dto.createdAt)
        assertEquals(1700000002000L, dto.updatedAt)
    }

    @Test
    fun toBackupDto_nullNote() {
        val entity =
            TransactionEntity(
                id = 1L,
                type = 1,
                amount = 10000000L,
                currencyCode = "USD",
                categoryId = 2L,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val dto = entity.toBackupDto()

        assertNull(dto.note)
    }

    @Test
    fun toEntity_mapsAllFields() {
        val dto =
            BackupTransactionDto(
                id = 1L,
                type = 0,
                amount = 50000L,
                currencyCode = "VND",
                categoryId = 1L,
                note = "Lunch",
                timestamp = 1700000000000L,
                createdAt = 1700000001000L,
                updatedAt = 1700000002000L,
            )

        val entity = dto.toEntity()

        assertEquals(1L, entity.id)
        assertEquals(0, entity.type)
        assertEquals(50000L, entity.amount)
        assertEquals("VND", entity.currencyCode)
        assertEquals(1L, entity.categoryId)
        assertEquals("Lunch", entity.note)
        assertEquals(1700000000000L, entity.timestamp)
        assertEquals(1700000001000L, entity.createdAt)
        assertEquals(1700000002000L, entity.updatedAt)
    }

    @Test
    fun roundTrip_preservesAllFields() {
        val original =
            TransactionEntity(
                id = 42L,
                type = 1,
                amount = 1000000L,
                currencyCode = "JPY",
                categoryId = 3L,
                note = "Bonus",
                timestamp = 1700000000000L,
                createdAt = 1700000001000L,
                updatedAt = 1700000002000L,
            )

        val backToEntity = original.toBackupDto().toEntity()

        assertEquals(original.id, backToEntity.id)
        assertEquals(original.type, backToEntity.type)
        assertEquals(original.amount, backToEntity.amount)
        assertEquals(original.currencyCode, backToEntity.currencyCode)
        assertEquals(original.categoryId, backToEntity.categoryId)
        assertEquals(original.note, backToEntity.note)
        assertEquals(original.timestamp, backToEntity.timestamp)
        assertEquals(original.createdAt, backToEntity.createdAt)
        assertEquals(original.updatedAt, backToEntity.updatedAt)
    }

    @Test
    fun roundTrip_preservesNullNote() {
        val original =
            TransactionEntity(
                id = 10L,
                type = 0,
                amount = 75000L,
                currencyCode = "EUR",
                categoryId = 1L,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val backToEntity = original.toBackupDto().toEntity()

        assertNull(backToEntity.note)
        assertEquals(original.id, backToEntity.id)
    }
}
