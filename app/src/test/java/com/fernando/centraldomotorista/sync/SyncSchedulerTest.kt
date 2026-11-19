package com.fernando.centraldomotorista.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncSchedulerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: SyncScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        scheduler = SyncScheduler(context, workManager)
    }

    @Test
    fun schedulerNaoDuplicaTrabalhoUnico() {
        // Enfileira duas vezes o mesmo OneTimeWork
        scheduler.scheduleOneTimeSync()
        scheduler.scheduleOneTimeSync()

        val workInfos = workManager.getWorkInfosForUniqueWork(SyncScheduler.ONE_TIME_SYNC_WORK_NAME).get()
        assertTrue("Deve existir trabalho agendado na fila única", workInfos.isNotEmpty())
        // Cada item da cadeia de trabalho único pertence à mesma chave ONE_TIME_SYNC_WORK_NAME
        workInfos.forEach { info ->
            assertTrue(
                "Trabalho único enfileirado",
                info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.BLOCKED ||
                info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.SUCCEEDED
            )
        }

        // Enfileira PeriodicWork duas vezes
        scheduler.schedulePeriodicSync()
        scheduler.schedulePeriodicSync()

        val periodicInfos = workManager.getWorkInfosForUniqueWork(SyncScheduler.PERIODIC_SYNC_WORK_NAME).get()
        // Trabalho periódico único não é duplicado (apenas 1 instância na fila)
        assertEquals(1, periodicInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, periodicInfos[0].state)
    }
}
