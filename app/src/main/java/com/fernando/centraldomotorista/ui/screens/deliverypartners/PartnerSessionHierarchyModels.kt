package com.fernando.centraldomotorista.ui.screens.deliverypartners

import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class PartnerPeriodPreset(val label: String) {
    DIA("Dia"),
    SEMANA("Semana"),
    QUINZENA("Quinzena"),
    MES("Mês"),
    ANO("Ano"),
    PERSONALIZADO("Intervalo")
}

data class PartnerPeriodFilter(
    val preset: PartnerPeriodPreset = PartnerPeriodPreset.SEMANA,
    val customStart: LocalDate = LocalDate.now().minusDays(7),
    val customEnd: LocalDate = LocalDate.now()
) {
    /** Espelha startOf()/endOf() do Relatorios.tsx, mas em LocalDate (sem hora). */
    fun resolveRange(today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> = when (preset) {
        PartnerPeriodPreset.DIA -> today to today
        PartnerPeriodPreset.SEMANA -> {
            // Semana começa na segunda-feira, como no startOf('semana') original
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            start to start.plusDays(6)
        }
        PartnerPeriodPreset.QUINZENA -> today.minusDays(14) to today
        PartnerPeriodPreset.MES -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
        PartnerPeriodPreset.ANO -> LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
        PartnerPeriodPreset.PERSONALIZADO -> customStart to customEnd
    }
}

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
