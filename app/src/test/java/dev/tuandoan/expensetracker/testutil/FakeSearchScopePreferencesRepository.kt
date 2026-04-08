package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.data.preferences.SearchScopePreferencesRepository
import dev.tuandoan.expensetracker.domain.model.SearchScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSearchScopePreferencesRepository(
    initialScope: SearchScope = SearchScope.CURRENT_MONTH,
) : SearchScopePreferencesRepository {
    private val scopeState = MutableStateFlow(initialScope)

    var lastSetScope: SearchScope? = null
        private set

    override val searchScope: Flow<SearchScope> = scopeState

    override suspend fun setSearchScope(scope: SearchScope) {
        lastSetScope = scope
        scopeState.value = scope
    }
}
