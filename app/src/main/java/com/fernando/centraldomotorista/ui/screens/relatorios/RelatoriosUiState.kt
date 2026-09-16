package com.fernando.centraldomotorista.ui.screens.relatorios

import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.ui.common.charts.FutureCashFlowItem
import com.fernando.centraldomotorista.ui.common.charts.PerformanceTimelineBucket
import com.fernando.centraldomotorista.ui.common.charts.PlatformProfitabilityBarItem
import com.fernando.centraldomotorista.ui.common.period.PeriodFilter

data class RelatoriosUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val periodFilter: PeriodFilter = PeriodFilter(),
    val isPeriodDropdownExpanded: Boolean = false,
    val selectedPlatformId: String = "all",
    val isPlatformDropdownExpanded: Boolean = false,
    val categoryMetric: CategoryMetric = CategoryMetric.AMOUNT,
    val platforms: List<Platform> = emptyList(),
    val stats: RelatoriosStats = RelatoriosStats(),
    val maintCostBreakdown: MaintCostBreakdown = MaintCostBreakdown(),
    val maintSchedule: List<MaintScheduleItem> = emptyList(),
    val timelineBuckets: List<PerformanceTimelineBucket> = emptyList(),
    val selectedTimelineBucket: PerformanceTimelineBucket? = null,
    val futureCashFlow: List<FutureCashFlowItem> = emptyList(),
    val platformProfitability: List<PlatformProfitabilityBarItem> = emptyList(),
    val bonificacoes: List<PlatformAdjustmentGroup> = emptyList(),
    val deliveredCategories: List<DeliveredCategoryItem> = emptyList(),
    val topOrigins: List<RankedPlaceItem> = emptyList(),
    val topDestinations: List<RankedPlaceItem> = emptyList(),
    val errorMessage: String? = null
)
