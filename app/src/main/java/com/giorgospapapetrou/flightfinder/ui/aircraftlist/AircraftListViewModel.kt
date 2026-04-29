package com.giorgospapapetrou.flightfinder.ui.aircraftlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giorgospapapetrou.flightfinder.data.api.describeNetworkError
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

data class AircraftListUiState(
    val aircraft: Map<String, Aircraft> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val sortedAircraft: List<Aircraft>
        get() = aircraft.values.sortedWith(
            compareByDescending<Aircraft> { it.lastPositionAt }
                .thenBy { it.icao }
        )
}

@HiltViewModel
class AircraftListViewModel @Inject constructor(
    private val aircraftRepository: AircraftRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AircraftListUiState())
    val uiState: StateFlow<AircraftListUiState> = _uiState.asStateFlow()

    init {
        refreshSnapshot()
        observeLiveStream()
    }

    /** Fetches authoritative current aircraft list from the backend. */
    private fun refreshSnapshot() {
        viewModelScope.launch {
            try {
                val initial = aircraftRepository.fetchCurrentAircraft()
                    .associateBy { it.icao }
                _uiState.update {
                    it.copy(aircraft = initial, isLoading = false, errorMessage = null)
                }
            } catch (t: Throwable) {
                Timber.w(t, "Aircraft snapshot failed")
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = describeNetworkError(t))
                }
            }
        }
    }

    private fun observeLiveStream() {
        viewModelScope.launch {
            aircraftRepository.liveEvents().collect { event ->
                when (event) {
                    is AircraftEvent.Update -> {
                        _uiState.update {
                            it.copy(aircraft = it.aircraft + (event.aircraft.icao to event.aircraft))
                        }
                    }
                    is AircraftEvent.Removed -> {
                        _uiState.update { it.copy(aircraft = it.aircraft - event.icao) }
                    }
                    AircraftEvent.Connected -> {
                        // Re-fetch snapshot to discard stale state from any
                        // disconnect window where we may have missed events.
                        Timber.i("WS connected — refreshing aircraft snapshot")
                        refreshSnapshot()
                    }
                    AircraftEvent.Disconnected -> { /* ignore */ }
                }
            }
        }
    }
}