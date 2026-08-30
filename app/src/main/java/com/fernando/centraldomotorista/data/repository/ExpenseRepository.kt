package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.Expense
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.ExpenseApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExpenseRepository(
    private val expenseApi: ExpenseApi = RetrofitClient.expenseApi
) {
    suspend fun createExpense(expense: Expense): Expense = withContext(Dispatchers.IO) {
        val dto = expense.toDto()
        val createdList = expenseApi.createExpense(dto)
        createdList.firstOrNull()?.toDomain() ?: expense
    }

    suspend fun updateExpense(expense: Expense): Expense = withContext(Dispatchers.IO) {
        val dto = expense.toDto()
        val updatedList = expenseApi.updateExpense("eq.${expense.id}", dto)
        updatedList.firstOrNull()?.toDomain() ?: expense
    }

    suspend fun deleteExpense(expenseId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            expenseApi.deleteExpense("eq.$expenseId")
            true
        } catch (e: Exception) {
            Log.e("ExpenseRepo", "Erro ao excluir despesa $expenseId: ${e.message}", e)
            false
        }
    }

    suspend fun getExpenses(userId: String): List<Expense> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            expenseApi.getExpenses(userFilter, "occurred_at.desc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("ExpenseRepo", "Erro ao buscar despesas: ${e.message}", e)
            emptyList()
        }
    }
}
