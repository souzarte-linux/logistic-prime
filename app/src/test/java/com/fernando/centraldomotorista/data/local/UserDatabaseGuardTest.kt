package com.fernando.centraldomotorista.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.entity.OutboxEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
    fun trocaDeUsuarioApagaOBancoLocal() = runTest {
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

        // 2. Simula troca de usuário no guard (D19)
        guard.onUserChanged(oldUserId = "user-123", newUserId = "user-456")

        // 3. O banco do usuário anterior deve ser apagado
        assertFalse("Banco do usuário anterior deve ser deletado ao trocar de usuário", dbFile1.exists())

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
        guard.closeAndDeleteDatabase("user-456")
        assertFalse(dbFile2.exists())
    }
}
