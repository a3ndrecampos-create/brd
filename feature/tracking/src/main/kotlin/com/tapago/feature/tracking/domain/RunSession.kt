package com.tapago.feature.tracking.domain

import kotlinx.serialization.Serializable

/** Ponto geográfico capturado durante a atividade. Sem dependência do Android SDK. */
@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
)

/** Tipo de atividade suportado pelo Tá Pago. */
enum class ActivityType { CORRIDA, CAMINHADA }

/**
 * Sessão de corrida/caminhada finalizada, com o percurso completo e as
 * estatísticas calculadas. Modelo de domínio puro (sem Room/Retrofit).
 */
data class RunSession(
    val id: String,
    val activityType: ActivityType,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val route: List<GeoPoint>,
    val distanceMeters: Double,
    val caloriesKcal: Int,
) {
    val durationSeconds: Long
        get() = (endTimeMillis - startTimeMillis) / 1000

    /** Ritmo médio em segundos por quilômetro. Null se distância for zero. */
    val avgPaceSecPerKm: Double?
        get() = if (distanceMeters <= 0) null else durationSeconds / (distanceMeters / 1000)
}
