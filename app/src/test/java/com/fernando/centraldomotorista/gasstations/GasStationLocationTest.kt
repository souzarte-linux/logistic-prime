package com.fernando.centraldomotorista.gasstations

import com.fernando.centraldomotorista.data.remote.api.OverpassApi
import com.fernando.centraldomotorista.data.remote.dto.NearbyGasStation
import com.fernando.centraldomotorista.data.remote.dto.OverpassResponseDto
import com.fernando.centraldomotorista.util.LocationHelper
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GasStationLocationTest {

    @Test
    fun testNormalizeBrand() {
        assertEquals("Shell", LocationHelper.normalizeBrand("Shell", "Posto Central", null))
        assertEquals("Shell", LocationHelper.normalizeBrand(null, "Auto Posto Shell Conveniência", null))
        assertEquals("Shell", LocationHelper.normalizeBrand(null, "Posto Raízen", "Raizen Combustíveis"))
        assertEquals("Ipiranga", LocationHelper.normalizeBrand("Ipiranga", "Posto AmPm", null))
        assertEquals("Petrobras / Vibra", LocationHelper.normalizeBrand("BR", "Posto Petrobras", "Vibra Energia"))
        assertEquals("Petrobras / Vibra", LocationHelper.normalizeBrand(null, "Posto Petrobrás Estrada", null))
        assertEquals("Ale", LocationHelper.normalizeBrand("ALE", "Posto Ale 24h", null))
        assertEquals("Texaco", LocationHelper.normalizeBrand("Texaco", null, null))
        assertEquals("Boxter", LocationHelper.normalizeBrand("Boxter", "Posto Boxter", null))
        assertEquals("TotalEnergies", LocationHelper.normalizeBrand("Total", null, null))
        assertEquals("Bandeira Branca", LocationHelper.normalizeBrand(null, null, null))
        assertEquals("Bandeira Branca", LocationHelper.normalizeBrand("", "Posto do Zé", ""))
    }

    @Test
    fun testDetectFuelTypes() {
        // Sem tags específicas -> Deve conter padrão brasileiro (Gasolina Comum, Gasolina Aditivada, Etanol)
        val defaultFuels = LocationHelper.detectFuelTypes(emptyMap())
        assertTrue("Deve conter Gasolina Comum", defaultFuels.contains("Gasolina Comum"))
        assertTrue("Deve conter Etanol", defaultFuels.contains("Etanol"))

        // Com tags específicas
        val dieselAndGnvTags = mapOf(
            "fuel:diesel" to "yes",
            "fuel:cng" to "yes"
        )
        val fuels = LocationHelper.detectFuelTypes(dieselAndGnvTags)
        assertTrue("Deve conter Diesel", fuels.contains("Diesel"))
        assertTrue("Deve conter GNV", fuels.contains("GNV"))
    }

    @Test
    fun testCalculateDistanceMeters() {
        // Mesma coordenada -> distância 0
        val zeroDist = LocationHelper.calculateDistanceMeters(-12.9716, -38.4632, -12.9716, -38.4632)
        assertEquals(0.0f, zeroDist, 1.0f)

        // Distância entre Farol da Barra (-13.0104, -38.5327) e Pelourinho (-12.9716, -38.5081) em Salvador (~5.1km)
        val dist = LocationHelper.calculateDistanceMeters(-13.0104, -38.5327, -12.9716, -38.5081)
        assertTrue("Distância deve ser em torno de 5000 a 5300m, obtido: $dist", dist in 4900f..5500f)
    }

    @Test
    fun testParseOverpassJsonAndMapping() {
        val sampleJson = """
        {
            "version": 0.6,
            "generator": "Overpass API",
            "elements": [
                {
                    "type": "node",
                    "id": 1001,
                    "lat": -12.9720,
                    "lon": -38.4640,
                    "tags": {
                        "name": "Posto Shell Paralela",
                        "brand": "Shell",
                        "addr:street": "Avenida Paralela",
                        "addr:housenumber": "500",
                        "addr:city": "Salvador",
                        "addr:state": "BA",
                        "fuel:gasoline": "yes",
                        "fuel:ethanol": "yes",
                        "fuel:diesel": "yes"
                    }
                },
                {
                    "type": "way",
                    "id": 2002,
                    "center": {
                        "lat": -12.9750,
                        "lon": -38.4680
                    },
                    "tags": {
                        "name": "Posto Ipiranga ACM",
                        "brand": "Ipiranga",
                        "addr:street": "Avenida ACM",
                        "addr:city": "Salvador",
                        "addr:state": "BA",
                        "fuel:cng": "yes"
                    }
                }
            ]
        }
        """.trimIndent()

        val dto = Gson().fromJson(sampleJson, OverpassResponseDto::class.java)
        assertNotNull(dto.elements)
        assertEquals(2, dto.elements?.size)

        val userLat = -12.971691
        val userLon = -38.463297

        val mappedStations = dto.elements!!.mapNotNull { element ->
            val lat = element.latitude ?: return@mapNotNull null
            val lon = element.longitude ?: return@mapNotNull null
            val tags = element.tags ?: emptyMap()

            val rawName = tags["name"] ?: tags["operator"] ?: tags["brand"] ?: "Posto de Combustível"
            val brand = LocationHelper.normalizeBrand(tags["brand"], rawName, tags["operator"])
            val street = tags["addr:street"]
            val number = tags["addr:housenumber"]
            val neighborhood = tags["addr:suburb"] ?: tags["addr:district"]
            val city = tags["addr:city"]
            val state = tags["addr:state"]
            val cep = tags["addr:postcode"]
            val distance = LocationHelper.calculateDistanceMeters(userLat, userLon, lat, lon)
            val fuelTypes = LocationHelper.detectFuelTypes(tags)

            NearbyGasStation(
                id = "${element.type}_${element.id}",
                name = rawName,
                brand = brand,
                street = street,
                number = number,
                neighborhood = neighborhood,
                city = city,
                state = state,
                cep = cep,
                fullAddress = street ?: "Sem endereço",
                latitude = lat,
                longitude = lon,
                distanceMeters = distance,
                fuelTypes = fuelTypes
            )
        }.sortedBy { it.distanceMeters }

        assertEquals(2, mappedStations.size)
        // Primeiro posto deve ser o Posto Shell (mais próximo de -12.971691, -38.463297)
        assertEquals("Posto Shell Paralela", mappedStations[0].name)
        assertEquals("Shell", mappedStations[0].brand)
        assertTrue(mappedStations[0].fuelTypes.contains("Diesel"))

        // Segundo posto deve ser o Posto Ipiranga (via elemento way com center)
        assertEquals("Posto Ipiranga ACM", mappedStations[1].name)
        assertEquals("Ipiranga", mappedStations[1].brand)
        assertTrue(mappedStations[1].fuelTypes.contains("GNV"))
        assertTrue(mappedStations[1].distanceMeters > mappedStations[0].distanceMeters)
    }

    @Test
    fun testLiveOverpassApiCall() = runBlocking {
        // Validação real chamando o OverpassApi da aplicação com coordenadas reais de Salvador
        val userLat = -12.971691
        val userLon = -38.463297

        val query = """
            [out:json][timeout:25];
            (
              node["amenity"="fuel"](around:5000,$userLat,$userLon);
              way["amenity"="fuel"](around:5000,$userLat,$userLon);
            );
            out center tags;
        """.trimIndent()

        val response = OverpassApi.instance.getNearbyGasStations(query)
        assertNotNull("A resposta não deve ser nula", response)
        assertNotNull("Elementos não devem ser nulos", response.elements)
        assertFalse("Deve retornar postos próximos no raio de 5km", response.elements!!.isEmpty())

        println("Teste Live Overpass: Encontrados ${response.elements!!.size} postos próximos!")
        val firstStation = response.elements!!.first()
        println("Primeiro posto retornado: id=${firstStation.id}, tipo=${firstStation.type}, tags=${firstStation.tags}")
    }
}
