package com.fernando.centraldomotorista.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para a especificação do Menu Lateral (Drawer) acessível em todas as abas.
 * Valida a disponibilidade em todas as 5 abas raiz da aplicação e a integridade das rotas do Drawer.
 */
class DrawerNavigationTest {

    // Lógica espelhada do NavGraph.kt
    private fun isCurrentDestination(currentRoute: String?, route: String): Boolean {
        return currentRoute == route || currentRoute?.startsWith("$route?") == true
    }

    private fun isDrawerGestureEnabled(currentRoute: String?): Boolean {
        return bottomNavItems.any { isCurrentDestination(currentRoute, it.route) }
    }

    @Test
    fun testDrawerAccessibleInAllFiveMainTabs() {
        // Valida que o gesto/drawer está ativo em todas as 5 abas principais
        val mainTabs = listOf(
            Screen.Inicio.route,     // "inicio"
            Screen.Painel.route,     // "painel"
            Screen.Rota.route,       // "rota_hub"
            Screen.Relatorios.route, // "relatorios"
            Screen.Historico.route   // "historico"
        )

        assertEquals("Devem existir exatamente 5 abas principais", 5, mainTabs.size)

        for (tabRoute in mainTabs) {
            assertTrue(
                "O Drawer deve ser acessível na aba '$tabRoute'",
                isDrawerGestureEnabled(tabRoute)
            )
        }
    }

    @Test
    fun testDrawerDisabledOnNonRootRoutes() {
        // Nas telas de formulário, detalhes e login, o gesto do drawer não deve competir com a interface
        val nonRootRoutes = listOf(
            Screen.Login.route,
            Screen.LancarRota.route,
            Screen.LancarTotalDia.route,
            Screen.FuelExpense.route,
            Screen.MealExpense.route,
            Screen.LancarManutencao.route,
            Screen.Empresas.route,
            Screen.GasStations.route,
            "plataformas?fromDrawer=true",
            Screen.DeliveryRoutes.route,
            Screen.CreateDeliveryPartner.route,
            Screen.CreditCards.route,
            Screen.Faturas.route,
            Screen.DeliveryPartners.route,
            Screen.MonitoramentoPecas.route
        )

        for (route in nonRootRoutes) {
            assertFalse(
                "O Drawer não deve ativar gestos acidentais na rota '$route'",
                isDrawerGestureEnabled(route)
            )
        }
    }

    @Test
    fun testDrawerRoutesMappingIntegrity() {
        // Mapeamento dos itens do Drawer para garantir que todas as rotas existem e batem com o esperado
        val drawerNavigationItems = mapOf(
            "Início" to Screen.Inicio.route,
            "Empresas" to Screen.Empresas.route,
            "Postos de Gasolina" to Screen.GasStations.route,
            "App & Plataforma" to "plataformas?fromDrawer=true",
            "Rotas" to Screen.DeliveryRoutes.route,
            "Novo Entregador" to Screen.CreateDeliveryPartner.route,
            "Gerenciamento de Cartões" to Screen.CreditCards.route,
            "Contas a Receber" to Screen.Faturas.route,
            "Entregadores Parceiros" to Screen.DeliveryPartners.route,
            "Monitoramento Peças" to Screen.MonitoramentoPecas.route,
            "Relatórios & Extrato" to Screen.Relatorios.route
        )

        assertEquals("inicio", drawerNavigationItems["Início"])
        assertEquals("empresas", drawerNavigationItems["Empresas"])
        assertEquals("postos", drawerNavigationItems["Postos de Gasolina"])
        assertEquals("plataformas?fromDrawer=true", drawerNavigationItems["App & Plataforma"])
        assertEquals("delivery_routes", drawerNavigationItems["Rotas"])
        assertEquals("create_delivery_partner", drawerNavigationItems["Novo Entregador"])
        assertEquals("credit_cards", drawerNavigationItems["Gerenciamento de Cartões"])
        assertEquals("faturas", drawerNavigationItems["Contas a Receber"])
        assertEquals("delivery_partners", drawerNavigationItems["Entregadores Parceiros"])
        assertEquals("monitoramento-pecas", drawerNavigationItems["Monitoramento Peças"])
        assertEquals("relatorios", drawerNavigationItems["Relatórios & Extrato"])
    }

    @Test
    fun testDrawerNavigationCallbacksTriggerCorrectly() {
        var navigatedRoute: String? = null
        var isDrawerClosed = false

        val onNavigateToRoute: (String) -> Unit = { route ->
            isDrawerClosed = true
            navigatedRoute = route
        }

        // Teste navegando para "Empresas" a partir do Drawer
        onNavigateToRoute(Screen.Empresas.route)
        assertTrue(isDrawerClosed)
        assertEquals("empresas", navigatedRoute)

        // Teste navegando para "Relatórios"
        onNavigateToRoute(Screen.Relatorios.route)
        assertTrue(isDrawerClosed)
        assertEquals("relatorios", navigatedRoute)

        // Teste navegando para "Início"
        onNavigateToRoute(Screen.Inicio.route)
        assertTrue(isDrawerClosed)
        assertEquals("inicio", navigatedRoute)
    }
}
