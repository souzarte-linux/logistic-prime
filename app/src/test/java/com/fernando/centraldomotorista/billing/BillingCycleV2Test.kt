package com.fernando.centraldomotorista.billing

import com.fernando.centraldomotorista.data.billing.BillingCycleCalculator
import com.fernando.centraldomotorista.data.billing.CycleInterval
import com.fernando.centraldomotorista.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class BillingCycleV2Test {

    // =========================================================================
    // CRITÉRIO 1: Cálculo de ciclos com includeEndDate = true e false em BillingCycleCalculator
    // =========================================================================

    @Test
    fun `test isDateInCycle with includeEndDate true includes start, intermediate and end dates`() {
        val start = LocalDate.of(2026, 5, 1)
        val end = LocalDate.of(2026, 5, 15)

        // Antes do início
        assertFalse(BillingCycleCalculator.isDateInCycle(LocalDate.of(2026, 4, 30), start, end, includeEndDate = true))

        // No início exato
        assertTrue(BillingCycleCalculator.isDateInCycle(LocalDate.of(2026, 5, 1), start, end, includeEndDate = true))

        // Dia intermediário
        assertTrue(BillingCycleCalculator.isDateInCycle(LocalDate.of(2026, 5, 8), start, end, includeEndDate = true))

        // No fim exato (inclusivo)
        assertTrue(BillingCycleCalculator.isDateInCycle(LocalDate.of(2026, 5, 15), start, end, includeEndDate = true))

        // Após o fim
        assertFalse(BillingCycleCalculator.isDateInCycle(LocalDate.of(2026, 5, 16), start, end, includeEndDate = true))
    }

    @Test
    fun `test isDateInCycle with includeEndDate false excludes end date (semi-open interval)`() {
        val start = LocalDate.of(2026, 5, 1)
        val end = LocalDate.of(2026, 5, 15)

        // Antes do início
        assertFalse(BillingCycleCalculator.isDateInCycle(LocalDate.of(2026, 4, 30), start, end, includeEndDate = false))

        // No início exato
        assertTrue(BillingCycleCalculator.isDateInCycle(LocalDate.of(2026, 5, 1), start, end, includeEndDate = false))

        // Véspera do fim
        assertTrue(BillingCycleCalculator.isDateInCycle(LocalDate.of(2026, 5, 14), start, end, includeEndDate = false))

        // No fim exato (deve ser FALSO para intervalo semi-aberto [start, end) )
        assertFalse(BillingCycleCalculator.isDateInCycle(LocalDate.of(2026, 5, 15), start, end, includeEndDate = false))

        // Após o fim
        assertFalse(BillingCycleCalculator.isDateInCycle(LocalDate.of(2026, 5, 16), start, end, includeEndDate = false))
    }

    @Test
    fun `test checkOverlap with inclusive and semi-open intervals`() {
        val cycle1Start = LocalDate.of(2026, 5, 1)
        val cycle1End = LocalDate.of(2026, 5, 15)

        val cycle2Start = LocalDate.of(2026, 5, 15)
        val cycle2End = LocalDate.of(2026, 5, 31)

        // Caso A: Ambos inclusivos [01/05..15/05] e [15/05..31/05] -> Conflito no dia 15/05
        assertTrue(
            "Ciclos com data final inclusiva adjacente devem reportar sobreposição no dia de fronteira",
            BillingCycleCalculator.checkOverlap(
                cycle1Start, cycle1End, includeEnd1 = true,
                cycle2Start, cycle2End, includeEnd2 = true
            )
        )

        // Caso B: Ciclo 1 semi-aberto [01/05..15/05) (efetivo até 14/05) e Ciclo 2 [15/05..31/05] -> Sem conflito
        assertFalse(
            "Ciclo 1 semi-aberto não deve sobrepor o ciclo seguinte que inicia exatamente na data final de C1",
            BillingCycleCalculator.checkOverlap(
                cycle1Start, cycle1End, includeEnd1 = false,
                cycle2Start, cycle2End, includeEnd2 = true
            )
        )

        // Caso C: Totalmente disjuntos [01/05..10/05] e [15/05..25/05]
        assertFalse(
            BillingCycleCalculator.checkOverlap(
                cycle1Start, LocalDate.of(2026, 5, 10), true,
                cycle2Start, cycle2End, true
            )
        )

        // Caso D: Sobreposição interna [05/05..20/05] e [10/05..15/05]
        assertTrue(
            BillingCycleCalculator.checkOverlap(
                LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 20), true,
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 15), true
            )
        )

        // Caso E: Ciclo de 1 único dia [15/05..15/05] inclusivo sobrepõe com [10/05..20/05]
        assertTrue(
            BillingCycleCalculator.checkOverlap(
                LocalDate.of(2026, 5, 15), LocalDate.of(2026, 5, 15), true,
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 20), true
            )
        )

        // Caso F: Ciclo de 1 único dia com includeEndDate = false é inválido/vazio e não deve sobrepor
        assertFalse(
            BillingCycleCalculator.checkOverlap(
                LocalDate.of(2026, 5, 15), LocalDate.of(2026, 5, 15), false,
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 20), true
            )
        )
    }

    @Test
    fun `test platform cycle intervals with variable entries and custom includeEndDate`() {
        val entry1 = CycleEntry(
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 15),
            includeEndDate = false, // semi-aberto
            payDelayDays = 2
        )
        val entry2 = CycleEntry(
            startDate = LocalDate.of(2026, 6, 15),
            endDate = LocalDate.of(2026, 6, 30),
            includeEndDate = true, // inclusivo
            payDelayDays = 3
        )

        val platform = Platform(
            id = "plat-var",
            userId = "user-1",
            name = "Transportadora Variável",
            cycle = "variavel",
            paymentDay = null,
            rules = PlatformRules(cycleEntries = listOf(entry1, entry2))
        )

        // No dia 14/06: deve cair na entry1 (que vai até 14/06 efetivo)
        val intervalsEntry1 = BillingCycleCalculator.getPlatformCycleIntervals(platform, LocalDate.of(2026, 6, 14))
        assertEquals(2, intervalsEntry1.size)
        assertEquals(LocalDate.of(2026, 6, 1), intervalsEntry1[0].periodStart)
        assertEquals(LocalDate.of(2026, 6, 15), intervalsEntry1[0].periodEnd)
        assertFalse("includeEndDate deve ser false na entry1", intervalsEntry1[0].includeEndDate)
        assertEquals(LocalDate.of(2026, 6, 17), intervalsEntry1[0].expectedPaymentDate)

        // No dia 15/06: como entry1 é semi-aberta (terminou no dia 14), o dia 15 já pertence à entry2
        val intervalsEntry2 = BillingCycleCalculator.getPlatformCycleIntervals(platform, LocalDate.of(2026, 6, 15))
        assertEquals(1, intervalsEntry2.size)
        assertEquals(LocalDate.of(2026, 6, 15), intervalsEntry2[0].periodStart)
        assertEquals(LocalDate.of(2026, 6, 30), intervalsEntry2[0].periodEnd)
        assertTrue("includeEndDate deve ser true na entry2", intervalsEntry2[0].includeEndDate)
        assertEquals(LocalDate.of(2026, 7, 3), intervalsEntry2[0].expectedPaymentDate)
    }

    // =========================================================================
    // CRITÉRIO 2: Prazos de pagamento com dias normais e payDelayDays = 0 (mesmo dia)
    // =========================================================================

    @Test
    fun `test weekly cycle with payDelayDays equal 0 pays on the periodEnd date (same day)`() {
        val platform = Platform(
            id = "plat-same-day",
            userId = "user-1",
            name = "Express Same Day",
            cycle = "semanal",
            paymentDay = "DOM",
            rules = PlatformRules(fixedPayDelay = 0)
        )

        // Quarta-feira, 20/05/2026
        val refDate = LocalDate.of(2026, 5, 20)
        val intervals = BillingCycleCalculator.getPlatformCycleIntervals(platform, refDate)

        assertEquals(2, intervals.size)

        // Ciclo 0: 18/05/2026 a 24/05/2026 (domingo) -> Pagamento no mesmo domingo (24/05/2026)
        val c0 = intervals[0]
        assertEquals(LocalDate.of(2026, 5, 18), c0.periodStart)
        assertEquals(LocalDate.of(2026, 5, 24), c0.periodEnd)
        assertEquals("Pagamento no mesmo dia com fixedPayDelay = 0", c0.periodEnd, c0.expectedPaymentDate)

        // Ciclo 1: 25/05/2026 a 31/05/2026 (domingo) -> Pagamento no mesmo domingo (31/05/2026)
        val c1 = intervals[1]
        assertEquals(LocalDate.of(2026, 5, 25), c1.periodStart)
        assertEquals(LocalDate.of(2026, 5, 31), c1.periodEnd)
        assertEquals(c1.periodEnd, c1.expectedPaymentDate)
    }

    @Test
    fun `test quinzenal and mensal cycles with payDelayDays equal 0 pays on periodEnd date`() {
        // Quinzenal com payDelay = 0
        val quinzenalPlat = Platform(
            id = "plat-q0",
            userId = "user-1",
            name = "Quinzena Sem Atraso",
            cycle = "quinzenal",
            paymentDay = null,
            rules = PlatformRules(fixedPayDelay = 0)
        )

        val qIntervals = BillingCycleCalculator.getPlatformCycleIntervals(quinzenalPlat, LocalDate.of(2026, 5, 10))
        assertEquals(LocalDate.of(2026, 5, 15), qIntervals[0].periodEnd)
        assertEquals(LocalDate.of(2026, 5, 15), qIntervals[0].expectedPaymentDate)

        // Mensal com payDelay = 0
        val mensalPlat = Platform(
            id = "plat-m0",
            userId = "user-1",
            name = "Mensal Imediato",
            cycle = "mensal",
            paymentDay = null,
            rules = PlatformRules(fixedPayDelay = 0)
        )

        val mIntervals = BillingCycleCalculator.getPlatformCycleIntervals(mensalPlat, LocalDate.of(2026, 2, 10))
        // Fevereiro 2026 tem 28 dias
        assertEquals(LocalDate.of(2026, 2, 28), mIntervals[0].periodEnd)
        assertEquals(LocalDate.of(2026, 2, 28), mIntervals[0].expectedPaymentDate)
    }

    @Test
    fun `test variable cycle entries with payDelayDays equal 0 versus normal delay and explicit paymentDate`() {
        val entryZeroDelay = CycleEntry(
            startDate = LocalDate.of(2026, 7, 1),
            endDate = LocalDate.of(2026, 7, 10),
            payDelayDays = 0
        )
        val entryFiveDaysDelay = CycleEntry(
            startDate = LocalDate.of(2026, 7, 11),
            endDate = LocalDate.of(2026, 7, 20),
            payDelayDays = 5
        )
        val entryExplicitPaymentDate = CycleEntry(
            startDate = LocalDate.of(2026, 7, 21),
            endDate = LocalDate.of(2026, 7, 31),
            payDelayDays = 10,
            paymentDate = LocalDate.of(2026, 8, 5) // sobrescreve payDelayDays
        )

        val platform = Platform(
            id = "plat-delays",
            userId = "user-1",
            name = "Plataforma Mista de Prazos",
            cycle = "variavel",
            paymentDay = null,
            rules = PlatformRules(cycleEntries = listOf(entryZeroDelay, entryFiveDaysDelay, entryExplicitPaymentDate))
        )

        // Teste 1: payDelayDays = 0 -> Pagamento dia 10/07
        val intervals1 = BillingCycleCalculator.getPlatformCycleIntervals(platform, LocalDate.of(2026, 7, 5))
        assertEquals(LocalDate.of(2026, 7, 10), intervals1[0].expectedPaymentDate)

        // Teste 2: payDelayDays = 5 -> Pagamento dia 20 + 5 = 25/07
        val intervals2 = BillingCycleCalculator.getPlatformCycleIntervals(platform, LocalDate.of(2026, 7, 15))
        assertEquals(LocalDate.of(2026, 7, 25), intervals2[0].expectedPaymentDate)

        // Teste 3: paymentDate explícito -> Pagamento dia 05/08
        val intervals3 = BillingCycleCalculator.getPlatformCycleIntervals(platform, LocalDate.of(2026, 7, 25))
        assertEquals(LocalDate.of(2026, 8, 5), intervals3[0].expectedPaymentDate)
    }

    // =========================================================================
    // CRITÉRIO 3: Recálculo de totais com precisão BigDecimal (pacotes, rotas, diárias e 9 subtipos)
    // =========================================================================

    @Test
    fun `test calculateRouteAmount with packages and unit price precision`() {
        // 50 pacotes x R$ 4,50 = R$ 225,00
        val r1 = BillingCycleCalculator.calculateRouteAmount(50, BigDecimal("4.50"))
        assertEquals(BigDecimal("225.00"), r1)

        // 137 pacotes x R$ 3,82 = R$ 523,34
        val r2 = BillingCycleCalculator.calculateRouteAmount(137, BigDecimal("3.82"))
        assertEquals(BigDecimal("523.34"), r2)

        // 0 pacotes -> R$ 0.00
        val rZeroPackages = BillingCycleCalculator.calculateRouteAmount(0, BigDecimal("4.50"))
        assertEquals(BigDecimal.ZERO, rZeroPackages)

        // Preço zero -> R$ 0.00
        val rZeroPrice = BillingCycleCalculator.calculateRouteAmount(50, BigDecimal.ZERO)
        assertEquals(BigDecimal.ZERO, rZeroPrice)
    }

    @Test
    fun `test FinancialAdjustmentSubtype enum provides exact 4 debits and 5 credits with metadata`() {
        val allSubtypes = FinancialAdjustmentSubtype.entries

        assertEquals("Devem existir exatamente 9 subtipos de ajustes financeiros", 9, allSubtypes.size)

        val debits = allSubtypes.filter { !it.isCredit }
        val credits = allSubtypes.filter { it.isCredit }

        assertEquals("Devem existir exatamente 4 subtipos de desconto (débito)", 4, debits.size)
        assertEquals("Devem existir exatamente 5 subtipos de ganho (crédito)", 5, credits.size)

        // Validar 4 descontos
        val debitsExpected = setOf("produto_extraviado", "desconto_previdenciario", "desconto_multa", "outros_descontos")
        assertEquals(debitsExpected, debits.map { it.key }.toSet())
        debits.forEach {
            assertEquals("debito", it.defaultType)
            assertFalse(it.isCredit)
        }

        // Validação de tracking code obrigatório exclusivamente para produto extraviado
        assertTrue(FinancialAdjustmentSubtype.PRODUTO_EXTRAVIADO.requiresTrackingCode)
        assertFalse(FinancialAdjustmentSubtype.DESCONTO_PREVIDENCIARIO.requiresTrackingCode)
        assertFalse(FinancialAdjustmentSubtype.DESCONTO_MULTA.requiresTrackingCode)
        assertFalse(FinancialAdjustmentSubtype.OUTROS_DESCONTOS.requiresTrackingCode)

        // Validar 5 ganhos
        val creditsExpected = setOf("bonus", "gratificacao", "incentivo", "metas", "outros_ganhos")
        assertEquals(creditsExpected, credits.map { it.key }.toSet())
        credits.forEach {
            assertEquals("credito", it.defaultType)
            assertTrue(it.isCredit)
        }

        // Testar fromKey case-insensitive e null-safe
        assertEquals(FinancialAdjustmentSubtype.PRODUTO_EXTRAVIADO, FinancialAdjustmentSubtype.fromKey("PRODUTO_EXTRAVIADO"))
        assertEquals(FinancialAdjustmentSubtype.BONUS, FinancialAdjustmentSubtype.fromKey("bonus"))
        assertEquals(FinancialAdjustmentSubtype.GRATIFICACAO, FinancialAdjustmentSubtype.fromKey("gratificacao"))
        assertNull(FinancialAdjustmentSubtype.fromKey("inexistente"))
        assertNull(FinancialAdjustmentSubtype.fromKey(null))
    }

    @Test
    fun `test complete cycle recalculation with all 9 adjustment subtypes, routes and dailies`() {
        val cycleStart = LocalDate.of(2026, 5, 1)
        val cycleEnd = LocalDate.of(2026, 5, 15)
        val now = OffsetDateTime.of(2026, 5, 10, 10, 0, 0, 0, ZoneOffset.UTC)

        // 2 Rotas com pacotes, valores, gorjeta e bônus
        val routes = listOf(
            Route(
                id = "route-1",
                userId = "user-1",
                platformId = "plat-1",
                origin = "CD Mercado Livre",
                destination = "Zona Sul",
                amount = BigDecimal("350.50"),
                tip = BigDecimal("25.00"),
                bonus = BigDecimal("50.00"),
                packageCount = 40,
                packageUnitPrice = BigDecimal("8.7625"),
                occurredAt = now
            ),
            Route(
                id = "route-2",
                userId = "user-1",
                platformId = "plat-1",
                origin = "CD Shopee",
                destination = "Centro",
                amount = BigDecimal("420.25"),
                tip = BigDecimal("15.50"),
                bonus = BigDecimal("0.00"),
                packageCount = 50,
                packageUnitPrice = BigDecimal("8.405"),
                occurredAt = now
            )
        )

        // 2 Diárias
        val dailies = listOf(
            DailyTotal(
                id = "daily-1",
                userId = "user-1",
                platformId = "plat-1",
                amount = BigDecimal("200.00"),
                occurredAt = now
            ),
            DailyTotal(
                id = "daily-2",
                userId = "user-1",
                platformId = "plat-1",
                amount = BigDecimal("180.75"),
                occurredAt = now
            )
        )

        // Todos os 9 Subtipos de Ajustes (4 Descontos + 5 Ganhos)
        val adjustments = listOf(
            // --- 4 DESCONTOS ---
            FinancialAdjustment(
                id = "adj-deb-1",
                userId = "user-1",
                platformId = "plat-1",
                billingCycleId = "cycle-1",
                type = "debito",
                subtype = FinancialAdjustmentSubtype.PRODUTO_EXTRAVIADO.key,
                amount = BigDecimal("120.00"),
                description = "Pacote Danificado BR12345",
                notes = "BR12345",
                occurredAt = cycleStart
            ),
            FinancialAdjustment(
                id = "adj-deb-2",
                userId = "user-1",
                platformId = "plat-1",
                billingCycleId = "cycle-1",
                type = "debito",
                subtype = FinancialAdjustmentSubtype.DESCONTO_PREVIDENCIARIO.key,
                amount = BigDecimal("85.50"),
                description = "Retenção INSS",
                occurredAt = cycleStart
            ),
            FinancialAdjustment(
                id = "adj-deb-3",
                userId = "user-1",
                platformId = "plat-1",
                billingCycleId = "cycle-1",
                type = "debito",
                subtype = FinancialAdjustmentSubtype.DESCONTO_MULTA.key,
                amount = BigDecimal("150.00"),
                description = "Atraso no carregamento",
                occurredAt = cycleStart
            ),
            FinancialAdjustment(
                id = "adj-deb-4",
                userId = "user-1",
                platformId = "plat-1",
                billingCycleId = "cycle-1",
                type = "debito",
                subtype = FinancialAdjustmentSubtype.OUTROS_DESCONTOS.key,
                amount = BigDecimal("35.25"),
                description = "Uniforme avariado",
                occurredAt = cycleStart
            ),

            // --- 5 GANHOS ---
            FinancialAdjustment(
                id = "adj-cred-1",
                userId = "user-1",
                platformId = "plat-1",
                billingCycleId = "cycle-1",
                type = "credito",
                subtype = FinancialAdjustmentSubtype.BONUS.key,
                amount = BigDecimal("100.00"),
                description = "Bônus de Domingo",
                occurredAt = cycleStart
            ),
            FinancialAdjustment(
                id = "adj-cred-2",
                userId = "user-1",
                platformId = "plat-1",
                billingCycleId = "cycle-1",
                type = "credito",
                subtype = FinancialAdjustmentSubtype.GRATIFICACAO.key,
                amount = BigDecimal("50.00"),
                description = "Elogio de cliente",
                occurredAt = cycleStart
            ),
            FinancialAdjustment(
                id = "adj-cred-3",
                userId = "user-1",
                platformId = "plat-1",
                billingCycleId = "cycle-1",
                type = "credito",
                subtype = FinancialAdjustmentSubtype.INCENTIVO.key,
                amount = BigDecimal("75.25"),
                description = "Incentivo combustível",
                occurredAt = cycleStart
            ),
            FinancialAdjustment(
                id = "adj-cred-4",
                userId = "user-1",
                platformId = "plat-1",
                billingCycleId = "cycle-1",
                type = "credito",
                subtype = FinancialAdjustmentSubtype.METAS.key,
                amount = BigDecimal("120.50"),
                description = "Meta semanal batida",
                occurredAt = cycleStart
            ),
            FinancialAdjustment(
                id = "adj-cred-5",
                userId = "user-1",
                platformId = "plat-1",
                billingCycleId = "cycle-1",
                type = "credito",
                subtype = FinancialAdjustmentSubtype.OUTROS_GANHOS.key,
                amount = BigDecimal("30.00"),
                description = "Ajuda de pedágio extra",
                occurredAt = cycleStart
            )
        )

        val totals = BillingCycleCalculator.calculateCycleTotals(routes, dailies, adjustments)

        // Verificação de Contagens
        assertEquals(2, totals.routesCount)
        assertEquals(90, totals.packagesCount) // 40 + 50
        assertEquals(2, totals.dailyTotalsCount)
        assertEquals(9, totals.adjustmentsCount)

        // Verificação de Valores Brutos e Gorjetas
        // 350.50 + 420.25 = 770.75
        assertEquals(BigDecimal("770.75"), totals.grossRoutesAmount)
        // 25.00 + 15.50 = 40.50
        assertEquals(BigDecimal("40.50"), totals.totalTipsAmount)
        // 50.00 + 0.00 = 50.00
        assertEquals(BigDecimal("50.00"), totals.totalBonusAmount)
        // 200.00 + 180.75 = 380.75
        assertEquals(BigDecimal("380.75"), totals.grossDailyAmount)

        // Verificação de Créditos: 100.00 + 50.00 + 75.25 + 120.50 + 30.00 = 375.75
        assertEquals(BigDecimal("375.75"), totals.adjustmentsCredit)

        // Verificação de Débitos: 120.00 + 85.50 + 150.00 + 35.25 = 390.75
        assertEquals(BigDecimal("390.75"), totals.adjustmentsDebit)

        // Saldo líquido de ajustes: 375.75 - 390.75 = -15.00
        assertEquals(BigDecimal("-15.00"), totals.adjustmentsTotal)

        // Total Líquido Final:
        // 770.75 (rotas) + 40.50 (gorjetas) + 50.00 (bônus rotas) + 380.75 (diárias) + (-15.00 ajustes)
        // = 1242.00 - 15.00 = 1227.00
        assertEquals(BigDecimal("1227.00"), totals.netTotalAmount)
    }

    @Test
    fun `test BigDecimal cent precision handles exact cents without floating point drift`() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        // Cria 100 ajustes de 1 centavo cada (débito)
        val microAdjustments = (1..100).map { i ->
            FinancialAdjustment(
                id = "adj-$i",
                userId = "user-1",
                platformId = "plat-1",
                billingCycleId = "cycle-1",
                type = "debito",
                subtype = FinancialAdjustmentSubtype.OUTROS_DESCONTOS.key,
                amount = BigDecimal("0.01"),
                description = "Centavo $i",
                occurredAt = LocalDate.now()
            )
        }

        val totals = BillingCycleCalculator.calculateCycleTotals(
            routes = emptyList(),
            dailyTotals = emptyList(),
            adjustments = microAdjustments
        )

        // 100 x 0.01 deve ser EXATAMENTE 1.00 sem dizimas periódicas
        assertEquals(BigDecimal("1.00"), totals.adjustmentsDebit)
        assertEquals(BigDecimal("-1.00"), totals.adjustmentsTotal)
        assertEquals(BigDecimal("-1.00"), totals.netTotalAmount)
    }

    @Test
    fun `test negative net total when heavy penalties exceed total earnings`() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val route = Route(
            id = "r1", userId = "u1", platformId = "p1", origin = null, destination = null,
            amount = BigDecimal("150.00"), occurredAt = now
        )
        val penalty = FinancialAdjustment(
            id = "pen-1", userId = "u1", platformId = "p1", billingCycleId = "c1",
            type = "debito", subtype = FinancialAdjustmentSubtype.PRODUTO_EXTRAVIADO.key,
            amount = BigDecimal("500.00"), description = "Carga de alto valor perdida",
            occurredAt = LocalDate.now()
        )

        val totals = BillingCycleCalculator.calculateCycleTotals(listOf(route), emptyList(), listOf(penalty))

        assertEquals(BigDecimal("150.00"), totals.grossRoutesAmount)
        assertEquals(BigDecimal("500.00"), totals.adjustmentsDebit)
        assertEquals(BigDecimal("-350.00"), totals.netTotalAmount)
    }

    @Test
    fun `test empty cycle produces zero amounts and counts`() {
        val totals = BillingCycleCalculator.calculateCycleTotals(emptyList(), emptyList(), emptyList())
        assertEquals(BigDecimal.ZERO, totals.grossRoutesAmount)
        assertEquals(BigDecimal.ZERO, totals.totalTipsAmount)
        assertEquals(BigDecimal.ZERO, totals.totalBonusAmount)
        assertEquals(BigDecimal.ZERO, totals.grossDailyAmount)
        assertEquals(BigDecimal.ZERO, totals.adjustmentsCredit)
        assertEquals(BigDecimal.ZERO, totals.adjustmentsDebit)
        assertEquals(BigDecimal.ZERO, totals.adjustmentsTotal)
        assertEquals(BigDecimal.ZERO, totals.netTotalAmount)
        assertEquals(0, totals.routesCount)
        assertEquals(0, totals.packagesCount)
        assertEquals(0, totals.dailyTotalsCount)
        assertEquals(0, totals.adjustmentsCount)
    }

    // =========================================================================
    // CRITÉRIO 4: Normalização e transição de status (normalizeBillingCycleStatus)
    // =========================================================================

    @Test
    fun `test normalizeBillingCycleStatus paid and cancelado states`() {
        val pastDate = LocalDate.of(2026, 4, 1)
        val refDate = LocalDate.of(2026, 5, 20)

        // 'paid' e 'pago' em maiúsculas, minúsculas e com espaços
        assertEquals("pago", normalizeBillingCycleStatus("paid", pastDate, refDate))
        assertEquals("pago", normalizeBillingCycleStatus("PAID", pastDate, refDate))
        assertEquals("pago", normalizeBillingCycleStatus("  paid  ", pastDate, refDate))
        assertEquals("pago", normalizeBillingCycleStatus("pago", pastDate, refDate))
        assertEquals("pago", normalizeBillingCycleStatus("PAGO", pastDate, refDate))

        // Fatura paga permanece 'pago' mesmo muito tempo após o corte
        assertEquals("pago", normalizeBillingCycleStatus("pago", LocalDate.of(2025, 1, 1), refDate))

        // Cancelado e atrasado
        assertEquals("cancelado", normalizeBillingCycleStatus("cancelado"))
        assertEquals("cancelado", normalizeBillingCycleStatus("CANCELADO"))
        assertEquals("atrasado", normalizeBillingCycleStatus("atrasado"))
        assertEquals("a_vencer", normalizeBillingCycleStatus("a_vencer"))
    }

    @Test
    fun `test normalizeBillingCycleStatus transitions open and pending based on cut date vs reference date`() {
        val cutDate = LocalDate.of(2026, 5, 15)

        // Caso 1: Data de referência anterior à data de corte (ciclo em andamento) -> 'em_aberto'
        val dateBeforeCut = LocalDate.of(2026, 5, 10)
        assertEquals("em_aberto", normalizeBillingCycleStatus("open", cutDate, dateBeforeCut))
        assertEquals("em_aberto", normalizeBillingCycleStatus("pending", cutDate, dateBeforeCut))
        assertEquals("em_aberto", normalizeBillingCycleStatus("em_aberto", cutDate, dateBeforeCut))
        assertEquals("em_aberto", normalizeBillingCycleStatus("OPEN", cutDate, dateBeforeCut))
        assertEquals("em_aberto", normalizeBillingCycleStatus(" PENDING ", cutDate, dateBeforeCut))

        // Caso 2: Data de referência exatamente no dia de corte -> 'em_aberto' (o dia do corte ainda está correndo)
        val dateOnCut = LocalDate.of(2026, 5, 15)
        assertEquals("em_aberto", normalizeBillingCycleStatus("open", cutDate, dateOnCut))
        assertEquals("em_aberto", normalizeBillingCycleStatus("pending", cutDate, dateOnCut))
        assertEquals("em_aberto", normalizeBillingCycleStatus("em_aberto", cutDate, dateOnCut))

        // Caso 3: Data de referência após a data de corte (corte finalizado, aguardando pagamento) -> 'a_vencer'
        val dateAfterCut = LocalDate.of(2026, 5, 16)
        assertEquals("a_vencer", normalizeBillingCycleStatus("open", cutDate, dateAfterCut))
        assertEquals("a_vencer", normalizeBillingCycleStatus("pending", cutDate, dateAfterCut))
        assertEquals("a_vencer", normalizeBillingCycleStatus("em_aberto", cutDate, dateAfterCut))
        assertEquals("a_vencer", normalizeBillingCycleStatus("OPEN", cutDate, dateAfterCut))
        assertEquals("a_vencer", normalizeBillingCycleStatus("PENDING", cutDate, dateAfterCut))

        // Caso 4: Vários dias após o corte
        val dateLongAfterCut = LocalDate.of(2026, 6, 1)
        assertEquals("a_vencer", normalizeBillingCycleStatus("open", cutDate, dateLongAfterCut))
    }

    @Test
    fun `test normalizeBillingCycleStatus default and null safety`() {
        val cutDate = LocalDate.of(2026, 5, 15)
        val dateAfterCut = LocalDate.of(2026, 5, 20)
        val dateBeforeCut = LocalDate.of(2026, 5, 10)

        // Raw status nulo com período final no passado vira 'a_vencer'
        assertEquals("a_vencer", normalizeBillingCycleStatus(null, cutDate, dateAfterCut))

        // Raw status nulo com período final no futuro permanece 'em_aberto'
        assertEquals("em_aberto", normalizeBillingCycleStatus(null, cutDate, dateBeforeCut))

        // Sem data de corte e sem status -> fallback para 'em_aberto'
        assertEquals("em_aberto", normalizeBillingCycleStatus(null, null))
    }

    @Test
    fun `test calculateCycleTotals with delivery partner sessions included`() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val route = Route(
            id = "r1", userId = "u1", platformId = "plat1", origin = null, destination = null,
            amount = BigDecimal("200.00"), packageCount = 30, occurredAt = now
        )
        val session1 = DeliveryPartnerSession(
            id = "sess-1", userId = "u1", partnerId = "part-1", platformId = "plat1",
            deliveredCount = 45, amountPaid = BigDecimal("150.00"), createdAt = now
        )
        val session2 = DeliveryPartnerSession(
            id = "sess-2", userId = "u1", partnerId = "part-2", platformId = "plat1",
            deliveredCount = 25, amountPaid = BigDecimal("85.50"), createdAt = now
        )

        val totals = BillingCycleCalculator.calculateCycleTotals(
            routes = listOf(route),
            dailyTotals = emptyList(),
            adjustments = emptyList(),
            sessions = listOf(session1, session2)
        )

        assertEquals(BigDecimal("200.00"), totals.grossRoutesAmount)
        assertEquals(BigDecimal("235.50"), totals.sessionAmount) // 150.00 + 85.50
        assertEquals(2, totals.sessionsCount)
        assertEquals(1, totals.routesCount)
        assertEquals(100, totals.packagesCount) // 30 (route) + 45 + 25 (sessions)
        assertEquals(BigDecimal("435.50"), totals.netTotalAmount) // 200.00 + 235.50
    }
}

