package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.BillingCycle
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class BillingCycleDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("platform_id")
    val platformId: String,
    @SerializedName("period_start")
    val periodStart: String,
    @SerializedName("period_end")
    val periodEnd: String,
    @SerializedName("expected_payment_date")
    val expectedPaymentDate: String,
    @SerializedName("status")
    val status: String = "pending",
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun BillingCycleDto.toDomain(): BillingCycle {
    return BillingCycle(
        id = id,
        userId = userId,
        platformId = platformId,
        periodStart = try { LocalDate.parse(periodStart) } catch (e: Exception) { LocalDate.now() },
        periodEnd = try { LocalDate.parse(periodEnd) } catch (e: Exception) { LocalDate.now() },
        expectedPaymentDate = try { LocalDate.parse(expectedPaymentDate) } catch (e: Exception) { LocalDate.now() },
        status = status
    )
}
