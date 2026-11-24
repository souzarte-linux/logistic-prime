package com.fernando.centraldomotorista.sync

import com.fernando.centraldomotorista.data.local.dao.OutboxDao
import com.fernando.centraldomotorista.data.local.dao.SyncCursorDao
import com.fernando.centraldomotorista.data.local.entity.SyncCursorEntity
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEngine @Inject constructor(
    private val outboxDao: OutboxDao,
    private val syncCursorDao: SyncCursorDao,
    private val registry: SyncHandlerRegistry
) {
    companion object {
        const val MAX_ATTEMPTS = 8
        const val BASE_BACKOFF_MS = 1000L
        const val LEDGER_TABLE = "vault_ledger"
    }

    enum class ConflictResolution {
        USE_REMOTE,
        KEEP_LOCAL
    }

    data class SyncPushSummary(
        val successCount: Int,
        val errorCount: Int,
        val permanentFailureCount: Int
    )

    data class SyncPullSummary(
        val pulledRecordsCount: Int,
        val failedTablesCount: Int
    )

    data class SyncResult(
        val push: SyncPushSummary,
        val pull: SyncPullSummary
    )

    fun calculateBackoffMs(attempts: Int): Long {
        if (attempts <= 0) return 0L
        val exponent = (attempts - 1).coerceAtMost(10)
        return BASE_BACKOFF_MS * (1L shl exponent)
    }

    fun isBackoffElapsed(item: OutboxItem, now: Long = System.currentTimeMillis()): Boolean {
        if (item.attempts <= 0) return true
        val requiredDelay = calculateBackoffMs(item.attempts)
        return (now - item.createdAt) >= requiredDelay
    }

    fun isConflictException(throwable: Throwable): Boolean {
        val msg = throwable.message?.lowercase() ?: ""
        return msg.contains("409") || msg.contains("conflict") || msg.contains("duplicate") || msg.contains("already exists")
    }

    fun resolveMutableConflict(localUpdatedAt: Instant, remoteUpdatedAt: Instant): ConflictResolution {
        return if (remoteUpdatedAt.isAfter(localUpdatedAt)) {
            ConflictResolution.USE_REMOTE
        } else {
            ConflictResolution.KEEP_LOCAL
        }
    }

    fun canMutateTable(table: String): Boolean {
        // NUNCA altera linhas da razão (vault_ledger)
        return table != LEDGER_TABLE
    }

    suspend fun processOutbox(currentTime: Long = System.currentTimeMillis()): SyncPushSummary {
        val pendingEntities = outboxDao.getPendingItems()
        var successCount = 0
        var errorCount = 0
        var permanentFailureCount = 0

        for (entity in pendingEntities) {
            val item = OutboxItem.fromEntity(entity)

            // Verifica se o tempo de backoff exponencial já passou
            if (!isBackoffElapsed(item, currentTime)) {
                continue
            }

            val handler = registry.getHandler(item.tableName)
            if (handler == null) {
                // Sem handler registrado, mantém na fila até o registro
                continue
            }

            val pushResult = try {
                handler.push(item)
            } catch (t: Throwable) {
                if (isConflictException(t)) {
                    PushResult.Conflict409(t.message ?: "409 Conflict")
                } else {
                    PushResult.Failure(t)
                }
            }

            when (pushResult) {
                is PushResult.Success, is PushResult.Conflict409 -> {
                    // Resposta 409 / duplicado tratada como SUCESSO idempotente
                    outboxDao.delete(item.id)
                    successCount++
                }
                is PushResult.Failure -> {
                    val newAttempts = item.attempts + 1
                    val errorMsg = pushResult.cause.message ?: "Unknown push error"
                    if (newAttempts >= MAX_ATTEMPTS) {
                        // Descartando para "falha permanente" após 8 tentativas
                        // Registrando o erro, nunca apagando em silêncio
                        val permanentError = "PERMANENT_FAILURE: $errorMsg"
                        outboxDao.updateAttempts(item.id, newAttempts, permanentError)
                        permanentFailureCount++
                    } else {
                        outboxDao.updateAttempts(item.id, newAttempts, errorMsg)
                        errorCount++
                    }
                }
            }
        }

        return SyncPushSummary(successCount, errorCount, permanentFailureCount)
    }

    suspend fun pullChanges(): SyncPullSummary {
        var pulledCount = 0
        var failedCount = 0

        for (handler in registry.getAllHandlers()) {
            val table = handler.table
            val cursorEntity = syncCursorDao.getCursor(table)
            val sinceInstant = cursorEntity?.lastPulledAt?.let {
                try { Instant.parse(it) } catch (_: Exception) { null }
            }

            val pullResult = try {
                handler.pull(sinceInstant)
            } catch (t: Throwable) {
                PullResult.Failure(t)
            }

            when (pullResult) {
                is PullResult.Success -> {
                    if (pullResult.newCursor != null) {
                        syncCursorDao.saveCursor(
                            SyncCursorEntity(
                                tableName = table,
                                lastPulledAt = pullResult.newCursor.toString()
                            )
                        )
                    }
                    pulledCount += pullResult.recordsCount
                }
                is PullResult.Failure -> {
                    failedCount++
                }
            }
        }

        return SyncPullSummary(pulledCount, failedCount)
    }

    suspend fun syncAll(): SyncResult {
        val push = processOutbox()
        val pull = pullChanges()
        return SyncResult(push, pull)
    }
}
