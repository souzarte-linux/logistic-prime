package com.fernando.centraldomotorista.data.repository.costcenter

import androidx.room.withTransaction
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.FixedCost
import com.fernando.centraldomotorista.data.model.FixedCostPayment
import com.fernando.centraldomotorista.data.model.FixedCostVersion
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
class FixedCostRepository @Inject constructor(
    private val localDatabaseProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val syncScheduler: SyncScheduler
) {
    private fun requireUserId(): String =
        userSession.currentUserId() ?: throw IllegalStateException("Sem sessão ativa")

    fun observeActive(): Flow<List<FixedCost>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().fixedCostDao().observeActive(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    fun observeAll(): Flow<List<FixedCost>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().fixedCostDao().observeAll(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    fun observeVersions(fixedCostId: String): Flow<List<FixedCostVersion>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().fixedCostVersionDao().observeByFixedCost(fixedCostId)
                .map { list -> list.map { it.toDomain() } }
        }

    fun observeAllVersions(): Flow<List<FixedCostVersion>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().fixedCostVersionDao().observeAll(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    fun observePayments(fixedCostId: String): Flow<List<FixedCostPayment>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().fixedCostPaymentDao().observeByFixedCost(fixedCostId)
                .map { list -> list.map { it.toDomain() } }
        }

    fun observeAllPayments(): Flow<List<FixedCostPayment>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().fixedCostPaymentDao().observeAll(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    suspend fun getById(id: String): FixedCost? =
        localDatabaseProvider.current().fixedCostDao().getById(id)?.toDomain()

    suspend fun saveFixedCost(fixedCost: FixedCost): FixedCost {
        val userId = requireUserId()
        val now = Instant.now()
        val toSave = fixedCost.copy(
            userId = userId,
            updatedAt = now,
            syncState = SyncState.PENDENTE
        )
        val db = localDatabaseProvider.current()
        db.withTransaction {
            db.fixedCostDao().upsert(toSave.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "fixed_costs",
                    rowId = toSave.id,
                    operation = "UPSERT",
                    dto = toSave.toDto()
                )
            )
        }
        notifyAfterMutation(syncScheduler)
        return toSave
    }

    suspend fun saveVersion(version: FixedCostVersion): FixedCostVersion {
        val userId = requireUserId()
        val now = Instant.now()
        val toSave = version.copy(
            userId = userId,
            amount = version.amount.setScale(2, RoundingMode.HALF_UP),
            updatedAt = now,
            syncState = SyncState.PENDENTE
        )
        val db = localDatabaseProvider.current()
        db.withTransaction {
            db.fixedCostVersionDao().upsert(toSave.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "fixed_cost_versions",
                    rowId = toSave.id,
                    operation = "UPSERT",
                    dto = toSave.toDto()
                )
            )
        }
        notifyAfterMutation(syncScheduler)
        return toSave
    }

    suspend fun savePayment(payment: FixedCostPayment): FixedCostPayment {
        val userId = requireUserId()
        val now = Instant.now()
        val toSave = payment.copy(
            userId = userId,
            amount = payment.amount.setScale(2, RoundingMode.HALF_UP),
            vaultAmount = payment.vaultAmount.setScale(2, RoundingMode.HALF_UP),
            accountAmount = payment.accountAmount.setScale(2, RoundingMode.HALF_UP),
            updatedAt = now,
            syncState = SyncState.PENDENTE
        )
        val db = localDatabaseProvider.current()
        db.withTransaction {
            db.fixedCostPaymentDao().upsert(toSave.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "fixed_cost_payments",
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
     * Custo fixo excluído/arquivado NUNCA remove fixed_cost_versions nem fixed_cost_payments.
     */
    suspend fun archiveOrDelete(id: String): FixedCost? {
        requireUserId()
        val db = localDatabaseProvider.current()
        val existing = db.fixedCostDao().getById(id)?.toDomain() ?: return null
        val hasReference = db.fixedCostVersionDao().existsByFixedCostId(id) ||
            db.fixedCostPaymentDao().existsByFixedCostId(id)

        val now = Instant.now()
        val updated = if (hasReference) {
            existing.copy(active = false, updatedAt = now, syncState = SyncState.PENDENTE)
        } else {
            existing.copy(active = false, deletedAt = now, updatedAt = now, syncState = SyncState.PENDENTE)
        }

        db.withTransaction {
            db.fixedCostDao().upsert(updated.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "fixed_costs",
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
