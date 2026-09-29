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
    }

    /**
     * Flow com o número de dias de retenção das fotos de etiquetas antes da exclusão.
     * Retorna [DEFAULT_PHOTO_RETENTION_DAYS] (3 dias) caso ainda não esteja configurado.
     */
    val photoRetentionDaysFlow: Flow<Int> = context.routeDataStore.data.map { preferences ->
        preferences[PHOTO_RETENTION_DAYS_KEY] ?: DEFAULT_PHOTO_RETENTION_DAYS
    }

    /**
     * Salva o número de dias de retenção de fotos configurado pelo motorista.
     */
    suspend fun setPhotoRetentionDays(days: Int) {
        context.routeDataStore.edit { preferences ->
            preferences[PHOTO_RETENTION_DAYS_KEY] = days
        }
    }
}
