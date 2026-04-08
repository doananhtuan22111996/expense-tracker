package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.model.SearchScope
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.SearchFilterPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchFilterDataStore by preferencesDataStore(
    name = "search_filter_preferences",
)

@Singleton
class SearchFilterPreferencesImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : SearchFilterPreferences {
        override val searchScope: Flow<SearchScope> =
            context.searchFilterDataStore.data.map { prefs ->
                val ordinal = prefs[KEY_SEARCH_SCOPE] ?: SearchScope.CURRENT_MONTH.ordinal
                SearchScope.entries.getOrElse(ordinal) { SearchScope.CURRENT_MONTH }
            }

        override val filterType: Flow<TransactionType?> =
            context.searchFilterDataStore.data.map { prefs ->
                prefs[KEY_FILTER_TYPE]?.let { TransactionType.entries.getOrNull(it) }
            }

        override val categoryId: Flow<Long?> =
            context.searchFilterDataStore.data.map { prefs -> prefs[KEY_CATEGORY_ID] }

        override val dateRangeStartEpochDay: Flow<Long?> =
            context.searchFilterDataStore.data.map { prefs -> prefs[KEY_DATE_RANGE_START] }

        override val dateRangeEndEpochDay: Flow<Long?> =
            context.searchFilterDataStore.data.map { prefs -> prefs[KEY_DATE_RANGE_END] }

        override suspend fun setSearchScope(scope: SearchScope) {
            withContext(ioDispatcher) {
                context.searchFilterDataStore.edit { prefs ->
                    prefs[KEY_SEARCH_SCOPE] = scope.ordinal
                }
            }
        }

        override suspend fun setFilterType(type: TransactionType?) {
            withContext(ioDispatcher) {
                context.searchFilterDataStore.edit { prefs ->
                    if (type == null) {
                        prefs.remove(KEY_FILTER_TYPE)
                    } else {
                        prefs[KEY_FILTER_TYPE] = type.ordinal
                    }
                }
            }
        }

        override suspend fun setCategoryId(id: Long?) {
            withContext(ioDispatcher) {
                context.searchFilterDataStore.edit { prefs ->
                    if (id == null) {
                        prefs.remove(KEY_CATEGORY_ID)
                    } else {
                        prefs[KEY_CATEGORY_ID] = id
                    }
                }
            }
        }

        override suspend fun setDateRange(
            startEpochDay: Long?,
            endEpochDay: Long?,
        ) {
            withContext(ioDispatcher) {
                context.searchFilterDataStore.edit { prefs ->
                    if (startEpochDay == null) {
                        prefs.remove(KEY_DATE_RANGE_START)
                    } else {
                        prefs[KEY_DATE_RANGE_START] = startEpochDay
                    }
                    if (endEpochDay == null) {
                        prefs.remove(KEY_DATE_RANGE_END)
                    } else {
                        prefs[KEY_DATE_RANGE_END] = endEpochDay
                    }
                }
            }
        }

        private companion object {
            val KEY_SEARCH_SCOPE = intPreferencesKey("search_scope")
            val KEY_FILTER_TYPE = intPreferencesKey("filter_type")
            val KEY_CATEGORY_ID = longPreferencesKey("category_id")
            val KEY_DATE_RANGE_START = longPreferencesKey("date_range_start")
            val KEY_DATE_RANGE_END = longPreferencesKey("date_range_end")
        }
    }
