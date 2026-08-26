package com.example.firesafe.data.repository.impl

import com.example.firesafe.data.supabase
import com.example.firesafe.data.model.AlertConfirmation
import com.example.firesafe.data.model.SupabaseIncidentRow
import com.example.firesafe.data.repository.AlertRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class SupabaseAlertRepository : AlertRepository {

    override suspend fun dispatchAlert(
        latitude: Double,
        longitude: Double,
        address: String
    ): AlertConfirmation {
        val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy, hh:mm a", Locale.getDefault())
        val displayTimestamp = LocalDateTime.now().format(formatter)

        val row = SupabaseIncidentRow(
            lat = latitude,
            lng = longitude,
            address = address,
            status = "alert_sent",
            otpVerified = false,
            dispatchHubLabel = "Central Fire Dispatch Hub",
            reportedAtDisplay = displayTimestamp
        )

        // Insert and get back the generated row (id + created_at)
        val inserted = supabase
            .from("incidents")
            .insert(row) {
                select(Columns.ALL)
            }
            .decodeSingle<SupabaseIncidentRow>()

        val id = inserted.id ?: throw IllegalStateException("Database failed to return a valid incident ID")

        return AlertConfirmation(
            alertId = id,
            dispatchHub = inserted.dispatchHubLabel ?: "Central Fire Dispatch Hub",
            timestamp = inserted.reportedAtDisplay ?: displayTimestamp
        )
    }

    override suspend fun cancelAlert(alertId: String) {
        supabase
            .from("incidents")
            .update({ set("status", "cancelled") }) {
                filter { eq("id", alertId) }
            }
    }

    override suspend fun markOtpVerified(alertId: String, phoneNumber: String) {
        supabase
            .from("incidents")
            .update({
                set("otp_verified", true)
                set("reporter_phone", phoneNumber)
                set("status", "confirmed")
            }) {
                filter { eq("id", alertId) }
            }
    }
}
