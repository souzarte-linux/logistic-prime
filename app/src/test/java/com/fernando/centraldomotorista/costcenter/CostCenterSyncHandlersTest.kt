package com.fernando.centraldomotorista.costcenter

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fernando.centraldomotorista.data.local.CostCenterDatabase
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.AccountType
import com.fernando.centraldomotorista.data.model.BankAccount
import com.fernando.centraldomotorista.data.model.LedgerEntry
import com.fernando.centraldomotorista.data.model.LedgerEntryType
import com.fernando.centraldomotorista.data.model.LedgerRefType
import com.fernando.centraldomotorista.data.model.SyncState
import com.fernando.centraldomotorista.data.remote.api.BankAccountApi
import com.fernando.centraldomotorista.data.remote.api.VaultLedgerApi
import com.fernando.centraldomotorista.data.remote.dto.BankAccountDto
import com.fernando.centraldomotorista.data.remote.dto.VaultLedgerDto
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.sync.OutboxItem
import com.fernando.centraldomotorista.sync.PullResult
import com.fernando.centraldomotorista.sync.PushResult
import com.fernando.centraldomotorista.sync.SyncEngine
import com.fernando.centraldomotorista.sync.SyncHandlerRegistry
import com.fernando.centraldomotorista.sync.handlers.BankAccountSyncHandler
import com.fernando.centraldomotorista.sync.handlers.VaultLedgerSyncHandler
import com.google.gson.JsonObject
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.math.BigDecimal
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CostCenterSyncHandlersTest {

    private lateinit var context: Context
    private lateinit var db: CostCenterDatabase
    private lateinit var dbProvider: FakeLocalDatabaseProvider
    private lateinit var userSession: FakeUserSession
    private lateinit var syncEngine: SyncEngine

    private val userId = "usr-sync-1"
    private val t1 = Instant.parse("2026-11-20T10:00:00Z")
    private val t2 = Instant.parse("2026-11-20T11:00:00Z")
    private val t3 = Instant.parse("2026-11-20T12:00:00Z")

    private class FakeBankAccountApi : BankAccountApi {
        var remoteItems: List<BankAccountDto> = emptyList()
        var upsertResponse: Response<List<BankAccountDto>> = Response.success(emptyList())

        override suspend fun getAll(
            userIdFilter: String,
            updatedAtFilter: String?,
            order: String
        ): Response<List<BankAccountDto>> = Response.success(remoteItems)

        override suspend fun upsert(
            onConflict: String,
            body: JsonObject
        ): Response<List<BankAccountDto>> = upsertResponse
    }

    private class FakeVaultLedgerApi : VaultLedgerApi {
        var remoteItems: List<VaultLedgerDto> = emptyList()
        var insertResponse: Response<List<VaultLedgerDto>> = Response.success(emptyList())

        override suspend fun getAll(
            userIdFilter: String,
            createdAtFilter: String?,
            order: String
        ): Response<List<VaultLedgerDto>> = Response.success(remoteItems)

        override suspend fun insert(
            onConflict: String,
            body: JsonObject
        ): Response<List<VaultLedgerDto>> = insertResponse
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = createInMemoryCostCenterDb(context)
        dbProvider = FakeLocalDatabaseProvider(db)
        userSession = FakeUserSession(userId)
        syncEngine = SyncEngine(dbProvider, SyncHandlerRegistry())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun pullDaRazaoNuncaSobrescreveLinhaExistenteEInsereNovas() = runTest {
        val localEntry = LedgerEntry(
            id = "led-1",
            userId = userId,
            vaultId = "vlt-1",
            operationId = "op-1",
            leg = 0,
            entryType = LedgerEntryType.APORTE,
            amount = BigDecimal("100.00"),
            occurredAt = t1,
            refType = LedgerRefType.MANUAL,
            note = "Original local",
            createdAt = t1,
            syncState = SyncState.PENDENTE
        )
        db.vaultLedgerDao().insertIgnore(localEntry.toEntity())

        // Servidor retorna o mesmo id (ou mesmo operation_id + leg) com valor alterado + uma nova linha
        val tamperedRemote = localEntry.copy(
            amount = BigDecimal("999.00"),
            note = "Alterado no servidor"
        ).toDto()
        val newRemote = LedgerEntry(
            id = "led-2",
            userId = userId,
            vaultId = "vlt-1",
            operationId = "op-2",
            leg = 0,
            entryType = LedgerEntryType.APORTE,
            amount = BigDecimal("50.00"),
            occurredAt = t2,
            refType = LedgerRefType.MANUAL,
            note = "Novo do servidor",
            createdAt = t2,
            syncState = SyncState.SINCRONIZADO
        ).toDto()

        val api = FakeVaultLedgerApi().apply {
            remoteItems = listOf(tamperedRemote, newRemote)
        }
        val handler = VaultLedgerSyncHandler(dbProvider, userSession, api)

        val result = handler.pull(since = null)
        assertTrue(result is PullResult.Success)
        val success = result as PullResult.Success
        assertEquals(2, success.recordsCount)
        assertEquals(t2, success.newCursor)

        // Linha existente NÃO pode ter sido sobrescrita
        val kept = db.vaultLedgerDao().getById("led-1")!!
        assertEquals(BigDecimal("100.00"), kept.amount)
        assertEquals("Original local", kept.note)
        assertEquals(SyncState.PENDENTE.name, kept.syncState)

        // Linha nova deve ter sido inserida como SINCRONIZADO
        val inserted = db.vaultLedgerDao().getById("led-2")
        assertNotNull(inserted)
        assertEquals(BigDecimal("50.00"), inserted!!.amount)
        assertEquals(SyncState.SINCRONIZADO.name, inserted.syncState)
    }

    @Test
    fun pullEmTabelaMutavelAplicaLwwRespeitandoUpdatedAtEPendenteLocal() = runTest {
        // Caso 1: registro local PENDENTE com updatedAt t2 (mais novo que remoto t1) -> mantém local
        val localNewerPending = BankAccount(
            id = "acc-keep-local",
            userId = userId,
            name = "Nome Local Mais Novo",
            accountType = AccountType.CORRENTE,
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.PENDENTE
        )
        // Caso 2: registro local PENDENTE com updatedAt t1 (mais antigo que remoto t3) -> usa remoto
        val localOlderPending = BankAccount(
            id = "acc-use-remote",
            userId = userId,
            name = "Nome Local Antigo",
            accountType = AccountType.CORRENTE,
            createdAt = t1,
            updatedAt = t1,
            syncState = SyncState.PENDENTE
        )
        // Caso 3: registro local SINCRONIZADO -> remoto sempre atualiza
        val localSynced = BankAccount(
            id = "acc-synced",
            userId = userId,
            name = "Nome Antigo Sincronizado",
            accountType = AccountType.CORRENTE,
            createdAt = t1,
            updatedAt = t2,
            syncState = SyncState.SINCRONIZADO
        )
        db.bankAccountDao().upsert(localNewerPending.toEntity())
        db.bankAccountDao().upsert(localOlderPending.toEntity())
        db.bankAccountDao().upsert(localSynced.toEntity())

        val api = FakeBankAccountApi().apply {
            remoteItems = listOf(
                localNewerPending.copy(name = "Remoto Antigo", updatedAt = t1).toDto(),
                localOlderPending.copy(name = "Remoto Mais Novo", updatedAt = t3).toDto(),
                localSynced.copy(name = "Remoto Atualizado", updatedAt = t1).toDto()
            )
        }
        val handler = BankAccountSyncHandler(
            dbProvider = dbProvider,
            userSession = userSession,
            syncEngine = Lazy { syncEngine },
            api = api
        )

        val result = handler.pull(since = null)
        assertTrue(result is PullResult.Success)
        assertEquals(t3, (result as PullResult.Success).newCursor)

        val keepLocal = db.bankAccountDao().getById("acc-keep-local")!!
        assertEquals("Nome Local Mais Novo", keepLocal.name)
        assertEquals(SyncState.PENDENTE.name, keepLocal.syncState)

        val useRemote = db.bankAccountDao().getById("acc-use-remote")!!
        assertEquals("Remoto Mais Novo", useRemote.name)
        assertEquals(SyncState.SINCRONIZADO.name, useRemote.syncState)

        val syncedUpdated = db.bankAccountDao().getById("acc-synced")!!
        assertEquals("Remoto Atualizado", syncedUpdated.name)
        assertEquals(SyncState.SINCRONIZADO.name, syncedUpdated.syncState)
    }

    @Test
    fun pushComConflito409OuSqlState23505MarcaSincronizadoERetornaAlreadyApplied() = runTest {
        val account = BankAccount(
            id = "acc-push-1",
            userId = userId,
            name = "Conta Push",
            createdAt = t1,
            updatedAt = t1,
            syncState = SyncState.PENDENTE
        )
        db.bankAccountDao().upsert(account.toEntity())

        val errorJson = """{"code":"23505","message":"duplicate key"}"""
        val api = FakeBankAccountApi().apply {
            upsertResponse = Response.error(
                409,
                errorJson.toResponseBody("application/json".toMediaType())
            )
        }
        val handler = BankAccountSyncHandler(
            dbProvider = dbProvider,
            userSession = userSession,
            syncEngine = Lazy { syncEngine },
            api = api
        )

        val pushResult = handler.push(
            OutboxItem(
                id = "out-1",
                tableName = "bank_accounts",
                rowId = "acc-push-1",
                operation = "UPSERT",
                payloadJson = "{}",
                attempts = 0,
                createdAt = 1000L
            )
        )

        assertEquals(PushResult.AlreadyApplied, pushResult)
        assertEquals(SyncState.SINCRONIZADO.name, db.bankAccountDao().getById("acc-push-1")!!.syncState)
    }
}
