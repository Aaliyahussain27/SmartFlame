package com.example.firesafe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firesafe.data.repository.*
import com.example.firesafe.data.repository.impl.*
import com.example.firesafe.data.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class EmergencyEvent {
    data class ShowToast(val message: String) : EmergencyEvent()
}

class EmergencyViewModel(
    private val locationRepository: LocationRepository = MockLocationRepository(),
    private val fireStationRepository: FireStationRepository = SupabaseFireStationRepository(),
    private val alertRepository: AlertRepository = SupabaseAlertRepository(),
    private val otpRepository: OtpRepository = MockOtpRepository()
) : ViewModel() {

    private companion object {
        val DEFAULT_FALLBACK_LOCATION = LocationData(28.6139, 77.2090, "New Delhi, India")
    }

    private val _uiState = MutableStateFlow<EmergencyFlowState>(EmergencyFlowState.Idle)
    val uiState: StateFlow<EmergencyFlowState> = _uiState.asStateFlow()

    private val _events = Channel<EmergencyEvent>()
    val events = _events.receiveAsFlow()

    private var otpTimerJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        otpTimerJob?.cancel()
    }

    fun startEmergency() {
        _uiState.value = EmergencyFlowState.DetectingLocation
    }

    fun detectLocation() {
        viewModelScope.launch {
            try {
                val location = locationRepository.getCurrentLocation()
                locationRepository.saveInitialLocation(location) // Save initially detected location
                
                _uiState.value = EmergencyFlowState.LocationResolved(
                    location = location,
                    editedAddress = location.address,
                    permissionDenied = false
                )
            } catch (e: SecurityException) {
                val fallback = locationRepository.getInitialLocation() ?: DEFAULT_FALLBACK_LOCATION
                _uiState.value = EmergencyFlowState.LocationResolved(
                    location = fallback,
                    editedAddress = fallback.address,
                    permissionDenied = true
                )
            } catch (e: Exception) {
                val fallback = locationRepository.getInitialLocation() ?: DEFAULT_FALLBACK_LOCATION
                _uiState.value = EmergencyFlowState.LocationResolved(
                    location = fallback,
                    editedAddress = fallback.address,
                    permissionDenied = false
                )
            }
        }
    }

    fun updateEditedAddress(newAddress: String) {
        _uiState.update { current ->
            if (current is EmergencyFlowState.LocationResolved && current.editedAddress != newAddress) {
                current.copy(editedAddress = newAddress)
            } else {
                current
            }
        }
    }

    fun resolveLocationAndDispatch(onResolved: () -> Unit) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val (location, address) = when (currentState) {
                is EmergencyFlowState.LocationResolved -> Pair(currentState.location, currentState.editedAddress)
                else -> {
                    try {
                        val loc = locationRepository.getCurrentLocation()
                        locationRepository.saveInitialLocation(loc)
                        Pair(loc, loc.address)
                    } catch (e: Exception) {
                        val fallback = locationRepository.getInitialLocation() ?: DEFAULT_FALLBACK_LOCATION
                        Pair(fallback, fallback.address)
                    }
                }
            }

            try {
                if (currentState is EmergencyFlowState.LocationResolved) {
                    _uiState.value = currentState.copy(isDispatching = true)
                }

                // 2. Dispatch Alert to Central Hub
                val alertConfirm = alertRepository.dispatchAlert(
                    location.latitude,
                    location.longitude,
                    address
                )
                
                // 3. Find Nearest Fire Station
                val station = fireStationRepository.getNearestStation(
                    location.latitude,
                    location.longitude
                )
                
                _uiState.value = EmergencyFlowState.AlertSent(
                    location = location.copy(address = address),
                    alertConfirmation = alertConfirm,
                    fireStation = station
                )
                onResolved()
            } catch (e: Exception) {
                // Emit an error event or handle the failure safely
                _events.send(EmergencyEvent.ShowToast("Failed to dispatch alert. Please check your connection and try again."))
                _uiState.value = EmergencyFlowState.Idle
            }
        }
    }

    fun showCancelConfirmationDialog(show: Boolean) {
        _uiState.update { currentState ->
            if (currentState is EmergencyFlowState.AlertSent) {
                currentState.copy(isCancelling = show)
            } else {
                currentState
            }
        }
    }

    fun cancelAlert(onCancelled: () -> Unit) {
        val currentState = _uiState.value
        if (currentState is EmergencyFlowState.AlertSent) {
            viewModelScope.launch {
                try {
                    alertRepository.cancelAlert(currentState.alertConfirmation.alertId)
                    _uiState.value = EmergencyFlowState.Idle
                    onCancelled()
                } catch (e: Exception) {
                    _events.send(EmergencyEvent.ShowToast("Failed to cancel alert. Please try again."))
                }
            }
        }
    }

    fun proceedToPhoto() {
        val currentState = _uiState.value
        if (currentState is EmergencyFlowState.AlertSent) {
            _uiState.value = EmergencyFlowState.AwaitingPhoto(
                location = currentState.location,
                alertConfirmation = currentState.alertConfirmation,
                fireStation = currentState.fireStation
            )
        }
    }

    fun savePhotoAndProceed(photoPath: String, defaultPhoneNum: String = "") {
        val currentState = _uiState.value
        val (location, alertConfirm, station) = when (currentState) {
            is EmergencyFlowState.AwaitingPhoto -> Triple(currentState.location, currentState.alertConfirmation, currentState.fireStation)
            is EmergencyFlowState.AlertSent -> Triple(currentState.location, currentState.alertConfirmation, currentState.fireStation)
            else -> return
        }

        _uiState.value = EmergencyFlowState.AwaitingOTP(
            location = location,
            alertConfirmation = alertConfirm,
            fireStation = station,
            photoPath = photoPath,
            phoneNumber = defaultPhoneNum,
            phoneError = null,
            isSendingOtp = false,
            otpError = null,
            isVerifying = false,
            countdownSeconds = 0
        )
    }

    fun startOtpCountdown() {
        otpTimerJob?.cancel()
        otpTimerJob = viewModelScope.launch {
            while (true) {
                val state = _uiState.value
                if (state is EmergencyFlowState.AwaitingOTP) {
                    if (state.countdownSeconds > 0) {
                        delay(1000)
                        _uiState.update { current ->
                            if (current is EmergencyFlowState.AwaitingOTP) {
                                current.copy(countdownSeconds = current.countdownSeconds - 1)
                            } else {
                                current
                            }
                        }
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
        }
    }

    fun sendOtp(phoneNumber: String) {
        val trimmed = phoneNumber.trim()
        val digitsOnly = trimmed.removePrefix("+91").removePrefix("91").filter { it.isDigit() }
        
        // Indian mobile validation: starts with 6-9 and exactly 10 digits
        val isValid = digitsOnly.length == 10 && digitsOnly.firstOrNull() in '6'..'9'
        
        if (!isValid) {
            _uiState.update { current ->
                if (current is EmergencyFlowState.AwaitingOTP) {
                    current.copy(phoneError = "Invalid phone number. Must be a 10-digit Indian number starting with 6-9.")
                } else {
                    current
                }
            }
            return
        }

        _uiState.update { current ->
            if (current is EmergencyFlowState.AwaitingOTP) {
                current.copy(phoneError = null, isSendingOtp = true)
            } else {
                current
            }
        }

        viewModelScope.launch {
            try {
                val success = otpRepository.sendOtp(trimmed)
                _uiState.update { current ->
                    if (current is EmergencyFlowState.AwaitingOTP) {
                        current.copy(isSendingOtp = false)
                    } else {
                        current
                    }
                }
                
                if (success) {
                    _uiState.update { current ->
                        if (current is EmergencyFlowState.AwaitingOTP) {
                            current.copy(countdownSeconds = 30)
                        } else {
                            current
                        }
                    }
                    startOtpCountdown()
                } else {
                    _events.send(EmergencyEvent.ShowToast("Couldn't send OTP. Please check your number and try again."))
                }
            } catch (e: Exception) {
                _uiState.update { current ->
                    if (current is EmergencyFlowState.AwaitingOTP) {
                        current.copy(isSendingOtp = false)
                    } else current
                }
                _events.send(EmergencyEvent.ShowToast("Network error while sending OTP."))
            }
        }
    }

    fun resendOtp() {
        val state = _uiState.value
        if (state is EmergencyFlowState.AwaitingOTP) {
            sendOtp(state.phoneNumber)
        }
    }

    fun updatePhoneNumber(newNumber: String) {
        _uiState.update { current ->
            if (current is EmergencyFlowState.AwaitingOTP && current.phoneNumber != newNumber) {
                current.copy(phoneNumber = newNumber, phoneError = null)
            } else {
                current
            }
        }
    }

    fun verifyOtp(otpCode: String, onVerified: () -> Unit) {
        val state = _uiState.value
        if (state is EmergencyFlowState.AwaitingOTP) {
            _uiState.update { current ->
                if (current is EmergencyFlowState.AwaitingOTP) {
                    current.copy(isVerifying = true, otpError = null)
                } else {
                    current
                }
            }
            viewModelScope.launch {
                try {
                    val success = otpRepository.verifyOtp(state.phoneNumber, otpCode)
                    if (success) {
                        otpTimerJob?.cancel()
                        
                        alertRepository.markOtpVerified(
                            alertId = state.alertConfirmation.alertId,
                            phoneNumber = state.phoneNumber
                        )
                        
                        _uiState.value = EmergencyFlowState.Confirmed(
                            location = state.location,
                            alertConfirmation = state.alertConfirmation,
                            fireStation = state.fireStation,
                            photoPath = state.photoPath,
                            verifiedPhoneNumber = state.phoneNumber
                        )
                        onVerified()
                    } else {
                        _uiState.update { current ->
                            if (current is EmergencyFlowState.AwaitingOTP) {
                                current.copy(isVerifying = false, otpError = "Incorrect verification code. Please try again.")
                            } else {
                                current
                            }
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { current ->
                        if (current is EmergencyFlowState.AwaitingOTP) {
                            current.copy(isVerifying = false, otpError = "Network error verifying OTP. Please check connection.")
                        } else {
                            current
                        }
                    }
                }
            }
        }
    }

    fun resetToHome() {
        otpTimerJob?.cancel()
        _uiState.value = EmergencyFlowState.Idle
    }
}
