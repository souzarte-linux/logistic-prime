package com.fernando.centraldomotorista.data.repository.costcenter

import androidx.room.withTransaction
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.costcenter.toEntity
import com.fernando.centraldomotorista.data.model.CardInvoicePayment
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
class CardInvoicePaymentRepository @Inject constructor(
    private val localDatabaseProvider: LocalDatabaseProvider,
    private val userSession: UserSession,
    private val syncScheduler: SyncScheduler
) {
    private fun requireUserId(): String =
        userSession.currentUserId() ?: throw IllegalStateException("Sem sessão ativa")

    fun observeAll(): Flow<List<CardInvoicePayment>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().cardInvoicePaymentDao().observeAll(userId)
                .map { list -> list.map { it.toDomain() } }
        }

    fun observeByCard(cardId: String): Flow<List<CardInvoicePayment>> =
        userSession.userIdFlow.flatMapLatest { userId ->
            if (userId.isNullOrBlank()) flowOf(emptyList())
            else localDatabaseProvider.current().cardInvoicePaymentDao().observeByCard(cardId)
                .map { list -> list.map { it.toDomain() } }
        }

    suspend fun getById(id: String): CardInvoicePayment? =
        localDatabaseProvider.current().cardInvoicePaymentDao().getById(id)?.toDomain()

    suspend fun save(payment: CardInvoicePayment): CardInvoicePayment {
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
            db.cardInvoicePaymentDao().upsert(toSave.toEntity(SyncState.PENDENTE))
            db.outboxDao().insert(
                buildOutboxEntity(
                    tableName = "card_invoice_payments",
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
