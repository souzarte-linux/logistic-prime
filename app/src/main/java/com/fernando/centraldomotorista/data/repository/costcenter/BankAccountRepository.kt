package com.fernando.centraldomotorista.data.repository.costcenter

import androidx.room.withTransaction
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.BankAccount
import com.fernando.centraldomotorista.data.model.SyncState
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.sync.SyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class BankAccountRepository @Inject constructor(
    private val localDatabaseProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val syncScheduler: SyncScheduler
) {
    private fun requireUserId(): String =
        userSession.currentUserId() ?: throw IllegalStateException("Sem sessão ativa")

    fun observeActive(): Flow<List<BankAccount>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().bankAccountDao().observeActive(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    fun observeAll(): Flow<List<BankAccount>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().bankAccountDao().observeAll(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    suspend fun getById(id: String): BankAccount? =
        localDatabaseProvider.current().bankAccountDao().getById(id)?.toDomain()

    suspend fun save(account: BankAccount): BankAccount {
        val userId = requireUserId()
        val now = Instant.now()
        val toSave = account.copy(
            userId = userId,
            updatedAt = now,
            syncState = SyncState.PENDENTE
        )
        val db = localDatabaseProvider.current()
        db.withTransaction {
            db.bankAccountDao().upsert(toSave.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "bank_accounts",
                    rowId = toSave.id,
                    operation = "UPSERT",
                    dto = toSave.toDto()
                )
            )
        }
        notifyAfterMutation(syncScheduler)
        return toSave
    }

    /**
     * Exclusão lógica: se houver qualquer referência local, arquiva (active = false);
     * sem referência, marca deletedAt = now e active = false.
     */
    suspend fun archiveOrDelete(id: String): BankAccount? {
        requireUserId()
        val db = localDatabaseProvider.current()
        val existing = db.bankAccountDao().getById(id)?.toDomain() ?: return null
        val hasReference = db.vaultDao().existsByAccountId(id) ||
            db.cardAccountLinkDao().existsByAccountId(id) ||
            db.expenseSettlementDao().existsByAccountId(id) ||
            db.fixedCostPaymentDao().existsByAccountId(id) ||
            db.cardInvoicePaymentDao().existsByAccountId(id)

        val now = Instant.now()
        val updated = if (hasReference) {
            existing.copy(active = false, updatedAt = now, syncState = SyncState.PENDENTE)
        } else {
            existing.copy(active = false, deletedAt = now, updatedAt = now, syncState = SyncState.PENDENTE)
        }

        db.withTransaction {
            db.bankAccountDao().upsert(updated.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "bank_accounts",
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
