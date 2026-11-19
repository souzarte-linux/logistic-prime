package com.fernando.centraldomotorista.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.CostCenterDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {

    private lateinit var context: Context
    private lateinit var database: CostCenterDatabase
    private lateinit var registry: SyncHandlerRegistry
    private lateinit var syncEngine: SyncEngine

    class FakeUserSession(var userId: String? = null) : UserSession {
        override val userIdFlow: Flow<String?> get() = flowOf(userId)
        override fun currentUserId(): String? = userId
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, CostCenterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        registry = SyncHandlerRegistry()
        syncEngine = SyncEngine(database.outboxDao(), database.syncCursorDao(), registry)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun syncWorkerSemSessaoRetornaSuccessSemChamarHandlers() = runTest {
        val sessionWithoutUser = FakeUserSession(userId = null)
        var handlerCalled = false

        registry.register(object : TableSyncHandler {
            override val table: String = "fixed_costs"
            override suspend fun push(item: OutboxItem): PushResult {
                handlerCalled = true
                return PushResult.Success
            }
            override suspend fun pull(since: Instant?): PullResult {
                handlerCalled = true
                return PullResult.Success(null, 0)
            }
        })

        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SyncWorker(appContext, workerParameters, sessionWithoutUser, syncEngine)
                }
            })
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertFalse("Nenhum handler deve ser chamado quando não houver sessão ativa", handlerCalled)
    }

    @Test
    fun syncWorkerComSessaoRetornaSuccess() = runTest {
        val sessionWithUser = FakeUserSession(userId = "user-authenticated")

        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SyncWorker(appContext, workerParameters, sessionWithUser, syncEngine)
                }
            })
            .build()

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
