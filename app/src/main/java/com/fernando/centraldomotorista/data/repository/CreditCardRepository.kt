package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.CardBrand
import com.fernando.centraldomotorista.data.model.CardOperator
import com.fernando.centraldomotorista.data.model.CreditCard
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.CardBrandApi
import com.fernando.centraldomotorista.data.remote.api.CardOperatorApi
import com.fernando.centraldomotorista.data.remote.api.CreditCardApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreditCardRepository(
    private val creditCardApi: CreditCardApi = RetrofitClient.creditCardApi,
    private val cardBrandApi: CardBrandApi = RetrofitClient.cardBrandApi,
    private val cardOperatorApi: CardOperatorApi = RetrofitClient.cardOperatorApi
) {
    suspend fun getCreditCards(userId: String): List<CreditCard> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            creditCardApi.getCreditCards(userFilter, "nickname.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("CreditCardRepo", "Erro ao buscar cartoes: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun saveCreditCard(card: CreditCard): CreditCard = withContext(Dispatchers.IO) {
        val dto = card.toDto()
        if (card.id.isNotBlank()) {
            val updated = creditCardApi.updateCreditCard("eq.${card.id}", dto)
            updated.firstOrNull()?.toDomain() ?: card
        } else {
            val created = creditCardApi.createCreditCard(dto)
            created.firstOrNull()?.toDomain() ?: card
        }
    }

    suspend fun toggleCardActive(cardId: String, active: Boolean, currentCard: CreditCard): Boolean = withContext(Dispatchers.IO) {
        try {
            val updatedDto = currentCard.copy(active = active).toDto()
            creditCardApi.updateCreditCard("eq.$cardId", updatedDto)
            true
        } catch (e: Exception) {
            Log.e("CreditCardRepo", "Erro ao alternar status do cartao: ${e.message}", e)
            false
        }
    }

    suspend fun deleteCreditCard(cardId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            creditCardApi.deleteCreditCard("eq.$cardId")
            true
        } catch (e: Exception) {
            Log.e("CreditCardRepo", "Erro ao excluir cartao: ${e.message}", e)
            false
        }
    }

    suspend fun getCardBrands(userId: String): List<CardBrand> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            cardBrandApi.getCardBrands(userFilter, "name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("CreditCardRepo", "Erro ao buscar bandeiras: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun createCardBrand(userId: String, name: String): CardBrand = withContext(Dispatchers.IO) {
        val brand = CardBrand(id = "", userId = userId, name = name)
        val created = cardBrandApi.createCardBrand(brand.toDto())
        created.firstOrNull()?.toDomain() ?: brand
    }

    suspend fun getCardOperators(userId: String): List<CardOperator> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            cardOperatorApi.getCardOperators(userFilter, "name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("CreditCardRepo", "Erro ao buscar emissores: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun createCardOperator(userId: String, name: String): CardOperator = withContext(Dispatchers.IO) {
        val operator = CardOperator(id = "", userId = userId, name = name)
        val created = cardOperatorApi.createCardOperator(operator.toDto())
        created.firstOrNull()?.toDomain() ?: operator
    }
}
