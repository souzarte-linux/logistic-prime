package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.PartProduct
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.PartProductApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PartProductRepository(
    private val partProductApi: PartProductApi = RetrofitClient.partProductApi
) {
    suspend fun getPartProducts(userId: String): List<PartProduct> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            partProductApi.getPartProducts(userFilter, "brand.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("PartProductRepository", "Erro ao buscar produtos de peças: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun savePartProduct(product: PartProduct): PartProduct = withContext(Dispatchers.IO) {
        val dto = product.toDto()
        if (product.id.isNotBlank()) {
            val updated = partProductApi.updatePartProduct("eq.${product.id}", dto)
            updated.firstOrNull()?.toDomain() ?: product
        } else {
            val created = partProductApi.createPartProduct(dto)
            created.firstOrNull()?.toDomain() ?: product
        }
    }

    suspend fun deletePartProduct(productId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            partProductApi.deletePartProduct("eq.$productId")
            true
        } catch (e: Exception) {
            Log.e("PartProductRepository", "Erro ao excluir produto $productId: ${e.message}", e)
            false
        }
    }
}
