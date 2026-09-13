package dev.tuandoan.expensetracker.domain.repository

import kotlinx.coroutines.flow.Flow

interface TripPreferences {
    val excludeTrips: Flow<Boolean>

    suspend fun setExcludeTrips(exclude: Boolean)
}
