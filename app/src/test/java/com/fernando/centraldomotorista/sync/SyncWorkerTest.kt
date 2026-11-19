package com.fernando.centraldomotorista.sync

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.CostCenterDatabase
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.entity.OutboxEntity
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
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SyncWorkerTest {

    private lateinit var context: Context
    private lateinit var database: CostCenterDatabase
    private lateinit var registry: SyncHandlerRegistry
    private lateinit var syncEngine: SyncEngine

    class FakeLocalDatabaseProvider(private val db: CostCenterDatabase) : LocalDatabaseProvider {
        override fun current(): CostCenterDatabase = db
    }

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
        val provider = FakeLocalDatabaseProvider(database)
        syncEngine = SyncEngine(provider, registry)
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

    @Test
    fun syncWorkerComFalhasDePushRetornaRetryNa1aTentativaESuccessNa3a() = runTest {
        // 7.2 (j) SyncWorker com falhas de push retorna retry na 1ª tentativa e success na 3ª
        val sessionWithUser = FakeUserSession(userId = "user-authenticated")

        // Insere item pendente no outbox
        database.outboxDao().insert(
            OutboxEntity(
                id = "item-push-error",
                tableName = "fixed_costs",
                rowId = "r1",
                operation = "INSERT",
                payloadJson = "{}",
                attempts = 0,
                createdAt = 1000L
            )
        )

        // Handler que sempre falha no push
        registry.register(object : TableSyncHandler {
            override val table: String = "fixed_costs"
            override suspend fun push(item: OutboxItem): PushResult {
                return PushResult.Failure(RuntimeException("Falha de rede"))
            }
            override suspend fun pull(since: Instant?): PullResult = PullResult.Success(null, 0)
        })

        // 1ª tentativa (runAttemptCount = 0 < 3): deve retornar retry
        val workerAttempt1 = TestListenableWorkerBuilder<SyncWorker>(context)
            .setRunAttemptCount(0)
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

        val resultAttempt1 = workerAttempt1.doWork()
        assertEquals(ListenableWorker.Result.retry(), resultAttempt1)

        // 3ª tentativa (runAttemptCount = 3 >= 3): deve retornar success para não travar o WorkManager
        val workerAttempt3 = TestListenableWorkerBuilder<SyncWorker>(context)
            .setRunAttemptCount(3)
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

        val resultAttempt3 = workerAttempt3.doWork()
        assertEquals(ListenableWorker.Result.success(), resultAttempt3)
    }
}
