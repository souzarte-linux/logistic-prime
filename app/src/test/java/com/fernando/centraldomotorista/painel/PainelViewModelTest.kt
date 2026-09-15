package com.fernando.centraldomotorista.painel

import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.ui.common.charts.DailyTrendBucket
import com.fernando.centraldomotorista.ui.common.charts.TrendRange
import com.fernando.centraldomotorista.ui.screens.painel.PainelViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PainelViewModelTest {

    private val testZone = ZoneOffset.UTC
    private val testScope = CoroutineScope(Dispatchers.Default)

    // Data fixa para testes: 10 de Setembro de 2026 (Quinta-feira)
    private val fixedDate = LocalDate.of(2026, 9, 10)

    private fun createViewModel(): PainelViewModel {
        return PainelViewModel(
            externalScope = testScope,
            observeDataSync = false,
            loadOnInit = false,
            zone = testZone,
            fixedToday = fixedDate
        )
    }

    @Test
    fun testCalculateEarliestSince() {
        val vm = createViewModel()
        val earliest = vm.calculateEarliestSince(fixedDate)

        val monthStart = fixedDate.withDayOfMonth(1)
        val minus35 = fixedDate.minusDays(35)

        // Deve retornar o menor entre os dois
        val expected = if (minus35.isBefore(monthStart)) minus35 else monthStart
        assertEquals(expected, earliest)
        assertTrue(earliest.isBefore(monthStart) || earliest.isEqual(monthStart))
    }

    @Test
    fun testStatCardsWithZeroGoalsDoesNotCrashOrProduceNaN() {
        val vm = createViewModel()
        val profile = Profile(
            id = "user-1",
            fullName = "Motorista Teste",
            email = "motorista@teste.com",
            phone = null,
            plate = null,
            avatarUrl = null,
            dailyGoal = BigDecimal.ZERO,
            weeklyGoal = BigDecimal.ZERO,
            monthlyGoal = BigDecimal.ZERO,
            vehicleBrand = null,
            vehicleModel = null,
            vehicleYear = null,
            tankSizeL = null,
            avgConsumptionKml = null,
            oilChangeKm = null,
            tireSizeFront = null,
            tireSizeRear = null,
            lastOilChangeAt = null
        )

        // Rota de hoje: 10/09/2026, R$ 150 + R$ 20 gorjeta, 15 pacotes
        val todayRoute = Route(
            id = "r-1",
            userId = "user-1",
            platformId = "plat-1",
            origin = "Centro",
            destination = "Bairro",
            amount = BigDecimal("150.00"),
            tip = BigDecimal("20.00"),
            packageCount = 15,
            occurredAt = OffsetDateTime.of(2026, 9, 10, 14, 0, 0, 0, testZone)
        )

        vm.setTestData(
            profile = profile,
            routes = listOf(todayRoute)
        )

        val state = vm.uiState.value

        assertEquals(BigDecimal("170.00"), state.dailyEarnings)
        assertEquals(BigDecimal("170.00"), state.weeklyEarnings)
        assertEquals(BigDecimal("170.00"), state.monthlyEarnings)
        assertEquals(15, state.dailyPackages)

        // Metas zeradas: os percentuais devem ser nulos (sem divisão por zero, sem NaN/Infinity)
        assertNull(state.dailyProgressPct)
        assertNull(state.weeklyProgressPct)
        assertNull(state.monthlyProgressPct)
    }

    @Test
    fun testStatCardsWithConfiguredGoalsCalculatesProgressCorrectly() {
        val vm = createViewModel()
        val profile = Profile(
            id = "user-1",
            fullName = "Motorista Teste",
            email = "motorista@teste.com",
            phone = null,
            plate = null,
            avatarUrl = null,
            dailyGoal = BigDecimal("200.00"),
            weeklyGoal = BigDecimal("1000.00"),
            monthlyGoal = BigDecimal("4000.00"),
            vehicleBrand = null,
            vehicleModel = null,
            vehicleYear = null,
            tankSizeL = null,
            avgConsumptionKml = null,
            oilChangeKm = null,
            tireSizeFront = null,
            tireSizeRear = null,
            lastOilChangeAt = null
        )

        // Rota de hoje (10/09): R$ 100.00, 10 pacotes
        val r1 = Route(
            id = "r-1",
            userId = "user-1",
            platformId = "plat-1",
            origin = null,
            destination = null,
            amount = BigDecimal("100.00"),
            packageCount = 10,
            occurredAt = OffsetDateTime.of(2026, 9, 10, 10, 0, 0, 0, testZone)
        )

        // Rota de terça-feira desta semana (08/09): R$ 200.00, 20 pacotes
        val r2 = Route(
            id = "r-2",
            userId = "user-1",
            platformId = "plat-1",
            origin = null,
            destination = null,
            amount = BigDecimal("200.00"),
            packageCount = 20,
            occurredAt = OffsetDateTime.of(2026, 9, 8, 10, 0, 0, 0, testZone)
        )

        // Rota do início do mês (02/09): R$ 300.00, 30 pacotes
        val r3 = Route(
            id = "r-3",
            userId = "user-1",
            platformId = "plat-1",
            origin = null,
            destination = null,
            amount = BigDecimal("300.00"),
            packageCount = 30,
            occurredAt = OffsetDateTime.of(2026, 9, 2, 10, 0, 0, 0, testZone)
        )

        vm.setTestData(
            profile = profile,
            routes = listOf(r1, r2, r3)
        )

        val state = vm.uiState.value

        // Diário: 100.00 (hoje)
        assertEquals(BigDecimal("100.00"), state.dailyEarnings)
        assertEquals(10, state.dailyPackages)
        // 100 / 200 = 50%
        assertEquals(BigDecimal("50"), state.dailyProgressPct)

        // Semanal: r1 + r2 = 300.00
        assertEquals(BigDecimal("300.00"), state.weeklyEarnings)
        assertEquals(30, state.weeklyPackages)
        // 300 / 1000 = 30%
        assertEquals(BigDecimal("30"), state.weeklyProgressPct)

        // Mensal: r1 + r2 + r3 = 600.00
        assertEquals(BigDecimal("600.00"), state.monthlyEarnings)
        assertEquals(60, state.monthlyPackages)
        // 600 / 4000 = 15%
        assertEquals(BigDecimal("15"), state.monthlyProgressPct)
    }

    @Test
    fun testPlatformEarningsRankingAndPercentage() {
        val vm = createViewModel()
        val p1 = Platform(id = "p-1", userId = "user-1", name = "Mercado Livre", cycle = "semanal", paymentDay = null)
        val p2 = Platform(id = "p-2", userId = "user-1", name = "Shopee", cycle = "semanal", paymentDay = null)
        val p3 = Platform(id = "p-3", userId = "user-1", name = "Loggi", cycle = "semanal", paymentDay = null)

        val r1 = Route(
            id = "r-1",
            userId = "user-1",
            platformId = "p-1",
            origin = null,
            destination = null,
            amount = BigDecimal("600.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 5, 10, 0, 0, 0, testZone)
        )
        val r2 = Route(
            id = "r-2",
            userId = "user-1",
            platformId = "p-2",
            origin = null,
            destination = null,
            amount = BigDecimal("400.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 6, 10, 0, 0, 0, testZone)
        )

        vm.setTestData(
            platforms = listOf(p1, p2, p3),
            routes = listOf(r1, r2)
        )

        val state = vm.uiState.value
        assertEquals(3, state.platformEarnings.size)

        // Top 1: Mercado Livre (600.00)
        assertEquals("p-1", state.platformEarnings[0].id)
        assertEquals(BigDecimal("600.00"), state.platformEarnings[0].total)
        // 600 / 1000 = 60%
        assertEquals(60.0f, state.platformEarnings[0].percentageOfTotal, 0.1f)

        // Top 2: Shopee (400.00)
        assertEquals("p-2", state.platformEarnings[1].id)
        assertEquals(BigDecimal("400.00"), state.platformEarnings[1].total)
        // 400 / 1000 = 40%
        assertEquals(40.0f, state.platformEarnings[1].percentageOfTotal, 0.1f)

        // Top 3: Loggi (0.00)
        assertEquals("p-3", state.platformEarnings[2].id)
        assertEquals(BigDecimal.ZERO, state.platformEarnings[2].total)
        assertEquals(0.0f, state.platformEarnings[2].percentageOfTotal, 0.1f)
    }

    @Test
    fun testExpenseCategorySummaryAlwaysShowsFixedCategories() {
        val vm = createViewModel()

        // Despesa de combustível apenas
        val exp1 = Expense(
            id = "e-1",
            userId = "user-1",
            category = "combustivel",
            title = "Gasolina",
            amount = BigDecimal("120.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 4, 10, 0, 0, 0, testZone)
        )

        vm.setTestData(expenses = listOf(exp1))

        val state = vm.uiState.value
        assertEquals(4, state.expensesByCategory.size)

        val combustivel = state.expensesByCategory.first { it.category == "combustivel" }
        assertEquals(BigDecimal("120.00"), combustivel.total)

        val manutencao = state.expensesByCategory.first { it.category == "manutencao" }
        assertEquals(BigDecimal.ZERO, manutencao.total)

        val alimentacao = state.expensesByCategory.first { it.category == "alimentacao" }
        assertEquals(BigDecimal.ZERO, alimentacao.total)

        val equipe = state.expensesByCategory.first { it.category == "equipe" }
        assertEquals(BigDecimal.ZERO, equipe.total)
    }

    @Test
    fun testTrendBucketsForSevenDaysAndThirtyDays() {
        val vm = createViewModel()

        val r1 = Route(
            id = "r-1",
            userId = "user-1",
            platformId = null,
            origin = null,
            destination = null,
            amount = BigDecimal("250.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 10, 10, 0, 0, 0, testZone)
        )

        vm.setTestData(routes = listOf(r1))

        // 1. 7D
        val state7d = vm.uiState.value
        assertEquals(TrendRange.SEVEN_DAYS, state7d.trendRange)
        assertEquals(7, state7d.trendBuckets.size)
        // Semana começou na segunda-feira 07/09/2026
        assertEquals(LocalDate.of(2026, 9, 7), state7d.trendBuckets.first().date)
        assertEquals(DayOfWeek.MONDAY, state7d.trendBuckets.first().date.dayOfWeek)
        // Quinta-feira (10/09) deve ter 250.00
        val thursdayBucket = state7d.trendBuckets.first { it.date == fixedDate }
        assertEquals(BigDecimal("250.00"), thursdayBucket.totalAmount)
        assertTrue(thursdayBucket.isToday)
        assertEquals(BigDecimal("250.00"), state7d.maxTrendAmount)

        // 2. Troca para 30D
        vm.setTrendRange(TrendRange.THIRTY_DAYS)
        val state30d = vm.uiState.value
        assertEquals(TrendRange.THIRTY_DAYS, state30d.trendRange)
        assertEquals(30, state30d.trendBuckets.size)
        assertEquals(fixedDate, state30d.trendBuckets.last().date)
        assertEquals(fixedDate.minusDays(29), state30d.trendBuckets.first().date)
    }

    @Test
    fun testMaintenanceAlertFilteringThresholds() {
        val vm = createViewModel()

        // Odômetro atual do motorista: 10.000 KM (vindo de uma despesa com odometerKm = 10000)
        val expenseWithOdo = Expense(
            id = "e-odo",
            userId = "user-1",
            category = "combustivel",
            title = "Abastecimento",
            amount = BigDecimal("50.00"),
            odometerKm = BigDecimal("10000"),
            occurredAt = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, testZone)
        )

        // Peça 1: vida útil 1000 km, última troca em 9200 km -> rodou 800 km (80% desgaste) -> NÃO DEVE APARECER (< 90%)
        val partNormal = PartMaintenance(
            id = "part-1",
            userId = "user-1",
            partName = "Vela de Ignição",
            lifeKm = BigDecimal("1000"),
            lastChangeKm = BigDecimal("9200"),
            lastChangeAt = OffsetDateTime.of(2026, 8, 1, 0, 0, 0, 0, testZone)
        )

        // Peça 2: vida útil 1000 km, última troca em 9050 km -> rodou 950 km (95% desgaste) -> PREVENTIVO (>= 90% e < 100%)
        val partPreventive = PartMaintenance(
            id = "part-2",
            userId = "user-1",
            partName = "Pastilha de Freio",
            lifeKm = BigDecimal("1000"),
            lastChangeKm = BigDecimal("9050"),
            lastChangeAt = OffsetDateTime.of(2026, 8, 1, 0, 0, 0, 0, testZone)
        )

        // Peça 3: vida útil 1000 km, última troca em 8900 km -> rodou 1100 km (110% desgaste) -> CRÍTICO / ATRASADA (>= 100%)
        val partCritical = PartMaintenance(
            id = "part-3",
            userId = "user-1",
            partName = "Óleo do Motor",
            lifeKm = BigDecimal("1000"),
            lastChangeKm = BigDecimal("8900"),
            lastChangeAt = OffsetDateTime.of(2026, 8, 1, 0, 0, 0, 0, testZone)
        )

        vm.setTestData(
            expenses = listOf(expenseWithOdo),
            parts = listOf(partNormal, partPreventive, partCritical)
        )

        val state = vm.uiState.value
        assertEquals(2, state.maintenanceAlerts.size)

        // partNormal (80%) não aparece
        assertFalse(state.maintenanceAlerts.any { it.part.id == "part-1" })

        // partCritical (110%)
        val criticalItem = state.maintenanceAlerts.first { it.part.id == "part-3" }
        assertTrue(criticalItem.isOverdue)
        assertEquals(BigDecimal("110.0"), criticalItem.wearPercentage)
        assertEquals(BigDecimal("100"), criticalItem.overdueKm)

        // partPreventive (95%)
        val preventiveItem = state.maintenanceAlerts.first { it.part.id == "part-2" }
        assertFalse(preventiveItem.isOverdue)
        assertEquals(BigDecimal("95.0"), preventiveItem.wearPercentage)
        assertEquals(BigDecimal("50"), preventiveItem.remainingKm)
    }

    @Test
    fun testTeamExpensesGroupedByVendorAndAppearsInCategorySummary() {
        val vm = createViewModel()

        // 3 despesas de equipe no mês de setembro de 2026
        // Carlos: 2 despesas (300.00 + 200.00 = 500.00)
        // Marcos: 1 despesa (250.00)
        val exp1 = Expense(
            id = "e-1",
            userId = "user-1",
            category = "equipe",
            vendor = "Carlos Silva",
            title = "Diária de entrega",
            amount = BigDecimal("300.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 3, 10, 0, 0, 0, testZone)
        )
        val exp2 = Expense(
            id = "e-2",
            userId = "user-1",
            category = "equipe",
            vendor = "Carlos Silva",
            title = "Diária de entrega",
            amount = BigDecimal("200.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 5, 10, 0, 0, 0, testZone)
        )
        val exp3 = Expense(
            id = "e-3",
            userId = "user-1",
            category = "equipe",
            vendor = "Marcos Souza",
            title = "Diária de entrega",
            amount = BigDecimal("250.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 8, 10, 0, 0, 0, testZone)
        )

        vm.setTestData(expenses = listOf(exp1, exp2, exp3))

        val state = vm.uiState.value

        // 1. Categoria equipe no resumo geral
        val equipeCategory = state.expensesByCategory.first { it.category == "equipe" }
        assertEquals(BigDecimal("750.00"), equipeCategory.total)

        // 2. Detalhamento por parceiro
        assertEquals(2, state.teamExpensesByPartner.size)
        assertEquals(BigDecimal("750.00"), state.totalTeamExpenses)

        // 1º: Carlos Silva (500.00 / 750.00 ≈ 66.7%)
        val partner1 = state.teamExpensesByPartner[0]
        assertEquals("Carlos Silva", partner1.vendorName)
        assertEquals(BigDecimal("500.00"), partner1.total)
        assertEquals(66.7f, partner1.percentageOfTotal, 0.1f)

        // 2º: Marcos Souza (250.00 / 750.00 ≈ 33.3%)
        val partner2 = state.teamExpensesByPartner[1]
        assertEquals("Marcos Souza", partner2.vendorName)
        assertEquals(BigDecimal("250.00"), partner2.total)
        assertEquals(33.3f, partner2.percentageOfTotal, 0.1f)

        // 3. Consistência: a soma dos itens do card de parceiros é exatamente igual à linha equipe
        val sumPartnerCard = state.teamExpensesByPartner.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.total) }
        assertEquals(equipeCategory.total, sumPartnerCard)
    }

    @Test
    fun testTeamExpensesEmptyWhenNoPaymentsThisMonth() {
        val vm = createViewModel()

        // Nenhuma despesa de equipe no mês
        val expFuel = Expense(
            id = "e-fuel",
            userId = "user-1",
            category = "combustivel",
            vendor = "Posto Ipiranga",
            title = "Abastecimento",
            amount = BigDecimal("150.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 5, 10, 0, 0, 0, testZone)
        )

        vm.setTestData(expenses = listOf(expFuel))

        val state = vm.uiState.value

        // Linha "equipe" deve existir com R$ 0.00 no resumo geral
        val equipeCategory = state.expensesByCategory.first { it.category == "equipe" }
        assertEquals(BigDecimal.ZERO, equipeCategory.total)

        // Card de parceiros deve estar vazio
        assertTrue(state.teamExpensesByPartner.isEmpty())
        assertEquals(BigDecimal.ZERO, state.totalTeamExpenses)
    }

    @Test
    fun testTeamExpensesIgnoresBlankVendor() {
        val vm = createViewModel()

        val expNoVendor = Expense(
            id = "e-no-vendor",
            userId = "user-1",
            category = "equipe",
            vendor = "   ", // Em branco
            title = "Pagamento",
            amount = BigDecimal("180.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 7, 10, 0, 0, 0, testZone)
        )

        vm.setTestData(expenses = listOf(expNoVendor))

        val state = vm.uiState.value
        assertEquals(1, state.teamExpensesByPartner.size)
        assertEquals("Sem identificação", state.teamExpensesByPartner[0].vendorName)
        assertEquals(BigDecimal("180.00"), state.teamExpensesByPartner[0].total)
        assertEquals(100.0f, state.teamExpensesByPartner[0].percentageOfTotal, 0.1f)
    }
}
