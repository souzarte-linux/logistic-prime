package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.Route
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.OffsetDateTime

data class RouteDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("platform_id")
    val platformId: String? = null,
    @SerializedName("origin")
    val origin: String? = null,
    @SerializedName("destination")
    val destination: String? = null,
    @SerializedName("distance_km")
    val distanceKm: BigDecimal = BigDecimal.ZERO,
    @SerializedName("amount")
    val amount: BigDecimal = BigDecimal.ZERO,
    @SerializedName("tip")
    val tip: BigDecimal = BigDecimal.ZERO,
    @SerializedName("bonus")
    val bonus: BigDecimal? = BigDecimal.ZERO,
    @SerializedName("product_type")
    val productType: String = "alimento",
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("package_count")
    val packageCount: Int = 1,
    @SerializedName("package_unit_price")
    val packageUnitPrice: BigDecimal = BigDecimal.ZERO,
    @SerializedName("small_packages_count")
    val smallPackagesCount: Int? = 0,
    @SerializedName("large_packages_count")
    val largePackagesCount: Int? = 0,
    @SerializedName("large_packages_prices")
    val largePackagesPrices: List<BigDecimal>? = null,
    @SerializedName("started_at")
    val startedAt: String? = null,
    @SerializedName("ended_at")
    val endedAt: String? = null,
    @SerializedName("break_minutes")
    val breakMinutes: Int = 0,
    @SerializedName("start_km")
    val startKm: BigDecimal = BigDecimal.ZERO,
    @SerializedName("end_km")
    val endKm: BigDecimal = BigDecimal.ZERO,
    @SerializedName("billing_cycle_id")
    val billingCycleId: String? = null,
    @SerializedName("occurred_at")
    val occurredAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

fun RouteDto.toDomain(): Route {
    val parsedOccurredAt = occurredAt?.let {
        try { OffsetDateTime.parse(it) } catch (e: Exception) { OffsetDateTime.now() }
    } ?: OffsetDateTime.now()

    return Route(
        id = id ?: "",
        userId = userId,
        platformId = platformId,
        origin = origin,
        destination = destination,
        distanceKm = distanceKm,
        amount = amount,
        tip = tip,
        bonus = bonus ?: BigDecimal.ZERO,
        productType = productType,
        notes = notes,
        packageCount = packageCount,
        packageUnitPrice = packageUnitPrice,
        smallPackagesCount = smallPackagesCount ?: 0,
        largePackagesCount = largePackagesCount ?: 0,
        largePackagesPrices = largePackagesPrices ?: emptyList(),
        startedAt = startedAt?.let { try { OffsetDateTime.parse(it) } catch (e: Exception) { null } },
        endedAt = endedAt?.let { try { OffsetDateTime.parse(it) } catch (e: Exception) { null } },
        breakMinutes = breakMinutes,
        startKm = startKm,
        endKm = endKm,
        billingCycleId = billingCycleId,
        occurredAt = parsedOccurredAt
    )
}

fun Route.toDto(): RouteDto {
    return RouteDto(
        id = if (id.isNotEmpty()) id else null,
        userId = userId,
        platformId = platformId,
        origin = origin,
        destination = destination,
        distanceKm = distanceKm,
        amount = amount,
        tip = tip,
        bonus = bonus,
        productType = productType,
        notes = notes,
        packageCount = packageCount,
        packageUnitPrice = packageUnitPrice,
        smallPackagesCount = smallPackagesCount,
        largePackagesCount = largePackagesCount,
        largePackagesPrices = if (largePackagesPrices.isNotEmpty()) largePackagesPrices else null,
        startedAt = startedAt?.toString(),
        endedAt = endedAt?.toString(),
        breakMinutes = breakMinutes,
        startKm = startKm,
        endKm = endKm,
        billingCycleId = billingCycleId,
        occurredAt = occurredAt.toString()
    )
}
