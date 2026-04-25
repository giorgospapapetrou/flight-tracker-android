package com.giorgospapapetrou.flightfinder.ui.flightdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giorgospapapetrou.flightfinder.data.repository.FlightRepository
import com.giorgospapapetrou.flightfinder.domain.model.FlightDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class FlightDetailUiState(
    val detail: FlightDetail? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

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
                    errorMessage = t.localizedMessage ?: "Failed to load flight",
                )
            }
        }
    }
}