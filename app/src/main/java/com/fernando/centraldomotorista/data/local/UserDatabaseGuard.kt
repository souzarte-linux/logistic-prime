package com.fernando.centraldomotorista.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
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
        if (userId.isNullOrBlank()) {
            throw IllegalStateException("Sem sessão ativa")
        }
        val safeId = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "cost_center_${safeId}.db"
    }

    fun getOrCreateDatabase(forcedUserId: String? = null): CostCenterDatabase {
        synchronized(lock) {
            val targetUserId = forcedUserId ?: userSession.currentUserId()
            if (targetUserId.isNullOrBlank()) {
                throw IllegalStateException("Sem sessão ativa")
            }
            val currentDb = activeDatabase
            if (currentDb != null && activeUserId == targetUserId && currentDb.isOpen) {
                return currentDb
            }

            // Se mudou de usuário e havia um aberto anterior, apenas fecha o anterior (NÃO apaga o arquivo ali)
            if (activeDatabase != null && !activeUserId.isNullOrBlank() && activeUserId != targetUserId) {
                try {
                    activeDatabase?.close()
                } catch (_: Exception) {
                }
                activeDatabase = null
                activeUserId = null
            }

            val dbName = getDatabaseName(targetUserId)
            val db = Room.databaseBuilder(
                context,
                CostCenterDatabase::class.java,
                dbName
            )
                .addMigrations(
                    CostCenterDatabase.MIGRATION_1_2,
                    CostCenterDatabase.MIGRATION_2_3
                )
                .build()

            activeDatabase = db
            activeUserId = targetUserId
            return db
        }
    }

    fun onUserChanged(oldUserId: String?, newUserId: String?) {
        synchronized(lock) {
            if (!oldUserId.isNullOrBlank() && oldUserId != newUserId) {
                if (activeUserId == oldUserId) {
                    try {
                        activeDatabase?.close()
                    } catch (_: Exception) {
                    }
                    activeDatabase = null
                    activeUserId = null
                }
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

    fun sweepOtherUsers(currentUserId: String) {
        val currentDbName = getDatabaseName(currentUserId)
        val dbRegex = Regex("""^cost_center_.+\.db$""")
        val databases = context.databaseList() ?: return

        for (name in databases) {
            if (!dbRegex.matches(name) || name == currentDbName) {
                continue
            }
            val dbFile = context.getDatabasePath(name)
            if (!dbFile.exists()) continue

            var sqliteDb: SQLiteDatabase? = null
            try {
                sqliteDb = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                val pendingCount = sqliteDb.rawQuery("SELECT COUNT(*) FROM outbox", null).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else 0
                }
                if (pendingCount == 0) {
                    try {
                        sqliteDb.close()
                    } catch (_: Exception) {
                    }
                    sqliteDb = null
                    context.deleteDatabase(name)
                } else {
                    Log.w("UserDatabaseGuard", "Banco $name preservado com $pendingCount registros pendentes no outbox")
                }
            } catch (e: Exception) {
                Log.w("UserDatabaseGuard", "Falha ao verificar banco $name, não será apagado: ${e.message}")
            } finally {
                try {
                    sqliteDb?.close()
                } catch (_: Exception) {
                }
            }
        }
    }
}
