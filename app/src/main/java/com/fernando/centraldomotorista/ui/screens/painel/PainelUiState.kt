package com.fernando.centraldomotorista.ui.screens.painel

import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.fernando.centraldomotorista.data.model.Profile
import com.fernando.centraldomotorista.ui.common.charts.DailyTrendBucket
import com.fernando.centraldomotorista.ui.common.charts.TrendRange
import java.math.BigDecimal
import java.time.LocalDate

data class PlatformEarningItem(
    val id: String,
    val name: String,
    val total: BigDecimal,
    val percentageOfTotal: Float // 0..100
)

data class CategoryExpenseItem(
    val category: String, // "combustivel", "manutencao", "alimentacao", "equipe"
    val label: String,
    val total: BigDecimal
)

data class PartnerExpenseItem(
    val vendorName: String, // Expense.vendor (nome do parceiro no momento do pagamento)
    val total: BigDecimal,
    val percentageOfTotal: Float // 0..100, mesmo padrão de PlatformEarningItem
)

data class PartMaintenanceAlertItem(
    val part: PartMaintenance,
    val lifeKm: BigDecimal,
    val lastChangeKm: BigDecimal,
    val drivenKm: BigDecimal,
    val wearPercentage: BigDecimal, // e.g. 95.5%
    val isOverdue: Boolean,
    val remainingKm: BigDecimal,
    val overdueKm: BigDecimal
)

data class PainelUiState(
    val profile: Profile? = null,
    val dailyEarnings: BigDecimal = BigDecimal.ZERO,
    val weeklyEarnings: BigDecimal = BigDecimal.ZERO,
    val monthlyEarnings: BigDecimal = BigDecimal.ZERO,
    val dailyPackages: Int = 0,
    val weeklyPackages: Int = 0,
    val monthlyPackages: Int = 0,
    val dailyGoal: BigDecimal = BigDecimal.ZERO,
    val weeklyGoal: BigDecimal = BigDecimal.ZERO,
    val monthlyGoal: BigDecimal = BigDecimal.ZERO,
    val dailyProgressPct: BigDecimal? = null,
    val weeklyProgressPct: BigDecimal? = null,
    val monthlyProgressPct: BigDecimal? = null,
    val platformEarnings: List<PlatformEarningItem> = emptyList(),
    val totalPlatformEarnings: BigDecimal = BigDecimal.ZERO,
    val expensesByCategory: List<CategoryExpenseItem> = emptyList(),
    val teamExpensesByPartner: List<PartnerExpenseItem> = emptyList(),
    val totalTeamExpenses: BigDecimal = BigDecimal.ZERO,
    val trendRange: TrendRange = TrendRange.SEVEN_DAYS,
    val trendBuckets: List<DailyTrendBucket> = emptyList(),
    val maxTrendAmount: BigDecimal = BigDecimal.ONE,
    val selectedTrendDay: DailyTrendBucket? = null,
    val maintenanceAlerts: List<PartMaintenanceAlertItem> = emptyList(),
    val isQuickActionsOpen: Boolean = false,
    val partToReset: PartMaintenanceAlertItem? = null,
    val isUpdatingPart: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)
