package com.fernando.centraldomotorista.functional

import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.ui.common.period.PeriodFilter
import com.fernando.centraldomotorista.ui.common.period.PeriodPreset
import com.fernando.centraldomotorista.ui.screens.historico.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class HistoricoCategoryFilterTest {

    private val testZone = ZoneOffset.UTC
    private val testScope = CoroutineScope(Dispatchers.Default)
    private val today = LocalDate.of(2026, 9, 15)
    private val nowOffset = OffsetDateTime.of(2026, 9, 15, 12, 0, 0, 0, testZone)

    // =========================================================================
    // 1. INFERÊNCIA DETERMINÍSTICA DE SUBTIPO DE MANUTENÇÃO (HEURÍSTICA BEST-EFFORT)
    // =========================================================================
    @Test
    fun testMaintenanceSubtypeInference() {
        // Cenário A: Óleo do Motor
        val expOleo = Expense(
            id = "exp-1", userId = "u1", category = "manutencao",
            title = "Manutenção: Óleo do Motor Mobil 20W50",
            amount = BigDecimal("45.00"), occurredAt = nowOffset
        )
        val txOleo = TransactionItem(
            id = "tx-1", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "MANUTENÇÃO: ÓLEO DO MOTOR MOBIL 20W50", subtitle = "Oficina",
            amount = BigDecimal("45.00"), netAmount = BigDecimal("45.00"),
            category = "MANUTENÇÃO", occurredAt = nowOffset, rawExpense = expOleo
        )
        assertEquals(MaintenanceSubtype.OLEO, inferMaintenanceSubtype(txOleo))
        val optOleo = resolveTransactionCategoryFilter(txOleo)
        assertEquals("MANUTENCAO_OLEO", optOleo.key)
        assertEquals("Manutenção - Óleo", optOleo.label)
        assertEquals("Manutenção", optOleo.group)

        // Cenário B: Peça (Pastilha de Freio Cobreq)
        val expPeca = Expense(
            id = "exp-2", userId = "u1", category = "manutencao",
            title = "Pastilhas de Freio", partBrand = "Cobreq", partModel = "Dianteira",
            amount = BigDecimal("85.00"), occurredAt = nowOffset
        )
        val txPeca = TransactionItem(
            id = "tx-2", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "PASTILHAS DE FREIO", subtitle = "Cobreq",
            amount = BigDecimal("85.00"), netAmount = BigDecimal("85.00"),
            category = "MANUTENÇÃO", occurredAt = nowOffset, rawExpense = expPeca
        )
        assertEquals(MaintenanceSubtype.PECA, inferMaintenanceSubtype(txPeca))
        val optPeca = resolveTransactionCategoryFilter(txPeca)
        assertEquals("MANUTENCAO_PECA", optPeca.key)
        assertEquals("Manutenção - Peça", optPeca.label)

        // Cenário C: Serviço (Mão de obra de alinhamento e balanceamento)
        val expServico = Expense(
            id = "exp-3", userId = "u1", category = "manutencao",
            title = "Alinhamento e Balanceamento", description = "Mão de obra mecânica",
            amount = BigDecimal("70.00"), occurredAt = nowOffset
        )
        val txServico = TransactionItem(
            id = "tx-3", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "ALINHAMENTO E BALANCEAMENTO", subtitle = "Auto Center",
            amount = BigDecimal("70.00"), netAmount = BigDecimal("70.00"),
            category = "MANUTENÇÃO", occurredAt = nowOffset, rawExpense = expServico
        )
        assertEquals(MaintenanceSubtype.SERVICO, inferMaintenanceSubtype(txServico))
        val optServico = resolveTransactionCategoryFilter(txServico)
        assertEquals("MANUTENCAO_SERVICO", optServico.key)
        assertEquals("Manutenção - Serviço", optServico.label)

        // Cenário D: Não classificado (cai em Outros sem quebrar)
        val expGenerico = Expense(
            id = "exp-4", userId = "u1", category = "manutencao",
            title = "Ajuste diverso",
            amount = BigDecimal("20.00"), occurredAt = nowOffset
        )
        val txGenerico = TransactionItem(
            id = "tx-4", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "AJUSTE DIVERSO", subtitle = "Garagem",
            amount = BigDecimal("20.00"), netAmount = BigDecimal("20.00"),
            category = "MANUTENÇÃO", occurredAt = nowOffset, rawExpense = expGenerico
        )
        assertEquals(MaintenanceSubtype.OUTROS, inferMaintenanceSubtype(txGenerico))
        val optGenerico = resolveTransactionCategoryFilter(txGenerico)
        assertEquals("MANUTENCAO_OUTROS", optGenerico.key)
        assertEquals("Manutenção - Outros", optGenerico.label)
    }

    // =========================================================================
    // 2. RESOLUÇÃO DE FILTROS PARA EQUIPE, ALIMENTAÇÃO, COMBUSTÍVEL E GANHOS
    // =========================================================================
    @Test
    fun testResolveCategoryFiltersForOtherDomains() {
        // 1. Equipe
        val partner = DeliveryPartner(id = "p-1", fullName = "João Silva")
        val txEquipe = TransactionItem(
            id = "tx-eq", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "DIÁRIA ENTREGADOR", subtitle = "João Silva",
            amount = BigDecimal("120.00"), netAmount = BigDecimal("120.00"),
            category = "EQUIPE", occurredAt = nowOffset, rawPartner = partner
        )
        val optEquipe = resolveTransactionCategoryFilter(txEquipe)
        assertEquals("EQUIPE_JOÃO SILVA", optEquipe.key)
        assertEquals("Equipe - João Silva", optEquipe.label)
        assertEquals("Equipe", optEquipe.group)

        // 2. Alimentação
        val expAlim = Expense(
            id = "exp-al", userId = "u1", category = "alimentacao",
            title = "Almoço Restaurante", mealType = "Almoço",
            amount = BigDecimal("32.00"), occurredAt = nowOffset
        )
        val txAlim = TransactionItem(
            id = "tx-al", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "ALMOÇO RESTAURANTE", subtitle = "Restaurante",
            amount = BigDecimal("32.00"), netAmount = BigDecimal("32.00"),
            category = "ALIMENTAÇÃO", occurredAt = nowOffset, rawExpense = expAlim
        )
        val optAlim = resolveTransactionCategoryFilter(txAlim)
        assertEquals("ALIMENTACAO_ALMOÇO", optAlim.key)
        assertEquals("Alimentação - Almoço", optAlim.label)
        assertEquals("Alimentação", optAlim.group)

        // 3. Combustível / Abastecimento
        val expComb = Expense(
            id = "exp-comb", userId = "u1", category = "combustivel",
            title = "Abastecimento Posto Ipiranga", fuelType = "Gasolina Comum",
            amount = BigDecimal("60.00"), occurredAt = nowOffset
        )
        val txComb = TransactionItem(
            id = "tx-comb", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "ABASTECIMENTO", subtitle = "Posto Ipiranga",
            amount = BigDecimal("60.00"), netAmount = BigDecimal("60.00"),
            category = "COMBUSTÍVEL", occurredAt = nowOffset, rawExpense = expComb
        )
        val optComb = resolveTransactionCategoryFilter(txComb)
        assertEquals("COMBUSTIVEL_GASOLINA COMUM", optComb.key)
        assertEquals("Abastecimento - Gasolina Comum", optComb.label)
        assertEquals("Abastecimento", optComb.group)

        // 4. Ganhos (Receita) por Plataforma
        val txGanho = TransactionItem(
            id = "tx-rt", type = TransactionType.GANHO, sourceType = TransactionSourceType.ROUTE,
            title = "IFOOD", subtitle = "SPO - SAO",
            amount = BigDecimal("150.00"), netAmount = BigDecimal("150.00"),
            category = "ROTA", occurredAt = nowOffset, establishment = "iFood"
        )
        val optGanho = resolveTransactionCategoryFilter(txGanho)
        assertEquals("GANHO_IFOOD", optGanho.key)
        assertEquals("Ganhos - iFood", optGanho.label)
        assertEquals("Ganhos", optGanho.group)
    }

    // =========================================================================
    // 3. DERIVAÇÃO DINÂMICA DE OPÇÕES (SEM OPÇÕES VAZIAS)
    // =========================================================================
    @Test
    fun testDeriveAvailableCategoryFilters_NoEmptyOptions() {
        val exp1 = Expense(
            id = "e1", userId = "u1", category = "combustivel",
            title = "Shell", fuelType = "Etanol", amount = BigDecimal("50.00"), occurredAt = nowOffset
        )
        val tx1 = TransactionItem(
            id = "t1", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "SHELL", subtitle = "Etanol", amount = BigDecimal("50.00"), netAmount = BigDecimal("50.00"),
            category = "COMBUSTÍVEL", occurredAt = nowOffset, rawExpense = exp1
        )
        val exp2 = Expense(
            id = "e2", userId = "u1", category = "combustivel",
            title = "BR", fuelType = "Etanol", amount = BigDecimal("40.00"), occurredAt = nowOffset
        )
        val tx2 = TransactionItem(
            id = "t2", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "BR", subtitle = "Etanol", amount = BigDecimal("40.00"), netAmount = BigDecimal("40.00"),
            category = "COMBUSTÍVEL", occurredAt = nowOffset, rawExpense = exp2
        )
        val tx3 = TransactionItem(
            id = "t3", type = TransactionType.GANHO, sourceType = TransactionSourceType.ROUTE,
            title = "RAPPI", subtitle = "Rota", amount = BigDecimal("100.00"), netAmount = BigDecimal("100.00"),
            category = "ROTA", occurredAt = nowOffset, establishment = "Rappi"
        )

        val items = listOf(tx1, tx2, tx3)
        val available = deriveAvailableCategoryFilters(items)

        // Deve derivar exatamente 2 opções (Etanol com contagem 2, Rappi com contagem 1)
        assertEquals(2, available.size)

        val etanolOption = available.find { it.key == "COMBUSTIVEL_ETANOL" }
        assertNotNull(etanolOption)
        assertEquals(2, etanolOption?.count)
        assertEquals("Abastecimento - Etanol", etanolOption?.label)

        val rappiOption = available.find { it.key == "GANHO_RAPPI" }
        assertNotNull(rappiOption)
        assertEquals(1, rappiOption?.count)
        assertEquals("Ganhos - Rappi", rappiOption?.label)

        // Não deve conter Gasolina, iFood ou outras opções vazias
        assertNull(available.find { it.key == "COMBUSTIVEL_GASOLINA COMUM" })
        assertNull(available.find { it.key == "GANHO_IFOOD" })
    }

    // =========================================================================
    // 4. FILTRAGEM VIA BUILDGROUPS: ISOLAMENTO, COMBINAÇÃO (OR) E INTERSEÇÃO (AND)
    // =========================================================================
    @Test
    fun testBuildGroupsWithCategoryFilter() {
        val vm = HistoricoViewModel(externalScope = testScope, observeDataSync = false, autoLoad = false)

        // Transação 1: Manutenção - Óleo
        val expOleo = Expense(
            id = "e-oil", userId = "u1", category = "manutencao",
            title = "Troca de Óleo Motor", amount = BigDecimal("50.00"), occurredAt = nowOffset
        )
        val txOleo = TransactionItem(
            id = "t-oil", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "TROCA DE ÓLEO MOTOR", subtitle = "Oficina", amount = BigDecimal("50.00"), netAmount = BigDecimal("50.00"),
            category = "MANUTENÇÃO", occurredAt = nowOffset, rawExpense = expOleo
        )

        // Transação 2: Manutenção - Peça
        val expPeca = Expense(
            id = "e-part", userId = "u1", category = "manutencao",
            title = "Pastilha de Freio", partBrand = "Cobreq", amount = BigDecimal("80.00"), occurredAt = nowOffset
        )
        val txPeca = TransactionItem(
            id = "t-part", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "PASTILHA DE FREIO", subtitle = "Cobreq", amount = BigDecimal("80.00"), netAmount = BigDecimal("80.00"),
            category = "MANUTENÇÃO", occurredAt = nowOffset, rawExpense = expPeca
        )

        // Transação 3: Abastecimento - Etanol
        val expEtanol = Expense(
            id = "e-fuel", userId = "u1", category = "combustivel",
            title = "Posto Shell", fuelType = "Etanol", amount = BigDecimal("70.00"), occurredAt = nowOffset
        )
        val txEtanol = TransactionItem(
            id = "t-fuel", type = TransactionType.DESPESA, sourceType = TransactionSourceType.EXPENSE,
            title = "POSTO SHELL", subtitle = "Etanol", amount = BigDecimal("70.00"), netAmount = BigDecimal("70.00"),
            category = "COMBUSTÍVEL", occurredAt = nowOffset, rawExpense = expEtanol
        )

        // Transação 4: Ganho - iFood
        val txIfood = TransactionItem(
            id = "t-ifood", type = TransactionType.GANHO, sourceType = TransactionSourceType.ROUTE,
            title = "IFOOD", subtitle = "Entrega", amount = BigDecimal("150.00"), netAmount = BigDecimal("150.00"),
            category = "ROTA", occurredAt = nowOffset, establishment = "iFood"
        )

        val allItems = listOf(txOleo, txPeca, txEtanol, txIfood)
        val defaultPeriod = PeriodFilter(preset = PeriodPreset.MES)

        // Caso 4A: Nenhum filtro de categoria ativo (deve retornar todas as 4)
        val (_, listAll) = vm.buildGroups(
            items = allItems,
            tab = HistoricoTab.TODOS,
            query = "",
            periodFilter = defaultPeriod,
            categoryFilters = emptySet(),
            today = today,
            zone = testZone
        )
        assertEquals(4, listAll.size)

        // Caso 4B: Filtro exclusivo por "Manutenção - Óleo"
        val (_, listOleo) = vm.buildGroups(
            items = allItems,
            tab = HistoricoTab.TODOS,
            query = "",
            periodFilter = defaultPeriod,
            categoryFilters = setOf("MANUTENCAO_OLEO"),
            today = today,
            zone = testZone
        )
        assertEquals(1, listOleo.size)
        assertEquals("t-oil", listOleo[0].id)

        // Caso 4C: Multi-seleção (OR entre categorias): "Manutenção - Óleo" + "Abastecimento - Etanol"
        val (_, listMulti) = vm.buildGroups(
            items = allItems,
            tab = HistoricoTab.TODOS,
            query = "",
            periodFilter = defaultPeriod,
            categoryFilters = setOf("MANUTENCAO_OLEO", "COMBUSTIVEL_ETANOL"),
            today = today,
            zone = testZone
        )
        assertEquals(2, listMulti.size)
        assertTrue(listMulti.any { it.id == "t-oil" })
        assertTrue(listMulti.any { it.id == "t-fuel" })
        assertFalse(listMulti.any { it.id == "t-part" })
        assertFalse(listMulti.any { it.id == "t-ifood" })

        // Caso 4D: Combinação AND com aba (Aba GANHOS com filtro de categoria de Despesa -> retorna vazio)
        val (_, listGanhosConflito) = vm.buildGroups(
            items = allItems,
            tab = HistoricoTab.GANHOS,
            query = "",
            periodFilter = defaultPeriod,
            categoryFilters = setOf("MANUTENCAO_OLEO"),
            today = today,
            zone = testZone
        )
        assertTrue(listGanhosConflito.isEmpty())

        // Caso 4E: Combinação AND com aba GANHOS + filtro "Ganhos - iFood"
        val (_, listGanhosIfood) = vm.buildGroups(
            items = allItems,
            tab = HistoricoTab.GANHOS,
            query = "",
            periodFilter = defaultPeriod,
            categoryFilters = setOf("GANHO_IFOOD"),
            today = today,
            zone = testZone
        )
        assertEquals(1, listGanhosIfood.size)
        assertEquals("t-ifood", listGanhosIfood[0].id)
    }

    // =========================================================================
    // 5. MUTAÇÕES DE ESTADO DO VIEWMODEL PARA CATEGORIAS
    // =========================================================================
    @Test
    fun testViewModelCategoryFilterMutations() {
        val vm = HistoricoViewModel(externalScope = testScope, observeDataSync = false, autoLoad = false)

        // Estado inicial
        assertTrue(vm.uiState.value.selectedCategoryFilters.isEmpty())
        assertFalse(vm.uiState.value.isCategoryFilterSheetOpen)

        // Abrir e fechar bottom sheet
        vm.openCategoryFilterSheet()
        assertTrue(vm.uiState.value.isCategoryFilterSheetOpen)
        vm.closeCategoryFilterSheet()
        assertFalse(vm.uiState.value.isCategoryFilterSheetOpen)

        // Toggle adicionar filtro 1
        vm.toggleCategoryFilter("MANUTENCAO_OLEO")
        assertEquals(setOf("MANUTENCAO_OLEO"), vm.uiState.value.selectedCategoryFilters)

        // Toggle adicionar filtro 2 (multi-select)
        vm.toggleCategoryFilter("COMBUSTIVEL_ETANOL")
        assertEquals(setOf("MANUTENCAO_OLEO", "COMBUSTIVEL_ETANOL"), vm.uiState.value.selectedCategoryFilters)

        // Toggle remover filtro 1
        vm.toggleCategoryFilter("MANUTENCAO_OLEO")
        assertEquals(setOf("COMBUSTIVEL_ETANOL"), vm.uiState.value.selectedCategoryFilters)

        // Limpar todos os filtros de categoria
        vm.clearCategoryFilters()
        assertTrue(vm.uiState.value.selectedCategoryFilters.isEmpty())
    }
}
