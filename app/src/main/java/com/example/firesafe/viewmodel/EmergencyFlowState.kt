package com.example.firesafe.viewmodel

import com.example.firesafe.data.model.AlertConfirmation
import com.example.firesafe.data.model.FireStation
import com.example.firesafe.data.model.LocationData

sealed class EmergencyFlowState {
    object Idle : EmergencyFlowState()
    object DetectingLocation : EmergencyFlowState()

    data class LocationResolved(
        val location: LocationData,
        val editedAddress: String,
        val permissionDenied: Boolean = false,
        val isDispatching: Boolean = false,
        val errorMessage: String? = null
    ) : EmergencyFlowState()

    data class AlertSent(
        val location: LocationData,
        val alertConfirmation: AlertConfirmation,
        val fireStation: FireStation,
        val isCancelling: Boolean = false
    ) : EmergencyFlowState()

    data class AwaitingPhoto(
        val location: LocationData,
        val alertConfirmation: AlertConfirmation,
        val fireStation: FireStation,
        val photoPath: String? = null
    ) : EmergencyFlowState()

    data class AwaitingOTP(
        val location: LocationData,
        val alertConfirmation: AlertConfirmation,
        val fireStation: FireStation,
        val photoPath: String,
        val phoneNumber: String,
        val phoneError: String? = null,
        val isSendingOtp: Boolean = false,
        val otpError: String? = null,
        val isVerifying: Boolean = false,
        val countdownSeconds: Int = 30
    ) : EmergencyFlowState()

    data class Confirmed(
        val location: LocationData,
        val alertConfirmation: AlertConfirmation,
        val fireStation: FireStation,
        val photoPath: String,
        val verifiedPhoneNumber: String
    ) : EmergencyFlowState()
}
