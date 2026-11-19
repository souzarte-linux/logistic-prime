package com.fernando.centraldomotorista.sync

import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.entity.SyncCursorEntity
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEngine @Inject constructor(
    private val provider: LocalDatabaseProvider,
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
        val baseTime = item.lastAttemptAt ?: item.createdAt
        return (now - baseTime) >= requiredDelay
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
        val db = provider.current()
        val outboxDao = db.outboxDao()
        val pendingEntities = outboxDao.getPendingItems()
        var successCount = 0
        var errorCount = 0
        var permanentFailureCount = 0

        for (entity in pendingEntities) {
            val item = OutboxItem.fromEntity(entity)

            // Verifica se o tempo de backoff exponencial já passou
            if (!isBackoffElapsed(item, currentTime)) {
                break
            }

            val handler = registry.getHandler(item.tableName)
            if (handler == null) {
                // Sem handler registrado, interrompe a fila (FIFO estrito)
                break
            }

            val pushResult = try {
                handler.push(item)
            } catch (t: Throwable) {
                PushResult.Failure(t)
            }

            when (pushResult) {
                is PushResult.Success, is PushResult.AlreadyApplied -> {
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
                        outboxDao.updateAttempts(item.id, newAttempts, permanentError, currentTime)
                        permanentFailureCount++
                        // Item vira falha permanente (sai de getPendingItems) e o laço continua
                    } else {
                        outboxDao.updateAttempts(item.id, newAttempts, errorMsg, currentTime)
                        errorCount++
                        // FIFO estrito: falha abaixo de 8 tentativas interrompe a fila nesta execução
                        break
                    }
                }
            }
        }

        return SyncPushSummary(successCount, errorCount, permanentFailureCount)
    }

    suspend fun pullChanges(): SyncPullSummary {
        val db = provider.current()
        val syncCursorDao = db.syncCursorDao()
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
