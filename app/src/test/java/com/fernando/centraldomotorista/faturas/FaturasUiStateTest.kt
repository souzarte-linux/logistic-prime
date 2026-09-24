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
}
