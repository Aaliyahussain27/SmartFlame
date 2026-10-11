package com.example.firesafe.data.local

import android.content.Context

/**
 * Remembers the IDs of the alerts that were sent from THIS phone, so the History tab can
 * ask the database for exactly those alerts (the app has no user login yet).
 */
class AlertHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("AlertHistory", Context.MODE_PRIVATE)

    /** Newest first. */
    fun ids(): List<String> =
        prefs.getString(KEY, "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    fun add(id: String) {
        val updated = (listOf(id) + ids().filter { it != id }).take(MAX_ITEMS)
        prefs.edit().putString(KEY, updated.joinToString(",")).apply()
    }

    private companion object {
        const val KEY = "alert_ids"
        const val MAX_ITEMS = 50
    }
}
