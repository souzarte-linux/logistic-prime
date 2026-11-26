package com.fernando.centraldomotorista.data.repository.costcenter

import androidx.room.withTransaction
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.CostCenterSettings
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
class CostCenterSettingsRepository @Inject constructor(
    private val localDatabaseProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val syncScheduler: SyncScheduler
) {
    private fun requireUserId(): String =
        userSession.currentUserId() ?: throw IllegalStateException("Sem sessão ativa")

    fun observe(): Flow<CostCenterSettings?> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(null)
            else localDatabaseProvider.current().costCenterSettingsDao().observeByUserId(userId)
                .map { it?.toDomain() }
        }

    suspend fun get(): CostCenterSettings? {
        val userId = userSession.currentUserId() ?: return null
        return localDatabaseProvider.current().costCenterSettingsDao().getByUserId(userId)?.toDomain()
    }

    suspend fun save(settings: CostCenterSettings): CostCenterSettings {
        val userId = requireUserId()
        val now = Instant.now()
        val toSave = settings.copy(
            userId = userId,
            updatedAt = now,
            syncState = SyncState.PENDENTE
        )
        val db = localDatabaseProvider.current()
        db.withTransaction {
            db.costCenterSettingsDao().upsert(toSave.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "cost_center_settings",
                    rowId = toSave.userId,
                    operation = "UPSERT",
                    dto = toSave.toDto()
                )
            )
        }
        notifyAfterMutation(syncScheduler)
        return toSave
    }
}
