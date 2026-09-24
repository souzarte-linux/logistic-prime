package com.fernando.centraldomotorista.ui.screens.deliverypartners

import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Presets de período para o seletor em alto relevo de cada plataforma.
 */
enum class PlatformFinancePeriodPreset(val label: String) {
    MES_CORRENTE("Mês Atual"),
    ESTA_SEMANA("Esta Semana"),
    SEMANA_PASSADA("Semana Passada"),
    ESTA_QUINZENA("Esta Quinzena"),
    MES_PASSADO("Mês Passado"),
    ULTIMOS_3_MESES("Últimos 3 Meses"),
    PERSONALIZADO("Personalizado...")
}

/**
 * Filtro de período selecionado para o card de uma plataforma.
 */
data class PlatformFinanceFilter(
    val preset: PlatformFinancePeriodPreset = PlatformFinancePeriodPreset.MES_CORRENTE,
    val customStart: LocalDate = LocalDate.now().withDayOfMonth(1),
    val customEnd: LocalDate = LocalDate.now(),
    val specificYearMonth: YearMonth? = null
) {
    fun resolveRange(today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> {
        return when (preset) {
            PlatformFinancePeriodPreset.MES_CORRENTE -> {
                val start = today.withDayOfMonth(1)
                val end = today.with(TemporalAdjusters.lastDayOfMonth())
                start to end
            }
            PlatformFinancePeriodPreset.ESTA_SEMANA -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = start.plusDays(6)
                start to end
            }
            PlatformFinancePeriodPreset.SEMANA_PASSADA -> {
                val lastMon = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val lastSun = lastMon.plusDays(6)
                lastMon to lastSun
            }
            PlatformFinancePeriodPreset.ESTA_QUINZENA -> {
                if (today.dayOfMonth <= 15) {
                    today.withDayOfMonth(1) to today.withDayOfMonth(15)
                } else {
                    today.withDayOfMonth(16) to today.with(TemporalAdjusters.lastDayOfMonth())
                }
            }
            PlatformFinancePeriodPreset.MES_PASSADO -> {
                val prev = today.minusMonths(1)
                prev.withDayOfMonth(1) to prev.with(TemporalAdjusters.lastDayOfMonth())
            }
            PlatformFinancePeriodPreset.ULTIMOS_3_MESES -> {
                val start = today.minusMonths(2).withDayOfMonth(1)
                val end = today.with(TemporalAdjusters.lastDayOfMonth())
                start to end
            }
            PlatformFinancePeriodPreset.PERSONALIZADO -> {
                if (specificYearMonth != null) {
                    val s = specificYearMonth.atDay(1)
                    val e = specificYearMonth.atEndOfMonth()
                    s to e
                } else {
                    val s = if (customStart.isAfter(customEnd)) customEnd else customStart
                    val e = if (customStart.isAfter(customEnd)) customStart else customEnd
                    s to e
                }
            }
        }
    }

    fun getDisplayLabel(today: LocalDate = LocalDate.now()): String {
        val (s, e) = resolveRange(today)
        val ddMM = DateTimeFormatter.ofPattern("dd/MM")
        val monthShortFmt = DateTimeFormatter.ofPattern("MMM/yyyy", Locale("pt", "BR"))
        return when (preset) {
            PlatformFinancePeriodPreset.MES_CORRENTE -> "Mês Atual"
            PlatformFinancePeriodPreset.ESTA_SEMANA -> "Esta Semana"
            PlatformFinancePeriodPreset.SEMANA_PASSADA -> "Sem. Passada"
            PlatformFinancePeriodPreset.ESTA_QUINZENA -> "Esta Quinzena"
            PlatformFinancePeriodPreset.MES_PASSADO -> "Mês Passado"
            PlatformFinancePeriodPreset.ULTIMOS_3_MESES -> "Últimos 3M"
            PlatformFinancePeriodPreset.PERSONALIZADO -> {
                if (specificYearMonth != null) {
                    specificYearMonth.format(monthShortFmt).replace(".", "").replaceFirstChar { it.uppercase() }
                } else {
                    "${s.format(ddMM)} a ${e.format(ddMM)}"
                }
            }
        }
    }

    val isSingleInterval: Boolean
        get() = preset == PlatformFinancePeriodPreset.ESTA_SEMANA ||
                preset == PlatformFinancePeriodPreset.SEMANA_PASSADA ||
                preset == PlatformFinancePeriodPreset.ESTA_QUINZENA ||
                (preset == PlatformFinancePeriodPreset.MES_CORRENTE && specificYearMonth == null) ||
                (preset == PlatformFinancePeriodPreset.MES_PASSADO && specificYearMonth == null)
}

/**
 * Subdivisão de semana dentro de um mês.
 */
data class PlatformFinanceWeekSubdivision(
    val weekIndex: Int,
    val label: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val sessions: List<DeliveryPartnerSession>,
    val deliveredCount: Int,
    val returnedCount: Int,
    val amountPaid: BigDecimal
)

/**
 * Subdivisão de mês dentro de um intervalo selecionado.
 */
data class PlatformFinanceMonthSubdivision(
    val yearMonth: YearMonth,
    val monthLabel: String,
    val weeks: List<PlatformFinanceWeekSubdivision>,
    val subtotalAmountPaid: BigDecimal,
    val subtotalDelivered: Int,
    val subtotalReturned: Int,
    val sessionCount: Int
)

/**
 * Detalhamento consolidado de um card de plataforma.
 */
data class PlatformFinanceBreakdown(
    val platformId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val months: List<PlatformFinanceMonthSubdivision>,
    val grandTotalAmount: BigDecimal,
    val grandTotalDelivered: Int,
    val grandTotalReturned: Int,
    val totalSessions: Int,
    val isMultiInterval: Boolean
)

/**
 * Ponto para o gráfico de linhas.
 */
data class PlatformChartPoint(
    val date: LocalDate,
    val label: String,
    val amount: BigDecimal,
    val deliveredCount: Int,
    val sessionCount: Int
)

/**
 * Utilitário de cálculo hierárquico e geração de séries para Finanças da Plataforma.
 */
object PlatformFinanceHelper {

    private val ptLocale = Locale("pt", "BR")
    private val ddMM = DateTimeFormatter.ofPattern("dd/MM", ptLocale)
    private val monthFull = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ptLocale)

    /**
     * Calcula o detalhamento hierárquico das sessões de uma plataforma no período selecionado.
     * Agrupa por mês e por semana dentro de cada mês em ordem cronológica (ex: Agosto depois Setembro),
     * computando subtotais por mês e o total geral do período.
     */
    fun calculatePlatformBreakdown(
        platformId: String,
        sessions: List<DeliveryPartnerSession>,
        filter: PlatformFinanceFilter,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): PlatformFinanceBreakdown {
        val (rangeStart, rangeEnd) = filter.resolveRange(today)

        // 1. Filtra as sessões pertencentes a esta plataforma dentro do intervalo de datas
        val inRangeSessions = sessions.filter { s ->
            if (s.platformId != platformId) return@filter false
            val sessionDate = (s.startTime ?: s.createdAt)?.atZoneSameInstant(zone)?.toLocalDate() ?: return@filter false
            !sessionDate.isBefore(rangeStart) && !sessionDate.isAfter(rangeEnd)
        }.sortedWith(
            compareBy<DeliveryPartnerSession> { (it.startTime ?: it.createdAt) }
                .thenBy { it.id }
        )

        // 2. Identifica todos os meses compreendidos entre rangeStart e rangeEnd
        var curYm = YearMonth.from(rangeStart)
        val endYm = YearMonth.from(rangeEnd)
        val monthList = mutableListOf<YearMonth>()
        while (!curYm.isAfter(endYm)) {
            monthList.add(curYm)
            curYm = curYm.plusMonths(1)
        }

        val monthSubdivisions = mutableListOf<PlatformFinanceMonthSubdivision>()

        var grandTotalAmt = BigDecimal.ZERO
        var grandTotalDel = 0
        var grandTotalRet = 0
        var grandTotalSess = 0

        for (ym in monthList) {
            val monthStart = ym.atDay(1)
            val monthEnd = ym.atEndOfMonth()

            // Intersecção do mês com o range do filtro
            val effectiveMonthStart = if (rangeStart.isAfter(monthStart)) rangeStart else monthStart
            val effectiveMonthEnd = if (rangeEnd.isBefore(monthEnd)) rangeEnd else monthEnd

            val monthSessions = inRangeSessions.filter { s ->
                val sDate = (s.startTime ?: s.createdAt)?.atZoneSameInstant(zone)?.toLocalDate() ?: return@filter false
                sDate.year == ym.year && sDate.monthValue == ym.monthValue
            }

            // Subdivisão por semanas dentro do mês
            // Cada semana vai de segunda a domingo (ou inicia no effectiveMonthStart e encerra no effectiveMonthEnd)
            val weeks = mutableListOf<PlatformFinanceWeekSubdivision>()
            var weekCur = effectiveMonthStart
            var weekIdx = 1

            while (!weekCur.isAfter(effectiveMonthEnd)) {
                // Fim da semana: próximo domingo ou o fim do mês/filtro
                val sun = weekCur.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val weekEnd = if (sun.isAfter(effectiveMonthEnd)) effectiveMonthEnd else sun

                val weekSessions = monthSessions.filter { s ->
                    val sDate = (s.startTime ?: s.createdAt)?.atZoneSameInstant(zone)?.toLocalDate() ?: return@filter false
                    !sDate.isBefore(weekCur) && !sDate.isAfter(weekEnd)
                }

                val wDelivered = weekSessions.sumOf { it.deliveredCount }
                val wReturned = weekSessions.sumOf { it.returnedCount }
                val wAmount = weekSessions.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.amountPaid) }

                val weekLabel = "Semana $weekIdx (${weekCur.format(ddMM)} a ${weekEnd.format(ddMM)})"

                weeks.add(
                    PlatformFinanceWeekSubdivision(
                        weekIndex = weekIdx,
                        label = weekLabel,
                        startDate = weekCur,
                        endDate = weekEnd,
                        sessions = weekSessions,
                        deliveredCount = wDelivered,
                        returnedCount = wReturned,
                        amountPaid = wAmount
                    )
                )

                weekCur = weekEnd.plusDays(1)
                weekIdx++
            }

            val mAmount = monthSessions.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.amountPaid) }
            val mDelivered = monthSessions.sumOf { it.deliveredCount }
            val mReturned = monthSessions.sumOf { it.returnedCount }
            val mLabel = ym.format(monthFull).replaceFirstChar { it.uppercase() }

            monthSubdivisions.add(
                PlatformFinanceMonthSubdivision(
                    yearMonth = ym,
                    monthLabel = mLabel,
                    weeks = weeks,
                    subtotalAmountPaid = mAmount,
                    subtotalDelivered = mDelivered,
                    subtotalReturned = mReturned,
                    sessionCount = monthSessions.size
                )
            )

            grandTotalAmt = grandTotalAmt.add(mAmount)
            grandTotalDel += mDelivered
            grandTotalRet += mReturned
            grandTotalSess += monthSessions.size
        }

        val isMulti = monthList.size > 1 || !filter.isSingleInterval

        return PlatformFinanceBreakdown(
            platformId = platformId,
            startDate = rangeStart,
            endDate = rangeEnd,
            months = monthSubdivisions,
            grandTotalAmount = grandTotalAmt.setScale(2, RoundingMode.HALF_UP),
            grandTotalDelivered = grandTotalDel,
            grandTotalReturned = grandTotalRet,
            totalSessions = grandTotalSess,
            isMultiInterval = isMulti
        )
    }

    /**
     * Gera os pontos diários do gráfico de linhas para a plataforma (ou todas) no período.
     */
    fun generateEarningsLineChartData(
        platformIdFilter: String?, // null para todas as plataformas ativas
        activePlatformIds: Set<String>,
        sessions: List<DeliveryPartnerSession>,
        filter: PlatformFinanceFilter,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PlatformChartPoint> {
        val (rangeStart, rangeEnd) = filter.resolveRange(today)

        val relevantSessions = sessions.filter { s ->
            val pId = s.platformId
            if (platformIdFilter != null) {
                if (pId != platformIdFilter) return@filter false
            } else {
                if (pId == null || !activePlatformIds.contains(pId)) return@filter false
            }
            val sDate = (s.startTime ?: s.createdAt)?.atZoneSameInstant(zone)?.toLocalDate() ?: return@filter false
            !sDate.isBefore(rangeStart) && !sDate.isAfter(rangeEnd)
        }

        val sessionsByDate = relevantSessions.groupBy { s ->
            (s.startTime ?: s.createdAt)?.atZoneSameInstant(zone)?.toLocalDate() ?: today
        }

        val points = mutableListOf<PlatformChartPoint>()
        var cur = rangeStart
        while (!cur.isAfter(rangeEnd)) {
            val daySessions = sessionsByDate[cur].orEmpty()
            val dayAmt = daySessions.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.amountPaid) }
            val dayDel = daySessions.sumOf { it.deliveredCount }
            points.add(
                PlatformChartPoint(
                    date = cur,
                    label = cur.format(ddMM),
                    amount = dayAmt,
                    deliveredCount = dayDel,
                    sessionCount = daySessions.size
                )
            )
            cur = cur.plusDays(1)
        }

        return points
    }
}
