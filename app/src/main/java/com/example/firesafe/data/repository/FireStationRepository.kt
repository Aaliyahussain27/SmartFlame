package com.example.firesafe.data.repository

import com.example.firesafe.data.model.FireStation

interface FireStationRepository {
    suspend fun getNearestStation(latitude: Double, longitude: Double): FireStation

    /** Every station in the database, nearest first, with distance/ETA from the given point. */
    suspend fun getAllStations(latitude: Double, longitude: Double): List<FireStation>
}
