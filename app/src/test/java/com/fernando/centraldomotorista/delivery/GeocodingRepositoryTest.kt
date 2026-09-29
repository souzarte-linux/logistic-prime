package com.fernando.centraldomotorista.delivery

import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.repository.GeocodingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class GeocodingRepositoryTest {

    private lateinit var geocodingRepository: GeocodingRepository

    @Before
    fun setUp() {
        geocodingRepository = GeocodingRepository()
        geocodingRepository.clearCache()
    }

    @Test
    fun testNormalizeCep_validAndInvalidCases() {
        assertEquals("cep:01310100", geocodingRepository.normalizeCep("01310-100"))
        assertEquals("cep:01310100", geocodingRepository.normalizeCep("01310100"))
        assertEquals("cep:01310100", geocodingRepository.normalizeCep("  01310-100  "))
        assertNull(geocodingRepository.normalizeCep("1234"))
        assertNull(geocodingRepository.normalizeCep(null))
        assertNull(geocodingRepository.normalizeCep(""))
    }

    @Test
    fun testNormalizeAddress_removesAccentsAndPunctuation() {
        val raw = "Avenida Paulista, nº 1578 - Bela Vista, São Paulo - SP!"
        val normalized = geocodingRepository.normalizeAddress(raw)
        assertEquals("addr:avenida paulista n 1578 bela vista sao paulo sp", normalized)
    }

    @Test
    fun testGeocodeStop_whenStopAlreadyHasCoordinates_returnsDirectlyAndCaches() = runBlocking {
        val stop = MasterRouteStop(
            id = "stop-1",
            barcode = "BR001",
            fullAddress = "Rua Teste, 100",
            cep = "01310-100",
            latitude = BigDecimal.valueOf(-23.561414),
            longitude = BigDecimal.valueOf(-46.655881)
        )

        val result = geocodingRepository.geocodeStop(stop)

        assertNotNull(result)
        assertEquals(-23.561414, result!!.first, 0.000001)
        assertEquals(-46.655881, result.second, 0.000001)

        // Deve ter armazenado no cache pelo CEP e pelo endereço
        assertEquals(result, geocodingRepository.getCachedCoordinates(stop))
    }

    @Test
    fun testBlindCache_avoidsRepeatedNetworkCallsForSameAddressOrCep() = runBlocking {
        // Pré-popula o cache com resultado null (simulando endereço não encontrado em tentativa anterior)
        val cepKey = "cep:04571010"
        geocodingRepository.putInCache(cepKey, null)

        val stop = MasterRouteStop(
            id = "stop-not-found",
            barcode = "NF001",
            fullAddress = "Endereço Desconhecido 9999",
            cep = "04571-010",
            latitude = null,
            longitude = null
        )

        // Não deve tentar a rede, deve retornar null do cache imediatamente
        val result = geocodingRepository.geocodeStop(stop)
        assertNull(result)

        // Agora pré-popula com sucesso
        val successCoords = Pair(-23.600000, -46.690000)
        geocodingRepository.putInCache(cepKey, successCoords)

        val resultCached = geocodingRepository.geocodeStop(stop)
        assertNotNull(resultCached)
        assertEquals(successCoords.first, resultCached!!.first, 0.00001)
        assertEquals(successCoords.second, resultCached.second, 0.00001)
    }
}
