package com.example.firesafe.data.repository.impl

import com.example.firesafe.data.supabase
import com.example.firesafe.data.model.FireStation
import com.example.firesafe.data.model.SupabaseFireStationRow
import com.example.firesafe.data.repository.FireStationRepository
import io.github.jan.supabase.postgrest.from
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SupabaseFireStationRepository : FireStationRepository {

    override suspend fun getNearestStation(latitude: Double, longitude: Double): FireStation {
        val stations = supabase
            .from("fire_stations")
            .select()
            .decodeList<SupabaseFireStationRow>()

        if (stations.isEmpty()) {
            // Graceful fallback if the fire_stations table has no data yet
            return FireStation(
                name = "Nearest Fire Station",
                distanceKm = 0.0,
                etaMinutes = 5,
                responderName = "Duty Officer",
                phoneNumber = "101"
            )
        }

        // Pick closest station using Haversine formula
        val nearest = stations.minByOrNull { haversineKm(latitude, longitude, it.lat, it.lng) }!!
        val distanceKm = haversineKm(latitude, longitude, nearest.lat, nearest.lng)
        // Rough ETA: assume 30 km/h average speed through city traffic
        val etaMinutes = ((distanceKm / 30.0) * 60).toInt().coerceAtLeast(2)

        val roundedDistance = kotlin.math.round(distanceKm * 10) / 10.0

        return FireStation(
            name = nearest.name,
            distanceKm = roundedDistance,
            etaMinutes = etaMinutes,
            responderName = nearest.responderName ?: "Duty Officer",
            phoneNumber = nearest.phoneNumber ?: "101"
        )
    }

    /**
     * Haversine formula — returns distance in kilometres between two lat/lng points.
     */
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
