package com.giorgospapapetrou.flightfinder.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giorgospapapetrou.flightfinder.data.repository.HealthRepository
import com.giorgospapapetrou.flightfinder.domain.model.HealthStatus
import com.giorgospapapetrou.flightfinder.data.api.describeNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface SplashUiState {
    data object Connecting : SplashUiState
    data class Connected(val status: HealthStatus) : SplashUiState
    data class Failed(val message: String) : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Connecting)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        connect()
    }

    fun connect() {
        _uiState.value = SplashUiState.Connecting
        viewModelScope.launch {
            try {
                val status = healthRepository.fetchHealth()
                _uiState.value = SplashUiState.Connected(status)
                Timber.i("Backend connected: %s", status)
            } catch (t: Throwable) {
                Timber.w(t, "Health check failed")
                _uiState.value = SplashUiState.Failed(describeNetworkError(t))
            }
        }
    }
}