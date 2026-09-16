package com.fernando.centraldomotorista.ui.screens.relatorios

import androidx.compose.ui.graphics.Color
import java.math.BigDecimal

enum class CategoryMetric {
    AMOUNT,
    COUNT
}

enum class MaintScheduleStatus {
    OK,
    WARN,
    CRITICAL
}

data class RelatoriosStats(
    val totalRevenue: BigDecimal = BigDecimal.ZERO,
    val totalKm: BigDecimal = BigDecimal.ZERO,
    val totalExpense: BigDecimal = BigDecimal.ZERO,
    val profit: BigDecimal = BigDecimal.ZERO,
    val hours: BigDecimal = BigDecimal.ZERO,
    val averageDailyHours: BigDecimal = BigDecimal.ZERO,
    val revPerKm: BigDecimal = BigDecimal.ZERO,
    val costPerKm: BigDecimal = BigDecimal.ZERO,
    val profitPerKm: BigDecimal = BigDecimal.ZERO,
    val revPerHour: BigDecimal = BigDecimal.ZERO,
    val profitPerHour: BigDecimal = BigDecimal.ZERO,
    val routeCount: Int = 0,
    val totalSmallPackages: Int = 0,
    val totalLargePackages: Int = 0,
    val totalPackages: Int = 0,
    val smallPackagesValue: BigDecimal = BigDecimal.ZERO,
    val largePackagesValue: BigDecimal = BigDecimal.ZERO,
    val avgTicket: BigDecimal = BigDecimal.ZERO,
    val avgPackagePrice: BigDecimal = BigDecimal.ZERO,
    val realConsumptionKml: BigDecimal = BigDecimal.ZERO,
    val currentOdometer: BigDecimal = BigDecimal.ZERO
)

data class MaintCostBreakdown(
    val fuel: BigDecimal = BigDecimal.ZERO,
    val oil: BigDecimal = BigDecimal.ZERO,
    val parts: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ZERO
)

data class MaintScheduleItem(
    val name: String,
    val lifeKm: BigDecimal,
    val lastKm: BigDecimal,
    val nextKm: BigDecimal,
    val remainingKm: BigDecimal,
    val status: MaintScheduleStatus,
    val expenseId: String? = null
)

data class AdjustmentDetail(
    val type: String,
    val label: String,
    val amount: BigDecimal,
    val isDiscount: Boolean
)

data class PlatformAdjustmentGroup(
    val platformId: String,
    val name: String,
    val descontosTotal: BigDecimal = BigDecimal.ZERO,
    val acrescimosTotal: BigDecimal = BigDecimal.ZERO,
    val details: List<AdjustmentDetail> = emptyList()
)

data class DeliveredCategoryItem(
    val name: String,
    val productType: String,
    val count: Int,
    val amount: BigDecimal,
    val color: Color
)

data class RankedPlaceItem(
    val name: String,
    val count: Int
)

// Paleta de cores para gráficos inspirada no tema escuro do pocket-pwa-builder
val RelatoriosChartColors = listOf(
    Color(0xFFFF6D00), // Laranja Neon
    Color(0xFFFFB300), // Âmbar Ouro
    Color(0xFF2979FF), // Azul Elétrico
    Color(0xFF00E676), // Verde Esmeralda
    Color(0xFFAA00FF), // Roxo Vibrante
    Color(0xFFFF1744), // Vermelho Coral
    Color(0xFF00B0FF), // Ciano
    Color(0xFFFF9100)  // Laranja Quente
)
