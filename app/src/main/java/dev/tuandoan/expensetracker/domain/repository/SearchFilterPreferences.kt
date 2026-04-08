package dev.tuandoan.expensetracker.domain.repository

import dev.tuandoan.expensetracker.domain.model.SearchScope
import dev.tuandoan.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface SearchFilterPreferences {
    val searchScope: Flow<SearchScope>
    val filterType: Flow<TransactionType?>
    val categoryId: Flow<Long?>
    val dateRangeStartEpochDay: Flow<Long?>
    val dateRangeEndEpochDay: Flow<Long?>

    suspend fun setSearchScope(scope: SearchScope)

    suspend fun setFilterType(type: TransactionType?)

    suspend fun setCategoryId(id: Long?)

    suspend fun setDateRange(
        startEpochDay: Long?,
        endEpochDay: Long?,
    )
}
