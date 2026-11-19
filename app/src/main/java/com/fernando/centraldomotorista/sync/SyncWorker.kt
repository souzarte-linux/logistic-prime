package com.fernando.centraldomotorista.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fernando.centraldomotorista.auth.UserSession
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userSession: UserSession,
    private val syncEngine: SyncEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = userSession.currentUserId()
        if (userId.isNullOrBlank()) {
            // Exige sessão válida: sem sessão retorna Result.success() sem processar
            return Result.success()
        }

        return try {
            val syncResult = syncEngine.syncAll()
            if (syncResult.push.errorCount > 0 || syncResult.pull.failedTablesCount > 0) {
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.success()
                }
            } else {
                Result.success()
            }
        } catch (c: CancellationException) {
            throw c
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
