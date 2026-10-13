package com.example.firesafe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firesafe.data.model.FireStation
import com.example.firesafe.data.model.LocationData
import com.example.firesafe.data.repository.FireStationRepository
import com.example.firesafe.data.repository.LocationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StationsUiState {
    object Loading : StationsUiState()
    data class Loaded(
        val stations: List<FireStation>,
        val userLocation: LocationData? = null
    ) : StationsUiState()
    data class Error(val message: String) : StationsUiState()
}

class StationsViewModel(
    private val stationRepository: FireStationRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<StationsUiState>(StationsUiState.Loading)
    val state: StateFlow<StationsUiState> = _state.asStateFlow()

    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()

    fun load() {
        viewModelScope.launch {
            if (_state.value !is StationsUiState.Loaded) _state.value = StationsUiState.Loading
            try {
                // Current GPS position, or the last one saved by the app
                val location = try {
                    locationRepository.getCurrentLocation()
                } catch (e: Exception) {
                    locationRepository.getInitialLocation()
                }

                _currentLocation.value = location

                if (location == null) {
                    _state.value = StationsUiState.Error(
                        "Location is needed to find the stations near you. Allow location access and try again."
                    )
                    return@launch
                }

                val stations = stationRepository.getAllStations(location.latitude, location.longitude)
                    .sortedBy { it.distanceKm }

                _state.value = StationsUiState.Loaded(
                    stations = stations,
                    userLocation = location
                )
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                _state.value = StationsUiState.Error(
                    "Couldn't load fire stations. Check your internet connection. (${e.message?.take(100) ?: e.javaClass.simpleName})"
                )
            }
        }
    }
}
