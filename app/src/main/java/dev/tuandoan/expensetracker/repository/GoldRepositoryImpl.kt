package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.database.dao.GoldHoldingDao
import dev.tuandoan.expensetracker.data.database.dao.GoldPriceDao
import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity
import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldPrice
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
import dev.tuandoan.expensetracker.domain.repository.GoldRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoldRepositoryImpl
    @Inject
    constructor(
        private val holdingDao: GoldHoldingDao,
        private val priceDao: GoldPriceDao,
        private val timeProvider: TimeProvider,
    ) : GoldRepository {
        override fun observeAllHoldings(): Flow<List<GoldHolding>> =
            holdingDao.observeAll().map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun getHolding(id: Long): GoldHolding? = holdingDao.getById(id)?.toDomain()

        override suspend fun addHolding(holding: GoldHolding): Long {
            val now = timeProvider.currentTimeMillis()
            return holdingDao.insert(holding.toEntity(createdAt = now, updatedAt = now))
        }

        override suspend fun updateHolding(holding: GoldHolding) {
            val now = timeProvider.currentTimeMillis()
            holdingDao.update(
                holding.toEntity(createdAt = holding.createdAt, updatedAt = now),
            )
        }

        override suspend fun deleteHolding(id: Long) {
            holdingDao.deleteById(id)
        }

        override fun observeAllPrices(): Flow<List<GoldPrice>> =
            priceDao.observeAll().map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun getPrice(
            type: GoldType,
            unit: GoldWeightUnit,
        ): GoldPrice? = priceDao.getByTypeAndUnit(type.name, unit.name)?.toDomain()

        override suspend fun upsertPrice(price: GoldPrice) {
            val now = timeProvider.currentTimeMillis()
            priceDao.upsert(price.toEntity(updatedAt = now))
        }

        override suspend fun upsertPrices(prices: List<GoldPrice>) {
            val now = timeProvider.currentTimeMillis()
            priceDao.upsertAll(prices.map { it.toEntity(updatedAt = now) })
        }

        private fun GoldHoldingEntity.toDomain(): GoldHolding =
            GoldHolding(
                id = id,
                type = GoldType.fromString(type),
                weightValue = weightValue,
                weightUnit = GoldWeightUnit.fromString(weightUnit),
                buyPricePerUnit = buyPricePerUnit,
                currencyCode = currencyCode,
                buyDateMillis = buyDateMillis,
                note = note,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun GoldHolding.toEntity(
            createdAt: Long,
            updatedAt: Long,
        ): GoldHoldingEntity =
            GoldHoldingEntity(
                id = id,
                type = type.name,
                weightValue = weightValue,
                weightUnit = weightUnit.name,
                buyPricePerUnit = buyPricePerUnit,
                currencyCode = currencyCode,
                buyDateMillis = buyDateMillis,
                note = note,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun GoldPriceEntity.toDomain(): GoldPrice =
            GoldPrice(
                type = GoldType.fromString(type),
                unit = GoldWeightUnit.fromString(unit),
                pricePerUnit = pricePerUnit,
                currencyCode = currencyCode,
                updatedAt = updatedAt,
            )

        private fun GoldPrice.toEntity(updatedAt: Long): GoldPriceEntity =
            GoldPriceEntity(
                type = type.name,
                unit = unit.name,
                pricePerUnit = pricePerUnit,
                currencyCode = currencyCode,
                updatedAt = updatedAt,
            )
    }
