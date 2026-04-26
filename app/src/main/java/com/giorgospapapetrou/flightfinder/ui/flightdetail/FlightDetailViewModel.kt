package com.giorgospapapetrou.flightfinder.ui.flightdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giorgospapapetrou.flightfinder.data.repository.FlightRepository
import com.giorgospapapetrou.flightfinder.domain.model.FlightDetail
import com.giorgospapapetrou.flightfinder.domain.model.FlightPosition
import com.giorgospapapetrou.flightfinder.data.api.describeNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class FlightDetailUiState(
    val detail: FlightDetail? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** 0.0 = start of flight, 1.0 = end of flight. Null = replay not engaged. */
    val replayFraction: Float? = null,
) {
    /** The interpolated position at the current scrub fraction, or null if not scrubbing. */
    val replayPosition: FlightPosition?
        get() {
            val f = replayFraction ?: return null
            return detail?.interpolateAt(f.toDouble())
        }
}

@HiltViewModel
class FlightDetailViewModel @Inject constructor(
    private val flightRepository: FlightRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlightDetailUiState())
    val uiState: StateFlow<FlightDetailUiState> = _uiState.asStateFlow()

    fun load(flightId: Int) {
        if (_uiState.value.detail?.summary?.id == flightId) return
        viewModelScope.launch {
            _uiState.value = FlightDetailUiState(isLoading = true)
            try {
                val detail = flightRepository.fetchFlightDetail(flightId)
                _uiState.value = FlightDetailUiState(detail = detail, isLoading = false)
            } catch (t: Throwable) {
                Timber.w(t, "Failed to load flight $flightId")
                _uiState.value = FlightDetailUiState(
                    isLoading = false,
                    errorMessage = describeNetworkError(t),
                )
            }
        }
    }

    fun setReplayFraction(fraction: Float?) {
        _uiState.update { it.copy(replayFraction = fraction) }
    }
}