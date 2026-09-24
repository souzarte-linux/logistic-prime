package com.fernando.centraldomotorista.faturas

import com.fernando.centraldomotorista.data.model.BillingCycle
import com.fernando.centraldomotorista.data.repository.BillingCycleWithTotals
import com.fernando.centraldomotorista.ui.screens.faturas.FaturasUiState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class FaturasUiStateTest {

    private fun createDummyCycle(
        id: String,
        platformId: String,
        status: String,
        amount: BigDecimal
    ): BillingCycleWithTotals {
        return BillingCycleWithTotals(
            cycle = BillingCycle(
                id = id,
                userId = "user_1",
                platformId = platformId,
                periodStart = LocalDate.of(2026, 3, 1),
                periodEnd = LocalDate.of(2026, 3, 15),
                expectedPaymentDate = LocalDate.of(2026, 3, 18),
                status = status
            ),
            platformName = if (platformId == "plat_1") "Mercado Livre" else "Shopee",
            routeAmount = amount,
            totalAmount = amount,
            routeCount = 5
        )
    }

    @Test
    fun `test totalEmAberto, totalAVencer and totalPago sum correctly`() {
        val cycles = listOf(
            createDummyCycle("1", "plat_1", "em_aberto", BigDecimal("1500.50")),
            createDummyCycle("2", "plat_2", "a_vencer", BigDecimal("850.00")),
            createDummyCycle("3", "plat_1", "pago", BigDecimal("2300.00")),
            createDummyCycle("4", "plat_2", "cancelado", BigDecimal("500.00"))
        )

        val state = FaturasUiState(cycles = cycles)

        assertEquals(BigDecimal("1500.50"), state.totalEmAberto)
        assertEquals(BigDecimal("850.00"), state.totalAVencer)
        assertEquals(BigDecimal("2300.00"), state.totalPago)
    }

    @Test
    fun `test filtering by platform preserves correct open and paid cycles`() {
        val cycles = listOf(
            createDummyCycle("1", "plat_1", "em_aberto", BigDecimal("1000.00")),
            createDummyCycle("2", "plat_2", "em_aberto", BigDecimal("500.00")),
            createDummyCycle("3", "plat_1", "pago", BigDecimal("1200.00")),
            createDummyCycle("4", "plat_2", "pago", BigDecimal("700.00")),
            createDummyCycle("5", "plat_1", "a_vencer", BigDecimal("350.00"))
        )

        // When all platforms selected
        val allState = FaturasUiState(cycles = cycles, selectedPlatformFilter = "all")
        assertEquals(2, allState.emAbertoCycles.size)
        assertEquals(1, allState.aVencerCycles.size)
        assertEquals(2, allState.pagoCycles.size)

        // When plat_1 is selected
        val plat1State = FaturasUiState(cycles = cycles, selectedPlatformFilter = "plat_1")
        assertEquals(1, plat1State.emAbertoCycles.size)
        assertEquals("1", plat1State.emAbertoCycles.first().cycle.id)
        assertEquals(1, plat1State.aVencerCycles.size)
        assertEquals("5", plat1State.aVencerCycles.first().cycle.id)
        assertEquals(1, plat1State.pagoCycles.size)
        assertEquals("3", plat1State.pagoCycles.first().cycle.id)

        // When plat_2 is selected
        val plat2State = FaturasUiState(cycles = cycles, selectedPlatformFilter = "plat_2")
        assertEquals(1, plat2State.emAbertoCycles.size)
        assertEquals("2", plat2State.emAbertoCycles.first().cycle.id)
        assertEquals(0, plat2State.aVencerCycles.size)
        assertEquals(1, plat2State.pagoCycles.size)
        assertEquals("4", plat2State.pagoCycles.first().cycle.id)
    }

    @Test
    fun `test pagoMonthGroups groups by month and week with correct sorting`() {
        // Ciclos pagos em Setembro de 2026:
        // c1: Shopee, pago em 05/09/2026 (Semana 1)
        // c2: Mercado Livre, pago em 03/09/2026 (Semana 1)
        // c3: Amazon, pago em 18/09/2026 (Semana 3)
        // c4: Loggi, pago em 25/09/2026 (Semana 4)
        val c1 = BillingCycleWithTotals(
            cycle = BillingCycle(
                id = "c1", userId = "u1", platformId = "p_shopee",
                periodStart = LocalDate.of(2026, 8, 20), periodEnd = LocalDate.of(2026, 8, 31),
                expectedPaymentDate = LocalDate.of(2026, 9, 5), paymentReceivedDate = LocalDate.of(2026, 9, 5),
                status = "pago"
            ),
            platformName = "Shopee",
            totalAmount = BigDecimal("500.00")
        )
        val c2 = BillingCycleWithTotals(
            cycle = BillingCycle(
                id = "c2", userId = "u1", platformId = "p_ml",
                periodStart = LocalDate.of(2026, 8, 20), periodEnd = LocalDate.of(2026, 8, 31),
                expectedPaymentDate = LocalDate.of(2026, 9, 3), paymentReceivedDate = LocalDate.of(2026, 9, 3),
                status = "pago"
            ),
            platformName = "Mercado Livre",
            totalAmount = BigDecimal("1200.00")
        )
        val c3 = BillingCycleWithTotals(
            cycle = BillingCycle(
                id = "c3", userId = "u1", platformId = "p_amazon",
                periodStart = LocalDate.of(2026, 9, 1), periodEnd = LocalDate.of(2026, 9, 15),
                expectedPaymentDate = LocalDate.of(2026, 9, 18), paymentReceivedDate = LocalDate.of(2026, 9, 18),
                status = "pago"
            ),
            platformName = "Amazon",
            totalAmount = BigDecimal("800.00")
        )
        val c4 = BillingCycleWithTotals(
            cycle = BillingCycle(
                id = "c4", userId = "u1", platformId = "p_loggi",
                periodStart = LocalDate.of(2026, 9, 15), periodEnd = LocalDate.of(2026, 9, 21),
                expectedPaymentDate = LocalDate.of(2026, 9, 25), paymentReceivedDate = LocalDate.of(2026, 9, 25),
                status = "pago"
            ),
            platformName = "Loggi",
            totalAmount = BigDecimal("350.00")
        )

        val state = FaturasUiState(cycles = listOf(c1, c2, c3, c4))
        val monthGroups = state.pagoMonthGroups

        assertEquals(1, monthGroups.size)
        val septGroup = monthGroups[0]
        assertEquals("Setembro de 2026", septGroup.monthLabel)
        assertEquals(4, septGroup.totalInvoices)
        assertEquals(BigDecimal("2850.00"), septGroup.totalAmount)

        // Verificação das semanas do mês (ordenadas por weekNumber decrescente)
        // Semana 4 (25/09), Semana 3 (18/09), Semana 1 (03/09 e 05/09)
        assertEquals(3, septGroup.weeks.size)
        assertEquals(4, septGroup.weeks[0].weekNumber)
        assertEquals(3, septGroup.weeks[1].weekNumber)
        assertEquals(1, septGroup.weeks[2].weekNumber)

        // Na Semana 1, temos Mercado Livre (03/09) e Shopee (05/09).
        // Ordem alfabética: Mercado Livre vem antes de Shopee!
        val week1Items = septGroup.weeks[2].items
        assertEquals(2, week1Items.size)
        assertEquals("Mercado Livre", week1Items[0].platformName)
        assertEquals("Shopee", week1Items[1].platformName)
    }
}
