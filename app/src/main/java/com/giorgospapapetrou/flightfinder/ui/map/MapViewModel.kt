package com.giorgospapapetrou.flightfinder.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giorgospapapetrou.flightfinder.data.repository.AircraftRepository
import com.giorgospapapetrou.flightfinder.domain.model.Aircraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class MapUiState(
    val aircraft: List<Aircraft> = emptyList(),
    val selectedIcao: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val selectedAircraft: Aircraft?
        get() = selectedIcao?.let { icao -> aircraft.firstOrNull { it.icao == icao } }
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val aircraftRepository: AircraftRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun refresh() {
        try {
            val aircraft = aircraftRepository.fetchCurrentAircraft()
                .filter { it.hasPosition }
            _uiState.update {
                it.copy(
                    aircraft = aircraft,
                    isLoading = false,
                    errorMessage = null,
                )
            }
        } catch (t: Throwable) {
            Timber.w(t, "Failed to fetch aircraft")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = t.localizedMessage ?: "Failed to load",
                )
            }
        }
    }

    fun selectAircraft(icao: String?) {
        _uiState.update { it.copy(selectedIcao = icao) }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 3_000L
    }
}