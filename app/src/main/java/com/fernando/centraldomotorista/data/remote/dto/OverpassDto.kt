package com.fernando.centraldomotorista.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OverpassResponseDto(
    @SerializedName("elements")
    val elements: List<OverpassElementDto>? = emptyList()
)

data class OverpassElementDto(
    @SerializedName("type")
    val type: String?,
    @SerializedName("id")
    val id: Long?,
    @SerializedName("lat")
    val lat: Double?,
    @SerializedName("lon")
    val lon: Double?,
    @SerializedName("center")
    val center: OverpassCenterDto?,
    @SerializedName("tags")
    val tags: Map<String, String>? = emptyMap()
) {
    val latitude: Double?
        get() = lat ?: center?.lat

    val longitude: Double?
        get() = lon ?: center?.lon
}

data class OverpassCenterDto(
    @SerializedName("lat")
    val lat: Double?,
    @SerializedName("lon")
    val lon: Double?
)

data class NearbyGasStation(
    val id: String,
    val name: String,
    val brand: String,
    val street: String?,
    val number: String?,
    val neighborhood: String?,
    val city: String?,
    val state: String?,
    val cep: String?,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Float,
    val fuelTypes: List<String> = emptyList()
)
