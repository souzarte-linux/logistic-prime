package com.fernando.centraldomotorista.data.repository.costcenter

import androidx.room.withTransaction
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.ExpenseSettlement
import com.fernando.centraldomotorista.data.model.SyncState
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.sync.SyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.math.RoundingMode
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SettlementRepository @Inject constructor(
    private val localDatabaseProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val syncScheduler: SyncScheduler
) {
    private fun requireUserId(): String =
        userSession.currentUserId() ?: throw IllegalStateException("Sem sessão ativa")

    fun observeAll(): Flow<List<ExpenseSettlement>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().expenseSettlementDao().observeAll(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    suspend fun getByExpenseId(expenseId: String): ExpenseSettlement? =
        localDatabaseProvider.current().expenseSettlementDao().getByExpenseId(expenseId)?.toDomain()

    suspend fun getById(id: String): ExpenseSettlement? =
        localDatabaseProvider.current().expenseSettlementDao().getById(id)?.toDomain()

    suspend fun save(settlement: ExpenseSettlement): ExpenseSettlement {
        val userId = requireUserId()
        val now = Instant.now()
        val toSave = settlement.copy(
            userId = userId,
            amount = settlement.amount.setScale(2, RoundingMode.HALF_UP),
            vaultAmount = settlement.vaultAmount.setScale(2, RoundingMode.HALF_UP),
            accountAmount = settlement.accountAmount.setScale(2, RoundingMode.HALF_UP),
            updatedAt = now,
            syncState = SyncState.PENDENTE
        )
        val db = localDatabaseProvider.current()
        db.withTransaction {
            db.expenseSettlementDao().upsert(toSave.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "expense_settlements",
                    rowId = toSave.id,
                    operation = "UPSERT",
                    dto = toSave.toDto()
                )
            )
        }
        notifyAfterMutation(syncScheduler)
        return toSave
    }
}
