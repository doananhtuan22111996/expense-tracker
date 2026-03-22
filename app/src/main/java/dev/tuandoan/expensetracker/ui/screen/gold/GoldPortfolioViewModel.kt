package dev.tuandoan.expensetracker.ui.screen.gold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldHoldingWithPnL
import dev.tuandoan.expensetracker.domain.model.GoldPortfolioSummary
import dev.tuandoan.expensetracker.domain.model.GoldPrice
import dev.tuandoan.expensetracker.domain.model.GoldType
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

        init {
            loadPortfolio()
        }

        private fun loadPortfolio() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)

                combine(
                    goldRepository.observeAllHoldings(),
                    goldRepository.observeAllPrices(),
                    currencyPreferenceRepository.observeDefaultCurrency(),
                ) { holdings, prices, currencyCode ->
                    buildPortfolioState(holdings, prices, currencyCode)
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
                            showPricesUpdated = current.showPricesUpdated,
                        )
                }
            }
        }

        fun deleteHolding(holding: GoldHolding) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(lastDeletedHolding = holding)
                goldRepository.deleteHolding(holding.id)
            }
        }

        fun undoDelete() {
            val holding = _uiState.value.lastDeletedHolding ?: return
            viewModelScope.launch {
                goldRepository.addHolding(holding)
                _uiState.value = _uiState.value.copy(lastDeletedHolding = null)
            }
        }

        fun clearLastDeleted() {
            _uiState.value = _uiState.value.copy(lastDeletedHolding = null)
        }

        fun savePrices(priceInputs: Map<Pair<GoldType, GoldWeightUnit>, Long>) {
            viewModelScope.launch {
                val currencyCode = currencyPreferenceRepository.getDefaultCurrency()
                val prices =
                    priceInputs.map { (key, amount) ->
                        GoldPrice(
                            type = key.first,
                            unit = key.second,
                            pricePerUnit = amount,
                            currencyCode = currencyCode,
                        )
                    }
                goldRepository.upsertPrices(prices)
                _uiState.value = _uiState.value.copy(showPricesUpdated = true)
            }
        }

        fun clearPricesUpdatedFlag() {
            _uiState.value = _uiState.value.copy(showPricesUpdated = false)
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(isError = false, errorMessage = null)
        }

        private fun buildPortfolioState(
            holdings: List<GoldHolding>,
            prices: List<GoldPrice>,
            currencyCode: String,
        ): GoldPortfolioUiState {
            val priceMap =
                prices.associateBy { it.type to it.unit }

            val holdingsWithPnL =
                holdings.map { holding ->
                    val currentPrice = priceMap[holding.type to holding.weightUnit]
                    GoldHoldingWithPnL(
                        holding = holding,
                        currentPricePerUnit = currentPrice?.pricePerUnit,
                    )
                }

            val holdingsWithPrice = holdingsWithPnL.filter { it.currentPricePerUnit != null }
            val summary =
                if (holdingsWithPrice.isNotEmpty()) {
                    GoldPortfolioSummary(
                        totalCost = holdingsWithPrice.sumOf { it.totalCost },
                        totalCurrentValue = holdingsWithPrice.sumOf { it.currentValue!! },
                        currencyCode = currencyCode,
                    )
                } else {
                    null
                }

            val distinctCombos =
                holdings.map { it.type to it.weightUnit }.distinct()
            val currentPrices =
                distinctCombos.map { combo ->
                    priceMap[combo] ?: GoldPrice(
                        type = combo.first,
                        unit = combo.second,
                        pricePerUnit = 0L,
                        currencyCode = currencyCode,
                    )
                }

            return GoldPortfolioUiState(
                holdings = holdingsWithPnL,
                summary = summary,
                currentPrices = currentPrices,
                currencyCode = currencyCode,
                isLoading = false,
                isError = false,
            )
        }
    }

data class GoldPortfolioUiState(
    val holdings: List<GoldHoldingWithPnL> = emptyList(),
    val summary: GoldPortfolioSummary? = null,
    val currentPrices: List<GoldPrice> = emptyList(),
    val currencyCode: String = "VND",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val lastDeletedHolding: GoldHolding? = null,
    val showPricesUpdated: Boolean = false,
)
