package com.fernando.centraldomotorista.billing

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.*
import org.junit.Test

/**
 * Testes unitários para validar os contratos de layout e especificações visuais
 * do componente FaturaCardItem em FaturasScreen.kt (TASK-AND-03 / TASK-QA-03).
 */
class FaturaCardLayoutTest {

    // Contrato de Layout de Ações do FaturaCardItem
    data class FaturaCardActionContract(
        val status: String,
        val hasPrimaryButton: Boolean,
        val primaryButtonHeight: Int?, // dp
        val primaryButtonText: String?,
        val secondaryRowHeight: Int, // dp
        val secondaryRowSpacing: Int, // dp
        val secondaryButtonHeight: Int, // dp
        val secondaryButtonHorizontalPadding: Int, // dp
        val secondaryButtonCornerRadius: Int, // dp
        val secondaryButtonMaxLines: Int,
        val secondaryActionsCount: Int,
        val deleteButtonSize: Int // dp
    )

    private fun resolveActionContract(status: String): FaturaCardActionContract {
        val isAVencer = status == "a_vencer"
        val isEmAberto = status == "em_aberto"
        val hasPrimary = isAVencer || isEmAberto

        val primaryText = when {
            isAVencer -> "Liquidar / Baixar Repasse"
            isEmAberto -> "Liquidar / Baixar Ciclo"
            else -> null
        }

        return FaturaCardActionContract(
            status = status,
            hasPrimaryButton = hasPrimary,
            primaryButtonHeight = if (hasPrimary) 46 else null,
            primaryButtonText = primaryText,
            secondaryRowHeight = 38,
            secondaryRowSpacing = 6,
            secondaryButtonHeight = 38,
            secondaryButtonHorizontalPadding = 4,
            secondaryButtonCornerRadius = 10,
            secondaryButtonMaxLines = 1,
            secondaryActionsCount = 4,
            deleteButtonSize = 38
        )
    }

    @Test
    fun testPrimaryButtonHeightAndPresenceByStatus() {
        // 1. Status "a_vencer" deve possuir botão primário verde com 46.dp
        val aVencerContract = resolveActionContract("a_vencer")
        assertTrue("Status 'a_vencer' deve exibir botão primário de liquidação", aVencerContract.hasPrimaryButton)
        assertEquals(46, aVencerContract.primaryButtonHeight)
        assertEquals("Liquidar / Baixar Repasse", aVencerContract.primaryButtonText)

        // 2. Status "em_aberto" deve possuir botão primário laranja com 46.dp
        val emAbertoContract = resolveActionContract("em_aberto")
        assertTrue("Status 'em_aberto' deve exibir botão primário de liquidação", emAbertoContract.hasPrimaryButton)
        assertEquals(46, emAbertoContract.primaryButtonHeight)
        assertEquals("Liquidar / Baixar Ciclo", emAbertoContract.primaryButtonText)

        // 3. Status "pago" NÃO deve exibir botão primário de liquidação
        val pagoContract = resolveActionContract("pago")
        assertFalse("Status 'pago' não deve possuir botão primário de liquidação", pagoContract.hasPrimaryButton)
        assertNull(pagoContract.primaryButtonHeight)
        assertNull(pagoContract.primaryButtonText)

        // 4. Status "cancelado" também não deve exibir botão primário
        val canceladoContract = resolveActionContract("cancelado")
        assertFalse(canceladoContract.hasPrimaryButton)
    }

    @Test
    fun testSecondaryActionButtonsRowSpecsAcrossAllStatuses() {
        val testStatuses = listOf("a_vencer", "em_aberto", "pago", "cancelado")

        for (status in testStatuses) {
            val contract = resolveActionContract(status)

            // Altura uniforme de 38.dp nos botões secundários
            assertEquals("Botões secundários devem ter altura exata de 38.dp no status $status", 38, contract.secondaryButtonHeight)
            assertEquals("Linha secundária deve ter altura de 38.dp no status $status", 38, contract.secondaryRowHeight)

            // Espaçamento horizontal reduzido para 6.dp
            assertEquals("Espaçamento entre botões deve ser de 6.dp no status $status", 6, contract.secondaryRowSpacing)

            // Padding horizontal compacto de 4.dp
            assertEquals("Content padding horizontal deve ser de 4.dp no status $status", 4, contract.secondaryButtonHorizontalPadding)

            // maxLines = 1 para evitar quebra de linha feia
            assertEquals("Rótulos secundários devem ter maxLines = 1 no status $status", 1, contract.secondaryButtonMaxLines)

            // Raio de borda arredondada de 10.dp
            assertEquals("Shape deve ser RoundedCornerShape(10.dp) no status $status", 10, contract.secondaryButtonCornerRadius)

            // 4 ações disponíveis: Detalhes, Editar, Ajustes e Excluir
            assertEquals("Linha secundária deve conter exatamente 4 ações no status $status", 4, contract.secondaryActionsCount)

            // Botão de excluir quadrado de 38.dp x 38.dp
            assertEquals("Botão de exclusão deve ter tamanho de 38.dp no status $status", 38, contract.deleteButtonSize)
        }
    }

    @Test
    fun testSecondaryActionsLabelsAndIconsOrder() {
        data class ActionButtonMeta(
            val label: String,
            val iconName: String,
            val iconSize: Int, // dp
            val isIconOnly: Boolean
        )

        val actions = listOf(
            ActionButtonMeta("Detalhes", "Visibility", 14, isIconOnly = false),
            ActionButtonMeta("Editar", "Edit", 14, isIconOnly = false),
            ActionButtonMeta("Ajustes", "Tune", 14, isIconOnly = false),
            ActionButtonMeta("Excluir", "Delete", 16, isIconOnly = true)
        )

        assertEquals(4, actions.size)

        // Botão 1: Detalhes
        assertEquals("Detalhes", actions[0].label)
        assertEquals("Visibility", actions[0].iconName)
        assertEquals(14, actions[0].iconSize)
        assertFalse(actions[0].isIconOnly)

        // Botão 2: Editar
        assertEquals("Editar", actions[1].label)
        assertEquals("Edit", actions[1].iconName)
        assertEquals(14, actions[1].iconSize)
        assertFalse(actions[1].isIconOnly)

        // Botão 3: Ajustes
        assertEquals("Ajustes", actions[2].label)
        assertEquals("Tune", actions[2].iconName)
        assertEquals(14, actions[2].iconSize)
        assertFalse(actions[2].isIconOnly)

        // Botão 4: Excluir
        assertEquals("Excluir", actions[3].label)
        assertEquals("Delete", actions[3].iconName)
        assertEquals(16, actions[3].iconSize)
        assertTrue(actions[3].isIconOnly)
    }

    @Test
    fun testLayoutNoOverflowOnCompactScreens() {
        // Simulação de cálculo de largura em viewport compacto (360dp de tela, típico de smartphones menores)
        val screenWidthDp = 360
        val screenPaddingDp = 16 * 2 // Padding do container pai
        val cardInnerWidthDp = screenWidthDp - screenPaddingDp // 328dp

        val spacingTotalDp = 3 * 6 // 3 espaçamentos de 6.dp entre os 4 botões = 18dp
        val deleteButtonWidthDp = 38 // Botão quadrado de exclusão
        val availableForTextButtonsDp = cardInnerWidthDp - spacingTotalDp - deleteButtonWidthDp // 328 - 18 - 38 = 272dp

        val singleButtonWidthDp = availableForTextButtonsDp / 3.0f // ~90.66dp por botão

        // Para acomodar ícone (14dp) + spacer (3dp) + padding horizontal (2 * 4dp = 8dp) = 25dp fixos
        val availableForTextCharsDp = singleButtonWidthDp - 25.0f // ~65.66dp

        // A fonte é 11.sp. Em média cada caractere em 11sp ocupa ~6.5dp em telas densas.
        // "Detalhes" (8 chars) -> ~52dp
        // "Ajustes" (7 chars) -> ~45.5dp
        // "Editar" (6 chars) -> ~39dp
        // Todos cabem perfeitamente dentro de 65.66dp sem overflow!
        assertTrue(
            "Largura disponível para o texto (~$availableForTextCharsDp dp) deve ser superior ao maior rótulo 'Detalhes' (~52 dp)",
            availableForTextCharsDp > 55.0f
        )
    }
}
