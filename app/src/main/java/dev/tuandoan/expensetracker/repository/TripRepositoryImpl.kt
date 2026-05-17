package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TripDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.TripEntity
import dev.tuandoan.expensetracker.domain.model.DeleteTripBehavior
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.model.TripFilter
import dev.tuandoan.expensetracker.domain.repository.TripRepository
import dev.tuandoan.expensetracker.repository.mapper.toDomain
import dev.tuandoan.expensetracker.repository.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepositoryImpl
    @Inject
    constructor(
        private val tripDao: TripDao,
        private val transactionDao: TransactionDao,
        private val categoryDao: CategoryDao,
        private val transactionRunner: TransactionRunner,
        private val timeProvider: TimeProvider,
    ) : TripRepository {
        override fun observeTrips(filter: TripFilter): Flow<List<Trip>> {
            val entityFlow =
                when (filter) {
                    TripFilter.All -> tripDao.observeAll()
                    is TripFilter.Active -> tripDao.observeActive(filter.nowEpochDay)
                    is TripFilter.Upcoming -> tripDao.observeUpcoming(filter.nowEpochDay)
                    is TripFilter.Past -> tripDao.observePast(filter.nowEpochDay)
                }
            return entityFlow.map { list -> list.map { it.toDomain() } }
        }

        override fun observeTripById(id: Long): Flow<Trip?> = tripDao.observeById(id).map { it?.toDomain() }

        override suspend fun getTripById(id: Long): Trip? = tripDao.getById(id)?.toDomain()

        override suspend fun createTrip(
            name: String,
            destination: String?,
            startDateEpochDay: Long,
            endDateEpochDay: Long,
            foreignCurrencyCode: String?,
            foreignToHomeRate: Double?,
        ): Long {
            val now = timeProvider.currentTimeMillis()
            val entity =
                TripEntity(
                    name = name,
                    destination = destination,
                    startDateEpochDay = startDateEpochDay,
                    endDateEpochDay = endDateEpochDay,
                    foreignCurrencyCode = foreignCurrencyCode,
                    foreignToHomeRate = foreignToHomeRate,
                    originalCategoryId = null,
                    originalCategoryNameSnapshot = null,
                    originalCategoryIconSnapshot = null,
                    originalCategoryColorSnapshot = null,
                    createdAt = now,
                )
            return tripDao.insert(entity)
        }

        override suspend fun updateTrip(trip: Trip) {
            tripDao.update(trip.toEntity())
        }

        override suspend fun deleteTrip(
            id: Long,
            behavior: DeleteTripBehavior,
        ) {
            transactionRunner.runInTransaction {
                val trip =
                    tripDao.getById(id)
                        ?: return@runInTransaction
                val now = timeProvider.currentTimeMillis()
                when (behavior) {
                    DeleteTripBehavior.UNTAG -> {
                        transactionDao.clearTripId(tripId = id, now = now)
                    }
                    DeleteTripBehavior.REVERT_TO_ORIGINAL_CATEGORY -> {
                        val originalCategoryId =
                            requireNotNull(trip.originalCategoryId) {
                                "REVERT requires a conversion-origin trip"
                            }
                        // EXPENSE-only per FR-24 — the conversion wizard's entry point only
                        // surfaces on EXPENSE categories, so a recreated source category is always
                        // EXPENSE. If FR-24 ever extends to INCOME, store the type on TripEntity
                        // (originalCategoryType) and pass it through here.
                        val restoredCategoryId =
                            categoryDao.getById(originalCategoryId)?.id
                                ?: categoryDao.insert(
                                    CategoryEntity(
                                        // id intentionally left as default (autoGenerate). SQLite may not
                                        // re-assign the original id; revertTripAssignments takes the
                                        // restored id explicitly so the test for id reuse stays honest.
                                        name =
                                            requireNotNull(trip.originalCategoryNameSnapshot) {
                                                "REVERT requires originalCategoryNameSnapshot"
                                            },
                                        type = CategoryEntity.TYPE_EXPENSE,
                                        iconKey = trip.originalCategoryIconSnapshot,
                                        colorKey = trip.originalCategoryColorSnapshot,
                                        isDefault = false,
                                    ),
                                )
                        transactionDao.revertTripAssignments(
                            tripId = id,
                            restoredCategoryId = restoredCategoryId,
                            now = now,
                        )
                    }
                }
                tripDao.deleteById(id)
            }
        }
    }
