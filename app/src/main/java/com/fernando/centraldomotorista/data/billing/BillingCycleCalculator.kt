package com.fernando.centraldomotorista.data.billing

import com.fernando.centraldomotorista.data.model.CycleEntry
import com.fernando.centraldomotorista.data.model.Platform
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class CycleInterval(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val expectedPaymentDate: LocalDate
)

object BillingCycleCalculator {

    /**
     * Formata o rótulo amigável do ciclo da plataforma (idêntico ao PWA de referência).
     */
    fun getCycleDisplayLabel(cycle: String?, paymentDay: String? = null): String {
        if (cycle.isNullOrBlank()) return "SEMANAL"
        val normalized = cycle.lowercase().trim()
        return when (normalized) {
            "semanal" -> {
                if (!paymentDay.isNullOrBlank()) {
                    val abbrev = paymentDay.take(3).uppercase()
                    "SEMANAL ($abbrev)"
                } else {
                    "SEMANAL"
                }
            }
            "quinzenal" -> "QUINZENAL"
            "mensal" -> "MENSAL"
            "misto", "variavel" -> "VARIÁVEL"
            "diario" -> "DIÁRIO"
            else -> cycle.uppercase()
        }
    }

    /**
     * Retorna os intervalos do ciclo ATUAL (c0) e do PRÓXIMO ciclo (c1) para uma plataforma,
     * baseando-se na data de referência fornecida (padrão: hoje).
     */
    fun getPlatformCycleIntervals(
        platform: Platform,
        refDate: LocalDate = LocalDate.now()
    ): List<CycleInterval> {
        val rules = platform.rules
        val payDelay = rules.fixedPayDelay.toLong().coerceAtLeast(1L)
        val cycle = platform.cycle.lowercase().trim()

        return when (cycle) {
            "semanal" -> {
                val c0Start = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val c0End = c0Start.plusDays(6)
                val c0Pay = c0End.plusDays(payDelay)

                val c1Start = c0Start.plusDays(7)
                val c1End = c1Start.plusDays(6)
                val c1Pay = c1End.plusDays(payDelay)

                listOf(
                    CycleInterval(c0Start, c0End, c0Pay),
                    CycleInterval(c1Start, c1End, c1Pay)
                )
            }
            "quinzenal" -> {
                val dom = refDate.dayOfMonth
                val c0Start: LocalDate
                val c0End: LocalDate
                val c1Start: LocalDate
                val c1End: LocalDate

                if (dom <= 15) {
                    c0Start = refDate.withDayOfMonth(1)
                    c0End = refDate.withDayOfMonth(15)
                    c1Start = refDate.withDayOfMonth(16)
                    c1End = refDate.with(TemporalAdjusters.lastDayOfMonth())
                } else {
                    c0Start = refDate.withDayOfMonth(16)
                    c0End = refDate.with(TemporalAdjusters.lastDayOfMonth())
                    val nextMonth = refDate.plusMonths(1)
                    c1Start = nextMonth.withDayOfMonth(1)
                    c1End = nextMonth.withDayOfMonth(15)
                }

                val c0Pay = c0End.plusDays(payDelay)
                val c1Pay = c1End.plusDays(payDelay)

                listOf(
                    CycleInterval(c0Start, c0End, c0Pay),
                    CycleInterval(c1Start, c1End, c1Pay)
                )
            }
            "mensal" -> {
                val c0Start = refDate.withDayOfMonth(1)
                val c0End = refDate.with(TemporalAdjusters.lastDayOfMonth())
                val c0Pay = c0End.plusDays(payDelay)

                val nextMonth = refDate.plusMonths(1)
                val c1Start = nextMonth.withDayOfMonth(1)
                val c1End = nextMonth.with(TemporalAdjusters.lastDayOfMonth())
                val c1Pay = c1End.plusDays(payDelay)

                listOf(
                    CycleInterval(c0Start, c0End, c0Pay),
                    CycleInterval(c1Start, c1End, c1Pay)
                )
            }
            "misto", "variavel" -> {
                val entries = if (rules.cycleEntries.isNotEmpty()) {
                    rules.cycleEntries
                } else {
                    listOf(CycleEntry(cut = 1, payDelay = 7), CycleEntry(cut = 16, payDelay = 7))
                }.sortedBy { it.cut }

                data class CutEvent(val date: LocalDate, val payDelay: Long)
                val cutEvents = mutableListOf<CutEvent>()

                for (monthOffset in -1L..3L) {
                    val targetMonth = refDate.plusMonths(monthOffset)
                    val daysInMonth = targetMonth.lengthOfMonth()
                    for (entry in entries) {
                        val cutDay = entry.cut.coerceIn(1, daysInMonth)
                        val date = targetMonth.withDayOfMonth(cutDay)
                        cutEvents.add(CutEvent(date, entry.payDelay.toLong().coerceAtLeast(1L)))
                    }
                }

                cutEvents.sortBy { it.date }

                var currentIdx = -1
                for (i in 0 until cutEvents.size - 1) {
                    val start = cutEvents[i].date
                    val nextCut = cutEvents[i + 1].date
                    val end = nextCut.minusDays(1)

                    if (!refDate.isBefore(start) && !refDate.isAfter(end)) {
                        currentIdx = i
                        break
                    }
                }

                if (currentIdx == -1) {
                    val nextFutureIdx = cutEvents.indexOfFirst { it.date.isAfter(refDate) }
                    currentIdx = if (nextFutureIdx > 0) nextFutureIdx - 1 else 0
                }

                val intervals = mutableListOf<CycleInterval>()
                for (offset in 0..1) {
                    val idx = currentIdx + offset
                    if (idx < cutEvents.size - 1) {
                        val ev = cutEvents[idx]
                        val nextEv = cutEvents[idx + 1]
                        val start = ev.date
                        val end = nextEv.date.minusDays(1)
                        val payDate = end.plusDays(ev.payDelay)
                        intervals.add(CycleInterval(start, end, payDate))
                    }
                }
                intervals
            }
            else -> {
                val c0Start = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val c0End = c0Start.plusDays(6)
                val c0Pay = c0End.plusDays(payDelay)
                listOf(CycleInterval(c0Start, c0End, c0Pay))
            }
        }
    }

    /**
     * Verifica sobreposição temporal inclusiva entre dois intervalos de datas.
     */
    fun checkOverlap(start1: LocalDate, end1: LocalDate, start2: LocalDate, end2: LocalDate): Boolean {
        return !start1.isAfter(end2) && !end1.isBefore(start2)
    }
}
