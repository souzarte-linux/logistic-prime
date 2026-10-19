package com.fernando.centraldomotorista.home

import com.fernando.centraldomotorista.data.model.BillingCycle
import com.fernando.centraldomotorista.data.repository.BillingCycleWithTotals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class HomeReceivablesSyncTest {

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
                periodStart = LocalDate.of(2026, 10, 1),
                periodEnd = LocalDate.of(2026, 10, 15),
                expectedPaymentDate = LocalDate.of(2026, 10, 30),
                status = status
            ),
            platformName = if (platformId == "plat_1") "Ashser" else "Shopee",
            totalAmount = amount,
            routeAmount = amount
        )
    }

    @Test
    fun `test contasAReceber matches exactly non-zero a_vencer cycles`() {
        val allCyclesWithTotals = listOf(
            createDummyCycle("1", "plat_1", "a_vencer", BigDecimal("850.50")),
            createDummyCycle("2", "plat_2", "a_vencer", BigDecimal("420.00")),
            createDummyCycle("3", "plat_1", "a_vencer", BigDecimal.ZERO), // zerada, não conta
            createDummyCycle("4", "plat_1", "em_aberto", BigDecimal("1200.00")), // em aberto, não entra no a receber
            createDummyCycle("5", "plat_2", "pago", BigDecimal("900.00")) // pago, não entra no a receber
        )

        // Filtro idêntico ao implementado em HomeRepository e FaturasViewModel
        val aVencerCycles = allCyclesWithTotals.filter { it.cycle.status == "a_vencer" && it.totalAmount > BigDecimal.ZERO }
        val contasAReceber = aVencerCycles.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.totalAmount) }

        assertEquals(2, aVencerCycles.size)
        assertEquals(BigDecimal("1270.50"), contasAReceber)
    }
}
