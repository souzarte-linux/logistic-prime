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
}
