package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.OilChange
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.OilChangeApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OilChangeRepository(
    private val api: OilChangeApi = RetrofitClient.oilChangeApi
) {
    suspend fun getOilChanges(userId: String): List<OilChange> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            api.getOilChanges(userFilter).map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("OilChangeRepository", "Erro ao buscar trocas de óleo: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun createOilChange(oilChange: OilChange): OilChange = withContext(Dispatchers.IO) {
        val dto = oilChange.toDto()
        val created = api.createOilChange(dto)
        val result = created.firstOrNull()?.toDomain() ?: oilChange
        com.fernando.centraldomotorista.util.AppDataSync.notifyDataChanged()
        result
    }

    suspend fun updateOilChange(oilChange: OilChange): OilChange = withContext(Dispatchers.IO) {
        val dto = oilChange.toDto()
        val updated = api.updateOilChange("eq.${oilChange.id}", dto)
        val result = updated.firstOrNull()?.toDomain() ?: oilChange
        com.fernando.centraldomotorista.util.AppDataSync.notifyDataChanged()
        result
    }

    suspend fun deleteOilChange(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            api.deleteOilChange("eq.$id")
            com.fernando.centraldomotorista.util.AppDataSync.notifyDataChanged()
            true
        } catch (e: Exception) {
            Log.e("OilChangeRepository", "Erro ao excluir troca de óleo $id: ${e.message}", e)
            false
        }
    }
}
