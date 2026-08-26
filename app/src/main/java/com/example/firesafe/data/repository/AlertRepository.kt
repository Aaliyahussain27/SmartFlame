package com.example.firesafe.data.repository

import com.example.firesafe.data.model.AlertConfirmation

interface AlertRepository {
    suspend fun dispatchAlert(latitude: Double, longitude: Double, address: String): AlertConfirmation
    suspend fun cancelAlert(alertId: String)
    /** Called after OTP is verified — persists reporter phone and marks otp_verified = true. */
    suspend fun markOtpVerified(alertId: String, phoneNumber: String)
}
