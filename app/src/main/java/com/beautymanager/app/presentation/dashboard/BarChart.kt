package com.beautymanager.app.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class BarChartEntry(val label: String, val value: Float)

/**
 * Gráfico de barras simples e leve, desenhado direto com Canvas do Compose — sem
 * depender de biblioteca de terceiros. Pensado para poucos itens (5–7), como
 * "produtos mais vendidos" ou "faturamento dos últimos 7 dias".
 */
@Composable
fun SimpleBarChart(
    entries: List<BarChartEntry>,
    modifier: Modifier = Modifier,
    barColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    if (entries.isEmpty()) return
    val maxValue = entries.maxOf { it.value }.coerceAtLeast(1f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val barSpacing = size.width / entries.size
            val barWidth = barSpacing * 0.55f
            entries.forEachIndexed { index, entry ->
                val barHeightRatio = entry.value / maxValue
                val barHeight = size.height * barHeightRatio
                val left = index * barSpacing + (barSpacing - barWidth) / 2
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
            }
            // Linha de base
            drawLine(
                color = barColor.copy(alpha = 0.2f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = Stroke.DefaultMiter
            )
        }
        Row(entries)
    }
}

@Composable
private fun Row(entries: List<BarChartEntry>) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        entries.forEach { entry ->
            Text(
                text = entry.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
