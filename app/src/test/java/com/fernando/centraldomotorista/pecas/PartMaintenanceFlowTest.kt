package com.fernando.centraldomotorista.pecas

import com.fernando.centraldomotorista.data.model.Company
import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.fernando.centraldomotorista.data.model.PartProduct
import com.fernando.centraldomotorista.data.model.PartType
import com.fernando.centraldomotorista.ui.screens.pecas.PartMaintenanceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Testes unitários para validar a segregação de fluxos entre o alerta proativo da HomeScreen
 * (Modo CRIAÇÃO para novo ciclo de troca preventiva) e a gestão cadastral de peças (Modo EDIÇÃO).
 * Referência: docs/design/specs-alerta-manutencao-e-edicao-pecas.md (TASK-DES-07).
 */
class PartMaintenanceFlowTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private fun createViewModel(): PartMaintenanceViewModel {
        return PartMaintenanceViewModel(
            externalScope = testScope,
            loadOnInit = false,
            userIdProvider = { "test-user-123" }
        )
    }

    private fun createSamplePart(
        id: String = "part-abc",
        partName: String = "Óleo Motor 5W30",
        lifeKm: BigDecimal = BigDecimal("10000"),
        lastChangeKm: BigDecimal = BigDecimal("40000"),
        expenseId: String? = "exp-999"
    ): PartMaintenance {
        return PartMaintenance(
            id = id,
            userId = "test-user-123",
            partName = partName,
            lifeKm = lifeKm,
            lastChangeKm = lastChangeKm,
            lastChangeAt = OffsetDateTime.now(),
            companyId = "comp-1",
            partProductId = "prod-1",
            expenseId = expenseId
        )
    }

    @Test
    fun testOpenAddFromAlertSetsCreationModeWithTargetPartId() {
        val vm = createViewModel()
        val samplePart = createSamplePart()
        val currentOdo = BigDecimal("49500")

        vm.setTestData(
            parts = listOf(samplePart),
            companies = listOf(Company(id = "comp-1", userId = "test-user-123", name = "Auto Mecânica Central")),
            partTypes = listOf(PartType(id = "type-1", userId = "test-user-123", name = "Óleo")),
            partProducts = listOf(PartProduct(id = "prod-1", userId = "test-user-123", partTypeId = "type-1", brand = "Mobil", model = "Super 3000", defaultLifeKm = BigDecimal("10000"))),
            currentOdometer = currentOdo
        )

        // Disparo do clique no alerta da HomeScreen
        vm.openAddFromAlert(samplePart)

        val state = vm.uiState.value

        // Validações cruciais da spec TASK-DES-07:
        assertTrue("O formulário deve ser aberto", state.isFormOpen)
        assertNull("editingPartId DEVE ser null no modo criação para evitar sobrescrever a peça", state.editingPartId)
        assertNull("editingExpenseId DEVE ser null no modo criação para gerar nova despesa financeira", state.editingExpenseId)
        assertEquals("targetPartIdForNewCycle deve apontar para o id da peça existente", samplePart.id, state.targetPartIdForNewCycle)

        // Dados herdados da peça e odômetro
        assertEquals(samplePart.partName, state.partName)
        assertEquals(samplePart.lifeKm.toPlainString(), state.lifeKm)
        assertEquals("O odômetro sugerido para a nova troca deve ser o odômetro atual", currentOdo.toPlainString(), state.lastChangeKm)
        assertEquals("Mobil", state.partBrand)
        assertEquals("Super 3000", state.partModel)
        assertEquals("comp-1", state.selectedCompanyId)

        // Campos limpos para digitação da nova troca
        assertEquals("Valor inicial deve estar em branco para preenchimento", "", state.totalAmountText)
        assertEquals("", state.receiptNumber)

        // Verificação da regra de tela isEditing
        val isEditing = state.editingPartId != null || state.editingExpenseId != null
        assertFalse("No fluxo vindo do alerta da HomeScreen, a tela NÃO deve estar em modo de edição", isEditing)
    }

    @Test
    fun testPrepareNewMaintenanceFromPartAliasHasIdenticalBehavior() {
        val vm = createViewModel()
        val samplePart = createSamplePart()
        val currentOdo = BigDecimal("52000")

        vm.setTestData(
            parts = listOf(samplePart),
            currentOdometer = currentOdo
        )

        // Chama o alias da spec UI/UX
        vm.prepareNewMaintenanceFromPart(samplePart)

        val state = vm.uiState.value

        assertTrue(state.isFormOpen)
        assertNull(state.editingPartId)
        assertNull(state.editingExpenseId)
        assertEquals(samplePart.id, state.targetPartIdForNewCycle)
        assertEquals(currentOdo.toPlainString(), state.lastChangeKm)
    }

    @Test
    fun testStartEditingSetsEditionModeWithPartId() {
        val vm = createViewModel()
        val samplePart = createSamplePart()

        vm.setTestData(
            parts = listOf(samplePart),
            currentOdometer = BigDecimal("55000")
        )

        // Disparo do clique no botão Lápis (IconButton 48dp)
        vm.startEditing(samplePart)

        val state = vm.uiState.value

        assertTrue("O formulário deve ser aberto", state.isFormOpen)
        assertEquals("editingPartId deve ser preenchido no modo de edição cadastral", samplePart.id, state.editingPartId)
        assertEquals("editingExpenseId deve ser o id da despesa vinculada", samplePart.expenseId, state.editingExpenseId)
        assertNull("targetPartIdForNewCycle deve ser null na edição cadastral", state.targetPartIdForNewCycle)
        assertEquals("O KM deve ser o KM original salvo na peça, e NÃO o odômetro atual", samplePart.lastChangeKm.toPlainString(), state.lastChangeKm)

        // Verificação da regra de tela isEditing
        val isEditing = state.editingPartId != null || state.editingExpenseId != null
        assertTrue("No fluxo de edição via botão lápis, a tela DEVE estar em modo de edição", isEditing)
    }

    @Test
    fun testOpenAddDialogResetsAllTargetAndEditingIds() {
        val vm = createViewModel()
        val samplePart = createSamplePart()

        vm.setTestData(parts = listOf(samplePart))

        // Primeiro entra em modo alerta
        vm.openAddFromAlert(samplePart)
        assertEquals(samplePart.id, vm.uiState.value.targetPartIdForNewCycle)

        // Depois abre o diálogo padrão de adição
        vm.openAddDialog(prefillPartName = "Nova Peça")

        val state = vm.uiState.value
        assertTrue(state.isFormOpen)
        assertNull(state.editingPartId)
        assertNull(state.editingExpenseId)
        assertNull("targetPartIdForNewCycle deve ser limpo ao abrir cadastro avulso", state.targetPartIdForNewCycle)
        assertEquals("Nova Peça", state.partName)
    }

    @Test
    fun testCloseFormResetsAllTargetAndEditingIds() {
        val vm = createViewModel()
        val samplePart = createSamplePart()

        vm.setTestData(parts = listOf(samplePart))
        vm.openAddFromAlert(samplePart)
        assertEquals(samplePart.id, vm.uiState.value.targetPartIdForNewCycle)

        // Fecha o formulário
        vm.closeForm()

        val state = vm.uiState.value
        assertFalse(state.isFormOpen)
        assertNull(state.editingPartId)
        assertNull(state.editingExpenseId)
        assertNull(state.targetPartIdForNewCycle)
        assertEquals("", state.partName)
    }
}
