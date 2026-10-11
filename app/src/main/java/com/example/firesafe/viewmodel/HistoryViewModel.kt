package com.example.firesafe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firesafe.data.model.HistoryItem
import com.example.firesafe.data.repository.HistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    data class Loaded(val items: List<HistoryItem>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _state = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    /** Call every time the History tab opens so a freshly sent alert shows up. */
    fun load() {
        viewModelScope.launch {
            // Keep showing the old list while refreshing (no flicker)
            if (_state.value !is HistoryUiState.Loaded) _state.value = HistoryUiState.Loading
            try {
                _state.value = HistoryUiState.Loaded(repository.getHistory())
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                _state.value = HistoryUiState.Error(
                    "Couldn't load history. Check your internet connection. (${e.message?.take(100) ?: e.javaClass.simpleName})"
                )
            }
        }
    }
}
