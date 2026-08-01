package com.tapago.feature.photoshare.domain

import com.tapago.feature.tracking.domain.RunSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Transforma uma [RunSession] finalizada nos dados prontos para o overlay
 * da foto — sem nenhuma dependência do Android SDK (testável em JVM puro).
 */
class BuildRunPhotoStatsUseCase @Inject constructor() {

    operator fun invoke(session: RunSession): RunPhotoStats = RunPhotoStats(
        activityType = session.activityType,
        distanceKm = session.distanceMeters / 1000,
        durationLabel = formatDuration(session.durationSeconds),
        paceLabel = formatPace(session.avgPaceSecPerKm),
        caloriesKcal = session.caloriesKcal,
        dateLabel = dateFormatter.format(Date(session.endTimeMillis)),
    )

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
        return "%d:%02d /km".format(minutes, seconds)
    }

    private companion object {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    }
}
