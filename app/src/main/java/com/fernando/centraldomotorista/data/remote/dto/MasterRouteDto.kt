package com.fernando.centraldomotorista.data.remote.dto

import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageOrigin
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.PartnerSessionPackage
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.data.model.TransferStatus
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
    val updatedAt: String? = null,
    // Extensões ADR-003
    @SerializedName("platform_id")
    val platformId: String? = null,
    @SerializedName("package_type")
    val packageType: String = "pacotinho",
    @SerializedName("photo_url")
    val photoUrl: String? = null,
    @SerializedName("photo_expires_at")
    val photoExpiresAt: String? = null,
    @SerializedName("assigned_partner_id")
    val assignedPartnerId: String? = null,
    @SerializedName("transfer_status")
    val transferStatus: String? = null,
    @SerializedName("transferred_via")
    val transferredVia: String? = null,
    @SerializedName("transferred_at")
    val transferredAt: String? = null,
    @SerializedName("marketplace_name")
    val marketplaceName: String? = null
)

data class UpdateStopDetailsDto(
    @SerializedName("recipient_name")
    val recipientName: String?,
    @SerializedName("full_address")
    val fullAddress: String,
    @SerializedName("cep")
    val cep: String?,
    @SerializedName("package_type")
    val packageType: String,
    @SerializedName("marketplace_name")
    val marketplaceName: String?,
    @SerializedName("notes")
    val notes: String?
)

data class UpdateStopStatusDto(
    @SerializedName("status")
    val status: String,
    @SerializedName("notes")
    val notes: String? = null,
    @SerializedName("delivered_at")
    val deliveredAt: String? = null
)

data class UpdateStopTransferDto(
    @SerializedName("assigned_partner_id")
    val assignedPartnerId: String?,
    @SerializedName("transfer_status")
    val transferStatus: String?,
    @SerializedName("transferred_via")
    val transferredVia: String?,
    @SerializedName("transferred_at")
    val transferredAt: String?
)

data class UpdateStopPhotoDto(
    @SerializedName("photo_url")
    val photoUrl: String?,
    @SerializedName("photo_expires_at")
    val photoExpiresAt: String?
)

data class UpdateStopLocationDto(
    @SerializedName("latitude")
    val latitude: BigDecimal?,
    @SerializedName("longitude")
    val longitude: BigDecimal?
)

data class UpdateStopOrderDto(
    @SerializedName("stop_order")
    val stopOrder: Int
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

data class PartnerSessionPackageDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("barcode")
    val barcode: String,
    @SerializedName("origin")
    val origin: String = "novo",
    @SerializedName("master_stop_id")
    val masterStopId: String? = null,
    @SerializedName("status")
    val status: String = "bipado",
    @SerializedName("scanned_at")
    val scannedAt: String? = null
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
        updatedAt = parseToLocalOffsetDateTime(updatedAt),
        platformId = platformId,
        packageType = PackageType.fromValue(packageType),
        photoUrl = photoUrl,
        photoExpiresAt = parseToLocalOffsetDateTime(photoExpiresAt),
        assignedPartnerId = assignedPartnerId,
        transferStatus = TransferStatus.fromValue(transferStatus),
        transferredVia = transferredVia,
        transferredAt = parseToLocalOffsetDateTime(transferredAt),
        marketplaceName = marketplaceName
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
        updatedAt = updatedAt?.toString(),
        platformId = platformId,
        packageType = packageType.value,
        photoUrl = photoUrl,
        photoExpiresAt = photoExpiresAt?.toString(),
        assignedPartnerId = assignedPartnerId,
        transferStatus = transferStatus?.value,
        transferredVia = transferredVia,
        transferredAt = transferredAt?.toString(),
        marketplaceName = marketplaceName
    )
}

fun PartnerSessionPackageDto.toDomain(): PartnerSessionPackage {
    return PartnerSessionPackage(
        id = id ?: "",
        sessionId = sessionId,
        userId = userId,
        barcode = barcode,
        origin = PackageOrigin.fromValue(origin),
        masterStopId = masterStopId,
        status = status,
        scannedAt = parseToLocalOffsetDateTime(scannedAt)
    )
}

fun PartnerSessionPackage.toDto(): PartnerSessionPackageDto {
    return PartnerSessionPackageDto(
        id = id.ifBlank { null },
        sessionId = sessionId,
        userId = userId,
        barcode = barcode,
        origin = origin.value,
        masterStopId = masterStopId,
        status = status,
        scannedAt = scannedAt?.toString()
    )
}
