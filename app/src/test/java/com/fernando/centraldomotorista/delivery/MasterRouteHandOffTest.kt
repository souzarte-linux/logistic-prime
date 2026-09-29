package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.Route
import com.fernando.centraldomotorista.ui.screens.relatorios.RelatoriosViewModel
import com.fernando.centraldomotorista.ui.screens.routes.NewRouteUiState
import com.fernando.centraldomotorista.ui.screens.routes.NewRouteViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Testes unitários para a validação de cálculo de pacotes e hand-off de dados
 * do cockpit da Rota Master para o formulário financeiro de receitas (NewRouteViewModel).
 */
class MasterRouteHandOffTest {

    private fun createViewModel(): NewRouteViewModel {
        return NewRouteViewModel(loadOnInit = false)
    }

    @Test
    fun testApplyMasterRouteHandOffBasic() {
        val viewModel = createViewModel()

        viewModel.applyMasterRouteHandOff(
            platformId = "plat-shopee-001",
            packagesCount = 42,
            origin = "CD Shopee Cajamar",
            destination = "Zona Sul - São Paulo",
            masterRouteId = "route-uuid-888"
        )

        val state = viewModel.uiState.value
        assertEquals("plat-shopee-001", state.selectedPlatformId)
        assertEquals("42", state.smallPackagesCountText)
        assertEquals(42, state.totalPackagesCount)
        assertEquals("CD Shopee Cajamar", state.origin)
        assertEquals("Zona Sul - São Paulo", state.destination)
        assertEquals("Encerrada via Central do Motorista (Rota Master #route-uuid-888)", state.notesText)
    }

    @Test
    fun testApplyMasterRouteHandOffWithPreExistingUnitPriceCalculatesTotal() {
        val viewModel = createViewModel()

        // Motorista já havia configurado preço unitário padrão R$ 4,50
        viewModel.onSmallPackagesUnitPriceChanged("4,50")
        assertEquals(BigDecimal("0.00"), viewModel.uiState.value.smallPackagesTotal)

        // Hand-off entrega 50 pacotes concluídos
        viewModel.applyMasterRouteHandOff(
            platformId = "plat-mercadolivre",
            packagesCount = 50,
            origin = "Galpão Cajamar",
            destination = "Osasco",
            masterRouteId = "route-ml-123"
        )

        val state = viewModel.uiState.value
        assertEquals("50", state.smallPackagesCountText)
        assertEquals(50, state.totalPackagesCount)
        // 50 * 4.50 = 225.00
        assertEquals(BigDecimal("225.00"), state.smallPackagesTotal)
        assertEquals(BigDecimal("225.00"), state.totalAmount)
    }

    @Test
    fun testApplyMasterRouteHandOffWithZeroCountPreservesExistingValues() {
        val viewModel = createViewModel()
        viewModel.onSmallPackagesCountChanged("15")

        viewModel.applyMasterRouteHandOff(
            platformId = "plat-amazon",
            packagesCount = 0,
            origin = null,
            destination = null,
            masterRouteId = null
        )

        val state = viewModel.uiState.value
        assertEquals("plat-amazon", state.selectedPlatformId)
        // packagesCount = 0 preserva os 15 pacotes anteriores
        assertEquals("15", state.smallPackagesCountText)
        assertEquals(15, state.totalPackagesCount)
    }

    @Test
    fun testSubsequentEditsAfterHandOffRecalculatesTotalsCorrectly() {
        val viewModel = createViewModel()

        // 1. Recebe hand-off de 38 pacotinhos
        viewModel.applyMasterRouteHandOff(
            platformId = "plat-loggi",
            packagesCount = 38,
            origin = "Crossdocking Barra Funda",
            destination = "Pinheiros",
            masterRouteId = "route-loggi-99"
        )

        // 2. Define o valor por pacotinho: R$ 3,80 -> 38 * 3.80 = 144.40
        viewModel.onSmallPackagesUnitPriceChanged("3,80")
        assertEquals(BigDecimal("144.40"), viewModel.uiState.value.smallPackagesTotal)

        // 3. Acrescenta 2 pacotes volumosos a R$ 25,00 cada -> 2 * 25.00 = 50.00
        viewModel.onLargePackagesCountChanged("2")
        viewModel.onLargePackageSingleUnitPriceChanged("25,00")
        assertEquals(BigDecimal("50.00"), viewModel.uiState.value.largePackagesTotal)

        // 4. Adiciona gorjeta de R$ 15,00 e bônus de R$ 30,00
        viewModel.onTipChanged("15,00")
        viewModel.onBonusChanged("30,00")

        val state = viewModel.uiState.value
        // Total de pacotes: 38 pequenos + 2 volumosos = 40 pacotes
        assertEquals(40, state.totalPackagesCount)

        // Total financeiro: 144.40 + 50.00 + 15.00 + 30.00 = 239.40
        assertEquals(BigDecimal("239.40"), state.totalAmount)
    }

    @Test
    fun testUiStateWorkedMinutesCalculation() {
        val stateSameDay = NewRouteUiState(
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(16, 30),
            breakMinutesText = "45"
        )
        // 8h30 = 510 min - 45 min = 465 min (7h 45min)
        assertEquals(465, stateSameDay.workedMinutes)
        assertEquals("07h 45min", stateSameDay.workedTimeFormatted)

        // Virada de dia (noturno)
        val stateOvernight = NewRouteUiState(
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(4, 0),
            breakMinutesText = "30"
        )
        // 22h até 4h = 6h = 360 min - 30 min = 330 min (5h 30min)
        assertEquals(330, stateOvernight.workedMinutes)
        assertEquals("05h 30min", stateOvernight.workedTimeFormatted)
    }

    @Test
    fun testUiStateDistanceCalculationFromKm() {
        val viewModel = createViewModel()
        viewModel.onStartKmChanged("100250,5")
        viewModel.onEndKmChanged("100375,8")

        val state = viewModel.uiState.value
        // 100375.8 - 100250.5 = 125.3 km
        assertEquals("125,3", state.distanceKmText)
    }

    @Test
    fun testIndividualLargePackagesPricingSum() {
        val viewModel = createViewModel()
        viewModel.onLargePackagesIndividualPricesConfirmed(
            listOf(
                BigDecimal("20.50"),
                BigDecimal("35.00"),
                BigDecimal("14.50")
            )
        )

        val state = viewModel.uiState.value
        assertEquals("3", state.largePackagesCountText)
        assertTrue(state.isLargePackageIndividualValue)
        assertEquals(3, state.totalPackagesCount)
    }

    @Test
    fun testApplyMasterRouteHandOffMultiPlatformWithSplitAndProratedKm() {
        val viewModel = createViewModel()
        viewModel.onSmallPackagesUnitPriceChanged("4,00")
        viewModel.onLargePackageSingleUnitPriceChanged("18,00")

        val routeDate = java.time.LocalDate.of(2026, 9, 29)
        val startTime = java.time.LocalTime.of(8, 30)
        val endTime = java.time.LocalTime.of(12, 30)

        viewModel.applyMasterRouteHandOff(
            platformId = "plat-shopee",
            smallPackagesCount = 25,
            largePackagesCount = 3,
            startKm = BigDecimal("150200.0"),
            endKm = BigDecimal("150265.5"),
            distanceKm = BigDecimal("65.5"),
            routeDate = routeDate,
            startTime = startTime,
            endTime = endTime,
            origin = "CD Shopee",
            destination = "Região Centro",
            masterRouteId = "master-route-456",
            notes = "Encerrada via Central do Motorista (Rota Master #master-route-456) - Shopee"
        )

        val state = viewModel.uiState.value
        assertEquals("plat-shopee", state.selectedPlatformId)
        assertEquals("25", state.smallPackagesCountText)
        assertEquals(BigDecimal("100.00"), state.smallPackagesTotal) // 25 * 4.00
        assertEquals("3", state.largePackagesCountText)
        assertEquals(BigDecimal("54.00"), state.largePackagesTotal) // 3 * 18.00
        assertEquals(28, state.totalPackagesCount) // 25 + 3
        assertEquals(BigDecimal("154.00"), state.totalAmount) // 100 + 54
        assertEquals("65,5", state.distanceKmText)
        assertEquals("150200,0", state.startKmText)
        assertEquals("150265,5", state.endKmText)
        assertEquals(routeDate, state.selectedDate)
        assertEquals(startTime, state.startTime)
        assertEquals(endTime, state.endTime)
        assertEquals("Encerrada via Central do Motorista (Rota Master #master-route-456) - Shopee", state.notesText)
    }

    @Test
    fun testProratedKmSumsExactlyToPhysicalRouteTotal() {
        // Simulação do algoritmo de rateio multi-plataforma
        val totalRouteDistanceKm = BigDecimal("157.3")
        val groups = listOf(17, 33, 50) // soma = 100 pacotes
        val totalPhysicalPackages = groups.sum()

        var accumulatedProratedKm = BigDecimal.ZERO
        val proratedKms = mutableListOf<BigDecimal>()

        for (i in groups.indices) {
            val isLast = (i == groups.size - 1)
            val shareRatio = groups[i].toBigDecimal().divide(totalPhysicalPackages.toBigDecimal(), 6, RoundingMode.HALF_UP)
            val prorated = if (isLast) {
                totalRouteDistanceKm.subtract(accumulatedProratedKm)
            } else {
                totalRouteDistanceKm.multiply(shareRatio).setScale(1, RoundingMode.HALF_UP)
            }
            accumulatedProratedKm = accumulatedProratedKm.add(prorated)
            proratedKms.add(prorated)
        }

        // Verifica que a soma dos rateios bate exatamente com o total físico
        val sumOfProrated = proratedKms.fold(BigDecimal.ZERO, BigDecimal::add)
        assertEquals(totalRouteDistanceKm, sumOfProrated)
        assertEquals(3, proratedKms.size)
    }

    @Test
    fun testRelatoriosTotalKmNotInflatedByMultiPlatformHandoff() {
        val testZone = ZoneOffset.UTC
        val fixedToday = java.time.LocalDate.of(2026, 9, 29)
        val testScope = CoroutineScope(Dispatchers.Default)

        val vm = RelatoriosViewModel(
            externalScope = testScope,
            observeDataSync = false,
            loadOnInit = false,
            zone = testZone,
            fixedToday = fixedToday
        )

        // Rota física total de 120.0 km com 100 pacotes, dividida entre Shopee (40 pacotes = 48.0 km) e ML (60 pacotes = 72.0 km)
        val routeShopee = Route(
            id = "r-shopee-synth",
            userId = "user-1",
            platformId = "plat-shopee",
            origin = "CD Cajamar",
            destination = "Zona Sul",
            amount = BigDecimal("160.00"),
            distanceKm = BigDecimal("48.0"),
            packageCount = 40,
            smallPackagesCount = 40,
            occurredAt = OffsetDateTime.of(2026, 9, 29, 10, 0, 0, 0, testZone)
        )

        val routeMercadoLivre = Route(
            id = "r-ml-synth",
            userId = "user-1",
            platformId = "plat-ml",
            origin = "CD Cajamar",
            destination = "Zona Sul",
            amount = BigDecimal("240.00"),
            distanceKm = BigDecimal("72.0"),
            packageCount = 60,
            smallPackagesCount = 60,
            occurredAt = OffsetDateTime.of(2026, 9, 29, 10, 0, 0, 0, testZone)
        )

        vm.setTestData(routes = listOf(routeShopee, routeMercadoLivre))

        val stats = vm.uiState.value.stats
        // Total de KM deve somar exatamente 120.0 km (48.0 + 72.0) e NÃO inflar/dobrar para 240.0 km
        assertEquals(0, BigDecimal("120.0").compareTo(stats.totalKm))
        assertEquals(2, stats.routeCount)
        assertEquals(100, stats.totalPackages)
    }

    @Test
    fun testHandOffGeneratesOneFinancialRoutePerPlatform() {
        // Simulação do recebimento de N handoffs de plataformas diferentes
        val handOffItems = listOf(
            com.fernando.centraldomotorista.ui.screens.routes.master.MasterRouteHandOffItem(
                platformId = "plat-shopee",
                platformName = "Shopee Xpress",
                smallPackagesCount = 28,
                largePackagesCount = 2,
                totalPackages = 30,
                proratedKm = BigDecimal("45.0"),
                startKm = BigDecimal("100000.0"),
                endKm = BigDecimal("100045.0"),
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(10, 30),
                routeDate = java.time.LocalDate.of(2026, 9, 29),
                origin = "CD Shopee",
                masterRouteId = "master-99",
                notes = "Encerrada via Central do Motorista (Rota Master #master-99) - Shopee Xpress"
            ),
            com.fernando.centraldomotorista.ui.screens.routes.master.MasterRouteHandOffItem(
                platformId = "plat-mercadolivre",
                platformName = "Mercado Livre",
                smallPackagesCount = 15,
                largePackagesCount = 0,
                totalPackages = 15,
                proratedKm = BigDecimal("25.0"),
                startKm = BigDecimal("100045.0"),
                endKm = BigDecimal("100070.0"),
                startTime = LocalTime.of(10, 30),
                endTime = LocalTime.of(12, 0),
                routeDate = java.time.LocalDate.of(2026, 9, 29),
                origin = "CD Shopee",
                masterRouteId = "master-99",
                notes = "Encerrada via Central do Motorista (Rota Master #master-99) - Mercado Livre"
            )
        )

        // Cada item deve ser transformado em 1 formulário financeiro NewRouteViewModel independente
        val createdViewModels = handOffItems.map { item ->
            val vm = createViewModel()
            vm.applyMasterRouteHandOff(
                platformId = item.platformId,
                smallPackagesCount = item.smallPackagesCount,
                largePackagesCount = item.largePackagesCount,
                startKm = item.startKm,
                endKm = item.endKm,
                distanceKm = item.proratedKm,
                routeDate = item.routeDate,
                startTime = item.startTime,
                endTime = item.endTime,
                origin = item.origin,
                destination = null,
                masterRouteId = item.masterRouteId,
                notes = item.notes
            )
            vm
        }

        assertEquals(2, createdViewModels.size)

        val vmShopee = createdViewModels[0].uiState.value
        assertEquals("plat-shopee", vmShopee.selectedPlatformId)
        assertEquals("28", vmShopee.smallPackagesCountText)
        assertEquals("2", vmShopee.largePackagesCountText)
        assertEquals(30, vmShopee.totalPackagesCount)
        assertEquals("45,0", vmShopee.distanceKmText)
        assertEquals("Encerrada via Central do Motorista (Rota Master #master-99) - Shopee Xpress", vmShopee.notesText)

        val vmMl = createdViewModels[1].uiState.value
        assertEquals("plat-mercadolivre", vmMl.selectedPlatformId)
        assertEquals("15", vmMl.smallPackagesCountText)
        assertEquals("", vmMl.largePackagesCountText)
        assertEquals(15, vmMl.totalPackagesCount)
        assertEquals("25,0", vmMl.distanceKmText)
        assertEquals("Encerrada via Central do Motorista (Rota Master #master-99) - Mercado Livre", vmMl.notesText)
    }
}
