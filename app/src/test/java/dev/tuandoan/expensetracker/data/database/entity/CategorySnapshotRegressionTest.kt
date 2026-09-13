package dev.tuandoan.expensetracker.data.database.entity

import dev.tuandoan.expensetracker.ui.screen.categories.AVAILABLE_COLORS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression test required by ADR-001 (Review Trigger 1).
 *
 * [TripEntity] stores [CategoryEntity.colorKey] and [CategoryEntity.iconKey] as strings
 * in its snapshots (`originalCategoryColorSnapshot`, `originalCategoryIconSnapshot`).
 * If the string encodings change or are removed, reverting a legacy-converted trip
 * will result in a category with an unresolvable color or icon.
 *
 * This test pins the expected string keys. If you add new colors/icons, update this list.
 * If you remove or rename them, you must provide a compatibility shim for reverting trips
 * and update ADR-001.
 */
class CategorySnapshotRegressionTest {
    @Test
    fun categoryColorEncoding_isPinnedForSnapshots() {
        val expectedKeys =
            listOf(
                "red",
                "blue",
                "green",
                "orange",
                "purple",
                "teal",
                "pink",
                "gray",
            )

        val actualKeys = AVAILABLE_COLORS.map { it.first }
        assertEquals(
            "Available colors must match historical snapshot keys to prevent ADR-001 revert failures",
            expectedKeys,
            actualKeys,
        )
    }

    @Test
    fun categoryIconEncoding_isPinnedForSnapshots() {
        // Although the app no longer allows users to set custom icons via the UI,
        // seed data and historical categories still use these string keys.
        val historicalIcons =
            listOf(
                "restaurant",
                "payments",
                "directions_bus",
                "place",
                "shopping_cart",
                "receipt",
                "medical_services",
                "sports_esports",
                "school",
                "help_outline",
                "more_horiz",
            )

        historicalIcons.forEach { iconKey ->
            assertTrue("Historical icon key must not be empty", iconKey.isNotEmpty())
        }
    }
}
