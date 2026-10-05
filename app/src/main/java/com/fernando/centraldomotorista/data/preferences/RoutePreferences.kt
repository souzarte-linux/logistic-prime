package com.fernando.centraldomotorista.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.routeDataStore: DataStore<Preferences> by preferencesDataStore(name = "route_preferences")

/**
 * Gerenciador de preferências do usuário relacionadas às rotas e entrega.
 * Armazena localmente configurações como o prazo de retenção das fotos de backup de etiquetas.
 */
class RoutePreferences(private val context: Context) {

    companion object {
        const val DEFAULT_PHOTO_RETENTION_DAYS = 3
        val PHOTO_RETENTION_DAYS_KEY = intPreferencesKey("photo_retention_days")
        val LAST_SELECTED_PLATFORM_KEY = androidx.datastore.preferences.core.stringPreferencesKey("last_selected_platform_id")
        const val PREFS_SYNC_NAME = "route_preferences_sync"
        const val PREF_KEY_LAST_PLATFORM_ID = "last_selected_platform_id"
    }

    /**
     * Flow com o número de dias de retenção das fotos de etiquetas antes da exclusão.
     * Retorna [DEFAULT_PHOTO_RETENTION_DAYS] (3 dias) caso ainda não esteja configurado.
     */
    val photoRetentionDaysFlow: Flow<Int> = context.routeDataStore.data.map { preferences ->
        preferences[PHOTO_RETENTION_DAYS_KEY] ?: DEFAULT_PHOTO_RETENTION_DAYS
    }

    /**
     * Flow com o ID da última plataforma selecionada para início de rota.
     */
    val lastPlatformIdFlow: Flow<String?> = context.routeDataStore.data.map { preferences ->
        preferences[LAST_SELECTED_PLATFORM_KEY]
    }

    /**
     * Salva o número de dias de retenção de fotos configurado pelo motorista.
     */
    suspend fun setPhotoRetentionDays(days: Int) {
        context.routeDataStore.edit { preferences ->
            preferences[PHOTO_RETENTION_DAYS_KEY] = days
        }
    }

    /**
     * Salva a última plataforma de entrega utilizada para pré-selecionar nas próximas rotas.
     */
    suspend fun setLastPlatformId(platformId: String?) {
        setLastPlatformIdSync(platformId)
        context.routeDataStore.edit { preferences ->
            if (platformId != null) {
                preferences[LAST_SELECTED_PLATFORM_KEY] = platformId
            } else {
                preferences.remove(LAST_SELECTED_PLATFORM_KEY)
            }
        }
    }

    /**
     * Obtém sincronicamente a última plataforma selecionada (para inicialização rápida de diálogos de UI).
     */
    fun getLastPlatformIdSync(): String? {
        val sp = context.getSharedPreferences(PREFS_SYNC_NAME, Context.MODE_PRIVATE)
        return sp.getString(PREF_KEY_LAST_PLATFORM_ID, null)
    }

    /**
     * Grava sincronicamente a última plataforma selecionada no SharedPreferences.
     */
    fun setLastPlatformIdSync(platformId: String?) {
        val sp = context.getSharedPreferences(PREFS_SYNC_NAME, Context.MODE_PRIVATE)
        sp.edit().apply {
            if (platformId != null) {
                putString(PREF_KEY_LAST_PLATFORM_ID, platformId)
            } else {
                remove(PREF_KEY_LAST_PLATFORM_ID)
            }
        }.apply()
    }
}
