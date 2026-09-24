package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.FinancialAdjustment
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.LocalDate

data class FinancialAdjustmentDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("platform_id")
    val platformId: String,
    @SerializedName("billing_cycle_id")
    val billingCycleId: String? = null,
    @SerializedName("type")
    val type: String,
    @SerializedName("subtype")
    val subtype: String? = null,
    @SerializedName("amount")
    val amount: BigDecimal,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("occurred_at")
    val occurredAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun FinancialAdjustmentDto.toDomain(): FinancialAdjustment {
    val parsedOccurredAt = try {
        if (!occurredAt.isNullOrBlank()) {
            LocalDate.parse(occurredAt.take(10))
        } else {
            LocalDate.now()
        }
    } catch (e: Exception) {
        LocalDate.now()
    }

    return FinancialAdjustment(
        id = id ?: "",
        userId = userId,
        platformId = platformId,
        billingCycleId = billingCycleId,
        type = type,
        subtype = subtype,
        amount = amount,
        description = description,
        notes = notes,
        occurredAt = parsedOccurredAt
    )
}

fun FinancialAdjustment.toDto(): FinancialAdjustmentDto {
    return FinancialAdjustmentDto(
        id = id.ifBlank { null },
        userId = userId,
        platformId = platformId,
        billingCycleId = billingCycleId,
        type = type,
        subtype = subtype,
        amount = amount,
        description = description,
        notes = notes,
        occurredAt = occurredAt.toString()
    )
}
