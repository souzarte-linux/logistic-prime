package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.Profile
import com.fernando.centraldomotorista.data.remote.RetrofitClient
import com.fernando.centraldomotorista.data.remote.api.ProfileApi
import com.fernando.centraldomotorista.data.remote.dto.ProfileDto
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ProfileRepository(
    private val profileApi: ProfileApi = RetrofitClient.profileApi
) {
    /**
     * Busca o perfil no Neon Data API. Se ainda não existir, cria o registro inicial.
     */
    suspend fun createOrFetchProfile(
        userId: String,
        email: String?,
        fullName: String?,
        avatarUrl: String?
    ): Profile = withContext(Dispatchers.IO) {
        try {
            Log.d("ProfileRepository", "Consultando profile para UID: $userId no Neon Data API...")
            val existingProfiles = profileApi.getProfile("eq.$userId")
            
            if (existingProfiles.isNotEmpty()) {
                val profile = existingProfiles.first().toDomain()
                Log.d("ProfileRepository", "Profile existente encontrado no Neon: $profile")
                return@withContext profile
            }

            Log.d("ProfileRepository", "Profile não encontrado. Criando novo profile no Neon...")
            val newDto = ProfileDto(
                id = userId,
                email = email,
                fullName = fullName,
                avatarUrl = avatarUrl,
                dailyGoal = BigDecimal("200"),
                weeklyGoal = BigDecimal("1000"),
                monthlyGoal = BigDecimal("3450"),
                vehicle = "moto",
                hasBag = false
            )

            val createdList = profileApi.createProfile(newDto)
            val created = createdList.firstOrNull()?.toDomain() ?: newDto.toDomain()
            Log.d("ProfileRepository", "Novo profile criado com sucesso no Neon: $created")
            created
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Erro na chamada do Neon Data API: ${e.message}", e)
            // Fallback gracioso para garantir inicialização caso a rede oscile
            Profile(
                id = userId,
                fullName = fullName,
                email = email,
                phone = null,
                vehicle = "moto",
                plate = null,
                avatarUrl = avatarUrl,
                dailyGoal = BigDecimal("200"),
                weeklyGoal = BigDecimal("1000"),
                monthlyGoal = BigDecimal("3450"),
                vehicleBrand = null,
                vehicleModel = null,
                vehicleYear = null,
                tankSizeL = null,
                avgConsumptionKml = null,
                oilChangeKm = null,
                tireSizeFront = null,
                tireSizeRear = null,
                hasBag = false,
                lastOilChangeAt = null
            )
        }
    }
}
