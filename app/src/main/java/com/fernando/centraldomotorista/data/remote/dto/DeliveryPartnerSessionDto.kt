package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.OffsetDateTime

import com.fernando.centraldomotorista.util.parseToLocalOffsetDateTime

data class DeliveryPartnerSessionDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("partner_id")
    val partnerId: String,
    @SerializedName("route_id")
    val routeId: String? = null,
    @SerializedName("expected_package_count")
    val expectedPackageCount: Int = 0,
    @SerializedName("scanned_barcodes")
    val scannedBarcodes: List<String> = emptyList(),
    @SerializedName("scanned_count")
    val scannedCount: Int = 0,
    @SerializedName("delivered_count")
    val deliveredCount: Int = 0,
    @SerializedName("returned_count")
    val returnedCount: Int = 0,
    @SerializedName("start_time")
    val startTime: String? = null,
    @SerializedName("end_time")
    val endTime: String? = null,
    @SerializedName("amount_paid")
    val amountPaid: BigDecimal = BigDecimal.ZERO,
    @SerializedName("expense_id")
    val expenseId: String? = null,
    @SerializedName("package_rate")
    val packageRate: BigDecimal? = null,
    @SerializedName("default_bonus")
    val defaultBonus: BigDecimal? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun DeliveryPartnerSessionDto.toDomain(): DeliveryPartnerSession {
    val parsedStartTime = parseToLocalOffsetDateTime(startTime)
    val parsedEndTime = parseToLocalOffsetDateTime(endTime)
    val parsedCreatedAt = parseToLocalOffsetDateTime(createdAt)

    return DeliveryPartnerSession(
        id = id ?: "",
        userId = userId,
        partnerId = partnerId,
        routeId = routeId,
        expectedPackageCount = expectedPackageCount,
        scannedBarcodes = scannedBarcodes,
        scannedCount = scannedCount,
        deliveredCount = deliveredCount,
        returnedCount = returnedCount,
        startTime = parsedStartTime,
        endTime = parsedEndTime,
        amountPaid = amountPaid,
        expenseId = expenseId,
        createdAt = parsedCreatedAt,
        packageRate = packageRate ?: BigDecimal.ZERO,
        defaultBonus = defaultBonus ?: BigDecimal.ZERO
    )
}

fun DeliveryPartnerSession.toDto(): DeliveryPartnerSessionDto {
    return DeliveryPartnerSessionDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        partnerId = partnerId,
        routeId = routeId,
        expectedPackageCount = expectedPackageCount,
        scannedBarcodes = scannedBarcodes,
        scannedCount = scannedCount,
        deliveredCount = deliveredCount,
        returnedCount = returnedCount,
        startTime = startTime?.toString(),
        endTime = endTime?.toString(),
        amountPaid = amountPaid,
        expenseId = expenseId,
        packageRate = packageRate,
        defaultBonus = defaultBonus,
        createdAt = createdAt?.toString()
    )
}
