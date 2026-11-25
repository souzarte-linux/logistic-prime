package com.fernando.centraldomotorista.data.repository.costcenter

import com.fernando.centraldomotorista.data.local.entity.OutboxEntity
import com.fernando.centraldomotorista.data.remote.dto.toOutboxJsonObject
import com.fernando.centraldomotorista.sync.OutboxPayloadSerializer
import com.fernando.centraldomotorista.sync.SyncScheduler
import com.fernando.centraldomotorista.util.AppDataSync
import java.util.UUID

internal fun buildOutboxEntity(
    tableName: String,
    rowId: String,
    operation: String,
    dto: Any
): OutboxEntity = OutboxEntity(
    id = UUID.randomUUID().toString(),
    tableName = tableName,
    rowId = rowId,
    operation = operation,
    payloadJson = OutboxPayloadSerializer.serialize(dto.toOutboxJsonObject())
)

internal fun notifyAfterMutation(syncScheduler: SyncScheduler) {
    syncScheduler.scheduleOneTimeSync()
    AppDataSync.notifyDataChanged()
}
