package com.fernando.centraldomotorista.costcenter

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.fernando.centraldomotorista.data.local.CostCenterDatabase
import com.fernando.centraldomotorista.data.model.AccountType
import com.fernando.centraldomotorista.data.model.BankAccount
import com.fernando.centraldomotorista.data.model.FixedCost
import com.fernando.centraldomotorista.data.model.FixedCostCategory
import com.fernando.centraldomotorista.data.model.FixedCostPayment
import com.fernando.centraldomotorista.data.model.FixedCostPeriodicity
import com.fernando.centraldomotorista.data.model.FixedCostVersion
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.data.model.VaultPurpose
import com.fernando.centraldomotorista.data.repository.costcenter.BankAccountRepository
import com.fernando.centraldomotorista.data.repository.costcenter.FixedCostRepository
import com.fernando.centraldomotorista.data.repository.costcenter.LedgerRepository
import com.fernando.centraldomotorista.data.repository.costcenter.VaultRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class LogicalDeletionTest {

    private lateinit var context: Context
    private lateinit var db: CostCenterDatabase
    private lateinit var userSession: FakeUserSession
    private lateinit var bankAccountRepository: BankAccountRepository
    private lateinit var vaultRepository: VaultRepository
    private lateinit var fixedCostRepository: FixedCostRepository
    private lateinit var ledgerRepository: LedgerRepository

    private val userId = "usr-del-1"
    private val now = Instant.parse("2026-11-20T10:00:00Z")
    private val today = LocalDate.of(2026, 11, 20)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = createInMemoryCostCenterDb(context)
        userSession = FakeUserSession(userId)
        val syncScheduler = createTestSyncScheduler(context)
        val dbProvider = FakeLocalDatabaseProvider(db)
        bankAccountRepository = BankAccountRepository(dbProvider, userSession, syncScheduler)
        vaultRepository = VaultRepository(dbProvider, userSession, syncScheduler)
        fixedCostRepository = FixedCostRepository(dbProvider, userSession, syncScheduler)
        ledgerRepository = LedgerRepository(dbProvider, userSession, syncScheduler)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun contaSemReferenciaRecebeDeletedAtEContaComReferenciaApenasArquiva() = runTest {
        val accNoRef = bankAccountRepository.save(
            BankAccount(
                id = "acc-no-ref",
                userId = userId,
                name = "Conta Sem Ref",
                accountType = AccountType.CORRENTE,
                createdAt = now,
                updatedAt = now
            )
        )
        val deleted = bankAccountRepository.archiveOrDelete(accNoRef.id)!!
        assertFalse(deleted.active)
        assertNotNull("Conta sem referência deve receber deletedAt", deleted.deletedAt)

        val accWithRef = bankAccountRepository.save(
            BankAccount(
                id = "acc-with-ref",
                userId = userId,
                name = "Conta Com Ref",
                accountType = AccountType.CORRENTE,
                createdAt = now,
                updatedAt = now
            )
        )
        vaultRepository.save(
            Vault(
                id = "vlt-ref-acc",
                userId = userId,
                accountId = accWithRef.id,
                name = "Caixinha vinculada",
                purpose = VaultPurpose.OUTRO,
                trackingStartDate = today,
                createdAt = now,
                updatedAt = now
            )
        )
        val archived = bankAccountRepository.archiveOrDelete(accWithRef.id)!!
        assertFalse(archived.active)
        assertNull("Conta referenciada deve ser apenas arquivada (deletedAt == null)", archived.deletedAt)
    }

    @Test
    fun caixinhaSemReferenciaRecebeDeletedAtECaixinhaComLancamentoApenasArquiva() = runTest {
        val vltNoRef = vaultRepository.save(
            Vault(
                id = "vlt-no-ref",
                userId = userId,
                name = "Caixinha Livre",
                purpose = VaultPurpose.VIAGEM,
                trackingStartDate = today,
                createdAt = now,
                updatedAt = now
            )
        )
        val deleted = vaultRepository.archiveOrDelete(vltNoRef.id)!!
        assertFalse(deleted.active)
        assertNotNull(deleted.deletedAt)

        val vltWithRef = vaultRepository.save(
            Vault(
                id = "vlt-with-ref",
                userId = userId,
                name = "Caixinha Usada",
                purpose = VaultPurpose.MANUTENCAO,
                trackingStartDate = today,
                createdAt = now,
                updatedAt = now
            )
        )
        ledgerRepository.aportar(vltWithRef.id, BigDecimal("50.00"), operationId = "op-vlt-ref")

        val archived = vaultRepository.archiveOrDelete(vltWithRef.id)!!
        assertFalse(archived.active)
        assertNull(archived.deletedAt)
    }

    @Test
    fun custoFixoExcluidoOuArquivadoPreservaVersoesEPagamentos() = runTest {
        // Sem referência -> recebe deletedAt
        val fcNoRef = fixedCostRepository.saveFixedCost(
            FixedCost(
                id = "fc-no-ref",
                userId = userId,
                name = "Sem histórico",
                category = FixedCostCategory.OUTRO,
                periodicity = FixedCostPeriodicity.MENSAL,
                createdAt = now,
                updatedAt = now
            )
        )
        val fcDeleted = fixedCostRepository.archiveOrDelete(fcNoRef.id)!!
        assertFalse(fcDeleted.active)
        assertNotNull(fcDeleted.deletedAt)

        // Com versões e pagamentos -> arquiva e preserva versões e pagamentos
        val fcWithHistory = fixedCostRepository.saveFixedCost(
            FixedCost(
                id = "fc-hist",
                userId = userId,
                name = "Seguro",
                category = FixedCostCategory.VEICULO_PROTECAO,
                periodicity = FixedCostPeriodicity.MENSAL,
                createdAt = now,
                updatedAt = now
            )
        )
        fixedCostRepository.saveVersion(
            FixedCostVersion(
                id = "fcv-hist-1",
                userId = userId,
                fixedCostId = fcWithHistory.id,
                amount = BigDecimal("220.00"),
                effectiveFrom = today,
                createdAt = now,
                updatedAt = now
            )
        )
        fixedCostRepository.savePayment(
            FixedCostPayment(
                id = "fcp-hist-1",
                userId = userId,
                fixedCostId = fcWithHistory.id,
                competenceDate = today.withDayOfMonth(1),
                paidAt = now,
                amount = BigDecimal("220.00"),
                accountAmount = BigDecimal("220.00"),
                operationId = "op-fc-hist-1",
                createdAt = now,
                updatedAt = now
            )
        )

        val archived = fixedCostRepository.archiveOrDelete(fcWithHistory.id)!!
        assertFalse(archived.active)
        assertNull(archived.deletedAt)

        fixedCostRepository.observeVersions(fcWithHistory.id).test {
            val versions = awaitItem()
            assertEquals(1, versions.size)
            assertEquals("fcv-hist-1", versions[0].id)
            cancelAndIgnoreRemainingEvents()
        }
        fixedCostRepository.observePayments(fcWithHistory.id).test {
            val payments = awaitItem()
            assertEquals(1, payments.size)
            assertEquals("fcp-hist-1", payments[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
