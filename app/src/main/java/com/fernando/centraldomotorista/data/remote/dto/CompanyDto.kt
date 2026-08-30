package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.Company
import com.google.gson.annotations.SerializedName

data class CompanyDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun CompanyDto.toDomain(): Company {
    return Company(
        id = id ?: "",
        userId = userId,
        name = name
    )
}

fun Company.toDto(): CompanyDto {
    return CompanyDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        name = name
    )
}
