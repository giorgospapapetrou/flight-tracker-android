package com.giorgospapapetrou.flightfinder.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giorgospapapetrou.flightfinder.data.api.describeNetworkError
import com.giorgospapapetrou.flightfinder.data.repository.FlightRepository
import com.giorgospapapetrou.flightfinder.domain.model.FlightSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HistoryUiState(
    val flights: List<FlightSummary> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val flightRepository: FlightRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
        startAutoRefresh()
    }

    fun refresh() {
        viewModelScope.launch {
            // Don't show the spinner if we already have data
            val showSpinner = _uiState.value.flights.isEmpty()
            if (showSpinner) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            }
            try {
                val flights = flightRepository.fetchFlights()
                _uiState.value = HistoryUiState(flights = flights, isLoading = false)
            } catch (t: Throwable) {
                Timber.w(t, "Failed to load flights")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = describeNetworkError(t),
                )
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                refresh()
            }
        }
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 15_000L
    }
}