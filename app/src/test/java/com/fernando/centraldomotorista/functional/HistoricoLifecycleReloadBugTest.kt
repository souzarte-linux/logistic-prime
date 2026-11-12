package com.fernando.centraldomotorista.functional

import com.fernando.centraldomotorista.data.model.*
import com.fernando.centraldomotorista.data.repository.HistoricoRepository
import com.fernando.centraldomotorista.ui.common.period.PeriodPreset
import com.fernando.centraldomotorista.ui.screens.historico.HistoricoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

class HistoricoLifecycleReloadBugTest {

    private val testScope = CoroutineScope(Dispatchers.Default)

    private val sampleProfile = Profile(
        id = "user-123",
        fullName = "Motorista Teste",
        email = "motorista@teste.com",
        phone = null,
        vehicle = "moto",
        plate = "ABC-1234",
        avatarUrl = null,
        dailyGoal = BigDecimal("300.00"),
        weeklyGoal = BigDecimal("1500.00"),
        monthlyGoal = BigDecimal("6000.00"),
        vehicleBrand = "Honda",
        vehicleModel = "CG 160",
        vehicleYear = 2022,
        tankSizeL = BigDecimal("16.0"),
        avgConsumptionKml = BigDecimal("38.0"),
        oilChangeKm = BigDecimal("3000"),
        tireSizeFront = null,
        tireSizeRear = null,
        hasBag = true,
        lastOilChangeAt = null
    )

    private val sampleItem = TransactionItem(
        id = "tx-1",
        type = TransactionType.GANHO,
        sourceType = TransactionSourceType.ROUTE,
        title = "MERCADO LIVRE",
        subtitle = "SPO - SAO • 10:00",
        amount = BigDecimal("150.00"),
        netAmount = BigDecimal("150.00"),
        category = "ROTA",
        occurredAt = OffsetDateTime.now(ZoneOffset.UTC),
        establishment = "MERCADO LIVRE"
    )

    /**
     * Repositório falso controlado para testar cenários de ciclo de vida e recarga.
     */
    class ControlledHistoricoRepository(
        var profileToReturn: Profile,
        var itemsToReturn: List<TransactionItem>,
        var shouldThrowOnLoad: Boolean = false,
        var simulateNetworkFailureReturningEmpty: Boolean = false
    ) : HistoricoRepository() {
        var callCount = 0

        override suspend fun loadHistoricoData(userId: String): Pair<Profile, List<TransactionItem>> {
            callCount++
            if (shouldThrowOnLoad) {
                throw java.io.IOException("Falha de conexão com o Supabase ao retomar app")
            }
            if (simulateNetworkFailureReturningEmpty) {
                // Simula comportamento antigo dos repositórios: engolia exceção e devolvia lista vazia
                return Pair(profileToReturn, emptyList())
            }
            return Pair(profileToReturn, itemsToReturn)
        }
    }

    @Test
    fun test_bugReproduction_whenAppResumesAndReloadFailsOrReturnsEmpty_existingTransactionsMustNotBeWiped() = runBlocking {
        val repo = ControlledHistoricoRepository(
            profileToReturn = sampleProfile,
            itemsToReturn = listOf(sampleItem)
        )

        // 1. Inicializa o ViewModel com dados carregados com sucesso
        val vm = HistoricoViewModel(
            historicoRepository = repo,
            externalScope = testScope,
            observeDataSync = false,
            autoLoad = true,
            userIdProvider = { "user-123" }
        )
        delay(200)

        // Verifica que os dados foram carregados normalmente
        assertEquals(1, vm.uiState.value.allTransactions.size)
        assertFalse("monthGroups não deve estar vazio inicialmente", vm.uiState.value.monthGroups.isEmpty())
        assertEquals(BigDecimal("150.00"), vm.uiState.value.saldoPeriodo)

        // 2. Simula o usuário alternando de app e voltando (ON_RESUME):
        // Neste momento a rede oscila ou a sessão falha, simulando o erro de recarga
        repo.simulateNetworkFailureReturningEmpty = true

        // Dispara o recarregamento (o que o DisposableEffect faz no ON_RESUME)
        vm.loadData()
        delay(200)

        // REGRA DE NEGÓCIO ESSENCIAL:
        // Os dados anteriores que já estavam visíveis na tela NÃO PODEM ser apagados!
        // A tela NÃO pode ficar sem nenhum dado após alternar de aplicativo!
        assertFalse(
            "BUG REPRODUZIDO: Ao retomar o app, uma falha/resposta vazia de recarga apagou todas as transações da tela!",
            vm.uiState.value.allTransactions.isEmpty()
        )
        assertFalse(
            "BUG REPRODUZIDO: Ao retomar o app, os grupos de meses da tela de histórico foram esvaziados!",
            vm.uiState.value.monthGroups.isEmpty()
        )
    }

    @Test
    fun test_bugReproduction_whenAppResumesWithActiveFilters_filtersAndDataMustBePreserved() = runBlocking {
        val repo = ControlledHistoricoRepository(
            profileToReturn = sampleProfile,
            itemsToReturn = listOf(sampleItem)
        )

        val vm = HistoricoViewModel(
            historicoRepository = repo,
            externalScope = testScope,
            observeDataSync = false,
            autoLoad = true,
            userIdProvider = { "user-123" }
        )
        delay(200)

        // Aplica um filtro de período ativo (ex: Mês)
        vm.applyPeriodPreset(PeriodPreset.MES)
        delay(200)
        assertEquals(PeriodPreset.MES, vm.uiState.value.periodFilter.preset)
        assertFalse(vm.uiState.value.monthGroups.isEmpty())

        // Simula alternância de app com falha temporária
        repo.simulateNetworkFailureReturningEmpty = true
        vm.loadData()
        delay(200)

        assertEquals(PeriodPreset.MES, vm.uiState.value.periodFilter.preset)
        assertFalse(
            "BUG REPRODUZIDO: Transações sumiram da tela ao alternar app com filtro de período ativo!",
            vm.uiState.value.allTransactions.isEmpty()
        )
        assertFalse(
            "BUG REPRODUZIDO: monthGroups sumiram ao alternar app com filtro de período ativo!",
            vm.uiState.value.monthGroups.isEmpty()
        )
    }
}
