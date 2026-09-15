package com.fernando.centraldomotorista.ui.common.charts

import java.math.BigDecimal
import java.time.LocalDate

enum class TrendRange(val label: String) {
    SEVEN_DAYS("7D"),
    THIRTY_DAYS("30D")
}

data class DailyTrendBucket(
    val date: LocalDate,
    val dayOfWeekLabel: String, // "Seg", "Ter", etc.
    val fullDayOfWeekLabel: String, // "Segunda-feira", etc.
    val dateLabel: String, // "12/09"
    val totalAmount: BigDecimal,
    val packageCount: Int,
    val isToday: Boolean
)
