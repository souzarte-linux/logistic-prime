package com.fernando.centraldomotorista.sync

class SyncHandlerRegistry {

    private val lock = Any()
    private val handlers = LinkedHashMap<String, TableSyncHandler>()

    fun register(handler: TableSyncHandler) {
        synchronized(lock) {
            handlers[handler.table] = handler
        }
    }

    fun unregister(table: String) {
        synchronized(lock) {
            handlers.remove(table)
        }
    }

    fun getHandler(table: String): TableSyncHandler? = synchronized(lock) {
        handlers[table]
    }

    fun getAllHandlers(): List<TableSyncHandler> = synchronized(lock) {
        handlers.values.toList()
    }

    fun clear() {
        synchronized(lock) {
            handlers.clear()
        }
    }
}
