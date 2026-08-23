package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.CardOperator
import com.google.gson.annotations.SerializedName

data class CardOperatorDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun CardOperatorDto.toDomain(): CardOperator {
    return CardOperator(
        id = id ?: "",
        userId = userId,
        name = name
    )
}

fun CardOperator.toDto(): CardOperatorDto {
    return CardOperatorDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        name = name
    )
}
