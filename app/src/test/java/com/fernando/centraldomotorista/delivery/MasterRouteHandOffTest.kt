package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.ui.screens.routes.NewRouteUiState
import com.fernando.centraldomotorista.ui.screens.routes.NewRouteViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalTime

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
        assertEquals(BigDecimal("70.00"), state.largePackagesTotal)
        assertEquals(3, state.totalPackagesCount)
    }
}
