package com.fernando.centraldomotorista.data.repository.costcenter

import androidx.room.withTransaction
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.SyncState
import com.fernando.centraldomotorista.data.model.Vault
import com.fernando.centraldomotorista.data.model.VaultBalance
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.sync.SyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class VaultRepository @Inject constructor(
    private val localDatabaseProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val syncScheduler: SyncScheduler
) {
    private fun requireUserId(): String =
        userSession.currentUserId() ?: throw IllegalStateException("Sem sessão ativa")

    fun observeActive(): Flow<List<Vault>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().vaultDao().observeActive(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    fun observeAll(): Flow<List<Vault>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().vaultDao().observeAll(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    fun observeVaultBalances(): Flow<List<VaultBalance>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else {
                val db = localDatabaseProvider.current()
                combine(
                    db.vaultDao().observeActive(userId),
                    db.vaultLedgerDao().observeAll(userId)
                ) { vaults, ledgerEntries ->
                    val sumsByVault = ledgerEntries.groupBy { it.vaultId }
                    vaults.map { vault ->
                        val entriesSum = sumsByVault[vault.id].orEmpty()
                            .fold(BigDecimal.ZERO) { acc, entry -> acc + entry.amount }
                        VaultBalance(
                            vaultId = vault.id,
                            userId = vault.userId,
                            accountId = vault.accountId,
                            balance = (vault.initialBalance + entriesSum).setScale(2, RoundingMode.HALF_UP)
                        )
                    }
                }
            }
        }

    fun observeVaultBalance(vaultId: String): Flow<BigDecimal> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
            else {
                val db = localDatabaseProvider.current()
                combine(
                    db.vaultDao().observeAll(userId),
                    db.vaultLedgerDao().observeEntriesForVault(vaultId)
                ) { vaults, entries ->
                    val vault = vaults.find { it.id == vaultId }
                    val initial = vault?.initialBalance ?: BigDecimal.ZERO
                    entries.fold(initial) { acc, entry -> acc + entry.amount }
                        .setScale(2, RoundingMode.HALF_UP)
                }
            }
        }

    suspend fun getById(id: String): Vault? =
        localDatabaseProvider.current().vaultDao().getById(id)?.toDomain()

    suspend fun save(vault: Vault): Vault {
        val userId = requireUserId()
        val now = Instant.now()
        val toSave = vault.copy(
            userId = userId,
            initialBalance = vault.initialBalance.setScale(2, RoundingMode.HALF_UP),
            goalAmount = vault.goalAmount?.setScale(2, RoundingMode.HALF_UP),
            updatedAt = now,
            syncState = SyncState.PENDENTE
        )
        val db = localDatabaseProvider.current()
        db.withTransaction {
            db.vaultDao().upsert(toSave.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "vaults",
                    rowId = toSave.id,
                    operation = "UPSERT",
                    dto = toSave.toDto()
                )
            )
        }
        notifyAfterMutation(syncScheduler)
        return toSave
    }

    suspend fun archiveOrDelete(id: String): Vault? {
        requireUserId()
        val db = localDatabaseProvider.current()
        val existing = db.vaultDao().getById(id)?.toDomain() ?: return null
        val hasReference = db.vaultLedgerDao().existsForVault(id) ||
            db.expenseSettlementDao().existsByVaultId(id) ||
            db.fixedCostDao().existsByVaultId(id) ||
            db.fixedCostPaymentDao().existsByVaultId(id) ||
            db.cardInvoicePaymentDao().existsByVaultId(id) ||
            db.reserveAllocationDao().existsByVaultId(id) ||
            db.costCenterSettingsDao().existsByMaintenanceVaultId(id)

        val now = Instant.now()
        val updated = if (hasReference) {
            existing.copy(active = false, updatedAt = now, syncState = SyncState.PENDENTE)
        } else {
            existing.copy(active = false, deletedAt = now, updatedAt = now, syncState = SyncState.PENDENTE)
        }

        db.withTransaction {
            db.vaultDao().upsert(updated.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "vaults",
                    rowId = updated.id,
                    operation = "UPSERT",
                    dto = updated.toDto()
                )
            )
        }
        notifyAfterMutation(syncScheduler)
        return updated
    }
}
