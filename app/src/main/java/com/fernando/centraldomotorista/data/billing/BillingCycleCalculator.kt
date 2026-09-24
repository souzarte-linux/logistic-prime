package com.fernando.centraldomotorista.data.billing

import com.fernando.centraldomotorista.data.model.CycleEntry
import com.fernando.centraldomotorista.data.model.DailyTotal
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.FinancialAdjustment
import com.fernando.centraldomotorista.data.model.FinancialAdjustmentSubtype
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.model.Route
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class CycleInterval(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val expectedPaymentDate: LocalDate,
    val includeEndDate: Boolean = true
)

data class BillingCycleTotals(
    val grossRoutesAmount: BigDecimal = BigDecimal.ZERO,
    val totalTipsAmount: BigDecimal = BigDecimal.ZERO,
    val totalBonusAmount: BigDecimal = BigDecimal.ZERO,
    val grossDailyAmount: BigDecimal = BigDecimal.ZERO,
    val sessionAmount: BigDecimal = BigDecimal.ZERO,
    val adjustmentsCredit: BigDecimal = BigDecimal.ZERO,
    val adjustmentsDebit: BigDecimal = BigDecimal.ZERO,
    val adjustmentsTotal: BigDecimal = BigDecimal.ZERO,
    val netTotalAmount: BigDecimal = BigDecimal.ZERO,
    val routesCount: Int = 0,
    val packagesCount: Int = 0,
    val dailyTotalsCount: Int = 0,
    val sessionsCount: Int = 0,
    val adjustmentsCount: Int = 0
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
        val payDelay = rules.fixedPayDelay.toLong().coerceAtLeast(0L)
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
                    CycleInterval(c0Start, c0End, c0Pay, true),
                    CycleInterval(c1Start, c1End, c1Pay, true)
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
                    CycleInterval(c0Start, c0End, c0Pay, true),
                    CycleInterval(c1Start, c1End, c1Pay, true)
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
                    CycleInterval(c0Start, c0End, c0Pay, true),
                    CycleInterval(c1Start, c1End, c1Pay, true)
                )
            }
            "misto", "variavel" -> {
                // 1. Suporte a ciclos variáveis com intervalos explícitos de data (início/fim/pagamento)
                val explicitDateEntries = rules.cycleEntries.filter { it.startDate != null && it.endDate != null }
                    .sortedBy { it.startDate }

                if (explicitDateEntries.isNotEmpty()) {
                    val matchingIndex = explicitDateEntries.indexOfFirst { entry ->
                        isDateInCycle(refDate, entry.startDate!!, entry.endDate!!, entry.includeEndDate)
                    }

                    val chosenIndex = if (matchingIndex != -1) {
                        matchingIndex
                    } else {
                        val nextFuture = explicitDateEntries.indexOfFirst { it.startDate!!.isAfter(refDate) }
                        if (nextFuture != -1) nextFuture else explicitDateEntries.lastIndex
                    }

                    val result = mutableListOf<CycleInterval>()
                    for (offset in 0..1) {
                        val idx = chosenIndex + offset
                        if (idx in explicitDateEntries.indices) {
                            val e = explicitDateEntries[idx]
                            val s = e.startDate!!
                            val end = e.endDate!!
                            val pay = e.paymentDate ?: end.plusDays(e.payDelayDays.toLong())
                            result.add(CycleInterval(s, end, pay, e.includeEndDate))
                        }
                    }
                    if (result.isNotEmpty()) return result
                }

                // 2. Fallback para ciclo misto por dias de corte mensais
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
                        cutEvents.add(CutEvent(date, entry.payDelay.toLong().coerceAtLeast(0L)))
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
                        intervals.add(CycleInterval(start, end, payDate, true))
                    }
                }
                intervals
            }
            else -> {
                val c0Start = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val c0End = c0Start.plusDays(6)
                val c0Pay = c0End.plusDays(payDelay)
                listOf(CycleInterval(c0Start, c0End, c0Pay, true))
            }
        }
    }

    /**
     * Verifica sobreposição temporal considerando se a data final é inclusiva ou semi-aberta.
     */
    fun checkOverlap(
        start1: LocalDate,
        end1: LocalDate,
        includeEnd1: Boolean = true,
        start2: LocalDate,
        end2: LocalDate,
        includeEnd2: Boolean = true
    ): Boolean {
        val effEnd1 = if (includeEnd1) end1 else end1.minusDays(1)
        val effEnd2 = if (includeEnd2) end2 else end2.minusDays(1)
        if (effEnd1.isBefore(start1) || effEnd2.isBefore(start2)) return false
        return !start1.isAfter(effEnd2) && !effEnd1.isBefore(start2)
    }

    /**
     * Sobrecarga de compatibilidade para verificação com datas inclusivas.
     */
    fun checkOverlap(start1: LocalDate, end1: LocalDate, start2: LocalDate, end2: LocalDate): Boolean {
        return checkOverlap(start1, end1, true, start2, end2, true)
    }

    /**
     * Verifica se uma data específica pertence a um ciclo de faturamento.
     */
    fun isDateInCycle(
        date: LocalDate,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        includeEndDate: Boolean = true
    ): Boolean {
        val effEnd = if (includeEndDate) periodEnd else periodEnd.minusDays(1)
        return !date.isBefore(periodStart) && !date.isAfter(effEnd)
    }

    /**
     * Calcula o valor da rota baseado na quantidade de pacotes e valor unitário com precisão BigDecimal.
     */
    fun calculateRouteAmount(packageCount: Int, unitPrice: BigDecimal): BigDecimal {
        if (packageCount <= 0 || unitPrice <= BigDecimal.ZERO) return BigDecimal.ZERO
        return BigDecimal(packageCount).multiply(unitPrice)
    }

    /**
     * Recalcula os totais de uma fatura / ciclo com precisão monetária (BigDecimal)
     * considerando rotas, pacotes, gorjetas, bônus, diárias e os 9 subtipos de ajustes financeiros.
     */
    fun calculateCycleTotals(
        routes: List<Route>,
        dailyTotals: List<DailyTotal>,
        adjustments: List<FinancialAdjustment>,
        sessions: List<DeliveryPartnerSession> = emptyList()
    ): BillingCycleTotals {
        val grossRoutes = routes.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
        val tips = routes.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.tip) }
        val bonus = routes.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.bonus) }
        val routePackages = routes.sumOf { it.packageCount }

        val grossDaily = dailyTotals.fold(BigDecimal.ZERO) { acc, dt -> acc.add(dt.amount) }

        val grossSessions = sessions.fold(BigDecimal.ZERO) { acc, s ->
            val sAmt = if (s.amountPaid > BigDecimal.ZERO) {
                s.amountPaid
            } else {
                BigDecimal(s.deliveredCount).multiply(s.packageRate).add(s.defaultBonus)
            }
            acc.add(sAmt)
        }
        val sessionPackages = sessions.sumOf { s ->
            if (s.deliveredCount > 0) s.deliveredCount else (if (s.scannedCount > 0) s.scannedCount else s.expectedPackageCount)
        }

        val adjustmentsCredit = adjustments.filter { adj ->
            val sub = FinancialAdjustmentSubtype.fromKey(adj.subtype)
            if (sub != null) {
                sub.isCredit
            } else {
                adj.type.lowercase().trim() in listOf("credito", "bonus", "acrescimo")
            }
        }.fold(BigDecimal.ZERO) { acc, adj -> acc.add(adj.amount) }

        val adjustmentsDebit = adjustments.filter { adj ->
            val sub = FinancialAdjustmentSubtype.fromKey(adj.subtype)
            if (sub != null) {
                !sub.isCredit
            } else {
                adj.type.lowercase().trim() in listOf("debito", "desconto", "avaria", "extravio")
            }
        }.fold(BigDecimal.ZERO) { acc, adj -> acc.add(adj.amount) }

        val adjustmentsTotal = adjustmentsCredit.subtract(adjustmentsDebit)

        val netTotal = grossRoutes
            .add(tips)
            .add(bonus)
            .add(grossDaily)
            .add(grossSessions)
            .add(adjustmentsTotal)

        return BillingCycleTotals(
            grossRoutesAmount = grossRoutes,
            totalTipsAmount = tips,
            totalBonusAmount = bonus,
            grossDailyAmount = grossDaily,
            sessionAmount = grossSessions,
            adjustmentsCredit = adjustmentsCredit,
            adjustmentsDebit = adjustmentsDebit,
            adjustmentsTotal = adjustmentsTotal,
            netTotalAmount = netTotal,
            routesCount = routes.size,
            packagesCount = routePackages + sessionPackages,
            dailyTotalsCount = dailyTotals.size,
            sessionsCount = sessions.size,
            adjustmentsCount = adjustments.size
        )
    }
}
