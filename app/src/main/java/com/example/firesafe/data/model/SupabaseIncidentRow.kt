package com.example.firesafe.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a row in the `incidents` Supabase table.
 * Used for insert and select operations — not used directly in UI.
 * Nullable fields are excluded from INSERT (let DB defaults apply).
 */
@Serializable
data class SupabaseIncidentRow(
    val id: String? = null,                         // UUID, DB-generated on insert
    val lat: Double,
    val lng: Double,
    val address: String,
    val status: String = "alert_sent",              // matches DB check constraint
    @SerialName("otp_verified")
    val otpVerified: Boolean = false,
    @SerialName("reporter_phone")
    val reporterPhone: String? = null,
    @SerialName("dispatch_hub_label")
    val dispatchHubLabel: String? = null,
    @SerialName("nearest_station_id")
    val nearestStationId: String? = null,
    @SerialName("eta_minutes")
    val etaMinutes: Int? = null,
    @SerialName("reported_at_display")
    val reportedAtDisplay: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,                  // timestamptz, DB-generated
)
