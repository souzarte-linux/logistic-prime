package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.ui.screens.deliverypartners.PartnerPeriodFilter
import com.fernando.centraldomotorista.ui.screens.deliverypartners.PartnerPeriodPreset
import com.fernando.centraldomotorista.ui.screens.deliverypartners.PartnerRoutesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
}
