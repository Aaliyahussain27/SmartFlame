package com.example.firesafe.data.repository.impl

import android.content.Context
import com.example.firesafe.data.local.AlertHistoryStore
import com.example.firesafe.data.model.HistoryItem
import com.example.firesafe.data.model.SupabaseIncidentRow
import com.example.firesafe.data.repository.HistoryRepository
import com.example.firesafe.data.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class SupabaseHistoryRepository(context: Context) : HistoryRepository {

    private val store = AlertHistoryStore(context)

    override suspend fun getHistory(): List<HistoryItem> {
        val ids = store.ids()
        if (ids.isEmpty()) return emptyList()

        val rows = supabase
            .from("incidents")
            .select {
                filter { isIn("id", ids) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<SupabaseIncidentRow>()

        return rows.map { it.toHistoryItem() }
    }

    private fun SupabaseIncidentRow.toHistoryItem(): HistoryItem {
        val local = try {
            OffsetDateTime.parse(createdAt).atZoneSameInstant(ZoneId.systemDefault())
        } catch (e: Exception) {
            null
        }
        val date = local?.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault()))
            ?: (reportedAtDisplay ?: "")
        val time = local?.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())) ?: ""

        // Same status words the dashboard uses
        val (label, good) = when (status) {
            "resolved" -> "Resolved & Handled" to true
            "monitoring" -> "Fire Under Control" to true
            "containment" -> "Fire Being Contained" to true
            "on_site" -> "Responders On Site" to true
            "en_route" -> "Responders En Route" to true
            "confirmed" -> "Verified & Forwarded" to true
            "cancelled" -> "Cancelled by You" to false
            else -> "Alert Sent - Not Verified" to false
        }

        return HistoryItem(
            alertId = "AL-" + (id ?: "").take(6).uppercase(),
            date = date,
            time = time,
            address = address,
            status = label,
            isResolved = good
        )
    }
}
