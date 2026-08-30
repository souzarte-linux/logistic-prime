package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.CardOperator
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.CardOperatorApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CardOperatorRepository(
    private val cardOperatorApi: CardOperatorApi = RetrofitClient.cardOperatorApi
) {
    suspend fun getCardOperators(userId: String): List<CardOperator> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            cardOperatorApi.getCardOperators(userFilter, "name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("CardOperatorRepo", "Erro ao buscar emissores: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun saveCardOperator(operator: CardOperator): CardOperator = withContext(Dispatchers.IO) {
        val dto = operator.toDto()
        if (operator.id.isNotBlank()) {
            val updated = cardOperatorApi.updateCardOperator("eq.${operator.id}", dto)
            updated.firstOrNull()?.toDomain() ?: operator
        } else {
            val created = cardOperatorApi.createCardOperator(dto)
            created.firstOrNull()?.toDomain() ?: operator
        }
    }

    suspend fun deleteCardOperator(operatorId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            cardOperatorApi.deleteCardOperator("eq.$operatorId")
            true
        } catch (e: Exception) {
            Log.e("CardOperatorRepo", "Erro ao excluir emissor $operatorId: ${e.message}", e)
            false
        }
    }
}
