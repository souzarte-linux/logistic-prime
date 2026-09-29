package com.fernando.centraldomotorista.util

import com.fernando.centraldomotorista.data.model.MasterRouteStop
import java.math.BigDecimal
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utilitário para ordenação e otimização de paradas de entrega na rota Master.
 *
 * Implementa:
 * 1. Cálculo de distância geodésica em quilômetros via fórmula de Haversine.
 * 2. Heurística de Vizinho Mais Próximo (Nearest Neighbor) 100% offline.
 * 3. Degradação graciosa: paradas sem latitude/longitude conhecidas são mantidas ao final
 *    da rota, garantindo que nenhum pacote seja omitido.
 * 4. Ponto de extensão para troca futura por VRP/TSP avançado via ORS Optimization API.
 */
object RouteOptimizationHelper {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calcula a distância do grande círculo (em km) entre dois pares de coordenadas
     * usando a fórmula de Haversine.
     */
    fun haversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                sin(dLon / 2) * sin(dLon / 2) * cos(rLat1) * cos(rLat2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Reordena a lista de paradas com base na heurística do Vizinho Mais Próximo (Nearest Neighbor).
     *
     * @param startLat Latitude do ponto de partida (garagem/galpão ou posição GPS atual do motorista).
     * @param startLng Longitude do ponto de partida.
     * @param stops Lista de paradas a serem reordenadas.
     * @return Nova lista de paradas com [MasterRouteStop.stopOrder] reindexado de 1 a N.
     */
    fun optimizeStopsByNearestNeighbor(
        startLat: Double?,
        startLng: Double?,
        stops: List<MasterRouteStop>
    ): List<MasterRouteStop> {
        if (stops.size <= 1) {
            return stops.mapIndexed { index, stop -> stop.copy(stopOrder = index + 1) }
        }

        // 1. Separa paradas com coordenadas válidas daquelas que ainda não foram geocodificadas
        val (withCoords, withoutCoords) = stops.partition {
            it.latitude != null && it.longitude != null
        }

        // Se nenhuma tiver coordenadas, apenas reindexa mantendo a ordem original
        if (withCoords.isEmpty()) {
            return stops.mapIndexed { index, stop -> stop.copy(stopOrder = index + 1) }
        }

        val remaining = withCoords.toMutableList()
        val optimized = ArrayList<MasterRouteStop>(withCoords.size)

        var currentLat = startLat
        var currentLng = startLng

        // Se o ponto inicial não foi informado, a primeira parada com coordenadas serve como âncora
        if (currentLat == null || currentLng == null) {
            val firstStop = remaining.removeAt(0)
            optimized.add(firstStop)
            currentLat = firstStop.latitude!!.toDouble()
            currentLng = firstStop.longitude!!.toDouble()
        }

        // 2. Loop guloso do Vizinho Mais Próximo
        while (remaining.isNotEmpty()) {
            var bestIndex = -1
            var minDistance = Double.MAX_VALUE

            val cLat = currentLat ?: break
            val cLng = currentLng ?: break

            for (i in remaining.indices) {
                val candidate = remaining[i]
                val dist = haversineDistanceKm(
                    lat1 = cLat,
                    lon1 = cLng,
                    lat2 = candidate.latitude!!.toDouble(),
                    lon2 = candidate.longitude!!.toDouble()
                )
                if (dist < minDistance) {
                    minDistance = dist
                    bestIndex = i
                }
            }

            if (bestIndex != -1) {
                val nextStop = remaining.removeAt(bestIndex)
                optimized.add(nextStop)
                currentLat = nextStop.latitude!!.toDouble()
                currentLng = nextStop.longitude!!.toDouble()
            } else {
                // Caso excepcional de salvaguarda
                val fallbackStop = remaining.removeAt(0)
                optimized.add(fallbackStop)
                currentLat = fallbackStop.latitude!!.toDouble()
                currentLng = fallbackStop.longitude!!.toDouble()
            }
        }

        // 3. Concatena: paradas otimizadas por GPS primeiro, seguidas pelas paradas sem coordenadas
        val combined = optimized + withoutCoords

        // 4. Reindexa stopOrder sequencialmente de 1 a N
        return combined.mapIndexed { index, stop ->
            stop.copy(stopOrder = index + 1)
        }
    }

    /**
     * Calcula a distância geodésica acumulada de todo o trajeto em km (apenas entre paradas com coordenadas).
     */
    fun calculateTotalRouteDistanceKm(
        startLat: Double?,
        startLng: Double?,
        stops: List<MasterRouteStop>
    ): Double {
        val validStops = stops.filter { it.latitude != null && it.longitude != null }
        if (validStops.isEmpty()) return 0.0

        var totalDist = 0.0
        var prevLat = startLat ?: validStops.first().latitude!!.toDouble()
        var prevLng = startLng ?: validStops.first().longitude!!.toDouble()

        val startIndex = if (startLat != null && startLng != null) 0 else 1
        for (i in startIndex until validStops.size) {
            val stop = validStops[i]
            val curLat = stop.latitude!!.toDouble()
            val curLng = stop.longitude!!.toDouble()
            totalDist += haversineDistanceKm(prevLat, prevLng, curLat, curLng)
            prevLat = curLat
            prevLng = curLng
        }

        return totalDist
    }
}
