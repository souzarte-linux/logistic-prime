package com.fernando.centraldomotorista.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fernando.centraldomotorista.data.local.dao.OutboxDao
import com.fernando.centraldomotorista.data.local.dao.SyncCursorDao
import com.fernando.centraldomotorista.data.local.entity.OutboxEntity
import com.fernando.centraldomotorista.data.local.entity.SyncCursorEntity

@Database(
    entities = [
        OutboxEntity::class,
        SyncCursorEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class CostCenterDatabase : RoomDatabase() {
    abstract fun outboxDao(): OutboxDao
    abstract fun syncCursorDao(): SyncCursorDao
}
