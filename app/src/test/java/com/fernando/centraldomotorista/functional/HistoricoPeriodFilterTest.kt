package com.fernando.centraldomotorista.functional

import com.fernando.centraldomotorista.data.model.TransactionItem
import com.fernando.centraldomotorista.data.model.TransactionSourceType
import com.fernando.centraldomotorista.data.model.TransactionType
import com.fernando.centraldomotorista.ui.common.period.PeriodFilter
import com.fernando.centraldomotorista.ui.common.period.PeriodPreset
import com.fernando.centraldomotorista.ui.screens.historico.HistoricoTab
import com.fernando.centraldomotorista.ui.screens.historico.HistoricoViewModel
import com.fernando.centraldomotorista.ui.screens.historico.toSaldoCardTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class HistoricoPeriodFilterTest {

    private val testZone = ZoneOffset.UTC
    private val testScope = CoroutineScope(Dispatchers.Default)

    // =========================================================================
    // 1. TÍTULOS DINÂMICOS DO CARD DE SALDO
    // =========================================================================
    @Test
    fun testDynamicCardTitlesForAllPresets() {
        assertEquals("SALDO DE HOJE", PeriodPreset.DIA.toSaldoCardTitle())
        assertEquals("SALDO DA SEMANA", PeriodPreset.SEMANA.toSaldoCardTitle())
        assertEquals("SALDO DA QUINZENA", PeriodPreset.QUINZENA.toSaldoCardTitle())
        assertEquals("SALDO MENSAL", PeriodPreset.MES.toSaldoCardTitle())
        assertEquals("SALDO ANUAL", PeriodPreset.ANO.toSaldoCardTitle())
        assertEquals("SALDO DO PERÍODO", PeriodPreset.PERSONALIZADO.toSaldoCardTitle())
    }

    // =========================================================================
    // 2. RESOLUÇÃO DE INTERVALOS DE DATAS (START/END OF PERIOD)
    // =========================================================================
    @Test
    fun testPeriodFilterResolveRange() {
        val today = LocalDate.of(2026, 9, 10) // Quinta-feira

        // 1. DIA: Hoje
        val (diaStart, diaEnd) = PeriodFilter(preset = PeriodPreset.DIA).resolveRange(today)
        assertEquals(today, diaStart)
        assertEquals(today, diaEnd)

        // 2. SEMANA: Segunda a Domingo
        val (semStart, semEnd) = PeriodFilter(preset = PeriodPreset.SEMANA).resolveRange(today)
        assertEquals(LocalDate.of(2026, 9, 7), semStart)
        assertEquals(DayOfWeek.MONDAY, semStart.dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 13), semEnd)
        assertEquals(DayOfWeek.SUNDAY, semEnd.dayOfWeek)

        // 3. QUINZENA: 14 dias atrás até hoje (15 dias corridos)
        val (quinzStart, quinzEnd) = PeriodFilter(preset = PeriodPreset.QUINZENA).resolveRange(today)
        assertEquals(today.minusDays(14), quinzStart)
        assertEquals(today, quinzEnd)

        // 4. MÊS: Primeiro ao último dia do mês
        val (mesStart, mesEnd) = PeriodFilter(preset = PeriodPreset.MES).resolveRange(today)
        assertEquals(LocalDate.of(2026, 9, 1), mesStart)
        assertEquals(LocalDate.of(2026, 9, 30), mesEnd)

        // 5. ANO: 01/01 a 31/12
        val (anoStart, anoEnd) = PeriodFilter(preset = PeriodPreset.ANO).resolveRange(today)
        assertEquals(LocalDate.of(2026, 1, 1), anoStart)
        assertEquals(LocalDate.of(2026, 12, 31), anoEnd)

        // 6. PERSONALIZADO: Intervalo customizado
        val cStart = LocalDate.of(2026, 8, 1)
        val cEnd = LocalDate.of(2026, 8, 20)
        val (customStart, customEnd) = PeriodFilter(
            preset = PeriodPreset.PERSONALIZADO,
            customStart = cStart,
            customEnd = cEnd
        ).resolveRange(today)
        assertEquals(cStart, customStart)
        assertEquals(cEnd, customEnd)
    }

    // =========================================================================
    // 3. MÉTRICAS FINANCEIRAS EM BIGDECIMAL E META PROPORCIONAL
    // =========================================================================
    @Test
    fun testCalculatePeriodMetrics_StrictBigDecimalAndProportionalGoal() {
        val today = LocalDate.of(2026, 9, 10)
        val vm = HistoricoViewModel(externalScope = testScope, observeDataSync = false, autoLoad = false)

        val txHojeGanho = TransactionItem(
            id = "tx-1",
            type = TransactionType.GANHO,
            sourceType = TransactionSourceType.ROUTE,
            title = "MERCADO LIVRE",
            subtitle = "SPO - SAO",
            amount = BigDecimal("150.00"),
            netAmount = BigDecimal("150.00"),
            category = "ROTA",
            occurredAt = OffsetDateTime.of(2026, 9, 10, 10, 0, 0, 0, testZone)
        )
        val txHojeDespesa = TransactionItem(
            id = "tx-2",
            type = TransactionType.DESPESA,
            sourceType = TransactionSourceType.EXPENSE,
            title = "ALMOÇO",
            subtitle = "RESTAURANTE",
            amount = BigDecimal("35.50"),
            netAmount = BigDecimal("35.50"),
            category = "ALIMENTAÇÃO",
            occurredAt = OffsetDateTime.of(2026, 9, 10, 12, 30, 0, 0, testZone)
        )
        val txSegundaGanho = TransactionItem(
            id = "tx-3",
            type = TransactionType.GANHO,
            sourceType = TransactionSourceType.DAILY_TOTAL,
            title = "TOTAL DO DIA",
            subtitle = "IFLASH",
            amount = BigDecimal("200.00"),
            netAmount = BigDecimal("180.00"),
            category = "TOTAL DO DIA",
            occurredAt = OffsetDateTime.of(2026, 9, 7, 18, 0, 0, 0, testZone) // Segunda-feira da mesma semana
        )
        val txMesPassado = TransactionItem(
            id = "tx-4",
            type = TransactionType.GANHO,
            sourceType = TransactionSourceType.ROUTE,
            title = "ROTA ANTIGA",
            subtitle = "AGOSTO",
            amount = BigDecimal("300.00"),
            netAmount = BigDecimal("300.00"),
            category = "ROTA",
            occurredAt = OffsetDateTime.of(2026, 8, 20, 10, 0, 0, 0, testZone)
        )

        val allItems = listOf(txHojeGanho, txHojeDespesa, txSegundaGanho, txMesPassado)
        val dailyGoal = BigDecimal("300.00")

        // Cenário A: Filtro DIA (apenas hoje)
        val metricsDia = vm.calculatePeriodMetrics(
            items = allItems,
            periodFilter = PeriodFilter(preset = PeriodPreset.DIA),
            dailyGoal = dailyGoal,
            today = today,
            zone = testZone
        )
        assertEquals("SALDO DE HOJE", metricsDia.cardTitle)
        assertEquals(BigDecimal("150.00"), metricsDia.entradasPeriodo)
        assertEquals(BigDecimal("35.50"), metricsDia.saidasPeriodo)
        assertEquals(BigDecimal("114.50"), metricsDia.saldoPeriodo)
        assertEquals(BigDecimal("300.00"), metricsDia.metaPeriodo) // 1 dia * 300
        assertEquals(38, metricsDia.metaPercent) // 114.50 / 300 = 38%
        assertEquals(BigDecimal("185.50"), metricsDia.faltamParaMeta)

        // Cenário B: Filtro SEMANA (segunda a domingo da semana de 10/09)
        val metricsSemana = vm.calculatePeriodMetrics(
            items = allItems,
            periodFilter = PeriodFilter(preset = PeriodPreset.SEMANA),
            dailyGoal = dailyGoal,
            today = today,
            zone = testZone
        )
        assertEquals("SALDO DA SEMANA", metricsSemana.cardTitle)
        // Entradas da semana: txHojeGanho (150.00) + txSegundaGanho (180.00) = 330.00
        assertEquals(BigDecimal("330.00"), metricsSemana.entradasPeriodo)
        // Saídas da semana: txHojeDespesa (35.50)
        assertEquals(BigDecimal("35.50"), metricsSemana.saidasPeriodo)
        // Saldo da semana: 330.00 - 35.50 = 294.50
        assertEquals(BigDecimal("294.50"), metricsSemana.saldoPeriodo)
        // Meta semanal: 7 dias * 300.00 = 2100.00
        assertEquals(BigDecimal("2100.00"), metricsSemana.metaPeriodo)
        // 294.50 / 2100 = 14%
        assertEquals(14, metricsSemana.metaPercent)
        assertEquals(BigDecimal("1805.50"), metricsSemana.faltamParaMeta)

        // Cenário C: Filtro MÊS (Setembro de 2026: 30 dias)
        val metricsMes = vm.calculatePeriodMetrics(
            items = allItems,
            periodFilter = PeriodFilter(preset = PeriodPreset.MES),
            dailyGoal = dailyGoal,
            today = today,
            zone = testZone
        )
        assertEquals("SALDO MENSAL", metricsMes.cardTitle)
        assertEquals(BigDecimal("330.00"), metricsMes.entradasPeriodo)
        assertEquals(BigDecimal("35.50"), metricsMes.saidasPeriodo)
        assertEquals(BigDecimal("294.50"), metricsMes.saldoPeriodo)
        // Meta mensal: 30 dias * 300.00 = 9000.00
        assertEquals(BigDecimal("9000.00"), metricsMes.metaPeriodo)

        // Cenário D: Meta atingida (100%+)
        val txSuperGanho = TransactionItem(
            id = "tx-super",
            type = TransactionType.GANHO,
            sourceType = TransactionSourceType.ROUTE,
            title = "SUPER ROTA",
            subtitle = "SP",
            amount = BigDecimal("500.00"),
            netAmount = BigDecimal("500.00"),
            category = "ROTA",
            occurredAt = OffsetDateTime.of(2026, 9, 10, 15, 0, 0, 0, testZone)
        )
        val metricsSuper = vm.calculatePeriodMetrics(
            items = listOf(txSuperGanho),
            periodFilter = PeriodFilter(preset = PeriodPreset.DIA),
            dailyGoal = BigDecimal("300.00"),
            today = today,
            zone = testZone
        )
        assertEquals(BigDecimal("500.00"), metricsSuper.saldoPeriodo)
        assertEquals(167, metricsSuper.metaPercent) // 500 / 300 = 167%
        assertEquals(BigDecimal.ZERO, metricsSuper.faltamParaMeta)
    }

    // =========================================================================
    // 4. FILTRAGEM CASCATA EM HIERARQUIA (BUILD GROUPS COM PERÍODO)
    // =========================================================================
    @Test
    fun testBuildGroupsWithPeriodFilter() {
        val today = LocalDate.of(2026, 9, 10)
        val vm = HistoricoViewModel(externalScope = testScope, observeDataSync = false, autoLoad = false)

        val txHoje = TransactionItem(
            id = "tx-1",
            type = TransactionType.GANHO,
            sourceType = TransactionSourceType.ROUTE,
            title = "ROTA HOJE",
            subtitle = "SPO",
            amount = BigDecimal("100.00"),
            netAmount = BigDecimal("100.00"),
            category = "ROTA",
            occurredAt = OffsetDateTime.of(2026, 9, 10, 10, 0, 0, 0, testZone)
        )
        val txSemanaPassada = TransactionItem(
            id = "tx-2",
            type = TransactionType.GANHO,
            sourceType = TransactionSourceType.ROUTE,
            title = "ROTA SEMANA PASSADA",
            subtitle = "SPO",
            amount = BigDecimal("80.00"),
            netAmount = BigDecimal("80.00"),
            category = "ROTA",
            occurredAt = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, testZone)
        )
        val txAnoPassado = TransactionItem(
            id = "tx-3",
            type = TransactionType.DESPESA,
            sourceType = TransactionSourceType.EXPENSE,
            title = "DESPESA 2025",
            subtitle = "POSTO",
            amount = BigDecimal("50.00"),
            netAmount = BigDecimal("50.00"),
            category = "COMBUSTÍVEL",
            occurredAt = OffsetDateTime.of(2025, 12, 15, 10, 0, 0, 0, testZone)
        )

        val items = listOf(txHoje, txSemanaPassada, txAnoPassado)

        // 1. Filtrando por SEMANA: apenas txHoje deve aparecer
        val (groupsSemana, filteredSemana) = vm.buildGroups(
            items = items,
            tab = HistoricoTab.TODOS,
            query = "",
            periodFilter = PeriodFilter(preset = PeriodPreset.SEMANA),
            today = today,
            zone = testZone
        )
        assertEquals(1, filteredSemana.size)
        assertEquals("tx-1", filteredSemana[0].id)
        assertEquals(1, groupsSemana.size) // 1 mês (2026-09)
        assertEquals(1, groupsSemana[0].weeks.size) // 1 semana
        assertEquals(1, groupsSemana[0].weeks[0].days.size) // 1 dia

        // 2. Filtrando por MÊS: txHoje e txSemanaPassada devem aparecer, txAnoPassado não
        val (groupsMes, filteredMes) = vm.buildGroups(
            items = items,
            tab = HistoricoTab.TODOS,
            query = "",
            periodFilter = PeriodFilter(preset = PeriodPreset.MES),
            today = today,
            zone = testZone
        )
        assertEquals(2, filteredMes.size)
        assertEquals(1, groupsMes.size) // Mês 2026-09
        assertEquals(2, groupsMes[0].weeks.size) // 2 semanas distintas em setembro

        // 3. Filtrando por Tab (DESPESAS) dentro do período MÊS: deve retornar vazio
        val (groupsDespesasMes, filteredDespesasMes) = vm.buildGroups(
            items = items,
            tab = HistoricoTab.DESPESAS,
            query = "",
            periodFilter = PeriodFilter(preset = PeriodPreset.MES),
            today = today,
            zone = testZone
        )
        assertTrue(filteredDespesasMes.isEmpty())
        assertTrue(groupsDespesasMes.isEmpty())
    }

    // =========================================================================
    // 5. MUTAÇÕES DE ESTADO DO VIEWMODEL
    // =========================================================================
    @Test
    fun testViewModelPeriodFilterMutations() {
        val vm = HistoricoViewModel(externalScope = testScope, observeDataSync = false, autoLoad = false)

        // Estado inicial
        assertEquals(PeriodPreset.SEMANA, vm.uiState.value.periodFilter.preset)
        assertFalse(vm.uiState.value.isPeriodDropdownExpanded)

        // Toggle dropdown
        vm.togglePeriodDropdown()
        assertTrue(vm.uiState.value.isPeriodDropdownExpanded)
        vm.closePeriodDropdown()
        assertFalse(vm.uiState.value.isPeriodDropdownExpanded)

        // Aplicar Preset Mês
        vm.applyPeriodPreset(PeriodPreset.MES)
        assertEquals(PeriodPreset.MES, vm.uiState.value.periodFilter.preset)
        assertFalse(vm.uiState.value.isPeriodDropdownExpanded)

        // Aplicar Período Personalizado
        val s = LocalDate.of(2026, 5, 1)
        val e = LocalDate.of(2026, 5, 15)
        vm.applyCustomPeriod(s, e)
        assertEquals(PeriodPreset.PERSONALIZADO, vm.uiState.value.periodFilter.preset)
        assertEquals(s, vm.uiState.value.periodFilter.customStart)
        assertEquals(e, vm.uiState.value.periodFilter.customEnd)

        // Reset para Semana
        vm.resetPeriodFilter()
        assertEquals(PeriodPreset.SEMANA, vm.uiState.value.periodFilter.preset)
    }
}
