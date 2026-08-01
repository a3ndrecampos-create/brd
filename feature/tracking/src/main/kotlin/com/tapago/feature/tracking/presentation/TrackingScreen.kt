package com.tapago.feature.tracking.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tapago.feature.tracking.domain.ActivityType
import com.tapago.feature.tracking.domain.LiveRunState

@Composable
fun TrackingScreen(
    onRunFinished: (sessionId: String) -> Unit,
    viewModel: TrackingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TrackingUiEvent.RunFinished -> onRunFinished(event.sessionId)
                is TrackingUiEvent.Error -> Unit // TODO: exibir snackbar
            }
        }
    }

    TrackingContent(
        state = state,
        onStart = viewModel::onStartClicked,
        onPauseResume = viewModel::onPauseResumeClicked,
        onStop = viewModel::onStopClicked,
    )
}

@Composable
private fun TrackingContent(
    state: LiveRunState,
    onStart: (ActivityType) -> Unit,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = formatDistanceKm(state.distanceMeters), style = MaterialTheme.typography.headlineLarge)
        Text(text = "km", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            StatColumn(label = "Tempo", value = formatDuration(state.elapsedSeconds))
            StatColumn(label = "Ritmo /km", value = formatPace(state.currentPaceSecPerKm))
        }

        Spacer(modifier = Modifier.height(40.dp))

        when {
            !state.isTracking -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onStart(ActivityType.CORRIDA) }) { Text("Iniciar corrida") }
                    OutlinedButton(onClick = { onStart(ActivityType.CAMINHADA) }) { Text("Iniciar caminhada") }
                }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = onPauseResume) {
                        Text(if (state.isPaused) "Retomar" else "Pausar")
                    }
                    Button(onClick = onStop) { Text("Finalizar") }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatDistanceKm(meters: Double): String = "%.2f".format(meters / 1000)

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun formatPace(secPerKm: Double?): String {
    if (secPerKm == null || secPerKm.isNaN() || secPerKm.isInfinite()) return "--:--"
    val minutes = (secPerKm / 60).toInt()
    val seconds = (secPerKm % 60).toInt()
    return "%d:%02d".format(minutes, seconds)
}
