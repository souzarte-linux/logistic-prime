package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.PartType
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.PartTypeApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PartTypeRepository(
    private val partTypeApi: PartTypeApi = RetrofitClient.partTypeApi
) {
    suspend fun getPartTypes(userId: String): List<PartType> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            partTypeApi.getPartTypes(userFilter, "name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("PartTypeRepository", "Erro ao buscar tipos de peças: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun savePartType(partType: PartType): PartType = withContext(Dispatchers.IO) {
        val dto = partType.toDto()
        if (partType.id.isNotBlank()) {
            val updated = partTypeApi.updatePartType("eq.${partType.id}", dto)
            updated.firstOrNull()?.toDomain() ?: partType
        } else {
            val created = partTypeApi.createPartType(dto)
            created.firstOrNull()?.toDomain() ?: partType
        }
    }

    suspend fun deletePartType(partTypeId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            partTypeApi.deletePartType("eq.$partTypeId")
            true
        } catch (e: Exception) {
            Log.e("PartTypeRepository", "Erro ao excluir tipo de peça $partTypeId: ${e.message}", e)
            false
        }
    }
}
