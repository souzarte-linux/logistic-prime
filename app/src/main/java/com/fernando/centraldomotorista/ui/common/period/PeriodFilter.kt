package com.fernando.centraldomotorista.ui.common.period

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class PeriodPreset(val label: String) {
    DIA("Dia"),
    SEMANA("Semana"),
    QUINZENA("Quinzena"),
    MES("Mês"),
    ANO("Ano"),
    PERSONALIZADO("Intervalo")
}

data class PeriodFilter(
    val preset: PeriodPreset = PeriodPreset.SEMANA,
    val customStart: LocalDate = LocalDate.now().minusDays(7),
    val customEnd: LocalDate = LocalDate.now()
) {
    /** Espelha startOf()/endOf() do Relatorios.tsx, mas em LocalDate (sem hora). */
    fun resolveRange(today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> = when (preset) {
        PeriodPreset.DIA -> today to today
        PeriodPreset.SEMANA -> {
            // Semana começa na segunda-feira, como no startOf('semana') original
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            start to start.plusDays(6)
        }
        PeriodPreset.QUINZENA -> today.minusDays(14) to today
        PeriodPreset.MES -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
        PeriodPreset.ANO -> LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
        PeriodPreset.PERSONALIZADO -> customStart to customEnd
    }
}
