package com.fernando.centraldomotorista.relatorios

import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.ui.common.period.PeriodPreset
import com.fernando.centraldomotorista.ui.screens.relatorios.CategoryMetric
import com.fernando.centraldomotorista.ui.screens.relatorios.MaintScheduleStatus
import com.fernando.centraldomotorista.ui.screens.relatorios.RelatoriosViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class RelatoriosViewModelTest {

    private val testZone = ZoneOffset.UTC
    private val testScope = CoroutineScope(Dispatchers.Default)

    // Data de referência fixada: 15 de Setembro de 2026 (Terça-feira)
    private val fixedToday = LocalDate.of(2026, 9, 15)

    private fun createViewModel(): RelatoriosViewModel {
        return RelatoriosViewModel(
            externalScope = testScope,
            observeDataSync = false,
            loadOnInit = false,
            zone = testZone,
            fixedToday = fixedToday
        )
    }

    @Test
    fun test17KpisCalculationWithZeroDataProducesNoCrashOrNaN() {
        val vm = createViewModel()
        vm.setTestData()

        val state = vm.uiState.value
        val stats = state.stats

        assertEquals(0, stats.totalRevenue.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.totalExpense.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.profit.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.totalKm.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.hours.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.averageDailyHours.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.revPerKm.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.costPerKm.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.profitPerKm.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.revPerHour.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.profitPerHour.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.routeCount)
        assertEquals(0, stats.totalPackages)
        assertEquals(0, stats.totalSmallPackages)
        assertEquals(0, stats.totalLargePackages)
        assertEquals(0, stats.smallPackagesValue.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.largePackagesValue.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.avgTicket.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.avgPackagePrice.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.realConsumptionKml.compareTo(BigDecimal.ZERO))
        assertEquals(0, stats.currentOdometer.compareTo(BigDecimal.ZERO))

        assertTrue(state.timelineBuckets.isNotEmpty()) // Buckets da semana gerados
        assertTrue(state.futureCashFlow.isEmpty())
        assertTrue(state.platformProfitability.isEmpty())
        assertTrue(state.bonificacoes.isEmpty())
    }

    @Test
    fun testRevenueProfitAndKmCalculations() {
        val vm = createViewModel()

        // Semana de 15/09/2026 (Segunda 14/09 a Domingo 20/09)
        val route1 = Route(
            id = "r-1",
            userId = "user-1",
            platformId = "plat-1",
            origin = "Centro",
            destination = "Bairro",
            amount = BigDecimal("200.00"),
            tip = BigDecimal("20.00"),
            distanceKm = BigDecimal("50.0"),
            occurredAt = OffsetDateTime.of(2026, 9, 14, 10, 0, 0, 0, testZone)
        )

        val daily1 = DailyTotal(
            id = "d-1",
            userId = "user-1",
            platformId = "plat-1",
            amount = BigDecimal("150.00"),
            distanceKm = BigDecimal("30.0"),
            occurredAt = OffsetDateTime.of(2026, 9, 15, 12, 0, 0, 0, testZone)
        )

        val expense1 = Expense(
            id = "e-1",
            userId = "user-1",
            category = "combustivel",
            title = "Gasolina Comum",
            amount = BigDecimal("100.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 15, 8, 0, 0, 0, testZone)
        )

        vm.setTestData(
            routes = listOf(route1),
            dailyTotals = listOf(daily1),
            expenses = listOf(expense1),
            preset = PeriodPreset.SEMANA
        )

        val stats = vm.uiState.value.stats

        // Receita: 200 + 20 + 150 = 370.00
        assertEquals(BigDecimal("370.00"), stats.totalRevenue)
        // Despesa: 100.00
        assertEquals(BigDecimal("100.00"), stats.totalExpense)
        // Lucro Líquido: 370 - 100 = 270.00
        assertEquals(BigDecimal("270.00"), stats.profit)
        // KM total: 50.0 + 30.0 = 80.0
        assertEquals(BigDecimal("80.0"), stats.totalKm)
        // Rev/KM: 370 / 80 = 4.63
        assertEquals(BigDecimal("4.63"), stats.revPerKm)
        // Custo/KM: 100 / 80 = 1.25
        assertEquals(BigDecimal("1.25"), stats.costPerKm)
        // Lucro/KM: 270 / 80 = 3.38
        assertEquals(BigDecimal("3.38"), stats.profitPerKm)
    }

    @Test
    fun testEffectiveHoursAndAverageDailyHours() {
        val vm = createViewModel()

        // Rota com 4 horas brutas (08:00 às 12:00) e 30 minutos de pausa -> 3.5h líquidas
        val route = Route(
            id = "r-hours",
            userId = "user-1",
            platformId = "plat-1",
            origin = "Centro",
            destination = "Bairro",
            amount = BigDecimal("100.00"),
            startedAt = OffsetDateTime.of(2026, 9, 15, 8, 0, 0, 0, testZone),
            endedAt = OffsetDateTime.of(2026, 9, 15, 12, 0, 0, 0, testZone),
            breakMinutes = 30,
            occurredAt = OffsetDateTime.of(2026, 9, 15, 12, 0, 0, 0, testZone)
        )

        vm.setTestData(
            routes = listOf(route),
            preset = PeriodPreset.DIA // Intervalo de 1 dia (hoje)
        )

        val stats = vm.uiState.value.stats
        assertEquals(BigDecimal("3.50"), stats.hours)
        assertEquals(BigDecimal("3.50"), stats.averageDailyHours)
        // Rev/Hora: 100 / 3.5 = 28.57
        assertEquals(BigDecimal("28.57"), stats.revPerHour)
        // Lucro/Hora: 100 / 3.5 = 28.57
        assertEquals(BigDecimal("28.57"), stats.profitPerHour)
    }

    @Test
    fun testPackageAndVolumeMetrics() {
        val vm = createViewModel()

        val routePackages = Route(
            id = "r-pkg",
            userId = "user-1",
            platformId = "plat-1",
            origin = "Centro",
            destination = "Bairro",
            productType = "pacote",
            smallPackagesCount = 40,
            packageUnitPrice = BigDecimal("4.50"),
            largePackagesCount = 2,
            largePackagesPrices = listOf(BigDecimal("15.00"), BigDecimal("20.00")),
            amount = BigDecimal("215.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 15, 14, 0, 0, 0, testZone)
        )

        vm.setTestData(
            routes = listOf(routePackages),
            preset = PeriodPreset.SEMANA
        )

        val stats = vm.uiState.value.stats
        assertEquals(40, stats.totalSmallPackages)
        assertEquals(2, stats.totalLargePackages)
        assertEquals(42, stats.totalPackages)
        // Valor pacotinhos: 40 * 4.50 = 180.00
        assertEquals(BigDecimal("180.00"), stats.smallPackagesValue)
        // Valor volumosos: 15 + 20 = 35.00
        assertEquals(BigDecimal("35.00"), stats.largePackagesValue)
        // Preço médio por pacote: 215 / 42 = 5.12
        assertEquals(BigDecimal("5.12"), stats.avgPackagePrice)
    }

    @Test
    fun testTankToTankRealFuelConsumption() {
        val vm = createViewModel()

        // Abastecimento 1: 50.000 km, 35 litros, tanque cheio
        val fill1 = Expense(
            id = "f-1",
            userId = "user-1",
            category = "combustivel",
            title = "Gasolina",
            amount = BigDecimal("200.00"),
            liters = BigDecimal("35.0"),
            odometerKm = BigDecimal("50000.0"),
            isFullTank = true,
            occurredAt = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, testZone)
        )

        // Abastecimento 2: 50.480 km, 40 litros, tanque cheio (480 km / 40 L = 12.00 km/L)
        val fill2 = Expense(
            id = "f-2",
            userId = "user-1",
            category = "combustivel",
            title = "Gasolina",
            amount = BigDecimal("240.00"),
            liters = BigDecimal("40.0"),
            odometerKm = BigDecimal("50480.0"),
            isFullTank = true,
            occurredAt = OffsetDateTime.of(2026, 9, 15, 9, 0, 0, 0, testZone)
        )

        vm.setTestData(
            expenses = listOf(fill1, fill2),
            preset = PeriodPreset.MES // Setembro inclui ambos
        )

        val stats = vm.uiState.value.stats
        assertEquals(BigDecimal("12.00"), stats.realConsumptionKml)
    }

    @Test
    fun testBonificacoesAndDescontosCalculationMirrorsOriginalPwa() {
        val vm = createViewModel()

        val platLoggi = Platform(id = "p-1", userId = "u1", name = "Loggi", cycle = "semanal", paymentDay = null, active = true)
        val platLalamove = Platform(id = "p-2", userId = "u1", name = "Lalamove", cycle = "semanal", paymentDay = null, active = true)
        val platInactive = Platform(id = "p-inact", userId = "u1", name = "Inativa", cycle = "semanal", paymentDay = null, active = false)

        val adjustments = listOf(
            FinancialAdjustment(id = "a1", userId = "u1", platformId = "p-1", billingCycleId = null, type = "previdenciario", amount = BigDecimal("-50.00"), description = null, occurredAt = LocalDate.of(2026, 9, 14)),
            FinancialAdjustment(id = "a2", userId = "u1", platformId = "p-1", billingCycleId = null, type = "pnr", amount = BigDecimal("-25.00"), description = null, occurredAt = LocalDate.of(2026, 9, 14)),
            FinancialAdjustment(id = "a3", userId = "u1", platformId = "p-1", billingCycleId = null, type = "bonus_fatura", amount = BigDecimal("100.00"), description = null, occurredAt = LocalDate.of(2026, 9, 15)),
            FinancialAdjustment(id = "a4", userId = "u1", platformId = "p-1", billingCycleId = null, type = "bonus", amount = BigDecimal("30.00"), description = null, occurredAt = LocalDate.of(2026, 9, 15)),
            // Ajuste em plataforma inativa (deve ser ignorado)
            FinancialAdjustment(id = "a5", userId = "u1", platformId = "p-inact", billingCycleId = null, type = "multa", amount = BigDecimal("-80.00"), description = null, occurredAt = LocalDate.of(2026, 9, 14))
        )

        vm.setTestData(
            platforms = listOf(platLoggi, platLalamove, platInactive),
            adjustments = adjustments,
            preset = PeriodPreset.SEMANA
        )

        val groups = vm.uiState.value.bonificacoes
        assertEquals(1, groups.size)

        val loggiGroup = groups.first()
        assertEquals("Loggi", loggiGroup.name)
        // Descontos: 50 + 25 = 75.00
        assertEquals(BigDecimal("75.00"), loggiGroup.descontosTotal)
        // Acréscimos: 100 + 30 = 130.00
        assertEquals(BigDecimal("130.00"), loggiGroup.acrescimosTotal)
        assertEquals(4, loggiGroup.details.size)
    }

    @Test
    fun testPreventiveMaintenanceScheduleStatus() {
        val vm = createViewModel()

        // Odômetro estimado: 50.000 km a partir de uma rota
        val route = Route(
            id = "r-odo",
            userId = "u1",
            platformId = "plat-1",
            origin = "Centro",
            destination = "Bairro",
            endKm = BigDecimal("50000"),
            occurredAt = OffsetDateTime.of(2026, 9, 15, 10, 0, 0, 0, testZone)
        )

        val partOk = PartMaintenance(
            id = "part-1",
            userId = "u1",
            partName = "Óleo do Motor",
            lifeKm = BigDecimal("5000"),
            lastChangeKm = BigDecimal("48000"), // Vence em 53.000 -> Restam 3.000 km (OK)
            lastChangeAt = OffsetDateTime.of(2026, 8, 1, 10, 0, 0, 0, testZone)
        )

        val partWarn = PartMaintenance(
            id = "part-2",
            userId = "u1",
            partName = "Pastilha de Freio",
            lifeKm = BigDecimal("10000"),
            lastChangeKm = BigDecimal("40300"), // Vence em 50.300 -> Restam 300 km (WARN)
            lastChangeAt = OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, testZone)
        )

        val partCritical = PartMaintenance(
            id = "part-3",
            userId = "u1",
            partName = "Vela de Ignição",
            lifeKm = BigDecimal("10000"),
            lastChangeKm = BigDecimal("39000"), // Vence em 49.000 -> Atrasado -1.000 km (CRITICAL)
            lastChangeAt = OffsetDateTime.of(2026, 5, 1, 10, 0, 0, 0, testZone)
        )

        vm.setTestData(
            routes = listOf(route),
            parts = listOf(partOk, partWarn, partCritical),
            preset = PeriodPreset.MES
        )

        val schedule = vm.uiState.value.maintSchedule
        assertEquals(3, schedule.size)

        assertEquals(MaintScheduleStatus.OK, schedule[0].status)
        assertEquals(BigDecimal("3000"), schedule[0].remainingKm)

        assertEquals(MaintScheduleStatus.WARN, schedule[1].status)
        assertEquals(BigDecimal("300"), schedule[1].remainingKm)

        assertEquals(MaintScheduleStatus.CRITICAL, schedule[2].status)
        assertEquals(BigDecimal("-1000"), schedule[2].remainingKm)
    }

    @Test
    fun testTimelineGranularityDailyVsMonthly() {
        val vm = createViewModel()

        // Presets: SEMANA deve gerar 7 buckets diários
        vm.applyPeriodPreset(PeriodPreset.SEMANA)
        assertEquals(7, vm.uiState.value.timelineBuckets.size)

        // Presets: ANO deve gerar 12 buckets mensais
        vm.applyPeriodPreset(PeriodPreset.ANO)
        assertEquals(12, vm.uiState.value.timelineBuckets.size)
        assertEquals("Jan", vm.uiState.value.timelineBuckets.first().label)
        assertEquals("Dez", vm.uiState.value.timelineBuckets.last().label)
    }
}
