package com.fernando.centraldomotorista.data.repository

import android.util.Log
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Repositório responsável pela Geocodificação de endereços e CEPs das paradas da rota Master.
 *
 * Blindagem de Cota e Resiliência (Prompt 9):
 * 1. Cache em memória thread-safe (inclusive para falhas/não-encontrados) garantindo que
 *    o mesmo endereço ou CEP NUNCA seja consultado duas vezes na mesma execução.
 * 2. Normalização estrita de CEPs e logradouros (remoção de acentos, pontuação e espaços redundantes).
 * 3. OpenRouteService Geocoding como provedor primário com fallback transparente para Nominatim (OSM).
 * 4. Rate-limiting cooperativo (throttling) com mutex para proteger cotas e evitar HTTP 429.
 */
open class GeocodingRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val orsApiKey: String? = null
) {
    private val tag = "GeocodingRepository"
    private val gson = Gson()
    private val requestMutex = Mutex()

    // Wrapper para permitir armazenar NotFound (coordenadas nulas) com segurança no ConcurrentHashMap
    private data class CacheEntry(val coordinates: Pair<Double, Double>?)

    // Cache em memória blindado: Chave Normalizada -> CacheEntry
    // Nota: CacheEntry com coordinates = null indica explicitamente que o endereço foi pesquisado e NÃO foi encontrado.
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()

    /**
     * Normaliza um CEP brasileiro para uma chave de 8 dígitos.
     */
    fun normalizeCep(rawCep: String?): String? {
        if (rawCep.isNullOrBlank()) return null
        val digits = rawCep.replace(Regex("[^0-9]"), "")
        return if (digits.length == 8) "cep:$digits" else null
    }

    /**
     * Normaliza uma string de endereço removendo diacríticos, pontuações e espaços extras.
     */
    fun normalizeAddress(address: String?): String {
        if (address.isNullOrBlank()) return ""
        val normalized = Normalizer.normalize(address.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return "addr:$normalized"
    }

    /**
     * Retorna a coordenada já em cache para a parada, se existente.
     */
    fun getCachedCoordinates(stop: MasterRouteStop): Pair<Double, Double>? {
        // Se a própria parada já possui coordenadas persistidas, retorna imediatamente
        if (stop.latitude != null && stop.longitude != null) {
            return Pair(stop.latitude.toDouble(), stop.longitude.toDouble())
        }

        val cepKey = normalizeCep(stop.cep)
        if (cepKey != null && memoryCache.containsKey(cepKey)) {
            return memoryCache[cepKey]?.coordinates
        }

        val addrKey = normalizeAddress(stop.fullAddress)
        if (addrKey.isNotBlank() && memoryCache.containsKey(addrKey)) {
            return memoryCache[addrKey]?.coordinates
        }

        return null
    }

    /**
     * Geocodifica uma parada de rota Master respeitando o cache local e rate limits.
     * Retorna Pair(latitude, longitude) ou null caso não encontrado.
     */
    open suspend fun geocodeStop(stop: MasterRouteStop): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        // 1. Se já possui coordenadas no modelo, não faz requisição de rede
        if (stop.latitude != null && stop.longitude != null) {
            val coords = Pair(stop.latitude.toDouble(), stop.longitude.toDouble())
            val entry = CacheEntry(coords)
            normalizeCep(stop.cep)?.let { memoryCache[it] = entry }
            val addrKey = normalizeAddress(stop.fullAddress)
            if (addrKey.isNotBlank()) memoryCache[addrKey] = entry
            return@withContext coords
        }

        val cepKey = normalizeCep(stop.cep)
        val addrKey = normalizeAddress(stop.fullAddress)

        // 2. Checagem de Cache Blindado (se já foi consultado com sucesso ou falha anterior)
        if (cepKey != null && memoryCache.containsKey(cepKey)) {
            return@withContext memoryCache[cepKey]?.coordinates
        }
        if (addrKey.isNotBlank() && memoryCache.containsKey(addrKey)) {
            return@withContext memoryCache[addrKey]?.coordinates
        }

        // 3. Monta a melhor consulta textual
        val query = when {
            stop.fullAddress.isNotBlank() -> stop.fullAddress
            !stop.street.isNullOrBlank() -> "${stop.street}, ${stop.number ?: ""}, ${stop.city ?: ""}, Brasil"
            !stop.cep.isNullOrBlank() -> stop.cep
            else -> null
        }

        if (query.isNullOrBlank()) {
            return@withContext null
        }

        // 4. Executa a geocodificação com rate-limiting cooperativo
        val result = geocodeQuery(query = query, cep = stop.cep)
        val cacheEntry = CacheEntry(result)

        // 5. Registra o resultado no cache blindado (tanto sucesso quanto nulo)
        if (cepKey != null) {
            memoryCache[cepKey] = cacheEntry
        }
        if (addrKey.isNotBlank()) {
            memoryCache[addrKey] = cacheEntry
        }

        result
    }

    /**
     * Geocodifica uma query textual, tentando OpenRouteService primeiro e depois Nominatim.
     */
    open suspend fun geocodeQuery(query: String, cep: String? = null): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        requestMutex.withLock {
            // Pequeno delay defensivo entre requisições consecutivas (500ms) para respeitar limites de taxa
            delay(500)

            // Tentativa 1: OpenRouteService (se chave configurada)
            if (!orsApiKey.isNullOrBlank()) {
                try {
                    val orsResult = queryOpenRouteService(query)
                    if (orsResult != null) {
                        return@withLock orsResult
                    }
                } catch (e: Exception) {
                    Log.w(tag, "ORS falhou para query '$query': ${e.message}. Tentando fallback Nominatim...")
                }
            }

            // Tentativa 2: Nominatim (OpenStreetMap) com User-Agent apropriado
            try {
                val nominatimResult = queryNominatim(query, cep)
                if (nominatimResult != null) {
                    return@withLock nominatimResult
                }
            } catch (e: Exception) {
                Log.w(tag, "Nominatim falhou para query '$query': ${e.message}")
            }

            null
        }
    }

    private fun queryOpenRouteService(query: String): Pair<Double, Double>? {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.openrouteservice.org/geocode/search?api_key=$orsApiKey&text=$encodedQuery&boundary.country=BRA&size=1"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyString = response.body?.string() ?: return null
            val json = gson.fromJson(bodyString, JsonObject::class.java)

            val features = json.getAsJsonArray("features")
            if (features == null || features.size() == 0) return null

            val firstFeature = features[0].asJsonObject
            val geometry = firstFeature.getAsJsonObject("geometry") ?: return null
            val coordinates = geometry.getAsJsonArray("coordinates") ?: return null

            // GeoJSON especifica coordenadas como [longitude, latitude]
            if (coordinates.size() >= 2) {
                val lng = coordinates[0].asDouble
                val lat = coordinates[1].asDouble
                return Pair(lat, lng)
            }
        }
        return null
    }

    private fun queryNominatim(query: String, cep: String?): Pair<Double, Double>? {
        // Se tiver CEP limpo de 8 dígitos, tenta busca por CEP ou texto
        val cleanCep = cep?.replace(Regex("[^0-9]"), "")
        val queryString = if (!cleanCep.isNullOrBlank() && cleanCep.length == 8) {
            "$cleanCep, Brasil"
        } else {
            "$query, Brasil"
        }

        val encodedQuery = java.net.URLEncoder.encode(queryString, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&countrycodes=br&limit=1"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "CentralDoMotorista/1.0 (Android; dev@centraldomotorista.com)")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyString = response.body?.string() ?: return null
            val jsonArray = gson.fromJson(bodyString, JsonArray::class.java)

            if (jsonArray != null && jsonArray.size() > 0) {
                val firstObj = jsonArray[0].asJsonObject
                val latStr = firstObj.get("lat")?.asString
                val lonStr = firstObj.get("lon")?.asString

                val lat = latStr?.toDoubleOrNull()
                val lon = lonStr?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    return Pair(lat, lon)
                }
            }
        }
        return null
    }

    /**
     * Limpa o cache em memória (usado primariamente em testes unitários).
     */
    fun clearCache() {
        memoryCache.clear()
    }

    /**
     * Insere manualmente no cache (usado em testes ou sincronização local).
     */
    fun putInCache(key: String, coordinates: Pair<Double, Double>?) {
        memoryCache[key] = CacheEntry(coordinates)
    }

    /**
     * Retorna a quantidade de entradas atualmente em cache.
     */
    fun getCacheSize(): Int = memoryCache.size
}
