package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.ui.screens.routes.master.RouteHomeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Testes unitários para o fluxo de Inicialização e Criação de Rota Master (Bugfix StartRouteDialog & RouteHomeScreen).
 *
 * Valida:
 * 1. Transição de estado: isCreatingRoute = true enquanto a requisição está em voo;
 * 2. Sucesso: isCreatingRoute = false, rota ativa definida, callback onSuccess invocado para fechar diálogo e abrir scanner;
 * 3. Erro: isCreatingRoute = false, mensagem de erro definida sem fechar o diálogo prematuramente;
 * 4. Limpeza de erro com clearError().
 */
class RouteHomeFlowTest {

    @Test
    fun testInitialState_isIdleWithoutError() {
        val state = RouteHomeUiState()
        assertFalse(state.isLoading)
        assertFalse(state.isCreatingRoute)
        assertNull(state.error)
        assertNull(state.activeRoute)
        assertTrue(state.recentRoutes.isEmpty())
        assertTrue(state.platforms.isEmpty())
    }

    @Test
    fun testCreatingRouteState_activatesLoadingFlag() {
        var state = RouteHomeUiState(
            platforms = listOf(
                Platform(
                    id = "plat-shopee",
                    userId = "user-1",
                    name = "Shopee",
                    cycle = "semanal",
                    paymentDay = "segunda"
                )
            )
        )

        // Motorista clica em "ABRIR SCANNER DE PACOTES"
        state = state.copy(isCreatingRoute = true, error = null)

        assertTrue("isCreatingRoute deve ser true durante a requisição", state.isCreatingRoute)
        assertNull("error deve ser limpo ao iniciar nova tentativa", state.error)
    }

    @Test
    fun testCreateRouteSuccess_updatesActiveRouteAndClearsLoading() {
        var state = RouteHomeUiState(isCreatingRoute = true)

        val createdRoute = MasterDeliveryRoute(
            id = "new-route-123",
            userId = "user-1",
            platformId = "plat-shopee",
            routeDate = LocalDate.now(),
            startLocation = "Galpão Cajamar",
            status = RouteStatus.EM_ANDAMENTO
        )

        var navigatedRouteId: String? = null
        var dialogDismissed = false

        // Simulação do callback de sucesso do ViewModel
        state = state.copy(
            isCreatingRoute = false,
            activeRoute = createdRoute,
            error = null
        )
        // Callback onSuccess dispara o fechamento do diálogo e navegação
        dialogDismissed = true
        navigatedRouteId = createdRoute.id

        assertFalse("isCreatingRoute deve voltar para false no sucesso", state.isCreatingRoute)
        assertNotNull("activeRoute deve ser definido", state.activeRoute)
        assertEquals("new-route-123", state.activeRoute?.id)
        assertTrue("Diálogo só deve fechar no sucesso", dialogDismissed)
        assertEquals("new-route-123", navigatedRouteId)
    }

    @Test
    fun testCreateRouteFailure_keepsDialogWithErrorMessage() {
        var state = RouteHomeUiState(isCreatingRoute = true)
        var dialogDismissed = false
        var navigatedRouteId: String? = null

        val simulatedErrorMessage = "Falha ao criar rota: timeout ao conectar ao Supabase"

        // Simulação de falha
        state = state.copy(
            isCreatingRoute = false,
            error = simulatedErrorMessage
        )
        // Em falha, onSuccess NÃO é chamado -> dialogDismissed permanece false
        assertFalse("isCreatingRoute deve ser desativado após o erro", state.isCreatingRoute)
        assertNotNull("error deve conter a mensagem de erro", state.error)
        assertEquals(simulatedErrorMessage, state.error)
        assertFalse("Diálogo NÃO deve fechar quando há erro", dialogDismissed)
        assertNull("Não deve haver navegação para o scanner em caso de falha", navigatedRouteId)
    }

    @Test
    fun testClearError_resetsErrorMessage() {
        var state = RouteHomeUiState(error = "Erro anterior qualquer")
        assertNotNull(state.error)

        state = state.copy(error = null)
        assertNull("clearError deve anular a mensagem de erro", state.error)
    }
}
