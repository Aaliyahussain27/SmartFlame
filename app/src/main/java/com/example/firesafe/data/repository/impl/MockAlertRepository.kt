package com.example.firesafe.data.repository.impl

import com.example.firesafe.data.model.AlertConfirmation
import com.example.firesafe.data.repository.AlertRepository
import kotlinx.coroutines.delay

class MockAlertRepository : AlertRepository {
    override suspend fun dispatchAlert(latitude: Double, longitude: Double, address: String): AlertConfirmation {
        delay(1000)
        return AlertConfirmation(
            alertId = "AL-8893-X",
            dispatchHub = "Bay Area Fire Dispatch Center Hub 4",
            timestamp = "July 15, 2026, 01:00 AM"
        )
    }

    override suspend fun cancelAlert(alertId: String) {
        delay(1000)
    }

    override suspend fun markOtpVerified(alertId: String, phoneNumber: String) {
        delay(200)
    }
}
