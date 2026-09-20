package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.CycleEntry
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.model.PlatformRules
import com.google.gson.annotations.SerializedName

data class CycleEntryDto(
    @SerializedName("cut")
    val cut: Int = 1,
    @SerializedName("payDelay")
    val payDelay: Int = 7
)

data class PlatformRulesDto(
    @SerializedName("fixed_pay_delay")
    val fixedPayDelay: Int? = null,
    @SerializedName("cycle_entries")
    val cycleEntries: List<CycleEntryDto>? = null,
    @SerializedName("cycle_days")
    val cycleDays: List<Int>? = null
)

data class PlatformDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("partner_id")
    val partnerId: String? = null,
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
    val paymentModel: String = "producao",
    @SerializedName("rules")
    val rules: PlatformRulesDto? = null,
    @SerializedName("bank_name")
    val bankName: String? = null,
    @SerializedName("bank_agency")
    val bankAgency: String? = null,
    @SerializedName("bank_account")
    val bankAccount: String? = null,
    @SerializedName("pix_key_type")
    val pixKeyType: String? = null,
    @SerializedName("pix_key")
    val pixKey: String? = null,
    @SerializedName("pix_bank")
    val pixBank: String? = null
)

fun PlatformRulesDto?.toDomain(): PlatformRules {
    if (this == null) return PlatformRules()
    val entries = when {
        !cycleEntries.isNullOrEmpty() -> cycleEntries.map { CycleEntry(cut = it.cut, payDelay = it.payDelay) }
        !cycleDays.isNullOrEmpty() -> cycleDays.map { CycleEntry(cut = it, payDelay = fixedPayDelay ?: 7) }
        else -> emptyList()
    }
    return PlatformRules(
        fixedPayDelay = fixedPayDelay ?: 7,
        cycleEntries = entries
    )
}

fun PlatformRules.toDto(): PlatformRulesDto {
    return PlatformRulesDto(
        fixedPayDelay = fixedPayDelay,
        cycleEntries = cycleEntries.map { CycleEntryDto(cut = it.cut, payDelay = it.payDelay) }
    )
}

fun PlatformDto.toDomain(): Platform {
    return Platform(
        id = id ?: "",
        userId = userId,
        partnerId = partnerId,
        name = name,
        cycle = cycle,
        paymentDay = paymentDay,
        active = active,
        segment = segment,
        paymentModel = paymentModel,
        rules = rules.toDomain(),
        bankName = bankName,
        bankAgency = bankAgency,
        bankAccount = bankAccount,
        pixKeyType = pixKeyType,
        pixKey = pixKey,
        pixBank = pixBank
    )
}

fun Platform.toDto(): PlatformDto {
    return PlatformDto(
        id = if (id.isNotEmpty()) id else null,
        userId = userId,
        partnerId = partnerId,
        name = name,
        cycle = cycle,
        paymentDay = paymentDay,
        active = active,
        segment = segment,
        paymentModel = paymentModel,
        rules = rules.toDto(),
        bankName = bankName,
        bankAgency = bankAgency,
        bankAccount = bankAccount,
        pixKeyType = pixKeyType,
        pixKey = pixKey,
        pixBank = pixBank
    )
}

