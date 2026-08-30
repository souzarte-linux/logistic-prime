package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.PartProduct
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class PartProductDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("part_type_id")
    val partTypeId: String,
    @SerializedName("brand")
    val brand: String,
    @SerializedName("model")
    val model: String? = null,
    @SerializedName("default_life_km")
    val defaultLifeKm: BigDecimal,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun PartProductDto.toDomain(): PartProduct {
    return PartProduct(
        id = id ?: "",
        userId = userId,
        partTypeId = partTypeId,
        brand = brand,
        model = model,
        defaultLifeKm = defaultLifeKm
    )
}

fun PartProduct.toDto(): PartProductDto {
    return PartProductDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        partTypeId = partTypeId,
        brand = brand,
        model = model?.takeIf { it.isNotBlank() },
        defaultLifeKm = defaultLifeKm
    )
}
