package com.fernando.centraldomotorista.costcenter

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fernando.centraldomotorista.data.local.CostCenterDatabase
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.ExpenseSettlement
import com.fernando.centraldomotorista.data.model.LedgerRefType
import com.fernando.centraldomotorista.data.model.SettlementKind
import com.fernando.centraldomotorista.data.model.SettlementStatus
import com.fernando.centraldomotorista.data.model.SyncState
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.data.model.VaultPurpose
import com.fernando.centraldomotorista.data.remote.api.ExpenseApi
import com.fernando.centraldomotorista.data.remote.dto.ExpenseDto
import com.fernando.centraldomotorista.data.repository.ExpenseDeletionHooks
import com.fernando.centraldomotorista.data.repository.ExpenseDeletionListener
import com.fernando.centraldomotorista.data.repository.ExpenseRepository
import com.fernando.centraldomotorista.data.repository.costcenter.LedgerRepository
import com.fernando.centraldomotorista.data.repository.costcenter.SettlementReverter
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
class ExpenseDeletionHookTest {

    private lateinit var context: Context
    private lateinit var db: CostCenterDatabase
    private lateinit var userSession: FakeUserSession
    private lateinit var ledgerRepository: LedgerRepository
    private lateinit var settlementReverter: SettlementReverter

    private val userId = "usr-hook-1"
    private val now = Instant.parse("2026-11-20T10:00:00Z")

    private class FakeExpenseApi(private val shouldFailDelete: Boolean = false) : ExpenseApi {
        val deletedIds = mutableListOf<String>()

        override suspend fun getExpenses(
            userIdFilter: String,
            order: String,
            filters: Map<String, String>
        ): List<ExpenseDto> = emptyList()

        override suspend fun createExpense(expense: ExpenseDto): List<ExpenseDto> = listOf(expense)

        override suspend fun updateExpense(idFilter: String, expense: ExpenseDto): List<ExpenseDto> = listOf(expense)

        override suspend fun deleteExpense(idFilter: String) {
            if (shouldFailDelete) throw RuntimeException("Falha HTTP 500 ao excluir")
            deletedIds.add(idFilter)
        }

        override suspend fun getExpenseById(idFilter: String): List<ExpenseDto> = emptyList()
    }

    @Before
    fun setup() {
        ExpenseDeletionHooks.clear()
        context = ApplicationProvider.getApplicationContext()
        db = createInMemoryCostCenterDb(context)
        userSession = FakeUserSession(userId)
        val syncScheduler = createTestSyncScheduler(context)
        val dbProvider = FakeLocalDatabaseProvider(db)
        ledgerRepository = LedgerRepository(dbProvider, userSession, syncScheduler)
        settlementReverter = SettlementReverter(dbProvider, userSession, ledgerRepository, syncScheduler)
    }

    @After
    fun tearDown() {
        ExpenseDeletionHooks.clear()
        db.close()
    }

    @Test
    fun deleteExpenseChamaSettlementReverterEEstornaLiquidezNaRazao() = runTest {
        db.vaultDao().upsert(
            Vault(
                id = "vault-1",
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
        ledgerRepository.registrarPagamento(
            refType = LedgerRefType.DESPESA,
            refId = "exp-1",
            vaultId = "vault-1",
            vaultAmount = BigDecimal("60.00"),
            operationId = "op-exp-1"
        )
        assertEquals(BigDecimal("40.00"), ledgerRepository.getVaultBalance("vault-1"))

        db.expenseSettlementDao().upsert(
            ExpenseSettlement(
                id = "set-exp-1",
                userId = userId,
                expenseId = "exp-1",
                kind = SettlementKind.PIX,
                accountId = "acc-1",
                vaultId = "vault-1",
                amount = BigDecimal("100.00"),
                vaultAmount = BigDecimal("60.00"),
                accountAmount = BigDecimal("40.00"),
                status = SettlementStatus.CONFIRMADA,
                operationId = "op-exp-1",
                createdAt = now,
                updatedAt = now,
                syncState = SyncState.SINCRONIZADO
            ).toEntity()
        )

        ExpenseDeletionHooks.register(settlementReverter)
        val repo = ExpenseRepository(FakeExpenseApi())

        val deleted = repo.deleteExpense("exp-1")
        assertTrue(deleted)

        // Saldo restaurado para 100.00 e liquidação marcada como ABORTADA
        assertEquals(BigDecimal("100.00"), ledgerRepository.getVaultBalance("vault-1"))
        val updatedSettlement = db.expenseSettlementDao().getByExpenseId("exp-1")!!
        assertEquals(SettlementStatus.ABORTADA.key, updatedSettlement.status)
    }

    @Test
    fun falhaNoListenerNaoImpedeDeleteExpenseDeRetornarTrue() = runTest {
        var secondListenerCalled = false
        ExpenseDeletionHooks.register(object : ExpenseDeletionListener {
            override suspend fun onExpenseDeleted(expenseId: String) {
                throw IllegalStateException("Erro simulado no listener")
            }
        })
        ExpenseDeletionHooks.register(object : ExpenseDeletionListener {
            override suspend fun onExpenseDeleted(expenseId: String) {
                secondListenerCalled = true
            }
        })

        val repo = ExpenseRepository(FakeExpenseApi())
        val deleted = repo.deleteExpense("exp-2")

        assertTrue("deleteExpense deve retornar true mesmo com falha no listener", deleted)
        assertTrue("Demais listeners devem continuar sendo executados", secondListenerCalled)
    }

    @Test
    fun falhaNaApiNaoChamaListenerERetornaFalse() = runTest {
        var listenerCalled = false
        ExpenseDeletionHooks.register(object : ExpenseDeletionListener {
            override suspend fun onExpenseDeleted(expenseId: String) {
                listenerCalled = true
            }
        })

        val repo = ExpenseRepository(FakeExpenseApi(shouldFailDelete = true))
        val deleted = repo.deleteExpense("exp-3")

        assertFalse(deleted)
        assertFalse("Listener não deve ser chamado se a exclusão na API falhar", listenerCalled)
    }
}
