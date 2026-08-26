package com.example.firesafe.data.repository.impl

import android.content.Context
import com.example.firesafe.data.model.LocationData
import com.example.firesafe.data.repository.LocationRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GPSLocationRepository(private val context: Context) : LocationRepository {
    
    private val sharedPreferences = context.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)

    @android.annotation.SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationData = suspendCancellableCoroutine { continuation ->
        try {
            // Ensure runtime permission check
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(SecurityException("ACCESS_FINE_LOCATION permission not granted")))
                }
                return@suspendCancellableCoroutine
            }

            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val address = try {
                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            addresses?.firstOrNull()?.getAddressLine(0) ?: "${location.latitude}, ${location.longitude}"
                        } catch (e: Exception) {
                            "${location.latitude}, ${location.longitude}"
                        }
                        if (continuation.isActive) {
                            continuation.resume(LocationData(location.latitude, location.longitude, address), onCancellation = null)
                        }
                    } else {
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.failure(Exception("GPS returned null location")))
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(exception))
                    }
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWith(Result.failure(e))
            }
        }
    }

    override suspend fun getInitialLocation(): LocationData? {
        val lat = sharedPreferences.getFloat("init_lat", Float.NaN)
        val lng = sharedPreferences.getFloat("init_lng", Float.NaN)
        val address = sharedPreferences.getString("init_address", null)

        return if (!lat.isNaN() && !lng.isNaN() && address != null) {
            LocationData(lat.toDouble(), lng.toDouble(), address)
        } else {
            null
        }
    }

    override suspend fun saveInitialLocation(location: LocationData) {
        // Only save if it hasn't been set yet
        if (!sharedPreferences.contains("init_lat")) {
            sharedPreferences.edit()
                .putFloat("init_lat", location.latitude.toFloat())
                .putFloat("init_lng", location.longitude.toFloat())
                .putString("init_address", location.address)
                .apply()
        }
    }
}
