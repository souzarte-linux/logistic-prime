package com.fernando.centraldomotorista.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fernando.centraldomotorista.data.local.dao.OutboxDao
import com.fernando.centraldomotorista.data.local.dao.SyncCursorDao
import com.fernando.centraldomotorista.data.local.entity.OutboxEntity
import com.fernando.centraldomotorista.data.local.entity.SyncCursorEntity

@Database(
    entities = [
        OutboxEntity::class,
        SyncCursorEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class CostCenterDatabase : RoomDatabase() {
    abstract fun outboxDao(): OutboxDao
    abstract fun syncCursorDao(): SyncCursorDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE outbox ADD COLUMN last_attempt_at INTEGER")
            }
        }
    }
}
