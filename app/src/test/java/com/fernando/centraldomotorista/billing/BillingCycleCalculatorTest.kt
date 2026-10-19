package com.fernando.centraldomotorista.billing

import com.fernando.centraldomotorista.data.billing.BillingCycleCalculator
import com.fernando.centraldomotorista.data.model.CycleEntry
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.model.PlatformRules
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class BillingCycleCalculatorTest {

    @Test
    fun testWeeklyCycleIntervals() {
        val platform = Platform(
            id = "plat-1",
            userId = "user-1",
            name = "Loggi",
            cycle = "semanal",
            paymentDay = "QUA",
            rules = PlatformRules(fixedPayDelay = 3)
        )

        // Quarta-feira, 15 de abril de 2026
        val refDate = LocalDate.of(2026, 4, 15)
        val intervals = BillingCycleCalculator.getPlatformCycleIntervals(platform, refDate)

        assertEquals(2, intervals.size)

        // Ciclo 0 (semana corrente): de segunda 13/04/2026 a domingo 19/04/2026
        val c0 = intervals[0]
        assertEquals(LocalDate.of(2026, 4, 13), c0.periodStart)
        assertEquals(LocalDate.of(2026, 4, 19), c0.periodEnd)
        // Pagamento 3 dias após o fim (19 + 3 = 22/04/2026, quarta-feira)
        assertEquals(LocalDate.of(2026, 4, 22), c0.expectedPaymentDate)

        // Ciclo 1 (próxima semana): de segunda 20/04/2026 a domingo 26/04/2026
        val c1 = intervals[1]
        assertEquals(LocalDate.of(2026, 4, 20), c1.periodStart)
        assertEquals(LocalDate.of(2026, 4, 26), c1.periodEnd)
        assertEquals(LocalDate.of(2026, 4, 29), c1.expectedPaymentDate)
    }

    @Test
    fun testQuinzenalCycleIntervalsFirstHalf() {
        val platform = Platform(
            id = "plat-2",
            userId = "user-1",
            name = "Shopee",
            cycle = "quinzenal",
            paymentDay = null,
            rules = PlatformRules(fixedPayDelay = 5)
        )

        // Dia 10 de maio de 2026 (primeira quinzena)
        val refDate = LocalDate.of(2026, 5, 10)
        val intervals = BillingCycleCalculator.getPlatformCycleIntervals(platform, refDate)

        assertEquals(2, intervals.size)

        // Ciclo 0: 01/05 a 15/05, pagamento dia 20/05 (15 + 5)
        val c0 = intervals[0]
        assertEquals(LocalDate.of(2026, 5, 1), c0.periodStart)
        assertEquals(LocalDate.of(2026, 5, 15), c0.periodEnd)
        assertEquals(LocalDate.of(2026, 5, 20), c0.expectedPaymentDate)

        // Ciclo 1: 16/05 a 31/05, pagamento dia 05/06 (31/05 + 5)
        val c1 = intervals[1]
        assertEquals(LocalDate.of(2026, 5, 16), c1.periodStart)
        assertEquals(LocalDate.of(2026, 5, 31), c1.periodEnd)
        assertEquals(LocalDate.of(2026, 6, 5), c1.expectedPaymentDate)
    }

    @Test
    fun testQuinzenalCycleIntervalsSecondHalf() {
        val platform = Platform(
            id = "plat-2",
            userId = "user-1",
            name = "Shopee",
            cycle = "quinzenal",
            paymentDay = null,
            rules = PlatformRules(fixedPayDelay = 7)
        )

        // Dia 20 de maio de 2026 (segunda quinzena)
        val refDate = LocalDate.of(2026, 5, 20)
        val intervals = BillingCycleCalculator.getPlatformCycleIntervals(platform, refDate)

        assertEquals(2, intervals.size)

        // Ciclo 0: 16/05 a 31/05, pagamento 07/06 (31/05 + 7)
        val c0 = intervals[0]
        assertEquals(LocalDate.of(2026, 5, 16), c0.periodStart)
        assertEquals(LocalDate.of(2026, 5, 31), c0.periodEnd)
        assertEquals(LocalDate.of(2026, 6, 7), c0.expectedPaymentDate)

        // Ciclo 1: 01/06 a 15/06, pagamento 22/06 (15/06 + 7)
        val c1 = intervals[1]
        assertEquals(LocalDate.of(2026, 6, 1), c1.periodStart)
        assertEquals(LocalDate.of(2026, 6, 15), c1.periodEnd)
        assertEquals(LocalDate.of(2026, 6, 22), c1.expectedPaymentDate)
    }

    @Test
    fun testMensalCycleIntervals() {
        val platform = Platform(
            id = "plat-3",
            userId = "user-1",
            name = "Frota Mensal",
            cycle = "mensal",
            paymentDay = null,
            rules = PlatformRules(fixedPayDelay = 5)
        )

        val refDate = LocalDate.of(2026, 2, 10)
        val intervals = BillingCycleCalculator.getPlatformCycleIntervals(platform, refDate)

        assertEquals(2, intervals.size)

        // Fevereiro 2026 tem 28 dias
        val c0 = intervals[0]
        assertEquals(LocalDate.of(2026, 2, 1), c0.periodStart)
        assertEquals(LocalDate.of(2026, 2, 28), c0.periodEnd)
        assertEquals(LocalDate.of(2026, 3, 5), c0.expectedPaymentDate)

        // Março 2026 tem 31 dias
        val c1 = intervals[1]
        assertEquals(LocalDate.of(2026, 3, 1), c1.periodStart)
        assertEquals(LocalDate.of(2026, 3, 31), c1.periodEnd)
        assertEquals(LocalDate.of(2026, 4, 5), c1.expectedPaymentDate)
    }

    @Test
    fun testMistoCycleIntervals() {
        val platform = Platform(
            id = "plat-4",
            userId = "user-1",
            name = "Misto Custom",
            cycle = "misto",
            paymentDay = null,
            rules = PlatformRules(
                cycleEntries = listOf(
                    CycleEntry(cut = 5, payDelay = 5),
                    CycleEntry(cut = 20, payDelay = 7)
                )
            )
        )

        // Dia 10 de maio de 2026: está dentro do período que começou no dia 05 e termina no dia 19
        val refDate = LocalDate.of(2026, 5, 10)
        val intervals = BillingCycleCalculator.getPlatformCycleIntervals(platform, refDate)

        assertEquals(2, intervals.size)

        val c0 = intervals[0]
        assertEquals(LocalDate.of(2026, 5, 5), c0.periodStart)
        assertEquals(LocalDate.of(2026, 5, 19), c0.periodEnd)
        assertEquals(LocalDate.of(2026, 5, 24), c0.expectedPaymentDate) // 19 + 5 = 24

        val c1 = intervals[1]
        assertEquals(LocalDate.of(2026, 5, 20), c1.periodStart)
        assertEquals(LocalDate.of(2026, 6, 4), c1.periodEnd)
        assertEquals(LocalDate.of(2026, 6, 11), c1.expectedPaymentDate) // 4 + 7 = 11
    }

    @Test
    fun testOverlapCheck() {
        // [10/05, 20/05] e [15/05, 25/05] -> Sobrepõem
        assertTrue(
            BillingCycleCalculator.checkOverlap(
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 20),
                LocalDate.of(2026, 5, 15),
                LocalDate.of(2026, 5, 25)
            )
        )

        // [01/05, 15/05] e [16/05, 31/05] -> Adjacentes sem sobreposição
        assertFalse(
            BillingCycleCalculator.checkOverlap(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 15),
                LocalDate.of(2026, 5, 16),
                LocalDate.of(2026, 5, 31)
            )
        )
    }

    @Test
    fun testCycleDisplayLabel() {
        assertEquals("SEMANAL (QUA)", BillingCycleCalculator.getCycleDisplayLabel("semanal", "Quarta-feira"))
        assertEquals("QUINZENAL", BillingCycleCalculator.getCycleDisplayLabel("quinzenal", null))
        assertEquals("MENSAL", BillingCycleCalculator.getCycleDisplayLabel("mensal", null))
        assertEquals("VARIÁVEL", BillingCycleCalculator.getCycleDisplayLabel("misto", null))
        assertEquals("VARIÁVEL", BillingCycleCalculator.getCycleDisplayLabel("variavel", null))
    }

    @Test
    fun testAsherQuinzenalOctober19Scenario() {
        // Cenário exato reportado: 19 de outubro de 2026, Plataforma Asher com pagamento quinzenal e delay de 15 dias
        val asher = Platform(
            id = "asher-1",
            userId = "user-1",
            name = "Asher - Entrega Cartão",
            cycle = "quinzenal",
            paymentDay = null,
            rules = PlatformRules(fixedPayDelay = 15)
        )

        val today = LocalDate.of(2026, 10, 19)

        // 1. Ciclo Atual (c0): 16/10 a 31/10 -> EM ABERTO
        val intervals = BillingCycleCalculator.getPlatformCycleIntervals(asher, today)
        assertEquals(2, intervals.size)
        val c0 = intervals[0]
        assertEquals(LocalDate.of(2026, 10, 16), c0.periodStart)
        assertEquals(LocalDate.of(2026, 10, 31), c0.periodEnd)
        assertEquals(LocalDate.of(2026, 11, 15), c0.expectedPaymentDate)
        // O ciclo corrente NÃO está fechado em 19/10
        assertFalse(BillingCycleCalculator.isCycleClosed(c0, today))

        // 2. Ciclo Anterior (c_prev): 01/10 a 15/10 -> A VENCER
        val cPrev = BillingCycleCalculator.getPreviousPlatformCycleInterval(asher, today)
        assertEquals(LocalDate.of(2026, 10, 1), cPrev.periodStart)
        assertEquals(LocalDate.of(2026, 10, 15), cPrev.periodEnd)
        // Pagamento 15 dias após o término da 1ª quinzena (15 + 15 = 30 de outubro)
        assertEquals(LocalDate.of(2026, 10, 30), cPrev.expectedPaymentDate)
        // O ciclo de 01 a 15/10 JÁ está fechado em 19/10 (19 > 15)
        assertTrue(BillingCycleCalculator.isCycleClosed(cPrev, today))
    }

    @Test
    fun testQuinzenalFirstHalfScenario() {
        val platform = Platform(
            id = "plat-quinzenal",
            userId = "user-1",
            name = "Asher",
            cycle = "quinzenal",
            paymentDay = null,
            rules = PlatformRules(fixedPayDelay = 15)
        )

        // Dia 05 de novembro de 2026 (primeira quinzena)
        val today = LocalDate.of(2026, 11, 5)

        // Ciclo anterior foi 16/10 a 31/10 com pagamento em 15/11
        val cPrev = BillingCycleCalculator.getPreviousPlatformCycleInterval(platform, today)
        assertEquals(LocalDate.of(2026, 10, 16), cPrev.periodStart)
        assertEquals(LocalDate.of(2026, 10, 31), cPrev.periodEnd)
        assertEquals(LocalDate.of(2026, 11, 15), cPrev.expectedPaymentDate)
        assertTrue(BillingCycleCalculator.isCycleClosed(cPrev, today))

        // Ciclo atual é 01/11 a 15/11 com pagamento em 30/11
        val c0 = BillingCycleCalculator.getPlatformCycleIntervals(platform, today)[0]
        assertEquals(LocalDate.of(2026, 11, 1), c0.periodStart)
        assertEquals(LocalDate.of(2026, 11, 15), c0.periodEnd)
        assertEquals(LocalDate.of(2026, 11, 30), c0.expectedPaymentDate)
        assertFalse(BillingCycleCalculator.isCycleClosed(c0, today))
    }

    @Test
    fun testWeeklyPreviousCycleClosed() {
        val platform = Platform(
            id = "plat-semanal",
            userId = "user-1",
            name = "Loggi",
            cycle = "semanal",
            paymentDay = null,
            rules = PlatformRules(fixedPayDelay = 3)
        )

        // Segunda-feira, 19 de outubro de 2026
        val today = LocalDate.of(2026, 10, 19)

        // Ciclo anterior foi de segunda 12/10 a domingo 18/10
        val cPrev = BillingCycleCalculator.getPreviousPlatformCycleInterval(platform, today)
        assertEquals(LocalDate.of(2026, 10, 12), cPrev.periodStart)
        assertEquals(LocalDate.of(2026, 10, 18), cPrev.periodEnd)
        assertEquals(LocalDate.of(2026, 10, 21), cPrev.expectedPaymentDate)
        // Fechou domingo (18/10), hoje é 19/10 -> fechado (A Vencer)
        assertTrue(BillingCycleCalculator.isCycleClosed(cPrev, today))

        // Ciclo atual de 19/10 a 25/10 -> Em aberto
        val c0 = BillingCycleCalculator.getPlatformCycleIntervals(platform, today)[0]
        assertEquals(LocalDate.of(2026, 10, 19), c0.periodStart)
        assertEquals(LocalDate.of(2026, 10, 25), c0.periodEnd)
        assertFalse(BillingCycleCalculator.isCycleClosed(c0, today))
    }
}
