package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.google.gson.annotations.SerializedName

data class DeliveryRouteDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun DeliveryRouteDto.toDomain(): DeliveryRoute {
    return DeliveryRoute(
        id = id ?: "",
        userId = userId,
        name = name
    )
}

fun DeliveryRoute.toDto(): DeliveryRouteDto {
    return DeliveryRouteDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        name = name
    )
}
