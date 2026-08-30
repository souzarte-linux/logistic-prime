package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.PartType
import com.google.gson.annotations.SerializedName

data class PartTypeDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun PartTypeDto.toDomain(): PartType {
    return PartType(
        id = id ?: "",
        userId = userId,
        name = name
    )
}

fun PartType.toDto(): PartTypeDto {
    return PartTypeDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        name = name
    )
}
