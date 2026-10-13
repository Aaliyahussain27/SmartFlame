package com.example.firesafe.data.repository

import com.example.firesafe.data.model.FireStation

interface FireStationRepository {
    suspend fun getNearestStation(latitude: Double, longitude: Double): FireStation
    suspend fun getAllStations(latitude: Double, longitude: Double): List<FireStation>
}
