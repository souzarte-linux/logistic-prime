package com.fernando.centraldomotorista.sync

import com.fernando.centraldomotorista.data.local.entity.OutboxEntity

data class OutboxItem(
    val id: String,
    val tableName: String,
    val rowId: String,
    val operation: String,
    val payloadJson: String,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null
) {
    fun toEntity(): OutboxEntity = OutboxEntity(
        id = id,
        tableName = tableName,
        rowId = rowId,
        operation = operation,
        payloadJson = payloadJson,
        attempts = attempts,
        lastError = lastError,
        createdAt = createdAt,
        lastAttemptAt = lastAttemptAt
    )

    companion object {
        fun fromEntity(entity: OutboxEntity): OutboxItem = OutboxItem(
            id = entity.id,
            tableName = entity.tableName,
            rowId = entity.rowId,
            operation = entity.operation,
            payloadJson = entity.payloadJson,
            attempts = entity.attempts,
            lastError = entity.lastError,
            createdAt = entity.createdAt,
            lastAttemptAt = entity.lastAttemptAt
        )
    }
}
