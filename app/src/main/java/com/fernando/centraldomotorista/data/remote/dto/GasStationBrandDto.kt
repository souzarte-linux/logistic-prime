package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.GasStationBrand
import com.google.gson.annotations.SerializedName

data class GasStationBrandDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun GasStationBrandDto.toDomain(): GasStationBrand = GasStationBrand(
    id = id ?: "",
    userId = userId,
    name = name
)

fun GasStationBrand.toDto(): GasStationBrandDto = GasStationBrandDto(
    id = id.ifBlank { null },
    userId = userId,
    name = name
)
