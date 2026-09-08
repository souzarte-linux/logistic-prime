package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.DailyTotal
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.DailyTotalApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.util.AppDataSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DailyTotalRepository(
    private val dailyTotalApi: DailyTotalApi = RetrofitClient.dailyTotalApi
) {
    suspend fun createDailyTotal(dailyTotal: DailyTotal): DailyTotal = withContext(Dispatchers.IO) {
        val dto = dailyTotal.toDto()
        val createdList = dailyTotalApi.createDailyTotal(dto)
        val result = createdList.firstOrNull()?.toDomain() ?: dailyTotal
        AppDataSync.notifyDataChanged()
        result
    }

    suspend fun updateDailyTotal(dailyTotal: DailyTotal): DailyTotal = withContext(Dispatchers.IO) {
        val dto = dailyTotal.toDto()
        val updatedList = dailyTotalApi.updateDailyTotal("eq.${dailyTotal.id}", dto)
        val result = updatedList.firstOrNull()?.toDomain() ?: dailyTotal
        AppDataSync.notifyDataChanged()
        result
    }

    suspend fun deleteDailyTotal(dailyTotalId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            dailyTotalApi.deleteDailyTotal("eq.$dailyTotalId")
            AppDataSync.notifyDataChanged()
            true
        } catch (e: Exception) {
            Log.e("DailyTotalRepo", "Erro ao excluir daily_total $dailyTotalId: ${e.message}", e)
            false
        }
    }

    suspend fun getDailyTotalById(id: String): DailyTotal? = withContext(Dispatchers.IO) {
        try {
            val list = dailyTotalApi.getDailyTotalById("eq.$id")
            list.firstOrNull()?.toDomain()
        } catch (e: Exception) {
            Log.e("DailyTotalRepo", "Erro ao buscar daily_total $id: ${e.message}", e)
            null
        }
    }

    suspend fun getDailyTotals(userId: String): List<DailyTotal> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            dailyTotalApi.getDailyTotals(userFilter, "occurred_at.desc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("DailyTotalRepo", "Erro ao buscar daily_totals: ${e.message}", e)
            emptyList()
        }
    }
}
