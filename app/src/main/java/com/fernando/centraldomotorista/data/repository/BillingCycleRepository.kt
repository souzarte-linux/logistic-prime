package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.BillingCycle
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.BillingCycleApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BillingCycleRepository(
    private val api: BillingCycleApi = RetrofitClient.billingCycleApi
) {
    suspend fun getBillingCycles(userId: String, status: String? = null): List<BillingCycle> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            val statusFilter = if (status != null) "eq.$status" else null
            api.getBillingCycles(userFilter, statusFilter).map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("BillingCycleRepository", "Erro ao buscar ciclos de faturamento: ${e.message}", e)
            emptyList()
        }
    }
}
