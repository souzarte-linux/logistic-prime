package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.FinancialAdjustment
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.FinancialAdjustmentApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FinancialAdjustmentRepository(
    private val api: FinancialAdjustmentApi = RetrofitClient.financialAdjustmentApi
) {
    suspend fun getFinancialAdjustments(userId: String): List<FinancialAdjustment> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            api.getFinancialAdjustments(userFilter).map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("AdjustmentRepository", "Erro ao buscar ajustes financeiros: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun createFinancialAdjustment(adjustment: FinancialAdjustment): FinancialAdjustment = withContext(Dispatchers.IO) {
        val dto = adjustment.toDto()
        val created = api.createFinancialAdjustment(dto)
        val result = created.firstOrNull()?.toDomain() ?: adjustment
        com.fernando.centraldomotorista.util.AppDataSync.notifyDataChanged()
        result
    }

    suspend fun updateFinancialAdjustment(adjustment: FinancialAdjustment): FinancialAdjustment = withContext(Dispatchers.IO) {
        val dto = adjustment.toDto()
        val updated = api.updateFinancialAdjustment("eq.${adjustment.id}", dto)
        val result = updated.firstOrNull()?.toDomain() ?: adjustment
        com.fernando.centraldomotorista.util.AppDataSync.notifyDataChanged()
        result
    }

    suspend fun deleteFinancialAdjustment(adjustmentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            api.deleteFinancialAdjustment("eq.$adjustmentId")
            com.fernando.centraldomotorista.util.AppDataSync.notifyDataChanged()
            true
        } catch (e: Exception) {
            Log.e("AdjustmentRepository", "Erro ao excluir ajuste $adjustmentId: ${e.message}", e)
            false
        }
    }
}
