package com.fernando.centraldomotorista.data.repository.costcenter

import androidx.room.withTransaction
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.CardAccountLink
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
class CardAccountLinkRepository @Inject constructor(
    private val localDatabaseProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val syncScheduler: SyncScheduler
) {
    private fun requireUserId(): String =
        userSession.currentUserId() ?: throw IllegalStateException("Sem sessão ativa")

    fun observeAll(): Flow<List<CardAccountLink>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().cardAccountLinkDao().observeAll(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    suspend fun getByCardId(cardId: String): CardAccountLink? =
        localDatabaseProvider.current().cardAccountLinkDao().getByCardId(cardId)?.toDomain()

    suspend fun save(link: CardAccountLink): CardAccountLink {
        val userId = requireUserId()
        val now = Instant.now()
        val toSave = link.copy(
            userId = userId,
            updatedAt = now,
            syncState = SyncState.PENDENTE
        )
        val db = localDatabaseProvider.current()
        db.withTransaction {
            db.cardAccountLinkDao().upsert(toSave.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "card_account_links",
                    rowId = toSave.id,
                    operation = "UPSERT",
                    dto = toSave.toDto()
                )
            )
        }
        notifyAfterMutation(syncScheduler)
        return toSave
    }

    suspend fun softDelete(id: String): CardAccountLink? {
        requireUserId()
        val db = localDatabaseProvider.current()
        val existing = db.cardAccountLinkDao().getById(id)?.toDomain() ?: return null
        val now = Instant.now()
        val updated = existing.copy(
            deletedAt = now,
            updatedAt = now,
            syncState = SyncState.PENDENTE
        )
        db.withTransaction {
            db.cardAccountLinkDao().upsert(updated.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "card_account_links",
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
