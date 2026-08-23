package com.fernando.centraldomotorista.data.repository

import com.fernando.centraldomotorista.data.model.Profile
import java.math.BigDecimal

class ProfileRepository {

    /**
     * Verifica se o profile do usuário existe ou cria um novo com os valores padrão.
     * TODO: Implementar a chamada real à API Retrofit integrada com a base Neon.tech.
     */
    suspend fun createOrFetchProfile(
        userId: String,
        email: String? = null,
        fullName: String? = null,
        avatarUrl: String? = null
    ): Profile {
        // Retorna Profile fake para teste completo do fluxo de login e navegação
        return Profile(
            id = userId,
            fullName = fullName ?: "Motorista",
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
