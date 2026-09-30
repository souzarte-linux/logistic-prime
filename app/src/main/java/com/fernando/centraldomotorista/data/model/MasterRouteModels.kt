package com.fernando.centraldomotorista.data.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Status operacional da rota do Usuário Master.
 */
enum class RouteStatus(val value: String) {
    EM_ANDAMENTO("em_andamento"),
    CONCLUIDA("concluida"),
    CANCELADA("cancelada");

    companion object {
        fun fromValue(value: String?): RouteStatus =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: EM_ANDAMENTO
    }
}

/**
 * Status individual de entrega de um pacote/parada.
 */
enum class StopStatus(val value: String) {
    PENDENTE("pendente"),
    ENTREGUE("entregue"),
    AUSENTE("ausente"),
    DEVOLVIDO("devolvido");

    companion object {
        fun fromValue(value: String?): StopStatus =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PENDENTE
    }
}

/**
 * Tipo de pacote bipado na rota Master.
 */
enum class PackageType(val value: String) {
    PACOTINHO("pacotinho"),
    VOLUMOSO("volumoso");

    companion object {
        fun fromValue(value: String?): PackageType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PACOTINHO
    }
}

/**
 * Status de transferência de um pacote do Master para entregador parceiro.
 */
enum class TransferStatus(val value: String) {
    ATRIBUIDO_PENDENTE("atribuido_pendente"),
    CONFIRMADO("confirmado");

    companion object {
        fun fromValue(value: String?): TransferStatus? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * Origem do pacote registrado na sessão do parceiro.
 */
enum class PackageOrigin(val value: String) {
    NOVO("novo"),
    IMPORTADO_MASTER("importado_master");

    companion object {
        fun fromValue(value: String?): PackageOrigin =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: NOVO
    }
}

/**
 * Modelo de domínio para o cabeçalho da rota do Usuário Master.
 */
data class MasterDeliveryRoute(
    val id: String = "",
    val userId: String = "",
    val platformId: String? = null,
    val routeDate: LocalDate = LocalDate.now(),
    val startLocation: String = "",
    val startLatitude: BigDecimal? = null,
    val startLongitude: BigDecimal? = null,
    val status: RouteStatus = RouteStatus.EM_ANDAMENTO,
    val totalPackages: Int = 0,
    val deliveredPackages: Int = 0,
    val returnedPackages: Int = 0,
    val createdAt: OffsetDateTime? = null,
    val finishedAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null
)

/**
 * Modelo de domínio para uma parada/pacote bipado na rota do Usuário Master.
 */
data class MasterRouteStop(
    val id: String = "",
    val routeId: String = "",
    val userId: String = "",
    val barcode: String = "",
    val recipientName: String? = null,
    val fullAddress: String = "",
    val street: String? = null,
    val number: String? = null,
    val neighborhood: String? = null,
    val city: String? = null,
    val state: String? = null,
    val cep: String? = null,
    val stopOrder: Int = 1,
    val status: StopStatus = StopStatus.PENDENTE,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val notes: String? = null,
    val scannedAt: OffsetDateTime? = null,
    val deliveredAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
    // Extensões ADR-003
    val platformId: String? = null,
    val packageType: PackageType = PackageType.PACOTINHO,
    val photoUrl: String? = null,
    val photoExpiresAt: OffsetDateTime? = null,
    val assignedPartnerId: String? = null,
    val transferStatus: TransferStatus? = null,
    val transferredVia: String? = null,
    val transferredAt: OffsetDateTime? = null,
    val marketplaceName: String? = null
)

/**
 * Modelo de domínio para Marketplaces (e-commerces geradores de pacotes de entrega).
 */
data class Marketplace(
    val id: String = "",
    val name: String,
    val active: Boolean = true
)

/**
 * Pacote individual registrado na sessão do entregador parceiro.
 */
data class PartnerSessionPackage(
    val id: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val barcode: String = "",
    val origin: PackageOrigin = PackageOrigin.NOVO,
    val masterStopId: String? = null,
    val status: String = "bipado",
    val scannedAt: OffsetDateTime? = null
)
