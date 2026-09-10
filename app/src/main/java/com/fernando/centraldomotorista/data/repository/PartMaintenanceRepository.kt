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
            // Verifica se já existe registro com mesmo user_id e part_name para atualizar ao invés de duplicar (evita HTTP 409)
            val existing = try {
                val userFilter = "eq.${part.userId}"
                partMaintenanceApi.getPartMaintenances(userFilter, "created_at.desc")
                    .firstOrNull { it.partName.trim().equals(part.partName.trim(), ignoreCase = true) }
            } catch (e: Exception) {
                null
            }

            if (existing != null && !existing.id.isNullOrBlank()) {
                val updated = partMaintenanceApi.updatePartMaintenance("eq.${existing.id}", dto.copy(id = existing.id))
                updated.firstOrNull()?.toDomain() ?: part.copy(id = existing.id)
            } else {
                val created = partMaintenanceApi.createPartMaintenance(dto)
                created.firstOrNull()?.toDomain() ?: part
            }
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
