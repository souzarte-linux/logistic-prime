package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.Expense
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.OffsetDateTime

data class ExpenseDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("vendor")
    val vendor: String? = null,
    @SerializedName("amount")
    val amount: BigDecimal,
    @SerializedName("liters")
    val liters: BigDecimal? = null,
    @SerializedName("fuel_type")
    val fuelType: String? = null,
    @SerializedName("price_per_liter")
    val pricePerLiter: BigDecimal? = null,
    @SerializedName("odometer_km")
    val odometerKm: BigDecimal? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("payment_method")
    val paymentMethod: String? = "pix",
    @SerializedName("is_full_tank")
    val isFullTank: Boolean = true,
    @SerializedName("receipt_number")
    val receiptNumber: String? = null,
    @SerializedName("invoice_number")
    val invoiceNumber: String? = null,
    @SerializedName("part_brand")
    val partBrand: String? = null,
    @SerializedName("part_model")
    val partModel: String? = null,
    @SerializedName("card_brand")
    val cardBrand: String? = null,
    @SerializedName("card_operator")
    val cardOperator: String? = null,
    @SerializedName("installment_group_id")
    val installmentGroupId: String? = null,
    @SerializedName("installment_number")
    val installmentNumber: Int? = null,
    @SerializedName("installment_total")
    val installmentTotal: Int? = null,
    @SerializedName("card_due_day")
    val cardDueDay: Int? = null,
    @SerializedName("occurred_at")
    val occurredAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun ExpenseDto.toDomain(): Expense {
    val parsedOccurredAt = occurredAt?.let {
        try { OffsetDateTime.parse(it) } catch (e: Exception) { OffsetDateTime.now() }
    } ?: OffsetDateTime.now()

    return Expense(
        id = id ?: "",
        userId = userId,
        category = category,
        title = title,
        vendor = vendor,
        amount = amount,
        liters = liters,
        fuelType = fuelType,
        pricePerLiter = pricePerLiter,
        odometerKm = odometerKm,
        paymentMethod = paymentMethod ?: "pix",
        isFullTank = isFullTank,
        occurredAt = parsedOccurredAt,
        partBrand = partBrand,
        partModel = partModel,
        installmentGroupId = installmentGroupId,
        installmentNumber = installmentNumber,
        installmentTotal = installmentTotal
    )
}

fun Expense.toDto(): ExpenseDto {
    return ExpenseDto(
        id = if (id.isNotEmpty()) id else null,
        userId = userId,
        category = category,
        title = title,
        vendor = vendor,
        amount = amount,
        liters = liters,
        fuelType = fuelType,
        pricePerLiter = pricePerLiter,
        odometerKm = odometerKm,
        paymentMethod = paymentMethod,
        isFullTank = isFullTank,
        partBrand = partBrand,
        partModel = partModel,
        installmentGroupId = installmentGroupId,
        installmentNumber = installmentNumber,
        installmentTotal = installmentTotal,
        occurredAt = occurredAt.toString()
    )
}
