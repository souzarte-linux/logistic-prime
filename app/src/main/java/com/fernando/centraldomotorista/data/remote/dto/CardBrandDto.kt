package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.CardBrand
import com.google.gson.annotations.SerializedName

data class CardBrandDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun CardBrandDto.toDomain(): CardBrand {
    return CardBrand(
        id = id ?: "",
        userId = userId,
        name = name
    )
}

fun CardBrand.toDto(): CardBrandDto {
    return CardBrandDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        name = name
    )
}
