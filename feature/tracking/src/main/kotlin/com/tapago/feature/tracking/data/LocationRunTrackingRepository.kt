package com.tapago.feature.tracking.data

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.tapago.core.common.Outcome
import com.tapago.feature.tracking.domain.ActivityType
import com.tapago.feature.tracking.domain.EstimateCaloriesUseCase
import com.tapago.feature.tracking.domain.GeoPoint
import com.tapago.feature.tracking.domain.LiveRunState
import com.tapago.feature.tracking.domain.RunSession
import com.tapago.feature.tracking.domain.RunTrackingRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Implementação real do rastreamento, usando o FusedLocationProviderClient
 * do Google Play Services. Acumula pontos de GPS e calcula distância
 * incrementalmente com [Location.distanceBetween].
 */
@Singleton
class LocationRunTrackingRepository @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val estimateCaloriesUseCase: EstimateCaloriesUseCase,
) : RunTrackingRepository {

    private val scope = CoroutineScope(SupervisorJob())
    private val _liveState = MutableStateFlow(LiveRunState())
    override val liveState: StateFlow<LiveRunState> = _liveState.asStateFlow()

    private var startTimeMillis: Long = 0L
    private var lastLocation: Location? = null
    private var timerJob: kotlinx.coroutines.Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            onNewLocation(location)
        }
    }

    @SuppressLint("MissingPermission") // permissão verificada na camada de UI antes de chamar
    override fun startTracking(activityType: ActivityType) {
        startTimeMillis = System.currentTimeMillis()
        lastLocation = null
        _liveState.update {
            LiveRunState(activityType = activityType, isTracking = true, isPaused = false)
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_METERS)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
        startTimer()
    }

    override fun pauseTracking() {
        _liveState.update { it.copy(isPaused = true) }
        timerJob?.cancel()
    }

    override fun resumeTracking() {
        _liveState.update { it.copy(isPaused = false) }
        startTimer()
    }

    override suspend fun stopTracking(): Outcome<RunSession> {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()

        val state = _liveState.value
        if (state.route.isEmpty()) {
            _liveState.update { LiveRunState() }
            return Outcome.Error(IllegalStateException("Nenhum ponto de GPS registrado"))
        }

        val endTimeMillis = System.currentTimeMillis()
        val session = RunSession(
            id = UUID.randomUUID().toString(),
            activityType = state.activityType,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            route = state.route,
            distanceMeters = state.distanceMeters,
            caloriesKcal = estimateCaloriesUseCase(
                activityType = state.activityType,
                durationSeconds = state.elapsedSeconds,
            ),
        )
        _liveState.update { LiveRunState() }
        return Outcome.Success(session)
    }

    private fun onNewLocation(location: Location) {
        val previous = lastLocation
        val incrementalDistance = previous?.distanceTo(location)?.toDouble() ?: 0.0
        lastLocation = location

        _liveState.update { current ->
            val newDistance = current.distanceMeters + incrementalDistance
            current.copy(
                distanceMeters = newDistance,
                route = current.route + GeoPoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestampMillis = location.time,
                ),
                currentPaceSecPerKm = if (newDistance > 0) {
                    current.elapsedSeconds / (newDistance / 1000)
                } else {
                    null
                },
            )
        }
    }

    private fun startTimer() {
        timerJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (!_liveState.value.isPaused) {
                    _liveState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    private companion object {
        const val LOCATION_INTERVAL_MS = 2000L
        const val MIN_UPDATE_DISTANCE_METERS = 3f
    }
}
