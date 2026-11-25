package com.fernando.centraldomotorista

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.UserDatabaseGuard
import com.fernando.centraldomotorista.data.repository.ExpenseDeletionHooks
import com.fernando.centraldomotorista.data.repository.costcenter.SettlementReverter
import com.fernando.centraldomotorista.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LogisticPrimeApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var userSession: UserSession

    @Inject
    lateinit var guard: UserDatabaseGuard

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var settlementReverter: SettlementReverter

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        try {
            if (::settlementReverter.isInitialized) {
                ExpenseDeletionHooks.register(settlementReverter)
            }
        } catch (e: Exception) {
            Log.w("LogisticPrimeApp", "Falha ao registrar SettlementReverter: ${e.message}")
        }

        try {
            if (::syncScheduler.isInitialized) {
                syncScheduler.schedulePeriodicSync()
            }
        } catch (e: Exception) {
            Log.w("LogisticPrimeApp", "Falha ao agendar periodic sync: ${e.message}")
        }

        applicationScope.launch {
            try {
                if (::userSession.isInitialized && ::guard.isInitialized) {
                    userSession.userIdFlow.collect { id ->
                        if (!id.isNullOrBlank()) {
                            try {
                                guard.sweepOtherUsers(id)
                            } catch (e: Exception) {
                                Log.w("LogisticPrimeApp", "Falha no sweepOtherUsers: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("LogisticPrimeApp", "Erro ao observar userIdFlow: ${e.message}")
            }
        }
    }
}
