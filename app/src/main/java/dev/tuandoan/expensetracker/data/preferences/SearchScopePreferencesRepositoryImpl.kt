package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.model.SearchScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchScopeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "search_scope_preferences",
)

@Singleton
class SearchScopePreferencesRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : SearchScopePreferencesRepository {
        private val scopeKey = intPreferencesKey("search_scope")

        override val searchScope: Flow<SearchScope> =
            context.searchScopeDataStore.data.map { preferences ->
                val ordinal = preferences[scopeKey] ?: SearchScope.CURRENT_MONTH.ordinal
                SearchScope.entries.getOrElse(ordinal) { SearchScope.CURRENT_MONTH }
            }

        override suspend fun setSearchScope(scope: SearchScope) {
            withContext(ioDispatcher) {
                context.searchScopeDataStore.edit { preferences ->
                    preferences[scopeKey] = scope.ordinal
                }
            }
        }
    }
