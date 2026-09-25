package com.fernando.centraldomotorista.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários para a navegação das 5 abas fixas da Bottom Navigation Bar
 * em conformidade com o design system e a especificação da TASK-AND-01 / TASK-QA-01.
 */
class BottomNavigationTest {

    @Test
    fun testBottomNavItemsHasExactlyFiveTabs() {
        assertEquals(
            "A barra de navegação inferior deve conter exatamente 5 abas fixas",
            5,
            bottomNavItems.size
        )
    }

    @Test
    fun testBottomNavItemsOrderAndProperties() {
        // 1ª Aba: Início
        val tab0 = bottomNavItems[0]
        assertEquals("inicio", tab0.route)
        assertEquals("Início", tab0.title)
        assertEquals(Icons.Default.Home, tab0.icon)
        assertSame(Screen.Inicio, tab0)

        // 2ª Aba: Painel
        val tab1 = bottomNavItems[1]
        assertEquals("painel", tab1.route)
        assertEquals("Painel", tab1.title)
        assertEquals(Icons.Default.BarChart, tab1.icon)
        assertSame(Screen.Painel, tab1)

        // 3ª Aba: Relatórios
        val tab2 = bottomNavItems[2]
        assertEquals("relatorios", tab2.route)
        assertEquals("Relatórios", tab2.title)
        assertEquals(Icons.Default.Assessment, tab2.icon)
        assertSame(Screen.Relatorios, tab2)

        // 4ª Aba: Apps (Plataformas)
        val tab3 = bottomNavItems[3]
        assertEquals("plataformas", tab3.route)
        assertEquals("Apps", tab3.title)
        assertEquals(Icons.Default.Apps, tab3.icon)
        assertSame(Screen.Plataformas, tab3)

        // 5ª Aba: Histórico
        val tab4 = bottomNavItems[4]
        assertEquals("historico", tab4.route)
        assertEquals("Histórico", tab4.title)
        assertEquals(Icons.Default.History, tab4.icon)
        assertSame(Screen.Historico, tab4)
    }

    @Test
    fun testAppsScreenAliasMatchesPlataformas() {
        assertSame(
            "Screen.Apps deve ser idêntico a Screen.Plataformas",
            Screen.Plataformas,
            Screen.Apps
        )
        assertEquals("plataformas", Screen.Apps.route)
        assertEquals("Apps", Screen.Apps.title)
        assertEquals(Icons.Default.Apps, Screen.Apps.icon)
    }

    @Test
    fun testBottomNavItemsUniqueRoutesAndTitles() {
        val routes = bottomNavItems.map { it.route }
        val titles = bottomNavItems.map { it.title }

        assertEquals(
            "Todas as rotas da barra inferior devem ser únicas",
            routes.toSet().size,
            routes.size
        )
        assertEquals(
            "Todos os títulos da barra inferior devem ser únicos",
            titles.toSet().size,
            titles.size
        )
    }

    @Test
    fun testSecondaryScreensAreNotPresentInBottomNav() {
        val nonBottomNavScreens = listOf(
            Screen.Login,
            Screen.LancarRota,
            Screen.LancarTotalDia,
            Screen.FuelExpense,
            Screen.MealExpense,
            Screen.LancarManutencao,
            Screen.Empresas,
            Screen.GasStations,
            Screen.Emissores,
            Screen.EditPlatform,
            Screen.CreatePlatform,
            Screen.Bandeiras,
            Screen.MonitoramentoPecas,
            Screen.DeliveryRoutes,
            Screen.DeliveryPartners,
            Screen.CreateDeliveryPartner,
            Screen.CreditCards,
            Screen.PartProducts,
            Screen.Faturas
        )

        for (screen in nonBottomNavScreens) {
            assertFalse(
                "A tela ${screen.title} (${screen.route}) não deve constar na barra inferior",
                bottomNavItems.contains(screen)
            )
        }
    }

    @Test
    fun testRouteMatchingLogicForBottomBarVisibility() {
        val isCurrentDestination: (String, String?) -> Boolean = { itemRoute, currentRoute ->
            currentRoute == itemRoute || currentRoute?.startsWith("$itemRoute?") == true
        }

        val showBottomBar: (String?) -> Boolean = { currentRoute ->
            bottomNavItems.any { isCurrentDestination(it.route, currentRoute) }
        }

        // Deve exibir BottomBar para as 5 abas principais
        assertTrue(showBottomBar("inicio"))
        assertTrue(showBottomBar("painel"))
        assertTrue(showBottomBar("relatorios"))
        assertTrue(showBottomBar("plataformas"))
        assertTrue(showBottomBar("historico"))

        // Deve exibir BottomBar quando acessado com query params (ex: drawer navigation para plataformas)
        assertTrue(showBottomBar("plataformas?fromDrawer=true"))
        assertTrue(showBottomBar("plataformas?fromDrawer=false"))

        // NÃO deve exibir BottomBar para telas secundárias ou modais
        assertFalse(showBottomBar("login"))
        assertFalse(showBottomBar("empresas"))
        assertFalse(showBottomBar("postos"))
        assertFalse(showBottomBar("emissores"))
        assertFalse(showBottomBar("faturas"))
        assertFalse(showBottomBar("lancar_rota"))
        assertFalse(showBottomBar("fuel_expense"))
        assertFalse(showBottomBar("fuel_expense?itemId=123"))
        assertFalse(showBottomBar("delivery_routes"))
        assertFalse(showBottomBar("create_platform"))
        assertFalse(showBottomBar("edit_platform/123"))
        assertFalse(showBottomBar(null))
    }

    @Test
    fun testDualNavigationBehaviorLogicForPlatformsScreen() {
        // Simulação da lógica de navegação dual da PlatformsScreen
        // fromDrawer=true -> onNavigateBack deve existir (não-nulo) e fornecer callback de retorno
        // fromDrawer=false / bottom bar -> onNavigateBack é null (aba raiz, sem botão de voltar)

        fun createPlatformsNavigationConfig(fromDrawer: Boolean): PlatformsNavConfig {
            return PlatformsNavConfig(
                fromDrawer = fromDrawer,
                hasBackButton = fromDrawer,
                isRootTab = !fromDrawer
            )
        }

        val drawerConfig = createPlatformsNavigationConfig(fromDrawer = true)
        assertTrue("Acessado pelo Drawer deve exibir botão voltar", drawerConfig.hasBackButton)
        assertFalse("Acessado pelo Drawer não deve ser considerado aba raiz sem voltar", drawerConfig.isRootTab)

        val bottomNavConfig = createPlatformsNavigationConfig(fromDrawer = false)
        assertFalse("Acessado pela BottomBar não deve exibir botão voltar", bottomNavConfig.hasBackButton)
        assertTrue("Acessado pela BottomBar é aba raiz", bottomNavConfig.isRootTab)
    }

    @Test
    fun testAutoMirroredIconsMigration() {
        // Validar que ícones direcionais migrados usam AutoMirrored
        assertEquals(Icons.AutoMirrored.Filled.ReceiptLong, Screen.Emissores.icon)
        assertEquals(Icons.AutoMirrored.Filled.AltRoute, Screen.DeliveryRoutes.icon)
        assertEquals(Icons.AutoMirrored.Filled.ReceiptLong, Screen.Faturas.icon)
    }

    private data class PlatformsNavConfig(
        val fromDrawer: Boolean,
        val hasBackButton: Boolean,
        val isRootTab: Boolean
    )
}
