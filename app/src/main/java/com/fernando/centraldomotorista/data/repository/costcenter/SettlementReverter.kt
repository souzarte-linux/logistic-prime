package com.fernando.centraldomotorista.data.repository.costcenter

import androidx.room.withTransaction
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.LedgerRefType
import com.fernando.centraldomotorista.data.model.SettlementStatus
import com.fernando.centraldomotorista.data.model.SyncState
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.data.repository.ExpenseDeletionListener
import com.fernando.centraldomotorista.sync.SyncScheduler
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettlementReverter @Inject constructor(
    private val localDatabaseProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val ledgerRepository: LedgerRepository,
    private val syncScheduler: SyncScheduler
) : ExpenseDeletionListener {

    override suspend fun onExpenseDeleted(expenseId: String) {
        val db = localDatabaseProvider.current()
        val settlement = db.expenseSettlementDao().getByExpenseId(expenseId)?.toDomain() ?: return
        if (settlement.status == SettlementStatus.ABORTADA) return

        val userId = userSession.currentUserId() ?: settlement.userId
        val now = Instant.now()
        val reversalOperationId = UUID.nameUUIDFromBytes(
            "revert-expense:${settlement.operationId}".toByteArray()
        ).toString()

        db.withTransaction {
            if (settlement.vaultAmount > BigDecimal.ZERO) {
                ledgerRepository.estornarPorReferenciaInTransaction(
                    db = db,
                    userId = userId,
                    refType = LedgerRefType.DESPESA,
                    refId = expenseId,
                    occurredAt = now,
                    note = "Estorno automático por exclusão da despesa",
                    operationId = reversalOperationId
                )
            }

            val aborted = settlement.copy(
                status = SettlementStatus.ABORTADA,
                updatedAt = now,
                syncState = SyncState.PENDENTE
            )
            db.expenseSettlementDao().upsert(aborted.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "expense_settlements",
                    rowId = aborted.id,
                    operation = "UPSERT",
                    dto = aborted.toDto()
                )
            )
        }
        notifyAfterMutation(syncScheduler)
    }
}
