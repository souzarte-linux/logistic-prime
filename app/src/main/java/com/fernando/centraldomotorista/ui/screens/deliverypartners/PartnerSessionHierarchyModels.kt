package com.fernando.centraldomotorista.ui.screens.deliverypartners

import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.ui.common.period.PeriodFilter
import com.fernando.centraldomotorista.ui.common.period.PeriodPreset
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

typealias PartnerPeriodPreset = PeriodPreset
typealias PartnerPeriodFilter = PeriodFilter

data class PartnerSessionDayGroup(
    val date: LocalDate,
    val label: String, // "HOJE, 10 DE SET", "ONTEM, 09 DE SET", "TERÇA-FEIRA, 08 DE SET"
    val sessions: List<DeliveryPartnerSession>,
    val totalDelivered: Int = 0,
    val totalAmountPaid: BigDecimal = BigDecimal.ZERO
)

data class PartnerSessionWeekGroup(
    val weekKey: String, // Ex: "2026-09-W37"
    val label: String,   // "Esta Semana" ou "Semana de 01/09 a 07/09"
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: List<PartnerSessionDayGroup>,
    val totalDelivered: Int = 0,
    val totalAmountPaid: BigDecimal = BigDecimal.ZERO,
    val isCurrentWeek: Boolean
)

data class PartnerSessionMonthGroup(
    val monthKey: String, // Ex: "2026-09"
    val label: String,    // "SETEMBRO 2026"
    val yearMonth: YearMonth,
    val weeks: List<PartnerSessionWeekGroup>,
    val totalDelivered: Int = 0,
    val totalAmountPaid: BigDecimal = BigDecimal.ZERO,
    val isCurrentMonth: Boolean
)
