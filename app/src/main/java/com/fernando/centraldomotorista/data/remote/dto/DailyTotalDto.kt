package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.DailyTotal
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.OffsetDateTime

data class DailyTotalDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("platform_id")
    val platformId: String? = null,
    @SerializedName("amount")
    val amount: BigDecimal,
    @SerializedName("distance_km")
    val distanceKm: BigDecimal? = BigDecimal.ZERO,
    @SerializedName("product_type")
    val productType: String? = "alimento",
    @SerializedName("subtract_routes")
    val subtractRoutes: Boolean? = false,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("billing_cycle_id")
    val billingCycleId: String? = null,
    @SerializedName("occurred_at")
    val occurredAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun DailyTotalDto.toDomain(): DailyTotal {
    val parsedOccurredAt = occurredAt?.let {
        try { OffsetDateTime.parse(it) } catch (e: Exception) { OffsetDateTime.now() }
    } ?: OffsetDateTime.now()

    return DailyTotal(
        id = id ?: "",
        userId = userId,
        platformId = platformId,
        amount = amount,
        distanceKm = distanceKm ?: BigDecimal.ZERO,
        productType = productType ?: "alimento",
        subtractRoutes = subtractRoutes ?: false,
        billingCycleId = billingCycleId,
        occurredAt = parsedOccurredAt
    )
}

fun DailyTotal.toDto(): DailyTotalDto {
    return DailyTotalDto(
        id = if (id.isNotEmpty()) id else null,
        userId = userId,
        platformId = platformId,
        amount = amount,
        distanceKm = distanceKm,
        productType = productType,
        subtractRoutes = subtractRoutes,
        billingCycleId = billingCycleId,
        occurredAt = occurredAt.toString()
    )
}
