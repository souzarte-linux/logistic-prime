package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.data.model.Expense
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerRepository
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerSessionRepository
import com.fernando.centraldomotorista.data.repository.DeliveryRouteRepository
import com.fernando.centraldomotorista.data.repository.ExpenseRepository
import com.fernando.centraldomotorista.ui.screens.deliverypartners.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PartnerRoutesViewModelTest {

    private val testZone = ZoneOffset.UTC
    private val testScope = CoroutineScope(Dispatchers.Default)
    private val vm = PartnerRoutesViewModel(externalScope = testScope, observeDataSync = false)

    @Test
    fun testPeriodFilterResolveRangePresets() {
        val today = LocalDate.of(2026, 9, 10) // Quinta-feira

        // 1. DIA
        val (diaStart, diaEnd) = PartnerPeriodFilter(preset = PartnerPeriodPreset.DIA).resolveRange(today)
        assertEquals(today, diaStart)
        assertEquals(today, diaEnd)

        // 2. SEMANA (Segunda a Domingo)
        val (semStart, semEnd) = PartnerPeriodFilter(preset = PartnerPeriodPreset.SEMANA).resolveRange(today)
        assertEquals(LocalDate.of(2026, 9, 7), semStart)
        assertEquals(DayOfWeek.MONDAY, semStart.dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 13), semEnd)
        assertEquals(DayOfWeek.SUNDAY, semEnd.dayOfWeek)

        // 3. QUINZENA (14 dias atrás até hoje)
        val (quinzStart, quinzEnd) = PartnerPeriodFilter(preset = PartnerPeriodPreset.QUINZENA).resolveRange(today)
        assertEquals(today.minusDays(14), quinzStart)
        assertEquals(today, quinzEnd)

        // 4. MES (1 a último dia do mês)
        val (mesStart, mesEnd) = PartnerPeriodFilter(preset = PartnerPeriodPreset.MES).resolveRange(today)
        assertEquals(LocalDate.of(2026, 9, 1), mesStart)
        assertEquals(LocalDate.of(2026, 9, 30), mesEnd)

        // 5. ANO (1 de jan a 31 de dez)
        val (anoStart, anoEnd) = PartnerPeriodFilter(preset = PartnerPeriodPreset.ANO).resolveRange(today)
        assertEquals(LocalDate.of(2026, 1, 1), anoStart)
        assertEquals(LocalDate.of(2026, 12, 31), anoEnd)

        // 6. PERSONALIZADO
        val customS = LocalDate.of(2026, 8, 15)
        val customE = LocalDate.of(2026, 8, 25)
        val (customStart, customEnd) = PartnerPeriodFilter(
            preset = PartnerPeriodPreset.PERSONALIZADO,
            customStart = customS,
            customEnd = customE
        ).resolveRange(today)
        assertEquals(customS, customStart)
        assertEquals(customE, customEnd)
    }

    @Test
    fun testBuildHierarchyMultilevelAndBigDecimalCalculations() {
        val today = LocalDate.of(2026, 9, 10)

        val s1 = DeliveryPartnerSession(
            id = "s-1",
            partnerId = "p-1",
            expectedPackageCount = 50,
            deliveredCount = 48,
            returnedCount = 2,
            startTime = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, testZone),
            endTime = OffsetDateTime.of(2026, 9, 10, 16, 0, 0, 0, testZone),
            amountPaid = BigDecimal("120.50")
        )

        val s2 = DeliveryPartnerSession(
            id = "s-2",
            partnerId = "p-1",
            expectedPackageCount = 60,
            deliveredCount = 55,
            returnedCount = 5,
            startTime = OffsetDateTime.of(2026, 9, 10, 17, 0, 0, 0, testZone),
            endTime = OffsetDateTime.of(2026, 9, 10, 21, 0, 0, 0, testZone),
            amountPaid = BigDecimal("137.25")
        )

        val s3 = DeliveryPartnerSession(
            id = "s-3",
            partnerId = "p-1",
            expectedPackageCount = 40,
            deliveredCount = 40,
            returnedCount = 0,
            startTime = OffsetDateTime.of(2026, 9, 8, 9, 0, 0, 0, testZone),
            endTime = OffsetDateTime.of(2026, 9, 8, 15, 0, 0, 0, testZone),
            amountPaid = BigDecimal("95.00")
        )

        // Sessão do mês anterior (Agosto)
        val s4 = DeliveryPartnerSession(
            id = "s-4",
            partnerId = "p-1",
            expectedPackageCount = 70,
            deliveredCount = 68,
            returnedCount = 2,
            startTime = OffsetDateTime.of(2026, 8, 20, 8, 0, 0, 0, testZone),
            endTime = OffsetDateTime.of(2026, 8, 20, 16, 0, 0, 0, testZone),
            amountPaid = BigDecimal("150.00")
        )

        val allSessions = listOf(s1, s2, s3, s4)

        // Filtro: ANO (cobre agosto e setembro)
        val filterAno = PartnerPeriodFilter(preset = PartnerPeriodPreset.ANO)
        val (monthGroups, totalDelivered, totalAmountPaid) = PartnerRoutesViewModel.buildHierarchy(
            sessions = allSessions,
            filter = filterAno,
            today = today,
            zone = testZone
        )

        assertEquals(2, monthGroups.size)
        // Mais recente primeiro (Setembro antes de Agosto)
        assertEquals("2026-09", monthGroups[0].monthKey)
        assertEquals("2026-08", monthGroups[1].monthKey)

        // Total consolidado geral
        assertEquals(48 + 55 + 40 + 68, totalDelivered)
        assertEquals(BigDecimal("502.75"), totalAmountPaid)

        // Verificação do Mês de Setembro
        val monthSep = monthGroups[0]
        assertTrue(monthSep.isCurrentMonth)
        assertEquals(48 + 55 + 40, monthSep.totalDelivered)
        assertEquals(BigDecimal("352.75"), monthSep.totalAmountPaid)

        // Verificação das semanas do Mês de Setembro
        assertEquals(1, monthSep.weeks.size)
        val weekSep = monthSep.weeks[0]
        assertTrue(weekSep.isCurrentWeek)
        assertEquals(monthSep.totalDelivered, weekSep.totalDelivered)
        assertEquals(monthSep.totalAmountPaid, weekSep.totalAmountPaid)

        // Verificação dos dias de Setembro (10 de Set e 08 de Set)
        assertEquals(2, weekSep.days.size)
        val day10 = weekSep.days[0]
        assertEquals(LocalDate.of(2026, 9, 10), day10.date)
        assertEquals(48 + 55, day10.totalDelivered)
        assertEquals(BigDecimal("257.75"), day10.totalAmountPaid)
        assertEquals(2, day10.sessions.size)

        val day8 = weekSep.days[1]
        assertEquals(LocalDate.of(2026, 9, 8), day8.date)
        assertEquals(40, day8.totalDelivered)
        assertEquals(BigDecimal("95.00"), day8.totalAmountPaid)
        assertEquals(1, day8.sessions.size)
    }

    @Test
    fun testPeriodFilterExcludesOutOfRangeSessions() {
        val today = LocalDate.of(2026, 9, 10)

        val sInWeek = DeliveryPartnerSession(
            id = "s-in",
            partnerId = "p-1",
            deliveredCount = 30,
            startTime = OffsetDateTime.of(2026, 9, 9, 10, 0, 0, 0, testZone),
            amountPaid = BigDecimal("75.00")
        )

        val sOutOfWeek = DeliveryPartnerSession(
            id = "s-out",
            partnerId = "p-1",
            deliveredCount = 50,
            startTime = OffsetDateTime.of(2026, 9, 2, 10, 0, 0, 0, testZone),
            amountPaid = BigDecimal("120.00")
        )

        // Filtro SEMANA (07/09 a 13/09): s-in (09/09) entra, s-out (02/09) sai
        val (monthGroups, totalDelivered, totalAmountPaid) = PartnerRoutesViewModel.buildHierarchy(
            sessions = listOf(sInWeek, sOutOfWeek),
            filter = PartnerPeriodFilter(preset = PartnerPeriodPreset.SEMANA),
            today = today,
            zone = testZone
        )

        assertEquals(1, monthGroups.size)
        assertEquals(30, totalDelivered)
        assertEquals(BigDecimal("75.00"), totalAmountPaid)
        assertEquals(1, monthGroups[0].weeks[0].days[0].sessions.size)
        assertEquals("s-in", monthGroups[0].weeks[0].days[0].sessions[0].id)
    }

    @Test
    fun testCustomPeriodValidation() {
        val today = LocalDate.now()
        // Data final anterior à inicial
        vm.applyCustomPeriod(start = today, end = today.minusDays(1))
        assertEquals("Data final não pode ser anterior à data inicial.", vm.uiState.value.error)

        // Data final no futuro
        vm.clearMessages()
        vm.applyCustomPeriod(start = today.minusDays(5), end = today.plusDays(1))
        assertEquals("Data final não pode ser posterior a hoje.", vm.uiState.value.error)
    }

    @Test
    fun testToggleMonthAndToggleWeek() {
        assertFalse(vm.uiState.value.expandedMonths.contains("2026-09"))
        vm.toggleMonth("2026-09")
        assertTrue(vm.uiState.value.expandedMonths.contains("2026-09"))
        vm.toggleMonth("2026-09")
        assertFalse(vm.uiState.value.expandedMonths.contains("2026-09"))

        assertFalse(vm.uiState.value.expandedWeeks.contains("2026-09-2026-09-07"))
        vm.toggleWeek("2026-09-2026-09-07")
        assertTrue(vm.uiState.value.expandedWeeks.contains("2026-09-2026-09-07"))
        vm.toggleWeek("2026-09-2026-09-07")
        assertFalse(vm.uiState.value.expandedWeeks.contains("2026-09-2026-09-07"))
    }

    @Test
    fun testOpenEditSessionClosesViewDetailSession() {
        val session = DeliveryPartnerSession(id = "s-detail", partnerId = "p-1")
        vm.openViewDetailSession(session)
        assertEquals(session, vm.uiState.value.viewDetailSession)
        assertNull(vm.uiState.value.editingSession)

        // Ao abrir a edição, editingSession é preenchido e viewDetailSession deve ser fechado
        vm.openEditSession(session)
        assertEquals(session, vm.uiState.value.editingSession)
        assertNull(vm.uiState.value.viewDetailSession)
    }

    @Test
    fun testSaveEditedSessionUpdatesLinkedExpenseWhenAmountChanges() {
        val fakeSessionRepo = FakeSessionRepository()
        val initialExpense = Expense(
            id = "exp-100",
            userId = "user-1",
            title = "Diária Equipe",
            description = "Diária Equipe",
            category = "equipe",
            amount = BigDecimal("150.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, testZone)
        )
        val fakeExpenseRepo = FakeExpenseRepository(initialExpense)

        val testVm = PartnerRoutesViewModel(
            sessionRepository = fakeSessionRepo,
            expenseRepository = fakeExpenseRepo,
            externalScope = CoroutineScope(Dispatchers.Unconfined),
            observeDataSync = false
        )

        val original = DeliveryPartnerSession(
            id = "s-1",
            partnerId = "p-1",
            expectedPackageCount = 50,
            deliveredCount = 48,
            amountPaid = BigDecimal("150.00"),
            expenseId = "exp-100"
        )

        testVm.openEditSession(original)

        val updated = original.copy(
            amountPaid = BigDecimal("220.00"),
            deliveredCount = 50
        )

        testVm.saveEditedSession(updated)

        assertEquals(updated, fakeSessionRepo.lastSavedSession)
        assertEquals(1, fakeExpenseRepo.getExpenseByIdCalls)
        assertEquals(1, fakeExpenseRepo.updateExpenseCalls)
        assertEquals(BigDecimal("220.00"), fakeExpenseRepo.currentExpense?.amount)
        assertNull(testVm.uiState.value.editingSession)
        assertEquals("Sessão atualizada com sucesso!", testVm.uiState.value.message)
    }

    @Test
    fun testSaveEditedSessionDoesNotCallExpenseUpdateWhenAmountUnchanged() {
        val fakeSessionRepo = FakeSessionRepository()
        val initialExpense = Expense(
            id = "exp-100",
            userId = "user-1",
            title = "Diária Equipe",
            description = "Diária Equipe",
            category = "equipe",
            amount = BigDecimal("150.00"),
            occurredAt = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, testZone)
        )
        val fakeExpenseRepo = FakeExpenseRepository(initialExpense)

        val testVm = PartnerRoutesViewModel(
            sessionRepository = fakeSessionRepo,
            expenseRepository = fakeExpenseRepo,
            externalScope = CoroutineScope(Dispatchers.Unconfined),
            observeDataSync = false
        )

        val original = DeliveryPartnerSession(
            id = "s-1",
            partnerId = "p-1",
            expectedPackageCount = 50,
            deliveredCount = 48,
            amountPaid = BigDecimal("150.00"),
            expenseId = "exp-100"
        )

        testVm.openEditSession(original)

        // Alterou apenas entregues e rota, mantendo amountPaid idêntico
        val updated = original.copy(
            routeId = "route-abc",
            deliveredCount = 50,
            returnedCount = 0
        )

        testVm.saveEditedSession(updated)

        assertEquals(updated, fakeSessionRepo.lastSavedSession)
        assertEquals(0, fakeExpenseRepo.getExpenseByIdCalls)
        assertEquals(0, fakeExpenseRepo.updateExpenseCalls)
        assertEquals(BigDecimal("150.00"), fakeExpenseRepo.currentExpense?.amount)
        assertNull(testVm.uiState.value.editingSession)
    }

    @Test
    fun testSaveEditedSessionWithoutExpenseIdDoesNotCrashOrQueryExpense() {
        val fakeSessionRepo = FakeSessionRepository()
        val fakeExpenseRepo = FakeExpenseRepository(null)

        val testVm = PartnerRoutesViewModel(
            sessionRepository = fakeSessionRepo,
            expenseRepository = fakeExpenseRepo,
            externalScope = CoroutineScope(Dispatchers.Unconfined),
            observeDataSync = false
        )

        val original = DeliveryPartnerSession(
            id = "s-open",
            partnerId = "p-1",
            amountPaid = BigDecimal.ZERO,
            expenseId = null
        )

        testVm.openEditSession(original)

        val updated = original.copy(
            amountPaid = BigDecimal("100.00")
        )

        testVm.saveEditedSession(updated)

        assertEquals(updated, fakeSessionRepo.lastSavedSession)
        assertEquals(0, fakeExpenseRepo.getExpenseByIdCalls)
        assertEquals(0, fakeExpenseRepo.updateExpenseCalls)
        assertNull(testVm.uiState.value.editingSession)
    }

    @Test
    fun testSessionScannedCountMatchesScannedBarcodesListSize() {
        val barcodesList = mutableListOf("BR123456", "BR123457")
        val session = DeliveryPartnerSession(
            id = "s-barcode",
            partnerId = "p-1",
            scannedBarcodes = barcodesList,
            scannedCount = barcodesList.size
        )
        assertEquals(2, session.scannedCount)

        // Adicionando um código manualmente
        barcodesList.add("BR123458")
        val updatedWithAdd = session.copy(
            scannedBarcodes = barcodesList.toList(),
            scannedCount = barcodesList.size
        )
        assertEquals(3, updatedWithAdd.scannedCount)
        assertEquals(3, updatedWithAdd.scannedBarcodes.size)

        // Removendo um código
        barcodesList.removeAt(0)
        val updatedWithRemove = updatedWithAdd.copy(
            scannedBarcodes = barcodesList.toList(),
            scannedCount = barcodesList.size
        )
        assertEquals(2, updatedWithRemove.scannedCount)
        assertEquals(listOf("BR123457", "BR123458"), updatedWithRemove.scannedBarcodes)
    }

    @Test
    fun testFormatDurationRecalculatesDurationCorrectly() {
        val start = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, testZone)
        val end8h30 = OffsetDateTime.of(2026, 9, 10, 16, 30, 0, 0, testZone)
        val end1h15 = OffsetDateTime.of(2026, 9, 10, 9, 15, 0, 0, testZone)
        val endSame = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, testZone)

        assertEquals("08:30", formatDuration(start, end8h30))
        assertEquals("01:15", formatDuration(start, end1h15))
        assertEquals("00:00", formatDuration(start, endSame))
        assertEquals("--:--", formatDuration(start, null))
        assertEquals("--:--", formatDuration(null, end8h30))
        assertEquals("--:--", formatDuration(null, null))
    }

    @Test
    fun testPerformanceTabAggregatesEarningsPackagesAndReturnRate() {
        val today = LocalDate.of(2026, 9, 10)
        val s1 = DeliveryPartnerSession(
            id = "s-1",
            partnerId = "p-1",
            expectedPackageCount = 50,
            deliveredCount = 45,
            returnedCount = 5,
            amountPaid = BigDecimal("100.00"),
            startTime = OffsetDateTime.of(2026, 9, 8, 8, 0, 0, 0, testZone)
        )
        val s2 = DeliveryPartnerSession(
            id = "s-2",
            partnerId = "p-1",
            expectedPackageCount = 50,
            deliveredCount = 45,
            returnedCount = 5,
            amountPaid = BigDecimal("150.00"),
            startTime = OffsetDateTime.of(2026, 9, 9, 8, 0, 0, 0, testZone)
        )

        val metrics = calculatePartnerPerformance(listOf(s1, s2), today = today, zone = testZone)

        assertEquals(BigDecimal("250.00"), metrics.totalEarnings)
        assertEquals(BigDecimal("250.00"), metrics.currentMonthEarnings)
        assertEquals(90, metrics.totalDelivered)
        assertEquals(10, metrics.totalReturned)
        // 10 / (90 + 10) * 100 = 10.00%
        assertEquals(BigDecimal("10.00"), metrics.returnRate)
        assertEquals(2, metrics.totalSessions)
    }

    @Test
    fun testBestPerformanceDayPicksHighestEarningDay() {
        val today = LocalDate.of(2026, 9, 10)
        // Dia 1: mais pacotes (100 entregues), mas menor valor (120.00)
        val sDay1 = DeliveryPartnerSession(
            id = "s-day1",
            partnerId = "p-1",
            deliveredCount = 100,
            returnedCount = 0,
            amountPaid = BigDecimal("120.00"),
            startTime = OffsetDateTime.of(2026, 9, 8, 8, 0, 0, 0, testZone)
        )
        // Dia 2: menos pacotes (40 entregues), mas maior valor (200.00)
        val sDay2 = DeliveryPartnerSession(
            id = "s-day2",
            partnerId = "p-1",
            deliveredCount = 40,
            returnedCount = 2,
            amountPaid = BigDecimal("200.00"),
            startTime = OffsetDateTime.of(2026, 9, 9, 8, 0, 0, 0, testZone)
        )

        val metrics = calculatePartnerPerformance(listOf(sDay1, sDay2), today = today, zone = testZone)

        assertNotNull(metrics.bestDay)
        val best = metrics.bestDay!!
        assertEquals(LocalDate.of(2026, 9, 9), best.date)
        assertEquals(BigDecimal("200.00"), best.totalEarnings)
        assertEquals(40, best.deliveredCount)
        assertEquals(2, best.returnedCount)
        assertEquals(1, best.sessionCount)
    }

    @Test
    fun testPerformanceTabHandlesPartnerWithNoSessions() {
        val metrics = calculatePartnerPerformance(emptyList())

        assertEquals(BigDecimal.ZERO, metrics.totalEarnings)
        assertEquals(BigDecimal.ZERO, metrics.currentMonthEarnings)
        assertEquals(0, metrics.totalDelivered)
        assertEquals(0, metrics.totalReturned)
        assertEquals(BigDecimal.ZERO, metrics.returnRate)
        assertEquals(BigDecimal.ZERO, metrics.averageEarningsPerSession)
        assertNull(metrics.bestDay)
        assertEquals(0, metrics.totalSessions)
    }

    @Test
    fun testCalculatePartnerPerformanceIgnoresActivePeriodFilterAndUsesAllSessions() {
        val today = LocalDate.of(2026, 9, 10)
        val sAug = DeliveryPartnerSession(
            id = "s-aug",
            partnerId = "p-1",
            deliveredCount = 30,
            returnedCount = 2,
            amountPaid = BigDecimal("100.00"),
            startTime = OffsetDateTime.of(2026, 8, 15, 8, 0, 0, 0, testZone)
        )
        val sSep = DeliveryPartnerSession(
            id = "s-sep",
            partnerId = "p-1",
            deliveredCount = 50,
            returnedCount = 3,
            amountPaid = BigDecimal("150.00"),
            startTime = OffsetDateTime.of(2026, 9, 5, 8, 0, 0, 0, testZone)
        )

        val allSessions = listOf(sAug, sSep)
        val metrics = calculatePartnerPerformance(allSessions, today = today, zone = testZone)

        // Deve somar ambas as sessões (agosto + setembro), totalizando 250.00
        assertEquals(BigDecimal("250.00"), metrics.totalEarnings)
        // Mês corrente (setembro) deve somar apenas sSep
        assertEquals(BigDecimal("150.00"), metrics.currentMonthEarnings)
        assertEquals(80, metrics.totalDelivered)
        assertEquals(5, metrics.totalReturned)
        assertEquals(2, metrics.totalSessions)
    }

    @Test
    fun testAverageEarningsPerSessionIsZeroWhenNoSessions() {
        val emptyMetrics = calculatePartnerPerformance(emptyList())
        assertEquals(BigDecimal.ZERO, emptyMetrics.averageEarningsPerSession)

        val s1 = DeliveryPartnerSession(
            id = "s-1",
            partnerId = "p-1",
            amountPaid = BigDecimal("150.00")
        )
        val s2 = DeliveryPartnerSession(
            id = "s-2",
            partnerId = "p-1",
            amountPaid = BigDecimal("200.00")
        )

        val metrics = calculatePartnerPerformance(listOf(s1, s2))
        // (150.00 + 200.00) / 2 = 175.00
        assertEquals(BigDecimal("175.00"), metrics.averageEarningsPerSession)
    }

    @Test
    fun testCostPerPackageCalculationWithAndWithoutDeliveries() {
        // Com entregas zero -> retorna ZERO
        val zeroResult = calculateCostPerPackage(BigDecimal("150.00"), 0)
        assertEquals(BigDecimal.ZERO, zeroResult)

        // Com entregas > 0 -> divisão com RoundingMode.HALF_UP
        val normalResult = calculateCostPerPackage(BigDecimal("150.00"), 30)
        assertEquals(BigDecimal("5.00"), normalResult)

        val fractionalResult = calculateCostPerPackage(BigDecimal("100.00"), 3)
        assertEquals(BigDecimal("33.33"), fractionalResult)
    }

    @Test
    fun testOthersAverageCostPerPackageExcludesCurrentPartnerAndZeroDeliveryPartners() {
        val sCurrent = DeliveryPartnerSession(
            id = "s-curr",
            partnerId = "p-current",
            amountPaid = BigDecimal("300.00"),
            deliveredCount = 50
        )
        val sB = DeliveryPartnerSession(
            id = "s-b",
            partnerId = "p-b",
            amountPaid = BigDecimal("50.00"),
            deliveredCount = 0
        )
        val sC = DeliveryPartnerSession(
            id = "s-c",
            partnerId = "p-c",
            amountPaid = BigDecimal("100.00"),
            deliveredCount = 20
        )
        val sD = DeliveryPartnerSession(
            id = "s-d",
            partnerId = "p-d",
            amountPaid = BigDecimal("70.00"),
            deliveredCount = 10
        )

        val allSessions = listOf(sCurrent, sB, sC, sD)
        val avg = calculateOthersAverageCostPerPackage(allSessions, excludingPartnerId = "p-current")

        assertNotNull(avg)
        // Média de C (5.00) e D (7.00) = (5.00 + 7.00) / 2 = 6.00
        assertEquals(BigDecimal("6.00"), avg)
    }

    @Test
    fun testOthersAverageCostPerPackageReturnsNullWhenNoOtherActivePartner() {
        val sCurrent = DeliveryPartnerSession(
            id = "s-curr",
            partnerId = "p-current",
            amountPaid = BigDecimal("200.00"),
            deliveredCount = 40
        )
        val resultSolo = calculateOthersAverageCostPerPackage(listOf(sCurrent), excludingPartnerId = "p-current")
        assertNull(resultSolo)

        val sZero = DeliveryPartnerSession(
            id = "s-zero",
            partnerId = "p-other",
            amountPaid = BigDecimal("50.00"),
            deliveredCount = 0
        )
        val resultZeroOnly = calculateOthersAverageCostPerPackage(listOf(sCurrent, sZero), excludingPartnerId = "p-current")
        assertNull(resultZeroOnly)
    }

    @Test
    fun testRegularityCountsDistinctDaysNotSessions() {
        val today = LocalDate.of(2026, 9, 10)
        val s1 = DeliveryPartnerSession(
            id = "s-1",
            partnerId = "p-1",
            startTime = OffsetDateTime.of(2026, 9, 2, 8, 0, 0, 0, ZoneOffset.UTC)
        )
        val s2 = DeliveryPartnerSession(
            id = "s-2",
            partnerId = "p-1",
            startTime = OffsetDateTime.of(2026, 9, 2, 14, 0, 0, 0, ZoneOffset.UTC)
        )
        val s3 = DeliveryPartnerSession(
            id = "s-3",
            partnerId = "p-1",
            startTime = OffsetDateTime.of(2026, 9, 5, 9, 0, 0, 0, ZoneOffset.UTC)
        )
        val sOld = DeliveryPartnerSession(
            id = "s-old",
            partnerId = "p-1",
            startTime = OffsetDateTime.of(2026, 8, 30, 9, 0, 0, 0, ZoneOffset.UTC)
        )

        val regularity = calculateRegularity(listOf(s1, s2, s3, sOld), today = today, zone = ZoneOffset.UTC)

        assertEquals(2, regularity.daysWorkedThisMonth)
        assertEquals(10, regularity.daysElapsedThisMonth)
        assertEquals(BigDecimal("20.0"), regularity.regularityPercent)
    }

    @Test
    fun testRegularityHandlesFirstDayOfMonth() {
        val today = LocalDate.of(2026, 9, 1)

        val s1 = DeliveryPartnerSession(
            id = "s-1",
            partnerId = "p-1",
            startTime = OffsetDateTime.of(2026, 9, 1, 8, 0, 0, 0, ZoneOffset.UTC)
        )
        val regWorked = calculateRegularity(listOf(s1), today = today, zone = ZoneOffset.UTC)
        assertEquals(1, regWorked.daysWorkedThisMonth)
        assertEquals(1, regWorked.daysElapsedThisMonth)
        assertEquals(BigDecimal("100.0"), regWorked.regularityPercent)

        val regEmpty = calculateRegularity(emptyList(), today = today, zone = ZoneOffset.UTC)
        assertEquals(0, regEmpty.daysWorkedThisMonth)
        assertEquals(1, regEmpty.daysElapsedThisMonth)
        assertEquals(BigDecimal("0.0"), regEmpty.regularityPercent)
    }

    @Test
    fun testLoadPartnerInsightsDoesNotRefetchIfAlreadyLoaded() {
        val fakeSessionRepo = FakeSessionRepository()
        val fakePartnerRepo = FakePartnerRepository()
        val fakeRouteRepo = FakeRouteRepository()
        val partnerA = DeliveryPartner(id = "p-a", fullName = "Parceiro A")
        fakePartnerRepo.partnersToReturn = listOf(partnerA)

        val sessionA = DeliveryPartnerSession(
            id = "s-a",
            partnerId = "p-a",
            amountPaid = BigDecimal("100.00"),
            deliveredCount = 20,
            startTime = OffsetDateTime.now(ZoneOffset.UTC)
        )
        fakeSessionRepo.partnerSessionsToReturn = listOf(sessionA)
        fakeSessionRepo.sessionsToReturn = listOf(sessionA)

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val testVm = PartnerRoutesViewModel(
            partnerRepository = fakePartnerRepo,
            routeRepository = fakeRouteRepo,
            sessionRepository = fakeSessionRepo,
            expenseRepository = FakeExpenseRepository(),
            externalScope = testScope,
            observeDataSync = false,
            currentUserIdOverride = "test-user"
        )

        testVm.loadData("p-a")
        assertEquals(0, fakeSessionRepo.getSessionsCalls)

        // 1ª chamada -> busca da rede
        testVm.loadPartnerInsights()
        assertEquals(1, fakeSessionRepo.getSessionsCalls)
        assertNotNull(testVm.uiState.value.costEfficiency)

        // 2ª chamada -> guarda evita nova busca
        testVm.loadPartnerInsights()
        assertEquals(1, fakeSessionRepo.getSessionsCalls)
    }

    @Test
    fun testLoadDataResetsInsightsWhenSwitchingPartner() {
        val fakeSessionRepo = FakeSessionRepository()
        val fakePartnerRepo = FakePartnerRepository()
        val fakeRouteRepo = FakeRouteRepository()
        val partnerA = DeliveryPartner(id = "p-a", fullName = "Parceiro A")
        val partnerB = DeliveryPartner(id = "p-b", fullName = "Parceiro B")
        fakePartnerRepo.partnersToReturn = listOf(partnerA, partnerB)

        val sessionA = DeliveryPartnerSession(
            id = "s-a",
            partnerId = "p-a",
            amountPaid = BigDecimal("100.00"),
            deliveredCount = 20,
            startTime = OffsetDateTime.now(ZoneOffset.UTC)
        )
        fakeSessionRepo.partnerSessionsToReturn = listOf(sessionA)
        fakeSessionRepo.sessionsToReturn = listOf(sessionA)

        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val testVm = PartnerRoutesViewModel(
            partnerRepository = fakePartnerRepo,
            routeRepository = fakeRouteRepo,
            sessionRepository = fakeSessionRepo,
            expenseRepository = FakeExpenseRepository(),
            externalScope = testScope,
            observeDataSync = false,
            currentUserIdOverride = "test-user"
        )

        testVm.loadData("p-a")
        testVm.loadPartnerInsights()
        assertNotNull(testVm.uiState.value.costEfficiency)
        assertNotNull(testVm.uiState.value.regularity)

        // Ao trocar de parceiro, os insights devem ser resetados para null
        fakeSessionRepo.partnerSessionsToReturn = emptyList()
        testVm.loadData("p-b")
        assertNull(testVm.uiState.value.costEfficiency)
        assertNull(testVm.uiState.value.regularity)
    }
}

private class FakePartnerRepository : DeliveryPartnerRepository() {
    var partnersToReturn: List<DeliveryPartner> = emptyList()
    override suspend fun getDeliveryPartners(userId: String): List<DeliveryPartner> = partnersToReturn
}

private class FakeRouteRepository : DeliveryRouteRepository() {
    var routesToReturn: List<DeliveryRoute> = emptyList()
    override suspend fun getDeliveryRoutes(userId: String): List<DeliveryRoute> = routesToReturn
}

private class FakeSessionRepository : DeliveryPartnerSessionRepository() {
    var lastSavedSession: DeliveryPartnerSession? = null
    var getSessionsCalls = 0
    var sessionsToReturn: List<DeliveryPartnerSession> = emptyList()
    var partnerSessionsToReturn: List<DeliveryPartnerSession> = emptyList()

    override suspend fun getSessions(userId: String): List<DeliveryPartnerSession> {
        getSessionsCalls++
        return sessionsToReturn
    }

    override suspend fun getSessionsForPartner(userId: String, partnerId: String): List<DeliveryPartnerSession> {
        return partnerSessionsToReturn
    }

    override suspend fun saveSession(session: DeliveryPartnerSession): DeliveryPartnerSession {
        lastSavedSession = session
        return session
    }
}

private class FakeExpenseRepository(initialExpense: Expense? = null) : ExpenseRepository() {
    var currentExpense: Expense? = initialExpense
    var getExpenseByIdCalls = 0
    var updateExpenseCalls = 0

    override suspend fun getExpenseById(expenseId: String): Expense? {
        getExpenseByIdCalls++
        return if (currentExpense?.id == expenseId) currentExpense else null
    }

    override suspend fun updateExpense(expense: Expense): Expense {
        updateExpenseCalls++
        currentExpense = expense
        return expense
    }
}
