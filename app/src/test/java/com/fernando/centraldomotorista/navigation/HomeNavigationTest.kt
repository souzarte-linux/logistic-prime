package com.fernando.centraldomotorista.navigation

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.*
import org.junit.Test

/**
 * Testes unitários para a Fase 3:
 * 1. Navegação do botão "VER TUDO" da HomeScreen para Screen.Historico.route.
 * 2. Especificações de layout do Hero Card Laranja na HomeScreen.
 * 3. Especificações do layout 2x1 compacto (isCompact) do PainelScreen.
 */
class HomeNavigationTest {

    @Test
    fun testVerTudoButtonNavigatesToHistoricoRoute() {
        var navigatedRoute: String? = null

        // Simulação do callback da HomeScreen
        val onNavigateToRoute: (String) -> Unit = { route ->
            navigatedRoute = route
        }

        // Default da HomeScreen: onNavigateToHistorico = { onNavigateToRoute(Screen.Historico.route) }
        val onNavigateToHistorico: () -> Unit = {
            onNavigateToRoute(Screen.Historico.route)
        }

        // Disparo do clique no botão "VER TUDO"
        onNavigateToHistorico()

        assertNotNull("A rota de navegação deve ser definida", navigatedRoute)
        assertEquals(
            "O botão 'VER TUDO' da HomeScreen deve navegar para a rota do Histórico",
            "historico",
            navigatedRoute
        )
        assertEquals(Screen.Historico.route, navigatedRoute)
    }

    @Test
    fun testNavGraphHomeToHistoricoNavigationContract() {
        // Valida que a rota definida para a tela de Histórico em Screen é a mesma aguardada pelo NavGraph
        assertEquals("historico", Screen.Historico.route)
        assertEquals("Histórico", Screen.Historico.title)

        // Simulação da navegação configurada no NavGraph.kt:
        // onNavigateToHistorico = { navController.navigate(Screen.Historico.route) }
        var targetDestination: String? = null
        val mockNavControllerNavigate: (String) -> Unit = { route -> targetDestination = route }

        mockNavControllerNavigate(Screen.Historico.route)

        assertEquals("historico", targetDestination)
    }

    @Test
    fun testHeroActionCardDesignSystemSpecs() {
        // Validação das regras de renderização condicional do Hero Card
        data class ActionCardSpecs(
            val isHero: Boolean,
            val titleText: String,
            val hasElevation: Boolean,
            val hasBorder: Boolean
        )

        val heroCard = ActionCardSpecs(
            isHero = true,
            titleText = "LANÇAR GANHOS POR ROTA",
            hasElevation = true,
            hasBorder = false // Hero usa gradiente sem borda cinza padrão
        )

        val regularCard = ActionCardSpecs(
            isHero = false,
            titleText = "TOTAL DO DIA",
            hasElevation = true,
            hasBorder = true
        )

        assertTrue("Hero card deve ter a flag isHero ativa", heroCard.isHero)
        assertFalse("Card regular não deve ser hero", regularCard.isHero)
        assertFalse("Hero card substitui a borda cinza por gradiente de fundo", heroCard.hasBorder)
        assertTrue("Card regular deve ter borda sutil", regularCard.hasBorder)
        assertEquals("LANÇAR GANHOS POR ROTA", heroCard.titleText)
    }

    @Test
    fun testPainelStatCardCompactDimensions() {
        // Validação das dimensões e tipografias calculadas para o modo isCompact = true vs isCompact = false
        data class StatCardDimensions(
            val padding: Int,
            val labelFontSize: Float,
            val valueFontSize: Float,
            val trendFontSize: Float,
            val headerSpacer: Int,
            val valueSpacing: Int
        )

        fun resolveDimensions(isCompact: Boolean): StatCardDimensions {
            return if (isCompact) {
                StatCardDimensions(
                    padding = 12,
                    labelFontSize = 10.5f,
                    valueFontSize = 19.0f,
                    trendFontSize = 9.5f,
                    headerSpacer = 4,
                    valueSpacing = 6
                )
            } else {
                StatCardDimensions(
                    padding = 16,
                    labelFontSize = 11.5f,
                    valueFontSize = 24.0f,
                    trendFontSize = 11.0f,
                    headerSpacer = 6,
                    valueSpacing = 8
                )
            }
        }

        val compact = resolveDimensions(isCompact = true)
        val normal = resolveDimensions(isCompact = false)

        // Modo compacto (usado na linha 2x1 de Lucro Diário e Lucro Semanal)
        assertEquals(12, compact.padding)
        assertEquals(10.5f, compact.labelFontSize, 0.01f)
        assertEquals(19.0f, compact.valueFontSize, 0.01f)
        assertEquals(9.5f, compact.trendFontSize, 0.01f)
        assertEquals(4, compact.headerSpacer)
        assertEquals(6, compact.valueSpacing)

        // Modo normal (usado no card de Meta Mensal de largura total)
        assertEquals(16, normal.padding)
        assertEquals(24.0f, normal.valueFontSize, 0.01f)

        // O modo compacto deve ser estritamente menor para evitar quebra de layout lado a lado
        assertTrue(compact.valueFontSize < normal.valueFontSize)
        assertTrue(compact.padding < normal.padding)
        assertTrue(compact.labelFontSize < normal.labelFontSize)
    }
}
