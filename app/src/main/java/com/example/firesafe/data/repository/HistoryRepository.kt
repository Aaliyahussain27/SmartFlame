package com.example.firesafe.data.repository

import com.example.firesafe.data.model.HistoryItem

interface HistoryRepository {
    /** Alerts sent from this phone, newest first, read from the database. */
    suspend fun getHistory(): List<HistoryItem>
}
