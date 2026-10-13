package com.example.firesafe.data.model

data class FireStation(
    val name: String,
    val distanceKm: Double,
    val etaMinutes: Int,
    val responderName: String,
    val phoneNumber: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null
)
