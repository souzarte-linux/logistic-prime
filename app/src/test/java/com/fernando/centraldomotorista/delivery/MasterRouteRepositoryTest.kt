package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.data.remote.api.MasterRouteApi
import com.fernando.centraldomotorista.data.remote.dto.FinishRouteDto
import com.fernando.centraldomotorista.data.remote.dto.MasterDeliveryRouteDto
import com.fernando.centraldomotorista.data.remote.dto.MasterRouteStopDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateRoutePackagesDto
import com.fernando.centraldomotorista.data.remote.dto.UpdateStopStatusDto
import com.fernando.centraldomotorista.data.remote.dto.toDomain
import com.fernando.centraldomotorista.data.remote.dto.toDto
import com.fernando.centraldomotorista.data.repository.MasterRouteRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class MasterRouteRepositoryTest {

    private class FakeMasterRouteApi : MasterRouteApi {
        val routesStorage = mutableMapOf<String, MasterDeliveryRouteDto>()
        val stopsStorage = mutableMapOf<String, MasterRouteStopDto>()

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
        assertEquals("Portaria 2", stop1.notes)

        // Verifica que total_packages da rota foi atualizado para 1
        val updatedRoute = repository.getRouteById(route.id)
        assertNotNull(updatedRoute)
        assertEquals(1, updatedRoute?.totalPackages)
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
        // Inicialmente nenhuma rota
        val none = repository.getActiveRoute()
        assertNull(none)

        // Cria rota ativa
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

        // Como foi concluída, não deve mais retornar como ativa
        val active = repository.getActiveRoute()
        assertNull(active)
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
            status = StopStatus.PENDENTE
        )
        val dtoStop = domainStop.toDto()
        val mappedStop = dtoStop.toDomain()
        assertEquals(domainStop.id, mappedStop.id)
        assertEquals(domainStop.barcode, mappedStop.barcode)
        assertEquals(domainStop.status, mappedStop.status)
    }
}
