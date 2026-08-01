package com.tapago.feature.tracking.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tapago.core.common.Outcome
import com.tapago.feature.tracking.domain.ActivityType
import com.tapago.feature.tracking.domain.LiveRunState
import com.tapago.feature.tracking.domain.RunSessionRepository
import com.tapago.feature.tracking.domain.RunTrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Eventos únicos de UI (navegação, erros pontuais). */
sealed interface TrackingUiEvent {
    data class RunFinished(val sessionId: String) : TrackingUiEvent
    data class Error(val message: String) : TrackingUiEvent
}

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val repository: RunTrackingRepository,
    private val sessionRepository: RunSessionRepository,
) : ViewModel() {

    val uiState: StateFlow<LiveRunState> = repository.liveState

    private val _events = MutableSharedFlow<TrackingUiEvent>()
    val events: SharedFlow<TrackingUiEvent> = _events

    fun onStartClicked(activityType: ActivityType) {
        repository.startTracking(activityType)
    }

    fun onPauseResumeClicked() {
        if (uiState.value.isPaused) repository.resumeTracking() else repository.pauseTracking()
    }

    fun onStopClicked() {
        viewModelScope.launch {
            when (val result = repository.stopTracking()) {
                is Outcome.Success -> {
                    sessionRepository.save(result.data)
                    _events.emit(TrackingUiEvent.RunFinished(result.data.id))
                }
                is Outcome.Error -> _events.emit(
                    TrackingUiEvent.Error(result.message ?: "Não foi possível salvar a atividade"),
                )
                Outcome.Loading -> Unit
            }
        }
    }
}
