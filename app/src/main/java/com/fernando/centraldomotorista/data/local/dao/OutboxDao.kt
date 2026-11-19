package com.fernando.centraldomotorista.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fernando.centraldomotorista.data.local.entity.OutboxEntity

@Dao
interface OutboxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: OutboxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OutboxEntity>)

    @Query("SELECT * FROM outbox WHERE attempts < 8 ORDER BY created_at ASC")
    suspend fun getPendingItems(): List<OutboxEntity>

    @Query("SELECT * FROM outbox WHERE attempts >= 8 ORDER BY created_at ASC")
    suspend fun getPermanentFailures(): List<OutboxEntity>

    @Query("SELECT * FROM outbox ORDER BY created_at ASC")
    suspend fun getAll(): List<OutboxEntity>

    @Query("SELECT * FROM outbox WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OutboxEntity?

    @Query("UPDATE outbox SET attempts = :attempts, last_error = :lastError WHERE id = :id")
    suspend fun updateAttempts(id: String, attempts: Int, lastError: String?)

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM outbox")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM outbox")
    suspend fun count(): Int
}
