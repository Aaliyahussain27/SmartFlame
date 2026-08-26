package com.example.firesafe.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a row from the `fire_stations` Supabase table.
 * Used only for DB reads — mapped to the UI [FireStation] model.
 */
@Serializable
data class SupabaseFireStationRow(
    val id: String,
    val name: String,
    val address: String? = null,
    val lat: Double,
    val lng: Double,
    @SerialName("responder_name")
    val responderName: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
)
