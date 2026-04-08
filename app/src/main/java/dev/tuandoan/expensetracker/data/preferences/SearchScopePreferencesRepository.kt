package dev.tuandoan.expensetracker.data.preferences

import dev.tuandoan.expensetracker.domain.model.SearchScope
import kotlinx.coroutines.flow.Flow

interface SearchScopePreferencesRepository {
    val searchScope: Flow<SearchScope>

    suspend fun setSearchScope(scope: SearchScope)
}
