package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.domain.model.SearchScope
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.SearchFilterPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSearchFilterPreferences(
    initialScope: SearchScope = SearchScope.CURRENT_MONTH,
    initialFilterType: TransactionType? = null,
    initialCategoryId: Long? = null,
    initialDateRangeStartEpochDay: Long? = null,
    initialDateRangeEndEpochDay: Long? = null,
) : SearchFilterPreferences {
    private val scopeState = MutableStateFlow(initialScope)
    private val filterTypeState = MutableStateFlow(initialFilterType)
    private val categoryIdState = MutableStateFlow(initialCategoryId)
    private val dateRangeStartState = MutableStateFlow(initialDateRangeStartEpochDay)
    private val dateRangeEndState = MutableStateFlow(initialDateRangeEndEpochDay)

    var lastSetScope: SearchScope? = null
        private set
    var setFilterTypeCalled = false
        private set
    var lastSetFilterType: TransactionType? = null
        private set
    var setCategoryIdCalled = false
        private set
    var lastSetCategoryId: Long? = null
        private set
    var setDateRangeCalled = false
        private set
    var lastSetDateRangeStart: Long? = null
        private set
    var lastSetDateRangeEnd: Long? = null
        private set

    override val searchScope: Flow<SearchScope> = scopeState
    override val filterType: Flow<TransactionType?> = filterTypeState
    override val categoryId: Flow<Long?> = categoryIdState
    override val dateRangeStartEpochDay: Flow<Long?> = dateRangeStartState
    override val dateRangeEndEpochDay: Flow<Long?> = dateRangeEndState

    override suspend fun setSearchScope(scope: SearchScope) {
        lastSetScope = scope
        scopeState.value = scope
    }

    override suspend fun setFilterType(type: TransactionType?) {
        setFilterTypeCalled = true
        lastSetFilterType = type
        filterTypeState.value = type
    }

    override suspend fun setCategoryId(id: Long?) {
        setCategoryIdCalled = true
        lastSetCategoryId = id
        categoryIdState.value = id
    }

    override suspend fun setDateRange(
        startEpochDay: Long?,
        endEpochDay: Long?,
    ) {
        setDateRangeCalled = true
        lastSetDateRangeStart = startEpochDay
        lastSetDateRangeEnd = endEpochDay
        dateRangeStartState.value = startEpochDay
        dateRangeEndState.value = endEpochDay
    }
}
