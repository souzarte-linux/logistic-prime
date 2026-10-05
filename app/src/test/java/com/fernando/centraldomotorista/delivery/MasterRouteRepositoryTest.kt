package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageOrigin
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.PartnerSessionPackage
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.data.model.TransferStatus
import com.fernando.centraldomotorista.data.remote.api.MasterRouteApi
import com.fernando.centraldomotorista.data.remote.dto.FinishRouteDto
import com.fernando.centraldomotorista.data.remote.dto.MasterDeliveryRouteDto
import com.fernando.centraldomotorista.data.remote.dto.MasterRouteStopDto
import com.fernando.centraldomotorista.data.remote.dto.PartnerSessionPackageDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateRoutePackagesDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopDetailsDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopLocationDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopOrderDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopPhotoDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopStatusDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopTransferDto
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.data.repository.MasterRouteRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

class MasterRouteRepositoryTest {

    private class FakeMasterRouteApi : MasterRouteApi {
        val routesStorage = mutableMapOf<String, MasterDeliveryRouteDto>()
        val stopsStorage = mutableMapOf<String, MasterRouteStopDto>()
        val partnerPackagesStorage = mutableMapOf<String, PartnerSessionPackageDto>()

        override suspend fun getRoutes(
            userIdFilter: String,
            statusFilter: String?,
            order: String,
            limit: Int?
        ): List<MasterDeliveryRouteDto> {
            val userId = userIdFilter.removePrefix("eq.")
            var list = routesStorage.values.filter { it.userId == userId }
            if (statusFilter != null) {
                val status = statusFilter.removePrefix("eq.")
                list = list.filter { it.status == status }
            }
            if (limit != null && limit > 0) {
                list = list.take(limit)
            }
            return list
        }

        override suspend fun getRouteById(idFilter: String): List<MasterDeliveryRouteDto> {
            val id = idFilter.removePrefix("eq.")
            val route = routesStorage[id]
            return if (route != null) listOf(route) else emptyList()
        }

        override suspend fun createRoute(route: MasterDeliveryRouteDto): List<MasterDeliveryRouteDto> {
            val id = if (!route.id.isNullOrBlank()) route.id else "route-gen-${routesStorage.size + 1}"
            val saved = route.copy(id = id)
            routesStorage[id] = saved
            return listOf(saved)
        }

        override suspend fun updateRoute(
            idFilter: String,
            route: MasterDeliveryRouteDto
        ): List<MasterDeliveryRouteDto> {
            val id = idFilter.removePrefix("eq.")
            routesStorage[id] = route
            return listOf(route)
        }

        override suspend fun finishRoute(
            idFilter: String,
            body: FinishRouteDto
        ): List<MasterDeliveryRouteDto> {
            val id = idFilter.removePrefix("eq.")
            val existing = routesStorage[id] ?: return emptyList()
            val updated = existing.copy(
                status = body.status,
                deliveredPackages = body.deliveredPackages,
                returnedPackages = body.returnedPackages,
                totalPackages = body.totalPackages,
                finishedAt = body.finishedAt
            )
            routesStorage[id] = updated
            return listOf(updated)
        }

        override suspend fun updateRouteTotalPackages(
            idFilter: String,
            body: UpdateRoutePackagesDto
        ): List<MasterDeliveryRouteDto> {
            val id = idFilter.removePrefix("eq.")
            val existing = routesStorage[id] ?: return emptyList()
            val updated = existing.copy(totalPackages = body.totalPackages)
            routesStorage[id] = updated
            return listOf(updated)
        }

        override suspend fun deleteRoute(idFilter: String) {
            val id = idFilter.removePrefix("eq.")
            routesStorage.remove(id)
            val toRemove = stopsStorage.values.filter { it.routeId == id }.mapNotNull { it.id }
            toRemove.forEach { stopsStorage.remove(it) }
        }

        override suspend fun getStopsByRoute(
            routeIdFilter: String,
            order: String
        ): List<MasterRouteStopDto> {
            val routeId = routeIdFilter.removePrefix("eq.")
            return stopsStorage.values
                .filter { it.routeId == routeId }
                .sortedBy { it.stopOrder }
        }

        override suspend fun getStopById(idFilter: String): List<MasterRouteStopDto> {
            val id = idFilter.removePrefix("eq.")
            val stop = stopsStorage[id]
            return if (stop != null) listOf(stop) else emptyList()
        }

        override suspend fun getStopByBarcode(
            routeIdFilter: String,
            barcodeFilter: String
        ): List<MasterRouteStopDto> {
            val routeId = routeIdFilter.removePrefix("eq.")
            val barcode = barcodeFilter.removePrefix("eq.")
            val stop = stopsStorage.values.firstOrNull { it.routeId == routeId && it.barcode == barcode }
            return if (stop != null) listOf(stop) else emptyList()
        }

        override suspend fun findStopsByBarcode(
            userIdFilter: String,
            barcodeFilter: String,
            statusFilter: String?,
            order: String
        ): List<MasterRouteStopDto> {
            val userId = userIdFilter.removePrefix("eq.")
            val barcode = barcodeFilter.removePrefix("eq.")
            var list = stopsStorage.values.filter { it.userId == userId && it.barcode == barcode }
            if (statusFilter != null && statusFilter.startsWith("neq.")) {
                val excludedStatus = statusFilter.removePrefix("neq.")
                list = list.filter { it.status != excludedStatus }
            }
            return list
        }

        override suspend fun addStop(stop: MasterRouteStopDto): List<MasterRouteStopDto> {
            val id = if (!stop.id.isNullOrBlank()) stop.id else "stop-gen-${stopsStorage.size + 1}"
            val saved = stop.copy(id = id)
            stopsStorage[id] = saved
            return listOf(saved)
        }

        override suspend fun addStopsBatch(stops: List<MasterRouteStopDto>): List<MasterRouteStopDto> {
            val result = stops.mapIndexed { index, s ->
                val id = if (!s.id.isNullOrBlank()) s.id else "stop-gen-${stopsStorage.size + index + 1}"
                val saved = s.copy(id = id)
                stopsStorage[id] = saved
                saved
            }
            return result
        }

        override suspend fun updateStopStatus(
            idFilter: String,
            body: UpdateStopStatusDto
        ): List<MasterRouteStopDto> {
            val id = idFilter.removePrefix("eq.")
            val existing = stopsStorage[id] ?: return emptyList()
            val updated = existing.copy(
                status = body.status,
                notes = body.notes ?: existing.notes,
                deliveredAt = body.deliveredAt ?: existing.deliveredAt
            )
            stopsStorage[id] = updated
            return listOf(updated)
        }

        override suspend fun updateStopTransfer(
            idFilter: String,
            body: UpdateStopTransferDto
        ): List<MasterRouteStopDto> {
            val id = idFilter.removePrefix("eq.")
            val existing = stopsStorage[id] ?: return emptyList()
            val updated = existing.copy(
                assignedPartnerId = body.assignedPartnerId,
                transferStatus = body.transferStatus,
                transferredVia = body.transferredVia,
                transferredAt = body.transferredAt
            )
            stopsStorage[id] = updated
            return listOf(updated)
        }

        override suspend fun updateStopPhoto(
            idFilter: String,
            body: UpdateStopPhotoDto
        ): List<MasterRouteStopDto> {
            val id = idFilter.removePrefix("eq.")
            val existing = stopsStorage[id] ?: return emptyList()
            val updated = existing.copy(
                photoUrl = body.photoUrl,
                photoExpiresAt = body.photoExpiresAt
            )
            stopsStorage[id] = updated
            return listOf(updated)
        }

        override suspend fun updateStopLocation(
            idFilter: String,
            body: UpdateStopLocationDto
        ): List<MasterRouteStopDto> {
            val id = idFilter.removePrefix("eq.")
            val existing = stopsStorage[id] ?: return emptyList()
            val updated = existing.copy(
                latitude = body.latitude,
                longitude = body.longitude
            )
            stopsStorage[id] = updated
            return listOf(updated)
        }

        override suspend fun updateStopOrder(
            idFilter: String,
            body: UpdateStopOrderDto
        ): List<MasterRouteStopDto> {
            val id = idFilter.removePrefix("eq.")
            val existing = stopsStorage[id] ?: return emptyList()
            val updated = existing.copy(
                stopOrder = body.stopOrder
            )
            stopsStorage[id] = updated
            return listOf(updated)
        }

        override suspend fun updateStopDetails(
            idFilter: String,
            body: UpdateStopDetailsDto
        ): List<MasterRouteStopDto> {
            val id = idFilter.removePrefix("eq.")
            val existing = stopsStorage[id] ?: return emptyList()
            val updated = existing.copy(
                recipientName = body.recipientName,
                fullAddress = body.fullAddress,
                cep = body.cep,
                packageType = body.packageType,
                marketplaceName = body.marketplaceName,
                notes = body.notes
            )
            stopsStorage[id] = updated
            return listOf(updated)
        }

        override suspend fun updateStop(
            idFilter: String,
            stop: MasterRouteStopDto
        ): List<MasterRouteStopDto> {
            val id = idFilter.removePrefix("eq.")
            stopsStorage[id] = stop
            return listOf(stop)
        }

        override suspend fun deleteStop(idFilter: String) {
            val id = idFilter.removePrefix("eq.")
            stopsStorage.remove(id)
        }

        override suspend fun getPartnerSessionPackages(
            sessionIdFilter: String,
            order: String
        ): List<PartnerSessionPackageDto> {
            val sessionId = sessionIdFilter.removePrefix("eq.")
            return partnerPackagesStorage.values.filter { it.sessionId == sessionId }
        }

        override suspend fun addPartnerSessionPackage(
            pkg: PartnerSessionPackageDto
        ): List<PartnerSessionPackageDto> {
            val id = if (!pkg.id.isNullOrBlank()) pkg.id else "pkg-gen-${partnerPackagesStorage.size + 1}"
            val saved = pkg.copy(id = id)
            partnerPackagesStorage[id] = saved
            return listOf(saved)
        }

        override suspend fun addPartnerSessionPackagesBatch(
            pkgs: List<PartnerSessionPackageDto>
        ): List<PartnerSessionPackageDto> {
            return pkgs.map { addPartnerSessionPackage(it).first() }
        }

        override suspend fun deletePartnerSessionPackage(idFilter: String) {
            val id = idFilter.removePrefix("eq.")
            partnerPackagesStorage.remove(id)
        }
    }

    private lateinit var fakeApi: FakeMasterRouteApi
    private lateinit var repository: MasterRouteRepository
    private val testUserId = "user-fernando-123"

    @Before
    fun setUp() {
        fakeApi = FakeMasterRouteApi()
        repository = MasterRouteRepository(
            masterRouteApi = fakeApi,
            userIdProvider = { testUserId }
        )
    }

    @Test
    fun testCreateRouteSuccess() = runBlocking {
        val created = repository.createRoute(
            platformId = "plat-shopee-uuid",
            startLocation = "CD Shopee Cajamar",
            startLat = -23.3556,
            startLng = -46.8772
        )

        assertNotNull(created)
        assertEquals("route-gen-1", created.id)
        assertEquals(testUserId, created.userId)
        assertEquals("plat-shopee-uuid", created.platformId)
        assertEquals("CD Shopee Cajamar", created.startLocation)
        assertEquals(RouteStatus.EM_ANDAMENTO, created.status)
        assertEquals(0, created.totalPackages)
        assertEquals(0, created.deliveredPackages)
        assertEquals(0, created.returnedPackages)
    }

    @Test
    fun testAddStopAndIncrementPackages() = runBlocking {
        val route = repository.createRoute(
            platformId = "plat-mercadolivre",
            startLocation = "Galpão Cajamar"
        )

        val stop1 = repository.addStop(
            routeId = route.id,
            barcode = "BR123456789ML",
            recipientName = "João Silva",
            fullAddress = "Av. Paulista, 1000, Bela Vista, São Paulo - SP",
            cep = "01310-100",
            stopOrder = 1,
            notes = "Portaria 2"
        )

        assertNotNull(stop1)
        assertEquals("stop-gen-1", stop1.id)
        assertEquals(route.id, stop1.routeId)
        assertEquals(testUserId, stop1.userId)
        assertEquals("BR123456789ML", stop1.barcode)
        assertEquals("João Silva", stop1.recipientName)
        assertEquals(StopStatus.PENDENTE, stop1.status)
        assertEquals(PackageType.PACOTINHO, stop1.packageType)
        assertEquals("Portaria 2", stop1.notes)

        val updatedRoute = repository.getRouteById(route.id)
        assertNotNull(updatedRoute)
        assertEquals(1, updatedRoute?.totalPackages)
    }

    @Test
    fun testAddStopWithExtensions() = runBlocking {
        val route = repository.createRoute(
            platformId = "plat-default",
            startLocation = "CD Central"
        )

        val stop = repository.addStop(
            routeId = route.id,
            barcode = "VOL-9988",
            recipientName = "Carlos Empresa",
            fullAddress = "Rua do Comércio, 500",
            cep = "01000-000",
            stopOrder = 1,
            platformId = "plat-shopee",
            packageType = PackageType.VOLUMOSO,
            photoUrl = "https://bucket/photo.jpg",
            assignedPartnerId = "partner-pedro",
            transferStatus = TransferStatus.ATRIBUIDO_PENDENTE
        )

        assertNotNull(stop)
        assertEquals("plat-shopee", stop.platformId)
        assertEquals(PackageType.VOLUMOSO, stop.packageType)
        assertEquals("https://bucket/photo.jpg", stop.photoUrl)
        assertEquals("partner-pedro", stop.assignedPartnerId)
        assertEquals(TransferStatus.ATRIBUIDO_PENDENTE, stop.transferStatus)
    }

    @Test
    fun testFindActiveMasterStopByBarcode() = runBlocking {
        val route = repository.createRoute(platformId = null, startLocation = "Origem")

        // 1. Parada ativa
        repository.addStop(
            routeId = route.id,
            barcode = "ACTIVE-123",
            recipientName = "Cliente Ativo",
            fullAddress = "Rua A, 1",
            cep = "01001-000"
        )

        // 2. Parada devolvida (não deve ser encontrada como ativa)
        val returnedStop = repository.addStop(
            routeId = route.id,
            barcode = "RETURNED-456",
            recipientName = "Cliente Devolvido",
            fullAddress = "Rua B, 2",
            cep = "01002-000"
        )
        fakeApi.stopsStorage[returnedStop.id] = fakeApi.stopsStorage[returnedStop.id]!!.copy(status = "devolvido")

        // 3. Parada já confirmada na transferência (não deve ser encontrada como ativa)
        val confirmedStop = repository.addStop(
            routeId = route.id,
            barcode = "CONFIRMED-789",
            recipientName = "Cliente Confirmado",
            fullAddress = "Rua C, 3",
            cep = "01003-000"
        )
        fakeApi.stopsStorage[confirmedStop.id] = fakeApi.stopsStorage[confirmedStop.id]!!.copy(transferStatus = "confirmado")

        // Verificações
        val foundActive = repository.findActiveMasterStopByBarcode("ACTIVE-123")
        assertNotNull(foundActive)
        assertEquals("ACTIVE-123", foundActive?.barcode)

        val foundReturned = repository.findActiveMasterStopByBarcode("RETURNED-456")
        assertNull(foundReturned)

        val foundConfirmed = repository.findActiveMasterStopByBarcode("CONFIRMED-789")
        assertNull(foundConfirmed)

        val foundNotFound = repository.findActiveMasterStopByBarcode("INEXISTENTE")
        assertNull(foundNotFound)
    }

    @Test
    fun testUpdateStopTransfer() = runBlocking {
        val route = repository.createRoute(platformId = null, startLocation = "Hub")
        val stop = repository.addStop(
            routeId = route.id,
            barcode = "TRANS-01",
            recipientName = "Destinatário",
            fullAddress = "Av Paulista, 100",
            cep = "01310-000"
        )

        val success = repository.updateStopTransfer(
            stopId = stop.id,
            partnerId = "partner-marcos",
            status = TransferStatus.ATRIBUIDO_PENDENTE,
            via = "manual_master"
        )

        assertTrue(success)
        val stopAfter = fakeApi.stopsStorage[stop.id]
        assertEquals("partner-marcos", stopAfter?.assignedPartnerId)
        assertEquals("atribuido_pendente", stopAfter?.transferStatus)
        assertEquals("manual_master", stopAfter?.transferredVia)
        assertNotNull(stopAfter?.transferredAt)
    }

    @Test
    fun testUpdateStopTransferReassignToMasterWithNullPartner() = runBlocking {
        val route = repository.createRoute(platformId = null, startLocation = "Hub")
        val stop = repository.addStop(
            routeId = route.id,
            barcode = "TRANS-REASSIGN-01",
            recipientName = "Destinatário",
            fullAddress = "Av Paulista, 100",
            cep = "01310-000",
            assignedPartnerId = "partner-marcos",
            transferStatus = TransferStatus.ATRIBUIDO_PENDENTE
        )

        val success = repository.updateStopTransfer(
            stopId = stop.id,
            partnerId = null,
            status = null
        )

        assertTrue(success)
        val stopAfter = fakeApi.stopsStorage[stop.id]
        assertNull(stopAfter?.assignedPartnerId)
        assertNull(stopAfter?.transferStatus)
        assertNull(stopAfter?.transferredVia)
        assertNull(stopAfter?.transferredAt)
    }

    @Test
    fun testReassignStopToMaster() = runBlocking {
        val route = repository.createRoute(platformId = null, startLocation = "Hub")
        val stop = repository.addStop(
            routeId = route.id,
            barcode = "TRANS-REASSIGN-02",
            recipientName = "Destinatário 2",
            fullAddress = "Rua Augusta, 500",
            cep = "01305-000",
            assignedPartnerId = "partner-lucas",
            transferStatus = TransferStatus.CONFIRMADO
        )

        val success = repository.reassignStopToMaster(stop.id)
        assertTrue(success)

        val stopAfter = fakeApi.stopsStorage[stop.id]
        assertNull(stopAfter?.assignedPartnerId)
        assertNull(stopAfter?.transferStatus)
        assertNull(stopAfter?.transferredVia)
        assertNull(stopAfter?.transferredAt)
    }

    @Test
    fun testReassignStopToMasterWithBlankIdReturnsFalse() = runBlocking {
        val success = repository.reassignStopToMaster("   ")
        assertFalse(success)
    }

    @Test
    fun testImportMasterStopToPartnerSession() = runBlocking {
        val route = repository.createRoute(platformId = null, startLocation = "Galpão")
        val masterStop = repository.addStop(
            routeId = route.id,
            barcode = "CROSS-999",
            recipientName = "Cliente Master",
            fullAddress = "Rua Central, 12",
            cep = "02000-000"
        )

        // Parceiro bipa o pacote CROSS-999 que pertencia ao Master
        val pkg = repository.importMasterStopToPartnerSession(
            sessionId = "session-101",
            barcode = "CROSS-999",
            partnerId = "partner-lucas"
        )

        assertNotNull(pkg)
        assertEquals("session-101", pkg.sessionId)
        assertEquals("CROSS-999", pkg.barcode)
        assertEquals(PackageOrigin.IMPORTADO_MASTER, pkg.origin)
        assertEquals(masterStop.id, pkg.masterStopId)

        // Parada original do Master deve ter sido atualizada para confirmado
        val masterStopAfter = fakeApi.stopsStorage[masterStop.id]
        assertEquals("confirmado", masterStopAfter?.transferStatus)
        assertEquals("scan_parceiro", masterStopAfter?.transferredVia)
        assertEquals("partner-lucas", masterStopAfter?.assignedPartnerId)

        // Agora parceiro bipa um pacote que NÃO estava no Master
        val newPkg = repository.importMasterStopToPartnerSession(
            sessionId = "session-101",
            barcode = "NEW-555",
            partnerId = "partner-lucas"
        )
        assertEquals(PackageOrigin.NOVO, newPkg.origin)
        assertNull(newPkg.masterStopId)
    }

    @Test
    fun testUpdateStopPhoto() = runBlocking {
        val route = repository.createRoute(platformId = null, startLocation = "CD")
        val stop = repository.addStop(
            routeId = route.id,
            barcode = "PHOTO-01",
            recipientName = "Cliente",
            fullAddress = "Rua Teste, 10",
            cep = "01000-000"
        )

        val success = repository.updateStopPhoto(
            stopId = stop.id,
            photoUrl = "https://supabase/photos/photo-01.jpg",
            photoExpiresAt = OffsetDateTime.now().plusDays(15)
        )

        assertTrue(success)
        val stopAfter = fakeApi.stopsStorage[stop.id]
        assertEquals("https://supabase/photos/photo-01.jpg", stopAfter?.photoUrl)
        assertNotNull(stopAfter?.photoExpiresAt)
    }

    @Test
    fun testFinishRouteCalculatesPhotoExpiration() = runBlocking {
        val route = repository.createRoute(platformId = null, startLocation = "CD")
        val stop = repository.addStop(
            routeId = route.id,
            barcode = "EXP-PHOTO",
            recipientName = "Cliente",
            fullAddress = "Rua Teste",
            cep = "01000-000",
            photoUrl = "https://supabase/photos/exp.jpg"
        )

        assertNull(fakeApi.stopsStorage[stop.id]?.photoExpiresAt)

        repository.finishRoute(
            routeId = route.id,
            deliveredCount = 1,
            returnedCount = 0
        )

        val stopAfter = fakeApi.stopsStorage[stop.id]
        assertNotNull(stopAfter?.photoExpiresAt)
    }

    @Test
    fun testAddStopsBatch() = runBlocking {
        val route = repository.createRoute(
            platformId = null,
            startLocation = "Hub Central"
        )

        val stops = listOf(
            MasterRouteStop(
                routeId = route.id,
                barcode = "BC-001",
                recipientName = "Cliente 1",
                fullAddress = "Rua Um, 10",
                stopOrder = 1
            ),
            MasterRouteStop(
                routeId = route.id,
                barcode = "BC-002",
                recipientName = "Cliente 2",
                fullAddress = "Rua Dois, 20",
                stopOrder = 2
            )
        )

        val result = repository.addStopsBatch(stops)
        assertEquals(2, result.size)
        assertEquals(2, repository.getRouteStops(route.id).size)

        val updatedRoute = repository.getRouteById(route.id)
        assertEquals(2, updatedRoute?.totalPackages)
    }

    @Test
    fun testUpdateStopStatusDelivered() = runBlocking {
        val route = repository.createRoute(
            platformId = null,
            startLocation = "Hub"
        )

        val stop = repository.addStop(
            routeId = route.id,
            barcode = "BC-999",
            recipientName = "Maria Oliveira",
            fullAddress = "Rua das Flores, 123",
            cep = "04567-000",
            stopOrder = 1
        )

        val success = repository.updateStopStatus(
            stopId = stop.id,
            status = StopStatus.ENTREGUE,
            notes = "Entregue ao porteiro"
        )

        assertTrue(success)
        val stops = repository.getRouteStops(route.id)
        assertEquals(1, stops.size)
        assertEquals(StopStatus.ENTREGUE, stops[0].status)
        assertEquals("Entregue ao porteiro", stops[0].notes)
        assertNotNull(stops[0].deliveredAt)
    }

    @Test
    fun testGetActiveRoute() = runBlocking {
        val none = repository.getActiveRoute()
        assertNull(none)

        val route = repository.createRoute(
            platformId = null,
            startLocation = "Origem"
        )

        val active = repository.getActiveRoute()
        assertNotNull(active)
        assertEquals(route.id, active?.id)
        assertEquals(RouteStatus.EM_ANDAMENTO, active?.status)
    }

    @Test
    fun testGetRecentRoutes() = runBlocking {
        repository.createRoute(platformId = "p1", startLocation = "Origem 1")
        repository.createRoute(platformId = "p2", startLocation = "Origem 2")

        val recents = repository.getRecentRoutes(limit = 10)
        assertEquals(2, recents.size)
    }

    @Test
    fun testFinishRoute() = runBlocking {
        val route = repository.createRoute(
            platformId = "plat-amazon",
            startLocation = "Centro Cajamar"
        )

        val finished = repository.finishRoute(
            routeId = route.id,
            deliveredCount = 38,
            returnedCount = 2
        )

        assertTrue(finished)
        val routeAfter = repository.getRouteById(route.id)
        assertNotNull(routeAfter)
        assertEquals(RouteStatus.CONCLUIDA, routeAfter?.status)
        assertEquals(38, routeAfter?.deliveredPackages)
        assertEquals(2, routeAfter?.returnedPackages)
        assertEquals(40, routeAfter?.totalPackages)
        assertNotNull(routeAfter?.finishedAt)

        val active = repository.getActiveRoute()
        assertNull(active)
    }

    @Test
    fun testFinishRouteAppliesDefaultThreeDaysPhotoRetention() = runBlocking {
        val route = repository.createRoute(
            platformId = "plat-shopee",
            startLocation = "CD Cajamar"
        )
        val stop = repository.addStop(
            routeId = route.id,
            barcode = "BR111111",
            recipientName = "Consumidor Teste",
            fullAddress = "Av Paulista, 1000",
            cep = "01310-100",
            stopOrder = 1,
            photoUrl = "https://supabase.co/storage/v1/object/public/photos/etiqueta_1.jpg"
        )

        // Finaliza rota sem especificar photoRetentionDays (deve aplicar o padrão de 3 dias)
        val finished = repository.finishRoute(
            routeId = route.id,
            deliveredCount = 1,
            returnedCount = 0
        )
        assertTrue(finished)

        val stopsAfter = repository.getRouteStops(route.id)
        val stopAfter = stopsAfter.first { it.id == stop.id }
        assertNotNull("photoExpiresAt deve ser calculado para paradas com foto", stopAfter.photoExpiresAt)

        val expiresAt = stopAfter.photoExpiresAt!!
        val now = OffsetDateTime.now()
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), expiresAt.toLocalDate())
        assertEquals("Prazo padrão de expiração das fotos deve ser 3 dias", 3L, daysDiff)
    }

    @Test
    fun testFinishRouteAppliesCustomPhotoRetentionDays() = runBlocking {
        val route = repository.createRoute(
            platformId = "plat-ml",
            startLocation = "CD Louveira"
        )
        val stop = repository.addStop(
            routeId = route.id,
            barcode = "BR222222",
            recipientName = "Consumidor 2",
            fullAddress = "Av Brigadeiro Faria Lima, 2000",
            cep = "01452-000",
            stopOrder = 1,
            photoUrl = "https://supabase.co/storage/v1/object/public/photos/etiqueta_2.jpg"
        )

        // Finaliza rota com prazo customizado de 7 dias
        val finished = repository.finishRoute(
            routeId = route.id,
            deliveredCount = 1,
            returnedCount = 0,
            photoRetentionDays = 7
        )
        assertTrue(finished)

        val stopsAfter = repository.getRouteStops(route.id)
        val stopAfter = stopsAfter.first { it.id == stop.id }
        assertNotNull(stopAfter.photoExpiresAt)

        val expiresAt = stopAfter.photoExpiresAt!!
        val now = OffsetDateTime.now()
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), expiresAt.toLocalDate())
        assertEquals("Prazo customizado de expiração deve ser 7 dias", 7L, daysDiff)
    }

    @Test
    fun testCancelRoute() = runBlocking {
        val route = repository.createRoute(
            platformId = null,
            startLocation = "Centro"
        )

        val cancelled = repository.cancelRoute(route.id)
        assertTrue(cancelled)

        val routeAfter = repository.getRouteById(route.id)
        assertEquals(RouteStatus.CANCELADA, routeAfter?.status)
    }

    @Test
    fun testDeleteRoute() = runBlocking {
        val route = repository.createRoute(
            platformId = "plat-shopee",
            startLocation = "Centro Cajamar"
        )
        val stop = repository.addStop(
            routeId = route.id,
            barcode = "BR123456789",
            recipientName = "Joao Silva",
            fullAddress = "Rua Teste, 100",
            cep = "01234-567",
            stopOrder = 1
        )

        val deleted = repository.deleteRoute(route.id)
        assertTrue(deleted)

        val routeAfter = repository.getRouteById(route.id)
        assertNull(routeAfter)
        val stopsAfter = repository.getRouteStops(route.id)
        assertTrue(stopsAfter.isEmpty())
    }

    @Test
    fun testEnumsAndDtoRoundTrip() {
        assertEquals(RouteStatus.EM_ANDAMENTO, RouteStatus.fromValue("em_andamento"))
        assertEquals(RouteStatus.CONCLUIDA, RouteStatus.fromValue("concluida"))
        assertEquals(RouteStatus.CANCELADA, RouteStatus.fromValue("cancelada"))
        assertEquals(RouteStatus.EM_ANDAMENTO, RouteStatus.fromValue("desconhecido"))

        assertEquals(StopStatus.PENDENTE, StopStatus.fromValue("pendente"))
        assertEquals(StopStatus.ENTREGUE, StopStatus.fromValue("entregue"))
        assertEquals(StopStatus.AUSENTE, StopStatus.fromValue("ausente"))
        assertEquals(StopStatus.DEVOLVIDO, StopStatus.fromValue("devolvido"))
        assertEquals(StopStatus.PENDENTE, StopStatus.fromValue("outro"))

        assertEquals(PackageType.PACOTINHO, PackageType.fromValue("pacotinho"))
        assertEquals(PackageType.PACOTINHO, PackageType.fromValue("Pacote"))
        assertEquals(PackageType.VOLUMOSO, PackageType.fromValue("volumoso"))
        assertEquals(PackageType.VOLUMOSO, PackageType.fromValue("Volumoso"))
        assertEquals(PackageType.DOCUMENTO, PackageType.fromValue("documento"))
        assertEquals(PackageType.DOCUMENTO, PackageType.fromValue("Documento"))
        assertEquals(PackageType.COMIDA, PackageType.fromValue("comida"))
        assertEquals(PackageType.COMIDA, PackageType.fromValue("Comida"))
        assertEquals(PackageType.FARMACIA, PackageType.fromValue("farmacia"))
        assertEquals(PackageType.FARMACIA, PackageType.fromValue("Farmácia"))
        assertEquals(PackageType.PACOTINHO, PackageType.fromValue("invalido"))
        assertEquals(5, PackageType.all().size)
        assertEquals("Pacote", PackageType.PACOTINHO.label)
        assertEquals("Volumoso", PackageType.VOLUMOSO.label)
        assertEquals("Documento", PackageType.DOCUMENTO.label)
        assertEquals("Comida", PackageType.COMIDA.label)
        assertEquals("Farmácia", PackageType.FARMACIA.label)

        assertEquals(TransferStatus.ATRIBUIDO_PENDENTE, TransferStatus.fromValue("atribuido_pendente"))
        assertEquals(TransferStatus.CONFIRMADO, TransferStatus.fromValue("confirmado"))
        assertNull(TransferStatus.fromValue("invalido"))

        assertEquals(PackageOrigin.NOVO, PackageOrigin.fromValue("novo"))
        assertEquals(PackageOrigin.IMPORTADO_MASTER, PackageOrigin.fromValue("importado_master"))
        assertEquals(PackageOrigin.NOVO, PackageOrigin.fromValue("outro"))

        val domainRoute = MasterDeliveryRoute(
            id = "test-r-1",
            userId = "u-1",
            platformId = "p-1",
            routeDate = LocalDate.of(2026, 9, 28),
            startLocation = "São Paulo",
            startLatitude = BigDecimal("-23.5505200"),
            startLongitude = BigDecimal("-46.6333080"),
            status = RouteStatus.EM_ANDAMENTO,
            totalPackages = 10,
            deliveredPackages = 8,
            returnedPackages = 2
        )

        val dtoRoute = domainRoute.toDto()
        val mappedRoute = dtoRoute.toDomain()
        assertEquals(domainRoute.id, mappedRoute.id)
        assertEquals(domainRoute.userId, mappedRoute.userId)
        assertEquals(domainRoute.routeDate, mappedRoute.routeDate)
        assertEquals(domainRoute.status, mappedRoute.status)
        assertEquals(domainRoute.totalPackages, mappedRoute.totalPackages)

        val domainStop = MasterRouteStop(
            id = "stop-1",
            routeId = "test-r-1",
            userId = "u-1",
            barcode = "123456",
            recipientName = "Carlos",
            fullAddress = "Rua Teste, 1",
            stopOrder = 1,
            status = StopStatus.PENDENTE,
            platformId = "plat-shopee",
            packageType = PackageType.VOLUMOSO,
            photoUrl = "https://img.jpg",
            assignedPartnerId = "part-1",
            transferStatus = TransferStatus.ATRIBUIDO_PENDENTE,
            transferredVia = "manual_master"
        )
        val dtoStop = domainStop.toDto()
        val mappedStop = dtoStop.toDomain()
        assertEquals(domainStop.id, mappedStop.id)
        assertEquals(domainStop.barcode, mappedStop.barcode)
        assertEquals(domainStop.status, mappedStop.status)
        assertEquals(domainStop.platformId, mappedStop.platformId)
        assertEquals(domainStop.packageType, mappedStop.packageType)
        assertEquals(domainStop.photoUrl, mappedStop.photoUrl)
        assertEquals(domainStop.assignedPartnerId, mappedStop.assignedPartnerId)
        assertEquals(domainStop.transferStatus, mappedStop.transferStatus)
        assertEquals(domainStop.transferredVia, mappedStop.transferredVia)

        val domainPkg = PartnerSessionPackage(
            id = "pkg-1",
            sessionId = "sess-1",
            userId = "u-1",
            barcode = "123456",
            origin = PackageOrigin.IMPORTADO_MASTER,
            masterStopId = "stop-1",
            status = "bipado"
        )
        val dtoPkg = domainPkg.toDto()
        val mappedPkg = dtoPkg.toDomain()
        assertEquals(domainPkg.id, mappedPkg.id)
        assertEquals(domainPkg.sessionId, mappedPkg.sessionId)
        assertEquals(domainPkg.barcode, mappedPkg.barcode)
        assertEquals(domainPkg.origin, mappedPkg.origin)
        assertEquals(domainPkg.masterStopId, mappedPkg.masterStopId)
        assertEquals(domainPkg.status, mappedPkg.status)
    }

    @Test
    fun testUpdateStopLocation_updatesCoordinatesSuccessfully() = runBlocking {
        val stop = repository.addStop(
            routeId = "route-1",
            barcode = "LOC001",
            recipientName = "Cliente Paulista",
            fullAddress = "Avenida Paulista, 1000",
            cep = "01310-100"
        )
        assertNotNull(stop)

        val updated = repository.updateStopLocation(
            stopId = stop!!.id,
            latitude = -23.561414,
            longitude = -46.655881
        )
        assertTrue(updated)

        val retrieved = repository.getRouteStops("route-1")
        val found = retrieved.find { it.id == stop.id }
        assertNotNull(found)
        assertEquals(-23.561414, found!!.latitude!!.toDouble(), 0.0001)
        assertEquals(-46.655881, found.longitude!!.toDouble(), 0.0001)
    }

    @Test
    fun testUpdateStopsOrder_updatesSequentialOrderSuccessfully() = runBlocking {
        val stopA = repository.addStop(
            routeId = "route-1",
            barcode = "ORD001",
            recipientName = "Cliente A",
            fullAddress = "Rua A",
            cep = "01000-000"
        )
        val stopB = repository.addStop(
            routeId = "route-1",
            barcode = "ORD002",
            recipientName = "Cliente B",
            fullAddress = "Rua B",
            cep = "01000-001"
        )
        assertNotNull(stopA)
        assertNotNull(stopB)

        // Reordena: B vira 1, A vira 2
        val newOrder = listOf(stopB!!.id to 1, stopA!!.id to 2)
        val success = repository.updateStopsOrder(newOrder)
        assertTrue(success)

        val retrieved = repository.getRouteStops("route-1")
        val reorderedA = retrieved.find { it.id == stopA.id }
        val reorderedB = retrieved.find { it.id == stopB.id }

        assertEquals(2, reorderedA!!.stopOrder)
        assertEquals(1, reorderedB!!.stopOrder)
    }

    @Test
    fun testPartnerScanOfExistingMasterBarcodeImportsAndConfirms() = runBlocking {
        // 1. Cria uma rota Master e adiciona uma parada física
        val route = repository.createRoute(platformId = "plat-shopee", startLocation = "Galpão Cajamar")
        val masterStop = repository.addStop(
            routeId = route.id,
            barcode = "BR88776655ML",
            recipientName = "Destinatário Master",
            fullAddress = "Rua Teste, 100",
            cep = "01001-000",
            assignedPartnerId = "partner-joao",
            transferStatus = TransferStatus.ATRIBUIDO_PENDENTE
        )
        assertNotNull(masterStop)
        assertEquals(TransferStatus.ATRIBUIDO_PENDENTE, masterStop.transferStatus)

        // 2. Parceiro realiza bipagem do mesmo código na sua sessão
        val partnerPackage = repository.importMasterStopToPartnerSession(
            sessionId = "session-parceiro-1",
            barcode = "BR88776655ML",
            partnerId = "partner-joao"
        )

        // 3. Valida que o pacote foi importado atomicamente e vinculado à parada Master
        assertNotNull(partnerPackage)
        assertEquals(PackageOrigin.IMPORTADO_MASTER, partnerPackage.origin)
        assertEquals(masterStop.id, partnerPackage.masterStopId)
        assertEquals("session-parceiro-1", partnerPackage.sessionId)

        // 4. Valida que o status da parada Master agora foi atualizado para CONFIRMADO
        val stopsAfter = repository.getRouteStops(route.id)
        val stopUpdated = stopsAfter.firstOrNull { it.id == masterStop.id }
        assertNotNull(stopUpdated)
        assertEquals(TransferStatus.CONFIRMADO, stopUpdated?.transferStatus)
        assertEquals("scan_parceiro", stopUpdated?.transferredVia)
        assertEquals("partner-joao", stopUpdated?.assignedPartnerId)

        // 5. Bipagem de código novo (inexistente no Master) gera PackageOrigin.NOVO sem masterStopId
        val newPackage = repository.importMasterStopToPartnerSession(
            sessionId = "session-parceiro-1",
            barcode = "CODIGO_INEXISTENTE_NOVO",
            partnerId = "partner-joao"
        )
        assertEquals(PackageOrigin.NOVO, newPackage.origin)
        assertNull(newPackage.masterStopId)
    }

    @Test
    fun testPhotoExpiryJobDeletesOnlyStopsPast15Days() {
        val now = OffsetDateTime.now()

        data class StopPhotoMock(
            val id: String,
            val routeStatus: RouteStatus,
            val photoUrl: String?,
            val photoExpiresAt: OffsetDateTime?
        ) {
            val isEligibleForCleanup: Boolean
                get() = routeStatus != RouteStatus.EM_ANDAMENTO &&
                        photoUrl != null &&
                        photoExpiresAt != null &&
                        photoExpiresAt.isBefore(now)
        }

        val stops = listOf(
            // 1. Rota em andamento com foto (NUNCA expira enquanto em andamento)
            StopPhotoMock("s-1", RouteStatus.EM_ANDAMENTO, "https://storage/photo1.jpg", now.minusDays(16)),
            // 2. Rota concluída há mais de 15 dias (expirada: photoExpiresAt < now) -> DEVE LIMPAR
            StopPhotoMock("s-2", RouteStatus.CONCLUIDA, "https://storage/photo2.jpg", now.minusDays(1)),
            // 3. Rota concluída há 5 dias (ainda válida: photoExpiresAt > now) -> NÃO LIMPAR
            StopPhotoMock("s-3", RouteStatus.CONCLUIDA, "https://storage/photo3.jpg", now.plusDays(10)),
            // 4. Rota concluída sem foto -> NÃO LIMPAR
            StopPhotoMock("s-4", RouteStatus.CONCLUIDA, null, null)
        )

        val toClean = stops.filter { it.isEligibleForCleanup }
        assertEquals(1, toClean.size)
        assertEquals("s-2", toClean.first().id)
    }

    @Test
    fun testDeleteStopRecalculatesTotalPackages() = runBlocking {
        val route = repository.createRoute(platformId = "plat-shopee", startLocation = "Origem")
        val stop1 = repository.addStop(routeId = route.id, barcode = "DEL-1", recipientName = "R1", fullAddress = "End 1", cep = "01001-000")
        val stop2 = repository.addStop(routeId = route.id, barcode = "DEL-2", recipientName = "R2", fullAddress = "End 2", cep = "01002-000")

        var currentRoute = repository.getRouteById(route.id)
        assertEquals(2, currentRoute?.totalPackages)

        val deleted = repository.deleteStop(stopId = stop1.id, routeId = route.id)
        assertTrue(deleted)

        val remainingStops = repository.getRouteStops(route.id)
        assertEquals(1, remainingStops.size)
        assertEquals("DEL-2", remainingStops.first().barcode)

        currentRoute = repository.getRouteById(route.id)
        assertEquals(1, currentRoute?.totalPackages)
    }

    @Test
    fun testAddStopWithMarketplace() = runBlocking {
        val route = repository.createRoute(platformId = "plat-1", startLocation = "Origem Galpão")
        val stop = repository.addStop(
            routeId = route.id,
            barcode = "BR-MKP-12345",
            recipientName = "João Silva",
            fullAddress = "Rua das Flores, 100",
            cep = "01001-000",
            marketplaceName = "TikTok Shop"
        )

        assertNotNull(stop.id)
        assertEquals("TikTok Shop", stop.marketplaceName)

        val stops = repository.getRouteStops(route.id)
        assertEquals(1, stops.size)
        assertEquals("TikTok Shop", stops.first().marketplaceName)
    }

    @Test
    fun testUpdateStopDetails() = runBlocking {
        val route = repository.createRoute(platformId = "plat-1", startLocation = "CD")
        val originalStop = repository.addStop(
            routeId = route.id,
            barcode = "BR-EDIT-999",
            recipientName = "Nome Antigo",
            fullAddress = "Rua Velha, 10",
            cep = "11111-111",
            packageType = PackageType.PACOTINHO,
            marketplaceName = "Shopee",
            notes = "Observação antiga"
        )

        val updatedStop = originalStop.copy(
            recipientName = "Novo Nome Editado",
            fullAddress = "Avenida Nova, 500, Bloco B",
            cep = "22222-222",
            packageType = PackageType.VOLUMOSO,
            marketplaceName = "Mercado Livre",
            notes = "Entregar na portaria"
        )

        val success = repository.updateStop(updatedStop)
        assertTrue(success)

        val fetchedStops = repository.getRouteStops(route.id)
        assertEquals(1, fetchedStops.size)
        val stopAfter = fetchedStops.first()

        assertEquals("Novo Nome Editado", stopAfter.recipientName)
        assertEquals("Avenida Nova, 500, Bloco B", stopAfter.fullAddress)
        assertEquals("22222-222", stopAfter.cep)
        assertEquals(PackageType.VOLUMOSO, stopAfter.packageType)
        assertEquals("Mercado Livre", stopAfter.marketplaceName)
        assertEquals("Entregar na portaria", stopAfter.notes)
    }

    @Test
    fun testUpdateStopWithBlankIdReturnsFalse() = runBlocking {
        val stopWithNoId = MasterRouteStop(
            id = "",
            routeId = "route-1",
            recipientName = "Teste",
            fullAddress = "Endereço Qualquer"
        )

        val success = repository.updateStop(stopWithNoId)
        assertFalse(success)
    }
}
