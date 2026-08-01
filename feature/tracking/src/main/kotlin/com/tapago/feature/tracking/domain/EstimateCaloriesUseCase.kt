package com.tapago.feature.tracking.domain

import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Estima calorias queimadas a partir do MET (Metabolic Equivalent of Task)
 * da atividade, peso do usuário e duração. Fórmula: kcal = MET * peso(kg) * horas.
 * Sem peso informado, assume 70kg como padrão conservador.
 */
class EstimateCaloriesUseCase @Inject constructor() {

    operator fun invoke(
        activityType: ActivityType,
        durationSeconds: Long,
        userWeightKg: Double = DEFAULT_WEIGHT_KG,
    ): Int {
        val met = when (activityType) {
            ActivityType.CORRIDA -> MET_RUNNING
            ActivityType.CAMINHADA -> MET_WALKING
        }
        val hours = durationSeconds / SECONDS_IN_HOUR
        return (met * userWeightKg * hours).roundToInt()
    }

    private companion object {
        const val MET_RUNNING = 9.8
        const val MET_WALKING = 3.8
        const val DEFAULT_WEIGHT_KG = 70.0
        const val SECONDS_IN_HOUR = 3600.0
    }
}
