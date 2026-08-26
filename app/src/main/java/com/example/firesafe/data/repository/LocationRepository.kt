package com.example.firesafe.data.repository

import com.example.firesafe.data.model.LocationData

interface LocationRepository {
    suspend fun getCurrentLocation(): LocationData
    suspend fun getInitialLocation(): LocationData?
    suspend fun saveInitialLocation(location: LocationData)
}
