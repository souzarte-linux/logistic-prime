package com.fernando.centraldomotorista.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fernando.centraldomotorista.data.local.CostCenterDatabase
import com.fernando.centraldomotorista.data.local.dao.OutboxDao
import com.fernando.centraldomotorista.data.local.dao.SyncCursorDao
import com.fernando.centraldomotorista.data.local.entity.OutboxEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class SyncEngineTest {

    private lateinit var context: Context
    private lateinit var database: CostCenterDatabase
    private lateinit var outboxDao: OutboxDao
    private lateinit var syncCursorDao: SyncCursorDao
    private lateinit var registry: SyncHandlerRegistry
    private lateinit var syncEngine: SyncEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, CostCenterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        outboxDao = database.outboxDao()
        syncCursorDao = database.syncCursorDao()
        registry = SyncHandlerRegistry()
        syncEngine = SyncEngine(outboxDao, syncCursorDao, registry)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun outboxPreservaAOrdemDeCriacao() = runTest {
        val item1 = OutboxEntity(
            id = "id-1",
            tableName = "fixed_costs",
            rowId = "row-1",
            operation = "INSERT",
            payloadJson = "{}",
            attempts = 0,
            createdAt = 300L
        )
        val item2 = OutboxEntity(
            id = "id-2",
            tableName = "fixed_costs",
            rowId = "row-2",
            operation = "INSERT",
            payloadJson = "{}",
            attempts = 0,
            createdAt = 100L
        )
        val item3 = OutboxEntity(
            id = "id-3",
            tableName = "fixed_costs",
            rowId = "row-3",
            operation = "INSERT",
            payloadJson = "{}",
            attempts = 0,
            createdAt = 200L
        )

        // Inserção em ordem desordenada
        outboxDao.insert(item1)
        outboxDao.insert(item2)
        outboxDao.insert(item3)

        val pending = outboxDao.getPendingItems()
        assertEquals(3, pending.size)
        // Deve ser retornado estritamente ordenado por created_at ASC
        assertEquals("id-2", pending[0].id)
        assertEquals(100L, pending[0].createdAt)
        assertEquals("id-3", pending[1].id)
        assertEquals(200L, pending[1].createdAt)
        assertEquals("id-1", pending[2].id)
        assertEquals(300L, pending[2].createdAt)
    }

    @Test
    fun retryIncrementaAttemptsERespeitaBackoff() = runTest {
        val testItem = OutboxEntity(
            id = "retry-item",
            tableName = "fixed_costs",
            rowId = "row-retry",
            operation = "INSERT",
            payloadJson = "{}",
            attempts = 0,
            createdAt = 1000L
        )
        outboxDao.insert(testItem)

        // Registra handler que sempre falha
        val failingHandler = object : TableSyncHandler {
            override val table: String = "fixed_costs"
            override suspend fun push(item: OutboxItem): PushResult {
                return PushResult.Failure(RuntimeException("Conexão instável"))
            }
            override suspend fun pull(since: Instant?): PullResult = PullResult.Success(null, 0)
        }
        registry.register(failingHandler)

        // Primeira tentativa em t = 1000 (delay para attempt 0 é 0)
        val summary1 = syncEngine.processOutbox(currentTime = 1000L)
        assertEquals(0, summary1.successCount)
        assertEquals(1, summary1.errorCount)

        val updated = outboxDao.getById("retry-item")
        assertNotNull(updated)
        assertEquals(1, updated!!.attempts)
        assertEquals("Conexão instável", updated.lastError)

        // Com attempts = 1, o backoff requerido é 1000ms a partir de createdAt (1000 + 1000 = 2000)
        assertFalse(syncEngine.isBackoffElapsed(OutboxItem.fromEntity(updated), now = 1500L))
        assertTrue(syncEngine.isBackoffElapsed(OutboxItem.fromEntity(updated), now = 2000L))

        // Tentativa em t = 1500 não deve processar devido ao backoff
        val summaryIgnored = syncEngine.processOutbox(currentTime = 1500L)
        assertEquals(0, summaryIgnored.successCount)
        assertEquals(0, summaryIgnored.errorCount)
        assertEquals(1, outboxDao.getById("retry-item")!!.attempts)

        // Tentativa em t = 2050 processa e incrementa attempts para 2
        val summary2 = syncEngine.processOutbox(currentTime = 2050L)
        assertEquals(1, summary2.errorCount)
        assertEquals(2, outboxDao.getById("retry-item")!!.attempts)
    }

    @Test
    fun resposta409ContaComoSucessoIdempotente() = runTest {
        val item409 = OutboxEntity(
            id = "item-409",
            tableName = "vaults",
            rowId = "row-vault",
            operation = "INSERT",
            payloadJson = "{}",
            attempts = 0,
            createdAt = 1000L
        )
        outboxDao.insert(item409)

        val conflictHandler = object : TableSyncHandler {
            override val table: String = "vaults"
            override suspend fun push(item: OutboxItem): PushResult {
                return PushResult.Conflict409("Registro já existente no Supabase")
            }
            override suspend fun pull(since: Instant?): PullResult = PullResult.Success(null, 0)
        }
        registry.register(conflictHandler)

        val summary = syncEngine.processOutbox(currentTime = 1000L)
        assertEquals(1, summary.successCount)
        assertEquals(0, summary.errorCount)

        // Registro removido do outbox como sucesso
        assertNull(outboxDao.getById("item-409"))
    }

    @Test
    fun resposta409ViaExcecaoTambemContaComoSucesso() = runTest {
        val itemEx = OutboxEntity(
            id = "item-ex-409",
            tableName = "vaults",
            rowId = "row-vault-2",
            operation = "INSERT",
            payloadJson = "{}",
            attempts = 0,
            createdAt = 1000L
        )
        outboxDao.insert(itemEx)

        val exceptionConflictHandler = object : TableSyncHandler {
            override val table: String = "vaults"
            override suspend fun push(item: OutboxItem): PushResult {
                throw RuntimeException("HTTP 409 Conflict: duplicate key value violates unique constraint")
            }
            override suspend fun pull(since: Instant?): PullResult = PullResult.Success(null, 0)
        }
        registry.register(exceptionConflictHandler)

        val summary = syncEngine.processOutbox(currentTime = 1000L)
        assertEquals(1, summary.successCount)
        assertNull(outboxDao.getById("item-ex-409"))
    }

    @Test
    fun oitoFalhasViramFalhaPermanenteVisivel() = runTest {
        val itemPermanent = OutboxEntity(
            id = "item-perm",
            tableName = "fixed_costs",
            rowId = "row-perm",
            operation = "INSERT",
            payloadJson = "{}",
            attempts = 7, // Já falhou 7 vezes
            createdAt = 1000L
        )
        outboxDao.insert(itemPermanent)

        val failingHandler = object : TableSyncHandler {
            override val table: String = "fixed_costs"
            override suspend fun push(item: OutboxItem): PushResult {
                return PushResult.Failure(IllegalStateException("Schema mismatch fatal"))
            }
            override suspend fun pull(since: Instant?): PullResult = PullResult.Success(null, 0)
        }
        registry.register(failingHandler)

        // Simula execução com tempo posterior ao backoff
        val summary = syncEngine.processOutbox(currentTime = 1_000_000L)
        assertEquals(0, summary.successCount)
        assertEquals(0, summary.errorCount)
        assertEquals(1, summary.permanentFailureCount)

        // Item NÃO é apagado em silêncio! Permanece visível na tabela
        val record = outboxDao.getById("item-perm")
        assertNotNull(record)
        assertEquals(8, record!!.attempts)
        assertTrue(record.lastError?.startsWith("PERMANENT_FAILURE:") == true)

        // Deve constar na lista de falhas permanentes e sumir da fila pendente normal
        val permanentFailures = outboxDao.getPermanentFailures()
        assertEquals(1, permanentFailures.size)
        assertEquals("item-perm", permanentFailures[0].id)
        assertEquals(0, outboxDao.getPendingItems().size)
    }

    @Test
    fun resolucaoDeConflitoPorUltimoUpdatedAtENuncaAlteraRazao() {
        val t1 = Instant.parse("2026-11-17T10:00:00Z")
        val t2 = Instant.parse("2026-11-17T10:05:00Z")

        assertEquals(
            SyncEngine.ConflictResolution.USE_REMOTE,
            syncEngine.resolveMutableConflict(localUpdatedAt = t1, remoteUpdatedAt = t2)
        )
        assertEquals(
            SyncEngine.ConflictResolution.KEEP_LOCAL,
            syncEngine.resolveMutableConflict(localUpdatedAt = t2, remoteUpdatedAt = t1)
        )

        // Tabelas mutáveis podem sofrer mutação; razão (vault_ledger) é ESTRITAMENTE IMUTÁVEL
        assertTrue(syncEngine.canMutateTable("fixed_costs"))
        assertTrue(syncEngine.canMutateTable("vaults"))
        assertFalse("vault_ledger nunca pode ser alterada", syncEngine.canMutateTable("vault_ledger"))
    }
}
