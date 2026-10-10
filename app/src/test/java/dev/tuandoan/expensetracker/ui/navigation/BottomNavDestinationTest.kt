package dev.tuandoan.expensetracker.ui.navigation

import dev.tuandoan.expensetracker.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavDestinationTest {
    @Test
    fun allDestinations_containsExactlyFiveTabs() {
        assertEquals(5, BottomNavDestination.allDestinations.size)
    }

    @Test
    fun eachDestination_hasValidStringResource() {
        val expected =
            mapOf(
                BottomNavDestination.Home to R.string.nav_home,
                BottomNavDestination.Summary to R.string.nav_summary,
                BottomNavDestination.Trips to R.string.nav_trips,
                BottomNavDestination.Gold to R.string.nav_gold,
                BottomNavDestination.Settings to R.string.nav_settings,
            )

        BottomNavDestination.allDestinations.forEach { destination ->
            assertTrue("Destination $destination should have positive titleRes", destination.titleRes > 0)
            assertEquals(
                "Destination $destination should map to expected titleRes",
                expected[destination],
                destination.titleRes,
            )
        }
    }

    @Test
    fun eachDestination_hasUniqueRoute() {
        val routes = BottomNavDestination.allDestinations.map { it.route }
        assertEquals(routes.distinct().size, routes.size)
    }
}
