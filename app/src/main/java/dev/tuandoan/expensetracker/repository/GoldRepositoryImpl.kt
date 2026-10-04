package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.GoldHoldingDao
import dev.tuandoan.expensetracker.data.database.dao.GoldPriceDao
import dev.tuandoan.expensetracker.data.database.dao.GoldSaleDao
import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity
import dev.tuandoan.expensetracker.data.database.entity.GoldSaleEntity
import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldPrice
import dev.tuandoan.expensetracker.domain.model.GoldSale
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
        private val saleDao: GoldSaleDao,
        private val transactionRunner: TransactionRunner,
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

        override fun observeAllSales(): Flow<List<GoldSale>> =
            saleDao.observeAll().map { entities ->
                entities.map { it.toDomain() }
            }

        override suspend fun getAllSales(): List<GoldSale> = saleDao.getAll().map { it.toDomain() }

        override suspend fun getSale(id: Long): GoldSale? = saleDao.getById(id)?.toDomain()

        override suspend fun recordSale(
            holdingId: Long,
            soldWeight: Double,
            sellPricePerUnit: Long,
            saleDateMillis: Long,
            note: String?,
        ): Long =
            transactionRunner.runInTransaction {
                val holding =
                    holdingDao.getById(holdingId)
                        ?: throw IllegalArgumentException("Holding not found with id $holdingId")
                require(soldWeight > 0.0) { "Sold weight must be positive" }
                require(soldWeight <= holding.weightValue) { "Sold weight cannot exceed holding weight" }
                require(sellPricePerUnit > 0L) { "Sell price per unit must be positive" }

                val now = timeProvider.currentTimeMillis()
                val saleId =
                    saleDao.insert(
                        GoldSaleEntity(
                            holdingId = holding.id,
                            type = holding.type,
                            soldWeight = soldWeight,
                            weightUnit = holding.weightUnit,
                            buyPricePerUnit = holding.buyPricePerUnit,
                            sellPricePerUnit = sellPricePerUnit,
                            currencyCode = holding.currencyCode,
                            saleDateMillis = saleDateMillis,
                            note = note,
                            createdAt = now,
                        ),
                    )

                if (soldWeight == holding.weightValue) {
                    holdingDao.deleteById(holding.id)
                } else {
                    holdingDao.update(
                        holding.copy(
                            weightValue = holding.weightValue - soldWeight,
                            updatedAt = now,
                        ),
                    )
                }
                saleId
            }

        override suspend fun deleteSale(id: Long) =
            transactionRunner.runInTransaction {
                val sale = saleDao.getById(id) ?: return@runInTransaction
                val now = timeProvider.currentTimeMillis()

                val holdingId = sale.holdingId
                val existingHolding = if (holdingId != null) holdingDao.getById(holdingId) else null

                if (existingHolding != null) {
                    // Reconstitute weight back into existing holding
                    holdingDao.update(
                        existingHolding.copy(
                            weightValue = existingHolding.weightValue + sale.soldWeight,
                            updatedAt = now,
                        ),
                    )
                } else {
                    // Holding was fully liquidated and deleted, recreate it
                    holdingDao.insert(
                        GoldHoldingEntity(
                            id = holdingId ?: 0L,
                            type = sale.type,
                            weightValue = sale.soldWeight,
                            weightUnit = sale.weightUnit,
                            buyPricePerUnit = sale.buyPricePerUnit,
                            currencyCode = sale.currencyCode,
                            buyDateMillis = sale.saleDateMillis,
                            note = sale.note,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
                saleDao.deleteById(id)
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
                sellPricePerUnit = pricePerUnit,
                buyBackPricePerUnit = buyBackPricePerUnit,
                currencyCode = currencyCode,
                updatedAt = updatedAt,
            )

        private fun GoldPrice.toEntity(updatedAt: Long): GoldPriceEntity =
            GoldPriceEntity(
                type = type.name,
                unit = unit.name,
                pricePerUnit = sellPricePerUnit,
                buyBackPricePerUnit = buyBackPricePerUnit,
                currencyCode = currencyCode,
                updatedAt = updatedAt,
            )

        private fun GoldSaleEntity.toDomain(): GoldSale =
            GoldSale(
                id = id,
                holdingId = holdingId,
                type = GoldType.fromString(type),
                soldWeight = soldWeight,
                weightUnit = GoldWeightUnit.fromString(weightUnit),
                buyPricePerUnit = buyPricePerUnit,
                sellPricePerUnit = sellPricePerUnit,
                currencyCode = currencyCode,
                saleDateMillis = saleDateMillis,
                note = note,
                createdAt = createdAt,
            )

        private fun GoldSale.toEntity(createdAt: Long): GoldSaleEntity =
            GoldSaleEntity(
                id = id,
                holdingId = holdingId,
                type = type.name,
                soldWeight = soldWeight,
                weightUnit = weightUnit.name,
                buyPricePerUnit = buyPricePerUnit,
                sellPricePerUnit = sellPricePerUnit,
                currencyCode = currencyCode,
                saleDateMillis = saleDateMillis,
                note = note,
                createdAt = createdAt,
            )
    }
