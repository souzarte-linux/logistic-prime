package com.fernando.centraldomotorista.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fernando.centraldomotorista.data.local.entity.SyncCursorEntity

@Dao
interface SyncCursorDao {

    @Query("SELECT * FROM sync_cursors WHERE table_name = :tableName LIMIT 1")
    suspend fun getCursor(tableName: String): SyncCursorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCursor(cursor: SyncCursorEntity)

    @Query("DELETE FROM sync_cursors WHERE table_name = :tableName")
    suspend fun deleteCursor(tableName: String)

    @Query("DELETE FROM sync_cursors")
    suspend fun clearAll()
}
