package com.fernando.centraldomotorista.data.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

// Cada classe abaixo espelha uma tabela do banco Neon (PostgreSQL).
// Os nomes dos campos foram mantidos iguais aos das colunas do banco
// para facilitar o mapeamento quando a API/backend responder em JSON.

data class Profile(
    val id: String,
    val fullName: String?,
    val email: String?,
    val phone: String?,
    val vehicle: String? = "moto",
    val plate: String?,
    val avatarUrl: String?,
    val dailyGoal: BigDecimal = BigDecimal("200"),
    val weeklyGoal: BigDecimal = BigDecimal("1000"),
    val monthlyGoal: BigDecimal = BigDecimal("3450"),
    val vehicleBrand: String?,
    val vehicleModel: String?,
    val vehicleYear: Int?,
    val tankSizeL: BigDecimal?,
    val avgConsumptionKml: BigDecimal?,
    val oilChangeKm: BigDecimal?,
    val tireSizeFront: String?,
    val tireSizeRear: String?,
    val hasBag: Boolean = false,
    val lastOilChangeAt: OffsetDateTime?,
)

data class Platform(
    val id: String,
    val userId: String,
    val name: String,
    val cycle: String,           // "semanal" | "quinzenal" | "misto"
    val paymentDay: String?,
    val active: Boolean = true,
    val segment: String = "logistica",   // "logistica" | "delivery"
    val paymentModel: String = "producao",
    val rules: Map<String, Any> = emptyMap(),
)

data class Route(
    val id: String,
    val userId: String,
    val platformId: String?,
    val origin: String?,
    val destination: String?,
    val distanceKm: BigDecimal = BigDecimal.ZERO,
    val amount: BigDecimal = BigDecimal.ZERO,
    val tip: BigDecimal = BigDecimal.ZERO,
    val productType: String = "alimento",
    val packageCount: Int = 1,
    val packageUnitPrice: BigDecimal = BigDecimal.ZERO,
    val smallPackagesCount: Int = 0,
    val largePackagesCount: Int = 0,
    val startedAt: OffsetDateTime?,
    val endedAt: OffsetDateTime?,
    val breakMinutes: Int = 0,
    val startKm: BigDecimal = BigDecimal.ZERO,
    val endKm: BigDecimal = BigDecimal.ZERO,
    val billingCycleId: String?,
    val occurredAt: OffsetDateTime,
)

data class DailyTotal(
    val id: String,
    val userId: String,
    val platformId: String?,
    val amount: BigDecimal,
    val distanceKm: BigDecimal = BigDecimal.ZERO,
    val productType: String = "alimento",
    val subtractRoutes: Boolean = false,
    val billingCycleId: String?,
    val occurredAt: OffsetDateTime,
)

data class Expense(
    val id: String,
    val userId: String,
    val category: String,   // "combustivel" | "manutencao" | "alimentacao" | ...
    val title: String,
    val vendor: String?,
    val amount: BigDecimal,
    val liters: BigDecimal?,
    val fuelType: String?,
    val pricePerLiter: BigDecimal?,
    val odometerKm: BigDecimal?,
    val paymentMethod: String = "pix",
    val isFullTank: Boolean = true,
    val occurredAt: OffsetDateTime,
    val partBrand: String?,
    val partModel: String?,
    val installmentGroupId: String?,
    val installmentNumber: Int?,
    val installmentTotal: Int?,
)

data class OilChange(
    val id: String,
    val userId: String,
    val changedAt: OffsetDateTime,
    val kmAtChange: BigDecimal,
    val notes: String?,
)

data class PartMaintenance(
    val id: String,
    val userId: String,
    val partName: String,
    val lifeKm: BigDecimal,
    val lastChangeKm: BigDecimal,
    val lastChangeAt: OffsetDateTime,
) {
    /** Quantos km restam até vencer (negativo = atrasado). Requer o odômetro atual do veículo. */
    fun kmRemaining(currentOdometerKm: BigDecimal): BigDecimal {
        val nextDueKm = lastChangeKm + lifeKm
        return nextDueKm - currentOdometerKm
    }
}

data class BillingCycle(
    val id: String,
    val userId: String,
    val platformId: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val expectedPaymentDate: LocalDate,
    val status: String = "pending",   // "pending" | "paid"
)

data class FinancialAdjustment(
    val id: String,
    val userId: String,
    val platformId: String,
    val billingCycleId: String?,
    val type: String,
    val amount: BigDecimal,
    val description: String?,
    val occurredAt: LocalDate,
)

data class AppNotification(
    val id: String,
    val userId: String,
    val type: String,
    val billingCycleId: String?,
    val read: Boolean = false,
)

data class GasStation(
    val id: String,
    val userId: String,
    val name: String,
    val address: String?,
    val brand: String,
    val fuelTypes: List<String> = emptyList(),
)

data class CardOperator(
    val id: String,
    val userId: String,
    val name: String,
    val cardDueDay: Int?,
)
