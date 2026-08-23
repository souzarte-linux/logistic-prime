package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.GasStation
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class GasStationDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("nickname")
    val nickname: String? = null,
    @SerializedName("brand")
    val brand: String,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("cep")
    val cep: String? = null,
    @SerializedName("street")
    val street: String? = null,
    @SerializedName("number")
    val number: String? = null,
    @SerializedName("neighborhood")
    val neighborhood: String? = null,
    @SerializedName("city")
    val city: String? = null,
    @SerializedName("state")
    val state: String? = null,
    @SerializedName("latitude")
    val latitude: BigDecimal? = null,
    @SerializedName("longitude")
    val longitude: BigDecimal? = null,
    @SerializedName("fuel_types")
    val fuelTypes: List<String>? = emptyList(),
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun GasStationDto.toDomain(): GasStation {
    return GasStation(
        id = id ?: "",
        userId = userId,
        name = name,
        nickname = nickname,
        brand = brand,
        address = address,
        cep = cep,
        street = street,
        number = number,
        neighborhood = neighborhood,
        city = city,
        state = state,
        latitude = latitude,
        longitude = longitude,
        fuelTypes = fuelTypes ?: emptyList()
    )
}

fun GasStation.toDto(): GasStationDto {
    return GasStationDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        name = name,
        nickname = nickname,
        brand = brand,
        address = address,
        cep = cep,
        street = street,
        number = number,
        neighborhood = neighborhood,
        city = city,
        state = state,
        latitude = latitude,
        longitude = longitude,
        fuelTypes = fuelTypes
    )
}
