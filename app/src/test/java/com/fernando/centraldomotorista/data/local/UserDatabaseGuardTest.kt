package com.fernando.centraldomotorista.data.local

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.entity.OutboxEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class UserDatabaseGuardTest {

    private lateinit var context: Context
    private lateinit var fakeUserSession: FakeUserSession
    private lateinit var guard: UserDatabaseGuard

    class FakeUserSession(var userId: String? = null) : UserSession {
        override val userIdFlow: Flow<String?> get() = flowOf(userId)
        override fun currentUserId(): String? = userId
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeUserSession = FakeUserSession("user-123")
        guard = UserDatabaseGuard(context, fakeUserSession)
    }

    @Test
    fun trocaDeUsuarioFechaOBancoSemApagarArquivo() = runTest {
        // 1. Inicializa o banco para o usuário 123
        val dbUser1 = guard.getOrCreateDatabase("user-123")
        val dbFile1 = context.getDatabasePath(guard.getDatabaseName("user-123"))

        // Insere registro para materializar o arquivo em disco
        dbUser1.outboxDao().insert(
            OutboxEntity(
                id = "dummy",
                tableName = "fixed_costs",
                rowId = "r1",
                operation = "INSERT",
                payloadJson = "{}"
            )
        )
        assertTrue("Arquivo do banco do usuário deve existir no disco", dbFile1.exists())

        // 2. Simula troca de usuário no guard
        guard.onUserChanged(oldUserId = "user-123", newUserId = "user-456")

        // 3. O banco do usuário anterior é fechado, mas o arquivo NÃO é apagado
        assertTrue("Arquivo do banco do usuário anterior deve ser mantido no disco", dbFile1.exists())
        assertFalse("Banco do usuário anterior deve estar fechado", dbUser1.isOpen)

        // 4. Ao inicializar banco para o novo usuário, um novo arquivo é criado
        fakeUserSession.userId = "user-456"
        val dbUser2 = guard.getOrCreateDatabase("user-456")
        assertNotNull(dbUser2)
        assertEquals("user-456", guard.getActiveUserId())
        val dbFile2 = context.getDatabasePath(guard.getDatabaseName("user-456"))

        dbUser2.outboxDao().insert(
            OutboxEntity(
                id = "dummy2",
                tableName = "fixed_costs",
                rowId = "r2",
                operation = "INSERT",
                payloadJson = "{}"
            )
        )
        assertTrue("Arquivo do banco do novo usuário deve existir", dbFile2.exists())

        // Limpeza
        guard.closeAndDeleteDatabase("user-123")
        guard.closeAndDeleteDatabase("user-456")
        assertFalse(dbFile1.exists())
        assertFalse(dbFile2.exists())
    }

    @Test
    fun sweepOtherUsersApagaBancoComOutboxVazioEPreservaComItemPendente() = runTest {
        // 7.2 (f) sweepOtherUsers apaga banco de outro usuário com outbox vazio e PRESERVA o de outro usuário com 1 item pendente

        // Usuário vazio: cria tabela e deixa 0 registros
        val dbEmpty = guard.getOrCreateDatabase("user-empty")
        dbEmpty.outboxDao().insert(
            OutboxEntity(
                id = "temp",
                tableName = "fixed_costs",
                rowId = "r",
                operation = "INSERT",
                payloadJson = "{}"
            )
        )
        dbEmpty.outboxDao().delete("temp")
        assertEquals(0, dbEmpty.outboxDao().count())
        val fileEmpty = context.getDatabasePath(guard.getDatabaseName("user-empty"))
        assertTrue(fileEmpty.exists())

        // Usuário com item pendente
        val dbPending = guard.getOrCreateDatabase("user-pending")
        dbPending.outboxDao().insert(
            OutboxEntity(
                id = "pending-1",
                tableName = "fixed_costs",
                rowId = "r1",
                operation = "INSERT",
                payloadJson = "{}"
            )
        )
        assertEquals(1, dbPending.outboxDao().count())
        val filePending = context.getDatabasePath(guard.getDatabaseName("user-pending"))
        assertTrue(filePending.exists())

        // Usuário ativo atual é "user-active"
        fakeUserSession.userId = "user-active"
        val dbActive = guard.getOrCreateDatabase("user-active")
        // Materializa banco em disco
        dbActive.outboxDao().count()
        val fileActive = context.getDatabasePath(guard.getDatabaseName("user-active"))
        assertTrue(fileActive.exists())

        // Executa varredura
        guard.sweepOtherUsers("user-active")

        assertFalse("Banco de outro usuário com outbox vazio deve ser apagado", fileEmpty.exists())
        assertTrue("Banco de outro usuário com 1 item pendente DEVE ser preservado", filePending.exists())
        assertTrue("Banco do usuário ativo DEVE ser preservado", fileActive.exists())

        // Limpeza
        guard.closeAndDeleteDatabase("user-pending")
        guard.closeAndDeleteDatabase("user-active")
    }

    @Test(expected = IllegalStateException::class)
    fun getOrCreateDatabaseSemUsuarioLancaIllegalStateException() {
        // 7.2 (g) getOrCreateDatabase sem usuário lança IllegalStateException
        fakeUserSession.userId = null
        guard.getOrCreateDatabase()
    }

    @Test(expected = IllegalStateException::class)
    fun getOrCreateDatabaseComForcedUserIdVazioLancaIllegalStateException() {
        // 7.2 (g) getOrCreateDatabase com userId em branco lança IllegalStateException
        guard.getOrCreateDatabase(forcedUserId = "")
    }

    @Test
    fun trocarDeUsuarioEChamarProviderCurrentDevolveBancoDoNovoUsuario() = runTest {
        // 7.2 (h) trocar de usuário e chamar provider.current() devolve o banco do novo usuário, não o antigo
        val provider = GuardedDatabaseProvider(guard)

        fakeUserSession.userId = "user-A"
        val dbA = provider.current()
        dbA.outboxDao().count() // Abre o banco A
        assertTrue(dbA.isOpen)
        assertEquals("user-A", guard.getActiveUserId())

        fakeUserSession.userId = "user-B"
        val dbB = provider.current()
        dbB.outboxDao().count() // Abre o banco B
        assertTrue(dbB.isOpen)
        assertEquals("user-B", guard.getActiveUserId())
        assertNotEquals(dbA, dbB)
        assertFalse("Banco do usuário anterior deve estar fechado", dbA.isOpen)

        // Limpeza
        guard.closeAndDeleteDatabase("user-A")
        guard.closeAndDeleteDatabase("user-B")
    }
}
