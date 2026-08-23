package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.CreditCard
import com.google.gson.annotations.SerializedName

data class CreditCardDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("holder_name")
    val holderName: String,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("first_four")
    val firstFour: String? = null,
    @SerializedName("last_four")
    val lastFour: String,
    @SerializedName("brand_id")
    val brandId: String? = null,
    @SerializedName("issuer_id")
    val issuerId: String? = null,
    @SerializedName("due_day")
    val dueDay: Int,
    @SerializedName("closing_day")
    val closingDay: Int,
    @SerializedName("card_type")
    val cardType: String = "credito",
    @SerializedName("active")
    val active: Boolean = true,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun CreditCardDto.toDomain(): CreditCard {
    return CreditCard(
        id = id ?: "",
        userId = userId,
        holderName = holderName,
        nickname = nickname,
        firstFour = firstFour,
        lastFour = lastFour,
        brandId = brandId,
        issuerId = issuerId,
        dueDay = dueDay,
        closingDay = closingDay,
        cardType = cardType,
        active = active
    )
}

fun CreditCard.toDto(): CreditCardDto {
    return CreditCardDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        holderName = holderName,
        nickname = nickname,
        firstFour = firstFour,
        lastFour = lastFour,
        brandId = brandId,
        issuerId = issuerId,
        dueDay = dueDay,
        closingDay = closingDay,
        cardType = cardType,
        active = active
    )
}
