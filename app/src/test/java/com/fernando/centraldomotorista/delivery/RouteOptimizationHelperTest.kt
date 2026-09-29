package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.util.RouteOptimizationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class RouteOptimizationHelperTest {

    @Test
    fun testHaversineDistance_calculatesCorrectDistance() {
        // Marco Zero de São Paulo (Praça da Sé) -> MASP (Av Paulista)
        // Distância real em linha reta ~ 2.6 km
        val seLat = -23.550520
        val seLng = -46.633308

        val maspLat = -23.561414
        val maspLng = -46.655881

        val distance = RouteOptimizationHelper.haversineDistanceKm(seLat, seLng, maspLat, maspLng)

        // Deve estar próximo de 2.6 km (+/- 0.3 km)
        assertTrue(distance in 2.3..2.9)
    }

    @Test
    fun testNearestNeighborOptimization_ordersClosestStopsSequentially() {
        // Ponto de partida: Origem (0, 0)
        val startLat = 0.0
        val startLng = 0.0

        // Parada A: Longe (0, 10) ~ 1111 km
        val stopFar = MasterRouteStop(
            id = "stop-far",
            barcode = "FAR001",
            fullAddress = "Longe",
            latitude = BigDecimal.valueOf(0.0),
            longitude = BigDecimal.valueOf(10.0),
            stopOrder = 1
        )

        // Parada B: Perto (0, 1) ~ 111 km
        val stopNear = MasterRouteStop(
            id = "stop-near",
            barcode = "NEAR001",
            fullAddress = "Perto",
            latitude = BigDecimal.valueOf(0.0),
            longitude = BigDecimal.valueOf(1.0),
            stopOrder = 2
        )

        // Parada C: Média (0, 3) ~ 333 km
        val stopMid = MasterRouteStop(
            id = "stop-mid",
            barcode = "MID001",
            fullAddress = "Medio",
            latitude = BigDecimal.valueOf(0.0),
            longitude = BigDecimal.valueOf(3.0),
            stopOrder = 3
        )

        val stops = listOf(stopFar, stopNear, stopMid)

        val optimized = RouteOptimizationHelper.optimizeStopsByNearestNeighbor(
            startLat = startLat,
            startLng = startLng,
            stops = stops
        )

        // A sequência esperada saindo de (0,0) é: Near (0,1) -> Mid (0,3) -> Far (0,10)
        assertEquals(3, optimized.size)
        assertEquals("stop-near", optimized[0].id)
        assertEquals(1, optimized[0].stopOrder)

        assertEquals("stop-mid", optimized[1].id)
        assertEquals(2, optimized[1].stopOrder)

        assertEquals("stop-far", optimized[2].id)
        assertEquals(3, optimized[2].stopOrder)
    }

    @Test
    fun testNearestNeighborOptimization_keepsStopsWithoutCoordinatesAtTheEnd() {
        val startLat = -23.550520
        val startLng = -46.633308

        val stopWithCoord1 = MasterRouteStop(
            id = "stop-c1",
            barcode = "C1",
            fullAddress = "Com Coordenada 1",
            latitude = BigDecimal.valueOf(-23.56),
            longitude = BigDecimal.valueOf(-46.65),
            stopOrder = 1
        )

        val stopWithoutCoord1 = MasterRouteStop(
            id = "stop-no-coord1",
            barcode = "NC1",
            fullAddress = "Sem Coordenada 1",
            latitude = null,
            longitude = null,
            stopOrder = 2
        )

        val stopWithCoord2 = MasterRouteStop(
            id = "stop-c2",
            barcode = "C2",
            fullAddress = "Com Coordenada 2",
            latitude = BigDecimal.valueOf(-23.57),
            longitude = BigDecimal.valueOf(-46.66),
            stopOrder = 3
        )

        val stopWithoutCoord2 = MasterRouteStop(
            id = "stop-no-coord2",
            barcode = "NC2",
            fullAddress = "Sem Coordenada 2",
            latitude = null,
            longitude = null,
            stopOrder = 4
        )

        val stops = listOf(stopWithCoord1, stopWithoutCoord1, stopWithCoord2, stopWithoutCoord2)

        val optimized = RouteOptimizationHelper.optimizeStopsByNearestNeighbor(
            startLat = startLat,
            startLng = startLng,
            stops = stops
        )

        assertEquals(4, optimized.size)
        // Os dois primeiros devem ter coordenadas
        assertTrue(optimized[0].latitude != null)
        assertTrue(optimized[1].latitude != null)
        assertEquals(1, optimized[0].stopOrder)
        assertEquals(2, optimized[1].stopOrder)

        // Os dois últimos não têm coordenadas e foram mantidos ao final
        assertEquals("stop-no-coord1", optimized[2].id)
        assertEquals(3, optimized[2].stopOrder)
        assertEquals("stop-no-coord2", optimized[3].id)
        assertEquals(4, optimized[3].stopOrder)
    }

    @Test
    fun testEmptyOrSingleStop_returnsIdenticalList() {
        val emptyList = emptyList<MasterRouteStop>()
        val optimizedEmpty = RouteOptimizationHelper.optimizeStopsByNearestNeighbor(0.0, 0.0, emptyList)
        assertTrue(optimizedEmpty.isEmpty())

        val singleStop = listOf(
            MasterRouteStop(
                id = "only",
                barcode = "ONLY1",
                fullAddress = "Unica",
                latitude = BigDecimal.valueOf(-23.55),
                longitude = BigDecimal.valueOf(-46.63),
                stopOrder = 99
            )
        )
        val optimizedSingle = RouteOptimizationHelper.optimizeStopsByNearestNeighbor(0.0, 0.0, singleStop)
        assertEquals(1, optimizedSingle.size)
        assertEquals(1, optimizedSingle[0].stopOrder)
    }
}
