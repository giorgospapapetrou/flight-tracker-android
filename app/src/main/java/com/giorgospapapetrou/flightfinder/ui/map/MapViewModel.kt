package com.giorgospapapetrou.flightfinder.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giorgospapapetrou.flightfinder.data.repository.AircraftRepository
import com.giorgospapapetrou.flightfinder.domain.model.Aircraft
import com.giorgospapapetrou.flightfinder.domain.model.AircraftEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class MapUiState(
    val aircraft: Map<String, Aircraft> = emptyMap(),
    val selectedIcao: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isStreamConnected: Boolean = false,
) {
    val aircraftList: List<Aircraft> get() = aircraft.values.toList()
    val selectedAircraft: Aircraft? get() = selectedIcao?.let { aircraft[it] }
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val aircraftRepository: AircraftRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadInitialSnapshot()
        observeLiveStream()
    }

    private fun loadInitialSnapshot() {
        viewModelScope.launch {
            try {
                val initial = aircraftRepository.fetchCurrentAircraft()
                    .filter { it.hasPosition }
                    .associateBy { it.icao }
                _uiState.update {
                    it.copy(aircraft = initial, isLoading = false, errorMessage = null)
                }
            } catch (t: Throwable) {
                Timber.w(t, "Initial snapshot failed")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = t.localizedMessage ?: "Load failed")
                }
            }
        }
    }

    private fun observeLiveStream() {
        viewModelScope.launch {
            aircraftRepository.liveEvents().collect { event ->
                when (event) {
                    is AircraftEvent.Connected -> {
                        _uiState.update { it.copy(isStreamConnected = true) }
                    }
                    is AircraftEvent.Disconnected -> {
                        _uiState.update { it.copy(isStreamConnected = false) }
                    }
                    is AircraftEvent.Update -> {
                        if (event.aircraft.hasPosition) {
                            _uiState.update {
                                it.copy(aircraft = it.aircraft + (event.aircraft.icao to event.aircraft))
                            }
                        }
                    }
                    is AircraftEvent.Removed -> {
                        _uiState.update {
                            it.copy(aircraft = it.aircraft - event.icao)
                        }
                    }
                }
            }
        }
    }

    fun selectAircraft(icao: String?) {
        _uiState.update { it.copy(selectedIcao = icao) }
    }
}