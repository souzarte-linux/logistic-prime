package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.Profile
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.OffsetDateTime

data class ProfileDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("full_name")
    val fullName: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("social_handle")
    val socialHandle: String? = null,
    @SerializedName("vehicle")
    val vehicle: String? = "moto",
    @SerializedName("plate")
    val plate: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("daily_goal")
    val dailyGoal: BigDecimal? = BigDecimal("200"),
    @SerializedName("weekly_goal")
    val weeklyGoal: BigDecimal? = BigDecimal("1000"),
    @SerializedName("monthly_goal")
    val monthlyGoal: BigDecimal? = BigDecimal("3450"),
    @SerializedName("gender")
    val gender: String? = null,
    @SerializedName("vehicle_brand")
    val vehicleBrand: String? = null,
    @SerializedName("vehicle_model")
    val vehicleModel: String? = null,
    @SerializedName("vehicle_year")
    val vehicleYear: Int? = null,
    @SerializedName("tank_size_l")
    val tankSizeL: BigDecimal? = null,
    @SerializedName("avg_consumption_kml")
    val avgConsumptionKml: BigDecimal? = null,
    @SerializedName("oil_change_km")
    val oilChangeKm: BigDecimal? = null,
    @SerializedName("tire_size_front")
    val tireSizeFront: String? = null,
    @SerializedName("tire_size_rear")
    val tireSizeRear: String? = null,
    @SerializedName("has_bag")
    val hasBag: Boolean? = false,
    @SerializedName("last_oil_change_at")
    val lastOilChangeAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

fun ProfileDto.toDomain(): Profile {
    return Profile(
        id = id,
        fullName = fullName,
        email = email,
        phone = phone,
        vehicle = vehicle ?: "moto",
        plate = plate,
        avatarUrl = avatarUrl,
        dailyGoal = dailyGoal ?: BigDecimal("200"),
        weeklyGoal = weeklyGoal ?: BigDecimal("1000"),
        monthlyGoal = monthlyGoal ?: BigDecimal("3450"),
        vehicleBrand = vehicleBrand,
        vehicleModel = vehicleModel,
        vehicleYear = vehicleYear,
        tankSizeL = tankSizeL,
        avgConsumptionKml = avgConsumptionKml,
        oilChangeKm = oilChangeKm,
        tireSizeFront = tireSizeFront,
        tireSizeRear = tireSizeRear,
        hasBag = hasBag ?: false,
        lastOilChangeAt = lastOilChangeAt?.let {
            try { OffsetDateTime.parse(it) } catch (e: Exception) { null }
        }
    )
}

fun Profile.toDto(): ProfileDto {
    return ProfileDto(
        id = id,
        fullName = fullName,
        email = email,
        phone = phone,
        vehicle = vehicle,
        plate = plate,
        avatarUrl = avatarUrl,
        dailyGoal = dailyGoal,
        weeklyGoal = weeklyGoal,
        monthlyGoal = monthlyGoal,
        vehicleBrand = vehicleBrand,
        vehicleModel = vehicleModel,
        vehicleYear = vehicleYear,
        tankSizeL = tankSizeL,
        avgConsumptionKml = avgConsumptionKml,
        oilChangeKm = oilChangeKm,
        tireSizeFront = tireSizeFront,
        tireSizeRear = tireSizeRear,
        hasBag = hasBag,
        lastOilChangeAt = lastOilChangeAt?.toString()
    )
}
