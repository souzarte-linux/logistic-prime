package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
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
    }
}
