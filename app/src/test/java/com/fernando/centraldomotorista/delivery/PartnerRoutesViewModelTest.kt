package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.Expense
import com.fernando.centraldomotorista.data.repository.DeliveryPartnerSessionRepository
import com.fernando.centraldomotorista.data.repository.ExpenseRepository
import com.fernando.centraldomotorista.ui.screens.deliverypartners.PartnerPeriodFilter
import com.fernando.centraldomotorista.ui.screens.deliverypartners.PartnerPeriodPreset
import com.fernando.centraldomotorista.ui.screens.deliverypartners.PartnerRoutesViewModel
import com.fernando.centraldomotorista.ui.screens.deliverypartners.formatDuration
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
}

private class FakeSessionRepository : DeliveryPartnerSessionRepository() {
    var lastSavedSession: DeliveryPartnerSession? = null
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
