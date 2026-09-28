package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.util.parseToLocalOffsetDateTime
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.LocalDate

data class MasterDeliveryRouteDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("platform_id")
    val platformId: String? = null,
    @SerializedName("route_date")
    val routeDate: String? = null,
    @SerializedName("start_location")
    val startLocation: String,
    @SerializedName("start_latitude")
    val startLatitude: BigDecimal? = null,
    @SerializedName("start_longitude")
    val startLongitude: BigDecimal? = null,
    @SerializedName("status")
    val status: String = "em_andamento",
    @SerializedName("total_packages")
    val totalPackages: Int = 0,
    @SerializedName("delivered_packages")
    val deliveredPackages: Int = 0,
    @SerializedName("returned_packages")
    val returnedPackages: Int = 0,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("finished_at")
    val finishedAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class MasterRouteStopDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("route_id")
    val routeId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("barcode")
    val barcode: String,
    @SerializedName("recipient_name")
    val recipientName: String? = null,
    @SerializedName("full_address")
    val fullAddress: String,
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
    @SerializedName("cep")
    val cep: String? = null,
    @SerializedName("stop_order")
    val stopOrder: Int = 1,
    @SerializedName("status")
    val status: String = "pendente",
    @SerializedName("latitude")
    val latitude: BigDecimal? = null,
    @SerializedName("longitude")
    val longitude: BigDecimal? = null,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("scanned_at")
    val scannedAt: String? = null,
    @SerializedName("delivered_at")
    val deliveredAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class UpdateStopStatusDto(
    @SerializedName("status")
    val status: String,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("delivered_at")
    val deliveredAt: String? = null
)

data class FinishRouteDto(
    @SerializedName("status")
    val status: String = "concluida",
    @SerializedName("delivered_packages")
    val deliveredPackages: Int,
    @SerializedName("returned_packages")
    val returnedPackages: Int,
    @SerializedName("total_packages")
    val totalPackages: Int,
    @SerializedName("finished_at")
    val finishedAt: String
)

data class UpdateRoutePackagesDto(
    @SerializedName("total_packages")
    val totalPackages: Int
)

fun MasterDeliveryRouteDto.toDomain(): MasterDeliveryRoute {
    val parsedDate = try {
        if (!routeDate.isNullOrBlank()) LocalDate.parse(routeDate.take(10)) else LocalDate.now()
    } catch (e: Exception) {
        LocalDate.now()
    }

    return MasterDeliveryRoute(
        id = id ?: "",
        userId = userId,
        platformId = platformId,
        routeDate = parsedDate,
        startLocation = startLocation,
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        status = RouteStatus.fromValue(status),
        totalPackages = totalPackages,
        deliveredPackages = deliveredPackages,
        returnedPackages = returnedPackages,
        createdAt = parseToLocalOffsetDateTime(createdAt),
        finishedAt = parseToLocalOffsetDateTime(finishedAt),
        updatedAt = parseToLocalOffsetDateTime(updatedAt)
    )
}

fun MasterDeliveryRoute.toDto(): MasterDeliveryRouteDto {
    return MasterDeliveryRouteDto(
        id = id.ifBlank { null },
        userId = userId,
        platformId = platformId,
        routeDate = routeDate.toString(),
        startLocation = startLocation,
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        status = status.value,
        totalPackages = totalPackages,
        deliveredPackages = deliveredPackages,
        returnedPackages = returnedPackages,
        createdAt = createdAt?.toString(),
        finishedAt = finishedAt?.toString(),
        updatedAt = updatedAt?.toString()
    )
}

fun MasterRouteStopDto.toDomain(): MasterRouteStop {
    return MasterRouteStop(
        id = id ?: "",
        routeId = routeId,
        userId = userId,
        barcode = barcode,
        recipientName = recipientName,
        fullAddress = fullAddress,
        street = street,
        number = number,
        neighborhood = neighborhood,
        city = city,
        state = state,
        cep = cep,
        stopOrder = stopOrder,
        status = StopStatus.fromValue(status),
        latitude = latitude,
        longitude = longitude,
        notes = notes,
        scannedAt = parseToLocalOffsetDateTime(scannedAt),
        deliveredAt = parseToLocalOffsetDateTime(deliveredAt),
        updatedAt = parseToLocalOffsetDateTime(updatedAt)
    )
}

fun MasterRouteStop.toDto(): MasterRouteStopDto {
    return MasterRouteStopDto(
        id = id.ifBlank { null },
        routeId = routeId,
        userId = userId,
        barcode = barcode,
        recipientName = recipientName,
        fullAddress = fullAddress,
        street = street,
        number = number,
        neighborhood = neighborhood,
        city = city,
        state = state,
        cep = cep,
        stopOrder = stopOrder,
        status = status.value,
        latitude = latitude,
        longitude = longitude,
        notes = notes,
        scannedAt = scannedAt?.toString(),
        deliveredAt = deliveredAt?.toString(),
        updatedAt = updatedAt?.toString()
    )
}
