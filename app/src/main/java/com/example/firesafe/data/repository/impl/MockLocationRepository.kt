package com.example.firesafe.data.repository.impl

import com.example.firesafe.data.model.LocationData
import com.example.firesafe.data.repository.LocationRepository
import kotlinx.coroutines.delay

class MockLocationRepository : LocationRepository {
    private var initialLocation: LocationData? = null

    override suspend fun getCurrentLocation(): LocationData {
        delay(1500) // Simulate GPS fix delay
        return LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            address = "850 Bryant St, San Francisco, CA 94103"
        )
    }

    override suspend fun getInitialLocation(): LocationData? {
        return initialLocation
    }

    override suspend fun saveInitialLocation(location: LocationData) {
        if (initialLocation == null) {
            initialLocation = location
        }
    }
}
