package com.fernando.centraldomotorista.costcenter

import android.content.Context
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.CostCenterDatabase
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeUserSession(initialUserId: String? = "usr-test-1") : UserSession {
    private val state = MutableStateFlow(initialUserId)
    override val userIdFlow: Flow<String?> = state
    override fun currentUserId(): String? = state.value
    fun setUserId(userId: String?) {
        state.value = userId
    }
}

internal class FakeLocalDatabaseProvider(
    private val db: CostCenterDatabase
) : LocalDatabaseProvider {
    override fun current(): CostCenterDatabase = db
}

internal fun createInMemoryCostCenterDb(context: Context): CostCenterDatabase =
    Room.inMemoryDatabaseBuilder(context, CostCenterDatabase::class.java)
        .allowMainThreadQueries()
        .build()

internal fun createTestSyncScheduler(context: Context): SyncScheduler {
    val config = Configuration.Builder().build()
    WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    return SyncScheduler(context, WorkManager.getInstance(context))
}
