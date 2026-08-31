package com.fernando.centraldomotorista.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val cancellationTokenSource = CancellationTokenSource()

            val highAccuracyLocation = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            if (highAccuracyLocation != null) {
                return@withContext highAccuracyLocation
            }

            // Fallback para lastLocation do FusedLocationProviderClient
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                return@withContext lastLocation
            }
        } catch (e: Exception) {
            // Continua para fallback nativo
        }

        // Fallback usando LocationManager nativo do Android
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val netLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                return@withContext when {
                    gpsLocation != null && netLocation != null -> {
                        if (gpsLocation.time > netLocation.time) gpsLocation else netLocation
                    }
                    gpsLocation != null -> gpsLocation
                    else -> netLocation
                }
            }
        } catch (e: Exception) {
            // Ignora e retorna null
        }

        null
    }

    suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): Address? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("pt", "BR"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        cont.resume(addresses.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun normalizeBrand(rawBrand: String?, rawName: String?, rawOperator: String?): String {
        val combined = "${rawBrand.orEmpty()} ${rawName.orEmpty()} ${rawOperator.orEmpty()}".lowercase()
        return when {
            combined.contains("shell") -> "Shell"
            combined.contains("ipiranga") -> "Ipiranga"
            combined.contains("petrobras") || combined.contains("br ") || combined.contains("vibra") || combined.contains("petrobrás") -> "Petrobras / Vibra"
            combined.contains("ale") -> "Ale"
            combined.contains("texaco") -> "Texaco"
            combined.contains("repsol") -> "Repsol"
            combined.contains("boxter") -> "Boxter"
            combined.contains("raizen") || combined.contains("raízen") -> "Shell"
            combined.contains("total") || combined.contains("totalenergies") -> "TotalEnergies"
            rawBrand?.isNotBlank() == true -> rawBrand.trim()
            else -> "Bandeira Branca"
        }
    }

    fun detectFuelTypes(tags: Map<String, String>): List<String> {
        val fuels = mutableListOf<String>()
        val hasGasoline = tags["fuel:gasoline"] == "yes" || tags["fuel:octane_95"] == "yes" || tags["fuel:octane_98"] == "yes" || tags.isEmpty()
        val hasEthanol = tags["fuel:ethanol"] == "yes" || tags["fuel:e10"] == "yes" || tags["fuel:e85"] == "yes" || tags.isEmpty()
        val hasDiesel = tags["fuel:diesel"] == "yes" || tags["fuel:diesel:class2"] == "yes"
        val hasGnv = tags["fuel:cng"] == "yes" || tags["fuel:lpg"] == "yes" || tags["fuel:gnv"] == "yes"

        if (hasGasoline) {
            fuels.add("Gasolina Comum")
            fuels.add("Gasolina Aditivada")
        }
        if (hasEthanol) fuels.add("Etanol")
        if (hasDiesel) fuels.add("Diesel")
        if (hasGnv) fuels.add("GNV")

        if (fuels.isEmpty()) {
            fuels.addAll(listOf("Gasolina Comum", "Etanol"))
        }
        return fuels.distinct()
    }
}
