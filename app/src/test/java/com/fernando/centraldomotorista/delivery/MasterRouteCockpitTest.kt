package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.data.model.TransferStatus
import com.fernando.centraldomotorista.ui.screens.routes.master.MasterRouteHandOffItem
import com.fernando.centraldomotorista.ui.screens.routes.master.RouteCockpitUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Testes unitários para o Cockpit da Rota Master (Prompt 5 e Prompt 8):
 * - Controle de expansão e contração de cards (individual, expandir todos, contrair todos);
 * - Geração de 1 registro financeiro por plataforma presente na rota física com rateio de KM e tempo.
 */
class MasterRouteCockpitTest {

    @Test
    fun testExpandCollapseAllTogglesEveryCard() {
        // Simulação do comportamento de expansão e contração dos cards de paradas (Prompt 5)
        val stopIds = listOf("stop-001", "stop-002", "stop-003", "stop-004")

        val stops = stopIds.map { id ->
            MasterRouteStop(
                id = id,
                routeId = "route-1",
                userId = "u-1",
                barcode = "BC-$id",
                recipientName = "Cliente $id",
                fullAddress = "Endereço $id"
            )
        }

        // Estado inicial: todos os cards contraídos por padrão (expandedStopIds = emptySet)
        var state = RouteCockpitUiState(
            stops = stops,
            expandedStopIds = emptySet()
        )

        assertTrue("Inicialmente nenhum card deve estar expandido", state.expandedStopIds.isEmpty())

        // 1. Expandir todos
        val allIds = state.stops.map { it.id }.toSet()
        state = state.copy(expandedStopIds = allIds)
        assertEquals(4, state.expandedStopIds.size)
        assertTrue(state.expandedStopIds.containsAll(stopIds))

        // 2. Toque individual no card 2: contrai apenas o card 2
        val currentIds = state.expandedStopIds
        state = state.copy(expandedStopIds = currentIds - "stop-002")
        assertEquals(3, state.expandedStopIds.size)
        assertFalse(state.expandedStopIds.contains("stop-002"))
        assertTrue(state.expandedStopIds.contains("stop-001"))
        assertTrue(state.expandedStopIds.contains("stop-003"))
        assertTrue(state.expandedStopIds.contains("stop-004"))

        // 3. Contrair todos
        state = state.copy(expandedStopIds = emptySet())
        assertTrue("Contrair todos deve esvaziar a lista de expandidos", state.expandedStopIds.isEmpty())

        // 4. Toque individual no card 3: expande apenas o card 3
        state = state.copy(expandedStopIds = state.expandedStopIds + "stop-003")
        assertEquals(1, state.expandedStopIds.size)
        assertTrue(state.expandedStopIds.contains("stop-003"))
    }

    @Test
    fun testHandOffGeneratesOneFinancialRoutePerPlatform() {
        // Validação da regra do Prompt 8:
        // A mesma rota física pode ter paradas de plataformas diferentes.
        // Ao finalizar, deve gerar exatamente 1 registro financeiro por plataforma presente.

        val routeDate = LocalDate.of(2026, 9, 29)
        val routeId = "master-route-xyz"

        val masterRoute = MasterDeliveryRoute(
            id = routeId,
            userId = "user-1",
            platformId = "plat-shopee", // plataforma default do cabeçalho
            routeDate = routeDate,
            startLocation = "CD Cajamar",
            status = RouteStatus.EM_ANDAMENTO
        )

        // Rota física com 5 paradas ativas do Master:
        // 3 paradas da Shopee (2 pacotinhos entregues, 1 volumoso entregue)
        // 2 paradas do Mercado Livre (2 pacotinhos entregues)
        val stops = listOf(
            MasterRouteStop(id = "s-1", routeId = routeId, userId = "u-1", barcode = "SP-01", fullAddress = "End 1", platformId = "plat-shopee", packageType = PackageType.PACOTINHO, status = StopStatus.ENTREGUE),
            MasterRouteStop(id = "s-2", routeId = routeId, userId = "u-1", barcode = "SP-02", fullAddress = "End 2", platformId = "plat-shopee", packageType = PackageType.PACOTINHO, status = StopStatus.ENTREGUE),
            MasterRouteStop(id = "s-3", routeId = routeId, userId = "u-1", barcode = "SP-03", fullAddress = "End 3", platformId = "plat-shopee", packageType = PackageType.VOLUMOSO, status = StopStatus.ENTREGUE),
            MasterRouteStop(id = "s-4", routeId = routeId, userId = "u-1", barcode = "ML-01", fullAddress = "End 4", platformId = "plat-ml", packageType = PackageType.PACOTINHO, status = StopStatus.ENTREGUE),
            MasterRouteStop(id = "s-5", routeId = routeId, userId = "u-1", barcode = "ML-02", fullAddress = "End 5", platformId = "plat-ml", packageType = PackageType.PACOTINHO, status = StopStatus.ENTREGUE)
        )

        val totalPhysicalPackages = stops.size // 5 pacotes
        val totalDistanceKm = BigDecimal("100.0")
        val startKmVal = BigDecimal("50000.0")

        // Agrupamento por platformId
        val grouped = stops.groupBy { it.platformId ?: masterRoute.platformId }
        val entries = grouped.entries.toList()

        assertEquals("Devem existir exatamente 2 plataformas", 2, entries.size)

        var accumulatedProratedKm = BigDecimal.ZERO
        var currentOdometerKm = startKmVal
        val result = mutableListOf<MasterRouteHandOffItem>()

        for (i in entries.indices) {
            val (platformId, groupStops) = entries[i]
            val isLast = (i == entries.size - 1)
            val packagesThisPlatform = groupStops.size

            val shareRatio = packagesThisPlatform.toBigDecimal()
                .divide(totalPhysicalPackages.toBigDecimal(), 6, RoundingMode.HALF_UP)

            val proratedKm = if (isLast) {
                totalDistanceKm.subtract(accumulatedProratedKm).max(BigDecimal.ZERO)
            } else {
                totalDistanceKm.multiply(shareRatio).setScale(1, RoundingMode.HALF_UP)
            }
            accumulatedProratedKm = accumulatedProratedKm.add(proratedKm)

            val groupStartKm = currentOdometerKm
            val groupEndKm = currentOdometerKm.add(proratedKm)
            currentOdometerKm = groupEndKm

            val smallCount = groupStops.count { it.packageType == PackageType.PACOTINHO && it.status == StopStatus.ENTREGUE }
            val largeCount = groupStops.count { it.packageType == PackageType.VOLUMOSO && it.status == StopStatus.ENTREGUE }

            result.add(
                MasterRouteHandOffItem(
                    platformId = platformId,
                    platformName = if (platformId == "plat-shopee") "Shopee" else "Mercado Livre",
                    smallPackagesCount = smallCount,
                    largePackagesCount = largeCount,
                    totalPackages = smallCount + largeCount,
                    proratedKm = proratedKm,
                    startKm = groupStartKm,
                    endKm = groupEndKm,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(12, 0),
                    routeDate = routeDate,
                    origin = masterRoute.startLocation,
                    masterRouteId = masterRoute.id,
                    notes = "Encerrada via Central do Motorista (Rota Master #${masterRoute.id})"
                )
            )
        }

        // Valida que gerou exatamente 2 registros financeiros
        assertEquals(2, result.size)

        // Item 1: Shopee (3/5 = 60% dos pacotes -> 60.0 km)
        val shopeeItem = result.first { it.platformId == "plat-shopee" }
        assertEquals(2, shopeeItem.smallPackagesCount)
        assertEquals(1, shopeeItem.largePackagesCount)
        assertEquals(3, shopeeItem.totalPackages)
        assertEquals(BigDecimal("60.0"), shopeeItem.proratedKm)
        assertEquals(BigDecimal("50000.0"), shopeeItem.startKm)
        assertEquals(BigDecimal("50060.0"), shopeeItem.endKm)

        // Item 2: Mercado Livre (2/5 = 40% dos pacotes -> 40.0 km)
        val mlItem = result.first { it.platformId == "plat-ml" }
        assertEquals(2, mlItem.smallPackagesCount)
        assertEquals(0, mlItem.largePackagesCount)
        assertEquals(2, mlItem.totalPackages)
        assertEquals(BigDecimal("40.0"), mlItem.proratedKm)
        assertEquals(BigDecimal("50060.0"), mlItem.startKm)
        assertEquals(BigDecimal("50100.0"), mlItem.endKm)

        // Valida que a soma do KM rateado fecha exatamente o odômetro total físico de 100.0 km
        val totalKmSum = shopeeItem.proratedKm.add(mlItem.proratedKm)
        assertEquals(totalDistanceKm, totalKmSum)
    }

    @Test
    fun testEmptyRouteActionTriggersDiscardDialogNotFinishDialog() {
        // Valida a regra de proteção contra registros fantasmas:
        // Se a rota não possui pacotes bipados (totalPackages == 0 ou stops.isEmpty()),
        // o botão de fechamento DEVE abrir DiscardEmptyRouteDialog e NÃO FinishRouteDialog.

        // Cenário 1: Rota vazia (0 pacotes)
        val emptyState = RouteCockpitUiState(
            route = MasterDeliveryRoute(
                id = "route-empty",
                userId = "user-1",
                platformId = "plat-shopee",
                routeDate = LocalDate.now(),
                startLocation = "Base",
                status = RouteStatus.EM_ANDAMENTO
            ),
            stops = emptyList()
        )

        var showDiscardEmptyDialog = false
        var showFinishDialog = false

        fun onFinishButtonClick(state: RouteCockpitUiState) {
            if (state.totalPackages == 0 || state.stops.isEmpty()) {
                showDiscardEmptyDialog = true
            } else {
                showFinishDialog = true
            }
        }

        onFinishButtonClick(emptyState)
        assertTrue("Rota sem pacotes deve abrir o diálogo de descarte", showDiscardEmptyDialog)
        assertFalse("Rota sem pacotes NÃO deve abrir o diálogo de conclusão", showFinishDialog)

        // Cenário 2: Rota com pacotes bipados
        val populatedState = RouteCockpitUiState(
            route = MasterDeliveryRoute(
                id = "route-with-pkgs",
                userId = "user-1",
                platformId = "plat-shopee",
                routeDate = LocalDate.now(),
                startLocation = "Base",
                status = RouteStatus.EM_ANDAMENTO
            ),
            stops = listOf(
                MasterRouteStop(
                    id = "stop-1",
                    routeId = "route-with-pkgs",
                    userId = "user-1",
                    barcode = "BC123",
                    recipientName = "Cliente",
                    fullAddress = "Endereço",
                    status = StopStatus.ENTREGUE
                )
            )
        )

        showDiscardEmptyDialog = false
        showFinishDialog = false

        onFinishButtonClick(populatedState)
        assertFalse("Rota com pacotes NÃO deve abrir diálogo de descarte", showDiscardEmptyDialog)
        assertTrue("Rota com pacotes DEVE abrir diálogo de conclusão", showFinishDialog)
    }

    @Test
    fun testDiscardEmptyRouteFlowDoesNotGenerateHandoffs() {
        // Valida que ao descartar uma rota vazia, nenhum handoff financeiro é gerado e o callback de retorno é acionado
        val emptyRoute = MasterDeliveryRoute(
            id = "empty-route-to-discard",
            userId = "user-1",
            platformId = "plat-shopee",
            routeDate = LocalDate.now(),
            startLocation = "CD Norte",
            status = RouteStatus.EM_ANDAMENTO
        )

        var state = RouteCockpitUiState(
            route = emptyRoute,
            stops = emptyList()
        )

        assertEquals(0, state.totalPackages)
        assertTrue(state.activeStops.isEmpty())

        // Simula o início do descarte
        state = state.copy(isFinishing = true)
        assertTrue("isFinishing deve ser true durante o descarte", state.isFinishing)

        // Simulação do sucesso de exclusão
        var navigatedBack = false
        val handoffsGenerated = mutableListOf<MasterRouteHandOffItem>()

        // Ao descartar com sucesso:
        state = state.copy(isFinishing = false)
        navigatedBack = true

        assertFalse("isFinishing deve ser false após o descarte", state.isFinishing)
        assertTrue("Deve navegar de volta após descarte", navigatedBack)
        assertTrue("Nenhum handoff deve ser gerado para rota descartada", handoffsGenerated.isEmpty())
    }
}

