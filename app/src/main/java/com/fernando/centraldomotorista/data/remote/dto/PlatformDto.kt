package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.CycleEntry
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.model.PlatformRules
import com.google.gson.annotations.SerializedName

import java.time.LocalDate

data class CycleEntryDto(
    @SerializedName("cut")
    val cut: Int = 1,
    @SerializedName("payDelay")
    val payDelay: Int = 7,
    @SerializedName("start_date")
    val startDate: String? = null,
    @SerializedName("end_date")
    val endDate: String? = null,
    @SerializedName("include_end_date")
    val includeEndDate: Boolean? = true,
    @SerializedName("pay_delay_days")
    val payDelayDays: Int? = null,
    @SerializedName("payment_date")
    val paymentDate: String? = null
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
        !cycleEntries.isNullOrEmpty() -> cycleEntries.map {
            val sDate = it.startDate?.take(10)?.let { str -> try { LocalDate.parse(str) } catch (e: Exception) { null } }
            val eDate = it.endDate?.take(10)?.let { str -> try { LocalDate.parse(str) } catch (e: Exception) { null } }
            val pDate = it.paymentDate?.take(10)?.let { str -> try { LocalDate.parse(str) } catch (e: Exception) { null } }
            val delay = it.payDelayDays ?: it.payDelay
            val calcPayDate = pDate ?: (if (eDate != null) eDate.plusDays(delay.toLong()) else null)

            CycleEntry(
                cut = it.cut,
                payDelay = delay,
                startDate = sDate,
                endDate = eDate,
                includeEndDate = it.includeEndDate ?: true,
                payDelayDays = delay,
                paymentDate = calcPayDate
            )
        }
        !cycleDays.isNullOrEmpty() -> cycleDays.map {
            CycleEntry(
                cut = it,
                payDelay = fixedPayDelay ?: 7,
                payDelayDays = fixedPayDelay ?: 7
            )
        }
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
        cycleEntries = cycleEntries.map {
            CycleEntryDto(
                cut = it.cut,
                payDelay = it.payDelayDays,
                startDate = it.startDate?.toString(),
                endDate = it.endDate?.toString(),
                includeEndDate = it.includeEndDate,
                payDelayDays = it.payDelayDays,
                paymentDate = (it.paymentDate ?: it.endDate?.plusDays(it.payDelayDays.toLong()))?.toString()
            )
        }
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

