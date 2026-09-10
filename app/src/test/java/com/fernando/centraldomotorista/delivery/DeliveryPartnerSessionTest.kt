package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.remote.dto.DeliveryPartnerSessionDto
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DeliveryPartnerSessionTest {

    @Test
    fun testDeliveryPartnerSessionDtoMapping() {
        val localOffset = java.time.ZoneId.systemDefault().rules.getOffset(java.time.Instant.now())
        val now = OffsetDateTime.of(2026, 9, 9, 10, 30, 0, 0, localOffset)
        val end = OffsetDateTime.of(2026, 9, 9, 18, 0, 0, 0, localOffset)

        val session = DeliveryPartnerSession(
            id = "session-123",
            userId = "user-abc",
            partnerId = "partner-xyz",
            routeId = "route-789",
            expectedPackageCount = 80,
            scannedBarcodes = listOf("PKG001", "PKG002", "PKG003"),
            scannedCount = 3,
            deliveredCount = 2,
            returnedCount = 1,
            startTime = now,
            endTime = end,
            amountPaid = BigDecimal("235.50"),
            expenseId = "expense-999"
        )

        val dto = session.toDto()
        assertEquals("session-123", dto.id)
        assertEquals("user-abc", dto.userId)
        assertEquals("partner-xyz", dto.partnerId)
        assertEquals("route-789", dto.routeId)
        assertEquals(80, dto.expectedPackageCount)
        assertEquals(3, dto.scannedCount)
        assertEquals(listOf("PKG001", "PKG002", "PKG003"), dto.scannedBarcodes)
        assertEquals(2, dto.deliveredCount)
        assertEquals(1, dto.returnedCount)
        assertEquals(BigDecimal("235.50"), dto.amountPaid)
        assertEquals("expense-999", dto.expenseId)
        assertEquals(now.toString(), dto.startTime)
        assertEquals(end.toString(), dto.endTime)

        val domain = dto.toDomain()
        assertEquals(session.id, domain.id)
        assertEquals(session.userId, domain.userId)
        assertEquals(session.partnerId, domain.partnerId)
        assertEquals(session.routeId, domain.routeId)
        assertEquals(session.expectedPackageCount, domain.expectedPackageCount)
        assertEquals(session.scannedCount, domain.scannedCount)
        assertEquals(session.scannedBarcodes, domain.scannedBarcodes)
        assertEquals(session.deliveredCount, domain.deliveredCount)
        assertEquals(session.returnedCount, domain.returnedCount)
        assertEquals(session.amountPaid, domain.amountPaid)
        assertEquals(session.expenseId, domain.expenseId)
        assertEquals(session.startTime, domain.startTime)
        assertEquals(session.endTime, domain.endTime)
    }

    @Test
    fun testUtcStringToLocalDomainConversion() {
        val dto = DeliveryPartnerSessionDto(
            userId = "user-abc",
            partnerId = "partner-xyz",
            startTime = "2026-09-09T18:30:00Z",
            endTime = "2026-09-09T22:00:00Z"
        )
        val domain = dto.toDomain()
        assertNotNull(domain.startTime)
        assertNotNull(domain.endTime)

        // Deve converter para o fuso local do dispositivo
        val expectedLocalHour = java.time.Instant.parse("2026-09-09T18:30:00Z")
            .atZone(java.time.ZoneId.systemDefault()).hour
        val expectedLocalEndHour = java.time.Instant.parse("2026-09-09T22:00:00Z")
            .atZone(java.time.ZoneId.systemDefault()).hour

        assertEquals(expectedLocalHour, domain.startTime!!.hour)
        assertEquals(expectedLocalEndHour, domain.endTime!!.hour)
    }

    @Test
    fun testPaymentCalculation() {
        val partner = DeliveryPartner(
            id = "p-1",
            userId = "u-1",
            fullName = "Carlos Entregador",
            packageRate = BigDecimal("2.50"),
            defaultBonus = BigDecimal("30.00")
        )

        // 80 entregues * 2.50 + 30 = 200.00 + 30.00 = 230.00
        val deliveredCount = 80
        val baseAmount = BigDecimal(deliveredCount).multiply(partner.packageRate)
        val total = baseAmount.add(partner.defaultBonus)

        assertEquals(BigDecimal("230.00"), total)
    }

    @Test
    fun testPaymentCalculationWithoutBonus() {
        val partner = DeliveryPartner(
            id = "p-2",
            userId = "u-1",
            fullName = "Mariana Motogirl",
            packageRate = BigDecimal("3.00"),
            defaultBonus = BigDecimal.ZERO
        )

        val deliveredCount = 45
        val baseAmount = BigDecimal(deliveredCount).multiply(partner.packageRate)
        val total = baseAmount.add(partner.defaultBonus)

        assertEquals(BigDecimal("135.00"), total)
    }

    @Test
    fun testBarcodeDeduplication() {
        val barcodes = mutableSetOf<String>()

        val scan1 = "PKG-10001"
        val scan2 = "PKG-10002"
        val scan3 = "PKG-10001" // duplicate

        barcodes.add(scan1)
        barcodes.add(scan2)
        barcodes.add(scan3)

        assertEquals(2, barcodes.size)
        assertTrue(barcodes.contains("PKG-10001"))
        assertTrue(barcodes.contains("PKG-10002"))
    }

    @Test
    fun testDivergenceDetection() {
        val expected = 80
        val scanned = 75

        // Expedição vs Bipagem
        val hasExpeditionDivergence = scanned != expected
        assertTrue(hasExpeditionDivergence)

        // Finalização: Entregues + Devolvidos vs Bipados
        val delivered = 70
        val returned = 4
        val totalAccounted = delivered + returned // 74 != 75

        val hasClosingDivergence = totalAccounted != scanned
        assertTrue(hasClosingDivergence)

        // Caso sem divergência
        val returnedFixed = 5 // 70 + 5 = 75
        assertEquals(scanned, delivered + returnedFixed)
    }
}
