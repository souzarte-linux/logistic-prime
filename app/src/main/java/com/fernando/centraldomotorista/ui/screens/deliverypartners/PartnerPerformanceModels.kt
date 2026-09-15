package com.fernando.centraldomotorista.ui.screens.deliverypartners

import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId

data class PartnerPerformanceMetrics(
    val totalEarnings: BigDecimal = BigDecimal.ZERO,
    val currentMonthEarnings: BigDecimal = BigDecimal.ZERO,
    val totalDelivered: Int = 0,
    val totalReturned: Int = 0,
    val returnRate: BigDecimal = BigDecimal.ZERO, // em % ex: 3.50
    val averageEarningsPerSession: BigDecimal = BigDecimal.ZERO,
    val bestDay: BestDayPerformance? = null,
    val totalSessions: Int = 0
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

    return PartnerPerformanceMetrics(
        totalEarnings = totalEarnings,
        currentMonthEarnings = currentMonthEarnings,
        totalDelivered = totalDelivered,
        totalReturned = totalReturned,
        returnRate = returnRate,
        averageEarningsPerSession = averageEarningsPerSession,
        bestDay = bestDay,
        totalSessions = totalSessions
    )
}
