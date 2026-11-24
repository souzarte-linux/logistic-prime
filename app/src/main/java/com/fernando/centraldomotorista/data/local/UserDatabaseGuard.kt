package com.fernando.centraldomotorista.data.local

import android.content.Context
import androidx.room.Room
import com.fernando.centraldomotorista.auth.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDatabaseGuard @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSession: UserSession
) {
    private var activeDatabase: CostCenterDatabase? = null
    private var activeUserId: String? = null
    private val lock = Any()

    fun getDatabaseName(userId: String?): String {
        val safeId = if (userId.isNullOrBlank()) "anonymous" else userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "cost_center_${safeId}.db"
    }

    fun getOrCreateDatabase(forcedUserId: String? = null): CostCenterDatabase {
        synchronized(lock) {
            val targetUserId = forcedUserId ?: userSession.currentUserId() ?: "anonymous"
            val currentDb = activeDatabase
            if (currentDb != null && activeUserId == targetUserId && currentDb.isOpen) {
                return currentDb
            }

            // Se mudou de usuário e havia um aberto anterior, fecha e apaga o anterior
            if (activeDatabase != null && !activeUserId.isNullOrBlank() && activeUserId != targetUserId) {
                closeAndDeleteDatabase(activeUserId!!)
            }

            val dbName = getDatabaseName(targetUserId)
            val db = Room.databaseBuilder(
                context,
                CostCenterDatabase::class.java,
                dbName
            ).build()

            activeDatabase = db
            activeUserId = targetUserId
            return db
        }
    }

    fun onUserChanged(oldUserId: String?, newUserId: String?) {
        synchronized(lock) {
            if (!oldUserId.isNullOrBlank() && oldUserId != newUserId) {
                closeAndDeleteDatabase(oldUserId)
            }
        }
    }

    fun closeAndDeleteDatabase(userId: String): Boolean {
        synchronized(lock) {
            if (activeUserId == userId) {
                try {
                    activeDatabase?.close()
                } catch (_: Exception) {
                }
                activeDatabase = null
                activeUserId = null
            }
            val dbName = getDatabaseName(userId)
            return context.deleteDatabase(dbName)
        }
    }

    fun wipeUserDatabase(userId: String): Boolean = closeAndDeleteDatabase(userId)

    fun getActiveUserId(): String? = synchronized(lock) { activeUserId }

    fun getActiveDatabase(): CostCenterDatabase? = synchronized(lock) { activeDatabase }
}
