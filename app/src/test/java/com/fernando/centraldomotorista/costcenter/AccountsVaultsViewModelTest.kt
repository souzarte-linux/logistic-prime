package com.fernando.centraldomotorista.costcenter

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.fernando.centraldomotorista.data.local.CostCenterDatabase
import com.fernando.centraldomotorista.data.model.AccountType
import com.fernando.centraldomotorista.data.model.LedgerEntryType
import com.fernando.centraldomotorista.data.model.VaultPurpose
import com.fernando.centraldomotorista.data.repository.costcenter.BankAccountRepository
import com.fernando.centraldomotorista.data.repository.costcenter.CostCenterSettingsRepository
import com.fernando.centraldomotorista.data.repository.costcenter.LedgerRepository
import com.fernando.centraldomotorista.data.repository.costcenter.VaultRepository
import com.fernando.centraldomotorista.ui.screens.costcenter.AccountsVaultsViewModel
import com.fernando.centraldomotorista.ui.screens.costcenter.ActionOutcome
import com.fernando.centraldomotorista.ui.screens.costcenter.SyncChipStatus
import com.fernando.centraldomotorista.ui.screens.relatorios.formatCurrency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AccountsVaultsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: CostCenterDatabase
    private lateinit var userSession: FakeUserSession
    private lateinit var bankAccountRepository: BankAccountRepository
    private lateinit var vaultRepository: VaultRepository
    private lateinit var ledgerRepository: LedgerRepository
    private lateinit var settingsRepository: CostCenterSettingsRepository
    private lateinit var viewModel: AccountsVaultsViewModel

    private val userId = "usr-vm-test-1"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = createInMemoryCostCenterDb(context)
        userSession = FakeUserSession(userId)
        val dbProvider = FakeLocalDatabaseProvider(db)
        val syncScheduler = createTestSyncScheduler(context)

        bankAccountRepository = BankAccountRepository(dbProvider, userSession, syncScheduler)
        vaultRepository = VaultRepository(dbProvider, userSession, syncScheduler)
        ledgerRepository = LedgerRepository(dbProvider, userSession, syncScheduler)
        settingsRepository = CostCenterSettingsRepository(dbProvider, userSession, syncScheduler)

        viewModel = AccountsVaultsViewModel(
            bankAccountRepository = bankAccountRepository,
            vaultRepository = vaultRepository,
            ledgerRepository = ledgerRepository,
            settingsRepository = settingsRepository,
            localDatabaseProvider = dbProvider,
            userSession = userSession,
            syncScheduler = syncScheduler
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `conta sem apelido e rejeitada`() = runTest {
        val outcome = viewModel.saveAccount(
            existing = null,
            name = "   ",
            institution = "Nubank",
            agency = null,
            lastDigits = "1234",
            accountType = AccountType.CORRENTE,
            informedBalance = null
        )

        assertTrue(outcome is ActionOutcome.Error)
        assertEquals("Informe o apelido da conta", (outcome as ActionOutcome.Error).message)
        assertEquals(0, viewModel.uiState.value.accounts.size)
    }

    @Test
    fun `conta com mais de 6 digitos e rejeitada`() = runTest {
        val outcome = viewModel.saveAccount(
            existing = null,
            name = "Nubank",
            institution = "Nubank",
            agency = "0001",
            lastDigits = "1234567",
            accountType = AccountType.CORRENTE,
            informedBalance = null
        )

        assertTrue(outcome is ActionOutcome.Error)
        assertEquals("Informe apenas até 6 dígitos da conta", (outcome as ActionOutcome.Error).message)
        assertEquals(0, viewModel.uiState.value.accounts.size)
    }

    @Test
    fun `so uma caixinha de desgaste por vez`() = runTest {
        val r1 = viewModel.saveVault(
            existing = null,
            accountId = null,
            name = "Manutenção 1",
            purpose = VaultPurpose.MANUTENCAO,
            initialBalance = BigDecimal("100.00"),
            goalAmount = null,
            isMaintenanceVault = true
        )
        assertTrue(r1 is ActionOutcome.Success)
        val state1 = viewModel.uiState.first { state ->
            state.vaults.any { it.name == "Manutenção 1" } && state.maintenanceVaultId != null
        }
        val vault1 = state1.vaults.first { it.name == "Manutenção 1" }
        assertEquals(vault1.id, state1.maintenanceVaultId)

        val r2 = viewModel.saveVault(
            existing = null,
            accountId = null,
            name = "Manutenção 2",
            purpose = VaultPurpose.MANUTENCAO,
            initialBalance = BigDecimal("50.00"),
            goalAmount = null,
            isMaintenanceVault = true
        )
        assertTrue(r2 is ActionOutcome.Success)
        val state2 = viewModel.uiState.first { state ->
            val v2 = state.vaults.find { it.name == "Manutenção 2" }
            v2 != null && state.maintenanceVaultId == v2.id
        }
        val vault2 = state2.vaults.first { it.name == "Manutenção 2" }

        assertEquals(vault2.id, state2.maintenanceVaultId)
        assertEquals(vault2.id, db.costCenterSettingsDao().getByUserId(userId)?.maintenanceVaultId)
    }

    @Test
    fun `transferencia para a mesma caixinha e rejeitada`() = runTest {
        viewModel.saveVault(
            existing = null,
            accountId = null,
            name = "Reserva",
            purpose = VaultPurpose.EMERGENCIA,
            initialBalance = BigDecimal("200.00"),
            goalAmount = null,
            isMaintenanceVault = false
        )
        val vault = viewModel.uiState.first { it.vaults.isNotEmpty() }.vaults.first()

        val outcome = viewModel.transferir(
            fromVaultId = vault.id,
            toVaultId = vault.id,
            amount = BigDecimal("50.00"),
            note = null,
            operationId = "op-same-vault"
        )

        assertTrue(outcome is ActionOutcome.Error)
        assertEquals("Selecione uma caixinha de destino diferente", (outcome as ActionOutcome.Error).message)
    }

    @Test
    fun `ajuste saldo 105 para 100 grava menos 5 e diferenca zero nao grava`() = runTest {
        viewModel.saveVault(
            existing = null,
            accountId = null,
            name = "Impostos",
            purpose = VaultPurpose.IMPOSTOS,
            initialBalance = BigDecimal("105.00"),
            goalAmount = null,
            isMaintenanceVault = false
        )
        val vault = viewModel.uiState.first { it.vaults.isNotEmpty() }.vaults.first()

        // Diferença zero => "Saldo já está correto", sem gravar
        val noOp = viewModel.ajustarSaldo(
            vaultId = vault.id,
            targetBalance = BigDecimal("105.00"),
            note = null,
            operationId = "op-adj-noop"
        )
        assertTrue(noOp is ActionOutcome.NoChange)
        assertEquals("Saldo já está correto", (noOp as ActionOutcome.NoChange).message)
        assertEquals(0, db.vaultLedgerDao().getEntriesForVault(vault.id).size)

        // Ajuste 105,00 -> 100,00 grava -5,00
        val adj = viewModel.ajustarSaldo(
            vaultId = vault.id,
            targetBalance = BigDecimal("100.00"),
            note = "Conferência manual",
            operationId = "op-adj-1"
        )
        assertTrue(adj is ActionOutcome.Success)
        val entries = db.vaultLedgerDao().getEntriesForVault(vault.id)
        assertEquals(1, entries.size)
        assertEquals(LedgerEntryType.AJUSTE.key, entries.first().entryType)
        assertEquals(0, BigDecimal("-5.00").compareTo(entries.first().amount))
        val updatedState = viewModel.uiState.first { state ->
            state.balancesByVaultId[vault.id]?.compareTo(BigDecimal("100.00")) == 0
        }
        assertEquals(0, BigDecimal("100.00").compareTo(updatedState.balancesByVaultId[vault.id]))
    }

    @Test
    fun `retirada acima do saldo e rejeitada com mensagem do saldo maximo`() = runTest {
        val balance = BigDecimal("80.00")
        viewModel.saveVault(
            existing = null,
            accountId = null,
            name = "Seguro",
            purpose = VaultPurpose.SEGURO,
            initialBalance = balance,
            goalAmount = null,
            isMaintenanceVault = false
        )
        val vault = viewModel.uiState.first { it.vaults.isNotEmpty() }.vaults.first()

        val outcome = viewModel.retirar(
            vaultId = vault.id,
            amount = BigDecimal("80.01"),
            note = null,
            operationId = "op-ret-over"
        )

        assertTrue(outcome is ActionOutcome.Error)
        assertEquals(
            "O valor máximo para retirada é o saldo existente (${balance.formatCurrency()})",
            (outcome as ActionOutcome.Error).message
        )
        assertEquals(0, db.vaultLedgerDao().getEntriesForVault(vault.id).size)
    }

    @Test
    fun `transferencia acima do saldo e rejeitada`() = runTest {
        val balance = BigDecimal("40.00")
        viewModel.saveVault(null, null, "Origem", VaultPurpose.OUTRO, balance, null, false)
        viewModel.saveVault(null, null, "Destino", VaultPurpose.OUTRO, BigDecimal.ZERO, null, false)
        val state = viewModel.uiState.first { s ->
            s.vaults.any { it.name == "Origem" } && s.vaults.any { it.name == "Destino" }
        }
        val origem = state.vaults.first { it.name == "Origem" }
        val destino = state.vaults.first { it.name == "Destino" }

        val outcome = viewModel.transferir(
            fromVaultId = origem.id,
            toVaultId = destino.id,
            amount = BigDecimal("41.00"),
            note = null,
            operationId = "op-transf-over"
        )

        assertTrue(outcome is ActionOutcome.Error)
        assertEquals(
            "O valor máximo para retirada é o saldo existente (${balance.formatCurrency()})",
            (outcome as ActionOutcome.Error).message
        )
    }

    @Test
    fun `ajuste para saldo negativo e rejeitado`() = runTest {
        viewModel.saveVault(null, null, "Garagem", VaultPurpose.GARAGEM, BigDecimal("50.00"), null, false)
        val vault = viewModel.uiState.first { it.vaults.isNotEmpty() }.vaults.first()

        val outcome = viewModel.ajustarSaldo(
            vaultId = vault.id,
            targetBalance = BigDecimal("-1.00"),
            note = null,
            operationId = "op-adj-neg"
        )

        assertTrue(outcome is ActionOutcome.Error)
        assertEquals("O saldo ajustado não pode ser negativo", (outcome as ActionOutcome.Error).message)
    }

    @Test
    fun `arquivar em vez de excluir quando ha referencia`() = runTest {
        viewModel.saveAccount(null, "Conta Ref", "Inter", null, "4321", AccountType.CORRENTE, null)
        val account = viewModel.uiState.first { it.accounts.isNotEmpty() }.accounts.first()

        // Cria caixinha vinculada à conta e faz um aporte (gera referência na caixinha e na conta)
        viewModel.saveVault(null, account.id, "Caixinha Ref", VaultPurpose.OUTRO, BigDecimal.ZERO, null, false)
        val vault = viewModel.uiState.first { it.vaults.isNotEmpty() }.vaults.first()
        viewModel.aportar(vault.id, BigDecimal("25.00"), null, "op-ref-1")

        // Arquivar caixinha com lançamento na razão => active = false, deletedAt = null
        viewModel.archiveVault(vault.id)
        val archivedVault = db.vaultDao().getById(vault.id)
        assertNotNull(archivedVault)
        assertEquals(false, archivedVault!!.active)
        assertNull(archivedVault.deletedAt)

        // Arquivar conta referenciada pela caixinha => active = false, deletedAt = null
        viewModel.archiveAccount(account.id)
        val archivedAccount = db.bankAccountDao().getById(account.id)
        assertNotNull(archivedAccount)
        assertEquals(false, archivedAccount!!.active)
        assertNull(archivedAccount.deletedAt)
    }

    @Test
    fun `reenvio com o mesmo operationId nao duplica`() = runTest {
        viewModel.saveVault(null, null, "Óleo", VaultPurpose.MANUTENCAO, BigDecimal.ZERO, null, false)
        val vault = viewModel.uiState.first { it.vaults.isNotEmpty() }.vaults.first()

        val sameOperationId = "op-idempotent-123"
        val first = viewModel.aportar(vault.id, BigDecimal("30.00"), "Aporte 1", sameOperationId)
        val second = viewModel.aportar(vault.id, BigDecimal("30.00"), "Aporte 1", sameOperationId)

        assertTrue(first is ActionOutcome.Success)
        assertTrue(second is ActionOutcome.Success)
        assertEquals(1, db.vaultLedgerDao().getEntriesForVault(vault.id).size)
        val updatedState = viewModel.uiState.first { state ->
            state.balancesByVaultId[vault.id]?.compareTo(BigDecimal("30.00")) == 0
        }
        assertEquals(0, BigDecimal("30.00").compareTo(updatedState.balancesByVaultId[vault.id]))
    }

    @Test
    fun `chip de sync reflete o outbox`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(SyncChipStatus.Synced, initial.syncStatus)

            // Uma escrita cria item pendente no outbox
            viewModel.saveAccount(null, "Conta Sync", null, null, null, null, null)
            val pendingState = expectMostRecentItem()
            assertTrue(pendingState.syncStatus is SyncChipStatus.Pending)
            assertEquals(1, (pendingState.syncStatus as SyncChipStatus.Pending).count)

            // Simula falha no outbox
            val outboxItem = db.outboxDao().getAll().first()
            db.outboxDao().updateAttempts(outboxItem.id, attempts = 8, lastError = "Erro de rede", lastAttemptAt = 1000L)
            val failedState = awaitItem()
            assertTrue(failedState.syncStatus is SyncChipStatus.Failed)
            assertEquals(1, (failedState.syncStatus as SyncChipStatus.Failed).count)

            // Limpa outbox -> volta para Synced
            db.outboxDao().clear()
            val syncedState = awaitItem()
            assertEquals(SyncChipStatus.Synced, syncedState.syncStatus)
        }
    }
}
