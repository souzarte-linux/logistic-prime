package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.CardBrand
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.CardBrandApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CardBrandRepository(
    private val cardBrandApi: CardBrandApi = RetrofitClient.cardBrandApi
) {
    suspend fun getCardBrands(userId: String): List<CardBrand> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            cardBrandApi.getCardBrands(userFilter, "name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("CardBrandRepo", "Erro ao buscar bandeiras: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun saveCardBrand(brand: CardBrand): CardBrand = withContext(Dispatchers.IO) {
        val dto = brand.toDto()
        if (brand.id.isNotBlank()) {
            val updated = cardBrandApi.updateCardBrand("eq.${brand.id}", dto)
            updated.firstOrNull()?.toDomain() ?: brand
        } else {
            val created = cardBrandApi.createCardBrand(dto)
            created.firstOrNull()?.toDomain() ?: brand
        }
    }

    suspend fun deleteCardBrand(brandId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            cardBrandApi.deleteCardBrand("eq.$brandId")
            true
        } catch (e: Exception) {
            Log.e("CardBrandRepo", "Erro ao excluir bandeira $brandId: ${e.message}", e)
            false
        }
    }
}
