package com.tapago.feature.photoshare.domain

import com.tapago.feature.tracking.domain.ActivityType

/**
 * Estatísticas de uma sessão já resolvidas para exibição no overlay da foto.
 * Modelo de domínio puro — a formatação visual (cores, fontes) fica na camada
 * de apresentação; aqui só existem os valores e labels textuais.
 */
data class RunPhotoStats(
    val activityType: ActivityType,
    val distanceKm: Double,
    val durationLabel: String,
    val paceLabel: String,
    val caloriesKcal: Int,
    val dateLabel: String,
    val appWatermark: String = "Tá Pago",
)
