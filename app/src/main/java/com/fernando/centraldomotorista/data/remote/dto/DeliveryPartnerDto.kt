package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class DeliveryPartnerDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("full_name")
    val fullName: String,
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
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("is_whatsapp")
    val isWhatsapp: Boolean = false,
    @SerializedName("social_media")
    val socialMedia: String? = null,
    @SerializedName("pix_key")
    val pixKey: String? = null,
    @SerializedName("pix_bank")
    val pixBank: String? = null,
    @SerializedName("cpf")
    val cpf: String? = null,
    @SerializedName("preferred_route_id")
    val preferredRouteId: String? = null,
    @SerializedName("package_rate")
    val packageRate: BigDecimal = BigDecimal.ZERO,
    @SerializedName("default_bonus")
    val defaultBonus: BigDecimal = BigDecimal.ZERO,
    @SerializedName("delivery_type")
    val deliveryType: String = "moto",
    @SerializedName("rating")
    val rating: Int = 3,
    @SerializedName("payment_cycle_type")
    val paymentCycleType: String = "fixed",
    @SerializedName("payment_cycle_fixed")
    val paymentCycleFixed: String? = null,
    @SerializedName("payment_cycle_variable_days")
    val paymentCycleVariableDays: List<Int>? = null,
    @SerializedName("active")
    val active: Boolean = true,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun DeliveryPartnerDto.toDomain(): DeliveryPartner {
    return DeliveryPartner(
        id = id ?: "",
        userId = userId,
        fullName = fullName,
        cep = cep,
        street = street,
        number = number,
        neighborhood = neighborhood,
        city = city,
        state = state,
        phone = phone,
        isWhatsapp = isWhatsapp,
        socialMedia = socialMedia,
        pixKey = pixKey,
        pixBank = pixBank,
        cpf = cpf,
        preferredRouteId = preferredRouteId,
        packageRate = packageRate,
        defaultBonus = defaultBonus,
        deliveryType = deliveryType,
        rating = rating,
        paymentCycleType = paymentCycleType,
        paymentCycleFixed = paymentCycleFixed,
        paymentCycleVariableDays = paymentCycleVariableDays,
        active = active
    )
}

fun DeliveryPartner.toDto(): DeliveryPartnerDto {
    return DeliveryPartnerDto(
        id = if (id.isNotBlank()) id else null,
        userId = userId,
        fullName = fullName.trim(),
        cep = cep?.trim()?.ifBlank { null },
        street = street?.trim()?.ifBlank { null },
        number = number?.trim()?.ifBlank { null },
        neighborhood = neighborhood?.trim()?.ifBlank { null },
        city = city?.trim()?.ifBlank { null },
        state = state?.trim()?.ifBlank { null },
        phone = phone?.filter { it.isDigit() }?.ifBlank { null },
        isWhatsapp = isWhatsapp,
        socialMedia = socialMedia?.trim()?.ifBlank { null },
        pixKey = pixKey?.trim()?.ifBlank { null },
        pixBank = pixBank?.trim()?.ifBlank { null },
        cpf = cpf?.filter { it.isDigit() }?.ifBlank { null },
        preferredRouteId = preferredRouteId?.ifBlank { null },
        packageRate = packageRate,
        defaultBonus = defaultBonus,
        deliveryType = deliveryType,
        rating = rating,
        paymentCycleType = paymentCycleType,
        paymentCycleFixed = if (paymentCycleType == "fixed") paymentCycleFixed?.ifBlank { null } else null,
        paymentCycleVariableDays = if (paymentCycleType == "variable") paymentCycleVariableDays else null,
        active = active
    )
}
