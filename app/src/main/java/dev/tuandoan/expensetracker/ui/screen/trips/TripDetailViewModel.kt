package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.database.entity.DailyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TripCategorySumRow
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryTotal
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.TripRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Backs [TripDetailScreen] (v3.13.0, T2.6). Read-only: loads trip metadata, KPIs,
 * category breakdown, day-by-day chart data, and transaction list for a single trip.
 *
 * All data flows are combined into a single [TripDetailUiState] so the screen
 * re-renders coherently on any upstream change (new transaction, currency switch, etc.).
 *
 * Action menu (Edit / Delete / Revert) callbacks live in T2.7.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TripDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val tripRepository: TripRepository,
        private val categoryRepository: CategoryRepository,
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
        private val currencyFormatter: CurrencyFormatter,
        clock: Clock,
    ) : ViewModel() {
        private val tripId: Long = savedStateHandle["tripId"] ?: 0L
        private val nowEpochDay: Long = LocalDate.now(clock).toEpochDay()

        private val _uiState = MutableStateFlow(TripDetailUiState())
        val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

        init {
            if (tripId <= 0L) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = UiText.DynamicString("Invalid trip id"))
                }
            } else {
                observeDetail()
            }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        private fun observeDetail() {
            viewModelScope.launch {
                tripRepository
                    .observeTripById(tripId)
                    .flatMapLatest { trip ->
                        if (trip == null) {
                            // Trip was deleted while screen was open.
                            flowOf(null to DetailAggregates())
                        } else {
                            aggregatesFlow(trip).flatMapLatest { agg ->
                                flowOf(trip to agg)
                            }
                        }
                    }.catch { e ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = ErrorUtils.getErrorMessage(e))
                        }
                    }.collect { (trip, agg) ->
                        if (trip == null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    tripGone = true,
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    trip = trip,
                                    tripStatus = trip.statusAt(nowEpochDay),
                                    totalLabel = agg.totalMinor?.let { currencyFormatter.format(it, agg.currencyCode) },
                                    dailyAvgLabel = computeDailyAvg(agg.totalMinor, trip, agg.currencyCode),
                                    transactionCount = agg.transactionCount,
                                    categoryTotals = buildCategoryTotals(agg.categoryBreakdown, agg.categoryMap),
                                    dailyPoints = buildDailyPoints(agg.dailyTotals, trip),
                                    transactions = agg.transactions,
                                )
                            }
                        }
                    }
            }
        }

        private fun aggregatesFlow(trip: Trip): Flow<DetailAggregates> {
            val totalFlow = tripRepository.observeTripTotal(trip.id)
            val countFlow = tripRepository.observeTripTransactionCount(trip.id)
            val dailyFlow = tripRepository.observeTripDailyTotals(trip.id)
            val breakdownFlow = tripRepository.observeTripCategoryBreakdown(trip.id)
            val txFlow = tripRepository.observeTripTransactions(trip.id)
            val currencyFlow = currencyPreferenceRepository.observeDefaultCurrency().distinctUntilChanged()
            val categoryFlow =
                combine(
                    categoryRepository.observeCategories(TransactionType.EXPENSE),
                    categoryRepository.observeCategories(TransactionType.INCOME),
                ) { exp, inc -> (exp + inc).associateBy { it.id } }

            return combine(
                combine(totalFlow, countFlow, dailyFlow) { total, count, daily ->
                    Triple(total, count, daily)
                },
                combine(breakdownFlow, txFlow, currencyFlow) { breakdown, txs, code ->
                    Triple(breakdown, txs, code)
                },
                categoryFlow,
            ) { (total, count, daily), (breakdown, txs, code), catMap ->
                DetailAggregates(
                    totalMinor = total,
                    transactionCount = count,
                    dailyTotals = daily,
                    categoryBreakdown = breakdown,
                    transactions = txs,
                    currencyCode = code,
                    categoryMap = catMap,
                )
            }
        }

        private fun computeDailyAvg(
            totalMinor: Long?,
            trip: Trip,
            currencyCode: String,
        ): String? {
            totalMinor ?: return null
            val days = maxOf(1L, trip.endDateEpochDay - trip.startDateEpochDay + 1L)
            return currencyFormatter.format(totalMinor / days, currencyCode)
        }

        private fun buildCategoryTotals(
            rows: List<TripCategorySumRow>,
            categoryMap: Map<Long, Category>,
        ): List<CategoryTotal> =
            rows.mapNotNull { row ->
                categoryMap[row.categoryId]?.let { cat -> CategoryTotal(cat, row.total) }
            }

        private fun buildDailyPoints(
            rows: List<DailyTotalRow>,
            trip: Trip,
        ): List<DailyBarPoint> {
            if (rows.isEmpty()) return emptyList()
            val durationDays = (trip.endDateEpochDay - trip.startDateEpochDay + 1L).toInt()
            return if (durationDays <= MAX_BARS) {
                rows.map { DailyBarPoint(dayLabel = it.date, totalMinor = it.total) }
            } else {
                // Bucket into weekly groups so the bar count stays ≤ MAX_BARS.
                rows
                    .groupBy { row ->
                        val epochDay = LocalDate.parse(row.date).toEpochDay()
                        val offset = (epochDay - trip.startDateEpochDay).toInt()
                        offset / 7
                    }.entries
                    .sortedBy { it.key }
                    .map { (weekIndex, weekRows) ->
                        DailyBarPoint(
                            dayLabel = "W${weekIndex + 1}",
                            totalMinor = weekRows.sumOf { it.total },
                        )
                    }
            }
        }

        private fun Trip.statusAt(nowDay: Long): TripStatus =
            when {
                nowDay < startDateEpochDay -> TripStatus.UPCOMING
                nowDay > endDateEpochDay -> TripStatus.PAST
                else -> TripStatus.ACTIVE
            }

        private data class DetailAggregates(
            val totalMinor: Long? = null,
            val transactionCount: Int = 0,
            val dailyTotals: List<DailyTotalRow> = emptyList(),
            val categoryBreakdown: List<TripCategorySumRow> = emptyList(),
            val transactions: List<Transaction> = emptyList(),
            val currencyCode: String = "",
            val categoryMap: Map<Long, Category> = emptyMap(),
        )

        companion object {
            private const val MAX_BARS = 31
        }
    }

enum class TripStatus { ACTIVE, UPCOMING, PAST }

data class DailyBarPoint(
    val dayLabel: String,
    val totalMinor: Long,
)

data class TripDetailUiState(
    val isLoading: Boolean = true,
    val trip: Trip? = null,
    val tripStatus: TripStatus = TripStatus.UPCOMING,
    val totalLabel: String? = null,
    val dailyAvgLabel: String? = null,
    val transactionCount: Int = 0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val dailyPoints: List<DailyBarPoint> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val errorMessage: UiText? = null,
    /** True when the trip was deleted while the screen was open → screen should pop. */
    val tripGone: Boolean = false,
)
