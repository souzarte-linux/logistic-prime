package com.fernando.centraldomotorista.sync

import java.time.Instant

interface TableSyncHandler {
    val table: String
    suspend fun push(item: OutboxItem): PushResult
    suspend fun pull(since: Instant?): PullResult
}
