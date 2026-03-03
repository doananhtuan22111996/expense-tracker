package dev.tuandoan.expensetracker.data.seed

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "seed_preferences")

@Singleton
class SeedRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val categoryDao: CategoryDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private val seedCompleteKey = booleanPreferencesKey("seed_complete")

        suspend fun seedDatabaseIfNeeded() {
            withContext(ioDispatcher) {
                val preferences = context.dataStore.data.first()
                val seedComplete = preferences[seedCompleteKey] ?: false

                // Primary guard: only seed when the categories table is empty.
                // The DataStore flag is a fast-path optimization; the table-empty
                // check is the safety net that prevents duplicate seeding after
                // backup restore or DataStore reset on upgrade.
                if (!seedComplete && categoryDao.count() == 0) {
                    seedDefaultCategories()
                    markSeedComplete()
                } else if (!seedComplete) {
                    // Categories exist (e.g., restored from backup) but flag was not set.
                    // Mark complete to avoid re-checking on every launch.
                    markSeedComplete()
                }
            }
        }

        private suspend fun seedDefaultCategories() {
            val expenseCategories =
                listOf(
                    CategoryEntity(name = "Food", type = CategoryEntity.TYPE_EXPENSE, isDefault = true),
                    CategoryEntity(name = "Transport", type = CategoryEntity.TYPE_EXPENSE, isDefault = true),
                    CategoryEntity(name = "Shopping", type = CategoryEntity.TYPE_EXPENSE, isDefault = true),
                    CategoryEntity(name = "Bills", type = CategoryEntity.TYPE_EXPENSE, isDefault = true),
                    CategoryEntity(name = "Health", type = CategoryEntity.TYPE_EXPENSE, isDefault = true),
                    CategoryEntity(name = "Entertainment", type = CategoryEntity.TYPE_EXPENSE, isDefault = true),
                    CategoryEntity(name = "Other", type = CategoryEntity.TYPE_EXPENSE, isDefault = true),
                )

            val incomeCategories =
                listOf(
                    CategoryEntity(name = "Salary", type = CategoryEntity.TYPE_INCOME, isDefault = true),
                    CategoryEntity(name = "Bonus", type = CategoryEntity.TYPE_INCOME, isDefault = true),
                    CategoryEntity(name = "Gift", type = CategoryEntity.TYPE_INCOME, isDefault = true),
                    CategoryEntity(name = "Other", type = CategoryEntity.TYPE_INCOME, isDefault = true),
                )

            categoryDao.insertAll(expenseCategories + incomeCategories)
        }

        private suspend fun markSeedComplete() {
            context.dataStore.edit { preferences ->
                preferences[seedCompleteKey] = true
            }
        }
    }
