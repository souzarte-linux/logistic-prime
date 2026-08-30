package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.OffsetDateTime

data class PartMaintenanceDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("part_name")
    val partName: String,
    @SerializedName("life_km")
    val lifeKm: BigDecimal,
    @SerializedName("last_change_km")
    val lastChangeKm: BigDecimal,
    @SerializedName("last_change_at")
    val lastChangeAt: String? = null,
    @SerializedName("company_id")
    val companyId: String? = null,
    @SerializedName("part_product_id")
    val partProductId: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun PartMaintenanceDto.toDomain(): PartMaintenance {
    val parsedDate = lastChangeAt?.let {
        try { OffsetDateTime.parse(it) } catch (e: Exception) { OffsetDateTime.now() }
    } ?: OffsetDateTime.now()

    return PartMaintenance(
        id = id ?: "",
        userId = userId,
        partName = partName,
        lifeKm = lifeKm,
        lastChangeKm = lastChangeKm,
        lastChangeAt = parsedDate,
        companyId = companyId,
        partProductId = partProductId
    )
}

fun PartMaintenance.toDto(): PartMaintenanceDto {
    return PartMaintenanceDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        partName = partName,
        lifeKm = lifeKm,
        lastChangeKm = lastChangeKm,
        lastChangeAt = lastChangeAt.toString(),
        companyId = companyId?.takeIf { it.isNotBlank() },
        partProductId = partProductId?.takeIf { it.isNotBlank() }
    )
}
