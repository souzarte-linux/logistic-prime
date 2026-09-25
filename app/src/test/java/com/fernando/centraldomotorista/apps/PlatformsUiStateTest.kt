package com.fernando.centraldomotorista.apps

import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.ui.screens.apps.PlatformSegmentFilter
import com.fernando.centraldomotorista.ui.screens.apps.PlatformSortBy
import com.fernando.centraldomotorista.ui.screens.apps.PlatformStatusFilter
import com.fernando.centraldomotorista.ui.screens.apps.PlatformsUiState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PlatformsUiStateTest {

    private val platform1 = Platform(
        id = "p-1",
        userId = "u-1",
        name = "Mercado Envios",
        segment = "logistica",
        cycle = "semanal",
        paymentDay = "QUA",
        active = true
    )

    private val platform2 = Platform(
        id = "p-2",
        userId = "u-1",
        name = "iFood",
        segment = "delivery",
        cycle = "semanal",
        paymentDay = "SEG",
        active = true
    )

    private val platform3 = Platform(
        id = "p-3",
        userId = "u-1",
        name = "Lalamove",
        segment = "logistica",
        cycle = "quinzenal",
        paymentDay = null,
        active = false
    )

    @Test
    fun testStatusFilter() {
        val state = PlatformsUiState(
            platforms = listOf(platform1, platform2, platform3),
            statusFilter = PlatformStatusFilter.ACTIVE
        )

        val activeList = state.filteredAndSortedPlatforms
        assertEquals(2, activeList.size)
        assertEquals(listOf("iFood", "Mercado Envios"), activeList.map { it.name })

        val inactiveState = state.copy(statusFilter = PlatformStatusFilter.INACTIVE)
        val inactiveList = inactiveState.filteredAndSortedPlatforms
        assertEquals(1, inactiveList.size)
        assertEquals("Lalamove", inactiveList[0].name)
    }

    @Test
    fun testSegmentFilter() {
        val state = PlatformsUiState(
            platforms = listOf(platform1, platform2, platform3),
            segmentFilter = PlatformSegmentFilter.DELIVERY
        )

        val deliveryList = state.filteredAndSortedPlatforms
        assertEquals(1, deliveryList.size)
        assertEquals("iFood", deliveryList[0].name)

        val logisticaState = state.copy(segmentFilter = PlatformSegmentFilter.LOGISTICA)
        val logisticaList = logisticaState.filteredAndSortedPlatforms
        assertEquals(2, logisticaList.size)
    }

    @Test
    fun testSortByEarnings() {
        val earnings = mapOf(
            "p-1" to BigDecimal("1500.50"), // Mercado Envios
            "p-2" to BigDecimal("3200.00"), // iFood
            "p-3" to BigDecimal("500.00")   // Lalamove
        )

        val state = PlatformsUiState(
            platforms = listOf(platform1, platform2, platform3),
            earningsMap = earnings,
            sortBy = PlatformSortBy.HIGHEST_EARNING
        )

        val sortedDesc = state.filteredAndSortedPlatforms
        assertEquals("iFood", sortedDesc[0].name)
        assertEquals("Mercado Envios", sortedDesc[1].name)
        assertEquals("Lalamove", sortedDesc[2].name)

        val stateAsc = state.copy(sortBy = PlatformSortBy.LOWEST_EARNING)
        val sortedAsc = stateAsc.filteredAndSortedPlatforms
        assertEquals("Lalamove", sortedAsc[0].name)
        assertEquals("Mercado Envios", sortedAsc[1].name)
        assertEquals("iFood", sortedAsc[2].name)
    }

    @Test
    fun testActiveFilterCount() {
        val stateNoFilters = PlatformsUiState()
        assertEquals(0, stateNoFilters.activeFilterCount)

        val stateOneFilter = stateNoFilters.copy(statusFilter = PlatformStatusFilter.ACTIVE)
        assertEquals(1, stateOneFilter.activeFilterCount)

        val stateAllFilters = stateOneFilter.copy(
            segmentFilter = PlatformSegmentFilter.DELIVERY,
            sortBy = PlatformSortBy.HIGHEST_EARNING
        )
        assertEquals(3, stateAllFilters.activeFilterCount)
    }

    @Test
    fun testFormStateForEditingPlatform() {
        val state = PlatformsUiState(
            editingPlatformId = platform1.id,
            name = platform1.name,
            segment = platform1.segment,
            cycle = platform1.cycle,
            paymentDay = platform1.paymentDay ?: "QUA",
            active = platform1.active
        )

        assertEquals("p-1", state.editingPlatformId)
        assertEquals("Mercado Envios", state.name)
        assertEquals("logistica", state.segment)
        assertEquals("semanal", state.cycle)
        assertEquals("QUA", state.paymentDay)
        assertEquals(true, state.active)
    }

    @Test
    fun testCycleEntriesDefaults() {
        val state = PlatformsUiState()
        assertEquals(2, state.cycleEntries.size)
        assertEquals(1, state.cycleEntries[0].cut)
        assertEquals(16, state.cycleEntries[1].cut)
    }

    @Test
    fun testSearchQueryFiltering() {
        val state = PlatformsUiState(
            platforms = listOf(platform1, platform2, platform3)
        )

        // Busca por nome com maiúsculas/minúsculas e espaços
        val searchByName = state.copy(searchQuery = "  mercado  ").filteredAndSortedPlatforms
        assertEquals(1, searchByName.size)
        assertEquals("Mercado Envios", searchByName[0].name)

        // Busca por segmento
        val searchBySegment = state.copy(searchQuery = "delivery").filteredAndSortedPlatforms
        assertEquals(1, searchBySegment.size)
        assertEquals("iFood", searchBySegment[0].name)

        // Busca por ciclo
        val searchByCycle = state.copy(searchQuery = "quinzenal").filteredAndSortedPlatforms
        assertEquals(1, searchByCycle.size)
        assertEquals("Lalamove", searchByCycle[0].name)

        // Busca por dia de pagamento
        val searchByPaymentDay = state.copy(searchQuery = "seg").filteredAndSortedPlatforms
        assertEquals(1, searchByPaymentDay.size)
        assertEquals("iFood", searchByPaymentDay[0].name)

        // Busca sem correspondência
        val searchNotFound = state.copy(searchQuery = "inexistente").filteredAndSortedPlatforms
        assertEquals(0, searchNotFound.size)

        // Busca em branco deve manter todos
        val searchBlank = state.copy(searchQuery = "   ").filteredAndSortedPlatforms
        assertEquals(3, searchBlank.size)
    }

    @Test
    fun testCombinedFilteringAndSorting() {
        val platform4 = Platform(
            id = "p-4",
            userId = "u-1",
            name = "Amazon Flex",
            segment = "logistica",
            cycle = "semanal",
            paymentDay = "TER",
            active = true
        )
        val platform5 = Platform(
            id = "p-5",
            userId = "u-1",
            name = "Rappi",
            segment = "delivery",
            cycle = "semanal",
            paymentDay = "SEG",
            active = false
        )

        val earnings = mapOf(
            "p-1" to BigDecimal("1000.00"), // Mercado Envios
            "p-4" to BigDecimal("2500.00")  // Amazon Flex
        )

        val state = PlatformsUiState(
            platforms = listOf(platform1, platform2, platform3, platform4, platform5),
            earningsMap = earnings,
            statusFilter = PlatformStatusFilter.ACTIVE,
            segmentFilter = PlatformSegmentFilter.LOGISTICA,
            searchQuery = "a", // Deve casar com "Mercado Envios" e "Amazon Flex"
            sortBy = PlatformSortBy.HIGHEST_EARNING
        )

        val result = state.filteredAndSortedPlatforms
        assertEquals(2, result.size)
        // Maior ganho primeiro: Amazon Flex (2500) antes de Mercado Envios (1000)
        assertEquals("Amazon Flex", result[0].name)
        assertEquals("Mercado Envios", result[1].name)
    }

    @Test
    fun testEarningsSortingWithMissingOrZeroValues() {
        val platform4 = Platform(
            id = "p-4",
            userId = "u-1",
            name = "Sem Ganhos",
            segment = "delivery",
            cycle = "semanal",
            paymentDay = "QUA",
            active = true
        )

        val earnings = mapOf(
            "p-1" to BigDecimal("1500.00")
            // p-2, p-3, p-4 não estão no mapa -> devem ser tratados como ZERO
        )

        val state = PlatformsUiState(
            platforms = listOf(platform1, platform2, platform3, platform4),
            earningsMap = earnings,
            sortBy = PlatformSortBy.HIGHEST_EARNING
        )

        val result = state.filteredAndSortedPlatforms
        assertEquals("p-1", result[0].id)
        // O primeiro deve ser p-1 (1500), e os demais empatados em 0 mantendo ordem estável
        assertEquals(BigDecimal("1500.00"), earnings[result[0].id])
    }

    @Test
    fun testDefaultVariableCycleEntriesLogic() {
        val entries = com.fernando.centraldomotorista.ui.screens.apps.defaultVariableCycleEntries(delay = 5)
        assertEquals(2, entries.size)

        val firstHalf = entries[0]
        assertEquals(1, firstHalf.cut)
        assertEquals(5, firstHalf.payDelay)
        assertEquals(5, firstHalf.payDelayDays)
        assertEquals(1, firstHalf.startDate?.dayOfMonth)
        assertEquals(15, firstHalf.endDate?.dayOfMonth)
        assertEquals(firstHalf.endDate?.plusDays(5), firstHalf.paymentDate)

        val secondHalf = entries[1]
        assertEquals(16, secondHalf.cut)
        assertEquals(5, secondHalf.payDelay)
        assertEquals(5, secondHalf.payDelayDays)
        assertEquals(16, secondHalf.startDate?.dayOfMonth)
        assertEquals(secondHalf.endDate?.plusDays(5), secondHalf.paymentDate)

        // Teste de atraso negativo (deve ser coagido para 0)
        val negativeEntries = com.fernando.centraldomotorista.ui.screens.apps.defaultVariableCycleEntries(delay = -3)
        assertEquals(0, negativeEntries[0].payDelay)
        assertEquals(0, negativeEntries[1].payDelay)
    }
}

