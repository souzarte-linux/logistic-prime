package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.PartMaintenanceApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PartMaintenanceRepository(
    private val partMaintenanceApi: PartMaintenanceApi = RetrofitClient.partMaintenanceApi
) {
    suspend fun getPartMaintenances(userId: String): List<PartMaintenance> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            partMaintenanceApi.getPartMaintenances(userFilter, "created_at.desc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("PartMaintRepository", "Erro ao buscar manutenções: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun savePartMaintenance(part: PartMaintenance): PartMaintenance = withContext(Dispatchers.IO) {
        val dto = part.toDto()
        if (part.id.isNotBlank()) {
            val updated = partMaintenanceApi.updatePartMaintenance("eq.${part.id}", dto)
            updated.firstOrNull()?.toDomain() ?: part
        } else {
            val created = partMaintenanceApi.createPartMaintenance(dto)
            created.firstOrNull()?.toDomain() ?: part
        }
    }

    suspend fun deletePartMaintenance(partId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            partMaintenanceApi.deletePartMaintenance("eq.$partId")
            true
        } catch (e: Exception) {
            Log.e("PartMaintRepository", "Erro ao excluir manutenção $partId: ${e.message}", e)
            false
        }
    }
}
