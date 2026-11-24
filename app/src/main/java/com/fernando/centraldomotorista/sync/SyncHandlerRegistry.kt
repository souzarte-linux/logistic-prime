package com.fernando.centraldomotorista.sync

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncHandlerRegistry @Inject constructor() {

    private val handlers = ConcurrentHashMap<String, TableSyncHandler>()

    fun register(handler: TableSyncHandler) {
        handlers[handler.table] = handler
    }

    fun unregister(table: String) {
        handlers.remove(table)
    }

    fun getHandler(table: String): TableSyncHandler? = handlers[table]

    fun getAllHandlers(): List<TableSyncHandler> = handlers.values.toList()

    fun clear() {
        handlers.clear()
    }
}
