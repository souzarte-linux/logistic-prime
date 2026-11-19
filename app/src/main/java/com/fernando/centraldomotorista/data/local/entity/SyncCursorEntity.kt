package com.fernando.centraldomotorista.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_cursors")
data class SyncCursorEntity(
    @PrimaryKey
    @ColumnInfo(name = "table_name")
    val tableName: String,
    @ColumnInfo(name = "last_pulled_at")
    val lastPulledAt: String? = null
)
