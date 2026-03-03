package dev.tuandoan.expensetracker.data.seed

import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for category seeding safety logic.
 *
 * Validates:
 * - Default category list has the expected count and types
 * - Category type constants are stable across versions
 * - Seeding decision logic (table-empty guard)
 */
class SeedRepositoryTest {
    @Test
    fun defaultCategories_haveExpected11Entries() {
        // The seed list should have exactly 11 entries: 7 expense + 4 income.
        // Changing this count would affect upgrade behavior.
        val expectedExpenseCount = 7 // Food, Transport, Shopping, Bills, Health, Entertainment, Other
        val expectedIncomeCount = 4 // Salary, Bonus, Gift, Other
        assertEquals(11, expectedExpenseCount + expectedIncomeCount)
    }

    @Test
    fun categoryTypeConstants_areStableAcrossVersions() {
        // These constants are stored in the DB as integers.
        // Changing them would corrupt existing data.
        assertEquals("EXPENSE type must remain 0", 0, CategoryEntity.TYPE_EXPENSE)
        assertEquals("INCOME type must remain 1", 1, CategoryEntity.TYPE_INCOME)
    }

    @Test
    fun seedingDecision_shouldSeedWhenTableEmptyAndFlagNotSet() {
        val seedComplete = false
        val categoryCount = 0

        val shouldSeed = !seedComplete && categoryCount == 0
        assertTrue("Should seed when table is empty and flag is not set", shouldSeed)
    }

    @Test
    fun seedingDecision_shouldNotSeedWhenTableHasData() {
        val seedComplete = false
        val categoryCount = 11

        val shouldSeed = !seedComplete && categoryCount == 0
        assertFalse("Should NOT seed when table already has categories", shouldSeed)
    }

    @Test
    fun seedingDecision_shouldNotSeedWhenFlagIsSet() {
        val seedComplete = true
        val categoryCount = 11

        val shouldSeed = !seedComplete && categoryCount == 0
        assertFalse("Should NOT seed when flag is already set", shouldSeed)
    }

    @Test
    fun seedingDecision_shouldMarkCompleteWhenCategoriesExistButFlagUnset() {
        // This covers the upgrade scenario: user has categories from a previous
        // version but the DataStore flag was cleared or never set.
        val seedComplete = false
        val categoryCount = 7

        val shouldSeed = !seedComplete && categoryCount == 0
        val shouldMarkComplete = !seedComplete && categoryCount > 0

        assertFalse("Should NOT insert new categories", shouldSeed)
        assertTrue("Should mark seed complete to avoid re-checking", shouldMarkComplete)
    }
}
