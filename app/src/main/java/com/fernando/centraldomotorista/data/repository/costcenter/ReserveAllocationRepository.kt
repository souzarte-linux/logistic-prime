package com.fernando.centraldomotorista.data.repository.costcenter

import androidx.room.withTransaction
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.ReserveAllocation
import com.fernando.centraldomotorista.data.model.ReserveAllocationDay
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
class ReserveAllocationRepository @Inject constructor(
    private val localDatabaseProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val syncScheduler: SyncScheduler
) {
    private fun requireUserId(): String =
        userSession.currentUserId() ?: throw IllegalStateException("Sem sessão ativa")

    fun observeAll(): Flow<List<ReserveAllocation>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().reserveAllocationDao().observeAll(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    fun observeByCycle(billingCycleId: String): Flow<List<ReserveAllocation>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().reserveAllocationDao().observeByCycle(billingCycleId)
                .map { list -> list.map { it.toDomain() } }
        }

    fun observeDays(allocationId: String): Flow<List<ReserveAllocationDay>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().reserveAllocationDayDao().observeByAllocation(allocationId)
                .map { list -> list.map { it.toDomain() } }
        }

    suspend fun getByCycle(billingCycleId: String): List<ReserveAllocation> =
        localDatabaseProvider.current().reserveAllocationDao().getByCycle(billingCycleId)
            .map { it.toDomain() }

    suspend fun getDays(allocationId: String): List<ReserveAllocationDay> =
        localDatabaseProvider.current().reserveAllocationDayDao().getByAllocation(allocationId)
            .map { it.toDomain() }

    suspend fun saveWithDays(
        allocation: ReserveAllocation,
        days: List<ReserveAllocationDay>
    ): ReserveAllocation {
        val userId = requireUserId()
        val now = Instant.now()
        val toSaveAllocation = allocation.copy(
            userId = userId,
            suggestedAmount = allocation.suggestedAmount.setScale(2, RoundingMode.HALF_UP),
            amount = allocation.amount.setScale(2, RoundingMode.HALF_UP),
            updatedAt = now,
            syncState = SyncState.PENDENTE
        )
        val toSaveDays = days.map { day ->
            day.copy(
                userId = userId,
                allocationId = toSaveAllocation.id,
                suggestedAmount = day.suggestedAmount.setScale(6, RoundingMode.HALF_UP),
                updatedAt = now,
                syncState = SyncState.PENDENTE
            )
        }

        val db = localDatabaseProvider.current()
        db.withTransaction {
            db.reserveAllocationDao().upsert(toSaveAllocation.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "reserve_allocations",
                    rowId = toSaveAllocation.id,
                    operation = "UPSERT",
                    dto = toSaveAllocation.toDto()
                )
            )
            if (toSaveDays.isNotEmpty()) {
                db.reserveAllocationDayDao().upsertAll(toSaveDays.map { it.toEntity(SyncState.PENDENTE) })
                db.outboxDao().insertAll(
                    toSaveDays.map { day ->
                        buildOutboxEntity(
                            tableName = "reserve_allocation_days",
                            rowId = day.id,
                            operation = "UPSERT",
                            dto = day.toDto()
                        )
                    }
                )
            }
        }
        notifyAfterMutation(syncScheduler)
        return toSaveAllocation
    }

    suspend fun save(allocation: ReserveAllocation): ReserveAllocation =
        saveWithDays(allocation, emptyList())
}
