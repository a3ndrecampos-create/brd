package com.tapago.feature.tracking.domain

import com.tapago.core.common.Outcome
import kotlinx.coroutines.flow.StateFlow

/** Estado ao vivo de uma sessão em andamento. */
data class LiveRunState(
    val activityType: ActivityType = ActivityType.CORRIDA,
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val route: List<GeoPoint> = emptyList(),
    val currentPaceSecPerKm: Double? = null,
)

/**
 * Contrato de domínio para rastreamento de atividade. A implementação real
 * mora na camada `data`, usando o FusedLocationProviderClient.
 */
interface RunTrackingRepository {
    val liveState: StateFlow<LiveRunState>

    fun startTracking(activityType: ActivityType)
    fun pauseTracking()
    fun resumeTracking()
    suspend fun stopTracking(): Outcome<RunSession>
}
