package com.fernando.centraldomotorista.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "table_name")
    val tableName: String,
    @ColumnInfo(name = "row_id")
    val rowId: String,
    @ColumnInfo(name = "operation")
    val operation: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "attempts")
    val attempts: Int = 0,
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
