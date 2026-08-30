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
    @SerializedName("cep")
    val cep: String? = null,
    @SerializedName("street")
    val street: String? = null,
    @SerializedName("number")
    val number: String? = null,
    @SerializedName("complement")
    val complement: String? = null,
    @SerializedName("cnpj")
    val cnpj: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("is_whatsapp")
    val isWhatsapp: Boolean = false,
    @SerializedName("social_media")
    val socialMedia: String? = null,
    @SerializedName("website")
    val website: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun CompanyDto.toDomain(): Company {
    return Company(
        id = id ?: "",
        userId = userId,
        name = name,
        cep = cep,
        street = street,
        number = number,
        complement = complement,
        cnpj = cnpj,
        phone = phone,
        isWhatsapp = isWhatsapp,
        socialMedia = socialMedia,
        website = website
    )
}

fun Company.toDto(): CompanyDto {
    return CompanyDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        name = name,
        cep = cep?.takeIf { it.isNotBlank() },
        street = street?.takeIf { it.isNotBlank() },
        number = number?.takeIf { it.isNotBlank() },
        complement = complement?.takeIf { it.isNotBlank() },
        cnpj = cnpj?.filter { it.isDigit() }?.takeIf { it.isNotBlank() },
        phone = phone?.takeIf { it.isNotBlank() },
        isWhatsapp = isWhatsapp,
        socialMedia = socialMedia?.takeIf { it.isNotBlank() },
        website = website?.takeIf { it.isNotBlank() }
    )
}
