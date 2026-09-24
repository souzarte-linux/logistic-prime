package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.BillingCycle
import com.fernando.centraldomotorista.data.model.normalizeBillingCycleStatus
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.LocalDate

data class BillingCycleDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("platform_id")
    val platformId: String,
    @SerializedName("period_start")
    val periodStart: String,
    @SerializedName("period_end")
    val periodEnd: String,
    @SerializedName("expected_payment_date")
    val expectedPaymentDate: String,
    @SerializedName("status")
    val status: String = "em_aberto",
    @SerializedName("include_end_date")
    val includeEndDate: Boolean? = true,
    @SerializedName("payment_received_date")
    val paymentReceivedDate: String? = null,
    @SerializedName("gross_routes_amount")
    val grossRoutesAmount: BigDecimal? = null,
    @SerializedName("total_tips_amount")
    val totalTipsAmount: BigDecimal? = null,
    @SerializedName("total_bonus_amount")
    val totalBonusAmount: BigDecimal? = null,
    @SerializedName("gross_daily_amount")
    val grossDailyAmount: BigDecimal? = null,
    @SerializedName("total_adjustments_credit")
    val totalAdjustmentsCredit: BigDecimal? = null,
    @SerializedName("total_adjustments_debit")
    val totalAdjustmentsDebit: BigDecimal? = null,
    @SerializedName("net_total_amount")
    val netTotalAmount: BigDecimal? = null,
    @SerializedName("routes_count")
    val routesCount: Int? = null,
    @SerializedName("packages_count")
    val packagesCount: Int? = null,
    @SerializedName("daily_totals_count")
    val dailyTotalsCount: Int? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun BillingCycleDto.toDomain(): BillingCycle {
    val parsedPeriodStart = try { LocalDate.parse(periodStart.take(10)) } catch (e: Exception) { LocalDate.now() }
    val parsedPeriodEnd = try { LocalDate.parse(periodEnd.take(10)) } catch (e: Exception) { LocalDate.now() }
    val parsedExpectedPay = try { LocalDate.parse(expectedPaymentDate.take(10)) } catch (e: Exception) { LocalDate.now() }
    val parsedReceivedDate = paymentReceivedDate?.take(10)?.let {
        try { LocalDate.parse(it) } catch (e: Exception) { null }
    }

    return BillingCycle(
        id = id ?: "",
        userId = userId,
        platformId = platformId,
        periodStart = parsedPeriodStart,
        periodEnd = parsedPeriodEnd,
        expectedPaymentDate = parsedExpectedPay,
        status = normalizeBillingCycleStatus(status, parsedPeriodEnd),
        includeEndDate = includeEndDate ?: true,
        paymentReceivedDate = parsedReceivedDate,
        grossRoutesAmount = grossRoutesAmount ?: BigDecimal.ZERO,
        totalTipsAmount = totalTipsAmount ?: BigDecimal.ZERO,
        totalBonusAmount = totalBonusAmount ?: BigDecimal.ZERO,
        grossDailyAmount = grossDailyAmount ?: BigDecimal.ZERO,
        totalAdjustmentsCredit = totalAdjustmentsCredit ?: BigDecimal.ZERO,
        totalAdjustmentsDebit = totalAdjustmentsDebit ?: BigDecimal.ZERO,
        netTotalAmount = netTotalAmount ?: BigDecimal.ZERO,
        routesCount = routesCount ?: 0,
        packagesCount = packagesCount ?: 0,
        dailyTotalsCount = dailyTotalsCount ?: 0
    )
}

fun BillingCycle.toDto(): BillingCycleDto {
    return BillingCycleDto(
        id = id.ifBlank { null },
        userId = userId,
        platformId = platformId,
        periodStart = periodStart.toString(),
        periodEnd = periodEnd.toString(),
        expectedPaymentDate = expectedPaymentDate.toString(),
        status = normalizeBillingCycleStatus(status, periodEnd),
        includeEndDate = includeEndDate,
        paymentReceivedDate = paymentReceivedDate?.toString(),
        grossRoutesAmount = grossRoutesAmount,
        totalTipsAmount = totalTipsAmount,
        totalBonusAmount = totalBonusAmount,
        grossDailyAmount = grossDailyAmount,
        totalAdjustmentsCredit = totalAdjustmentsCredit,
        totalAdjustmentsDebit = totalAdjustmentsDebit,
        netTotalAmount = netTotalAmount,
        routesCount = routesCount,
        packagesCount = packagesCount,
        dailyTotalsCount = dailyTotalsCount
    )
}

