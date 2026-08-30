package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.Platform
import com.google.gson.annotations.SerializedName

data class PlatformDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("cycle")
    val cycle: String = "semanal",
    @SerializedName("payment_day")
    val paymentDay: String? = null,
    @SerializedName("active")
    val active: Boolean = true,
    @SerializedName("segment")
    val segment: String = "logistica",
    @SerializedName("payment_model")
    val paymentModel: String = "producao"
)

fun PlatformDto.toDomain(): Platform {
    return Platform(
        id = id ?: "",
        userId = userId,
        name = name,
        cycle = cycle,
        paymentDay = paymentDay,
        active = active,
        segment = segment,
        paymentModel = paymentModel
    )
}

fun Platform.toDto(): PlatformDto {
    return PlatformDto(
        id = if (id.isNotEmpty()) id else null,
        userId = userId,
        name = name,
        cycle = cycle,
        paymentDay = paymentDay,
        active = active,
        segment = segment,
        paymentModel = paymentModel
    )
}
