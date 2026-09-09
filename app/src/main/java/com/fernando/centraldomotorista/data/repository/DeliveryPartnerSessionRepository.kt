package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.Expense
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.DeliveryPartnerSessionApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.util.AppDataSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeliveryPartnerSessionRepository(
    private val sessionApi: DeliveryPartnerSessionApi = RetrofitClient.deliveryPartnerSessionApi,
    private val expenseRepository: ExpenseRepository = ExpenseRepository()
) {
    suspend fun getSessions(userId: String): List<DeliveryPartnerSession> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            sessionApi.getSessions(userFilter).map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("PartnerSessionRepo", "Erro ao buscar sessões: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getSessionsForPartner(userId: String, partnerId: String): List<DeliveryPartnerSession> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            val partnerFilter = "eq.$partnerId"
            sessionApi.getSessionsForPartner(userFilter, partnerFilter).map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("PartnerSessionRepo", "Erro ao buscar sessões do parceiro $partnerId: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getSessionById(sessionId: String): DeliveryPartnerSession? = withContext(Dispatchers.IO) {
        try {
            val list = sessionApi.getSessionById("eq.$sessionId")
            list.firstOrNull()?.toDomain()
        } catch (e: Exception) {
            Log.e("PartnerSessionRepo", "Erro ao buscar sessão $sessionId: ${e.message}", e)
            null
        }
    }

    suspend fun saveSession(session: DeliveryPartnerSession): DeliveryPartnerSession = withContext(Dispatchers.IO) {
        val dto = session.toDto()
        val result = if (session.id.isNotBlank()) {
            val updated = sessionApi.updateSession("eq.${session.id}", dto)
            updated.firstOrNull()?.toDomain() ?: session
        } else {
            val created = sessionApi.createSession(dto)
            created.firstOrNull()?.toDomain() ?: session
        }
        AppDataSync.notifyDataChanged()
        result
    }

    suspend fun finalizeSession(session: DeliveryPartnerSession, expense: Expense): DeliveryPartnerSession = withContext(Dispatchers.IO) {
        // 1. Criar a despesa na tabela expenses
        val createdExpense = expenseRepository.createExpense(expense)
        Log.d("PartnerSessionRepo", "Despesa de equipe criada com id: ${createdExpense.id}, valor: ${createdExpense.amount}")

        // 2. Atualizar a sessão com o expenseId gerado
        val updatedSession = session.copy(expenseId = createdExpense.id)
        val dto = updatedSession.toDto()
        val resultList = sessionApi.updateSession("eq.${session.id}", dto)
        val result = resultList.firstOrNull()?.toDomain() ?: updatedSession

        // 3. Notificar sincronização para atualizar Início / Lucro Líquido
        AppDataSync.notifyDataChanged()
        result
    }

    suspend fun deleteSession(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            sessionApi.deleteSession("eq.$sessionId")
            AppDataSync.notifyDataChanged()
            true
        } catch (e: Exception) {
            Log.e("PartnerSessionRepo", "Erro ao excluir sessão $sessionId: ${e.message}", e)
            false
        }
    }
}
