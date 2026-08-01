package com.tapago.feature.photoshare.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.tapago.feature.photoshare.domain.RunPhotoStats
import com.tapago.feature.tracking.domain.ActivityType
import javax.inject.Inject

/**
 * Desenha as estatísticas da corrida/caminhada diretamente sobre o bitmap
 * capturado pela câmera, gerando a imagem final pronta para compartilhar —
 * equivalente ao "Stats Sticker" do Strava, mas já renderizado nativamente
 * pelo Tá Pago (sem depender do editor do Instagram).
 */
class StatsOverlayComposer @Inject constructor() {

    fun compose(sourcePhoto: Bitmap, stats: RunPhotoStats): Bitmap {
        val output = sourcePhoto.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val width = output.width.toFloat()
        val height = output.height.toFloat()

        drawScrim(canvas, width, height)
        drawActivityBadge(canvas, width, stats)
        drawMainStat(canvas, width, height, stats)
        drawSecondaryStats(canvas, width, height, stats)
        drawWatermark(canvas, width, height, stats)

        return output
    }

    private fun drawScrim(canvas: Canvas, width: Float, height: Float) {
        val scrimHeight = height * SCRIM_HEIGHT_RATIO
        val paint = Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, height - scrimHeight, 0f, height,
                Color.TRANSPARENT, Color.argb(190, 0, 0, 0),
                android.graphics.Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(RectF(0f, height - scrimHeight, width, height), paint)
    }

    private fun drawActivityBadge(canvas: Canvas, width: Float, stats: RunPhotoStats) {
        val label = when (stats.activityType) {
            ActivityType.CORRIDA -> "CORRIDA"
            ActivityType.CAMINHADA -> "CAMINHADA"
        }
        val paint = textPaint(size = 28f, bold = true, color = Color.parseColor("#FF5A1F"))
        canvas.drawText(label, PADDING, 80f, paint)
        val datePaint = textPaint(size = 24f, color = Color.WHITE)
        canvas.drawText(stats.dateLabel, PADDING, 115f, datePaint)
    }

    private fun drawMainStat(canvas: Canvas, width: Float, height: Float, stats: RunPhotoStats) {
        val distanceText = "%.2f km".format(stats.distanceKm)
        val paint = textPaint(size = 96f, bold = true, color = Color.WHITE)
        canvas.drawText(distanceText, PADDING, height - 220f, paint)
    }

    private fun drawSecondaryStats(canvas: Canvas, width: Float, height: Float, stats: RunPhotoStats) {
        val y = height - 130f
        val labelPaint = textPaint(size = 22f, color = Color.argb(200, 255, 255, 255))
        val valuePaint = textPaint(size = 34f, bold = true, color = Color.WHITE)

        val columnWidth = (width - PADDING * 2) / 3
        val entries = listOf(
            "TEMPO" to stats.durationLabel,
            "RITMO" to stats.paceLabel,
            "KCAL" to stats.caloriesKcal.toString(),
        )
        entries.forEachIndexed { index, (label, value) ->
            val x = PADDING + columnWidth * index
            canvas.drawText(label, x, y, labelPaint)
            canvas.drawText(value, x, y + 40f, valuePaint)
        }
    }

    private fun drawWatermark(canvas: Canvas, width: Float, height: Float, stats: RunPhotoStats) {
        val paint = textPaint(size = 26f, bold = true, color = Color.WHITE)
        val textWidth = paint.measureText(stats.appWatermark)
        canvas.drawText(stats.appWatermark, width - textWidth - PADDING, height - PADDING, paint)
    }

    private fun textPaint(size: Float, bold: Boolean = false, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL,
        )
        setShadowLayer(6f, 0f, 2f, Color.argb(160, 0, 0, 0))
    }

    private companion object {
        const val PADDING = 48f
        const val SCRIM_HEIGHT_RATIO = 0.42f
    }
}
