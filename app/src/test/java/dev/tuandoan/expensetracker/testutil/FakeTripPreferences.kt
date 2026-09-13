package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.domain.repository.TripPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTripPreferences(
    initialExcludeTrips: Boolean = false,
) : TripPreferences {
    private val _excludeTrips = MutableStateFlow(initialExcludeTrips)
    override val excludeTrips: Flow<Boolean> = _excludeTrips

    var lastExcludeTrips: Boolean = initialExcludeTrips
        private set

    override suspend fun setExcludeTrips(exclude: Boolean) {
        lastExcludeTrips = exclude
        _excludeTrips.value = exclude
    }

    fun setExcludeTripsSync(exclude: Boolean) {
        lastExcludeTrips = exclude
        _excludeTrips.value = exclude
    }
}
