package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.ui.screens.deliverypartners.PlatformChartPoint
import com.fernando.centraldomotorista.ui.screens.deliverypartners.PlatformFinanceFilter
import com.fernando.centraldomotorista.ui.screens.deliverypartners.PlatformFinanceHelper
import com.fernando.centraldomotorista.ui.screens.deliverypartners.PlatformFinancePeriodPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PlatformFinanceCalculationTest {

    private val platformA = "plat-shopee"
    private val platformB = "plat-mercadolivre"

    @Test
    fun testCustomIntervalMultiMonthBreakdownWithSubtotals() {
        val today = LocalDate.of(2026, 9, 23)
        val startDate = LocalDate.of(2026, 8, 1)
        val endDate = LocalDate.of(2026, 9, 23)

        val filter = PlatformFinanceFilter(
            preset = PlatformFinancePeriodPreset.PERSONALIZADO,
            customStart = startDate,
            customEnd = endDate
        )

        val s1 = DeliveryPartnerSession(
            id = "sess-1",
            partnerId = "partner-1",
            platformId = platformA,
            deliveredCount = 40,
            returnedCount = 2,
            amountPaid = BigDecimal("200.00"),
            startTime = OffsetDateTime.of(2026, 8, 5, 9, 0, 0, 0, ZoneOffset.UTC)
        )
        val s2 = DeliveryPartnerSession(
            id = "sess-2",
            partnerId = "partner-1",
            platformId = platformA,
            deliveredCount = 50,
            returnedCount = 3,
            amountPaid = BigDecimal("250.00"),
            startTime = OffsetDateTime.of(2026, 8, 18, 9, 0, 0, 0, ZoneOffset.UTC)
        )
        val s3 = DeliveryPartnerSession(
            id = "sess-3",
            partnerId = "partner-1",
            platformId = platformA,
            deliveredCount = 60,
            returnedCount = 1,
            amountPaid = BigDecimal("300.00"),
            startTime = OffsetDateTime.of(2026, 9, 10, 9, 0, 0, 0, ZoneOffset.UTC)
        )
        val s4 = DeliveryPartnerSession(
            id = "sess-4",
            partnerId = "partner-1",
            platformId = platformA,
            deliveredCount = 35,
            returnedCount = 0,
            amountPaid = BigDecimal("175.00"),
            startTime = OffsetDateTime.of(2026, 9, 22, 9, 0, 0, 0, ZoneOffset.UTC)
        )
        // Sessão de outra plataforma (deve ser ignorada no card da platformA)
        val sOther = DeliveryPartnerSession(
            id = "sess-other",
            partnerId = "partner-1",
            platformId = platformB,
            deliveredCount = 100,
            amountPaid = BigDecimal("500.00"),
            startTime = OffsetDateTime.of(2026, 8, 10, 9, 0, 0, 0, ZoneOffset.UTC)
        )

        val breakdown = PlatformFinanceHelper.calculatePlatformBreakdown(
            platformId = platformA,
            sessions = listOf(s1, s2, s3, s4, sOther),
            filter = filter,
            today = today,
            zone = ZoneOffset.UTC
        )

        assertTrue("Deve ser identificado como multi-intervalo", breakdown.isMultiInterval)
        assertEquals(2, breakdown.months.size)

        // Mês 1: Agosto de 2026
        val month1 = breakdown.months[0]
        assertEquals(2026, month1.yearMonth.year)
        assertEquals(8, month1.yearMonth.monthValue)
        assertEquals(2, month1.sessionCount)
        assertEquals(90, month1.subtotalDelivered) // 40 + 50
        assertEquals(5, month1.subtotalReturned)   // 2 + 3
        assertEquals(BigDecimal("450.00"), month1.subtotalAmountPaid) // 200 + 250

        // Mês 2: Setembro de 2026
        val month2 = breakdown.months[1]
        assertEquals(2026, month2.yearMonth.year)
        assertEquals(9, month2.yearMonth.monthValue)
        assertEquals(2, month2.sessionCount)
        assertEquals(95, month2.subtotalDelivered) // 60 + 35
        assertEquals(1, month2.subtotalReturned)   // 1 + 0
        assertEquals(BigDecimal("475.00"), month2.subtotalAmountPaid) // 300 + 175

        // Linha de Total Geral do Período
        assertEquals(185, breakdown.grandTotalDelivered) // 90 + 95
        assertEquals(6, breakdown.grandTotalReturned)    // 5 + 1
        assertEquals(4, breakdown.totalSessions)
        assertEquals(BigDecimal("925.00"), breakdown.grandTotalAmount) // 450 + 475
    }

    @Test
    fun testPlatformFilterIsolation() {
        val today = LocalDate.of(2026, 9, 23)
        val filter = PlatformFinanceFilter(preset = PlatformFinancePeriodPreset.MES_CORRENTE)

        val sA = DeliveryPartnerSession(
            id = "sess-a",
            partnerId = "partner-1",
            platformId = platformA,
            deliveredCount = 30,
            amountPaid = BigDecimal("150.00"),
            startTime = OffsetDateTime.of(2026, 9, 5, 9, 0, 0, 0, ZoneOffset.UTC)
        )
        val sB = DeliveryPartnerSession(
            id = "sess-b",
            partnerId = "partner-1",
            platformId = platformB,
            deliveredCount = 80,
            amountPaid = BigDecimal("400.00"),
            startTime = OffsetDateTime.of(2026, 9, 5, 9, 0, 0, 0, ZoneOffset.UTC)
        )

        val breakdownA = PlatformFinanceHelper.calculatePlatformBreakdown(
            platformId = platformA,
            sessions = listOf(sA, sB),
            filter = filter,
            today = today,
            zone = ZoneOffset.UTC
        )

        assertEquals(1, breakdownA.totalSessions)
        assertEquals(30, breakdownA.grandTotalDelivered)
        assertEquals(BigDecimal("150.00"), breakdownA.grandTotalAmount)
    }

    @Test
    fun testEarningsLineChartDataGeneration() {
        val today = LocalDate.of(2026, 9, 10)
        val filter = PlatformFinanceFilter(
            preset = PlatformFinancePeriodPreset.PERSONALIZADO,
            customStart = LocalDate.of(2026, 9, 8),
            customEnd = LocalDate.of(2026, 9, 10)
        )

        val s1 = DeliveryPartnerSession(
            id = "sess-1",
            partnerId = "partner-1",
            platformId = platformA,
            deliveredCount = 20,
            amountPaid = BigDecimal("100.00"),
            startTime = OffsetDateTime.of(2026, 9, 8, 10, 0, 0, 0, ZoneOffset.UTC)
        )
        val s2 = DeliveryPartnerSession(
            id = "sess-2",
            partnerId = "partner-1",
            platformId = platformA,
            deliveredCount = 35,
            amountPaid = BigDecimal("175.00"),
            startTime = OffsetDateTime.of(2026, 9, 10, 10, 0, 0, 0, ZoneOffset.UTC)
        )

        val points = PlatformFinanceHelper.generateEarningsLineChartData(
            platformIdFilter = platformA,
            activePlatformIds = setOf(platformA),
            sessions = listOf(s1, s2),
            filter = filter,
            today = today,
            zone = ZoneOffset.UTC
        )

        assertEquals(3, points.size) // dias 08, 09, 10
        assertEquals(BigDecimal("100.00"), points[0].amount)
        assertEquals(20, points[0].deliveredCount)

        assertEquals(BigDecimal.ZERO, points[1].amount) // dia 09 sem sessão
        assertEquals(0, points[1].deliveredCount)

        assertEquals(BigDecimal("175.00"), points[2].amount)
        assertEquals(35, points[2].deliveredCount)
    }
}
