package dev.tuandoan.expensetracker.domain.repository

import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldPrice
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
import kotlinx.coroutines.flow.Flow

interface GoldRepository {
    fun observeAllHoldings(): Flow<List<GoldHolding>>

    suspend fun getHolding(id: Long): GoldHolding?

    suspend fun addHolding(holding: GoldHolding): Long

    suspend fun updateHolding(holding: GoldHolding)

    suspend fun deleteHolding(id: Long)

    fun observeAllPrices(): Flow<List<GoldPrice>>

    suspend fun getPrice(
        type: GoldType,
        unit: GoldWeightUnit,
    ): GoldPrice?

    suspend fun upsertPrice(price: GoldPrice)

    suspend fun upsertPrices(prices: List<GoldPrice>)
}
