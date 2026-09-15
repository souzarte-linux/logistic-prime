package com.fernando.centraldomotorista.ui.screens.deliverypartners

import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.ui.common.charts.DailyTrendBucket
import com.fernando.centraldomotorista.ui.common.charts.TrendRange
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class TimedRouteDetail(
    val sessionId: String,
    val date: LocalDate,
    val routeId: String?,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val durationMinutes: Long,
    val deliveredCount: Int,
    val returnedCount: Int
) {
    val formattedDuration: String
        get() = formatMinutesToHoursAndMinutes(durationMinutes)

    val minutesPerPackage: BigDecimal?
        get() = if (deliveredCount > 0) {
            BigDecimal(durationMinutes).divide(BigDecimal(deliveredCount), 1, RoundingMode.HALF_UP)
        } else null
}

data class PartnerDurationMetrics(
    val totalTimedRoutes: Int = 0,
    val totalDurationMinutes: Long = 0L,
    val averageDurationMinutes: Long = 0L,
    val shortestDurationMinutes: Long? = null,
    val longestDurationMinutes: Long? = null,
    val averageMinutesPerPackage: BigDecimal? = null,
    val timedRoutes: List<TimedRouteDetail> = emptyList()
) {
    val formattedAverage: String
        get() = formatMinutesToHoursAndMinutes(averageDurationMinutes)

    val formattedTotal: String
        get() = formatMinutesToHoursAndMinutes(totalDurationMinutes)

    val formattedShortest: String
        get() = shortestDurationMinutes?.let { formatMinutesToHoursAndMinutes(it) } ?: "--"

    val formattedLongest: String
        get() = longestDurationMinutes?.let { formatMinutesToHoursAndMinutes(it) } ?: "--"
}

fun formatMinutesToHoursAndMinutes(minutes: Long): String {
    if (minutes <= 0) return "0 min"
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}min"
        hours > 0 -> "${hours}h"
        else -> "${remainingMinutes} min"
    }
}

fun calculatePartnerDurationMetrics(
    sessions: List<DeliveryPartnerSession>,
    zone: ZoneId = ZoneId.systemDefault()
): PartnerDurationMetrics {
    val timedSessions = sessions.filter {
        it.startTime != null && it.endTime != null && it.endTime.isAfter(it.startTime)
    }

    if (timedSessions.isEmpty()) {
        return PartnerDurationMetrics()
    }

    var totalMinutes = 0L
    var shortest = Long.MAX_VALUE
    var longest = Long.MIN_VALUE
    var totalDeliveredInTimed = 0

    val timedRoutes = mutableListOf<TimedRouteDetail>()

    for (session in timedSessions) {
        val start = session.startTime!!
        val end = session.endTime!!
        val duration = Duration.between(start, end).toMinutes()
        totalMinutes += duration
        if (duration < shortest) shortest = duration
        if (duration > longest) longest = duration
        totalDeliveredInTimed += session.deliveredCount

        val sessionDate = start.atZoneSameInstant(zone).toLocalDate()
        timedRoutes.add(
            TimedRouteDetail(
                sessionId = session.id,
                date = sessionDate,
                routeId = session.routeId,
                startTime = start,
                endTime = end,
                durationMinutes = duration,
                deliveredCount = session.deliveredCount,
                returnedCount = session.returnedCount
            )
        )
    }

    val avgMinutes = totalMinutes / timedSessions.size
    val avgMinutesPerPackage = if (totalDeliveredInTimed > 0) {
        BigDecimal(totalMinutes).divide(BigDecimal(totalDeliveredInTimed), 1, RoundingMode.HALF_UP)
    } else null

    return PartnerDurationMetrics(
        totalTimedRoutes = timedSessions.size,
        totalDurationMinutes = totalMinutes,
        averageDurationMinutes = avgMinutes,
        shortestDurationMinutes = if (shortest != Long.MAX_VALUE) shortest else null,
        longestDurationMinutes = if (longest != Long.MIN_VALUE) longest else null,
        averageMinutesPerPackage = avgMinutesPerPackage,
        timedRoutes = timedRoutes.sortedByDescending { it.startTime }
    )
}

data class PartnerPerformanceMetrics(
    val totalEarnings: BigDecimal = BigDecimal.ZERO,
    val currentMonthEarnings: BigDecimal = BigDecimal.ZERO,
    val totalDelivered: Int = 0,
    val totalReturned: Int = 0,
    val returnRate: BigDecimal = BigDecimal.ZERO, // em % ex: 3.50
    val averageEarningsPerSession: BigDecimal = BigDecimal.ZERO,
    val bestDay: BestDayPerformance? = null,
    val totalSessions: Int = 0,
    val durationMetrics: PartnerDurationMetrics = PartnerDurationMetrics()
)

data class BestDayPerformance(
    val date: LocalDate,
    val totalEarnings: BigDecimal,
    val deliveredCount: Int,
    val returnedCount: Int,
    val sessionCount: Int
)

fun calculatePartnerPerformance(
    sessions: List<DeliveryPartnerSession>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): PartnerPerformanceMetrics {
    if (sessions.isEmpty()) {
        return PartnerPerformanceMetrics()
    }

    var totalEarnings = BigDecimal.ZERO
    var currentMonthEarnings = BigDecimal.ZERO
    var totalDelivered = 0
    var totalReturned = 0
    val totalSessions = sessions.size

    val currentYear = today.year
    val currentMonth = today.monthValue

    data class DayAccumulator(
        var totalAmount: BigDecimal = BigDecimal.ZERO,
        var delivered: Int = 0,
        var returned: Int = 0,
        var count: Int = 0
    )

    val dailyMap = mutableMapOf<LocalDate, DayAccumulator>()

    for (session in sessions) {
        val amount = session.amountPaid
        totalEarnings = totalEarnings.add(amount)
        totalDelivered += session.deliveredCount
        totalReturned += session.returnedCount

        val sessionDateTime = session.startTime ?: session.createdAt
        val sessionDate = sessionDateTime?.atZoneSameInstant(zone)?.toLocalDate() ?: today

        if (sessionDate.year == currentYear && sessionDate.monthValue == currentMonth) {
            currentMonthEarnings = currentMonthEarnings.add(amount)
        }

        val dayAcc = dailyMap.getOrPut(sessionDate) { DayAccumulator() }
        dayAcc.totalAmount = dayAcc.totalAmount.add(amount)
        dayAcc.delivered += session.deliveredCount
        dayAcc.returned += session.returnedCount
        dayAcc.count += 1
    }

    val totalPackages = totalDelivered + totalReturned
    val returnRate = if (totalPackages > 0) {
        BigDecimal(totalReturned)
            .multiply(BigDecimal(100))
            .divide(BigDecimal(totalPackages), 2, RoundingMode.HALF_UP)
    } else {
        BigDecimal.ZERO
    }

    val averageEarningsPerSession = if (totalSessions > 0) {
        totalEarnings.divide(BigDecimal(totalSessions), 2, RoundingMode.HALF_UP)
    } else {
        BigDecimal.ZERO
    }

    val bestDayEntry = dailyMap.maxWithOrNull(
        compareBy<Map.Entry<LocalDate, DayAccumulator>> { it.value.totalAmount }
            .thenBy { it.value.delivered }
    )

    val bestDay = bestDayEntry?.let { (date, acc) ->
        BestDayPerformance(
            date = date,
            totalEarnings = acc.totalAmount,
            deliveredCount = acc.delivered,
            returnedCount = acc.returned,
            sessionCount = acc.count
        )
    }

    val durationMetrics = calculatePartnerDurationMetrics(sessions, zone)

    return PartnerPerformanceMetrics(
        totalEarnings = totalEarnings,
        currentMonthEarnings = currentMonthEarnings,
        totalDelivered = totalDelivered,
        totalReturned = totalReturned,
        returnRate = returnRate,
        averageEarningsPerSession = averageEarningsPerSession,
        bestDay = bestDay,
        totalSessions = totalSessions,
        durationMetrics = durationMetrics
    )
}

data class PartnerCostEfficiency(
    val costPerPackageThisMonth: BigDecimal = BigDecimal.ZERO,   // currentMonthEarnings / pacotes entregues no mês
    val othersAverageCostPerPackage: BigDecimal? = null,          // null = sem outros parceiros ativos para comparar
    val percentageVsOthers: BigDecimal? = null                    // positivo = custa mais que a média; negativo = custa menos
)

data class PartnerRegularity(
    val daysWorkedThisMonth: Int,
    val daysElapsedThisMonth: Int,   // dias já passados no mês corrente (não o total do mês)
    val regularityPercent: BigDecimal  // daysWorkedThisMonth / daysElapsedThisMonth × 100, protegido contra /0
)

fun calculateCostPerPackage(
    currentMonthEarnings: BigDecimal,
    currentMonthDelivered: Int
): BigDecimal =
    if (currentMonthDelivered > 0) {
        currentMonthEarnings.divide(BigDecimal(currentMonthDelivered), 2, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO

fun calculateOthersAverageCostPerPackage(
    allPartnersSessions: List<DeliveryPartnerSession>, // TODOS os parceiros, mês corrente já filtrado pelo chamador
    excludingPartnerId: String
): BigDecimal? {
    val byPartner = allPartnersSessions
        .filter { it.partnerId != excludingPartnerId }
        .groupBy { it.partnerId }
        .mapValues { (_, sessions) ->
            val earnings = sessions.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.amountPaid) }
            val delivered = sessions.sumOf { it.deliveredCount }
            if (delivered > 0) earnings.divide(BigDecimal(delivered), 2, RoundingMode.HALF_UP) else null
        }
        .values
        .filterNotNull()

    if (byPartner.isEmpty()) return null
    val sum = byPartner.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
    return sum.divide(BigDecimal(byPartner.size), 2, RoundingMode.HALF_UP)
}

fun calculateRegularity(
    sessions: List<DeliveryPartnerSession>, // deste parceiro, sem filtro de período (mesma allSessions da Fase 2)
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): PartnerRegularity {
    val distinctDaysThisMonth = sessions
        .mapNotNull { (it.startTime ?: it.createdAt)?.atZoneSameInstant(zone)?.toLocalDate() }
        .filter { it.year == today.year && it.monthValue == today.monthValue }
        .distinct()
        .size

    val daysElapsed = today.dayOfMonth
    val pct = if (daysElapsed > 0) {
        BigDecimal(distinctDaysThisMonth).multiply(BigDecimal(100))
            .divide(BigDecimal(daysElapsed), 1, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO

    return PartnerRegularity(distinctDaysThisMonth, daysElapsed, pct)
}

fun buildPartnerTrendBuckets(
    sessions: List<DeliveryPartnerSession>, // deste parceiro
    range: TrendRange,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
): Pair<List<DailyTrendBucket>, BigDecimal> {
    val daysCount = if (range == TrendRange.SEVEN_DAYS) 7 else 30
    val startDate = if (range == TrendRange.SEVEN_DAYS) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    } else {
        today.minusDays((daysCount - 1).toLong())
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR"))
    val buckets = mutableListOf<DailyTrendBucket>()

    for (i in 0 until daysCount) {
        val date = startDate.plusDays(i.toLong())
        val dayOfWeekShort = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
            .replace(".", "")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        val dayOfWeekFull = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }

        val daySessions = sessions.filter { session ->
            val sessionDate = (session.startTime ?: session.createdAt)?.atZoneSameInstant(zone)?.toLocalDate()
            sessionDate == date
        }
        val dayAmount = daySessions.fold(BigDecimal.ZERO) { acc, s -> acc.add(s.amountPaid) }
        val dayPackages = daySessions.sumOf { it.deliveredCount }

        buckets.add(
            DailyTrendBucket(
                date = date,
                dayOfWeekLabel = dayOfWeekShort,
                fullDayOfWeekLabel = dayOfWeekFull,
                dateLabel = date.format(dateFormatter),
                totalAmount = dayAmount,
                packageCount = dayPackages,
                isToday = (date == today)
            )
        )
    }

    val maxAmount = buckets.maxOfOrNull { it.totalAmount } ?: BigDecimal.ONE
    val safeMax = if (maxAmount > BigDecimal.ZERO) maxAmount else BigDecimal.ONE
    return Pair(buckets, safeMax)
}

