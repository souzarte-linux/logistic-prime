package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.data.model.VariableCycleItem
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.ui.screens.deliverypartners.VariableCycleFormEntry
import com.fernando.centraldomotorista.ui.utils.isValidCpf
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class DeliveryPartnerTest {

    @Test
    fun testCpfValidation() {
        // Valid CPF examples
        assertTrue(isValidCpf("52998224725"))
        assertTrue(isValidCpf("529.982.247-25"))
        assertTrue(isValidCpf("11144477735"))

        // Invalid CPF examples
        assertFalse(isValidCpf("11111111111")) // Repeated digits
        assertFalse(isValidCpf("00000000000"))
        assertFalse(isValidCpf("12345678900")) // Invalid checksum
        assertFalse(isValidCpf("123")) // Too short
        assertFalse(isValidCpf(""))
    }

    @Test
    fun testDeliveryRouteDtoMapping() {
        val route = DeliveryRoute(
            id = "route-1",
            userId = "user-123",
            name = "Av. Hilda"
        )
        val dto = route.toDto()
        assertEquals("route-1", dto.id)
        assertEquals("user-123", dto.userId)
        assertEquals("Av. Hilda", dto.name)

        val domain = dto.toDomain()
        assertEquals(route.id, domain.id)
        assertEquals(route.userId, domain.userId)
        assertEquals(route.name, domain.name)
    }

    @Test
    fun testDeliveryPartnerDtoMappingWithBigDecimal() {
        val partner = DeliveryPartner(
            id = "partner-1",
            userId = "user-123",
            fullName = "João da Silva",
            cep = "41000-000",
            street = "Rua Principal",
            number = "100",
            neighborhood = "Centro",
            city = "Salvador",
            state = "BA",
            phone = "71999998888",
            isWhatsapp = true,
            socialMedia = "@joao_silva",
            pixKey = "71999998888",
            pixBank = "Nubank",
            cpf = "52998224725",
            preferredRouteId = "route-1",
            packageRate = BigDecimal("4.50"),
            defaultBonus = BigDecimal("1.00"),
            deliveryType = "moto",
            rating = 5,
            paymentCycleType = "variable",
            paymentCycleFixed = null,
            paymentCycleVariableDays = listOf(7, 7, 15, 15),
            active = true
        )

        val dto = partner.toDto()
        assertEquals("partner-1", dto.id)
        assertEquals("user-123", dto.userId)
        assertEquals("João da Silva", dto.fullName)
        assertEquals("41000-000", dto.cep)
        assertEquals("52998224725", dto.cpf)
        assertEquals(BigDecimal("4.50"), dto.packageRate)
        assertEquals(BigDecimal("1.00"), dto.defaultBonus)
        assertNotNull(dto.defaultBonus)
        assertEquals(listOf(7, 7, 15, 15), dto.paymentCycleVariableDays)

        val domain = dto.toDomain()
        assertEquals(partner.id, domain.id)
        assertEquals(partner.fullName, domain.fullName)
        assertEquals(partner.packageRate, domain.packageRate)
        assertEquals(partner.defaultBonus, domain.defaultBonus)
        assertEquals(partner.paymentCycleVariableDays, domain.paymentCycleVariableDays)
    }

    @Test
    fun testDeliveryPartnerDefaultValues() {
        val partner = DeliveryPartner(
            fullName = "Maria Santos"
        )
        assertEquals(BigDecimal.ZERO, partner.packageRate)
        assertEquals(BigDecimal.ZERO, partner.defaultBonus)
        assertEquals("moto", partner.deliveryType)
        assertEquals(3, partner.rating)
        assertEquals(true, partner.active)
        assertEquals(7, partner.paymentDelayDays)
        assertTrue(partner.includeEndDate)
    }

    @Test
    fun testVariablePaymentCycleDtoMapping() {
        val partner = DeliveryPartner(
            id = "partner-var-1",
            userId = "user-123",
            fullName = "Carlos Entregador",
            paymentCycleType = "variable",
            paymentCycleFixed = null,
            cycleStartDate = "2026-09-01",
            cycleEndDate = "2026-09-07",
            includeEndDate = true,
            paymentDelayDays = 7,
            paymentDate = "2026-09-14",
            active = true
        )

        val dto = partner.toDto()
        assertEquals("2026-09-01", dto.cycleStartDate)
        assertEquals("2026-09-07", dto.cycleEndDate)
        assertTrue(dto.includeEndDate)
        assertEquals(7, dto.paymentDelayDays)
        assertEquals("2026-09-14", dto.paymentDate)

        val domain = dto.toDomain()
        assertEquals("2026-09-01", domain.cycleStartDate)
        assertEquals("2026-09-07", domain.cycleEndDate)
        assertTrue(domain.includeEndDate)
        assertEquals(7, domain.paymentDelayDays)
        assertEquals("2026-09-14", domain.paymentDate)
    }

    @Test
    fun testMultipleVariableCyclesDtoMapping() {
        val cycles = listOf(
            VariableCycleItem(
                startDate = "2026-09-01",
                endDate = "2026-09-07",
                includeEndDate = true,
                paymentDelayDays = 7,
                paymentDate = "2026-09-14"
            ),
            VariableCycleItem(
                startDate = "2026-09-08",
                endDate = "2026-09-14",
                includeEndDate = false,
                paymentDelayDays = 5,
                paymentDate = "2026-09-19"
            )
        )

        val partner = DeliveryPartner(
            id = "partner-var-multi",
            fullName = "Roberto Ciclos",
            paymentCycleType = "variable",
            variableCycles = cycles
        )

        val dto = partner.toDto()
        assertNotNull(dto.variableCycles)
        assertEquals(2, dto.variableCycles?.size)
        assertEquals("2026-09-01", dto.variableCycles?.get(0)?.startDate)
        assertEquals("2026-09-07", dto.variableCycles?.get(0)?.endDate)
        assertTrue(dto.variableCycles?.get(0)?.includeEndDate == true)
        assertEquals(7, dto.variableCycles?.get(0)?.paymentDelayDays)
        assertEquals("2026-09-14", dto.variableCycles?.get(0)?.paymentDate)

        assertEquals("2026-09-08", dto.variableCycles?.get(1)?.startDate)
        assertEquals("2026-09-14", dto.variableCycles?.get(1)?.endDate)
        assertFalse(dto.variableCycles?.get(1)?.includeEndDate == true)
        assertEquals(5, dto.variableCycles?.get(1)?.paymentDelayDays)
        assertEquals("2026-09-19", dto.variableCycles?.get(1)?.paymentDate)

        val domain = dto.toDomain()
        assertNotNull(domain.variableCycles)
        assertEquals(2, domain.variableCycles?.size)
        assertEquals(cycles[0], domain.variableCycles?.get(0))
        assertEquals(cycles[1], domain.variableCycles?.get(1))
    }

    @Test
    fun testVariableCycleFormEntryCalculation() {
        val cycleEntry = VariableCycleFormEntry(
            startDate = "2026-09-01",
            endDate = "2026-09-07",
            includeEndDate = true,
            paymentDelayDaysText = "7"
        )

        assertEquals("01/09/2026", cycleEntry.formattedStartDate)
        assertEquals("07/09/2026", cycleEntry.formattedEndDate)
        assertEquals(7, cycleEntry.paymentDelayDays)
        assertNotNull(cycleEntry.calculatedPaymentDate)
        assertEquals("2026-09-14", cycleEntry.calculatedPaymentDate.toString())
        assertTrue(cycleEntry.formattedPaymentDateText.contains("14/09/2026"))
        assertTrue(cycleEntry.formattedPaymentDateText.contains("Segunda-feira"))
    }

    @Test
    fun testDeliveryPartnerFormDataCalculation() {
        val form = com.fernando.centraldomotorista.ui.screens.deliverypartners.DeliveryPartnerFormData(
            cycle = "misto",
            cycleStartDate = "2026-09-01",
            cycleEndDate = "2026-09-07",
            includeEndDate = true,
            paymentDelayDaysText = "7"
        )

        assertEquals("01/09/2026", form.formattedCycleStartDate)
        assertEquals("07/09/2026", form.formattedCycleEndDate)
        assertEquals(7, form.paymentDelayDays)
        assertNotNull(form.calculatedPaymentDate)
        assertEquals("2026-09-14", form.calculatedPaymentDate.toString())
        assertTrue(form.formattedPaymentDateText.contains("14/09/2026"))
        assertTrue(form.formattedPaymentDateText.contains("Segunda-feira"))
    }
}

