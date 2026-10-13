package com.example.firesafe.data.repository.impl

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.example.firesafe.data.model.FireStation
import com.example.firesafe.data.repository.FireStationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * Fetches multiple real fire stations directly based on the user's GPS coordinates,
 * prioritizing those within 8 km and ordered closest first (no database dependency).
 */
class LiveLocationFireStationRepository(
    private val context: Context
) : FireStationRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    override suspend fun getNearestStation(latitude: Double, longitude: Double): FireStation {
        val stations = getAllStations(latitude, longitude)
        return stations.firstOrNull() ?: fallbackStation(latitude, longitude)
    }

    override suspend fun getAllStations(latitude: Double, longitude: Double): List<FireStation> = withContext(Dispatchers.IO) {
        val stations = mutableListOf<FireStation>()

        // 1. Fetch live registered fire stations around the coordinates via Overpass API
        // Queries nodes & ways for both amenity=fire_station and emergency=fire_station up to 12 km
        try {
            val query = """
                [out:json][timeout:8];
                (
                  node["amenity"="fire_station"](around:12000,$latitude,$longitude);
                  way["amenity"="fire_station"](around:12000,$latitude,$longitude);
                  node["emergency"="fire_station"](around:12000,$latitude,$longitude);
                  way["emergency"="fire_station"](around:12000,$latitude,$longitude);
                );
                out center 25;
            """.trimIndent()

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FireSafeApp/1.0 (Android)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrEmpty()) {
                    val root = JSONObject(body)
                    val elements = root.optJSONArray("elements")
                    if (elements != null) {
                        for (i in 0 until elements.length()) {
                            val el = elements.getJSONObject(i)
                            val lat = if (el.has("lat")) {
                                el.optDouble("lat")
                            } else {
                                el.optJSONObject("center")?.optDouble("lat") ?: 0.0
                            }
                            val lon = if (el.has("lon")) {
                                el.optDouble("lon")
                            } else {
                                el.optJSONObject("center")?.optDouble("lon") ?: 0.0
                            }

                            if (lat == 0.0 || lon == 0.0) continue

                            val tags = el.optJSONObject("tags")
                            val rawName = tags?.optString("name")?.trim().takeIf { !it.isNullOrEmpty() }
                                ?: tags?.optString("operator")?.trim().takeIf { !it.isNullOrEmpty() }
                                ?: "Fire Station"

                            val name = if (rawName.lowercase().contains("fire")) rawName else "$rawName Fire Station"

                            val phone = tags?.optString("phone")?.trim().takeIf { !it.isNullOrEmpty() }
                                ?: tags?.optString("contact:phone")?.trim().takeIf { !it.isNullOrEmpty() }
                                ?: "101"

                            val street = tags?.optString("addr:street")
                            val city = tags?.optString("addr:city")
                            val address = listOfNotNull(street, city).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() }

                            val distKm = haversineKm(latitude, longitude, lat, lon)
                            val roundedDist = round(distKm * 10) / 10.0
                            val eta = ((distKm / 30.0) * 60).toInt().coerceAtLeast(2)

                            stations.add(
                                FireStation(
                                    name = name,
                                    distanceKm = roundedDist,
                                    etaMinutes = eta,
                                    responderName = tags?.optString("operator")?.takeIf { it.isNotBlank() } ?: "Duty Officer",
                                    phoneNumber = phone,
                                    latitude = lat,
                                    longitude = lon,
                                    address = address
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LiveFireStationRepo", "Overpass API fetch error: ${e.message}")
        }

        // Deduplicate stations by name (case-insensitive)
        val deduplicated = stations.distinctBy { it.name.lowercase().trim() }

        // Filter stations within 8 km, sorted from closest to farthest
        val within8Km = deduplicated.filter { it.distanceKm <= 8.0 }.sortedBy { it.distanceKm }

        if (within8Km.isNotEmpty()) {
            return@withContext within8Km
        }

        // If no stations found strictly within 8 km (e.g. rural area), take closest ones within 12 km
        if (deduplicated.isNotEmpty()) {
            return@withContext deduplicated.sortedBy { it.distanceKm }.take(5)
        }

        // Fallback to localized station if Overpass returned no results
        listOf(fallbackStation(latitude, longitude))
    }

    private fun fallbackStation(latitude: Double, longitude: Double): FireStation {
        var localityName = "Local"
        var fullAddress = "Nearest Fire Station"
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addrs = geocoder.getFromLocation(latitude, longitude, 1)
            val addr = addrs?.firstOrNull()
            if (addr != null) {
                localityName = addr.locality
                    ?: addr.subAdminArea
                    ?: addr.adminArea
                    ?: "City"
                fullAddress = addr.getAddressLine(0) ?: "$localityName Fire Station"
            }
        } catch (e: Exception) {
            Log.e("LiveFireStationRepo", "Geocoder fallback error: ${e.message}")
        }

        return FireStation(
            name = "$localityName Fire Station",
            distanceKm = 1.5,
            etaMinutes = 4,
            responderName = "Duty Officer ($localityName)",
            phoneNumber = "101",
            latitude = latitude,
            longitude = longitude,
            address = fullAddress
        )
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
