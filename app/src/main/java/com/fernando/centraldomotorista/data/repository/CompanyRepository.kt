package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.Company
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.CompanyApi
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CompanyRepository(
    private val companyApi: CompanyApi = RetrofitClient.companyApi
) {
    suspend fun getCompanies(userId: String): List<Company> = withContext(Dispatchers.IO) {
        try {
            val userFilter = "eq.$userId"
            companyApi.getCompanies(userFilter, "name.asc").map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("CompanyRepository", "Erro ao buscar empresas: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun saveCompany(company: Company): Company = withContext(Dispatchers.IO) {
        val dto = company.toDto()
        if (company.id.isNotBlank()) {
            val updated = companyApi.updateCompany("eq.${company.id}", dto)
            updated.firstOrNull()?.toDomain() ?: company
        } else {
            val created = companyApi.createCompany(dto)
            created.firstOrNull()?.toDomain() ?: company
        }
    }

    suspend fun deleteCompany(companyId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            companyApi.deleteCompany("eq.$companyId")
            true
        } catch (e: Exception) {
            Log.e("CompanyRepository", "Erro ao excluir empresa $companyId: ${e.message}", e)
            false
        }
    }
}
