package com.fernando.centraldomotorista.costcenter

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fernando.centraldomotorista.data.local.CostCenterDatabase
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.LedgerEntryType
import com.fernando.centraldomotorista.data.model.LedgerRefType
import com.fernando.centraldomotorista.data.model.SyncState
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.data.model.VaultPurpose
import com.fernando.centraldomotorista.data.repository.costcenter.InsufficientVaultBalance
import com.fernando.centraldomotorista.data.repository.costcenter.LedgerRepository
import com.fernando.centraldomotorista.sync.SyncEngine
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LedgerRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: CostCenterDatabase
    private lateinit var userSession: FakeUserSession
    private lateinit var ledgerRepository: LedgerRepository

    private val userId = "usr-ledger-1"
    private val now = Instant.parse("2026-11-20T10:00:00Z")

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        db = createInMemoryCostCenterDb(context)
        userSession = FakeUserSession(userId)
        val syncScheduler = createTestSyncScheduler(context)
        ledgerRepository = LedgerRepository(
            localDatabaseProvider = FakeLocalDatabaseProvider(db),
            userSession = userSession,
            syncScheduler = syncScheduler
        )

        db.vaultDao().upsert(
            Vault(
                id = "vault-a",
                userId = userId,
                name = "Manutenção",
                purpose = VaultPurpose.MANUTENCAO,
                initialBalance = BigDecimal("100.00"),
                trackingStartDate = LocalDate.of(2026, 11, 1),
                createdAt = now,
                updatedAt = now,
                syncState = SyncState.SINCRONIZADO
            ).toEntity()
        )
        db.vaultDao().upsert(
            Vault(
                id = "vault-b",
                userId = userId,
                name = "Impostos",
                purpose = VaultPurpose.IMPOSTOS,
                initialBalance = BigDecimal("20.00"),
                trackingStartDate = LocalDate.of(2026, 11, 1),
                createdAt = now,
                updatedAt = now,
                syncState = SyncState.SINCRONIZADO
            ).toEntity()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun transferirGravaDuasLinhasQueSomamZeroComMesmaOperationIdEDoisItensNoOutbox() = runTest {
        val opId = "op-transfer-1"
        val entries = ledgerRepository.transferir(
            fromVaultId = "vault-a",
            toVaultId = "vault-b",
            amount = BigDecimal("40.00"),
            occurredAt = now,
            note = "Reserva impostos",
            operationId = opId
        )

        assertEquals(2, entries.size)
        val leg0 = entries[0]
        val leg1 = entries[1]

        assertEquals(0, leg0.leg)
        assertEquals(1, leg1.leg)
        assertEquals(opId, leg0.operationId)
        assertEquals(opId, leg1.operationId)
        assertEquals(LedgerEntryType.TRANSFERENCIA, leg0.entryType)
        assertEquals(LedgerEntryType.TRANSFERENCIA, leg1.entryType)
        assertEquals(BigDecimal("-40.00"), leg0.amount)
        assertEquals(BigDecimal("40.00"), leg1.amount)
        assertEquals(0, (leg0.amount + leg1.amount).compareTo(BigDecimal.ZERO))
        assertEquals("vault-b", leg0.contraVaultId)
        assertEquals("vault-a", leg1.contraVaultId)

        assertEquals(BigDecimal("60.00"), ledgerRepository.getVaultBalance("vault-a"))
        assertEquals(BigDecimal("60.00"), ledgerRepository.getVaultBalance("vault-b"))

        val outboxItems = db.outboxDao().getPendingItems()
            .filter { it.tableName == SyncEngine.LEDGER_TABLE }
        assertEquals(2, outboxItems.size)
    }

    @Test
    fun mesmaOperationIdDuasVezesNaoDuplicaLinhasNemOutbox() = runTest {
        val opAporte = "op-idem-aporte"
        val first = ledgerRepository.aportar("vault-a", BigDecimal("50.00"), operationId = opAporte)
        val second = ledgerRepository.aportar("vault-a", BigDecimal("50.00"), operationId = opAporte)

        assertNotNull(first)
        assertNull(second)
        assertEquals(BigDecimal("150.00"), ledgerRepository.getVaultBalance("vault-a"))
        assertEquals(1, db.vaultLedgerDao().getEntriesForVault("vault-a").size)
        assertEquals(1, db.outboxDao().getPendingItems().size)

        val opTransfer = "op-idem-transfer"
        val t1 = ledgerRepository.transferir("vault-a", "vault-b", BigDecimal("30.00"), operationId = opTransfer)
        val t2 = ledgerRepository.transferir("vault-a", "vault-b", BigDecimal("30.00"), operationId = opTransfer)

        assertEquals(2, t1.size)
        assertTrue(t2.isEmpty())
        assertEquals(BigDecimal("120.00"), ledgerRepository.getVaultBalance("vault-a"))
        assertEquals(BigDecimal("50.00"), ledgerRepository.getVaultBalance("vault-b"))
        assertEquals(3, db.outboxDao().getPendingItems().size)
    }

    @Test
    fun ajustarSaldoGravaDiferencaComSinalCertoENaoGravaQuandoDiferencaZero() = runTest {
        // Saldo atual vault-a = 100.00 -> ajusta para 135.50 => +35.50
        val up = ledgerRepository.ajustarSaldo("vault-a", BigDecimal("135.50"), operationId = "op-adj-1")
        assertNotNull(up)
        assertEquals(LedgerEntryType.AJUSTE, up!!.entryType)
        assertEquals(BigDecimal("35.50"), up.amount)
        assertEquals(BigDecimal("135.50"), ledgerRepository.getVaultBalance("vault-a"))

        // Ajusta para 90.00 => -45.50
        val down = ledgerRepository.ajustarSaldo("vault-a", BigDecimal("90.00"), operationId = "op-adj-2")
        assertNotNull(down)
        assertEquals(BigDecimal("-45.50"), down!!.amount)
        assertEquals(BigDecimal("90.00"), ledgerRepository.getVaultBalance("vault-a"))

        val outboxCountBefore = db.outboxDao().getPendingItems().size

        // Diferença zero => não grava nada
        val noOp = ledgerRepository.ajustarSaldo("vault-a", BigDecimal("90.00"), operationId = "op-adj-3")
        assertNull(noOp)
        assertEquals(outboxCountBefore, db.outboxDao().getPendingItems().size)
    }

    @Test
    fun estornarDuasVezesNaMesmaEntradaGravaApenasUmaLinhaDeEstorno() = runTest {
        val aporte = ledgerRepository.aportar("vault-a", BigDecimal("50.00"), operationId = "op-ap-est")!!
        assertEquals(BigDecimal("150.00"), ledgerRepository.getVaultBalance("vault-a"))

        val rev1 = ledgerRepository.estornar(aporte.id, operationId = "op-rev-1")
        val rev2 = ledgerRepository.estornar(aporte.id, operationId = "op-rev-2")

        assertNotNull(rev1)
        assertNull(rev2)
        assertEquals(LedgerEntryType.ESTORNO, rev1!!.entryType)
        assertEquals(BigDecimal("-50.00"), rev1.amount)
        assertEquals(aporte.id, rev1.reversesEntryId)
        assertEquals(BigDecimal("100.00"), ledgerRepository.getVaultBalance("vault-a"))

        val entries = db.vaultLedgerDao().getEntriesForVault("vault-a")
        assertEquals(2, entries.size)
        assertEquals(2, db.outboxDao().getPendingItems().size)
    }

    @Test
    fun escritaEOutboxSaoAtomicosFalhaNoOutboxDesfazEscritaNaRazao() = runTest {
        // Trigger SQLite que aborta qualquer INSERT na tabela outbox dentro da mesma transação
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_outbox_insert
            BEFORE INSERT ON outbox
            BEGIN
                SELECT RAISE(ABORT, 'Falha simulada ao gravar outbox');
            END;
            """.trimIndent()
        )

        var threw = false
        try {
            ledgerRepository.aportar("vault-a", BigDecimal("75.00"), operationId = "op-atomic-fail")
        } catch (e: Exception) {
            threw = true
        }

        assertTrue("Deveria lançar exceção quando o outbox falha", threw)
        assertTrue(
            "Rollback deve desfazer a inserção em vault_ledger",
            db.vaultLedgerDao().getEntriesForVault("vault-a").isEmpty()
        )
        assertEquals(BigDecimal("100.00"), ledgerRepository.getVaultBalance("vault-a"))
        assertTrue(db.outboxDao().getPendingItems().isEmpty())
    }

    @Test
    fun retiradaTransferenciaEPagamentoAcimaDoSaldoSaoRejeitadosEAjusteNegativoTambem() = runTest {
        // Retirada acima do saldo (saldo = 100.00, tentativa = 120.00)
        try {
            ledgerRepository.retirar("vault-a", BigDecimal("120.00"), operationId = "op-ret-over")
            fail("Esperava InsufficientVaultBalance na retirada")
        } catch (e: InsufficientVaultBalance) {
            assertEquals("vault-a", e.vaultId)
            assertEquals(BigDecimal("100.00"), e.available)
            assertEquals(BigDecimal("120.00"), e.required)
        }

        // Transferência acima do saldo (saldo = 100.00, tentativa = 101.00)
        try {
            ledgerRepository.transferir("vault-a", "vault-b", BigDecimal("101.00"), operationId = "op-trf-over")
            fail("Esperava InsufficientVaultBalance na transferência")
        } catch (e: InsufficientVaultBalance) {
            assertEquals("vault-a", e.vaultId)
            assertEquals(BigDecimal("100.00"), e.available)
            assertEquals(BigDecimal("101.00"), e.required)
        }

        // Pagamento acima do saldo (saldo = 100.00, tentativa = 150.00)
        try {
            ledgerRepository.registrarPagamento(
                refType = LedgerRefType.DESPESA,
                refId = "exp-1",
                vaultId = "vault-a",
                vaultAmount = BigDecimal("150.00"),
                operationId = "op-pay-over"
            )
            fail("Esperava InsufficientVaultBalance no pagamento")
        } catch (e: InsufficientVaultBalance) {
            assertEquals("vault-a", e.vaultId)
            assertEquals(BigDecimal("100.00"), e.available)
            assertEquals(BigDecimal("150.00"), e.required)
        }

        // Ajuste para saldo negativo é rejeitado
        try {
            ledgerRepository.ajustarSaldo("vault-a", BigDecimal("-1.00"), operationId = "op-adj-neg")
            fail("Esperava IllegalArgumentException para saldo negativo")
        } catch (e: IllegalArgumentException) {
            // esperado
        }

        // Nenhuma entrada gravada
        assertTrue(db.vaultLedgerDao().getEntriesForVault("vault-a").isEmpty())
    }

    @Test
    fun estornoEhAUnicaOperacaoQuePodeDeixarSaldoNegativo() = runTest {
        // vault-b começa com initialBalance = 20.00
        // Aporte de 100.00 -> saldo 120.00
        val aporte = ledgerRepository.aportar("vault-b", BigDecimal("100.00"), operationId = "op-neg-1")!!
        // Retirada de 80.00 -> saldo 40.00
        ledgerRepository.retirar("vault-b", BigDecimal("80.00"), operationId = "op-neg-2")
        assertEquals(BigDecimal("40.00"), ledgerRepository.getVaultBalance("vault-b"))

        // Estorno do aporte de 100.00 -> saldo fica -60.00 (permitido apenas no estorno)
        val estorno = ledgerRepository.estornar(aporte.id, operationId = "op-neg-3")
        assertNotNull(estorno)
        assertEquals(BigDecimal("-60.00"), ledgerRepository.getVaultBalance("vault-b"))
    }
}
