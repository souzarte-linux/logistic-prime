package com.fernando.centraldomotorista.data.remote.api

import com.fernando.centraldomotorista.data.remote.dto.FinishRouteDto
import com.fernando.centraldomotorista.data.remote.dto.MasterDeliveryRouteDto
import com.fernando.centraldomotorista.data.remote.dto.MasterRouteStopDto
import com.fernando.centraldomotorista.data.remote.dto.PartnerSessionPackageDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateRoutePackagesDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopLocationDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopOrderDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopPhotoDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopStatusDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopTransferDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface MasterRouteApi {

    // --- Rotas Master (master_delivery_routes) ---

    @GET("master_delivery_routes")
    suspend fun getRoutes(
        @Query("user_id") userIdFilter: String,
        @Query("status") statusFilter: String? = null,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int? = null
    ): List<MasterDeliveryRouteDto>

    @GET("master_delivery_routes")
    suspend fun getRouteById(
        @Query("id") idFilter: String
    ): List<MasterDeliveryRouteDto>

    @Headers("Prefer: return=representation")
    @POST("master_delivery_routes")
    suspend fun createRoute(
        @Body route: MasterDeliveryRouteDto
    ): List<MasterDeliveryRouteDto>

    @Headers("Prefer: return=representation")
    @PATCH("master_delivery_routes")
    suspend fun updateRoute(
        @Query("id") idFilter: String,
        @Body route: MasterDeliveryRouteDto
    ): List<MasterDeliveryRouteDto>

    @Headers("Prefer: return=representation")
    @PATCH("master_delivery_routes")
    suspend fun finishRoute(
        @Query("id") idFilter: String,
        @Body body: FinishRouteDto
    ): List<MasterDeliveryRouteDto>

    @Headers("Prefer: return=representation")
    @PATCH("master_delivery_routes")
    suspend fun updateRouteTotalPackages(
        @Query("id") idFilter: String,
        @Body body: UpdateRoutePackagesDto
    ): List<MasterDeliveryRouteDto>

    @DELETE("master_delivery_routes")
    suspend fun deleteRoute(
        @Query("id") idFilter: String
    )

    // --- Paradas Master (master_route_stops) ---

    @GET("master_route_stops")
    suspend fun getStopsByRoute(
        @Query("route_id") routeIdFilter: String,
        @Query("order") order: String = "stop_order.asc,scanned_at.asc"
    ): List<MasterRouteStopDto>

    @GET("master_route_stops")
    suspend fun getStopById(
        @Query("id") idFilter: String
    ): List<MasterRouteStopDto>

    @GET("master_route_stops")
    suspend fun getStopByBarcode(
        @Query("route_id") routeIdFilter: String,
        @Query("barcode") barcodeFilter: String
    ): List<MasterRouteStopDto>

    @GET("master_route_stops")
    suspend fun findStopsByBarcode(
        @Query("user_id") userIdFilter: String,
        @Query("barcode") barcodeFilter: String,
        @Query("status") statusFilter: String? = null,
        @Query("order") order: String = "scanned_at.desc"
    ): List<MasterRouteStopDto>

    @Headers("Prefer: return=representation")
    @POST("master_route_stops")
    suspend fun addStop(
        @Body stop: MasterRouteStopDto
    ): List<MasterRouteStopDto>

    @Headers("Prefer: return=representation")
    @POST("master_route_stops")
    suspend fun addStopsBatch(
        @Body stops: List<MasterRouteStopDto>
    ): List<MasterRouteStopDto>

    @Headers("Prefer: return=representation")
    @PATCH("master_route_stops")
    suspend fun updateStopStatus(
        @Query("id") idFilter: String,
        @Body body: UpdateStopStatusDto
    ): List<MasterRouteStopDto>

    @Headers("Prefer: return=representation")
    @PATCH("master_route_stops")
    suspend fun updateStopTransfer(
        @Query("id") idFilter: String,
        @Body body: UpdateStopTransferDto
    ): List<MasterRouteStopDto>

    @Headers("Prefer: return=representation")
    @PATCH("master_route_stops")
    suspend fun updateStopPhoto(
        @Query("id") idFilter: String,
        @Body body: UpdateStopPhotoDto
    ): List<MasterRouteStopDto>

    @Headers("Prefer: return=representation")
    @PATCH("master_route_stops")
    suspend fun updateStopLocation(
        @Query("id") idFilter: String,
        @Body body: UpdateStopLocationDto
    ): List<MasterRouteStopDto>

    @Headers("Prefer: return=representation")
    @PATCH("master_route_stops")
    suspend fun updateStopOrder(
        @Query("id") idFilter: String,
        @Body body: UpdateStopOrderDto
    ): List<MasterRouteStopDto>

    @Headers("Prefer: return=representation")
    @PATCH("master_route_stops")
    suspend fun updateStop(
        @Query("id") idFilter: String,
        @Body stop: MasterRouteStopDto
    ): List<MasterRouteStopDto>

    @DELETE("master_route_stops")
    suspend fun deleteStop(
        @Query("id") idFilter: String
    )

    // --- Pacotes da Sessão do Parceiro (partner_session_packages) ---

    @GET("partner_session_packages")
    suspend fun getPartnerSessionPackages(
        @Query("session_id") sessionIdFilter: String,
        @Query("order") order: String = "scanned_at.asc"
    ): List<PartnerSessionPackageDto>

    @Headers("Prefer: return=representation")
    @POST("partner_session_packages")
    suspend fun addPartnerSessionPackage(
        @Body pkg: PartnerSessionPackageDto
    ): List<PartnerSessionPackageDto>

    @Headers("Prefer: return=representation")
    @POST("partner_session_packages")
    suspend fun addPartnerSessionPackagesBatch(
        @Body pkgs: List<PartnerSessionPackageDto>
    ): List<PartnerSessionPackageDto>

    @DELETE("partner_session_packages")
    suspend fun deletePartnerSessionPackage(
        @Query("id") idFilter: String
    )
}
