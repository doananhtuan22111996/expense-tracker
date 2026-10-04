package dev.tuandoan.expensetracker.ui.screen.gold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldHoldingWithPnL
import dev.tuandoan.expensetracker.domain.model.GoldPortfolioSummary
import dev.tuandoan.expensetracker.domain.model.GoldPrice
import dev.tuandoan.expensetracker.domain.model.GoldSale
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldTypeAllocation
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.GoldRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoldPortfolioViewModel
    @Inject
    constructor(
        private val goldRepository: GoldRepository,
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(GoldPortfolioUiState())
        val uiState: StateFlow<GoldPortfolioUiState> = _uiState.asStateFlow()

        private val selectedTypeFilterFlow = MutableStateFlow<GoldType?>(null)
        private val sortOptionFlow = MutableStateFlow(GoldHoldingSortOption.BUY_DATE_DESC)
        private val selectedTabFlow = MutableStateFlow(GoldPortfolioTab.HOLDINGS)

        init {
            loadPortfolio()
        }

        private fun loadPortfolio() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)

                combine(
                    combine(
                        goldRepository.observeAllHoldings(),
                        goldRepository.observeAllPrices(),
                        goldRepository.observeAllSales(),
                    ) { holdings, prices, sales -> Triple(holdings, prices, sales) },
                    currencyPreferenceRepository.observeDefaultCurrency(),
                    selectedTypeFilterFlow,
                    sortOptionFlow,
                    selectedTabFlow,
                ) { (holdings, prices, sales), currencyCode, typeFilter, sortOption, selectedTab ->
                    buildPortfolioState(
                        holdings = holdings,
                        prices = prices,
                        sales = sales,
                        currencyCode = currencyCode,
                        typeFilter = typeFilter,
                        sortOption = sortOption,
                        selectedTab = selectedTab,
                    )
                }.catch { e ->
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }.collect { state ->
                    val current = _uiState.value
                    _uiState.value =
                        state.copy(
                            lastDeletedHolding = current.lastDeletedHolding,
                            lastDeletedSale = current.lastDeletedSale,
                            showPricesUpdated = current.showPricesUpdated,
                            showSaleSuccess = current.showSaleSuccess,
                            holdingToSell = current.holdingToSell,
                        )
                }
            }
        }

        fun selectTab(tab: GoldPortfolioTab) {
            selectedTabFlow.value = tab
        }

        fun setTypeFilter(type: GoldType?) {
            selectedTypeFilterFlow.value = type
        }

        fun setSortOption(sortOption: GoldHoldingSortOption) {
            sortOptionFlow.value = sortOption
        }

        fun startSell(holdingWithPnL: GoldHoldingWithPnL) {
            _uiState.value = _uiState.value.copy(holdingToSell = holdingWithPnL)
        }

        fun cancelSell() {
            _uiState.value = _uiState.value.copy(holdingToSell = null)
        }

        fun recordSale(
            holdingId: Long,
            soldWeight: Double,
            sellPricePerUnit: Long,
            saleDateMillis: Long,
            note: String?,
        ) {
            viewModelScope.launch {
                try {
                    goldRepository.recordSale(
                        holdingId = holdingId,
                        soldWeight = soldWeight,
                        sellPricePerUnit = sellPricePerUnit,
                        saleDateMillis = saleDateMillis,
                        note = note,
                    )
                    _uiState.value =
                        _uiState.value.copy(
                            holdingToSell = null,
                            showSaleSuccess = true,
                        )
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        fun clearSaleSuccessFlag() {
            _uiState.value = _uiState.value.copy(showSaleSuccess = false)
        }

        fun deleteSale(sale: GoldSale) {
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(lastDeletedSale = sale)
                    goldRepository.deleteSale(sale.id)
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            lastDeletedSale = null,
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        fun clearLastDeletedSale() {
            _uiState.value = _uiState.value.copy(lastDeletedSale = null)
        }

        fun deleteHolding(holding: GoldHolding) {
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(lastDeletedHolding = holding)
                    goldRepository.deleteHolding(holding.id)
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            lastDeletedHolding = null,
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        fun undoDelete() {
            val holding = _uiState.value.lastDeletedHolding ?: return
            viewModelScope.launch {
                try {
                    goldRepository.addHolding(holding)
                    _uiState.value = _uiState.value.copy(lastDeletedHolding = null)
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        fun clearLastDeleted() {
            _uiState.value = _uiState.value.copy(lastDeletedHolding = null)
        }

        fun savePrices(priceInputs: Map<Pair<GoldType, GoldWeightUnit>, PriceInput>) {
            viewModelScope.launch {
                try {
                    val currencyCode = currencyPreferenceRepository.getDefaultCurrency()
                    val prices =
                        priceInputs.map { (key, input) ->
                            GoldPrice(
                                type = key.first,
                                unit = key.second,
                                sellPricePerUnit = input.sellPrice,
                                buyBackPricePerUnit = input.buyBackPrice,
                                currencyCode = currencyCode,
                            )
                        }
                    goldRepository.upsertPrices(prices)
                    _uiState.value = _uiState.value.copy(showPricesUpdated = true)
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        fun clearPricesUpdatedFlag() {
            _uiState.value = _uiState.value.copy(showPricesUpdated = false)
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(isError = false, errorMessage = null)
        }

        fun retry() {
            clearError()
            loadPortfolio()
        }

        private fun buildPortfolioState(
            holdings: List<GoldHolding>,
            prices: List<GoldPrice>,
            sales: List<GoldSale>,
            currencyCode: String,
            typeFilter: GoldType?,
            sortOption: GoldHoldingSortOption,
            selectedTab: GoldPortfolioTab,
        ): GoldPortfolioUiState {
            val priceMap =
                prices.associateBy { it.type to it.unit }

            val allHoldingsWithPnL =
                holdings.map { holding ->
                    val currentPrice = priceMap[holding.type to holding.weightUnit]
                    GoldHoldingWithPnL(
                        holding = holding,
                        currentSellPricePerUnit = currentPrice?.sellPricePerUnit,
                        currentBuyBackPricePerUnit = currentPrice?.buyBackPricePerUnit,
                    )
                }

            val holdingsWithPrice = allHoldingsWithPnL.filter { it.currentSellPricePerUnit != null }
            val totalWeightGrams = holdings.sumOf { it.weightInGrams() }
            val totalRealizedPnL = sales.sumOf { it.realizedPnL }
            val totalRealizedProceeds = sales.sumOf { it.totalProceeds }

            val summary =
                if (holdingsWithPrice.isNotEmpty() || sales.isNotEmpty()) {
                    val totalLiquidation =
                        if (holdingsWithPrice.any { it.currentBuyBackPricePerUnit != null }) {
                            holdingsWithPrice.sumOf { it.liquidationValue ?: it.marketValue ?: 0L }
                        } else {
                            null
                        }
                    GoldPortfolioSummary(
                        totalCost = holdingsWithPrice.sumOf { it.totalCost },
                        totalMarketValue = holdingsWithPrice.sumOf { it.marketValue ?: 0L },
                        totalLiquidationValue = totalLiquidation,
                        totalRealizedPnL = totalRealizedPnL,
                        totalRealizedProceeds = totalRealizedProceeds,
                        totalWeightGrams = totalWeightGrams,
                        currencyCode = currencyCode,
                    )
                } else {
                    null
                }

            val allocations = computeAllocations(allHoldingsWithPnL)

            val distinctCombos =
                holdings.map { it.type to it.weightUnit }.distinct()
            val currentPrices =
                distinctCombos.map { combo ->
                    priceMap[combo] ?: GoldPrice(
                        type = combo.first,
                        unit = combo.second,
                        sellPricePerUnit = 0L,
                        currencyCode = currencyCode,
                    )
                }

            val filteredHoldings =
                if (typeFilter != null) {
                    allHoldingsWithPnL.filter { it.holding.type == typeFilter }
                } else {
                    allHoldingsWithPnL
                }

            val sortedHoldings = sortHoldings(filteredHoldings, sortOption)

            return GoldPortfolioUiState(
                holdings = sortedHoldings,
                allHoldings = allHoldingsWithPnL,
                sales = sales,
                selectedTab = selectedTab,
                summary = summary,
                allocations = allocations,
                totalWeightGrams = totalWeightGrams,
                currentPrices = currentPrices,
                currencyCode = currencyCode,
                selectedTypeFilter = typeFilter,
                sortOption = sortOption,
                isLoading = false,
                isError = false,
            )
        }

        private fun computeAllocations(holdings: List<GoldHoldingWithPnL>): List<GoldTypeAllocation> {
            if (holdings.isEmpty()) return emptyList()

            val byType = holdings.groupBy { it.holding.type }
            val totalPortfolioValue =
                holdings.sumOf { it.liquidationValue ?: it.marketValue ?: it.totalCost }
            val totalPortfolioCost = holdings.sumOf { it.totalCost }

            return byType
                .map { (type, typeHoldings) ->
                    val typeWeightGrams = typeHoldings.sumOf { it.holding.weightInGrams() }
                    val typeCost = typeHoldings.sumOf { it.totalCost }
                    val typeValue =
                        typeHoldings.sumOf { it.liquidationValue ?: it.marketValue ?: it.totalCost }
                    val percentage =
                        when {
                            totalPortfolioValue > 0 -> (typeValue.toDouble() / totalPortfolioValue) * 100.0
                            totalPortfolioCost > 0 -> (typeCost.toDouble() / totalPortfolioCost) * 100.0
                            else -> 0.0
                        }
                    GoldTypeAllocation(
                        type = type,
                        totalWeightGrams = typeWeightGrams,
                        totalCost = typeCost,
                        currentValue = typeValue,
                        percentageOfPortfolio = percentage,
                    )
                }.sortedByDescending { it.percentageOfPortfolio }
        }

        private fun sortHoldings(
            holdings: List<GoldHoldingWithPnL>,
            sortOption: GoldHoldingSortOption,
        ): List<GoldHoldingWithPnL> =
            when (sortOption) {
                GoldHoldingSortOption.BUY_DATE_DESC ->
                    holdings.sortedWith(
                        compareByDescending<GoldHoldingWithPnL> { it.holding.buyDateMillis }
                            .thenByDescending { it.holding.id },
                    )
                GoldHoldingSortOption.BUY_DATE_ASC ->
                    holdings.sortedWith(
                        compareBy<GoldHoldingWithPnL> { it.holding.buyDateMillis }
                            .thenBy { it.holding.id },
                    )
                GoldHoldingSortOption.VALUE_DESC ->
                    holdings.sortedWith(
                        compareByDescending<GoldHoldingWithPnL> {
                            it.liquidationValue ?: it.marketValue ?: it.totalCost
                        }.thenByDescending { it.holding.id },
                    )
                GoldHoldingSortOption.PNL_DESC ->
                    holdings.sortedWith(
                        compareByDescending<GoldHoldingWithPnL> {
                            it.liquidationPnLPercent ?: it.marketPnLPercent ?: -Double.MAX_VALUE
                        }.thenByDescending { it.holding.id },
                    )
                GoldHoldingSortOption.WEIGHT_DESC ->
                    holdings.sortedWith(
                        compareByDescending<GoldHoldingWithPnL> { it.holding.weightInGrams() }
                            .thenByDescending { it.holding.id },
                    )
            }
    }

enum class GoldPortfolioTab {
    HOLDINGS,
    SALES,
}

enum class GoldHoldingSortOption {
    BUY_DATE_DESC,
    BUY_DATE_ASC,
    VALUE_DESC,
    PNL_DESC,
    WEIGHT_DESC,
}

data class PriceInput(
    val sellPrice: Long,
    val buyBackPrice: Long? = null,
)

data class GoldPortfolioUiState(
    val holdings: List<GoldHoldingWithPnL> = emptyList(),
    val allHoldings: List<GoldHoldingWithPnL> = emptyList(),
    val sales: List<GoldSale> = emptyList(),
    val selectedTab: GoldPortfolioTab = GoldPortfolioTab.HOLDINGS,
    val summary: GoldPortfolioSummary? = null,
    val allocations: List<GoldTypeAllocation> = emptyList(),
    val totalWeightGrams: Double = 0.0,
    val currentPrices: List<GoldPrice> = emptyList(),
    val currencyCode: String = "VND",
    val selectedTypeFilter: GoldType? = null,
    val sortOption: GoldHoldingSortOption = GoldHoldingSortOption.BUY_DATE_DESC,
    val holdingToSell: GoldHoldingWithPnL? = null,
    val lastDeletedSale: GoldSale? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: UiText? = null,
    val lastDeletedHolding: GoldHolding? = null,
    val showPricesUpdated: Boolean = false,
    val showSaleSuccess: Boolean = false,
) {
    val totalHoldingsCount: Int get() = allHoldings.size
    val totalSalesCount: Int get() = sales.size
    val hasActiveFilter: Boolean get() = selectedTypeFilter != null
    val totalWeightTaels: Double get() = totalWeightGrams / GoldWeightUnit.TAEL.gramsPerUnit
}
