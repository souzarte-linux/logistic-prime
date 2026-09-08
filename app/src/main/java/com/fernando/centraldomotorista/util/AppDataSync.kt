package com.fernando.centraldomotorista.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Barramento central de eventos de sincronização.
 * Dispara notificações sempre que transações, rotas, despesas ou totais
 * diários forem criados, atualizados ou excluídos, garantindo que
 * HomeViewModel e HistoricoViewModel recarreguem seus dados imediatamente.
 */
object AppDataSync {
    private val _dataChangedEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val dataChangedEvents: SharedFlow<Unit> = _dataChangedEvents.asSharedFlow()

    fun notifyDataChanged() {
        _dataChangedEvents.tryEmit(Unit)
    }
}
